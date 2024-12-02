package org.evosuite.ga.operators.crossover;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.evosuite.ga.Chromosome;
import org.evosuite.ga.ConstructionFailedException;
import org.evosuite.ga.metaheuristics.GeneticAlgorithm;
import org.evosuite.ga.metaheuristics.SearchListener;
import org.evosuite.utils.LoggingUtils;

public abstract class SushiCrossOver extends CrossOverFunction implements SearchListener {

	private static final long serialVersionUID = -8188793546885245087L;
	
	private final List<CrossOverListener> crossOverListeners = new ArrayList<CrossOverListener>();
	
	public void addCrossOverListener(CrossOverListener l) {
		if (l != null) {
			crossOverListeners.add(l);
		}
	}
	
	@Override
	public final void crossOver(Chromosome parent1, Chromosome parent2) throws ConstructionFailedException {
		notifyCrossOverBegin(parent1, parent2);
		try {
			doCrossOver(parent1, parent2);
			notifyCrossOverDone(parent1, parent2);
		} catch (Exception e) {
			notifyCrossOverException(parent1, parent2);	
			throw e;
		}
	}

	private void notifyCrossOverBegin(Chromosome parent1, Chromosome parent2) {
		for (CrossOverListener listener : crossOverListeners) {
			try {
				listener.crossOverBegin(parent1, parent2);
			} catch (Exception e) {
				LoggingUtils.getEvoLogger().info("Exception from CrossOverListener : " + listener +
						"\n\t failed because of: " + e.getCause() + 
						"\n\t stack trace is " + Arrays.toString(e.getCause().getStackTrace()));
			}
		}
	}

	private void notifyCrossOverDone(Chromosome parent1, Chromosome parent2) {
		for (CrossOverListener listener : crossOverListeners) {
			try {
				listener.crossOverDone(parent1, parent2);
			} catch (Exception e) {
				LoggingUtils.getEvoLogger().info("Exception from CrossOverListener : " + listener +
						"\n\t failed because of: " + e.getCause() + 
						"\n\t stack trace is " + Arrays.toString(e.getCause().getStackTrace()));
			}
		}
	}

	private void notifyCrossOverException(Chromosome parent1, Chromosome parent2) {
		for (CrossOverListener listener : crossOverListeners) {
			try {
				listener.crossOverException(parent1, parent2);
			} catch (Exception e) {
				LoggingUtils.getEvoLogger().info("Exception from CrossOverListener : " + listener +
						"\n\t failed because of: " + e.getCause() + 
						"\n\t stack trace is " + Arrays.toString(e.getCause().getStackTrace()));
			}
		}
	}
	
	// the actual implementation of CrossOver
	protected abstract void doCrossOver(Chromosome parent1, Chromosome parent2)  throws ConstructionFailedException;


	/**
	 * If the crossover operator may use the minimizer,
	 * returns the number of calls of the minimizer
	 */
	public abstract int getNumberOfMinimizerCalls(); /*SUSHI: Minimization*/

	/**
	 * Returns the number of successes of the operator, that is,
	 * the number of times in which this oprator succeeded in improving
	 * the best individual of the population, mapped to a String name 
	 * of the operator. The returned map may include statistics of sub-operators.
	 */
	public abstract Map<String, Integer> getIterationBestSuccessCountsCrossOver(); /* SUSHI: Statistics */
	
	public abstract Map<String, Integer> getIterationBestSuccessCountsMutation(); /* SUSHI: Statistics */
	
	public abstract Map<String, Integer> getOverallSuccessCountsCrossOver(); /* SUSHI: Statistics */
	
	public abstract Map<String, Integer> getOverallSuccessCountsMutation(); /* SUSHI: Statistics */


	// Implements methods of interface SearchListener by notifying all CrossOverListeners
	
	@Override
	public void searchStarted(GeneticAlgorithm algorithm) {
		for (CrossOverListener listener : crossOverListeners) {
			try {
				listener.searchStarted(algorithm);
			} catch (Exception e) {
				LoggingUtils.getEvoLogger().info("Exception from CrossOverListener : " + listener +
						"\n\t failed because of: " + e.getCause() + 
						"\n\t stack trace is " + Arrays.toString(e.getStackTrace()));
			}
		}
	}

	@Override
	public void iteration(GeneticAlgorithm algorithm) {
		for (CrossOverListener listener : crossOverListeners) {
			try {
				listener.iteration(algorithm);
			} catch (Exception e) {
				LoggingUtils.getEvoLogger().info("Exception from CrossOverListener : " + listener +
						"\n\t failed because of: " + e.getCause() + 
						"\n\t stack trace is " + Arrays.toString(e.getStackTrace()));
			}
		}
	}

	@Override
	public void searchFinished(GeneticAlgorithm algorithm) {
		for (CrossOverListener listener : crossOverListeners) {
			try {
				listener.searchFinished(algorithm);
			} catch (Exception e) {
				LoggingUtils.getEvoLogger().info("Exception from CrossOverListener : " + listener +
						"\n\t failed because of: " + e.getCause() + 
						"\n\t stack trace is " + Arrays.toString(e.getStackTrace()));
			}
		}
	}

	@Override
	public void fitnessEvaluation(Chromosome individual) {
		for (CrossOverListener listener : crossOverListeners) {
			try {
				listener.fitnessEvaluation(individual);
			} catch (Exception e) {
				LoggingUtils.getEvoLogger().info("Exception from CrossOverListener : " + listener +
						"\n\t failed because of: " + e.getCause() + 
						"\n\t stack trace is " + Arrays.toString(e.getStackTrace()));
			}
		}
	}

	@Override
	public void modification(Chromosome individual) {
		for (CrossOverListener listener : crossOverListeners) {
			try {
				listener.modification(individual);
			} catch (Exception e) {
				LoggingUtils.getEvoLogger().info("Exception from CrossOverListener : " + listener +
						"\n\t failed because of: " + e.getCause() + 
						"\n\t stack trace is " + Arrays.toString(e.getStackTrace()));
			}
		}
	}

	public void inNextGeneration(Chromosome individual) {
		for (CrossOverListener listener : crossOverListeners) {
			try {
				listener.inNextGeneration(individual);
			} catch (Exception e) {
				LoggingUtils.getEvoLogger().info("Exception from CrossOverListener : " + listener +
						"\n\t failed because of: " + e.getCause() + 
						"\n\t stack trace is " + Arrays.toString(e.getStackTrace()));
			}
		}
	}
	
}
