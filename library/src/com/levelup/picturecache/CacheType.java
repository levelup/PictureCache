/**
 * 
 */
package com.levelup.picturecache;

/**
 * the lifespan of the item in the cache, can be {@link #CACHE_SHORTTERM},  {@link #CACHE_LONGTERM} or {@link #CACHE_ETERNAL}
 */
public class CacheType {
	/**
	 * the item can be removed from the cache as soon as we need room
	 */
	public static final int CACHE_SHORTTERM = 0;
	/**
	 * the item remains as long as there is room for other long term and short term items
	 */
	public static final int CACHE_LONGTERM = 1;
	/**
	 * the item should remain forever in the cache (unless there's no room for all eternal files)
	 */
	public static final int CACHE_ETERNAL = 2;

	/**
	 * 
	 * @param value
	 * @param otherValue
	 * @return true if value is lower (but not equal) to otherValue
	 */
	static boolean isStrictlyLowerThan(int value, int otherValue) {
		return value < otherValue;
	}
}