package com.android.music.adapters;

import android.annotation.ColorInt;
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
import android.support.annotation.Nullable;
import android.support.v7.graphics.Palette;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AlphabetIndexer;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.SectionIndexer;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.android.music.AlbumBrowserFragment;
import com.android.music.MediaPlaybackActivity;
import com.android.music.MusicAlphabetIndexer;
import com.android.music.MusicUtils;
import com.android.music.PaletteParser;
import com.android.music.R;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.bitmap.BitmapTransitionOptions;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;

import static com.android.music.AlbumBrowserFragment.mSub;
import static com.android.music.MusicUtils.Defs.ADD_TO_PLAYLIST;
import static com.android.music.MusicUtils.Defs.DELETE_ITEM;
import static com.android.music.MusicUtils.Defs.PLAY_SELECTION;

public class AlbumListAdapter extends SimpleCursorAdapter implements
        SectionIndexer {

    private final BitmapDrawable mDefaultAlbumIcon;
    private final Resources mResources;
    @SuppressWarnings("unused")
    private final StringBuilder mStringBuilder = new StringBuilder();
    private final String mUnknownAlbum;
    private final String mUnknownArtist;
    @SuppressWarnings("unused")
    private final String mAlbumSongSeparator;
    @SuppressWarnings("unused")
    private final Object[] mFormatArgs = new Object[1];
    @SuppressWarnings("unused")
    public Drawable mNowPlayingOverlay;
    public int mAlbumIdx;
    public int mArtistIdx;
    public int mAlbumArtIndex;
    public AlphabetIndexer mIndexer;
    public AlbumBrowserFragment mFragment;
    public AsyncQueryHandler mQueryHandler;
    public String mConstraint = null;
    public boolean mConstraintIsValid = false;
    public boolean mFastscroll = false;

    @SuppressWarnings("deprecation")
    public AlbumListAdapter(Context context, AlbumBrowserFragment fragment,
                            int layout, Cursor cursor, String[] from, int[] to) {
        super(context, layout, cursor, from, to);
        mQueryHandler = new QueryHandler(context.getContentResolver());
        mFragment = fragment;
        mUnknownAlbum = context.getString(R.string.unknown_album_name);
        mUnknownArtist = context.getString(R.string.unknown_artist_name);
        mAlbumSongSeparator = context
                .getString(R.string.albumsongseparator);
        Resources r = context.getResources();
        mNowPlayingOverlay = r.getDrawable(R.drawable.indicator_ic_mp_playing_list);
        Bitmap b = BitmapFactory.decodeResource(r, R.drawable.album_cover);
        mDefaultAlbumIcon = new BitmapDrawable(context.getResources(), b);
        // no filter or dither, it's a lot faster and we can't tell the
        // difference
        mDefaultAlbumIcon.setFilterBitmap(false);
        mDefaultAlbumIcon.setDither(false);
        getColumnIndices(cursor);
        mResources = context.getResources();
    }

    private void getColumnIndices(Cursor cursor) {
        if (cursor != null) {
            mAlbumIdx = cursor
                    .getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM);
            mArtistIdx = cursor
                    .getColumnIndexOrThrow(MediaStore.Audio.Albums.ARTIST);
            mAlbumArtIndex = cursor
                    .getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM_ART);

            if (mIndexer != null) {
                mIndexer.setCursor(cursor);
            } else {
                mIndexer = new MusicAlphabetIndexer(cursor, mAlbumIdx,
                        mFragment.getParentActivity().getResources()
                                .getString(R.string.fast_scroll_alphabet));
            }
        }
    }

    public void setFragment(AlbumBrowserFragment newFragment) {
        mFragment = newFragment;
    }

    public AsyncQueryHandler getQueryHandler() {
        return mQueryHandler;
    }

    @SuppressWarnings("deprecation")
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View v = super.newView(context, cursor, parent);
        ViewHolder vh = new ViewHolder();
        vh.line1 = v.findViewById(R.id.line1);
        vh.line2 = v.findViewById(R.id.line2);
        vh.popup_menu_button = v.findViewById(R.id.list_menu_button);
        ((MediaPlaybackActivity) mFragment.getParentActivity()).setTouchDelegate(vh.popup_menu_button);
        vh.layout = v.findViewById(R.id.album_background);
        vh.icon = v.findViewById(R.id.icon);
        vh.icon.setBackgroundDrawable(mDefaultAlbumIcon);
        vh.icon.setPadding(0, 0, 0, 0);
        v.setTag(vh);
        return v;
    }

    @Override
    public void bindView(View view, Context context, final Cursor cursor) {
        final ViewHolder vh = (ViewHolder) view.getTag();
        vh.albumID = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums._ID));
        String name = cursor.getString(mAlbumIdx);
        String displayname = name;
        vh.mCurrentAlbumName = displayname;
        vh.albumName = name;
        boolean unknown = name == null
                || name.equals(MediaStore.UNKNOWN_STRING);
        if (unknown) {
            displayname = mUnknownAlbum;
        }
        vh.line1.setText(displayname);
        name = cursor.getString(mArtistIdx);
        vh.artistNameForAlbum = name;
        displayname = name;
        if (name == null || name.equals(MediaStore.UNKNOWN_STRING)) {
            displayname = mUnknownArtist;
        }
        vh.line2.setText(displayname);
        ImageView iv = vh.icon;
        // We don't actually need the path to the thumbnail file,
        // we just use it to see if there is album art or not
        String art = cursor.getString(mAlbumArtIndex);
        Bitmap[] albumArt;
        if (art != null) {
            albumArt = MusicUtils.mAlbumArtCache.get(art);
        } else {
            albumArt = null;
        }
        if (mFastscroll || unknown || art == null || art.length() == 0) {
            iv.setImageDrawable(null);
            iv.setTag(null);
        } else {
            if (albumArt == null || albumArt[0] == null) {
                Glide
                        .with(iv.getContext())
                        .asBitmap()
                        .load(art)
                        .apply(new RequestOptions()
                                .centerCrop()
                                .placeholder(R.color.colorTransparent)
                                .diskCacheStrategy(DiskCacheStrategy.ALL))
                        .transition(new BitmapTransitionOptions().crossFade())
                        .listener(new RequestListener<Bitmap>() {
                            @Override
                            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Bitmap> target, boolean isFirstResource) {
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(Bitmap resource, Object model, Target<Bitmap> target, DataSource dataSource, boolean isFirstResource) {
                                getPalette(resource, vh);
                                return false;
                            }
                        })
                        .into(iv);
            } else {
                iv.setImageBitmap(albumArt[0]);
            }
        }

        view.setOnClickListener(v -> mFragment.enterAlbum(Long.valueOf(vh.albumID)));

        vh.popup_menu_button.setOnClickListener(v -> {
            if (mFragment.mPopupMenu != null) {
                mFragment.mPopupMenu.dismiss();
            }
            mFragment.mCurrentAlbumName = vh.mCurrentAlbumName;
            PopupMenu popup = new PopupMenu(mFragment
                    .getParentActivity(), vh.popup_menu_button);
            popup.getMenu().add(0, PLAY_SELECTION, 0,
                    R.string.play_selection);
            mSub = popup.getMenu().addSubMenu(0,
                    ADD_TO_PLAYLIST, 0, R.string.add_to_playlist);
            MusicUtils.makePlaylistMenu(mFragment.getParentActivity(),
                    mSub);
            popup.getMenu()
                    .add(0, DELETE_ITEM, 0, R.string.delete_item);
            popup.show();
            popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {

                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    // TODO Auto-generated method stub
                    mFragment.onContextItemSelected(item);
                    return true;
                }
            });
            mFragment.mPopupMenu = popup;
            mFragment.mCurrentAlbumId = vh.albumID;
            mFragment.mCurrentAlbumName = vh.albumName;
            mFragment.mCurrentArtistNameForAlbum = vh.artistNameForAlbum;
            mFragment.mIsUnknownArtist = mFragment.mCurrentArtistNameForAlbum == null
                    || mFragment.mCurrentArtistNameForAlbum
                    .equals(MediaStore.UNKNOWN_STRING);
            mFragment.mIsUnknownAlbum = mFragment.mCurrentAlbumName == null
                    || mFragment.mCurrentAlbumName
                    .equals(MediaStore.UNKNOWN_STRING);
        });
    }

    private void getPalette(Bitmap bitmap, ViewHolder vh) {
        Palette.from(bitmap).generate(palette -> {
            if (palette != null)
                paintEmAll(palette, vh);
        });
    }

    private void paintEmAll(Palette palette, ViewHolder vh) {
        PaletteParser mParser = new PaletteParser(palette);
        @ColorInt int colorPrimary = mParser.getColorPrimary();
        @ColorInt int colorTextPrimary = mParser.getColorTextPrimary();
        @ColorInt int colorTextSecondary = mParser.getColorTextSecondary();
        @ColorInt int colorControlActivated = mParser.getColorControlActivated();
        vh.layout.setBackgroundColor(colorPrimary);
        vh.line1.setTextColor(colorTextPrimary);
        vh.line2.setTextColor(colorTextSecondary);
        vh.popup_menu_button.setColorFilter(colorControlActivated);
    }

    @Override
    public void changeCursor(Cursor cursor) {
        if (mFragment.getParentActivity().isFinishing() && cursor != null) {
            cursor.close();
            cursor = null;
        }
        if (cursor != mFragment.mAlbumCursor) {
            if (mFragment.mAlbumCursor != null) {
                mFragment.mAlbumCursor.close();
                mFragment.mAlbumCursor = null;
            }
            mFragment.mAlbumCursor = cursor;
            if (cursor == null || !cursor.isClosed()) {
                getColumnIndices(cursor);
                super.changeCursor(cursor);
            }
        }
    }

    @Override
    public Cursor runQueryOnBackgroundThread(CharSequence constraint) {
        String s = constraint.toString();
        if (mConstraintIsValid
                && ((s == null && mConstraint == null) || (s != null && s
                .equals(mConstraint)))) {
            return getCursor();
        }
        Cursor c = mFragment.getAlbumCursor(null, s);
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

    static class ViewHolder {
        TextView line1;
        TextView line2;
        ImageView popup_menu_button;
        ImageView icon;
        RelativeLayout layout;
        String albumID;
        String albumName;
        String artistNameForAlbum, mCurrentAlbumName;
    }

    class QueryHandler extends AsyncQueryHandler {
        QueryHandler(ContentResolver res) {
            super(res);
        }

        @Override
        protected void onQueryComplete(int token, Object cookie,
                                       Cursor cursor) {
            mFragment.init(cursor);
        }
    }
}
