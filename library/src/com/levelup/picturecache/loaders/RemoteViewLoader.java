package com.levelup.picturecache.loaders;

import java.io.File;

import uk.co.senab.bitmapcache.BitmapLruCache;
import android.graphics.drawable.Drawable;
import android.widget.RemoteViews;

import com.levelup.picturecache.PictureLoaderHandler;
import com.levelup.picturecache.transforms.bitmap.BitmapTransform;
import com.levelup.picturecache.transforms.storage.StorageTransform;


public class RemoteViewLoader extends PictureLoaderHandler {
	private final RemoteViews remoteViews;
	private final int viewId;
	private final int defaultResourceId;
	private String mLoadingUrl;

	public RemoteViewLoader(RemoteViews remoteViews, int viewId, int defaultDrawableResId, StorageTransform storageTransform, BitmapTransform loadTransform) {
		super(storageTransform, loadTransform);
		this.remoteViews = remoteViews;
		this.viewId = viewId;
		this.defaultResourceId = defaultDrawableResId;
	}

	@Override
	public void drawDefaultPicture(String url, BitmapLruCache drawableCache) {
		remoteViews.setImageViewResource(viewId, defaultResourceId);
	}

	@Override
	public void drawBitmap(Drawable bmp, String url, Object cookie, BitmapLruCache drawableCache) {
		remoteViews.setImageViewBitmap(viewId, ViewLoader.drawableToBitmap(bmp));
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
	public String setLoadingURL(String newURL, BitmapLruCache cache) {
		String oldLoadingUrl = mLoadingUrl;
		mLoadingUrl = newURL;
		return oldLoadingUrl;
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
