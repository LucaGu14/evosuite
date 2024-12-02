/**
 * Copyright (C) 2011,2012 Gordon Fraser, Andrea Arcuri and EvoSuite
 * contributors
 * 
 * This file is part of EvoSuite.
 * 
 * EvoSuite is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 * 
 * EvoSuite is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU Public License for more details.
 * 
 * You should have received a copy of the GNU Public License along with
 * EvoSuite. If not, see <http://www.gnu.org/licenses/>.
 */
package org.evosuite.coverage.pathcondition;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

import org.evosuite.TestGenerationContext;
import org.evosuite.instrumentation.InstrumentingClassLoader;
import org.evosuite.testcase.execution.EvosuiteError;
import org.evosuite.testcase.execution.ExecutionResult;

/**
 * A single path condition coverage goal
 * 
 * @author G. Denaro
 */
public class PathConditionCoverageGoal implements Serializable {  /*SUSHI: Path condition fitness*/
	
	private static final long serialVersionUID = 7022535456018762227L;

	private final String className;
	private final String methodName;
	private final String evaluatorName;
	private final int pathConditionId;
	private transient Object evaluator;
	private String customDescription = null; //set when the evaluator object is created

	/**
	 * @param className
	 *            a {@link java.lang.String} object.
	 * @param methodName
	 *            a {@link java.lang.String} object.
	 */
	public PathConditionCoverageGoal(int pathConditionId, String className, 
	        String methodName, String evaluatorName) {
		if (className == null || methodName == null)
			throw new IllegalArgumentException("null given");

		this.pathConditionId = pathConditionId;
		this.className = className;
		this.methodName = methodName;
		this.evaluatorName = evaluatorName;
		this.evaluator = null; //postpone creation at first actual access, to avoid early creation, when the ClassLoaderForSUT is not yet ready
	}


	private Object getEvaluatorInstance() {
		try {
			InstrumentingClassLoader cl = TestGenerationContext.getInstance().getClassLoaderForSUT();
			Class<?> clazz = Class.forName(evaluatorName, true, cl);
			Object evaluator = null;
			try {
				Constructor<?> cnstr = clazz.getConstructor(ClassLoader.class);
				evaluator = cnstr.newInstance(cl);
			} catch (NoSuchMethodException e) {
				evaluator = clazz.newInstance();							
			}
			return evaluator;
		} catch (SecurityException | ClassNotFoundException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			throw new EvosuiteError("Cannot instantiate path condition evaluator: " + evaluatorName +
					" because of: " + e + " ::: " + Arrays.toString(e.getStackTrace()));
		}
	}

	/**
	 * @return the className that this path condition refers to
	 */
	public String getClassName() {
		return className;
	}

	/**
	 * @return the methodName that this path condition refers to
	 */
	public String getMethodName() {
		return methodName;
	}

	/**
	 * @return the path condition id
	 */
	public int getPathConditionId() {
		return pathConditionId;
	}

	/**
	 * @return the name of the evaluator of this path condition
	 */
	public String getEvaluatorName() {
		return evaluatorName;
	}
	
	/**
	 * @return the evaluator of this path condition
	 */
	public Object getEvaluator() {
		if (evaluator == null) {
			evaluator = getEvaluatorInstance();
		}
		return evaluator;
	}

	public void setCustomDescription(String customDescription) {
		this.customDescription = customDescription;
	}

	/**
	 * <p>
	 * getDistance
	 * </p>
	 * 
	 * @param result
	 *            a {@link org.evosuite.testcase.ExecutionResult} object.
	 * @return a {@link org.evosuite.coverage.ControlFlowDistance} object.
	 */
	public double getDistance(ExecutionResult result) {

		Double distance = result.getTrace().getPathConditionDistances().get(pathConditionId);
		
		if (distance == null) return Double.MAX_VALUE; /*TODO: which is maximum distance for non-evaluated path conditions?*/
		else return distance;

	}

	// inherited from Object

	/**
	 * {@inheritDoc}
	 * 
	 * Readable representation
	 */
	@Override
	public String toString() {
		String name = className + "." + methodName + 
				(customDescription != null ? ": " + customDescription : "") + 
				" -- path condition " + evaluatorName + " (id = " + pathConditionId + ")";
		return name;
	}

	/** {@inheritDoc} */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + pathConditionId;
		//result = prime * result + className.hashCode();
		//result = prime * result + methodName.hashCode();
		result = prime * result + evaluatorName.hashCode();
		return result;
	}

	/** {@inheritDoc} */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;

		PathConditionCoverageGoal other = (PathConditionCoverageGoal) obj;
		return this.pathConditionId == other.pathConditionId && 
				evaluatorName.equals(other.evaluatorName);
	}

}
