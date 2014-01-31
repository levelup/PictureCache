package com.levelup.picturecache;

import java.security.NoSuchAlgorithmException;

import android.text.TextUtils;

import com.levelup.picturecache.internal.CacheKey;

public class PictureJob {

	public final String mURL;
	public final String mUUID;
	public final Object mCookie;
	public final long mFreshDate;
	public final LifeSpan mLifeSpan;
	public final int mDimension;
	public final boolean mWidthBased;
	public final StorageType mExtensionMode;
	public final IPictureLoaderRender mDisplayHandler;
	public final IPictureLoaderTransforms mTransformHandler;
	public final IPictureLoadConcurrency mConcurrencyHandler;
	public final NetworkLoader networkLoader;

	public static class Builder {

		private String mURL;
		private String mUUID;
		private Object mCookie;
		private long mFreshDate;
		private LifeSpan mLifeSpan = LifeSpan.LONGTERM;
		private int mDimension;
		private boolean mWidthBased;
		private StorageType mExtensionMode = StorageType.AUTO;
		protected final IPictureLoaderRender mDisplayHandler;
		protected final IPictureLoaderTransforms mTransformHandler;
		protected final IPictureLoadConcurrency mConcurrencyHandler;
		private NetworkLoader networkLoader;

		public Builder(IPictureLoaderRender draw, IPictureLoaderTransforms transforms, IPictureLoadConcurrency concurrencyHandler) {
			this.mDisplayHandler = draw;
			this.mTransformHandler = transforms;
			this.mConcurrencyHandler = concurrencyHandler;
		}

		public Builder setURL(String URL) {
			mURL = URL;
			return this;
		}

		public Builder setUUID(String UUID) {
			mUUID = UUID;
			return this;
		}

		/**
		 * set for how long the item should remain in the cache
		 * 
		 * @return the {@link PictureJob} being created
		 */
		public Builder setLifeType(LifeSpan lifeSpan) {
			mLifeSpan = lifeSpan;
			return this;
		}

		public Builder setFreshDate(long date) {
			mFreshDate = date;
			return this;
		}

		public Builder setDimension(int dimension, boolean widthBased) {
			mDimension = dimension;
			mWidthBased = widthBased;
			return this;
		}

		public Builder setExtensionMode(StorageType extensionMode) {
			mExtensionMode = extensionMode;
			return this;
		}

		public Builder setCookie(Object cookie) {
			mCookie = cookie;
			return this;
		}

		public Builder setNetworkLoader(NetworkLoader networkLoader) {
			this.networkLoader = networkLoader;
			return this;
		}

		public PictureJob build() {
			return new PictureJob(this);
		}
	}

	protected PictureJob(Builder builder) {
		this.mURL = builder.mURL;
		this.mUUID = builder.mUUID;
		this.mCookie = builder.mCookie;
		this.mFreshDate = builder.mFreshDate;
		this.mLifeSpan = builder.mLifeSpan;
		this.mDimension = builder.mDimension;
		this.mWidthBased = builder.mWidthBased;
		this.mExtensionMode = builder.mExtensionMode;
		this.mDisplayHandler = builder.mDisplayHandler;
		this.mTransformHandler = builder.mTransformHandler;
		this.mConcurrencyHandler = builder.mConcurrencyHandler;
		this.networkLoader = builder.networkLoader;
	}

	private CacheKey buildKey() throws NoSuchAlgorithmException {
		if (!TextUtils.isEmpty(mUUID))
			return CacheKey.newUUIDBasedKey(mUUID, mDimension, mWidthBased, mExtensionMode,
					null!=mTransformHandler && mTransformHandler.getStorageTransform() != null ? mTransformHandler.getStorageTransform().getVariantPostfix() : null);

		if (!TextUtils.isEmpty(mURL))
			return CacheKey.newUrlBasedKey(mURL, mDimension, mWidthBased, mExtensionMode,
					null!=mTransformHandler && mTransformHandler.getStorageTransform() != null ? mTransformHandler.getStorageTransform().getVariantPostfix() : null);

		return null;
	}

	/**
	 * Retrieve picture into cache
	 * 
	 * @param cache
	 * @throws NoSuchAlgorithmException if the UUID is {@code null} and the URL cannot be used
	 */
	public void startLoading(PictureCache cache) throws NoSuchAlgorithmException {
		CacheKey key = buildKey();
		if (key == null) {
			LogManager.logger.w(PictureCache.LOG_TAG, "could not generate a CacheKey for " + mUUID + " / " + mURL);
			return;
		}

		cache.getPicture(mURL, key, mCookie, mFreshDate, mDisplayHandler, mTransformHandler, mConcurrencyHandler, mLifeSpan, networkLoader);
	}
}