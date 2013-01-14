package com.levelup.picturecache;

import java.io.File;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;

import com.levelup.HandlerUIThread;

public class ImageViewLoader extends PictureLoaderHandler {
	protected final ImageView view;
	protected final Drawable defaultDrawable;

	private static final long MAX_SIZE_IN_UI_THREAD = 19000;
	private static final boolean DEBUG_VIEW_LOADING = false;

	public static class ViewTag {
		private final String url;
		private final StorageTransform storageTransform;
		private final BitmapTransform displayTransform;

		private boolean isLoaded;
		private boolean isDefault;

		// pending draw data
		private Bitmap mPendingDraw;
		private String mPendingUrl;
		private DrawInUI mDrawInUI;


		public ViewTag(String url, StorageTransform storageTransform, BitmapTransform displayTransform) {
			this.url = url;
			this.displayTransform = displayTransform;
			this.storageTransform = storageTransform;
		}

		public void setPendingDraw(Bitmap pendingDraw, String pendingUrl) {
			if (mDrawInUI!=null)
				mDrawInUI.setPendingDraw(pendingDraw, pendingUrl);
			else {
				if (DEBUG_VIEW_LOADING) LogManager.logger.i(PictureCache.TAG, "temporary store pending draw:"+pendingDraw+" for "+pendingUrl);
				this.mPendingDraw = pendingDraw;
				this.mPendingUrl = pendingUrl;
			}
		}

		public boolean isUrlLoaded() {
			return isLoaded;
		}

		public void setUrlIsLoaded(boolean set) {
			isLoaded = set;
		}

		public boolean isDefault() {
			return isDefault;
		}

		public boolean setAndGetIsDefault(boolean set) {
			boolean old = isDefault;
			isDefault = set;
			return old;
		}

		@Override
		public boolean equals(Object o) {
			if (this==o) return true;
			if (!(o instanceof ViewTag)) return false;
			ViewTag tag = (ViewTag) o;
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
				synchronized (viewLoader) {
					this.mPendingDraw = pendingDraw;
					this.mPendingUrl = pendingUrl;
				}
			}

			@Override
			public void run() {
				synchronized (viewLoader) {
					boolean skipDrawing = false;
					final ViewTag tag = (ViewTag) viewLoader.view.getTag();
					if (tag!=null) {
						if (mPendingDraw!=null && mPendingUrl!=null && (tag.url==null || !mPendingUrl.equals(tag.url))) {
							skipDrawing = true;
							if (DEBUG_VIEW_LOADING) LogManager.logger.e(PictureCache.TAG, viewLoader+" skip drawing "+mPendingUrl+" instead of "+tag.url+" with "+mPendingDraw);
							//throw new IllegalStateException(ImageViewLoader.this+" try to draw "+mPendingUrl+" instead of "+tag.url+" with "+mPendingDraw);
						}
					}

					if (!skipDrawing) {
						boolean wasAlreadyDefault = false; // false: by default nothing is drawn
						if (tag!=null) {
							wasAlreadyDefault = tag.setAndGetIsDefault(mPendingDraw==null);
							tag.setUrlIsLoaded(mPendingDraw!=null);
						}

						if (DEBUG_VIEW_LOADING) LogManager.logger.e(PictureCache.TAG, viewLoader+" drawing "+(mPendingDraw==null ? "default view" : mPendingDraw)+" tag:"+tag);

						if (mPendingDraw==null) {
							if (!wasAlreadyDefault)
								viewLoader.displayDefaultView();
							else if (DEBUG_VIEW_LOADING) LogManager.logger.e(PictureCache.TAG, viewLoader+" saved a default drawing");
						} else
							viewLoader.displayCustomBitmap(mPendingDraw);
					}

					mPendingDraw = null;
				}
			}
		};

		synchronized void drawInView(HandlerUIThread postHandler, ImageViewLoader viewLoader) {
			if (mDrawInUI == null) {
				mDrawInUI = new DrawInUI(viewLoader);
				mDrawInUI.setPendingDraw(mPendingDraw, mPendingUrl);
				mPendingDraw = null;
				mPendingUrl = null;
			}

			if (DEBUG_VIEW_LOADING) LogManager.logger.i(PictureCache.TAG, viewLoader+" drawInView run mDrawInUI bitmap:"+mDrawInUI.mPendingDraw+" for "+mDrawInUI.mPendingUrl);
			postHandler.removeCallbacks(mDrawInUI);
			postHandler.runOnUIThread(mDrawInUI);
		}
	}

	public ImageViewLoader(ImageView view, Drawable defaultDrawable, StorageTransform storageTransform, BitmapTransform loadTransform) {
		super(storageTransform, loadTransform);
		if (view==null) throw new NullPointerException("empty view to load to, use PrecacheImageLoader");
		this.view = view;
		this.defaultDrawable = defaultDrawable;
	}

	public ImageView getView() {
		return view;
	}

	@Override
	public boolean equals(Object o) {
		if (o==this) return true;
		if (!(o instanceof ImageViewLoader)) return false;
		ImageViewLoader loader = (ImageViewLoader) o;
		//if (DEBUG_VIEW_LOADING && toString().equals(loader.toString())) Log.e("PlumeCache",this+" same equals "+loader+" = "+(loader.view==view && Float.compare(loader.mRotation,mRotation)==0 && loader.mRoundedCorner==mRoundedCorner));
		return loader.view==view && super.equals(loader);
	}

	@Override
	public int hashCode() {
		return super.hashCode() * 31 + view.hashCode();
	}

	@Override
	public String toString() {
		return getClass().getSimpleName()+"@"+hashCode()+(getStorageTransform()!=null ? getStorageTransform().getVariantPostfix() : "");
	}

	@Override
	public final void drawDefaultPicture(String url, HandlerUIThread postHandler) {
		if (DEBUG_VIEW_LOADING) LogManager.logger.d(PictureCache.TAG, this+" drawDefaultPicture");
		showDrawable(postHandler, null, url);
	}

	@Override
	public final void drawBitmap(Bitmap bmp, String url, HandlerUIThread postHandler) {
		if (DEBUG_VIEW_LOADING) LogManager.logger.d(PictureCache.TAG, this+" drawBitmap "+view+" with "+bmp);
		showDrawable(postHandler, bmp, url);
	}

	/**
	 * display the default view, called in the UI thread
	 * called under a lock on {@link view}
	 */
	protected void displayDefaultView() {
		view.setImageDrawable(defaultDrawable);
	}

	/**
	 * display this Bitmap in the view, called in the UI thread
	 * @param bmp the Bitmap to display in {@link view}
	 * called under a lock on {@link view}
	 */
	protected void displayCustomBitmap(Bitmap bmp) {
		view.setImageBitmap(bmp);
	}

	private void showDrawable(HandlerUIThread postHandler, Bitmap customBitmap, String url) {
		synchronized (this) {
			ViewTag tag = (ViewTag) view.getTag();
			if (tag==null) {
				tag = new ViewTag(url, getStorageTransform(), getDisplayTransform());
				view.setTag(tag);
			}
			tag.setPendingDraw(customBitmap, url);
			tag.drawInView(postHandler, this);
		}
	}

	@Override
	public boolean setLoadingNewURL(DownloadManager downloadManager, String newURL) {
		ViewTag newTag = new ViewTag(newURL, getStorageTransform(), getDisplayTransform());

		ViewTag currentTag = null;
		synchronized (this) {
			currentTag = (ViewTag) view.getTag();
			if (newTag.equals(currentTag)) {
				if (DEBUG_VIEW_LOADING) LogManager.logger.d(PictureCache.TAG, this+" setting the same picture in "+view+" isLoaded:"+currentTag.isUrlLoaded());
				return !currentTag.isUrlLoaded(); // no need to do anything
			}

			if (currentTag!=null) { // the previous URL loading is not good for this view anymore
				if (DEBUG_VIEW_LOADING) LogManager.logger.i(PictureCache.TAG, this+" the old picture in "+view+" doesn't match "+newURL+" was "+currentTag+" isDefault:"+currentTag.isDefault());
				// keep the previous state of the tag
				newTag.setAndGetIsDefault(currentTag.isDefault());
				newTag.mDrawInUI = currentTag.mDrawInUI;
			}

			view.setTag(newTag);
		}
		if (DEBUG_VIEW_LOADING) LogManager.logger.e(PictureCache.TAG, this+" set loading "+view+" with "+newURL+" tag:"+newTag);

		if (currentTag!=null) { // the previous URL loading is not good for this view anymore
			if (DEBUG_VIEW_LOADING) LogManager.logger.i(PictureCache.TAG, this+" the old picture in "+view+" doesn't match "+newURL+" was "+currentTag+" isDefault:"+currentTag.isDefault());
			boolean wasRunning = downloadManager.cancelDownloadForLoader(this, currentTag.url);
			if (wasRunning && DEBUG_VIEW_LOADING) LogManager.logger.w(PictureCache.TAG, this+" canceled a load running");
		}
		return true;
	}

	@Override
	public String getLoadingURL() {
		ViewTag tag = (ViewTag) view.getTag();
		if (tag==null)
			return null;
		return tag.url;
	}

	@Override
	protected boolean canDirectLoad(File file) {
		return !HandlerUIThread.isUIThread() || file.length() < MAX_SIZE_IN_UI_THREAD;
	}
}