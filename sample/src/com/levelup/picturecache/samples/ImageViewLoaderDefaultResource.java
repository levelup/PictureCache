package com.levelup.picturecache.samples;

import android.widget.ImageView;

import com.levelup.picturecache.ImageViewLoader;
import com.levelup.picturecache.transforms.bitmap.BitmapTransform;
import com.levelup.picturecache.transforms.storage.StorageTransform;

/** an {@link ImageViewLoader} that reads its default drawable from the resources */
public class ImageViewLoaderDefaultResource extends ImageViewLoader {

	private final int defaultDrawableResId;
	
	public ImageViewLoaderDefaultResource(ImageView view, int defaultId, StorageTransform storageTransform, BitmapTransform loadTransform) {
		super(view, null, storageTransform, loadTransform);
		
		defaultDrawableResId = defaultId;
	}
	
	@Override
	protected void displayDefaultView() {
		view.setImageResource(defaultDrawableResId);
	}

}
