package com.levelup.picturecache;

import java.io.File;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.MediaScannerConnectionClient;
import android.net.Uri;

public class GalleryScanner implements MediaScannerConnectionClient {
	final private MediaScannerConnection mScanner;
	private File mDst;
	//CountDownLatch mLock;
	
	public GalleryScanner(Context context) {
		mScanner = new MediaScannerConnection(context, this);
	}
	
	public void scan(File dst) {
		mDst = dst;
		//mLock = new CountDownLatch(1);
		mScanner.connect();
	}

	public void onMediaScannerConnected() {
		mScanner.scanFile(mDst.getAbsolutePath(), null);
	}

	public void onScanCompleted(String path, Uri uri) {
		mScanner.disconnect();
		//mLock.countDown();
	}
}
