package org.evosuite.coverage.pathcondition;

public class PathConditionClassLoader {
	private static PathConditionClassLoader _I = new PathConditionClassLoader();
	
	private PathConditionClassLoader() { }
	
	public static PathConditionClassLoader _I() {
		return _I;
	}
	
	private ClassLoader cl = PathConditionClassLoader.class.getClassLoader();
	
	public void setClassLoader(ClassLoader cl) {
		this.cl = cl;
	}
	
	public ClassLoader getClassLoader() {
		return cl;
	}

}
