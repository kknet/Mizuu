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

public class MenuItem {
	
	public static final int HEADER = 1000, SECTION = 2000, THIRD_PARTY_APP = 3000;
	
	private int mType, mCount;
	private String mTitle, mPackageName;
	
	public MenuItem(String title, int count, int type, String packageName) {
		mTitle = title;
		mCount = count;
		mType = type;
		mPackageName = packageName;
	}
	
	public String getTitle() {
		return mTitle;
	}
	
	public int getCount() {
		return mCount;
	}
	
	public String getPackageName() {
		return mPackageName;
	}
	
	public int getType() {
		return mType;
	}
}