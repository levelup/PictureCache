package com.levelup.picturecache;

import java.security.NoSuchAlgorithmException;

import android.text.TextUtils;

public class PictureJobBuilder {
	private String mURL;
	private String mUUID;
	private long mFreshDate;
	private int mLifeType;
	private int mDimension;
	private boolean mWidthBased;
	private int mExtensionMode;
	protected final PictureLoaderHandler mHandler;
	protected float mRotation;

	protected PictureJobBuilder(PictureLoaderHandler handler) {
		this.mHandler = handler;
	}

	public PictureJobBuilder setURL(String URL) {
		mURL = URL;
		return this;
	}

	public PictureJobBuilder setUUID(String UUID) {
		mUUID = UUID;
		return this;
	}

	/**
	 * set for how long the item should remain in the cache
	 * <p>
	 * see {@link CacheType}
	 * @param type can be {@link CacheType#CACHE_SHORTTERM},  {@link CacheType#CACHE_LONGTERM} or {@link CacheType#CACHE_ETERNAL}
	 * @return
	 */
	public PictureJobBuilder setLifeType(int type) {
		mLifeType = type;
		return this;
	}

	public PictureJobBuilder setFreshDate(long date) {
		mFreshDate = date;
		return this;
	}

	public PictureJobBuilder setDimension(int dimension, boolean widthBased) {
		mDimension = dimension;
		mWidthBased = widthBased;
		return this;
	}

	public PictureJobBuilder setExtensionMode(int extensionMode) {
		mExtensionMode = extensionMode;
		return this;
	}

	public PictureJobBuilder setRotation(float rotation) {
		mRotation = rotation;
		return this;
	}

	CacheKey buildKey() throws NoSuchAlgorithmException {
		if (!TextUtils.isEmpty(mUUID))
			return CacheKey.newUUIDBasedKey(mUUID, mDimension, mWidthBased, mExtensionMode, mHandler.getStorageTransform()!=null ? mHandler.getStorageTransform().getVariantPostfix() : null);

		if (!TextUtils.isEmpty(mURL))
			return CacheKey.newUrlBasedKey(mURL, mDimension, mWidthBased, mExtensionMode, mHandler.getStorageTransform()!=null ? mHandler.getStorageTransform().getVariantPostfix() : null);

		return null;
	}

	void startLoading(PictureCache cache) throws NoSuchAlgorithmException {
		CacheKey key = buildKey();
		if (key==null) {
			cache.getLogger().w("could not generate a CacheKey for "+mUUID+" / "+mURL);
			return;
		}

		cache.getPicture(mURL, key, mFreshDate, mHandler, mLifeType);
	}
}