package com.android.music.adapters;

import android.annotation.SuppressLint;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.provider.MediaStore;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AlphabetIndexer;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.SectionIndexer;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.android.music.FolderBrowserFragment;
import com.android.music.MusicAlphabetIndexer;
import com.android.music.MusicUtils;
import com.android.music.R;

import static com.android.music.FolderBrowserFragment.getRootPath;
import static com.android.music.FolderBrowserFragment.mCurrentParent;
import static com.android.music.FolderBrowserFragment.mCurrentPath;
import static com.android.music.FolderBrowserFragment.mFilesCursor;
import static com.android.music.FolderBrowserFragment.mSub;
import static com.android.music.MusicUtils.Defs.ADD_TO_PLAYLIST;
import static com.android.music.MusicUtils.Defs.PLAY_SELECTION;

public class FolderListAdapter extends SimpleCursorAdapter implements SectionIndexer {

    private final BitmapDrawable mDefaultAlbumIcon;
    private final String mUnknownPath;
    private final String mUnknownCount;
    private final Object[] mFormatArgs = new Object[1];
    private Resources mResources;
    private Drawable mNowPlaying;
    private int mDataIdx;
    private int mCountIdx;
    private AlphabetIndexer mIndexer;
    private FolderBrowserFragment mFragment;
    private AsyncQueryHandler mQueryHandler;
    private String mConstraint = null;
    private boolean mConstraintIsValid = false;

    public FolderListAdapter(Context context, FolderBrowserFragment currentfragment,
                             int layout, Cursor cursor, String[] from, int[] to) {
        super(context, layout, cursor, from, to);

        mFragment = currentfragment;
        mQueryHandler = new QueryHandler(context.getContentResolver());
        mUnknownPath = context.getString(R.string.unknown_folder_name);
        mUnknownCount = context.getString(R.string.unknown_folder_count);
        Resources r = context.getResources();
        mNowPlaying = r.getDrawable(R.drawable.indicator_ic_mp_playing_list);
        Bitmap b = BitmapFactory.decodeResource(r, R.drawable.album_cover_background);
        mDefaultAlbumIcon = new BitmapDrawable(context.getResources(), b);
        mDefaultAlbumIcon.setFilterBitmap(false);
        mDefaultAlbumIcon.setDither(false);
        getColumnIndices(cursor);
        mResources = context.getResources();
    }

    private void getColumnIndices(Cursor cursor) {
        if (cursor != null) {
            mDataIdx = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA);
            mCountIdx = cursor.getColumnIndexOrThrow("count(*)");
            if (mIndexer != null) {
                mIndexer.setCursor(cursor);
            } else {
                if (mResources == null)
                    mResources = mFragment.getResources();
                mIndexer = new MusicAlphabetIndexer(cursor, mDataIdx, mResources.getString(
                        R.string.fast_scroll_alphabet));
            }
        }
    }

    public void setActivity(FolderBrowserFragment newfragment) {
        mFragment = newfragment;
    }

    public AsyncQueryHandler getQueryHandler() {
        return mQueryHandler;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View v = super.newView(context, cursor, parent);
        ViewHolder vh = new ViewHolder();
        vh.line1 = v.findViewById(R.id.line1);
        vh.line2 = v.findViewById(R.id.line2);
        vh.play_indicator = v.findViewById(R.id.play_indicator);
        View animView = v.findViewById(R.id.animView);
        animView.setVisibility(View.GONE);
        //vh.icon = (ImageView) v.findViewById(R.id.icon);
        //vh.icon.setBackgroundDrawable(mDefaultAlbumIcon);
        //vh.icon.setPadding(0, 0, 1, 0);
        v.setTag(vh);
        return v;
    }

    @Override
    public void bindView(View view, Context context, final Cursor cursor) {
        final ViewHolder vh = (ViewHolder) view.getTag();

        String path = cursor.getString(mDataIdx);
        String rootPath = getRootPath(path);
        String name;
        if (path == null || path.trim().equals("")) {
            name = mUnknownPath;
        } else {
            String[] split = rootPath.split("/");
            name = split[split.length - 1];
        }
        vh.line1.setText(name);

        String numString = mUnknownCount;
        int count = cursor.getInt(mCountIdx);
        if (count == 1) {
            numString = context.getString(R.string.onesong);
        } else {
            final Object[] args = mFormatArgs;
            args[0] = count;
            numString = mResources.getQuantityString(R.plurals.Nsongs, count, args);
        }

        final int p = cursor.getPosition();
        vh.line2.setText(numString);

        vh.play_indicator.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // mFragment.mCurrentAlbumName = vh.mCurrentAlbumName;
                if (mFragment.mPopupMenu != null) {
                    mFragment.mPopupMenu.dismiss();
                }
                PopupMenu popup = new PopupMenu(mFragment
                        .getParentActivity(), vh.play_indicator);
                popup.getMenu().add(0, PLAY_SELECTION, 0,
                        R.string.play_selection);
                mSub = popup.getMenu().addSubMenu(0,
                        ADD_TO_PLAYLIST, 0, R.string.add_to_playlist);
                MusicUtils.makePlaylistMenu(mFragment.getParentActivity(),
                        mSub);

                popup.show();
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {

                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        mFragment.onContextItemSelected(item);
                        return true;
                    }
                });
                mFragment.mPopupMenu = popup;

                mFilesCursor.moveToPosition(p);
                mCurrentParent = mFilesCursor.getString(mFilesCursor
                        .getColumnIndexOrThrow(MediaStore.Files.FileColumns.PARENT));
                mCurrentPath = mFilesCursor.getString(mFilesCursor
                        .getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA));
            }
        });
    }

    @Override
    public void changeCursor(Cursor cursor) {
        if (mFragment.getParentActivity() != null
                && mFragment.getParentActivity().isFinishing()
                && cursor != null) {
            cursor.close();
            cursor = null;
        }
        if (cursor != mFilesCursor) {
            if (mFilesCursor != null) {
                mFilesCursor.close();
                mFilesCursor = null;
            }
            mFilesCursor = cursor;
            if (cursor == null || !cursor.isClosed()) {
                getColumnIndices(cursor);
                super.changeCursor(cursor);
            }
        }
    }

    @Override
    public Cursor runQueryOnBackgroundThread(CharSequence constraint) {
        String s = constraint.toString();
        if (mConstraintIsValid && s.equals(mConstraint)) {
            return getCursor();
        }
        Cursor c = mFragment.getFolderCursor(null, s);
        mConstraint = s;
        mConstraintIsValid = true;
        return c;
    }

    public Object[] getSections() {
        return mIndexer.getSections();
    }

    public int getPositionForSection(int section) {
        return mIndexer.getPositionForSection(section);
    }

    public int getSectionForPosition(int position) {
        return 0;
    }

    class ViewHolder {
        TextView line1;
        TextView line2;
        ImageView play_indicator;
        ImageView icon;
    }

    @SuppressLint("HandlerLeak")
    class QueryHandler extends AsyncQueryHandler {
        QueryHandler(ContentResolver res) {
            super(res);
        }

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
            mFragment.init(cursor);
        }
    }
}