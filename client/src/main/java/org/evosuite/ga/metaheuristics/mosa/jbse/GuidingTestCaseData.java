package org.evosuite.ga.metaheuristics.mosa.jbse;

import java.nio.file.Path;

public class GuidingTestCaseData {
	
	private final String className;
	private final String methodDescriptor;
	private final String methodName;
	private final Path sourcePath;
	/**
	 * Constructor. Builds a {@link GuidingTestCaseData} from the specification 
	 * of a test method.
	 * 
	 * @param className a {@link String}, the name of the class of the test method.
	 * @param methodDescriptor a {@link String}, the descriptor of the parameters of the 
	 *        test method.
	 * @param methodName a {@link String}, the name of the test method.
	 * @param sourceDir the {@link Path} of the directory where the source file of the 
	 *        test case is found.
	 * @param evoSuiteTC 
	 */
	public GuidingTestCaseData(String className, String methodDescriptor, String methodName, Path sourceDir) {
		this.className = className;
		this.methodDescriptor = methodDescriptor;
		this.methodName = methodName;
		this.sourcePath = sourceDir.resolve(className + ".java");
	}

	public String getClassName(){
		return this.className;
	}
	
	public String getMethodDescriptor(){
		return this.methodDescriptor;
	}
	
	public String getMethodName(){
		return this.methodName;
	}
	
	public Path getSourcePath() {
		return this.sourcePath;
	}
	
}
