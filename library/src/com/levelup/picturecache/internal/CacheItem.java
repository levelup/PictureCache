/**
 * 
 */
package com.levelup.picturecache.internal;

import java.io.File;

import com.levelup.picturecache.LifeSpan;


public class CacheItem {
	/**
	 * the path in the cache directory
	 */
	public final File path;
	/**
	 * the image URL used to generate this file 
	 */
	public final String URL;
	/**
	 *  the storage life span of URL see {@link LifeSpan}
	 */
	private LifeSpan lifeSpan;
	/**
	 * the last logical item date using to the cache item (if applicable)
	 */
	public long remoteDate;
	/**
	 * the date of last access to the item
	 */
	public long lastAccessDate;
	
	public CacheItem(File path, String url) {
		this.path = path;
		this.URL = url;
	}
	
	public final LifeSpan getLifeSpan() {
		return lifeSpan;
	}

	public final void setLifeSpan(LifeSpan lifeSpan) {
		this.lifeSpan = lifeSpan;
	}

	@Override
	public String toString() {
		return lifeSpan+":"+URL+":"+path.length()+":"+path;
	}
	
	public final CacheItem copyWithNewPath(File dst) {
		CacheItem copy = new CacheItem(dst, URL);
		copy.lifeSpan = lifeSpan;
		copy.remoteDate = remoteDate;
		copy.lastAccessDate = lastAccessDate;
		return copy;
	}
}