/**
 * 
 */
package com.levelup.picturecache.internal;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import android.annotation.SuppressLint;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.TextUtils;

import com.levelup.picturecache.LogManager;
import com.levelup.picturecache.PictureCache;
import com.levelup.picturecache.PictureJob;

public class DownloadManager {

	private static final boolean DEBUG_DOWNLOADER = false;

	private final static int THREAD_POOL_SIZE = Runtime.getRuntime().availableProcessors();

	private static final BlockingQueue<Runnable> sPoolWorkQueue = new LinkedBlockingQueue<Runnable>();

	private final ThreadPoolExecutor threadPool = new ThreadPoolExecutor(THREAD_POOL_SIZE, 2*THREAD_POOL_SIZE, 10, TimeUnit.SECONDS, sPoolWorkQueue) {
		@Override
		protected void beforeExecute(Thread t, Runnable r) {
			super.beforeExecute(t, r);
			if (r instanceof PictureJobList) {
				t.setName("PictureDL-"+((PictureJobList) r).url.hashCode());
			} else {
				t.setName("PictureDL-"+t);
			}
			t.setPriority(Thread.MIN_PRIORITY);
		}
	};

	/** Running downloads per URL/joblist pair */
	private final Hashtable<String, PictureJobList> mDownloadJobs = new Hashtable<String, PictureJobList>();
	private final PictureCache mCache;

	@SuppressLint("NewApi")
	public DownloadManager(PictureCache pictureCache) {
		this.mCache = pictureCache;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD)
			threadPool.allowCoreThreadTimeOut(true);
	}

	public void addDownloadTarget(PictureJob job) {
		// find out if that URL is already loading, if so add the view to the recipient
		boolean isNewJobList = false;
		PictureJobList jobList;
		synchronized (mDownloadJobs) {
			// add job by URL
            jobList = mDownloadJobs.get(job.url);
			if (null==jobList) {
				isNewJobList = true;
				if (DEBUG_DOWNLOADER) LogManager.getLogger().i(PictureCache.LOG_TAG, "add new downloader for "+job.url+" key:"+job.key+" job:"+job+" downloads:"+mDownloadJobs);
				// create a fresh new one if an old one is not ready to accept our loadHandler
				jobList = new PictureJobList(job, mCache, this);
				jobList.addJob(job);
				mDownloadJobs.put(job.url, jobList);
			} else {
				boolean jobAdded;
				//noinspection SynchronizationOnLocalVariableOrMethodParameter
				synchronized (jobList) { // avoid the downloader from aborting while we're adding a job
					if (DEBUG_DOWNLOADER) LogManager.getLogger().i(PictureCache.LOG_TAG, "add job:"+job+" to downloader:"+jobList);
					jobAdded = jobList.addJob(job);
				}
				if (!jobAdded) {
					if (DEBUG_DOWNLOADER) LogManager.getLogger().i(PictureCache.LOG_TAG, "add new downloader for "+job.url+" key:"+job.key+" job:"+job+" downloads:"+mDownloadJobs);
					// create a fresh new one if an old one is not ready to accept our loadHandler
					jobList = new PictureJobList(job, mCache, this);
					jobList.addJob(job);
					mDownloadJobs.put(job.url, jobList);
					isNewJobList = true;
				}
			}
		}

		if (isNewJobList)
			try {
				threadPool.execute(jobList);
			} catch (RejectedExecutionException e) {
				LogManager.getLogger().w(PictureCache.LOG_TAG, "can't execute "+jobList, e);
			}

		if (DEBUG_DOWNLOADER) {
			LogManager.getLogger().e(PictureCache.LOG_TAG, "downloader for "+job.url+" = "+jobList+" new task:"+isNewJobList);
		}
	}

	/**
	 * has to be done before a new {@link PictureLoaderHandler.setLoadingNewURL(DownloadManager, String, SimpleLogger)}
	 * @param job
	 * @param URL TODO
	 */
	public void removeDownloadTarget(PictureJob job, String URL) {
		if (DEBUG_DOWNLOADER) LogManager.getLogger().i(PictureCache.LOG_TAG, "removeDownloadTarget for "+URL+" job:"+job);

		synchronized (mDownloadJobs) {
			PictureJobList downloader = null;
			try {
				if (!TextUtils.isEmpty(URL)) {
					downloader = mDownloadJobs.get(URL);
					if (DEBUG_DOWNLOADER) LogManager.getLogger().i(PictureCache.LOG_TAG, " removeDownloadTarget job:"+job+" found:"+downloader);
					if (downloader!=null) {
						//LogManager.getLogger().d(PictureCache.TAG, "cancelDownloadForTarget for URL " + URL+" for "+loader);
						downloader.removeJob(job);
					}
					return;
				}

				// find the target by view
				//LogManager.getLogger().w(PictureCache.TAG, "cancelDownloadForTarget by key " + loader);
				Enumeration<PictureJobList> downloaders = mDownloadJobs.elements();
				while (downloaders.hasMoreElements()) {
					downloader = downloaders.nextElement();
					if (downloader.removeJob(job)) {
						if (DEBUG_DOWNLOADER) LogManager.getLogger().i(PictureCache.LOG_TAG, " removeDownloadTarget loadHandler:"+job+" deleted on:"+downloader/*+" url:"+url*/);
						return;
					}
				}
				downloader = null;
			} finally {
				if (null!=downloader && !downloader.hasTargets()) {
					if (DEBUG_DOWNLOADER) LogManager.getLogger().v(PictureCache.LOG_TAG, "last target removed, delete task:"+downloader);
					threadPool.remove(downloader);
					mDownloadJobs.remove(downloader.url);
				}
			}
		}

		if (DEBUG_DOWNLOADER) LogManager.getLogger().w(PictureCache.LOG_TAG, "removeDownloadTarget do nothing for loadHandler:"+job);
	}

	void onJobFinishedWithNewBitmaps(PictureJobList downloader, HashMap<CacheVariant,Drawable> newBitmaps) {
		mCache.onNewBitmapLoaded(newBitmaps, downloader.url, downloader.getItemDate(), downloader.getLifeSpan());

		synchronized (mDownloadJobs) {
			if (null != mDownloadJobs.remove(downloader.url)) {
				if (DEBUG_DOWNLOADER) LogManager.getLogger().i(PictureCache.LOG_TAG, "Job Finishing for "+downloader.url + " remaining:"+mDownloadJobs);
			} else {
				LogManager.getLogger().i(PictureCache.LOG_TAG, "Unknown job finishing for "+downloader.url + " remaining:"+mDownloadJobs);
			}
		}
	}
}