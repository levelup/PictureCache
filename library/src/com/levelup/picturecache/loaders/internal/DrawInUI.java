package com.levelup.picturecache.loaders.internal;

import uk.co.senab.bitmapcache.BitmapLruCache;
import android.graphics.drawable.Drawable;

import com.levelup.picturecache.LogManager;
import com.levelup.picturecache.PictureCache;
import com.levelup.picturecache.loaders.ViewLoader;

class DrawInUI implements Runnable {
	private final ViewLoader<?> viewLoader;
	private final BitmapLruCache cache;

	// pending draw data
	DrawType mPendingDrawType;
	Drawable mPendingDrawable;
	String mPendingUrl;

	DrawInUI(ViewLoader<?> view, BitmapLruCache cache) {
		this.viewLoader = view;
		this.cache = cache;
	}

	public void setPendingDrawable(Drawable pendingDrawable, String forUrl, DrawType drawType) {
		synchronized (viewLoader.getImageView()) {
			this.mPendingDrawable = pendingDrawable;
			this.mPendingUrl = forUrl;
			this.mPendingDrawType = drawType;
		}
	}

	@Override
	public void run() {
		synchronized (viewLoader.getImageView()) {
			boolean skipDrawing = false;
			final ViewLoadingTag tag = (ViewLoadingTag) viewLoader.getImageView().getTag();
			if (tag!=null) {
				if (mPendingDrawType==tag.getDrawType() && mPendingUrl!=null && (tag.url==null || !mPendingUrl.equals(tag.url))) {
					skipDrawing = true;
					if (ViewLoader.DEBUG_VIEW_LOADING) LogManager.getLogger().e(PictureCache.LOG_TAG, viewLoader+" skip drawing "+mPendingUrl+" instead of "+tag.url+" with "+mPendingDrawable);
					//throw new IllegalStateException(ImageViewLoader.this+" try to draw "+mPendingUrl+" instead of "+tag.url+" with "+mPendingDraw);
				}
			}

			if (!skipDrawing) {
				if (ViewLoader.DEBUG_VIEW_LOADING) LogManager.getLogger().e(PictureCache.LOG_TAG, this+" / "+viewLoader+" drawing "+(mPendingDrawable==null ? "default view" : mPendingDrawable)+" tag:"+tag);

				if (mPendingDrawType==DrawType.DEFAULT) {
					if (tag.setDrawType(mPendingDrawType)!=DrawType.DEFAULT) {
						viewLoader.displayDefaultView(cache);
					}
					else if (ViewLoader.DEBUG_VIEW_LOADING) LogManager.getLogger().e(PictureCache.LOG_TAG, viewLoader+" saved a default drawing");
				} else if (mPendingDrawType==DrawType.ERROR) {
					if (tag.setDrawType(mPendingDrawType)!=DrawType.ERROR) {
						viewLoader.displayErrorView(cache);
					}
					else if (ViewLoader.DEBUG_VIEW_LOADING) LogManager.getLogger().e(PictureCache.LOG_TAG, viewLoader+" saved an error drawing");
				} else {
					tag.setDrawType(mPendingDrawType);
					viewLoader.displayCustomBitmap(mPendingDrawable);
					mPendingDrawable = null;
				}
			}
		}
	}
}