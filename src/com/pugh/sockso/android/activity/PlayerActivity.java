package com.pugh.sockso.android.activity;

import java.util.List;

import android.app.Activity;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.pugh.sockso.android.R;
import com.pugh.sockso.android.data.CoverArtFetcher;
import com.pugh.sockso.android.data.MusicManager;
import com.pugh.sockso.android.music.Track;
import com.pugh.sockso.android.player.MusicUtils;
import com.pugh.sockso.android.player.PlayerService;

public class PlayerActivity extends Activity {

    private static final String TAG = PlayerActivity.class.getSimpleName();

    // View buttons
    private ImageButton mPlayButton;
    private ImageButton mForwardButton;
    private ImageButton mBackwardButton;
    private ImageButton mNextButton;
    private ImageButton mPreviousButton;
    private ImageButton mPlaylistButton;
    private ImageButton mRepeatButton;
    private ImageButton mShuffleButton;

    // View seekbar
    private SeekBar mTrackProgressBar;

    // View duration
    private TextView mTrackCurrentDurationLabel;
    private TextView mTrackTotalDurationLabel;

    // View track info
    private TextView mArtistNameLabel;
    private TextView mAlbumNameLabel;
    private TextView mTrackNameLabel;

    // View cover art
    private ImageView mAlbumCover;

    // Service that is playing music in the background
    private PlayerService mService;

    // Is this activity bound to the PlayerService?
    private boolean mIsBound = false;

    // Is the activity paused?
    private boolean mIsActivityPaused = false;
    
    private boolean mIsShuffling = false;
    private boolean mIsRepeating = false;
    private boolean mProgressIsSeeking = false;
    
    // Intent actions
    public static final String ACTION_PLAY_TRACK  = "com.pugh.sockso.android.player.ACTION_PLAY_TRACK";
    public static final String ACTION_PLAY_ALBUM  = "com.pugh.sockso.android.player.ACTION_PLAY_ALBUM";
    public static final String ACTION_VIEW_PLAYER = "com.pugh.sockso.android.player.ACTION_VIEW_PLAYER";
    
    private final static int REFRESH = 1;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate() ran");

        super.onCreate(savedInstanceState);
 
        setContentView(R.layout.player);

        // All player buttons
        mPlayButton     = (ImageButton) findViewById(R.id.playPauseButton);
        mForwardButton  = (ImageButton) findViewById(R.id.forwardButton);
        mBackwardButton = (ImageButton) findViewById(R.id.backwardButton);
        mNextButton     = (ImageButton) findViewById(R.id.nextButton);
        mPreviousButton = (ImageButton) findViewById(R.id.previousButton);
        //mPlaylistButton = (ImageButton) findViewById(R.id.playlistButton);
        //mRepeatButton   = (ImageButton) findViewById(R.id.repeatButton);
        //mShuffleButton  = (ImageButton) findViewById(R.id.shuffleButton);

        mTrackProgressBar = (SeekBar) findViewById(R.id.trackProgressBar);
        mTrackProgressBar.setMax(100); // 100%
        
        mTrackCurrentDurationLabel = (TextView) findViewById(R.id.trackCurrentDurationLabel);
        mTrackTotalDurationLabel   = (TextView) findViewById(R.id.trackTotalDurationLabel);

        mTrackNameLabel = (TextView) findViewById(R.id.trackNameLabel);
        // TODO Add to the view:
        //mAlbumNameLabel = (TextView) findViewById(R.id.);
        mArtistNameLabel = (TextView) findViewById(R.id.artistNameLabel);
        mAlbumCover = (ImageView) findViewById(R.id.coverImage);
        
        
        /**
         * Play button click event
         * Plays a song and changes button to pause image Pauses a song and
         * changes button to play image
         */
        mPlayButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                Log.d(TAG, "PlayPause Button clicked");
                togglePlayPause();
            }
        });

        /**
         * Forward button click event Forwards song specified seconds
         */
        mForwardButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                Log.d(TAG, "ForwardSeek Button clicked");
                seekForward();
            }
        });

        /**
         * Backward button click event Backward song to specified seconds
         */
        mBackwardButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                Log.d(TAG, "BackwardSeek Button clicked");
                seekBackward();
            }
        });

        /**
         * Next Track button click event
         */
        mNextButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                Log.d(TAG, "NextTrack Button clicked");
                
                nextTrack();
            }
        });

        /**
         * Previous Track button click event
         */
        mPreviousButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                Log.d(TAG, "PreviousTrack Button clicked");
                
                prevTrack();
            }
        });

        /**
         * Button Click event for Repeat button Enables repeat flag to true
         */
//        mRepeatButton.setOnClickListener(new View.OnClickListener() {
//
//            @Override
//            public void onClick(View view) {
//                toggleRepeat();
//            }
//        });

        /**
         * Button Click event for Shuffle button Enables shuffle flag to true
         */
//        mShuffleButton.setOnClickListener(new View.OnClickListener() {
//
//            @Override
//            public void onClick(View view) {
//                toggleShuffle();
//
//            }
//        });

        /**
         * Button Click event for Playlist click event Launches list activity
         * which displays list of songs
         */
//        mPlaylistButton.setOnClickListener(new View.OnClickListener() {
//
//            @Override
//            public void onClick(View view) {
//                Log.d(TAG, "PlaylistButton clicked");
//                /*
//                 * Intent i = new Intent(getApplicationContext(),
//                 * PlayListActivity.class); startActivityForResult(i, 100);
//                 */
//            }
//        });

        mTrackProgressBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

            // When user starts moving the progress handler
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                Log.d(TAG, "onStartTrackingTouch() ran: " + seekBar.getProgress());

                // Set a flag to tell the UI Handler for the progress bar not to change it 
                // while the user is dragging it:
                mProgressIsSeeking = true;
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromTouch) {
                //Log.d(TAG, "onProgressChanged(): " + progress);
                // TODO
            }

            // When user stops moving the progress handler
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Log.d(TAG, "onStopTrackingTouch() ran: " + seekBar.getProgress());
                seekTo(seekBar.getProgress());
                mProgressIsSeeking = false;
            }
        });
     
    }

    // progress - percentage
    protected void seekTo(int progress) {
        
        if ( mService == null ) {
            return;
        }
        
        // Seek position in msecs
        int seekPos = MusicUtils.progressToTimer(progress, mService.getDuration());
        
        mService.seekTo(seekPos);
    }
    
    protected void seekForward() {
        
        if ( mService == null ) {
            return;
        }
        
        mService.seekForward();        
    }
    
    protected void seekBackward() {
        
        if ( mService == null ) {
            return;
        }
        
        mService.seekBackward();        
    }

    protected void nextTrack() {
        
        if ( mService == null ) {
            return;
        }
        
        mService.skipTrack();
    }

    protected void prevTrack() {
        
        if ( mService == null ) {
            return;
        }
        
        mService.prevTrack();
    }

    private final Handler mHandler = new Handler() {
        
        @Override
        public void handleMessage(Message msg) {
            
            switch (msg.what) {

                case REFRESH:
                    
                    long next = refreshTime();
                    queueNextRefresh(next);
                    
                    break;
                default:
                    break;
            }
        }
    };

    
    private void queueNextRefresh( long delay ) {
        //Log.d(TAG, "queueNextRefresh() delay: " + delay);
        
        if ( ! mIsActivityPaused ) {
            Message message = mHandler.obtainMessage(REFRESH);
            mHandler.removeMessages(REFRESH);
            mHandler.sendMessageDelayed(message, delay);
        }
    }
    
    
    private BroadcastReceiver mStatusListener = new BroadcastReceiver() {
        
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "mStatusListener.onReceive() called");
            
            String action = intent.getAction();
            
            if ( action.equals(PlayerService.TRACK_BUFFERING)) {
                int bufferProgress = intent.getIntExtra("percentage", 0);
                Log.d(TAG, "Track buffer updated: " + bufferProgress);
                if ( bufferProgress > 0 && bufferProgress < 100 ) {
                    mTrackProgressBar.setSecondaryProgress(bufferProgress);
                }
            }
            else if ( action.equals(PlayerService.PLAYSTATE_CHANGE) ) {
                Log.d(TAG, "Track stopped/resumed");
                setPlayButtonImage();
            }
            else if ( action.equals(PlayerService.TRACK_STARTED) 
                   || action.equals(PlayerService.TRACK_CHANGED) ) {
                Log.d(TAG, "Track started/changed");
                
                setPlayButtonImage();
                updateTrackInfo();
                //long next = refreshTime();
                //queueNextRefresh(next);
            }
            else if (action.equals(PlayerService.TRACK_ERROR)) {
                Log.d(TAG, "Track error");
                
                setPlayButtonImage();
                
                // TODO Could this happen in the middle of playing? TEST IT
                Toast.makeText(PlayerActivity.this, R.string.player_error, Toast.LENGTH_LONG).show();
            }
        }
    };
    
    private ServiceConnection mServiceConn = new ServiceConnection() {
        
        @Override
        public void onServiceConnected(ComponentName classname, IBinder service) {
            Log.d(TAG, "onServiceConnected() ran");
            mService = ((PlayerService.PlayerServiceBinder) service).getService();
            
            // Now that the service is connected, handle the intent sent to this activity:
            handleIntent(getIntent());
        }

        @Override
        public void onServiceDisconnected(ComponentName classname) {
            mService = null;
        }
    };

    
    @Override
    protected void onNewIntent(Intent intent) {
        Log.d(TAG, "onNewIntent() called");
        setIntent(intent);
    }


    private void handleIntent(Intent intent) {
        
        String action = intent.getAction();
        Log.d(TAG, "intent.getAction(): " + action);
        
        if (action != null) {

            // Play a track
            if (action.equals(ACTION_PLAY_TRACK)) {

                Bundle bundle = intent.getExtras();
                if (bundle == null) {
                    return;
                }

                long trackId = bundle.getLong(MusicManager.TRACK, -1);

                if (trackId != -1) {
                    playTrack(trackId);
                }

            }
            else if (action.equals(ACTION_PLAY_ALBUM)) {

                Bundle bundle = intent.getExtras();
                if (bundle == null) {
                    return;
                }

                long albumId  = bundle.getLong("album_id", -1);
                int trackPos = bundle.getInt("track_position", 1);
                // Map the list view item position to playlist (offset -1)
                trackPos--;
                
                if (albumId != -1) {
                    playAlbum(albumId, trackPos);
                }

            }
            else if (action.equals(ACTION_VIEW_PLAYER)) {

            }

        }
           
        // Update the UI
        setPlayButtonImage();
        updateTrackInfo();

        long next = refreshTime();
        queueNextRefresh(next);
        
        // Reset the intent that started this activity
        // This is important: if the activity stops and then re-starts,
        // we need to know that we've already started the playback
        setIntent(new Intent());
    }

    
    private void playTrack( long trackId ) {
        Log.d(TAG, "playTrack() called");

        if (mService == null) {
            Log.d(TAG, "mService is null");
            return;
        }
        
        Track serviceTrack = mService.getTrack();

        // Only start track if the track is different
        if (serviceTrack == null || trackId != serviceTrack.getId()) {
            Log.d(TAG, "Playing track: " + trackId);

            Track track = MusicManager.getTrack(getContentResolver(), trackId);

            mService.stop(); // stop whatever is currently playing
            mService.open(track);
            // updateTrackInfo();
            // Service starts playback asynchronously
            play();
        }
        else {
            // just update the UI
            // setRepeatButtonImage();
            // setShuffleButtonImage();
            setPlayButtonImage();
            updateTrackInfo();
        }
        
        long next = refreshTime();
        queueNextRefresh(next);
    }  
    
    
    private void playAlbum( long albumId, int trackPos ) {
        Log.d(TAG, "playAlbum() called");

        if (mService == null) {
            Log.d(TAG, "mService is null");
            return;
        }

        List<Track> tracks = MusicManager.getTrackForAlbum(getContentResolver(), albumId);

        mService.stop(); // stop whatever is currently playing
        mService.open(tracks);
        mService.setPlaylistPosition(trackPos);
        // updateTrackInfo();
        // Service starts playback asynchronously
        play();
        
        long next = refreshTime();
        queueNextRefresh(next);
    }  
    
    
    /** 
     * This should update the UI to reflect the current state of the player
     * 
     * Track Name
     * Artist Name
     * Album Name
     * Cover Artwork
     * Track Duration
     */ 
    private void updateTrackInfo() {
        Log.d(TAG, "updateTrackInfo() called");
        
        if (mService == null) {
            return;
        }
        
        Track track = mService.getTrack();
        
        if ( track != null ) {
            
            mArtistNameLabel.setText(track.getArtist());
            //mAlbumNameLabel.setText(track.getAlbum());
            mTrackNameLabel.setText(track.getName());
            
            int duration = mService.getDuration();
            Log.d(TAG, "duration: " + duration);
            
            String durationLabel = "--:--";
            
            if (duration != 0) {
                durationLabel = MusicUtils.millisToTimer(duration);
            }

            mTrackTotalDurationLabel.setText(durationLabel);
            
            CoverArtFetcher coverFetcher = new CoverArtFetcher(this);
            coverFetcher.setDimensions(300, 300);
            coverFetcher.loadCoverArtTrack(track.getServerId(), mAlbumCover);
        }
    }

    // This should update the parts of the UI that need to change quickly and often:
    // * progress bar
    // * timers
    private long refreshTime() {
        
        long half = 500; // half a second
        long whole = 1000; // whole second
        
        if ( mService == null ) {
            return half;
        }
        
        int position = mService.getPosition();
        int progress = 0;
        
        if ( position > 0 ) {          

            int duration = mService.getDuration();
            progress = MusicUtils.getProgressPercentage(position, duration);
            String currentTime = MusicUtils.millisToTimer(position);
            mTrackCurrentDurationLabel.setText(currentTime);
        }
        
        //Log.d(TAG, "refreshTime() progress: " + progress);
        if ( ! mProgressIsSeeking ) {
            mTrackProgressBar.setProgress(progress);
        }
        
        return whole;
    }
    
    
    @Override
    public void onStart() {
        Log.d(TAG, "onStart() ran");
        super.onStart();

        mIsActivityPaused = false;
        
        // Start the PlayerService
        Intent playerIntent = new Intent(this, PlayerService.class);
        startService(playerIntent);
        
        // Bind to PlayerService
        if ( ! mIsBound ) {
            Intent bindIntent = new Intent(PlayerActivity.this, PlayerService.class);
            bindService(bindIntent, mServiceConn, BIND_AUTO_CREATE);
            mIsBound = true;
        }
        
        // Setup listener to PlayerService
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(PlayerService.TRACK_STARTED);
        intentFilter.addAction(PlayerService.PLAYSTATE_CHANGE);
        intentFilter.addAction(PlayerService.TRACK_CHANGED);
        intentFilter.addAction(PlayerService.TRACK_ERROR);
        intentFilter.addAction(PlayerService.TRACK_BUFFERING);
        LocalBroadcastManager.getInstance(this).registerReceiver(mStatusListener, new IntentFilter(intentFilter));
    }

    @Override
    public void onStop() {
        Log.d(TAG, "onStop() ran");

        mIsActivityPaused = true;

        mHandler.removeMessages(REFRESH);
        
        if (mIsBound) {
            unbindService(mServiceConn);
            mIsBound = false;
        }
        
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mStatusListener);
        
        super.onStop();
    }

    @Override
    public void onPause() {
        Log.d(TAG, "onPause() ran");
        super.onPause();
    }
    
    @Override
    public void onResume() {
        Log.d(TAG, "onResume() ran");
        super.onResume();
        
        // updateTrackInfo();
        // setPlayButtonImage();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy() ran");

        // mAlbumArtWorker.quit();

        super.onDestroy();
    }

    protected void togglePlayPause() {

        if (mService == null || mService.getTrack() == null) {
            return;
        }
        
        if (mService.isPlaying()) {
            pause();
        }
        else {
            play();           
        } 
    }

    private void play() {
        
        mService.play();
        setPlayButtonImage();              
    }
    
    private void pause() {
        
        mService.pause();
        setPlayButtonImage();
    }
    
    protected void setPlayButtonImage() {
        
        if (mService != null && mService.isPlaying()) {
            mPlayButton.setImageResource(R.drawable.btn_pause);
        }
        else {
            mPlayButton.setImageResource(R.drawable.btn_play);
        }
    }

    protected void toggleShuffle() {
        // TODO Auto-generated method stub

    }

//    protected void setShuffleButtonImage() {
//
//        if (mIsShuffling) {
//            mShuffleButton.setImageResource(R.drawable.btn_shuffle);
//        }
//        else {
//            mShuffleButton.setImageResource(R.drawable.btn_shuffle_focused);
//        }
//    }

    protected void toggleRepeat() {
        // TODO Auto-generated method stub

    }

//    protected void setRepeatButtonImage() {
//
//        if (mIsRepeating) {
//            mRepeatButton.setImageResource(R.drawable.btn_repeat);
//        }
//        else {
//            mRepeatButton.setImageResource(R.drawable.btn_repeat_focused);
//        }
//    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.player_menu, menu);

        // Get the SearchView and set the searchable configuration
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) menu.findItem(R.id.menu_item_search).getActionView();
        
        // Assumes current activity is the searchable activity
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setIconifiedByDefault(false); // Do not iconify the widget; expand it by default
        
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        Intent intent;

        switch (item.getItemId()) {

        case R.id.menu_item_library:

            intent = new Intent(this, TabControllerActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);

            break;
        case R.id.menu_item_settings:

            intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);

            break;
            
        default:
            // No-op
            break;
        }

        return true;
    }
}
