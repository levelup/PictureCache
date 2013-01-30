/**
 * 
 */
package com.levelup.picturecache;

/**
 * the life span of the item in the cache, can be {@link #SHORTTERM}, {@link #LONGTERM} or {@link #ETERNAL}
 */
public enum LifeSpan {
	/** the item can be removed from the cache as soon as we need room */
	SHORTTERM(0),

	/** the item remains as long as there is room for other long term and short term items */
	LONGTERM(1),

	/** the item should remain forever in the cache (unless there's no room for all eternal files) */
	ETERNAL(2);

	LifeSpan(int storageValue) {
		this.storageValue = storageValue;
	}

	private final int storageValue;

	/**
	 * 
	 * @param value
	 * @param otherValue
	 * @return true if value is lower (but not equal) to otherValue
	 */
	boolean isStrictlyLowerThan(LifeSpan otherValue) {
		return storageValue < otherValue.storageValue;
	}

	int toStorage() {
		return storageValue;
	}

	static LifeSpan fromStorage(int storedValue) {
		if (storedValue < 0 || storedValue >= LifeSpan.values().length) {
			LogManager.logger.w(PictureCache.TAG, "unknown cache life span value " + storedValue);
			return SHORTTERM;
		}
		return LifeSpan.values()[storedValue];
	}
}