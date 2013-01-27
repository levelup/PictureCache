package com.levelup.picturecache.loaders;

import android.graphics.drawable.Drawable;
import android.widget.ImageView;

import com.levelup.picturecache.transforms.bitmap.BitmapTransform;
import com.levelup.picturecache.transforms.storage.StorageTransform;

public class ImageViewLoaderDefaultDrawable extends ImageViewLoader {
	protected final Drawable defaultDrawable;

	public ImageViewLoaderDefaultDrawable(ImageView view, Drawable defaultDrawable, StorageTransform storageTransform, BitmapTransform loadTransform) {
		super(view, storageTransform, loadTransform);
		this.defaultDrawable = defaultDrawable;
	}

	/**
	 * display the default view, called in the UI thread
	 * called under a lock on {@link view}
	 */
	protected void displayDefaultView() {
		getImageView().setImageDrawable(defaultDrawable);
	}
}