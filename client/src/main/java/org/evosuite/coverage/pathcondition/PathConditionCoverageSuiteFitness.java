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

import java.util.List;

import org.evosuite.testcase.TestChromosome;
import org.evosuite.testcase.TestFitnessFunction;
import org.evosuite.testsuite.TestSuiteChromosome;
import org.evosuite.testsuite.TestSuiteFitnessFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fitness function for a whole test suite for all branches
 * 
 * @author G. Denaro
 */
public class PathConditionCoverageSuiteFitness extends TestSuiteFitnessFunction { /*SUSHI: Path condition fitness*/

	private static final long serialVersionUID = 2991632394620406243L;

	private final static Logger logger = LoggerFactory.getLogger(TestSuiteFitnessFunction.class);

	/** Target pathConditions */
	private final List<PathConditionCoverageGoalFitness> goals;

	/**
	 * Constructor - fitness for a set of path conditions
	 * 
	 */
	public PathConditionCoverageSuiteFitness(List<PathConditionCoverageGoalFitness> goals) throws IllegalArgumentException {
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
	 * Execute all tests and compute fitness
	 */
	@Override
	public double getFitness(
	        TestSuiteChromosome suite) {
		logger.trace("Calculating path condition fitness");

		double suiteFitness = 0d;
		for (PathConditionCoverageGoalFitness g : goals) {
			double minFitness = Double.MAX_VALUE;
			for (TestChromosome t : suite.getTestChromosomes()) {
				double f = g.getFitness(t);
				if (f < minFitness) 
					minFitness = f;
			}
			suiteFitness += minFitness;
		}
		
		updateIndividual(suite, suiteFitness);

		// Assign also fitness to single test cases, to allow reasoning on the results crossover steps
		TestFitnessFunction tf = PathConditionCoverageFactory._I().getPathConditionCoverageTestFitness();
		for (TestChromosome t : suite.getTestChromosomes()) {
			tf.getFitness(t);
		}

		return suiteFitness;		
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
		PathConditionCoverageSuiteFitness other = (PathConditionCoverageSuiteFitness) obj;
		if (goals == null) {
			if (other.goals != null)
				return false;
		} else if (!goals.equals(other.goals))
			return false;
		return true;
	}
	


}
