package com.levelup.picturecache;

/** indicate which format to use when storage the image in the cache */
public enum StorageType {
	/** select the storage format automatically based on the source file */
	AUTO(0),

	/** store the image in the cache as a JPEG */
	JPEG(1),

	/** store the image in the cache as a PNG */
	PNG(2);

	private StorageType(int storageValue) {
		this.storageValue = storageValue;
	}

	private final int storageValue;

	/**
	 * get the {@link StorageType} for the stored value
	 * @param storedValue
	 * @see {@link #toStorage()}
	 * @return
	 */
	public static StorageType fromStorage(int storedValue) {
		for (StorageType lifeSpan : StorageType.values()) {
			if (lifeSpan.storageValue == storedValue)
				return lifeSpan;
		}
		LogManager.logger.w(PictureCache.LOG_TAG, "unknown cache life span value " + storedValue);
		return AUTO;
	}

	/**
	 * get the value that can be stored persistently
	 * @see also {@link #fromStorage(int)}} 
	 * @return
	 */
	public int toStorage() {
		return storageValue;
	}
}
