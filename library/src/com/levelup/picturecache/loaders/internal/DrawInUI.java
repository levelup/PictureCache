package com.levelup.picturecache.loaders.internal;

import android.graphics.drawable.Drawable;

import com.levelup.picturecache.LogManager;
import com.levelup.picturecache.PictureCache;
import com.levelup.picturecache.loaders.ViewLoader;

class DrawInUI implements Runnable {
	final ViewLoader<?> viewLoader;

	// pending draw data
	DrawType mPendingDrawType;
	Drawable mPendingDrawable;
	String mPendingUrl;

	DrawInUI(ViewLoader<?> view) {
		this.viewLoader = view;
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
			final ViewLoadingTag tag = (ViewLoadingTag) viewLoader.getImageView().getTag();
			if (tag!=null) {
				if (mPendingDrawType==tag.getDrawnType() && mPendingUrl!=null && (tag.url==null || !mPendingUrl.equals(tag.url))) {
					if (ViewLoader.DEBUG_VIEW_LOADING) LogManager.getLogger().e(PictureCache.LOG_TAG, viewLoader+" skip drawing "+mPendingUrl+" instead of "+tag.url+" with "+mPendingDrawable);
					//throw new IllegalStateException(ImageViewLoader.this+" try to draw "+mPendingUrl+" instead of "+tag.url+" with "+mPendingDraw);
				} else {
					if (ViewLoader.DEBUG_VIEW_LOADING) LogManager.getLogger().e(PictureCache.LOG_TAG, this+" / "+viewLoader+" drawing "+(mPendingDrawable==null ? "default view" : mPendingDrawable)+" tag:"+tag);

					if (mPendingDrawType==DrawType.DEFAULT) {
						if (tag.setDrawnType(mPendingDrawType)!=DrawType.DEFAULT) {
							viewLoader.displayDefaultView(tag.bitmapCache);
							tag.setUrlIsLoaded(false);
						}
						else if (ViewLoader.DEBUG_VIEW_LOADING) LogManager.getLogger().e(PictureCache.LOG_TAG, viewLoader+" saved a default drawing");
					} else if (mPendingDrawType==DrawType.ERROR) {
						if (tag.setDrawnType(mPendingDrawType)!=DrawType.ERROR) {
							viewLoader.displayErrorView(tag.bitmapCache);
							tag.setUrlIsLoaded(true);
						}
						else if (ViewLoader.DEBUG_VIEW_LOADING) LogManager.getLogger().e(PictureCache.LOG_TAG, viewLoader+" saved an error drawing");
					} else {
						tag.setDrawnType(mPendingDrawType);
						viewLoader.displayLoadedDrawable(mPendingDrawable);
						tag.setUrlIsLoaded(true);
						mPendingDrawable = null;
					}
				}
			}
		}
	}
}