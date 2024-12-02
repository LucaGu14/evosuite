package org.evosuite.ga.operators.crossover;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import org.evosuite.Properties;
import org.evosuite.ga.Chromosome;
import org.evosuite.ga.FitnessFunction;
import org.evosuite.ga.metaheuristics.GeneticAlgorithm;
import org.evosuite.utils.EqualityByIdentityWrapper;
import org.evosuite.utils.LoggingUtils;

public class SuccessCountCrossOverStrategy implements CrossOverListener { /*SUSHI: Statistics */

	private final String operatorName;
	
	private int bestSuccessCounterCrossOver = 0;
	private int bestSuccessCounterMutation = 0;
	private int overallSuccessCounterCrossOver = 0;
	private int overallSuccessCounterMutation = 0;
	private int unretained = 0;
	
	private boolean crossoverCompleted = false;
	//private boolean mutationDone = false;

	private Set<EqualityByIdentityWrapper<Chromosome>> unretainedGoodOffsprings = new HashSet<>();
	//private Map<FitnessFunction<?>, Double> bestFitnessValues = new HashMap<>();
	private Map<FitnessFunction<?>, Double> initialFitnessValues = new HashMap<>();
	private Map<EqualityByIdentityWrapper<Chromosome>, Chromosome> intermediateOffsprings =  new HashMap<>();

	public SuccessCountCrossOverStrategy(String operatorName) {
		this.operatorName = operatorName == null ? "" : operatorName;
	}
	
	@Override
	public void iteration(GeneticAlgorithm algorithm)  {
		if (!Properties.SUSHI_STATISTICS) return;
		
		unretained += unretainedGoodOffsprings.size();
		unretainedGoodOffsprings.clear();
		LoggingUtils.getEvoLogger().info("STATISTICS FOR CROSSOVER {} IT {}: {} improving offsprings ({} not retained)", operatorName, algorithm.getAge(), overallSuccessCounterCrossOver, unretained);
		
		/*LoggingUtils.getEvoLogger().info("{} best offsprings", bestSuccessCounterCrossOver);
		LoggingUtils.getEvoLogger().info("Mutation {} {}", overallSuccessCounterMutation, bestSuccessCounterMutation);
		
		for (FitnessFunction<?> ff: algorithm.getFitnessFunctions()) {
			Chromosome best = ff.getBestStoredIndividual();
			if (best != null) {
				bestFitnessValues.put(ff, best.getFitness(ff));
			}
		}*/
		
	}

	@Override
	public void crossOverBegin(Chromosome parent1, Chromosome parent2) {
		if (!Properties.SUSHI_STATISTICS) return;

		intermediateOffsprings.clear();
		initialFitnessValues.clear();
		Map<FitnessFunction<?>, Double> parent1FF = parent1.getFitnessValues();
		Map<FitnessFunction<?>, Double> parent2FF = parent2.getFitnessValues();
		for (FitnessFunction<?> ff: parent1FF.keySet()) {
			Double fitness2 = parent2FF.get(ff);
			if (fitness2 == null) {
				continue; //we are interested in values that exist in both parents
			}
			Double fitness1 = parent1FF.get(ff);
			initialFitnessValues.put(ff, fitness1 < fitness2 ? fitness1 : fitness2);
		}
		
		crossoverCompleted = false;					
		//mutationDone = false;
	
	}

	@Override
	public void crossOverDone(Chromosome offspring1, Chromosome offspring2) {
		if (!Properties.SUSHI_STATISTICS) return;

		intermediateOffsprings.put(new EqualityByIdentityWrapper<Chromosome>(offspring1), offspring1.clone());
		intermediateOffsprings.put(new EqualityByIdentityWrapper<Chromosome>(offspring2), offspring2.clone());
		crossoverCompleted = true;				
	}

	@Override
	public void modification(Chromosome individual)  { 
		/*if (!Properties.SUSHI_STATISTICS) return;
		
		if (crossoverCompleted) {
			mutationDone = true;
		}*/
	}
	

	@Override
	public void crossOverException(Chromosome parent1, Chromosome parent2) {  /*nothing to do */ }

	@Override
	public void fitnessEvaluation(Chromosome individual) { 
		if (!Properties.SUSHI_STATISTICS) return;

		if (!intermediateOffsprings.containsKey(new EqualityByIdentityWrapper<Chromosome>(individual))) return;
		
		/*Map<FitnessFunction<?>, Double> initialFitnessForMutation = initialFitnessValues;	
		if (crossoverCompleted && mutationDone) {
			Chromosome intermediate = intermediateOffsprings.get(individual);
			finalFitnessForCrossover	 = intermediate.getFitnessValues();
			intermediate.setChanged(true);
			for (FitnessFunction<?> ff: finalFitnessForCrossover.keySet()) {
				((TestFitnessFunction) ff).getFitness((TestChromosome) intermediate);
			}
			initialFitnessForMutation = finalFitnessForCrossover; 
		}*/

		if (improves(individual.getFitnessValues(), initialFitnessValues)) {
			++overallSuccessCounterCrossOver;
			unretainedGoodOffsprings.add(new EqualityByIdentityWrapper<Chromosome>(individual));
			/*if (improves(finalFitnessForCrossover, bestFitnessValues)) {
					++bestSuccessCounterCrossOver;	
					updateBestFitnessValues(finalFitnessForCrossover);
			}*/
		}

		/*if (mutationDone) { 
			if (improves(initialFitnessForMutation, individual.getCoverageValues())) {
				++overallSuccessCounterMutation;
				unretainedGoodOffsprings.add(individual);
				if (improves(individual.getCoverageValues(), bestFitnessValues)) {
					++bestSuccessCounterMutation;
					updateBestFitnessValues(finalFitnessForCrossover);
				}				
			}
		} */

	}

	/*private void updateBestFitnessValues(Map<FitnessFunction<?>, Double> fitnessValues) {
		for (FitnessFunction<?> ff: fitnessValues.keySet()) {
			double value = fitnessValues.get(ff);
			Double bestValue = bestFitnessValues.get(ff);
			if(bestValue == null || value < bestValue) {
				bestFitnessValues.put(ff, value);
			}
		}
	}*/

	private boolean improves(Map<FitnessFunction<?>, Double> fitnessValues1,
			Map<FitnessFunction<?>, Double> fitnessValues2) {
		for (FitnessFunction<?> ff: fitnessValues1.keySet()) {
			Double value2 = fitnessValues2.get(ff);
			if (value2 == null) 
				continue;
			if (fitnessValues1.get(ff) < value2) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void searchStarted(GeneticAlgorithm algorithm) { /*nothing to do */ }

	@Override
	public void searchFinished(GeneticAlgorithm algorithm)  { /*nothing to do */ }

	@Override
	public void inNextGeneration(Chromosome chromosome) {
		unretainedGoodOffsprings.remove(new EqualityByIdentityWrapper<Chromosome>(chromosome));			
	}

	public Map<String, Integer> getIterationBestSuccessCountsCrossOver() {
		 Map<String, Integer> ret = new Hashtable<String, Integer>();
		 
		 ret.put(operatorName, bestSuccessCounterCrossOver);
		 
		 return ret;
	}

	public Map<String, Integer> getIterationBestSuccessCountsMutation() {
		 Map<String, Integer> ret = new Hashtable<String, Integer>();
		 
		 ret.put(operatorName, bestSuccessCounterMutation);
		 
		 return ret;
	}

	public Map<String, Integer> getOverallSuccessCountsCrossOver() {
		 Map<String, Integer> ret = new Hashtable<String, Integer>();
		 
		 ret.put(operatorName, overallSuccessCounterCrossOver);
		 
		 return ret;
	}

	public Map<String, Integer> getOverallSuccessCountsMutation() {
		 Map<String, Integer> ret = new Hashtable<String, Integer>();
		 
		 ret.put(operatorName, overallSuccessCounterMutation);
		 
		 return ret;
	}

}
