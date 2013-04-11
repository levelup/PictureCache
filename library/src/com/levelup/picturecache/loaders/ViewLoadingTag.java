package com.levelup.picturecache.loaders;

import uk.co.senab.bitmapcache.BitmapLruCache;
import android.graphics.drawable.Drawable;
import android.os.Handler;

import com.levelup.picturecache.LogManager;
import com.levelup.picturecache.PictureCache;
import com.levelup.picturecache.UIHandler;
import com.levelup.picturecache.transforms.bitmap.BitmapTransform;
import com.levelup.picturecache.transforms.storage.StorageTransform;

class ViewLoadingTag {
	final String url;
	private final StorageTransform storageTransform;
	private final BitmapTransform displayTransform;
	private final BitmapLruCache cache;

	private boolean isLoaded;
	private boolean isDefault;

	// pending draw data
	private Drawable mPendingDraw;
	private String mPendingUrl;
	private DrawInUI mDrawInUI;

	ViewLoadingTag(BitmapLruCache cache, String url, StorageTransform storageTransform, BitmapTransform displayTransform) {
		this.cache = cache;
		this.url = url;
		this.displayTransform = displayTransform;
		this.storageTransform = storageTransform;
	}

	void setPendingDraw(Drawable pendingDraw, String pendingUrl) {
		if (mDrawInUI!=null)
			mDrawInUI.setPendingDraw(pendingDraw, pendingUrl);
		else {
			if (ViewLoader.DEBUG_VIEW_LOADING) LogManager.getLogger().i(PictureCache.LOG_TAG, "temporary store pending draw:"+pendingDraw+" for "+pendingUrl);
			this.mPendingDraw = pendingDraw;
			this.mPendingUrl = pendingUrl;
		}
	}

	boolean isUrlLoaded() {
		return isLoaded;
	}

	private void setUrlIsLoaded(boolean set) {
		isLoaded = set;
	}

	boolean isDefault() {
		return isDefault;
	}

	private boolean setAndGetIsDefault(boolean set) {
		boolean old = isDefault;
		isDefault = set;
		return old;
	}

	void recoverStateFrom(ViewLoadingTag oldTag) {
		setAndGetIsDefault(oldTag.isDefault());
		mDrawInUI = oldTag.mDrawInUI;
	}

	@Override
	public boolean equals(Object o) {
		if (this==o) return true;
		if (!(o instanceof ViewLoadingTag)) return false;
		ViewLoadingTag tag = (ViewLoadingTag) o;
		return url!=null && url.equals(tag.url)
				&& ((displayTransform==null && tag.displayTransform==null) || (displayTransform!=null && displayTransform.equals(tag.displayTransform)))
				&& ((storageTransform==null && tag.storageTransform==null) || (storageTransform!=null && storageTransform.equals(tag.storageTransform)))
				;
	}

	@Override
	public String toString() {
		return "ViewTag:"+url+(isDefault?"_def":"");
	}

	private static class DrawInUI implements Runnable {
		private final ViewLoader<?> viewLoader;
		private final BitmapLruCache cache;

		// pending draw data
		private Drawable mPendingDrawable;
		private String mPendingUrl;

		DrawInUI(ViewLoader<?> view, BitmapLruCache cache) {
			this.viewLoader = view;
			this.cache = cache;
		}

		public void setPendingDraw(Drawable pendingDraw, String pendingUrl) {
			synchronized (viewLoader.getImageView()) {
				this.mPendingDrawable = pendingDraw;
				this.mPendingUrl = pendingUrl;
			}
		}

		@Override
		public void run() {
			synchronized (viewLoader.getImageView()) {
				boolean skipDrawing = false;
				final ViewLoadingTag tag = (ViewLoadingTag) viewLoader.getImageView().getTag();
				if (tag!=null) {
					if (mPendingDrawable!=null && mPendingUrl!=null && (tag.url==null || !mPendingUrl.equals(tag.url))) {
						skipDrawing = true;
						if (ViewLoader.DEBUG_VIEW_LOADING) LogManager.getLogger().e(PictureCache.LOG_TAG, viewLoader+" skip drawing "+mPendingUrl+" instead of "+tag.url+" with "+mPendingDrawable);
						//throw new IllegalStateException(ImageViewLoader.this+" try to draw "+mPendingUrl+" instead of "+tag.url+" with "+mPendingDraw);
					}
				}

				if (!skipDrawing) {
					boolean wasAlreadyDefault = false; // false: by default nothing is drawn
					if (tag!=null) {
						wasAlreadyDefault = tag.setAndGetIsDefault(mPendingDrawable==null);
						tag.setUrlIsLoaded(mPendingDrawable!=null);
					}

					if (ViewLoader.DEBUG_VIEW_LOADING) LogManager.getLogger().e(PictureCache.LOG_TAG, this+" / "+viewLoader+" drawing "+(mPendingDrawable==null ? "default view" : mPendingDrawable)+" tag:"+tag);

					if (mPendingDrawable==null) {
						if (!wasAlreadyDefault)
							viewLoader.displayDefaultView(cache);
						else if (ViewLoader.DEBUG_VIEW_LOADING) LogManager.getLogger().e(PictureCache.LOG_TAG, viewLoader+" saved a default drawing");
					} else {
						viewLoader.displayCustomBitmap(mPendingDrawable);
						mPendingDrawable = null;
					}
				}
			}
		}
	};

	void drawInView(UIHandler postHandler, ViewLoader<?> viewLoader) {
		if (mDrawInUI == null) {
			if (ViewLoader.DEBUG_VIEW_LOADING) LogManager.getLogger().d(PictureCache.LOG_TAG, viewLoader+" create new DrawInUI with "+mPendingDraw+" for "+mPendingUrl);
			mDrawInUI = new DrawInUI(viewLoader, cache);
			mDrawInUI.setPendingDraw(mPendingDraw, mPendingUrl);
			mPendingDraw = null;
			mPendingUrl = null;
		}

		if (ViewLoader.DEBUG_VIEW_LOADING) LogManager.getLogger().i(PictureCache.LOG_TAG, mDrawInUI+" / "+viewLoader+" drawInView run mDrawInUI bitmap:"+mDrawInUI.mPendingDrawable+" for "+mDrawInUI.mPendingUrl);
		if (postHandler instanceof Handler)
			((Handler) postHandler).removeCallbacks(mDrawInUI);
		postHandler.runOnUiThread(mDrawInUI);
	}

	public boolean isBitmapPending() {
		return mPendingDraw!=null;
	}
}