package com.levelup.picturecache.loaders;

import android.widget.ImageView;

import com.levelup.picturecache.transforms.bitmap.BitmapTransform;
import com.levelup.picturecache.transforms.storage.StorageTransform;


public class ImageViewLoaderDefaultResource extends ImageViewLoader {
	protected final int defaultDrawable;

	public ImageViewLoaderDefaultResource(ImageView view, int defaultResourceId, StorageTransform storageTransform, BitmapTransform loadTransform) {
		super(view, storageTransform, loadTransform);
		this.defaultDrawable = defaultResourceId;
	}

	/**
	 * display the default view, called in the UI thread
	 * called under a lock on {@link view}
	 */
	protected void displayDefaultView() {
		getImageView().setImageResource(defaultDrawable);
	}
}