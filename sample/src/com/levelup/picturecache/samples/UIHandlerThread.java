package com.levelup.picturecache.samples;

import android.os.Handler;

import com.levelup.picturecache.UIHandler;

/**
 * basic class to run some code in the UI thread, based on {@link UIHandler}
 */
public final class UIHandlerThread extends Handler implements UIHandler {

	private final long mUIThread;
	
	/**
	 * constructor that should only be called from the UI thread
	 */
	public UIHandlerThread() {
		mUIThread = Thread.currentThread().getId();
	}
	
	@Override
	public void runOnUiThread(Runnable runnable) {
		if (isUIThread())
			runnable.run();
		else
			post(runnable);
	}

	@Override
	public boolean isUIThread() {
		return mUIThread == Thread.currentThread().getId();
	}

}
