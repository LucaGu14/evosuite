package org.evosuite.ga.operators.crossover;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.evosuite.Properties;
import org.evosuite.TestSuiteGenerator;
import org.evosuite.coverage.TestFitnessFactory;
import org.evosuite.ga.Chromosome;
import org.evosuite.ga.FitnessFunction;
import org.evosuite.ga.metaheuristics.GeneticAlgorithm;
import org.evosuite.ga.metaheuristics.mosa.DynaMOSA;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.testcase.TestFitnessFunction;
import org.evosuite.utils.LoggingUtils;
import org.evosuite.utils.generic.GenericMethod;

public class SelectSuccessfulModifiersCrossOverStrategy implements CrossOverListener {
	private GeneticAlgorithm<?> algorithm; 
	private Set<FitnessFunction<TestChromosome>> goals = null;
	
	private Chromosome offspring1;
	private Chromosome offspring2;
	private Map<FitnessFunction<?>, Double> offspring1InitialFitness = new HashMap<>();
	private Map<FitnessFunction<?>, Double> offspring2InitialFitness = new HashMap<>();
	private List<GenericMethod> offspring1CandidateModifiers = new ArrayList<>();
	private List<GenericMethod> offspring2CandidateModifiers = new ArrayList<>();

	private Map<GenericMethod, Integer> successfulModifiers = new HashMap<>(); /*Local Singleton: share the identified modifiers globally, for use in local search operators*/

	
	Set<GenericMethod> iterationSuccessfullModifiers = new HashSet<>();
	

	public Set<GenericMethod> getSuccessfulModifiers() {
		return successfulModifiers.keySet();
	}

	@Override
	public void crossOverBegin(Chromosome parent1, Chromosome parent2) {
		if (!Properties.SUSHI_MODIFIERS_LOCAL_SEARCH) return;
		
		offspring1 = parent1;
		offspring2 = parent2;
		offspring1CandidateModifiers.clear();			
		offspring2CandidateModifiers.clear();	
		
		offspring1InitialFitness.clear();
		offspring2InitialFitness.clear();
		Map<FitnessFunction<?>, Double> parent1FF = parent1.getFitnessValues();
		for (FitnessFunction<?> ff1: parent1FF.keySet()) {
			offspring1InitialFitness.put(ff1, parent1FF.get(ff1));
		}
		Map<FitnessFunction<?>, Double> parent2FF = parent2.getFitnessValues();
		for (FitnessFunction<?> ff2: parent2FF.keySet()) {
			offspring2InitialFitness.put(ff2, parent1FF.get(ff2));
		}
	}
	
	@Override
	public void crossOverException(Chromosome parent1, Chromosome parent2) {
		if (!Properties.SUSHI_MODIFIERS_LOCAL_SEARCH) return;
		
		offspring1 = null;
		offspring2 = null;
	}
	
	@Override
	public void crossOverDone(Chromosome parent1, Chromosome parent2) { /*nothing to do*/ }

	@Override
	public void inNextGeneration(Chromosome chromosome) {  /*nothing to do*/ }

	
	// record information that a modifier has been added to a parent to form an offspring
	public void recordApplieddModifier(TestChromosome originalOffspring, TestChromosome currentOffspring, GenericMethod appliedModifier) {
		if (!Properties.SUSHI_MODIFIERS_LOCAL_SEARCH) return;
		
		if (originalOffspring == offspring1) {
			offspring1CandidateModifiers.add(appliedModifier);
		} else if (originalOffspring == offspring2) {
			offspring2CandidateModifiers.add(appliedModifier);
		}
	}
	
	//Implements methods of SearchListener
	
	@Override
	public void searchStarted(GeneticAlgorithm algorithm) {
		if (!Properties.SUSHI_MODIFIERS_LOCAL_SEARCH) return;
		
		this.algorithm = algorithm;
		
		List<TestFitnessFactory<? extends TestFitnessFunction>> testFitnessFactories = TestSuiteGenerator.getFitnessFactories();
        goals = new HashSet<>();
        for (TestFitnessFactory<?> ff : testFitnessFactories) {
            goals.addAll(ff.getCoverageGoals());
        }

		if (goals == null || goals.isEmpty()) {
			LoggingUtils.getEvoLogger().info("* Cannot evaluate goodness of modifiers: miss a suitable **Test**FitnessFunction. Deactivating this option");
			Properties.SUSHI_MODIFIERS_LOCAL_SEARCH = false;
		}
	}

	@Override
	public void fitnessEvaluation(Chromosome individual) {
		if (!Properties.SUSHI_MODIFIERS_LOCAL_SEARCH) return;
			
		List<GenericMethod> candidates  = null;
		Map<FitnessFunction<?>, Double> initialFitness = null;
		if (individual == offspring1) {
			candidates = offspring1CandidateModifiers;
			initialFitness = offspring1InitialFitness;
		} else if (individual == offspring2) {
			candidates = offspring2CandidateModifiers;
			initialFitness = offspring2InitialFitness;
		} else return; //no candidate modifiers for this individual

		if (candidates.isEmpty()) return;
		
		Set<TestFitnessFunction> coveredGoals = ((DynaMOSA) algorithm).getCoveredGoals();
		goals.removeAll(coveredGoals);
		
		for (FitnessFunction<?> g: goals) {
			Double individualFitness = ((TestChromosome) individual).getFitnessValues().get(g);
			if (individualFitness == null) {
				continue; // some goals may still be beyond the current frontier of DynaMosa
				//throw new RuntimeException("new generation offspring misses a fitness value for: " + g);
			}
			Double parentFitness = initialFitness.get(g);
			if (parentFitness == null || individualFitness < parentFitness) { //NB: parentFitness==null implies that the individual reaches a new goal, beyond of the parent's frontier
				//offspring improves over parent for at least a goal
				storeSuccessfulModifiersForLS(candidates);
				break;
			}
		}
		
		//done with this individual
		candidates.clear();
	}
	
	private	void storeSuccessfulModifiersForLS(List<GenericMethod> candidateModifiers) {
		
		for (GenericMethod m: candidateModifiers) {
			
			if (iterationSuccessfullModifiers.contains(m)) {
				continue; //already considered for this test
			}
			iterationSuccessfullModifiers.add(m);
			
			Integer ttl = successfulModifiers.get(m);
			if (ttl == null) { /* new item */
				ttl = 5; // newly found item, keep it for a bit
			}
			ttl += 2; // item from a previous iteration, reward it with some ttl
			successfulModifiers.put(m, ttl);
		}
	}

	@Override
	public void modification(Chromosome individual)  { /*nothing to do */ }
	
	@Override
	public void iteration(GeneticAlgorithm algorithm)  { 
		List<GenericMethod> toRemove = new LinkedList<>(); 
		for (GenericMethod m: successfulModifiers.keySet()) { // decrease the ttl of unseen modifiers
			if (!iterationSuccessfullModifiers.contains(m)) {
				Integer ttl = successfulModifiers.get(m);
				ttl--;
				if (ttl > 0) {
					successfulModifiers.put(m, ttl);
				} else {
					toRemove.add(m);
				}
			}
		}
		for (GenericMethod m : toRemove) {
			successfulModifiers.remove(m);			
		}
		iterationSuccessfullModifiers.clear();
		//LoggingUtils.getEvoLogger().info("\n* {} modifiers: {}", successfulModifiers.size(), successfulModifiers.toString());
	}
	
	@Override
	public void searchFinished(GeneticAlgorithm algorithm)  { /*nothing to do */ }
		
}
