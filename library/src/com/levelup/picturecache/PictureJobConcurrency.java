package com.levelup.picturecache;

import java.io.File;

public interface PictureJobConcurrency {

	boolean isDownloadAllowed();

	/**
	 * called to tell the loader which URL is being loaded in the target
	 * @param url
	 * @return the URL that was previously loading, null if there wasn't any
	 */
	String setLoadingURL(String url);

	/**
	 * Allow downloading pictures in the calling thread, won't happen in the UI thread
	 * @param file File that we are trying to decode in the calling thread
	 * @return {@code true} if the file can be decoded
	 */
	boolean canDirectLoad(File file);

}
