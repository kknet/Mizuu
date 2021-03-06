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

package com.miz.functions;

import java.io.File;

import com.miz.mizuu.R;

import android.content.Context;
import android.preference.PreferenceManager;

import static com.miz.functions.PreferenceKeys.TVSHOWS_COLLECTION_LAYOUT;
import static com.miz.functions.PreferenceKeys.TVSHOWS_SEASON_ORDER;

public class GridSeason implements Comparable<GridSeason> {
	
	private Context mContext;
	private String mSubtitleText;
	private int mSeason, mEpisodeCount, mWatchedCount;
	private boolean mUseGridView;
	private File mCover;
	
	public GridSeason(Context context, int season, int episodeCount, int watchedCount, File cover) {
		mContext = context;
		mSeason = season;
		mEpisodeCount = episodeCount;
		mWatchedCount = watchedCount;
		mCover = cover;
		
		mUseGridView = PreferenceManager.getDefaultSharedPreferences(mContext).getString(TVSHOWS_COLLECTION_LAYOUT, mContext.getString(R.string.gridView)).equals(mContext.getString(R.string.gridView));
		
		// Subtitle text
		StringBuilder sb = new StringBuilder();
		sb.append(getEpisodeCount() + " ");
		sb.append(mContext.getResources().getQuantityString(R.plurals.episodes, getEpisodeCount(), getEpisodeCount()));
		
		// Change the text depending on the available space
		if (getUnwatchedCount() > 0) {
			if (MizLib.isXlargeTablet(mContext) || !mUseGridView) // Large tablets or list view: (23 unwatched)
				sb.append(" (" + String.format(mContext.getString(R.string.unwatchedEpisodesCount), getUnwatchedCount()) + ")");
			else if (MizLib.isTablet(mContext)) // Small tablets (23 left)
				sb.append(" (" + String.format(mContext.getString(R.string.leftEpisodesCount), getUnwatchedCount()) + ")");
			else // Phones (23)
				sb.append(" (" + getUnwatchedCount() + ")");
		}
		
		mSubtitleText = sb.toString();
	}

	public int getSeason() {
		return mSeason;
	}
	
	public String getSeasonZeroIndex() {
		return MizLib.addIndexZero(mSeason);
	}
	
	public int getEpisodeCount() {
		return mEpisodeCount;
	}
	
	public int getWatchedCount() {
		return mWatchedCount;
	}
	
	public int getUnwatchedCount() {
		return mEpisodeCount - mWatchedCount;
	}
	
	public String getSubtitleText() {
		return mSubtitleText;
	}

	public File getCover() {
		return mCover;
	}

	/**
	 * Custom comparator in order to put regular seasons before specials.
	 * The comparator takes the user's sorting preference into consideration
	 * - for regular seasons only. Specials remain at the end.
	 */
	@Override
	public int compareTo(GridSeason another) {
		String defaultOrder = mContext.getString(R.string.oldestFirst);
		boolean oldestFirst = PreferenceManager.getDefaultSharedPreferences(mContext).getString(TVSHOWS_SEASON_ORDER, defaultOrder).equals(defaultOrder);
		int multiplier = oldestFirst ? 1 : -1;
		
		if (getSeason() == 0)
			return 1;
		if (another.getSeason() == 0)
			return -1;
		
		// Regular sorting
		if (getSeason() < another.getSeason())
			return -1 * multiplier;
		if (getSeason() > another.getSeason())
			return 1 * multiplier;
		return 0;
	}
}