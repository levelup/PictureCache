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
	 *  the storage life span of URL see {@link LifeSpan}
	 */
	LifeSpan lifeSpan;
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
		return lifeSpan+":"+URL+":"+getFileSize()+":"+path;
	}
	
	long getFileSize() {
		return path.length();
	}

	public CacheItem copyWithNewPath(File dst) {
		CacheItem copy = new CacheItem(dst, URL);
		copy.lifeSpan = lifeSpan;
		copy.remoteDate = remoteDate;
		copy.lastAccessDate = lastAccessDate;
		return copy;
	}
}