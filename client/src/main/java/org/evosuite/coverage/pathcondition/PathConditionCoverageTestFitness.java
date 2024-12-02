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

import org.evosuite.testcase.execution.ExecutionResult;

import java.util.Arrays;
import java.util.List;

import org.evosuite.testcase.TestChromosome;
import org.evosuite.testcase.TestFitnessFunction;

/**
 * Fitness function for a single test on a single path condition
 * 
 * @author G. Denaro
 */
public class PathConditionCoverageTestFitness extends TestFitnessFunction { /*SUSHI: Path condition fitness*/

	private static final long serialVersionUID = -8111511902712198947L;

	/** Target pathConditions */
	private final List<PathConditionCoverageGoalFitness> goals;

	/**
	 * Constructor - fitness for a set of path conditions
	 * 
	 */
	public PathConditionCoverageTestFitness(List<PathConditionCoverageGoalFitness> goals) throws IllegalArgumentException {
		if(goals == null){
			throw new IllegalArgumentException("goal cannot be null");
		}
		this.goals = goals;
	}

	public List<PathConditionCoverageGoalFitness> getPathConditionGoals() {
		return goals;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * Calculate fitness as computed by the path condition evaluator during test execution
	 */
	@Override
	public double getFitness(TestChromosome individual, ExecutionResult result) {
		double fitness = 0d;
		for (PathConditionCoverageGoalFitness g : goals) {
			fitness += g.getFitness(individual, result);
		}
		
		logger.debug("Cumulative Path condition fitness = " + fitness);

		updateIndividual(individual, fitness);
		return fitness;
	}


	/** {@inheritDoc} */
	@Override
	public String toString() {
		return Arrays.toString(goals.toArray());
	}


	/* (non-Javadoc)
	 * @see org.evosuite.testcase.TestFitnessFunction#getTargetClass()
	 */
	@Override
	public String getTargetClass() {
		throw new RuntimeException("Not implemented");
	}

	/* (non-Javadoc)
	 * @see org.evosuite.testcase.TestFitnessFunction#getTargetMethod()
	 */
	@Override
	public String getTargetMethod() {
		throw new RuntimeException("Not implemented");
	}

	@Override
	public int compareTo(TestFitnessFunction other) {
		return 0; /* No ordering defined*/
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((goals == null) ? 0 : goals.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PathConditionCoverageTestFitness other = (PathConditionCoverageTestFitness) obj;
		if (goals == null) {
			if (other.goals != null)
				return false;
		} else if (!goals.equals(other.goals))
			return false;
		return true;
	}
	

}
