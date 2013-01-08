package com.levelup.picturecache;

import java.io.File;

public class CacheVariant {
	final File path;
	final CacheKey key;
	
	CacheVariant(File path, CacheKey key) {
		if (path==null) throw new NullPointerException("we need a path for this variant "+key);
		if (key==null) throw new NullPointerException("we need a key for this variant "+path);
		this.path = path;
		this.key = key;
	}
	
	@Override
	public boolean equals(Object o) {
		if (this==o) return true;
		if (!(o instanceof CacheVariant)) return false;
		return path.equals(((CacheVariant) o).path);
	}
	
	@Override
	public int hashCode() {
		return path.hashCode();
	}
}
