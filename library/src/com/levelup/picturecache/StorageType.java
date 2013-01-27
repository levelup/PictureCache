package com.levelup.picturecache;

/** indicate which format to use when storage the image in the cache */
public enum StorageType {
	/** select the storage format automatically based on the source file */
	AUTO,
	
	/** store the image in the cache as a JPEG */
	JPEG,
	
	/** store the image in the cache as a PNG */
	PNG;

	public static StorageType fromStorage(int storedValue) {
		if (storedValue<0 || storedValue>=StorageType.values().length) {
			LogManager.logger.w(PictureCache.TAG, "unknown cache life span value "+storedValue);
			return AUTO;
		}
		return StorageType.values()[storedValue];
	}

	public int toStorage() {
		return ordinal();
	}
}
