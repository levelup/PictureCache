package com.levelup.picturecache;

import java.security.NoSuchAlgorithmException;

import android.text.TextUtils;

import com.levelup.picturecache.internal.CacheKey;

public class PictureJob {

	public final String url;
	public final String mUUID;
	public final Object drawCookie;
	public final long mFreshDate;
	public final LifeSpan mLifeSpan;
	public final int mDimension;
	public final boolean mWidthBased;
	public final StorageType mExtensionMode;
	public final PictureJobRenderer mDisplayHandler;
	public final PictureJobTransforms mTransformHandler;
	public final PictureJobConcurrency mConcurrencyHandler;
	public final NetworkLoader networkLoader;
	public final CacheKey key;

	public static class Builder {

		protected final PictureJobRenderer mDisplayHandler;
		protected final PictureJobTransforms mTransformHandler;
		protected final PictureJobConcurrency mConcurrencyHandler;
		private String mURL;
		private String mUUID;
		private Object drawCookie;
		private long mFreshDate;
		private LifeSpan mLifeSpan = LifeSpan.LONGTERM;
		private int mDimension;
		private boolean mWidthBased;
		private StorageType mExtensionMode = StorageType.AUTO;
		private NetworkLoader networkLoader;
		private CacheKey key;

		public Builder(PictureJobRenderer draw, PictureJobTransforms transforms, PictureJobConcurrency concurrencyHandler) {
			if (null==concurrencyHandler) throw new IllegalArgumentException("missing a IPictureLoadConcurrency");
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

		public Builder setDrawCookie(Object cookie) {
			drawCookie = cookie;
			return this;
		}

		public Builder setNetworkLoader(NetworkLoader networkLoader) {
			this.networkLoader = networkLoader;
			return this;
		}

		Builder forceCacheKey(CacheKey key) {
			this.key = key;
			return this;
		}

		public PictureJob build() {
			return new PictureJob(this);
		}

		@Override
		public String toString() {
			return "{PictureJob.Builder "+mUUID+" / "+mURL+'}';
		}
	}

	protected PictureJob(Builder builder) {
		this.mDisplayHandler = builder.mDisplayHandler;
		this.mTransformHandler = builder.mTransformHandler;
		this.mConcurrencyHandler = builder.mConcurrencyHandler;
		this.url = builder.mURL;
		this.mUUID = builder.mUUID;
		this.drawCookie = builder.drawCookie;
		this.mFreshDate = builder.mFreshDate;
		this.mLifeSpan = builder.mLifeSpan;
		this.mDimension = builder.mDimension;
		this.mWidthBased = builder.mWidthBased;
		this.mExtensionMode = builder.mExtensionMode;
		this.networkLoader = builder.networkLoader;
		if (null!=builder.key)
			this.key = builder.key;
		else
			this.key = buildKey(builder);
		if (null==this.key) throw new IllegalStateException("failed to create a CacheKey with "+builder);
	}

	public Builder cloneBuilder() {
		Builder builder = new Builder(mDisplayHandler, mTransformHandler, mConcurrencyHandler);
		builder.mURL = this.url;
		builder.mUUID = this.mUUID;
		builder.drawCookie = this.drawCookie;
		builder.mFreshDate = this.mFreshDate;
		builder.mLifeSpan = this.mLifeSpan;
		builder.mDimension = this.mDimension;
		builder.mWidthBased = this.mWidthBased;
		builder.mExtensionMode = this.mExtensionMode;
		builder.networkLoader = this.networkLoader;
		return builder;
	}

	private static CacheKey buildKey(Builder builder) {
		if (!TextUtils.isEmpty(builder.mUUID))
			return CacheKey.newUUIDBasedKey(builder.mUUID, builder.mDimension, builder.mWidthBased, builder.mExtensionMode,
					null!=builder.mTransformHandler && builder.mTransformHandler.getStorageTransform() != null ? builder.mTransformHandler.getStorageTransform().getVariantPostfix() : null);

		if (!TextUtils.isEmpty(builder.mURL))
			try {
				return CacheKey.newUrlBasedKey(builder.mURL, builder.mDimension, builder.mWidthBased, builder.mExtensionMode,
						null!=builder.mTransformHandler && builder.mTransformHandler.getStorageTransform() != null ? builder.mTransformHandler.getStorageTransform().getVariantPostfix() : null);
			} catch (NoSuchAlgorithmException e) {
				LogManager.getLogger().w(PictureCache.LOG_TAG, "Failed to create a CacheKey for "+builder, e);
			}

		return null;
	}

	@Override
	public boolean equals(Object o) {
		if (o==this) return true;
		if (!(o instanceof PictureJob)) return false;
		PictureJob p = (PictureJob) o;
		return key.equals(p.key) && ((mDisplayHandler==null && p.mDisplayHandler==null) || (mDisplayHandler!=null && mDisplayHandler.equals(p.mDisplayHandler)))
				&& ((mTransformHandler==null && p.mTransformHandler==null) || (mTransformHandler!=null && mTransformHandler.equals(p.mTransformHandler)))
				&& (mConcurrencyHandler.equals(p.mConcurrencyHandler));
	}

	/**
	 * Retrieve picture into cache
	 * 
	 * @param cache
	 */
	public void startLoading(PictureCache cache) {
		cache.doPictureJob(this);
	}

	public void stopLoading(PictureCache cache, boolean resetToDefault) {
		cache.cancelPictureJob(this);
		if (resetToDefault)
			mDisplayHandler.drawDefaultPicture(url, cache.getBitmapCache());
	}
}