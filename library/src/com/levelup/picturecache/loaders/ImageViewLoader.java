package com.levelup.picturecache.loaders;

import java.io.File;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.widget.ImageView;

import com.levelup.picturecache.AbstractUIHandler;
import com.levelup.picturecache.LogManager;
import com.levelup.picturecache.PictureCache;
import com.levelup.picturecache.PictureLoaderHandler;
import com.levelup.picturecache.transforms.bitmap.BitmapTransform;
import com.levelup.picturecache.transforms.storage.StorageTransform;


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
				if (DEBUG_VIEW_LOADING) LogManager.getLogger().i(PictureCache.TAG, "temporary store pending draw:"+pendingDraw+" for "+pendingUrl);
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

		void recoverStateFrom(ViewTag oldTag) {
			setAndGetIsDefault(oldTag.isDefault());
			mDrawInUI = oldTag.mDrawInUI;
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
							if (DEBUG_VIEW_LOADING) LogManager.getLogger().e(PictureCache.TAG, viewLoader+" skip drawing "+mPendingUrl+" instead of "+tag.url+" with "+mPendingDraw);
							//throw new IllegalStateException(ImageViewLoader.this+" try to draw "+mPendingUrl+" instead of "+tag.url+" with "+mPendingDraw);
						}
					}

					if (!skipDrawing) {
						boolean wasAlreadyDefault = false; // false: by default nothing is drawn
						if (tag!=null) {
							wasAlreadyDefault = tag.setAndGetIsDefault(mPendingDraw==null);
							tag.setUrlIsLoaded(mPendingDraw!=null);
						}

						if (DEBUG_VIEW_LOADING) LogManager.getLogger().e(PictureCache.TAG, viewLoader+" drawing "+(mPendingDraw==null ? "default view" : mPendingDraw)+" tag:"+tag);

						if (mPendingDraw==null) {
							if (!wasAlreadyDefault)
								viewLoader.displayDefaultView();
							else if (DEBUG_VIEW_LOADING) LogManager.getLogger().e(PictureCache.TAG, viewLoader+" saved a default drawing");
						} else
							viewLoader.displayCustomBitmap(mPendingDraw);
					}

					mPendingDraw = null;
				}
			}
		};

		synchronized void drawInView(AbstractUIHandler postHandler, ImageViewLoader viewLoader) {
			if (mDrawInUI == null) {
				mDrawInUI = new DrawInUI(viewLoader);
				mDrawInUI.setPendingDraw(mPendingDraw, mPendingUrl);
				mPendingDraw = null;
				mPendingUrl = null;
			}

			if (DEBUG_VIEW_LOADING) LogManager.getLogger().i(PictureCache.TAG, viewLoader+" drawInView run mDrawInUI bitmap:"+mDrawInUI.mPendingDraw+" for "+mDrawInUI.mPendingUrl);
			if (postHandler instanceof Handler)
				((Handler) postHandler).removeCallbacks(mDrawInUI);
			postHandler.runOnUiThread(mDrawInUI);
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
	public final void drawDefaultPicture(String url, AbstractUIHandler postHandler) {
		if (DEBUG_VIEW_LOADING) LogManager.getLogger().d(PictureCache.TAG, this+" drawDefaultPicture");
		showDrawable(postHandler, null, url);
	}

	@Override
	public final void drawBitmap(Bitmap bmp, String url, AbstractUIHandler postHandler) {
		if (DEBUG_VIEW_LOADING) LogManager.getLogger().d(PictureCache.TAG, this+" drawBitmap "+view+" with "+bmp);
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

	private void showDrawable(AbstractUIHandler postHandler, Bitmap customBitmap, String url) {
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
	public String setLoadingURL(String newURL) {
		ViewTag newTag = new ViewTag(newURL, getStorageTransform(), getDisplayTransform());

		ViewTag oldTag = null;
		synchronized (this) {
			oldTag = (ViewTag) view.getTag();
			if (newTag.equals(oldTag)) {
				if (DEBUG_VIEW_LOADING) LogManager.getLogger().d(PictureCache.TAG, this+" setting the same picture in "+view+" isLoaded:"+oldTag.isUrlLoaded());
				return newURL; // no need to do anything
			}

			if (oldTag!=null) {
				if (DEBUG_VIEW_LOADING) LogManager.getLogger().i(PictureCache.TAG, this+" the old picture in "+view+" doesn't match "+newURL+" was "+oldTag+" isDefault:"+oldTag.isDefault());
				// keep the previous state of the tag
				newTag.recoverStateFrom(oldTag);
			}

			view.setTag(newTag);
		}
		if (DEBUG_VIEW_LOADING) LogManager.getLogger().e(PictureCache.TAG, this+" set loading "+view+" with "+newURL+" tag:"+newTag);

		if (oldTag!=null) {
			if (DEBUG_VIEW_LOADING)
				// the previous URL loading is not good for this view anymore
				LogManager.getLogger().i(PictureCache.TAG, this+" the old picture in "+view+" doesn't match "+newURL+" was "+oldTag+" isDefault:"+oldTag.isDefault());
		}

		return oldTag!=null ? oldTag.url : null;
	}

	@Override
	public String getLoadingURL() {
		ViewTag tag = (ViewTag) view.getTag();
		if (tag==null)
			return null;
		return tag.url;
	}

	@Override
	protected boolean canDirectLoad(File file, AbstractUIHandler uiHandler) {
		return !uiHandler.isUIThread() || file.length() < MAX_SIZE_IN_UI_THREAD;
	}
}