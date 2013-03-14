package com.levelup.picturecache.loaders;

import android.graphics.Bitmap;
import android.os.Handler;

import com.levelup.picturecache.UIHandler;
import com.levelup.picturecache.LogManager;
import com.levelup.picturecache.PictureCache;
import com.levelup.picturecache.transforms.bitmap.BitmapTransform;
import com.levelup.picturecache.transforms.storage.StorageTransform;

class ImageViewLoadingTag {
	final String url;
	private final StorageTransform storageTransform;
	private final BitmapTransform displayTransform;

	private boolean isLoaded;
	private boolean isDefault;

	// pending draw data
	private Bitmap mPendingDraw;
	private String mPendingUrl;
	private DrawInUI mDrawInUI;

	ImageViewLoadingTag(String url, StorageTransform storageTransform, BitmapTransform displayTransform) {
		this.url = url;
		this.displayTransform = displayTransform;
		this.storageTransform = storageTransform;
	}

	void setPendingDraw(Bitmap pendingDraw, String pendingUrl) {
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

	void recoverStateFrom(ImageViewLoadingTag oldTag) {
		setAndGetIsDefault(oldTag.isDefault());
		mDrawInUI = oldTag.mDrawInUI;
	}

	@Override
	public boolean equals(Object o) {
		if (this==o) return true;
		if (!(o instanceof ImageViewLoadingTag)) return false;
		ImageViewLoadingTag tag = (ImageViewLoadingTag) o;
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
		private final ImageViewLoader viewLoader;

		// pending draw data
		private Bitmap mPendingDraw;
		private String mPendingUrl;

		DrawInUI(ImageViewLoader view) {
			this.viewLoader = view;
		}

		public void setPendingDraw(Bitmap pendingDraw, String pendingUrl) {
			synchronized (viewLoader.getImageView()) {
				this.mPendingDraw = pendingDraw;
				this.mPendingUrl = pendingUrl;
			}
		}

		@Override
		public void run() {
			synchronized (viewLoader.getImageView()) {
				boolean skipDrawing = false;
				final ImageViewLoadingTag tag = (ImageViewLoadingTag) viewLoader.getImageView().getTag();
				if (tag!=null) {
					if (mPendingDraw!=null && mPendingUrl!=null && (tag.url==null || !mPendingUrl.equals(tag.url))) {
						skipDrawing = true;
						if (ViewLoader.DEBUG_VIEW_LOADING) LogManager.getLogger().e(PictureCache.LOG_TAG, viewLoader+" skip drawing "+mPendingUrl+" instead of "+tag.url+" with "+mPendingDraw);
						//throw new IllegalStateException(ImageViewLoader.this+" try to draw "+mPendingUrl+" instead of "+tag.url+" with "+mPendingDraw);
					}
				}

				if (!skipDrawing) {
					boolean wasAlreadyDefault = false; // false: by default nothing is drawn
					if (tag!=null) {
						wasAlreadyDefault = tag.setAndGetIsDefault(mPendingDraw==null);
						tag.setUrlIsLoaded(mPendingDraw!=null);
					}

					if (ViewLoader.DEBUG_VIEW_LOADING) LogManager.getLogger().e(PictureCache.LOG_TAG, this+" / "+viewLoader+" drawing "+(mPendingDraw==null ? "default view" : mPendingDraw)+" tag:"+tag);

					if (mPendingDraw==null) {
						if (!wasAlreadyDefault)
							viewLoader.displayDefaultView();
						else if (ViewLoader.DEBUG_VIEW_LOADING) LogManager.getLogger().e(PictureCache.LOG_TAG, viewLoader+" saved a default drawing");
					} else
						viewLoader.displayCustomBitmap(mPendingDraw);
				}

				//TODO: could cause memory leaks ?  mPendingDraw = null;
			}
		}
	};

	void drawInView(UIHandler postHandler, ImageViewLoader viewLoader) {
		if (mDrawInUI == null) {
			if (ViewLoader.DEBUG_VIEW_LOADING) LogManager.getLogger().d(PictureCache.LOG_TAG, viewLoader+" create new DrawInUI with "+mPendingDraw+" for "+mPendingUrl);
			mDrawInUI = new DrawInUI(viewLoader);
			mDrawInUI.setPendingDraw(mPendingDraw, mPendingUrl);
			mPendingDraw = null;
			mPendingUrl = null;
		}

		if (ViewLoader.DEBUG_VIEW_LOADING) LogManager.getLogger().i(PictureCache.LOG_TAG, mDrawInUI+" / "+viewLoader+" drawInView run mDrawInUI bitmap:"+mDrawInUI.mPendingDraw+" for "+mDrawInUI.mPendingUrl);
		if (postHandler instanceof Handler)
			((Handler) postHandler).removeCallbacks(mDrawInUI);
		postHandler.runOnUiThread(mDrawInUI);
	}

	public boolean isBitmapPending() {
		return mPendingDraw!=null;
	}
}