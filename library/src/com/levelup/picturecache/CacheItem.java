/**
 * 
 */
package com.levelup.picturecache;

import java.io.File;


class CacheItem {
	/**
	 * the path in the cache directory
	 */
	final File path;
	/**
	 * the image URL used to generate this file 
	 */
	final String URL;
	/**
	 *  the type of URL (short term 0 / long term 1 / eternal 2) see {@link CacheType}
	 */
	int type;
	/**
	 * the last logical item date using to the cache item (if applicable)
	 */
	long remoteDate;
	/**
	 * the date of last access to the item
	 */
	long lastAccessDate;
	
	CacheItem(File path, String url) {
		this.path = path;
		this.URL = url;
	}
	
	@Override
	public String toString() {
		return type+":"+URL+":"+getFileSize()+":"+path;
	}
	
	long getFileSize() {
		return path.length();
	}

	public CacheItem copyWithNewPath(File dst) {
		CacheItem copy = new CacheItem(dst, URL);
		copy.type = type;
		copy.remoteDate = remoteDate;
		copy.lastAccessDate = lastAccessDate;
		return copy;
	}
}