package com.miz.functions;

import static com.miz.functions.PreferenceKeys.TVSHOWS_EPISODE_ORDER;

import java.io.File;

import com.miz.mizuu.R;

import android.content.Context;
import android.preference.PreferenceManager;

public class GridEpisode implements Comparable<GridEpisode> {

	private Context mContext;
	private File mCover;
	private String mSubtitleText, mTitle, mFilepath;
	private int mSeason, mEpisode;
	private boolean mWatched;

	public GridEpisode(Context context, String title, String filepath, int season, int episode, boolean watched, File cover) {
		mContext = context;
		mTitle = title;
		mFilepath = filepath;
		mSeason = season;
		mEpisode = episode;
		mWatched = watched;
		mCover = cover;

		// Subtitle text
		StringBuilder sb = new StringBuilder();
		sb.append(mContext.getString(R.string.showEpisode));
		sb.append(" ");
		sb.append(getEpisode());
		if (!hasWatched())
			sb.append(" " + mContext.getString(R.string.unwatched));

		mSubtitleText = sb.toString();
	}
	
	public String getTitle() {
		if (MizLib.isEmpty(mTitle)) {
			String temp = mFilepath.contains("<MiZ>") ? mFilepath.split("<MiZ>")[0] : mFilepath;
			File fileName = new File(temp);
			int pointPosition=fileName.getName().lastIndexOf(".");
			return pointPosition == -1 ? fileName.getName() : fileName.getName().substring(0, pointPosition);
		} else {
			return mTitle;
		}
	}

	public int getEpisode() {
		return mEpisode;
	}

	public int getSeason() {
		return mSeason;
	}

	public String getSeasonZeroIndex() {
		return MizLib.addIndexZero(mSeason);
	}

	public String getSubtitleText() {
		return mSubtitleText;
	}

	public boolean hasWatched() {
		return mWatched;
	}
	
	public File getCover() {
		return mCover;
	}

	@Override
	public int compareTo(GridEpisode another) {
		String defaultOrder = mContext.getString(R.string.oldestFirst);
		boolean oldestFirst = PreferenceManager.getDefaultSharedPreferences(mContext).getString(TVSHOWS_EPISODE_ORDER, defaultOrder).equals(defaultOrder);
		int multiplier = oldestFirst ? 1 : -1;

		// Regular sorting
		if (getEpisode() < another.getEpisode())
			return -1 * multiplier;
		if (getEpisode() > another.getEpisode())
			return 1 * multiplier;
		return 0;
	}
}