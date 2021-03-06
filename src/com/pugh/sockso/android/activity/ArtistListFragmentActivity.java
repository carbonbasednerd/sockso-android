package com.pugh.sockso.android.activity;

import android.accounts.Account;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AlphabetIndexer;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SectionIndexer;
import android.widget.TextView;

import com.pugh.sockso.android.R;
import com.pugh.sockso.android.account.SocksoAccountAuthenticator;
import com.pugh.sockso.android.data.CoverArtFetcher;
import com.pugh.sockso.android.data.MusicManager;
import com.pugh.sockso.android.data.SocksoProvider;
import com.pugh.sockso.android.data.SocksoProvider.ArtistColumns;
import com.pugh.sockso.android.data.SocksoProvider.TrackColumns;

public class ArtistListFragmentActivity extends FragmentActivity {

    private static final String TAG = ArtistListFragmentActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FragmentManager fm = getSupportFragmentManager();

        // Create the list fragment and add it as our sole content.
        if (fm.findFragmentById(android.R.id.content) == null) {
            ArtistListFragment list = new ArtistListFragment();
            fm.beginTransaction().add(android.R.id.content, list).commit();
        }
    }

    // Utility class to store the View ID's retrieved from the layout only once for efficiency
    static class ArtistViewHolder {

        TextView artist;
        ImageView cover;
    }

    // Custom list view item (cover image | artist text)
    public static class ArtistCursorAdapter extends SimpleCursorAdapter implements SectionIndexer {

        private Context mContext;
        private int mLayout;
        private CoverArtFetcher mCoverFetcher;
        private SectionIndexer mAlphaIndexer;


        public ArtistCursorAdapter(Context context, int layout, Cursor cursor, String[] from, int[] to, int flags) {
            super(context, layout, cursor, from, to, flags);
            
            this.mContext = context;
            this.mLayout = layout;
            this.mCoverFetcher = new CoverArtFetcher(mContext);
            this.mCoverFetcher.setDimensions(115, 115);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            Log.d(TAG, "newView() ran");

            final LayoutInflater inflater = LayoutInflater.from(context);
            View view = inflater.inflate(mLayout, parent, false);

            ArtistViewHolder viewHolder = new ArtistViewHolder();

            viewHolder.artist = (TextView) view.findViewById(R.id.artist_name_id);
            viewHolder.cover = (ImageView) view.findViewById(R.id.artist_image_id);

            view.setTag(viewHolder);

            return view;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            Log.d(TAG, "bindView() ran");

            ArtistViewHolder viewHolder = (ArtistViewHolder) view.getTag();

            int artistIdCol = cursor.getColumnIndex(ArtistColumns.SERVER_ID);
            int artistId = cursor.getInt(artistIdCol);

            int artistNameCol = cursor.getColumnIndex(ArtistColumns.NAME);
            viewHolder.artist.setText(cursor.getString(artistNameCol));

            mCoverFetcher.loadCoverArtArtist(artistId, viewHolder.cover);
        }
        @Override
        public Cursor swapCursor(Cursor cursor) {
            // Create our indexer
            if (cursor != null) {
                mAlphaIndexer = new AlphabetIndexer(cursor, cursor.getColumnIndex(TrackColumns.NAME), 
                        " 0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ");
            }
            return super.swapCursor(cursor);
        }

        public int getPositionForSection(int section) {
            return mAlphaIndexer.getPositionForSection(section);
        }

        public int getSectionForPosition(int position) {
            return mAlphaIndexer.getSectionForPosition(position);
        }

        public Object[] getSections() {
            return mAlphaIndexer.getSections();
        }
        // @Override
        // TODO, this is for filtered searches
        // public Cursor runQueryOnBackgroundThread(CharSequence constraint) {}
    }

    public static class ArtistListFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor> {

        private final static String TAG = ArtistListFragment.class.getSimpleName();

        private static final int ARTIST_LIST_LOADER = 1;

        private ArtistCursorAdapter mAdapter;

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);

            String[] uiBindFrom = { ArtistColumns.NAME };
            int[] uiBindTo = { R.id.artist_name_id };

            mAdapter = new ArtistCursorAdapter(getActivity().getApplicationContext(), R.layout.artist_list_item, null,
                    uiBindFrom, uiBindTo, 0);

            setListAdapter(mAdapter);

            setEmptyText(getString(R.string.no_artists));
            
            // Start out with a progress indicator
            setListShown(false);

            getLoaderManager().initLoader(ARTIST_LIST_LOADER, null, this);
        }

        @Override
        public void onListItemClick(ListView l, View v, int position, long id) {
            Log.d(TAG, "onListItemClick(): Item clicked: " + id);
            
            Intent intent = new Intent(getActivity(), ArtistActivity.class);
            intent.putExtra(MusicManager.ARTIST, id);
             
            startActivity(intent);
        }

        @Override 
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            Log.d(TAG, "onCreateLoader() ran");

            String[] projection = { ArtistColumns._ID, ArtistColumns.SERVER_ID, ArtistColumns.NAME };
            Uri contentUri = Uri.parse(SocksoProvider.CONTENT_URI + "/" + ArtistColumns.TABLE_NAME);
            CursorLoader cursorLoader = new CursorLoader(getActivity(), contentUri, projection, null, null, ArtistColumns.FULL_NAME + " ASC");

            return cursorLoader;
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
            Log.d(TAG, "onLoadFinished: " + cursor.getCount());
            mAdapter.swapCursor(cursor);

            // Enable FastScrolling
            final ListView view = getListView();
            view.setScrollBarStyle(ListView.SCROLLBARS_INSIDE_OVERLAY);
            view.setFastScrollEnabled(true);
            
            Account account = SocksoAccountAuthenticator.getSocksoAccount(getActivity().getApplicationContext());

            if ( account != null ) {
                boolean isNewAccount = SocksoAccountAuthenticator.isNewAccount(account, getActivity().getApplicationContext());
                // Show the list once the initial sync finishes (indicated by setting isNewAccount = false)
                if ( ! isNewAccount ) {
                    setListShown(true);
                }
            }
        }

        @Override
        public void onLoaderReset(Loader<Cursor> arg0) {
            Log.d(TAG, "onLoaderReset() ran");
            mAdapter.swapCursor(null);
        }

    }
}