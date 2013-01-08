package com.levelup;

import android.os.Handler;

public class HandlerUIThread extends Handler {
	private static long mUIThreadID = -1;
	
	public HandlerUIThread() {
		mUIThreadID = Thread.currentThread().getId();
	}
	
	public static boolean isUIThread() {
		return mUIThreadID == Thread.currentThread().getId();
	}
	
	public void runOnUIThread(Runnable runnable) {
		if (mUIThreadID == Thread.currentThread().getId())
			runnable.run();
		else
			post(runnable);
	}
}
