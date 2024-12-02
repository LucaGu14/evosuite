package org.evosuite.coverage.seepep;

import org.evosuite.testcase.TestChromosome;
import org.evosuite.testcase.TestFitnessFunction;
import org.evosuite.testcase.execution.ExecutionResult;

/**
 * Fitness function for a single test on a single seepep-dag
 * 
 * @author G. Denaro
 */
public class SeepepCoverageGoalFitness extends TestFitnessFunction { /*SEEPEP: DAG coverage*/

	private static final long serialVersionUID = -4709193271343944253L;

	/** Target goal */
	private final SeepepCoverageGoal goal;

	/**
	 * Constructor - fitness is specific to a seepep-dag
	 * 
	 */
	public SeepepCoverageGoalFitness(SeepepCoverageGoal goal) throws IllegalArgumentException {
		if(goal == null){
			throw new IllegalArgumentException("goal cannot be null");
		}
		this.goal = goal;
	}

	public SeepepCoverageGoal getSeepepDagGoal() {
		return goal;
	}

	/**
	 * @return the goal id
	 */
	public int getSeepepDagId() {
		return goal.getSeepepDagId();
	}

	/**
	 * @return the spark methods of this goal
	 */
	public String getSparkActions() {
		return goal.getTargetDag();
	}

	
	/**
	 * {@inheritDoc}
	 * 
	 * Calculate fitness as computed by the path condition evaluator during test execution
	 */
	@Override
	public double getFitness(TestChromosome individual, ExecutionResult result) {
		double fitness = goal.getDistance(result);

		// If there is an undeclared exception it is a failing test
		if (result.hasUndeclaredException())
			fitness += 1;

		logger.debug("Seepep goal fitness = " + fitness);

		updateIndividual(individual, fitness);
		return fitness;
	}

	/** {@inheritDoc} */
	@Override
	public String toString() {
		return goal.toString();
	}
	
	/** {@inheritDoc} */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((goal == null) ? 0 : goal.hashCode());
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
		SeepepCoverageGoalFitness other = (SeepepCoverageGoalFitness) obj;
		if (goal == null) {
			if (other.goal != null)
				return false;
		} else if (!goal.equals(other.goal))
			return false;
		return true;
	}

	@Override
	public int compareTo(TestFitnessFunction other) {
		return 0; /* No ordering defined*/
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


}
