/*
 * Copyright (C) 2014 Michell Bak
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.miz.mizuu.fragments;

import static com.miz.functions.PreferenceKeys.DISABLE_ETHERNET_WIFI_CHECK;
import static com.miz.functions.PreferenceKeys.GRID_ITEM_SIZE;
import static com.miz.functions.PreferenceKeys.IGNORED_NFO_FILES;
import static com.miz.functions.PreferenceKeys.IGNORED_TITLE_PREFIXES;
import static com.miz.functions.PreferenceKeys.SHOW_TITLES_IN_GRID;
import static com.miz.functions.PreferenceKeys.SORTING_COLLECTIONS_OVERVIEW;
import static com.miz.functions.PreferenceKeys.SORTING_MOVIES;

import static com.miz.functions.SortingKeys.DATE;
import static com.miz.functions.SortingKeys.DURATION;
import static com.miz.functions.SortingKeys.RATING;
import static com.miz.functions.SortingKeys.RELEASE;
import static com.miz.functions.SortingKeys.TITLE;
import static com.miz.functions.SortingKeys.WEIGHTED_RATING;
import static com.miz.functions.SortingKeys.ALL_MOVIES;
import static com.miz.functions.SortingKeys.AVAILABLE_FILES;
import static com.miz.functions.SortingKeys.COLLECTIONS;
import static com.miz.functions.SortingKeys.FAVORITES;
import static com.miz.functions.SortingKeys.OFFLINE_COPIES;
import static com.miz.functions.SortingKeys.UNIDENTIFIED_MOVIES;
import static com.miz.functions.SortingKeys.UNWATCHED_MOVIES;
import static com.miz.functions.SortingKeys.WATCHED_MOVIES;
import static com.miz.functions.SortingKeys.GENRES;
import static com.miz.functions.SortingKeys.CERTIFICATION;
import static com.miz.functions.SortingKeys.FILE_SOURCES;
import static com.miz.functions.SortingKeys.FOLDERS;

import java.io.File;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.TreeMap;
import java.util.regex.Pattern;

import jcifs.smb.SmbFile;
import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.ActionBar.OnNavigationListener;
import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.graphics.Bitmap.Config;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.SearchView.OnQueryTextListener;
import android.widget.TextView;

import com.miz.db.DbAdapter;
import com.miz.db.DbAdapterSources;
import com.miz.db.DbHelper;
import com.miz.functions.AsyncTask;
import com.miz.functions.CoverItem;
import com.miz.functions.FileSource;
import com.miz.functions.MediumMovie;
import com.miz.functions.MizLib;
import com.miz.functions.MovieSectionAsyncTask;
import com.miz.functions.MovieSortHelper;
import com.miz.functions.SQLiteCursorLoader;
import com.miz.functions.SpinnerItem;
import com.miz.mizuu.Main;
import com.miz.mizuu.MizuuApplication;
import com.miz.mizuu.MovieCollection;
import com.miz.mizuu.MovieDetails;
import com.miz.mizuu.Preferences;
import com.miz.mizuu.R;
import com.miz.mizuu.Update;
import com.squareup.picasso.Picasso;

@SuppressLint("NewApi")
public class MovieLibraryFragment extends Fragment implements OnNavigationListener, OnSharedPreferenceChangeListener {

	public static final int MAIN = 0, OTHER = 1;

	private SharedPreferences mSharedPreferences;
	private int mImageThumbSize, mImageThumbSpacing, mType, mResizedWidth, mResizedHeight, mCurrentSort;
	private LoaderAdapter mAdapter;
	private ArrayList<MediumMovie> mMovies = new ArrayList<MediumMovie>();
	private ArrayList<Integer> mMovieKeys = new ArrayList<Integer>();
	private GridView mGridView = null;
	private ProgressBar mProgressBar;
	private boolean mIgnorePrefixes, mDisableEthernetWiFiCheck, mIgnoreNfo, mLoading, mShowTitles;
	private ActionBar mActionBar;
	private ArrayList<SpinnerItem> mSpinnerItems = new ArrayList<SpinnerItem>();
	private ActionBarSpinner mSpinnerAdapter;
	private Picasso mPicasso;
	private Config mConfig;
	private MovieSectionLoader mMovieSectionLoader;
	private SearchTask mSearch;
	private View mEmptyLibraryLayout;
	private ImageView mEmptyLibraryIcon;
	private TextView mEmptyLibraryTitle, mEmptyLibraryDescription;

	/**
	 * Empty constructor as per the Fragment documentation
	 */
	public MovieLibraryFragment() {}

	public static MovieLibraryFragment newInstance(int type) {
		MovieLibraryFragment frag = new MovieLibraryFragment();
		Bundle b = new Bundle();
		b.putInt("type", type);
		frag.setArguments(b);		
		return frag;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mType = getArguments().getInt("type");

		setRetainInstance(true);
		setHasOptionsMenu(true);

		setupSpinnerItems();

		// Set OnSharedPreferenceChange listener
		PreferenceManager.getDefaultSharedPreferences(getActivity()).registerOnSharedPreferenceChangeListener(this);

		// Initialize the PreferenceManager variable and preference variable(s)
		mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());

		mIgnorePrefixes = mSharedPreferences.getBoolean(IGNORED_TITLE_PREFIXES, false);
		mIgnoreNfo = mSharedPreferences.getBoolean(IGNORED_NFO_FILES, true);
		mDisableEthernetWiFiCheck = mSharedPreferences.getBoolean(DISABLE_ETHERNET_WIFI_CHECK, false);
		mShowTitles = mSharedPreferences.getBoolean(SHOW_TITLES_IN_GRID, true);

		String thumbnailSize = mSharedPreferences.getString(GRID_ITEM_SIZE, getString(R.string.normal));
		if (thumbnailSize.equals(getString(R.string.large))) 
			mImageThumbSize = (int) (getResources().getDimensionPixelSize(R.dimen.image_thumbnail_size) * 1.33);
		else if (thumbnailSize.equals(getString(R.string.normal))) 
			mImageThumbSize = (int) (getResources().getDimensionPixelSize(R.dimen.image_thumbnail_size) * 1);
		else
			mImageThumbSize = (int) (getResources().getDimensionPixelSize(R.dimen.image_thumbnail_size) * 0.75);
		mImageThumbSpacing = getResources().getDimensionPixelSize(R.dimen.image_thumbnail_spacing);

		mPicasso = MizuuApplication.getPicasso(getActivity());
		mConfig = MizuuApplication.getBitmapConfig();

		mAdapter = new LoaderAdapter(getActivity());
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		// Setup ActionBar with the action list
		setupActionBar();
		if (mActionBar.getNavigationMode() == ActionBar.NAVIGATION_MODE_LIST)
			mActionBar.setListNavigationCallbacks(mSpinnerAdapter, this);

		LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mMessageReceiver, new IntentFilter("mizuu-movies-update"));
		LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mMessageReceiver, new IntentFilter("mizuu-library-change"));
		LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mMessageReceiver, new IntentFilter("mizuu-movie-cover-change"));
		LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mMessageReceiver, new IntentFilter("mizuu-movie-actor-search"));
	}

	private void setupActionBar() {
		mActionBar = getActivity().getActionBar();
		mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
		if (mSpinnerAdapter == null)
			mSpinnerAdapter = new ActionBarSpinner();
	}

	private void setupSpinnerItems() {
		mSpinnerItems.clear();
		if (mType == MAIN)
			mSpinnerItems.add(new SpinnerItem(getString(R.string.chooserMovies), getString(R.string.choiceAllMovies)));
		else
			mSpinnerItems.add(new SpinnerItem(getString(R.string.chooserWatchList), getString(R.string.choiceAllMovies)));
		mSpinnerItems.add(new SpinnerItem(getString(R.string.choiceFavorites), getString(R.string.choiceFavorites)));
		mSpinnerItems.add(new SpinnerItem(getString(R.string.choiceAvailableFiles), getString(R.string.choiceAvailableFiles)));
		mSpinnerItems.add(new SpinnerItem(getString(R.string.choiceCollections), getString(R.string.choiceCollections)));
		mSpinnerItems.add(new SpinnerItem(getString(R.string.choiceWatchedMovies), getString(R.string.choiceWatchedMovies)));
		mSpinnerItems.add(new SpinnerItem(getString(R.string.choiceUnwatchedMovies), getString(R.string.choiceUnwatchedMovies)));
		mSpinnerItems.add(new SpinnerItem(getString(R.string.choiceUnidentifiedMovies), getString(R.string.choiceUnidentifiedMovies)));
		mSpinnerItems.add(new SpinnerItem(getString(R.string.choiceOffline), getString(R.string.choiceOffline)));
	}

	private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.filterEquals(new Intent("mizuu-movie-actor-search"))) {
				search("actor: " + intent.getStringExtra("intent_extra_data_key"));
			} else {
				if (intent.filterEquals(new Intent("mizuu-library-change")) || intent.filterEquals(new Intent("mizuu-movie-cover-change"))) {
					clearCaches();
				}

				forceLoaderLoad();
			}
		}
	};

	LoaderCallbacks<Cursor> loaderCallbacks = new LoaderCallbacks<Cursor>() {
		@Override
		public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
			mLoading = true;
			return new SQLiteCursorLoader(getActivity(), DbHelper.getHelper(getActivity()), DbAdapter.DATABASE_TABLE, DbAdapter.SELECT_ALL, "NOT(" + DbAdapter.KEY_TITLE + " = 'MIZ_REMOVED_MOVIE')", null, "(CASE WHEN " + DbAdapter.KEY_TMDBID + " = 'invalid' OR " + DbAdapter.KEY_TMDBID + " = '' THEN " + DbAdapter.KEY_FILEPATH + " ELSE " + DbAdapter.KEY_TMDBID + " END)", null, DbAdapter.KEY_TITLE + " ASC");
		}

		@Override
		public void onLoadFinished(Loader<Cursor> arg0, final Cursor cursor) {			
			new AsyncTask<Void, Void, Void>() {
				@Override
				protected void onPreExecute() {
					mMovies.clear();
				}

				@Override
				protected Void doInBackground(Void... params) {
					try {
						while (cursor.moveToNext()) {
							if (mType == OTHER && cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_TO_WATCH)).equals("0"))
								continue;

							mMovies.add(new MediumMovie(getActivity(),
									cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_ROWID)),
									cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_FILEPATH)),
									cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_TITLE)),
									cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_TMDBID)),
									cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_RATING)),
									cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_RELEASEDATE)),
									cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_GENRES)),
									cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_FAVOURITE)),
									cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_CAST)),
									cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_COLLECTION)),
									cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_EXTRA_2)),
									cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_TO_WATCH)),
									cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_HAS_WATCHED)),
									cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_EXTRA_1)),
									cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_CERTIFICATION)),
									cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_RUNTIME)),
									mIgnorePrefixes,
									mIgnoreNfo
									));
						}
					} catch (Exception e) {
					} finally {
						cursor.close();
					}

					mMovieKeys.clear();

					for (int i = 0; i < mMovies.size(); i++) {
						if (mType == OTHER)
							if (!mMovies.get(i).toWatch())
								continue;
						mMovieKeys.add(i);
					}

					return null;
				}

				@Override
				protected void onPostExecute(Void result) {
					showMovieSection(mActionBar.getSelectedNavigationIndex());

					mLoading = false;
				}
			}.execute();
		}

		@Override
		public void onLoaderReset(Loader<Cursor> arg0) {
			mMovies.clear();
			mMovieKeys.clear();
			notifyDataSetChanged();
		}
	};

	private void clearCaches() {
		if (isAdded())
			MizuuApplication.getLruCache(getActivity()).clear();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.image_grid_fragment, container, false);
	}

	@Override
	public void onViewCreated(View v, Bundle savedInstanceState) {
		super.onViewCreated(v, savedInstanceState);

		mProgressBar = (ProgressBar) v.findViewById(R.id.progress);
		if (mMovieKeys.size() > 0)
			mProgressBar.setVisibility(View.GONE);

		mEmptyLibraryLayout = v.findViewById(R.id.empty_library_layout);
		mEmptyLibraryIcon = (ImageView) v.findViewById(R.id.empty_library_icon);
		mEmptyLibraryTitle = (TextView) v.findViewById(R.id.empty_library_title);
		mEmptyLibraryTitle.setTypeface(MizuuApplication.getOrCreateTypeface(getActivity(), "RobotoCondensed-Regular.ttf"));
		mEmptyLibraryDescription = (TextView) v.findViewById(R.id.empty_library_description);
		if (mType == OTHER)
			mEmptyLibraryDescription.setText(R.string.empty_watchlist_description);
		mEmptyLibraryDescription.setTypeface(MizuuApplication.getOrCreateTypeface(getActivity(), "Roboto-Light.ttf"));

		if (!MizuuApplication.usesDarkTheme(getActivity())) {
			mEmptyLibraryIcon.setImageResource(R.drawable.no_content_face_dark);
			mEmptyLibraryTitle.setTextColor(Color.parseColor("#222222"));
			mEmptyLibraryDescription.setTextColor(Color.parseColor("#222222"));
		}

		mAdapter = new LoaderAdapter(getActivity());

		mGridView = (GridView) v.findViewById(R.id.gridView);
		mGridView.setAdapter(mAdapter);
		mGridView.setEmptyView(mEmptyLibraryLayout);
		mGridView.setColumnWidth(mImageThumbSize);

		// Calculate the total column width to set item heights by factor 1.5
		mGridView.getViewTreeObserver().addOnGlobalLayoutListener(
				new ViewTreeObserver.OnGlobalLayoutListener() {
					@Override
					public void onGlobalLayout() {
						if (mAdapter.getNumColumns() == 0) {
							final int numColumns = (int) Math.floor(mGridView.getWidth() / (mImageThumbSize + mImageThumbSpacing));
							if (numColumns > 0) {
								mAdapter.setNumColumns(numColumns);
								mResizedWidth = (int) (((mGridView.getWidth() - (numColumns * mImageThumbSpacing))
										/ numColumns) * 1.1); // * 1.1 is a hack to make images look slightly less blurry
								mResizedHeight = (int) (mResizedWidth * 1.5);
							}

							MizLib.removeViewTreeObserver(mGridView.getViewTreeObserver(), this);
						}
					}
				});
		mGridView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				Intent intent = new Intent();
				if (mActionBar.getSelectedNavigationIndex() == 3) { // Collection
					intent.putExtra("collectionId", mMovies.get(mMovieKeys.get(arg2)).getCollectionId());
					intent.putExtra("collectionTitle", mMovies.get(mMovieKeys.get(arg2)).getCollection());
					intent.setClass(getActivity(), MovieCollection.class);
					startActivity(intent);
				} else {
					intent.putExtra("rowId", Integer.parseInt(mMovies.get(mMovieKeys.get(arg2)).getRowId()));
					intent.setClass(getActivity(), MovieDetails.class);
					startActivityForResult(intent, 0);
				}
			}
		});
	}

	@Override
	public void onResume() {
		super.onResume();

		if (mMovies.size() == 0)
			forceLoaderLoad();

		if (mAdapter != null)
			mAdapter.notifyDataSetChanged();
	}

	@Override
	public void onDestroy() {	
		// Unregister since the activity is about to be closed.
		LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mMessageReceiver);
		PreferenceManager.getDefaultSharedPreferences(getActivity()).unregisterOnSharedPreferenceChangeListener(this);

		super.onDestroy();
	}

	private class LoaderAdapter extends BaseAdapter {

		private LayoutInflater mInflater;
		private final Context mContext;
		private int mNumColumns = 0, mSidePadding, mBottomPadding, mCard, mCardBackground, mCardTitleColor;
		private boolean mCollectionsView = false;

		public LoaderAdapter(Context context) {
			mContext = context;
			mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			mSidePadding = MizLib.convertDpToPixels(mContext, 1);
			mBottomPadding = MizLib.convertDpToPixels(mContext, 2);
			mCard = MizuuApplication.getCardDrawable(mContext);
			mCardBackground = MizuuApplication.getCardColor(mContext);
			mCardTitleColor = MizuuApplication.getCardTitleColor(mContext);
		}

		@Override
		public boolean isEmpty() {
			return (!mLoading && mMovies.size() == 0);
		}

		@Override
		public int getCount() {
			return mMovieKeys.size();
		}

		@Override
		public Object getItem(int position) {
			return position;
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup container) {

			int mKeyPosition = mMovieKeys.get(position);

			CoverItem holder;
			if (convertView == null) {
				convertView = mInflater.inflate(R.layout.grid_item, container, false);
				holder = new CoverItem();

				holder.mLinearLayout = (LinearLayout) convertView.findViewById(R.id.card_layout);
				holder.cover = (ImageView) convertView.findViewById(R.id.cover);
				holder.text = (TextView) convertView.findViewById(R.id.text);
				holder.subtext = (TextView) convertView.findViewById(R.id.gridCoverSubtitle);

				holder.mLinearLayout.setBackgroundResource(mCard);
				holder.text.setBackgroundResource(mCardBackground);
				holder.text.setTextColor(mCardTitleColor);
				holder.text.setTypeface(MizuuApplication.getOrCreateTypeface(mContext, "Roboto-Medium.ttf"));
				holder.subtext.setBackgroundResource(mCardBackground);

				convertView.setTag(holder);
			} else {
				holder = (CoverItem) convertView.getTag();
			}

			if (!mShowTitles) {
				holder.text.setVisibility(View.GONE);
				holder.subtext.setVisibility(View.GONE);
				holder.cover.setPadding(mSidePadding, 0, mSidePadding, mBottomPadding);
			} else {
				holder.text.setVisibility(View.VISIBLE);
				holder.subtext.setVisibility(View.VISIBLE);

				holder.text.setText(mCollectionsView ? mMovies.get(mKeyPosition).getCollection() : mMovies.get(mKeyPosition).getTitle());
				holder.subtext.setText(mMovies.get(mKeyPosition).getSubText(mCurrentSort));
			}

			holder.cover.setImageResource(mCardBackground);

			if (!mCollectionsView) {
				// Movie poster
				if (!mIgnoreNfo && mMovies.get(mKeyPosition).isNetworkFile()) {
					if (mResizedWidth > 0)
						mPicasso.load(mMovies.get(mKeyPosition).getFilepath() + "<MiZ>" + mMovies.get(mKeyPosition).getThumbnail()).resize(mResizedWidth, mResizedHeight).config(mConfig).into(holder);
					else
						mPicasso.load(mMovies.get(mKeyPosition).getFilepath() + "<MiZ>" + mMovies.get(mKeyPosition).getThumbnail()).config(mConfig).into(holder);
				} else {
					if (mResizedWidth > 0)
						mPicasso.load(mMovies.get(mKeyPosition).getThumbnail()).resize(mResizedWidth, mResizedHeight).config(mConfig).into(holder);
					else
						mPicasso.load(mMovies.get(mKeyPosition).getThumbnail()).config(mConfig).into(holder);
				}
			} else {
				// Collection poster
				if (mResizedWidth > 0)
					mPicasso.load(mMovies.get(mKeyPosition).getCollectionPoster()).resize(mResizedWidth, mResizedHeight).config(mConfig).into(holder);
				else
					mPicasso.load(mMovies.get(mKeyPosition).getCollectionPoster()).config(mConfig).into(holder);
			}

			return convertView;
		}

		public void setNumColumns(int numColumns) {
			mNumColumns = numColumns;
		}

		public int getNumColumns() {
			return mNumColumns;
		}
	}

	private void notifyDataSetChanged() {		
		if (mAdapter != null)
			mAdapter.notifyDataSetChanged();

		if (mSpinnerAdapter != null)
			mSpinnerAdapter.notifyDataSetChanged();
	}

	@Override
	public boolean onNavigationItemSelected(int itemPosition, long itemId) {
		if (!mLoading)
			showMovieSection(itemPosition);

		return true;
	}

	private class MovieSectionLoader extends MovieSectionAsyncTask<Void, Void, Boolean> {
		private int mPosition;
		private ArrayList<Integer> mTempKeys = new ArrayList<Integer>();

		public MovieSectionLoader(int position) {
			mPosition = position;
		}

		@Override
		protected void onPreExecute() {
			setProgressBarVisible(true);
			mMovieKeys.clear();
		}

		@Override
		protected Boolean doInBackground(Void... params) {
			if (isCancelled())
				return false;

			switch (mPosition) {
			case ALL_MOVIES:
				for (int i = 0; i < mMovies.size(); i++) 
					if (!mMovies.get(i).isUnidentified())
						mTempKeys.add(i);
				break;

			case FAVORITES:
				for (int i = 0; i < mMovies.size(); i++)
					if (mMovies.get(i).isFavourite())
						mTempKeys.add(i);
				break;

			case AVAILABLE_FILES:
				ArrayList<FileSource> filesources = MizLib.getFileSources(MizLib.TYPE_MOVIE, true);

				for (int i = 0; i < mMovies.size(); i++) {
					if (isCancelled())
						return false;

					if (mMovies.get(i).isNetworkFile())
						if (mMovies.get(i).hasOfflineCopy())
							mTempKeys.add(i);
						else {						
							if (MizLib.isWifiConnected(getActivity(), mDisableEthernetWiFiCheck)) {
								FileSource source = null;

								for (int j = 0; j < filesources.size(); j++)
									if (mMovies.get(i).getFilepath().contains(filesources.get(j).getFilepath())) {
										source = filesources.get(j);
										continue;
									}

								if (source == null)
									continue;

								try {
									final SmbFile file = new SmbFile(
											MizLib.createSmbLoginString(
													URLEncoder.encode(source.getDomain(), "utf-8"),
													URLEncoder.encode(source.getUser(), "utf-8"),
													URLEncoder.encode(source.getPassword(), "utf-8"),
													mMovies.get(i).getFilepath(),
													false
													));
									if (file.exists())
										mTempKeys.add(i);
								} catch (Exception e) {}  // Do nothing - the file isn't available (either MalformedURLException or SmbException)
							}
						}
					else if (mMovies.get(i).isUpnpFile())
						if (MizLib.exists(mMovies.get(i).getFilepath()))
							mTempKeys.add(i);
						else
							if (new File(mMovies.get(i).getFilepath()).exists())
								mTempKeys.add(i);
				}
				break;

			case COLLECTIONS:
				HashMap<String, MediumMovie> map = new HashMap<String, MediumMovie>();
				for (int i = 0; i < mMovies.size(); i++)
					if (!MizLib.isEmpty(mMovies.get(i).getCollection()) && !mMovies.get(i).getCollection().equals("null"))
						if (!map.containsKey(mMovies.get(i).getCollection())) {
							map.put(mMovies.get(i).getCollection(), mMovies.get(i));
							mTempKeys.add(i);
						}
				map.clear();
				break;

			case WATCHED_MOVIES:
				for (int i = 0; i < mMovies.size(); i++)
					if (mMovies.get(i).hasWatched())
						mTempKeys.add(i);
				break;

			case UNWATCHED_MOVIES:
				for (int i = 0; i < mMovies.size(); i++)
					if (!mMovies.get(i).hasWatched())
						mTempKeys.add(i);
				break;

			case UNIDENTIFIED_MOVIES:
				for (int i = 0; i < mMovies.size(); i++)
					if (mMovies.get(i).isUnidentified())
						mTempKeys.add(i);
				break;

			case OFFLINE_COPIES:
				for (int i = 0; i < mMovies.size(); i++) {
					if (isCancelled())
						return false;

					if (mMovies.get(i).hasOfflineCopy())
						mTempKeys.add(i);
				}
				break;
			}

			return true;
		}

		@Override
		protected void onPostExecute(Boolean success) {
			// Make sure that the loading was successful, that the Fragment is still added and
			// that the currently selected navigation index is the same as when we started loading
			if (success && isAdded() && mActionBar.getSelectedNavigationIndex() == mPosition) {
				mMovieKeys.addAll(mTempKeys);

				sortMovies();
				notifyDataSetChanged();
				setProgressBarVisible(false);
			}
		}
	}

	private void showMovieSection(int position) {
		if (mSpinnerAdapter != null)
			mSpinnerAdapter.notifyDataSetChanged(); // To show "0 movies" when loading

		if (mMovieSectionLoader != null)
			mMovieSectionLoader.cancel(true);
		mMovieSectionLoader = new MovieSectionLoader(position);
		mMovieSectionLoader.execute();
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		if (((Main) getActivity()).isDrawerOpen()) {
			if (mActionBar == null && getActivity() != null)
				mActionBar = getActivity().getActionBar();
			mActionBar.setNavigationMode(ActionBar.DISPLAY_SHOW_TITLE);
			((Main) getActivity()).showDrawerOptionsMenu(menu, inflater);
		} else {
			setupActionBar();
			inflater.inflate(R.menu.menu, menu);
			if (mType == OTHER) // Don't show the Update icon if this is the Watchlist
				menu.removeItem(R.id.update);

			SearchManager searchManager = (SearchManager) getActivity().getSystemService(Context.SEARCH_SERVICE);
			SearchView searchView = (SearchView) menu.findItem(R.id.search_textbox).getActionView();
			searchView.setSearchableInfo(searchManager.getSearchableInfo(getActivity().getComponentName()));
			searchView.setOnQueryTextListener(new OnQueryTextListener() {
				@Override
				public boolean onQueryTextChange(String newText) {
					if (newText.length() > 0) {
						search(newText);
					} else {
						showMovieSection(ALL_MOVIES);
					}
					return true;
				}
				@Override
				public boolean onQueryTextSubmit(String query) { return false; }
			});
		}
		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {		
		super.onOptionsItemSelected(item);

		switch (item.getItemId()) {
		case R.id.update:
			startActivityForResult(getUpdateIntent(), 0);
			break;
		case R.id.menuSortAdded:
			sortBy(DATE);
			break;
		case R.id.menuSortRating:
			sortBy(RATING);
			break;
		case R.id.menuSortWeightedRating:
			sortBy(WEIGHTED_RATING);
			break;
		case R.id.menuSortRelease:
			sortBy(RELEASE);
			break;
		case R.id.menuSortTitle:
			sortBy(TITLE);
			break;
		case R.id.menuSortDuration:
			sortBy(DURATION);
			break;
		case R.id.menuSettings:
			startActivity(new Intent(getActivity(), Preferences.class));
			break;
		case R.id.genres:
			showGenres();
			break;
		case R.id.certifications:
			showCertifications();
			break;
		case R.id.folders:
			showFolders();
			break;
		case R.id.fileSources:
			showFileSources();
			break;
		case R.id.clear_filters:
			showMovieSection(mActionBar.getSelectedNavigationIndex());
			break;
		}

		return true;
	}

	private Intent getUpdateIntent() {
		Intent intent = new Intent();
		intent.setClass(getActivity(), Update.class);
		intent.putExtra("isMovie", true);
		return intent;
	}

	private void sortMovies() {
		if (!isAdded())
			return;

		String SORT_TYPE = mSharedPreferences.getString((getActivity().getActionBar().getSelectedNavigationIndex() == 3) ? SORTING_COLLECTIONS_OVERVIEW : SORTING_MOVIES, "sortTitle");

		Editor editor = mSharedPreferences.edit();
		editor.putString((getActivity().getActionBar().getSelectedNavigationIndex() == 3) ? SORTING_COLLECTIONS_OVERVIEW : SORTING_MOVIES, SORT_TYPE);
		editor.apply();

		if (SORT_TYPE.equals("sortRelease")) {
			sortBy(RELEASE);
		} else if (SORT_TYPE.equals("sortRating")) {
			sortBy(RATING);
		} else if (SORT_TYPE.equals("sortWeightedRating")) {
			sortBy(WEIGHTED_RATING);
		} else if (SORT_TYPE.equals("sortAdded")) {
			sortBy(DATE);
		} else if (SORT_TYPE.equals("sortDuration")) {
			sortBy(DURATION);
		} else { // if SORT_TYPE equals "sortTitle"
			sortBy(TITLE);
		}
	}

	public void sortBy(int sort) {
		mCurrentSort = sort;

		boolean isCollection = getActivity().getActionBar().getSelectedNavigationIndex() == 3;	
		ArrayList<MovieSortHelper> tempHelper = new ArrayList<MovieSortHelper>();
		for (int i = 0; i < mMovieKeys.size(); i++) {
			tempHelper.add(new MovieSortHelper(mMovies.get(mMovieKeys.get(i)), mMovieKeys.get(i), mCurrentSort, isCollection));
		}

		Collections.sort(tempHelper);

		mMovieKeys.clear();
		for (int i = 0; i < tempHelper.size(); i++) {
			mMovieKeys.add(tempHelper.get(i).getIndex());
		}

		tempHelper.clear();

		setProgressBarVisible(false);
		notifyDataSetChanged();
	}

	private void showGenres() {
		final TreeMap<String, Integer> map = new TreeMap<String, Integer>();
		String[] splitGenres;
		for (int i = 0; i < mMovieKeys.size(); i++) {
			if (!mMovies.get(mMovieKeys.get(i)).getGenres().isEmpty()) {
				splitGenres = mMovies.get(mMovieKeys.get(i)).getGenres().split(",");
				for (int j = 0; j < splitGenres.length; j++) {
					if (map.containsKey(splitGenres[j].trim())) {
						map.put(splitGenres[j].trim(), map.get(splitGenres[j].trim()) + 1);
					} else {
						map.put(splitGenres[j].trim(), 1);
					}
				}
			}
		}

		final CharSequence[] temp = setupItemArray(map, R.string.allGenres);

		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(R.string.selectGenre)
		.setItems(temp, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				handleDialogOnClick(dialog, which, GENRES, temp);
			}
		});
		builder.show();
	}

	private void showCertifications() {
		final TreeMap<String, Integer> map = new TreeMap<String, Integer>();
		for (int i = 0; i < mMovieKeys.size(); i++) {
			String certification = mMovies.get(mMovieKeys.get(i)).getCertification();
			if (!MizLib.isEmpty(certification)) {
				if (map.containsKey(certification.trim())) {
					map.put(certification.trim(), map.get(certification.trim()) + 1);
				} else {
					map.put(certification.trim(), 1);
				}
			}
		}

		final CharSequence[] temp = setupItemArray(map, R.string.allCertifications);

		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(R.string.selectCertification)
		.setItems(temp, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				handleDialogOnClick(dialog, which, CERTIFICATION, temp);
			}
		});
		builder.show();
	}

	private void showFileSources() {
		ArrayList<FileSource> sources = new ArrayList<FileSource>();

		DbAdapterSources dbHelper = MizuuApplication.getSourcesAdapter();
		Cursor cursor = dbHelper.fetchAllMovieSources();
		while (cursor.moveToNext()) {
			sources.add(new FileSource(
					cursor.getLong(cursor.getColumnIndex(DbAdapterSources.KEY_ROWID)),
					cursor.getString(cursor.getColumnIndex(DbAdapterSources.KEY_FILEPATH)),
					cursor.getInt(cursor.getColumnIndex(DbAdapterSources.KEY_FILESOURCE_TYPE)),
					cursor.getString(cursor.getColumnIndex(DbAdapterSources.KEY_USER)),
					cursor.getString(cursor.getColumnIndex(DbAdapterSources.KEY_PASSWORD)),
					cursor.getString(cursor.getColumnIndex(DbAdapterSources.KEY_DOMAIN)),
					cursor.getString(cursor.getColumnIndex(DbAdapterSources.KEY_TYPE))
					));
		}
		cursor.close();

		final TreeMap<String, Integer> map = new TreeMap<String, Integer>();

		for (int i = 0; i < mMovieKeys.size(); i++) {
			String filepath = mMovies.get(mMovieKeys.get(i)).getFilepath();
			for (int j = 0; j < sources.size(); j++) {
				String source = sources.get(j).getFilepath();

				if (!MizLib.isEmpty(source) && !MizLib.isEmpty(filepath) && filepath.contains(source)) {
					if (map.containsKey(source.trim())) {
						map.put(source.trim(), map.get(source.trim()) + 1);
					} else {
						map.put(source.trim(), 1);
					}
				}
			}
		}

		final CharSequence[] temp = setupItemArray(map, R.string.allFileSources);

		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(R.string.selectFileSource)
		.setItems(temp, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				handleDialogOnClick(dialog, which, FILE_SOURCES, temp);
			}
		});
		builder.show();
	}

	private void showFolders() {
		final TreeMap<String, Integer> map = new TreeMap<String, Integer>();
		for (int i = 0; i < mMovieKeys.size(); i++) {
			String folder = MizLib.getParentFolder(mMovies.get(mMovieKeys.get(i)).getFilepath());
			if (!MizLib.isEmpty(folder)) {
				if (map.containsKey(folder.trim())) {
					map.put(folder.trim(), map.get(folder.trim()) + 1);
				} else {
					map.put(folder.trim(), 1);
				}
			}
		}

		final CharSequence[] temp = setupItemArray(map, R.string.allFolders);

		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(R.string.selectFolder)
		.setItems(temp, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				handleDialogOnClick(dialog, which, FOLDERS, temp);
			}
		});
		builder.show();
	}

	private CharSequence[] setupItemArray(TreeMap<String, Integer> map, int stringId) {
		final CharSequence[] tempArray = map.keySet().toArray(new CharSequence[map.keySet().size()]);	
		for (int i = 0; i < tempArray.length; i++)
			tempArray[i] = tempArray[i] + " (" + map.get(tempArray[i]) +  ")";

		final CharSequence[] temp = new CharSequence[tempArray.length + 1];
		temp[0] = getString(stringId);

		for (int i = 1; i < temp.length; i++)
			temp[i] = tempArray[i-1];

		return temp;
	}

	private void handleDialogOnClick(DialogInterface dialog, int which, int type, CharSequence[] temp) {
		if (which > 0) {
			ArrayList<Integer> currentlyShown = new ArrayList<Integer>(mMovieKeys);
			mMovieKeys.clear();

			String selected = temp[which].toString();
			selected = selected.substring(0, selected.lastIndexOf("(")).trim();

			boolean condition;
			for (int i = 0; i < currentlyShown.size(); i++) {
				condition = false;

				switch (type) {
				case GENRES:
					if (mMovies.get(currentlyShown.get(i)).getGenres().contains(selected)) {
						String[] genres = mMovies.get(currentlyShown.get(i)).getGenres().split(",");
						for (String genre : genres) {
							if (genre.trim().equals(selected)) {
								condition = true;
								break;
							}
						}
					}
					break;
				case CERTIFICATION:
					condition = mMovies.get(currentlyShown.get(i)).getCertification().trim().contains(selected);
					break;
				case FILE_SOURCES:
					condition = mMovies.get(currentlyShown.get(i)).getFilepath().trim().contains(selected);
					break;
				case FOLDERS:
					condition = mMovies.get(currentlyShown.get(i)).getFilepath().trim().startsWith(selected);
					break;
				}

				if (condition)
					mMovieKeys.add(currentlyShown.get(i));
			}

			sortMovies();
			notifyDataSetChanged();
		}

		dialog.dismiss();
	}

	private void search(String query) {
		if (mSearch != null)
			mSearch.cancel(true);
		mSearch = new SearchTask(query);
		mSearch.execute();
	}

	private class SearchTask extends AsyncTask<String, String, String> {

		private String mSearchQuery = "";
		private List<Integer> mTempKeys;

		public SearchTask(String query) {
			mSearchQuery = query.toLowerCase(Locale.ENGLISH);
		}

		@Override
		protected void onPreExecute() {
			setProgressBarVisible(true);
			mMovieKeys.clear();
		}

		@Override
		protected String doInBackground(String... params) {
			mTempKeys = new ArrayList<Integer>();

			if (mSearchQuery.startsWith("actor:")) {
				for (int i = 0; i < mMovies.size(); i++) {
					if (isCancelled())
						return null;

					if (mMovies.get(i).getCast().toLowerCase(Locale.ENGLISH).contains(mSearchQuery.replace("actor:", "").trim()))
						mTempKeys.add(i);
				}
			} else if (mSearchQuery.equalsIgnoreCase("missing_genres")) {
				for (int i = 0; i < mMovies.size(); i++) {
					if (isCancelled())
						return null;

					if (MizLib.isEmpty(mMovies.get(i).getGenres()))
						mTempKeys.add(i);
				}
			} else if (mSearchQuery.equalsIgnoreCase("multiple_versions")) {
				DbAdapter db = MizuuApplication.getMovieAdapter();

				for (int i = 0; i < mMovies.size(); i++) {
					if (isCancelled())
						return null;

					if (db.hasMultipleVersions(mMovies.get(i).getTmdbId()))
						mTempKeys.add(i);
				}
			} else {
				String lowerCase = "", filepath; // Reuse String variables
				Pattern p = Pattern.compile(MizLib.CHARACTER_REGEX); // Use a pre-compiled pattern as it's a lot faster (approx. 3x for ~700 movies)

				for (int i = 0; i < mMovies.size(); i++) {
					if (isCancelled())
						return null;

					lowerCase = mMovies.get(i).getTitle().toLowerCase(Locale.ENGLISH);
					filepath = mMovies.get(i).getFilepath().toLowerCase(Locale.ENGLISH);

					if (lowerCase.indexOf(mSearchQuery) != -1 || filepath.indexOf(mSearchQuery) != -1 || p.matcher(lowerCase).replaceAll("").indexOf(mSearchQuery) != -1)
						mTempKeys.add(i);
				}
			}

			return null;
		}

		@Override
		protected void onPostExecute(String result) {
			mMovieKeys.addAll(mTempKeys);

			sortMovies();
			notifyDataSetChanged();
			setProgressBarVisible(false);
		}
	}

	private void setProgressBarVisible(boolean visible) {
		mProgressBar.setVisibility(visible ? View.VISIBLE : View.GONE);
		mGridView.setVisibility(visible ? View.GONE : View.VISIBLE);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (resultCode == 1) { // Update
			forceLoaderLoad();
		} else if (resultCode == 2 && mActionBar.getSelectedNavigationIndex() == FAVORITES) { // Favourite removed
			showMovieSection(FAVORITES);
		} else if (resultCode == 3) {
			notifyDataSetChanged();
		}
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (key.equals(IGNORED_TITLE_PREFIXES)) {
			mDisableEthernetWiFiCheck = mSharedPreferences.getBoolean(IGNORED_TITLE_PREFIXES, false);
			forceLoaderLoad();
		} else if (key.equals(GRID_ITEM_SIZE)) {
			String thumbnailSize = mSharedPreferences.getString(GRID_ITEM_SIZE, getString(R.string.normal));
			if (thumbnailSize.equals(getString(R.string.large))) 
				mImageThumbSize = (int) (getResources().getDimensionPixelSize(R.dimen.image_thumbnail_size) * 1.33);
			else if (thumbnailSize.equals(getString(R.string.normal))) 
				mImageThumbSize = (int) (getResources().getDimensionPixelSize(R.dimen.image_thumbnail_size) * 1);
			else
				mImageThumbSize = (int) (getResources().getDimensionPixelSize(R.dimen.image_thumbnail_size) * 0.75);

			mGridView.setColumnWidth(mImageThumbSize);

			final int numColumns = (int) Math.floor(mGridView.getWidth() / (mImageThumbSize + mImageThumbSpacing));
			if (numColumns > 0) {
				mAdapter.setNumColumns(numColumns);
			}

			notifyDataSetChanged();
		} else if (key.equals(SHOW_TITLES_IN_GRID)) {
			mShowTitles = sharedPreferences.getBoolean(SHOW_TITLES_IN_GRID, true);
			notifyDataSetChanged();
		}
	}

	private void forceLoaderLoad() {
		if (isAdded())
			if (getLoaderManager().getLoader(0) == null) {
				getLoaderManager().initLoader(0, null, loaderCallbacks);
			} else {
				getLoaderManager().restartLoader(0, null, loaderCallbacks);
			}
	}

	private class ActionBarSpinner extends BaseAdapter {

		private LayoutInflater mInflater;

		public ActionBarSpinner() {
			mInflater = LayoutInflater.from(getActivity());
		}

		@Override
		public int getCount() {
			return mSpinnerItems.size();
		}

		@Override
		public Object getItem(int position) {
			return null;
		}

		@Override
		public long getItemId(int position) {
			return 0;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			convertView = mInflater.inflate(R.layout.spinner_header, parent, false);
			((TextView) convertView.findViewById(R.id.title)).setText(mSpinnerItems.get(position).getTitle());

			int size = mMovieKeys.size();
			((TextView) convertView.findViewById(R.id.subtitle)).setText(size + " " + getResources().getQuantityString((mActionBar.getSelectedNavigationIndex() == 3) ?
					R.plurals.collectionsInLibrary : R.plurals.moviesInLibrary, size, size));

			return convertView;
		}

		@Override
		public boolean isEmpty() {
			return mSpinnerItems.size() == 0;
		}

		@Override
		public View getDropDownView(int position, View convertView, ViewGroup parent) {			
			convertView = mInflater.inflate(android.R.layout.simple_spinner_dropdown_item, parent, false);
			((TextView) convertView.findViewById(android.R.id.text1)).setText(mSpinnerItems.get(position).getSubtitle());
			return convertView;
		}
	}
}