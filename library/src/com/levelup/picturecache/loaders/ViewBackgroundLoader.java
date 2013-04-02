package com.levelup.picturecache.loaders;

import java.io.File;

import android.graphics.drawable.Drawable;
import android.view.View;

import com.levelup.picturecache.PictureLoaderHandler;
import com.levelup.picturecache.UIHandler;
import com.levelup.picturecache.transforms.bitmap.BitmapTransform;
import com.levelup.picturecache.transforms.storage.StorageTransform;

/**
 * class used by the picture cache to display bitmaps on a {@link View},
 * the bitmap is set as the background of the view as with {@code android:background}
 * <p>
 * it also handles the Bitmaps transformations use for storage and/or display
 */
public class ViewBackgroundLoader<V extends View> extends PictureLoaderHandler {

	private final V view;
	private final int defaultResId;

	/**
	 * constructor of the {@link ViewBackgroundLoader}
	 * @param view the view on which the background will be set
	 * @param defaultResourceId the drawable resource ID to use while the bitmap is loading
	 * @param bitmapStorageTransform the transformation to use before storing the bitmap in the cache
	 * @param bitmapTransform the non-persistent transformation to use on the bitmap before displaying it
	 */
	public ViewBackgroundLoader(V view, int defaultResourceId, StorageTransform bitmapStorageTransform, BitmapTransform bitmapTransform) {
		super(bitmapStorageTransform, bitmapTransform);
		this.view = view;
		this.defaultResId = defaultResourceId;
	}

	private String mLoadingUrl;
	
	/**
	 * the view on which the background will be set
	 * @return
	 */
	protected V getView() {
		return view;
	}

	@Override
	protected void drawDefaultPicture(String url, UIHandler postHandler) {
		postHandler.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				// TODO use the ImageViewLoadingTag
				view.setBackgroundResource(defaultResId);
			}
		});
	}

	@Override
	protected void drawBitmap(final Drawable bmp, final String url, UIHandler postHandler) {
		postHandler.runOnUiThread(new Runnable() {
			@SuppressWarnings("deprecation")
			@Override
			public void run() {
				// TODO use the ImageViewLoadingTag
				if (url.equals(mLoadingUrl)) {
					view.setBackgroundDrawable(bmp);
				}
			}
		});
	}

	@Override
	protected String setLoadingURL(String newURL) {
		// TODO use the ImageViewLoadingTag
		String oldLoadingUrl = mLoadingUrl;
		mLoadingUrl = newURL;
		return oldLoadingUrl;
	}

	@Override
	protected String getLoadingURL() {
		// TODO use the ImageViewLoadingTag
		return mLoadingUrl;
	}

	@Override
	protected boolean canDirectLoad(File file, UIHandler uiHandler) {
		return true;
	}

	@Override
	public boolean equals(Object o) {
		if (o==this) return true;
		if (!(o instanceof ViewBackgroundLoader)) return false;
		ViewBackgroundLoader<?> loader = (ViewBackgroundLoader<?>) o;
		return loader.view==view && super.equals(loader);
	}
	
	@Override
	public int hashCode() {
		return view.hashCode()*31 + super.hashCode();
	}
}
