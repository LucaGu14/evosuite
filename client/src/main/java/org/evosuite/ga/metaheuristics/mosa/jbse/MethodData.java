package org.evosuite.ga.metaheuristics.mosa.jbse;

public class MethodData {
	private final String className;
	private final String methodName;
	private final String methodDescriptor;
	
	public MethodData(String className, String methodName, String methodDescriptor) {
		this.className = className.replace('.', '/');
		this.methodName = methodName;
		this.methodDescriptor = methodDescriptor;
	}

	public String getClassNameDotted() {
		return className.replace('/', '.');
	}

	public String getClassNameSlashed() {
		return className;
	}

	public String getMethodName() {
		return methodName;
	}

	public String getMethodDescriptor() {
		return methodDescriptor;
	}

	@Override
	public String toString() {
		return className + "." + methodName + methodDescriptor;
	}
	
	
}