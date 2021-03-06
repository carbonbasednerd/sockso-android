package com.pugh.sockso.android.data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.json.JSONException;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import com.pugh.sockso.android.ServerFactory;
import com.pugh.sockso.android.api.SocksoAPI;
import com.pugh.sockso.android.api.SocksoAPIImpl;
import com.pugh.sockso.android.data.SocksoProvider.AlbumColumns;
import com.pugh.sockso.android.data.SocksoProvider.ArtistColumns;
import com.pugh.sockso.android.data.SocksoProvider.GenreColumns;
import com.pugh.sockso.android.data.SocksoProvider.TrackColumns;
import com.pugh.sockso.android.music.Album;
import com.pugh.sockso.android.music.Artist;
import com.pugh.sockso.android.music.Genre;
import com.pugh.sockso.android.music.Track;

public class MusicManager {

    private static final String TAG = MusicManager.class.getSimpleName();
    
    private static final int BATCH_MAX = 100;
    
    public static final String ALBUM  = "album_id";
    public static final String ARTIST = "artist_id";
    public static final String TRACK  = "track_id";
    public static final String GENRE  = "genre_id";
    
    public static long syncLibrary(final Context context, long syncMarker) throws IOException, JSONException {
        Log.d(TAG, "syncLibrary() ran");

        SocksoAPI socksoAPI = new SocksoAPIImpl(ServerFactory.getServer(context));

        Date syncDate = new Date(syncMarker);
        
        // Grab data from server
        List<Artist> artists = socksoAPI.getArtists(syncDate);
        List<Genre>  genres  = socksoAPI.getGenres(syncDate);
        List<Album>  albums  = socksoAPI.getAlbums(syncDate);
        List<Track>  tracks  = socksoAPI.getTracks(syncDate);
        
        final ContentResolver resolver = context.getContentResolver();
        
        long newSyncMarker = System.currentTimeMillis();
        
        syncArtists(artists, resolver);
        syncAlbums(albums, resolver);
        syncTracks(tracks, resolver);
        syncGenres(genres, resolver);
        
        return newSyncMarker;
    }

    private static void syncArtists(List<Artist> artists, ContentResolver resolver) {
        Log.d(TAG, "syncArtists() ran");

        final Uri uri = Uri.parse(SocksoProvider.CONTENT_URI + "/" + ArtistColumns.TABLE_NAME);
        final BatchOperation batchOperation = new BatchOperation(uri, resolver);

        for (final Artist artist : artists) {
            addArtist(artist, batchOperation);

            if (batchOperation.size() >= BATCH_MAX) {
                Log.d(TAG, "syncArtists(): " + BATCH_MAX + " batched. Executing current batch...");
                batchOperation.execute();
            }
        }

        // add the remaining
        if (batchOperation.size() >= 0) {
            batchOperation.execute();
        }
    }

    private static void syncAlbums(List<Album> albums, ContentResolver resolver) {
        Log.d(TAG, "syncAlbums() ran");
        
        final Uri uri = Uri.parse(SocksoProvider.CONTENT_URI + "/" + AlbumColumns.TABLE_NAME);
        final BatchOperation batchOperation = new BatchOperation(uri, resolver);

        for (final Album album : albums) {
            addAlbum(album, batchOperation);

            if (batchOperation.size() >= BATCH_MAX) {
                Log.d(TAG, "syncAlbums(): " + BATCH_MAX + " batched. Executing current batch...");
                batchOperation.execute();
            }
        }

        // add the remaining
        if (batchOperation.size() >= 0) {
            batchOperation.execute();
        }
    }

    private static void syncTracks(List<Track> tracks, ContentResolver resolver) {
        Log.d(TAG, "syncTracks() ran");
        
        final Uri uri = Uri.parse(SocksoProvider.CONTENT_URI + "/" + TrackColumns.TABLE_NAME);
        final BatchOperation batchOperation = new BatchOperation(uri, resolver);

        for (final Track track : tracks) {
            addTrack(track, batchOperation);

            if (batchOperation.size() >= BATCH_MAX) {
                Log.d(TAG, "syncTracks(): " + BATCH_MAX + " batched. Executing current batch...");
                batchOperation.execute();
            }
        }

        // add the remaining
        if (batchOperation.size() >= 0) {
            batchOperation.execute();
        }
    }


    private static void syncGenres(List<Genre> genres, ContentResolver resolver) {
        Log.d(TAG, "syncGenres() ran");
        
        final Uri uri = Uri.parse(SocksoProvider.CONTENT_URI + "/" + GenreColumns.TABLE_NAME);
        final BatchOperation batchOperation = new BatchOperation(uri, resolver);

        for (final Genre genre : genres) {
            addGenre(genre, batchOperation);

            if (batchOperation.size() >= BATCH_MAX) {
                Log.d(TAG, "syncGenres(): " + BATCH_MAX + " batched. Executing current batch...");
                batchOperation.execute();
            }
        }

        // add the remaining
        if (batchOperation.size() >= 0) {
            batchOperation.execute();
        }
    }
    
    private static void addArtist(Artist artist, BatchOperation batchOperation) {
        Log.d(TAG, "addArtist() ran");

        ContentValues contentValues = new ContentValues();

        contentValues.put(ArtistColumns.SERVER_ID, artist.getServerId());
        contentValues.put(ArtistColumns.NAME, artist.getName());

        batchOperation.add(contentValues);
    }

    private static void addAlbum(Album album, BatchOperation batchOperation) {
        Log.d(TAG, "addAlbum() ran");

        ContentValues contentValues = new ContentValues();

        contentValues.put(AlbumColumns.SERVER_ID, album.getServerId());
        contentValues.put(AlbumColumns.NAME, album.getName());
        contentValues.put(AlbumColumns.ARTIST_ID, album.getArtistId());
        // TODO .AlbumColumns.YEAR;

        batchOperation.add(contentValues);
    }

    private static void addTrack(Track track, BatchOperation batchOperation) {
        Log.d(TAG, "addTrack() ran");

        ContentValues contentValues = new ContentValues();

        contentValues.put(TrackColumns.SERVER_ID, track.getServerId());
        contentValues.put(TrackColumns.NAME, track.getName());
        contentValues.put(TrackColumns.TRACK_NO, track.getTrackNumber());
        contentValues.put(TrackColumns.ARTIST_ID, track.getArtistId());
        contentValues.put(TrackColumns.ALBUM_ID, track.getAlbumId());
        contentValues.put(TrackColumns.GENRE_ID, track.getGenreId());

        batchOperation.add(contentValues);
    }

    private static void addGenre(Genre genre, BatchOperation batchOperation) {
        Log.d(TAG, "addGenre() ran");

        ContentValues contentValues = new ContentValues();

        contentValues.put(GenreColumns.SERVER_ID, genre.getServerId());
        contentValues.put(GenreColumns.NAME, genre.getName());

        batchOperation.add(contentValues);
    }

    
    public static Track getTrack( final ContentResolver contentResolver, long trackId ) {
        Log.d(TAG, "getTrack() called");
        
        Track track = null;
        
        String[] projection = { 
                TrackColumns.SERVER_ID, 
                TrackColumns.ARTIST_NAME, 
                TrackColumns.NAME,
                TrackColumns.TRACK_NO
                };
        Uri uri = Uri.parse(SocksoProvider.CONTENT_URI + "/" + TrackColumns.TABLE_NAME + "/" + trackId);
        
        Cursor cursor = contentResolver.query(uri, projection, null, null, null);
        
        // TODO check return value (if false, return null)
        cursor.moveToNext();
        
        long serverTrackId = cursor.getLong(0);
        String artistName = cursor.getString(1);
        String trackName  = cursor.getString(2);
        int trackNumber   = cursor.getInt(3);
        
        cursor.close();
        
        Log.d(TAG, "serverTrackId: " + serverTrackId);
        
        track = new Track();
        track.setId(trackId);
        track.setServerId(serverTrackId);
        track.setName(trackName);
        track.setArtist(artistName);
        track.setTrackNumber(trackNumber);
        
        return track;
    }

    public static Album getAlbum( final ContentResolver contentResolver, long albumId ) {
        Log.d(TAG, "getAlbum() called");

        Album album = null;

        String[] projection = { 
                AlbumColumns.SERVER_ID, 
                AlbumColumns.ARTIST_NAME, 
                AlbumColumns.NAME 
                };
        Uri uri = Uri.parse(SocksoProvider.CONTENT_URI + "/" + AlbumColumns.TABLE_NAME + "/" + albumId);

        Cursor cursor = contentResolver.query(uri, projection, null, null, null);

        cursor.moveToNext();

        long serverAlbumId = cursor.getLong(0);
        String artistName = cursor.getString(1);
        String trackName = cursor.getString(2);

        cursor.close();
        
        Log.d(TAG, "serverAlbumId: " + serverAlbumId);

        album = new Album();
        album.setId(albumId);
        album.setServerId(serverAlbumId);
        album.setName(trackName);
        album.setArtist(artistName);

        return album;
    }

    public static List<Track> getTrackForAlbum(ContentResolver contentResolver, long albumId) {       
        Log.d(TAG, "getTrackForAlbum() called");
        
        List<Track> tracks = new ArrayList<Track>(10);
        
        String[] projection = {
                TrackColumns._ID,
                TrackColumns.SERVER_ID, 
                TrackColumns.ARTIST_NAME,
                TrackColumns.ALBUM_NAME,
                TrackColumns.NAME,
                TrackColumns.TRACK_NO
                };
        Uri uri = Uri.parse(SocksoProvider.CONTENT_URI + "/" + AlbumColumns.TABLE_NAME + "/" + albumId
                + "/" + TrackColumns.TABLE_NAME);        
        Cursor cursor = contentResolver.query(uri, projection, null, null,  TrackColumns.TRACK_NO + " ASC");
        
        while (cursor.moveToNext()) {

            long trackId = cursor.getLong(0);
            long serverTrackId = cursor.getLong(1);
            String artistName = cursor.getString(2);
            String albumName = cursor.getString(3);
            String trackName = cursor.getString(4);
            int trackNumber = cursor.getInt(5);

            Log.d(TAG, "serverTrackId: " + serverTrackId);

            Track track = new Track();
            track.setId(trackId);
            track.setServerId(serverTrackId);
            track.setName(trackName);
            track.setArtist(artistName);
            track.setAlbum(albumName);
            track.setTrackNumber(trackNumber);
            
            tracks.add(track);
        }
        
        cursor.close();
        
        return tracks;
    }

    public static Artist getArtist(ContentResolver contentResolver, long artistId) {
        Log.d(TAG, "getArtist() called");

        Artist artist = null;

        String[] projection = { 
                ArtistColumns.SERVER_ID, 
                ArtistColumns.NAME         
                };
        Uri uri = Uri.parse(SocksoProvider.CONTENT_URI + "/" + ArtistColumns.TABLE_NAME + "/" + artistId);

        Cursor cursor = contentResolver.query(uri, projection, null, null, null);

        cursor.moveToNext();

        long serverArtistId = cursor.getLong(0);
        String artistName = cursor.getString(1);

        cursor.close();
        
        Log.d(TAG, "serverArtistId: " + serverArtistId);

        artist = new Artist();
        artist.setId(artistId);
        artist.setServerId(serverArtistId);
        artist.setName(artistName);

        return artist;
    }
    
}
