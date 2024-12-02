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
package org.evosuite.ga.operators.crossover;

import java.util.Hashtable;
import java.util.Map;

import org.evosuite.ga.Chromosome;
import org.evosuite.ga.ConstructionFailedException;
import org.evosuite.ga.metaheuristics.GeneticAlgorithm;
import org.evosuite.utils.LoggingUtils;

/**
 * Use methods from a parent in the other parent
 *
 * @author Giovanni Denaro
 */
public class MultiOperatorAlternatingCrossOver extends SushiCrossOver  { /*SUSHI: Crossover*/

	private static final long serialVersionUID = -3086477941208910388L;
	
	private int currentOperator= 0; 
	
	private final CrossOverFunction[] crossoverOperators;
	
	public MultiOperatorAlternatingCrossOver(CrossOverFunction... crossoverOperators) {
		if (crossoverOperators == null || crossoverOperators.length == 0) 
			throw new RuntimeException("HybridCrossOver called with no delegable crossover operator");
		this.crossoverOperators = crossoverOperators;
	}

	/**
	 * {@inheritDoc}
	 *
	 */
	@Override
	protected void doCrossOver(Chromosome parent1, Chromosome parent2)
	        throws ConstructionFailedException {
		
		//LoggingUtils.getEvoLogger().info("-- BEGIN Crossover: {}", crossoverOperators[currentOperator].getClass().getSimpleName());
		crossoverOperators[currentOperator].crossOver(parent1, parent2);
		//LoggingUtils.getEvoLogger().info("-- END Crossover: {}", crossoverOperators[currentOperator].getClass().getSimpleName());
		
	}

	//Forward SushiCrossOver notifications to sub operators
	
	@Override
	public void searchStarted(GeneticAlgorithm algorithm) {
		super.searchStarted(algorithm);
		for (CrossOverFunction co : crossoverOperators) {
			if (co instanceof SushiCrossOver)
				((SushiCrossOver) co).searchStarted(algorithm);
		}
	}

	@Override
	public void iteration(GeneticAlgorithm algorithm) {
		super.iteration(algorithm);

		currentOperator = algorithm.getAge() % crossoverOperators.length;
		
		for (CrossOverFunction co : crossoverOperators) {
			if (co instanceof SushiCrossOver)
				((SushiCrossOver) co).iteration(algorithm);
		}
	}

	@Override
	public void searchFinished(GeneticAlgorithm algorithm) {
		super.searchFinished(algorithm);

		for (CrossOverFunction co : crossoverOperators) {
			if (co instanceof SushiCrossOver)
				((SushiCrossOver) co).searchFinished(algorithm);
		}
	}

	@Override
	public void fitnessEvaluation(Chromosome individual) {
		super.fitnessEvaluation(individual);

		if (crossoverOperators[currentOperator] instanceof SushiCrossOver)
			((SushiCrossOver) crossoverOperators[currentOperator]).fitnessEvaluation(individual);		
	}

	@Override
	public void modification(Chromosome individual) {
		super.modification(individual);

		if (crossoverOperators[currentOperator] instanceof SushiCrossOver)
			((SushiCrossOver) crossoverOperators[currentOperator]).modification(individual);
	}

	@Override
	public void inNextGeneration(Chromosome individual) {
		super.inNextGeneration(individual);

		if (crossoverOperators[currentOperator] instanceof SushiCrossOver)
			((SushiCrossOver) crossoverOperators[currentOperator]).inNextGeneration(individual);
	}
	
	//Implements Statistics as composition of statics of sub operators

	@Override
	public int getNumberOfMinimizerCalls() {
		int minimizerCount = 0;
		for (CrossOverFunction co : crossoverOperators) {
			if (co instanceof SushiCrossOver) {
				minimizerCount += ((SushiCrossOver) co).getNumberOfMinimizerCalls();
			}
		}
		return minimizerCount;
	}

	@Override
	public Map<String, Integer> getIterationBestSuccessCountsCrossOver() {
		Map<String, Integer> successCount = new Hashtable<String, Integer>();
		for (CrossOverFunction co : crossoverOperators) {
			if (co instanceof SushiCrossOver) {
				successCount.putAll(((SushiCrossOver) co).getIterationBestSuccessCountsCrossOver());
			}
		}
		return successCount;
	}

	@Override
	public Map<String, Integer> getIterationBestSuccessCountsMutation() {
		Map<String, Integer> successCount = new Hashtable<String, Integer>();
		for (CrossOverFunction co : crossoverOperators) {
			if (co instanceof SushiCrossOver) {
				successCount.putAll(((SushiCrossOver) co).getIterationBestSuccessCountsMutation());
			}
		}
		return successCount;
	}

	@Override
	public Map<String, Integer> getOverallSuccessCountsCrossOver() {
		Map<String, Integer> successCount = new Hashtable<String, Integer>();
		for (CrossOverFunction co : crossoverOperators) {
			if (co instanceof SushiCrossOver) {
				successCount.putAll(((SushiCrossOver) co).getOverallSuccessCountsCrossOver());
			}
		}
		return successCount;
	}

	@Override
	public Map<String, Integer> getOverallSuccessCountsMutation() {
		Map<String, Integer> successCount = new Hashtable<String, Integer>();
		for (CrossOverFunction co : crossoverOperators) {
			if (co instanceof SushiCrossOver) {
				successCount.putAll(((SushiCrossOver) co).getOverallSuccessCountsMutation());
			}
		}
		return successCount;
	}
	
}
