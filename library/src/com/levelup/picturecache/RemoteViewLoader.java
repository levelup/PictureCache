package com.levelup.picturecache;

import java.io.File;

import android.graphics.Bitmap;
import android.widget.RemoteViews;

import com.levelup.HandlerUIThread;
import com.levelup.SimpleLogger;

public class RemoteViewLoader extends PictureLoaderHandler {
	private final RemoteViews remoteViews;
	private final int viewId;
	private final int defaultView;
	private String mLoadingUrl;

	public RemoteViewLoader(RemoteViews remoteViews, int viewId, int defaultView, StorageTransform storageTransform, BitmapTransform loadTransform) {
		super(storageTransform, loadTransform);
		this.remoteViews = remoteViews;
		this.viewId = viewId;
		this.defaultView = defaultView;
	}

	@Override
	public void drawDefaultPicture(String url, HandlerUIThread postHandler, SimpleLogger logger) {
		remoteViews.setImageViewResource(viewId, defaultView);
	}

	@Override
	public void drawBitmap(Bitmap bmp, String url, HandlerUIThread postHandler, SimpleLogger logger) {
		remoteViews.setImageViewBitmap(viewId, bmp);
	}

	@Override
	public boolean equals(Object o) {
		if (o==this) return true;
		if (!(o instanceof RemoteViewLoader)) return false;
		RemoteViewLoader loader = (RemoteViewLoader) o;
		return viewId==loader.viewId && remoteViews==loader.remoteViews && super.equals(loader);
	}

	@Override
	public int hashCode() {
		return (super.hashCode()*31 + viewId) * 31 + remoteViews.hashCode();
	}

	@Override
	public String toString() {
		return remoteViews.toString()+":"+viewId;
	}

	@Override
	public boolean setLoadingNewURL(DownloadManager downloadManager, String newURL, SimpleLogger logger) {
		if (newURL!=null && newURL.equals(mLoadingUrl))
			return false;
		downloadManager.cancelDownloadForLoader(this, mLoadingUrl);
		mLoadingUrl = newURL;
		return true;
	}

	@Override
	public String getLoadingURL() {
		return mLoadingUrl;
	}

	@Override
	protected boolean canDirectLoad(File file) {
		return true;
	}
}
