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

	private LifeSpan(int storageValue) {
		this.storageValue = storageValue;
	}

	private final int storageValue;

	/**
	 * compare {@link LifeSpan} items like Comparable does
	 * @param o item to compare with
	 * @return 0 if equals, negative if o has a longer life span, positive otherwise
	 */
	public int compare(LifeSpan o) {
		return storageValue - o.storageValue;
	}
	
	/**
	 * get the value that can be stored persistently
	 * @see also {@link #fromStorage(int)}} 
	 * @return
	 */
	int toStorage() {
		return storageValue;
	}

	/**
	 * get the {@link LifeSpan} for the stored value
	 * @param storedValue
	 * @see {@link #toStorage()}
	 * @return
	 */
	static LifeSpan fromStorage(int storedValue) {
		for (LifeSpan lifeSpan : LifeSpan.values()) {
			if (lifeSpan.storageValue == storedValue)
				return lifeSpan;
		}
		LogManager.logger.w(PictureCache.TAG, "unknown cache life span value " + storedValue);
		return SHORTTERM;
	}
}