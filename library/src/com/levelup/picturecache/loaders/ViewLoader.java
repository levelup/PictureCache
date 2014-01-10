package com.levelup.picturecache.loaders;

import java.io.File;
import java.security.InvalidParameterException;

import uk.co.senab.bitmapcache.BitmapLruCache;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.View;
import android.widget.ImageView;

import com.levelup.picturecache.BuildConfig;
import com.levelup.picturecache.LogManager;
import com.levelup.picturecache.PictureCache;
import com.levelup.picturecache.PictureLoaderHandler;
import com.levelup.picturecache.UIHandler;
import com.levelup.picturecache.loaders.internal.ImageViewReference;
import com.levelup.picturecache.loaders.internal.ImageViewReferenceSDK12;
import com.levelup.picturecache.loaders.internal.ViewLoadingTag;
import com.levelup.picturecache.loaders.internal.ViewReference;
import com.levelup.picturecache.transforms.bitmap.BitmapTransform;
import com.levelup.picturecache.transforms.storage.StorageTransform;

/**
 * Base class used to display the loaded/default bitmap in an View
 * @see {@link ViewLoaderDefaultResource} and {@link ViewLoaderDefaultDrawable} 
 */
public abstract class ViewLoader<T extends View> extends PictureLoaderHandler {
	private final ViewReference<T> view;

	private static final long MAX_SIZE_IN_UI_THREAD = 19000;
	public static final boolean DEBUG_VIEW_LOADING = BuildConfig.DEBUG && false;

	public ViewLoader(T view, StorageTransform storageTransform, BitmapTransform loadTransform) {
		super(storageTransform, loadTransform);
		if (view==null) throw new NullPointerException("empty view to load to, use PrecacheImageLoader");
		this.view = createViewReference(view);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected ViewReference<T> createViewReference(T view) {
		if (!(view instanceof ImageView)) throw new InvalidParameterException("You need to override createViewReference() in your loader to handle non ImageView targets like "+view);
		if (Build.VERSION.SDK_INT >= 12) {
			return new ImageViewReferenceSDK12((ImageView) view);
		} else {
			return new ImageViewReference((ImageView) view);
		}
	}

	public T getImageView() {
		return view.getImageView();
	}

	@Override
	public boolean equals(Object o) {
		if (o==this) return true;
		if (!(o instanceof ViewLoader)) return false;
		ViewLoader<?> loader = (ViewLoader<?>) o;
		//if (DEBUG_VIEW_LOADING && toString().equals(loader.toString())) Log.e("PlumeCache",this+" same equals "+loader+" = "+(loader.view==view && Float.compare(loader.mRotation,mRotation)==0 && loader.mRoundedCorner==mRoundedCorner));
		return loader.view.equals(view) && super.equals(loader);
	}

	@Override
	public int hashCode() {
		return super.hashCode() * 31 + view.hashCode();
	}

	@Override
	public String toString() {
		return getClass().getSimpleName()+"@"+hashCode()+(getStorageTransform()!=null ? getStorageTransform().getVariantPostfix() : "");
	}

	/**
	 * To override the default display, use {@link #displayDefaultView(BitmapLruCache)}
	 */
	@Override
	public final void drawDefaultPicture(String url, BitmapLruCache drawableCache) {
		if (DEBUG_VIEW_LOADING) LogManager.getLogger().d(PictureCache.LOG_TAG, this+" drawDefaultPicture");
		showDrawable(drawableCache, null, url);
	}

	/**
	 * To override the drawable display, use {@link #displayCustomBitmap(Drawable)}
	 */
	@Override
	public final void drawBitmap(Drawable bmp, String url, Object cookie, BitmapLruCache drawableCache) {
		if (DEBUG_VIEW_LOADING) LogManager.getLogger().d(PictureCache.LOG_TAG, this+" drawBitmap "+view+" with "+bmp);
		showDrawable(drawableCache, bmp, url);
	}

	/**
	 * Display the default view, called in the UI thread
	 * <p>called under a lock on {@link view}</p>
	 * @param cache the bitmap cache
	 */
	public abstract void displayDefaultView(BitmapLruCache cache);

	/**
	 * Display this Bitmap in the view, called in the UI thread
	 * <p>called under a lock on {@link view}</p>
	 * @param pendingDrawable Drawable to display in {@link view}
	 */
	public void displayCustomBitmap(Drawable pendingDrawable) {
		view.setImageDrawable(pendingDrawable);
	}

	private void showDrawable(BitmapLruCache cache, Drawable customBitmap, String url) {
		synchronized (view.getImageView()) {
			ViewLoadingTag tag = view.getTag();
			if (tag==null) {
				tag = new ViewLoadingTag(cache, url, getStorageTransform(), getDisplayTransform());
				view.setTag(tag);
			}
			tag.setPendingDraw(customBitmap, url);
			tag.drawInView(this);
		}
	}

	@Override
	public String setLoadingURL(String newURL, BitmapLruCache cache) {
		ViewLoadingTag newTag = new ViewLoadingTag(cache, newURL, getStorageTransform(), getDisplayTransform());

		ViewLoadingTag oldTag = null;
		synchronized (view.getImageView()) {
			oldTag = view.getTag();
			if (newTag.equals(oldTag)) {
				if (oldTag.isUrlLoaded() || oldTag.isBitmapPending()) {
					if (DEBUG_VIEW_LOADING) LogManager.getLogger().d(PictureCache.LOG_TAG, this+" setting the same picture in "+view+" isLoaded:"+oldTag.isUrlLoaded()+" drawPending:"+oldTag.isBitmapPending());
					return newURL; // no need to do anything
				}
				return null; // hack for now as the PictureCache will consider it's the same URL and do nothing, but it's the same URL loading as before
			}

			if (oldTag!=null) {
				// the previous URL loading is not good for this view anymore
				if (DEBUG_VIEW_LOADING) LogManager.getLogger().i(PictureCache.LOG_TAG, this+" the old picture in "+view+" doesn't match "+newURL+" was "+oldTag+" isLoaded:"+oldTag.isUrlLoaded()+" drawPending:"+oldTag.isBitmapPending());
				// keep the previous state of the tag
				newTag.recoverStateFrom(oldTag);
			}

			view.setTag(newTag);
		}
		if (DEBUG_VIEW_LOADING) LogManager.getLogger().e(PictureCache.LOG_TAG, this+" set loading "+view+" with "+newURL+" tag:"+newTag);


		if (oldTag==null || oldTag.url==null)
			return null;
		if (oldTag.url.equals(newURL))
			return null; // hack for now as the PictureCache will consider it's the same URL and do nothing, but the transforms have changed
		return oldTag.url;
	}

	@Override
	public String getLoadingURL() {
		ViewLoadingTag tag = view.getTag();
		if (tag==null)
			return null;
		return tag.url;
	}

	@Override
	protected boolean canDirectLoad(File file) {
		return !UIHandler.isUIThread() || file.length() < MAX_SIZE_IN_UI_THREAD;
	}

	public static Bitmap drawableToBitmap(Drawable drawable) {
		if (drawable instanceof BitmapDrawable) {
			return ((BitmapDrawable)drawable).getBitmap();
		}

		int width = drawable.getIntrinsicWidth();
		width = width > 0 ? width : 1;
		int height = drawable.getIntrinsicHeight();
		height = height > 0 ? height : 1;

		Bitmap bitmap = Bitmap.createBitmap(width, height, Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmap); 
		drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
		drawable.draw(canvas);

		return bitmap;
	}
}