package com.levelup.picturecache;

import java.io.File;

import uk.co.senab.bitmapcache.BitmapLruCache;

public interface IPictureLoadConcurrency {

	boolean isDownloadAllowed();

	/**
	 * called to tell the loader which URL is being loaded in the target
	 * @param url
	 * @param bitmapCache TODO
	 * @return the URL that was previously loading, null if there wasn't any
	 */
	String setLoadingURL(String url, BitmapLruCache bitmapCache);

	/**
	 * Allow downloading pictures in the calling thread (usually the UI thread), not recommended, won't happen in the UI thread in the memory cache
	 * @param file File that we are trying to decode in the calling thread
	 * @return {@code true} if the file can be decoded
	 */
	boolean canDirectLoad(File file);

}
