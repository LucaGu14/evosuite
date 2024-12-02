package org.evosuite.utils;

public class EqualityByIdentityWrapper<T> {
	private final T obj;

	public EqualityByIdentityWrapper(T obj) {
		this.obj = obj;
	}

	public T unwrap() {
		return obj;
	}

	@Override
	public int hashCode() {
		return System.identityHashCode(obj);
	}

	@Override
	public boolean equals(Object otherWrapper) {
		if (this == otherWrapper)
			return true;
		if (otherWrapper == null)
			return false;
		if (getClass() != otherWrapper.getClass())
			return false;
	
		return (this.obj == ((EqualityByIdentityWrapper) otherWrapper).obj);
	}

}
