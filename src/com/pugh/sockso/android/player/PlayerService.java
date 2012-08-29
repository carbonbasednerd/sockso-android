package com.pugh.sockso.android.player;

import java.io.IOException;
import java.util.List;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.pugh.sockso.android.R;
import com.pugh.sockso.android.activity.PlayerActivity;
import com.pugh.sockso.android.music.Track;

public class PlayerService extends Service implements OnPreparedListener, 
                                                      OnCompletionListener, 
                                                      OnBufferingUpdateListener,
                                                      OnErrorListener {

    private static final String TAG = PlayerService.class.getSimpleName();

    // indicates the state our service:
    enum State {
        Retrieving, // the MediaRetriever is retrieving music
        Stopped,    // media player is stopped and not prepared to play
        Preparing,  // media player is preparing...
        Playing,    // playback active (media player ready!). (but the media player may actually be
                    // paused in this state if we don't have audio focus. But we stay in this state
                    // so that we know we have to resume playback once we get focus back)
        Paused      // playback paused (media player ready!)
    };
    
    private State mState = State.Retrieving; // correct start state?
    
    // Media Player
    private MediaPlayer mPlayer = null;

    // Notification status bar
    private Notification mNotification = null;
    
    private static final int NOTIFICATION_ID = 1; // just a number to identify notification type
    
    // Playlist of tracks (can be one)
    private List<Track> mPlaylist = null;

    // Binder object for clients that want to call methods on this service
    private IBinder mBinder = new PlayerServiceBinder();

    
    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate() called");
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy() called");

        mBinder = null;

        super.onDestroy();

        if (mPlayer != null) {
            if (mPlayer.isPlaying()) {
                mPlayer.stop();
            }
            mPlayer.release();
        }
        
        mPlayer = null;

        // clearNotification();
        // releaseLocks();
    }

    

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind() ran");
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "onUnbind() ran");
        return super.onUnbind(intent);
    }

    /**
     * Class for clients to access. Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     * Supposably there is a memory leak in Android with this approach:
     * http://code.google.com/p/android/issues/detail?id=6426
     * Consider a work-around
     */
    public class PlayerServiceBinder extends Binder {

        public PlayerService getService() {
            return PlayerService.this;
        }
    }

    public boolean isPlaying() {
        Log.d(TAG, "isPlaying() called");
        
        if (mPlayer == null || !mPlayer.isPlaying() ) {
            return false;
        }
        
        return true;
    }

    public void pause() {
        Log.d(TAG, "pause() called");
        
        if (mPlayer != null && mPlayer.isPlaying()) {
            
            mPlayer.pause();
            
            // stop being a foreground service
            stopForeground(true);
            
            mState = State.Paused;
        }
    }

    /**
     * TODO
     * Starts playback of a previously opened file.
     */
    public void play() {
        Log.d(TAG, "play() called");

        if ( mState == State.Paused ) {
            // If we're paused, just continue playback and restore the 'foreground service' state.
            mState = State.Playing;
            setUpAsForeground("Sockso: (playing)");
            configAndStartMediaPlayer();
        }
        else {
            
            // TODO REMOVE
            String url = "http://sockso.perrierliquors.com:4444/stream/459"; // alice deejay
            // String url = "http://www.vorbis.com/music/Epoq-Lepidoptera.ogg";
            
            createMediaPlayerIfNeeded();
            
            mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

            try {
                mPlayer.setDataSource(url);
                // mPlayer.prepareAsync();
                // TODO I don't know why, but prepareAsync does not work correctly (needs a looper
                // message?)
                mPlayer.prepare();
                
                // puts the notification in the status bar
                setUpAsForeground("Sockso: (playing)");
                configAndStartMediaPlayer();
            }
            catch (IOException e) {
                Log.e(TAG, "IOException with url " + url + ": " + e.getMessage());
            }
        }
    }

    // TODO
    public void stop() {
        Log.d(TAG, "stop() called");
        mState = State.Stopped;
    }

    /**
     * Makes sure the media player exists and has been reset.
     * This will create the media player if needed, or reset the existing media player if one
     * already exists.
     */
    void createMediaPlayerIfNeeded() {

        if (mPlayer == null) {

            Log.d(TAG, "Creating a new MediaPlayer!");

            mPlayer = new MediaPlayer();

            /**
             * Make sure the media player will acquire a wake-lock while playing.
             * If we don't do that, the CPU might go to sleep while the song is playing, causing
             * playback to stop.
             * Remember that to use this, we have to declare the android.permission.WAKE_LOCK
             * permission in AndroidManifest.xml.
             * mPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
             */
        }
        else {
            mPlayer.reset();
        }
    }

    /**
     * Reconfigures MediaPlayer according to audio focus settings and starts/restarts it. This
     * method starts/restarts the MediaPlayer respecting the current audio focus state. So if
     * we have focus, it will play normally; if we don't have focus, it will either leave the
     * MediaPlayer paused or set it to a low volume, depending on what is allowed by the
     * current focus settings. This method assumes mPlayer != null, so if you are calling it,
     * you have to do so from a context where you are sure this is the case.
     */
    void configAndStartMediaPlayer() {
        Log.d(TAG, "configAndStartMediaPlayer() called");
        
        mPlayer.setVolume(1.0f, 1.0f);
        
        if ( ! mPlayer.isPlaying() ) {
            mPlayer.start();
            mState = State.Playing;
        }
    }

    /**
     * Called when media player is done preparing.
     * That means we can start playing!
     */
    public void onPrepared(MediaPlayer player) {
        Log.d(TAG, "onPrepared() called");
        // mState = State.Playing;
        // updateNotification(mSongTitle + " (playing)");
        configAndStartMediaPlayer();
    }

    /**
     * Called when the mediaplayer is done playing the current track
     */
    @Override
    public void onCompletion(MediaPlayer player) {
        Log.d(TAG, "onCompletion() called");
    }
    
    @Override
    public void onBufferingUpdate(MediaPlayer mediaPlayer, int percentage) {
        Log.d(TAG, "onBufferingUpdate(): " + percentage);
    }

    @Override
    public boolean onError(MediaPlayer player, int what, int extra) {
        Log.d(TAG, "onError() called");
        
        // TODO handle errors
        //MediaPlayer.MEDIA_ERROR_UNKNOWN
        return false;
    }

    
    /**
     * Configures service as a foreground service. 
     * 
     * A foreground service is a service that's doing something the user is actively aware of 
     * (such as playing music), and must appear to the user as a notification. 
     * 
     * That's why we create the notification here.
     */
    void setUpAsForeground(final String text) {
        
        Intent intent = new Intent(getApplicationContext(), PlayerActivity.class);
        intent.setAction("From Service!");
        // These flags are important: 
        // "Clear top" means 
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);
        
        mNotification = new Notification();
        mNotification.tickerText = text;
        mNotification.icon = R.drawable.ic_stat_playing;
        mNotification.flags |= Notification.FLAG_ONGOING_EVENT;
        mNotification.setLatestEventInfo(getApplicationContext(), TAG, text, pendingIntent);
        
        startForeground(NOTIFICATION_ID, mNotification);
    }

}