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
package org.evosuite.coverage.seepep;

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
public class SeepepCoverageTestFitness extends TestFitnessFunction { /*SEEPEP: DAG coverage*/
	
	private static final long serialVersionUID = 624688974223998930L;

	/** Target seepep-dags */
	private final List<SeepepCoverageGoalFitness> goals;

	/**
	 * Constructor - fitness for a set of seepep-dags
	 * 
	 */
	public SeepepCoverageTestFitness(List<SeepepCoverageGoalFitness> goals) throws IllegalArgumentException {
		if(goals == null){
			throw new IllegalArgumentException("goal cannot be null");
		}
		this.goals = goals;
	}

	public List<SeepepCoverageGoalFitness> getPathConditionGoals() {
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
		for (SeepepCoverageGoalFitness g : goals) {
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
		SeepepCoverageTestFitness other = (SeepepCoverageTestFitness) obj;
		if (goals == null) {
			if (other.goals != null)
				return false;
		} else if (!goals.equals(other.goals))
			return false;
		return true;
	}
	

}
