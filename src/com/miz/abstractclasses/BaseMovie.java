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

package com.miz.abstractclasses;

import android.content.Context;

import java.io.File;
import java.util.Locale;

import com.miz.functions.MizLib;

public abstract class BaseMovie implements Comparable<BaseMovie> {

	protected Context mContext;
	protected String mRowId, mFilepath, mTitle, mTmdbId;
	protected boolean mIgnorePrefixes, mIgnoreNfo;

	public BaseMovie(Context context, String rowId, String filepath, String title, String tmdbId, boolean ignorePrefixes, boolean ignoreNfo) {
		// Set up movie fields based on constructor
		mContext = context;
		mRowId = rowId;
		mFilepath = filepath;
		mTitle = title;
		mTmdbId = tmdbId;
		mIgnorePrefixes = ignorePrefixes;
		mIgnoreNfo = ignoreNfo;

		// getTitle()
		if (mTitle == null || mTitle.isEmpty()) {
			String temp = mFilepath.contains("<MiZ>") ? mFilepath.split("<MiZ>")[0] : mFilepath;
			File fileName = new File(temp);
			int pointPosition=fileName.getName().lastIndexOf(".");
			mTitle = pointPosition == -1 ? fileName.getName() : fileName.getName().substring(0, pointPosition);
		} else {
			if (ignorePrefixes) {
				String temp = mTitle.toLowerCase(Locale.ENGLISH);
				String[] prefixes = MizLib.getPrefixes(mContext);
				int count = prefixes.length;
				for (int i = 0; i < count; i++) {
					if (temp.startsWith(prefixes[i])) {
						mTitle = mTitle.substring(prefixes[i].length());
						break;
					}
				}
			}
		}
	}

	public String getRowId() {
		return mRowId;
	}

	public String getTitle() {
		return mTitle;
	}

	/**
	 * This is only used for the SectionIndexer in the overview
	 */
	@Override
	public String toString() {
		try {
			return getTitle().substring(0, 1);
		} catch (Exception e) {
			return "";
		}
	}

	public File getThumbnail() {
		if (!mIgnoreNfo) {
			try {
				// Check if there's a custom cover art image
				String filename = mFilepath.substring(0, mFilepath.lastIndexOf(".")).replaceAll("part[1-9]|cd[1-9]", "").trim();
				File parentFolder = new File(mFilepath).getParentFile();

				if (parentFolder != null) {
					File[] list = parentFolder.listFiles();

					if (list != null) {
						String name, absolutePath;
						int count = list.length;
						for (int i = 0; i < count; i++) {
							name = list[i].getName();
							absolutePath = list[i].getAbsolutePath();
							if (name.equalsIgnoreCase("poster.jpg") ||
									name.equalsIgnoreCase("poster.jpeg") ||
									name.equalsIgnoreCase("poster.tbn") ||
									name.equalsIgnoreCase("folder.jpg") ||
									name.equalsIgnoreCase("folder.jpeg") ||
									name.equalsIgnoreCase("folder.tbn") ||
									name.equalsIgnoreCase("cover.jpg") ||
									name.equalsIgnoreCase("cover.jpeg") ||
									name.equalsIgnoreCase("cover.tbn") ||
									absolutePath.equalsIgnoreCase(filename + "-poster.jpg") ||
									absolutePath.equalsIgnoreCase(filename + "-poster.jpeg") ||
									absolutePath.equalsIgnoreCase(filename + "-poster.tbn") ||
									absolutePath.equalsIgnoreCase(filename + "-folder.jpg") ||
									absolutePath.equalsIgnoreCase(filename + "-folder.jpeg") ||
									absolutePath.equalsIgnoreCase(filename + "-folder.tbn") ||
									absolutePath.equalsIgnoreCase(filename + "-cover.jpg") ||
									absolutePath.equalsIgnoreCase(filename + "-cover.jpeg") ||
									absolutePath.equalsIgnoreCase(filename + "-cover.tbn") ||
									absolutePath.equalsIgnoreCase(filename + ".jpg") ||
									absolutePath.equalsIgnoreCase(filename + ".jpeg") ||
									absolutePath.equalsIgnoreCase(filename + ".tbn")) {
								return list[i];
							}
						}
					}
				}
			} catch (Exception e) {}
		}

		// New naming style
		return MizLib.getMovieThumb(mContext, mTmdbId);
	}

	public String getBackdrop() {
		if (!mIgnoreNfo) {
			try {
				// Check if there's a custom cover art image
				String filename = mFilepath.substring(0, mFilepath.lastIndexOf(".")).replaceAll("part[1-9]|cd[1-9]", "").trim();
				File parentFolder = new File(mFilepath).getParentFile();

				if (parentFolder != null) {
					File[] list = parentFolder.listFiles();

					if (list != null) {
						String name, absolutePath;
						int count = list.length;
						for (int i = 0; i < count; i++) {
							name = list[i].getName();
							absolutePath = list[i].getAbsolutePath();
							if (name.equalsIgnoreCase("fanart.jpg") ||
									name.equalsIgnoreCase("fanart.jpeg") ||
									name.equalsIgnoreCase("fanart.tbn") ||
									name.equalsIgnoreCase("banner.jpg") ||
									name.equalsIgnoreCase("banner.jpeg") ||
									name.equalsIgnoreCase("banner.tbn") ||
									name.equalsIgnoreCase("backdrop.jpg") ||
									name.equalsIgnoreCase("backdrop.jpeg") ||
									name.equalsIgnoreCase("backdrop.tbn") ||
									absolutePath.equalsIgnoreCase(filename + "-fanart.jpg") ||
									absolutePath.equalsIgnoreCase(filename + "-fanart.jpeg") ||
									absolutePath.equalsIgnoreCase(filename + "-fanart.tbn") ||
									absolutePath.equalsIgnoreCase(filename + "-banner.jpg") ||
									absolutePath.equalsIgnoreCase(filename + "-banner.jpeg") ||
									absolutePath.equalsIgnoreCase(filename + "-banner.tbn") ||
									absolutePath.equalsIgnoreCase(filename + "-backdrop.jpg") ||
									absolutePath.equalsIgnoreCase(filename + "-backdrop.jpeg") ||
									absolutePath.equalsIgnoreCase(filename + "-backdrop.tbn")) {
								return absolutePath;
							}
						}
					}
				}
			} catch (Exception e) {}
		}

		// New naming style
		return MizLib.getMovieBackdrop(mContext, mTmdbId).getAbsolutePath();
	}

	@Override
	public int compareTo(BaseMovie another) {
		return getTitle().compareToIgnoreCase(another.getTitle());
	}

}