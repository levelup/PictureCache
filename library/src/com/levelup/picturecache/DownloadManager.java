/**
 * 
 */
package com.levelup.picturecache;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;

import android.graphics.Bitmap;
import android.text.TextUtils;

import com.levelup.picturecache.BitmapDownloader.JobMonitor;

class DownloadManager implements JobMonitor {

	private static final boolean DEBUG_DOWNLOADER = false;

	abstract interface JobsMonitor {
		abstract void onNewBitmapLoaded(HashMap<CacheVariant,Bitmap> newBitmaps, String url, long cacheDate, LifeSpan lifeSpan);
	}

	private final Hashtable<String, BitmapDownloader> mJobs = new Hashtable<String, BitmapDownloader>();
	private JobsMonitor mMonitor;

	void setMonitor(JobsMonitor monitor) {
		mMonitor = monitor;
	}

	void addDownloadTarget(PictureCache cache, String URL, PictureLoaderHandler loadHandler, CacheKey key, long itemDate, LifeSpan lifeSpan) {
		// find out if that URL is already loading, if so add the view to the recipient
		synchronized (mJobs) {
			// add job by URL
			BitmapDownloader downloader = mJobs.get(URL);
			if (DEBUG_DOWNLOADER) LogManager.logger.i(PictureCache.TAG, "add loader:"+loadHandler+" to downloader:"+downloader);
			final boolean targetAdded = downloader!=null && downloader.addTarget(loadHandler, key, itemDate, lifeSpan);
			if (!targetAdded) {
				if (DEBUG_DOWNLOADER) LogManager.logger.i(PictureCache.TAG, "add new downloader for "+URL+" key:"+key+" loader:"+loadHandler+" jobs:"+mJobs);
				// create a fresh new one if an old one is not ready to accept our loadHandler
				downloader = new BitmapDownloader(URL, cache);
				downloader.setMonitor(this);
				mJobs.put(URL, downloader);
				downloader.addTarget(loadHandler, key, itemDate, lifeSpan);
			}
			if (DEBUG_DOWNLOADER) {
				downloader = mJobs.get(URL);
				LogManager.logger.e(PictureCache.TAG, "downloader for "+URL+" = "+downloader+" loader added:"+targetAdded);
			}
		}
	}

	/**
	 * has to be done before a new {@link PictureLoaderHandler.setLoadingNewURL(DownloadManager, String, SimpleLogger)}
	 * @param loadHandler
	 * @param URL TODO
	 * @return true if there was a task loading
	 */
	boolean cancelDownloadForLoader(PictureLoaderHandler loadHandler, String URL) {
		synchronized (mJobs) {
			if (DEBUG_DOWNLOADER) LogManager.logger.i(PictureCache.TAG, "cancelDownloadForLoader for "+URL+" loadHandler:"+loadHandler);
			if (!TextUtils.isEmpty(URL)) {
				BitmapDownloader downloader = mJobs.get(URL);
				if (DEBUG_DOWNLOADER) LogManager.logger.i(PictureCache.TAG, " cancelDownloadForLoader loadHandler:"+loadHandler+" found:"+downloader);
				if (downloader!=null) {
					//LogManager.logger.d(PictureCache.TAG, "cancelDownloadForTarget for URL " + URL+" for "+loader);
					return downloader.removeTarget(loadHandler);
				}
			}
		}

		// find the target by view
		//LogManager.logger.w(PictureCache.TAG, "cancelDownloadForTarget by key " + loader);
		Enumeration<BitmapDownloader> downloaders = mJobs.elements();
		while (downloaders.hasMoreElements()) {
			BitmapDownloader downloader = downloaders.nextElement();
			if (downloader.removeTarget(loadHandler)) {
				if (DEBUG_DOWNLOADER) LogManager.logger.i(PictureCache.TAG, " cancelDownloadForLoader loadHandler:"+loadHandler+" deleted on:"+downloader/*+" url:"+url*/);
				return true;
			}
		}
		if (DEBUG_DOWNLOADER) LogManager.logger.w(PictureCache.TAG, "cancelDownloadForLoader do nothing for loadHandler:"+loadHandler);
		return false;
	}

	public void onJobFinishedWithNewBitmaps(BitmapDownloader downloader, HashMap<CacheVariant,Bitmap> newBitmaps) {
		if (mMonitor!=null)
			mMonitor.onNewBitmapLoaded(newBitmaps, downloader.getURL(), downloader.getItemDate(), downloader.getLifeSpan());

		synchronized (mJobs) {
			if (mJobs.containsKey(downloader.getURL())) {
				mJobs.remove(downloader.getURL());
				if (DEBUG_DOWNLOADER) LogManager.logger.i(PictureCache.TAG, "Job Finishing for "+downloader.getURL() + " remaining:"+mJobs);
			}
			else
				LogManager.logger.w(PictureCache.TAG, "Unknown job finishing for "+downloader.getURL() + " remaining:"+mJobs);
		}
	}
}