package com.levelup.picturecache;

import com.levelup.picturecache.transforms.bitmap.BitmapTransform;
import com.levelup.picturecache.transforms.storage.StorageTransform;

public interface PictureJobTransforms {
	StorageTransform getStorageTransform();
	
	BitmapTransform getDisplayTransform();
	
}
