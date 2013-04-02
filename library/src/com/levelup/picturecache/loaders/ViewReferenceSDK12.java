package com.levelup.picturecache.loaders;

import android.annotation.TargetApi;
import android.os.Build;
import android.view.View;
import android.view.View.OnAttachStateChangeListener;

@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
public abstract class ViewReferenceSDK12<V extends View> extends ViewReference<V> implements OnAttachStateChangeListener {
	private boolean mViewAttached;

	ViewReferenceSDK12(V view) {
		super(view);
		mViewAttached = true;
		view.addOnAttachStateChangeListener(this);
	}

	@Override
	public void onViewAttachedToWindow(View v) {
		mViewAttached = true;
	}

	@Override
	public void onViewDetachedFromWindow(View v) {
		mViewAttached = false;
	}
	
	@Override
	protected boolean canDisplay() {
		return mViewAttached && super.canDisplay();
	}
}
