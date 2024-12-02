package org.evosuite.ga.metaheuristics.mosa.structural;

import java.util.List;

import org.evosuite.coverage.seepep.SeepepCoverageGoalFitness;
import org.evosuite.ga.metaheuristics.GeneticAlgorithm;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.testcase.TestFitnessFunction;

public class SeepepManager extends MultiCriteriaManager {
	
	private static final long serialVersionUID = -1337018499019708503L;

	public SeepepManager(List<TestFitnessFunction> targets) {
		super(targets);

		//add all dag goals as current goals
		for (TestFitnessFunction goal : targets) {
			if (goal instanceof SeepepCoverageGoalFitness) {
				this.currentGoals.add(goal);
			}
		}
	}

	@Override
	public void calculateFitness(TestChromosome c, GeneticAlgorithm<TestChromosome> ga) {
		super.calculateFitness(c, ga);

		// if all Seepep-dags are covered, we don't need to continue the search
		boolean stopSearch = true;
		for (TestFitnessFunction goal: this.getUncoveredGoals()) {
			if (goal instanceof SeepepCoverageGoalFitness) {
				stopSearch = false;
				break;
			}
		}
		if (stopSearch) {
			this.currentGoals.clear();
			this.getUncoveredGoals().clear();
		}
	}	
}
