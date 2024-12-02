/*
 * Copyright (C) 2010-2018 Gordon Fraser, Andrea Arcuri and EvoSuite
 * contributors
 *
 * This file is part of EvoSuite.
 *
 * EvoSuite is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3.0 of the License, or
 * (at your option) any later version.
 *
 * EvoSuite is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with EvoSuite. If not, see <http://www.gnu.org/licenses/>.
 */
package org.evosuite.ga.metaheuristics.mosa;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.evosuite.Properties.Criterion;
import org.evosuite.Properties.PathConditionTarget;
import org.evosuite.coverage.branch.BranchCoverageTestFitness;
import org.evosuite.coverage.pathcondition.PathConditionCoverageGoalFitness;
import org.evosuite.ga.metaheuristics.mosa.structural.PathConditionManager;
import org.evosuite.ga.metaheuristics.mosa.structural.SeepepManager;
import org.evosuite.ga.metaheuristics.mosa.structural.AidingPathConditionManager;
import org.evosuite.testcase.execution.ExecutionTracer;
import org.evosuite.utils.ArrayUtil;
import org.evosuite.Properties;
import org.evosuite.ga.ChromosomeFactory;
import org.evosuite.ga.FitnessFunction;
import org.evosuite.ga.archive.Archive;
import org.evosuite.ga.comparators.OnlyCrowdingComparator;
import org.evosuite.ga.metaheuristics.mosa.structural.MultiCriteriaManager;
import org.evosuite.ga.operators.ranking.CrowdingDistance;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.testcase.TestFitnessFunction;
import org.evosuite.utils.LoggingUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of the DynaMOSA (Many Objective Sorting Algorithm) described in the paper
 * "Automated Test Case Generation as a Many-Objective Optimisation Problem with Dynamic Selection
 * of the Targets".
 *
 * @author Annibale Panichella, Fitsum M. Kifetew, Paolo Tonella
 */
public class DynaMOSA extends AbstractMOSA {

	private static final long serialVersionUID = 146182080947267628L;

	private static final Logger logger = LoggerFactory.getLogger(DynaMOSA.class);

    /**
     * Manager to determine the test goals to consider at each generation
     */
	protected MultiCriteriaManager goalsManager = null;

	protected CrowdingDistance<TestChromosome> distance = new CrowdingDistance<>();

	private int unchangedPopulationIterations = 0; /*GIO: Reset*/

	/**
	 * Constructor based on the abstract class {@link AbstractMOSA}.
	 *
	 * @param factory
	 */
	public DynaMOSA(ChromosomeFactory<TestChromosome> factory) {
		super(factory);
	}

    /**
     * {@inheritDoc}
     */
	@Override
	protected void evolve() {
		int goodCrosoversAtBegin = goodOffsprings; /*GIO: Reset*/

		// Generate offspring, compute their fitness, update the archive and coverage goals.
		List<TestChromosome> offspringPopulation = this.breedNextGeneration();

		// Create the union of parents and offspring
		List<TestChromosome> union = new ArrayList<>(this.population.size() + offspringPopulation.size());
		union.addAll(this.population);
		union.addAll(offspringPopulation);

		// Ranking the union
		logger.debug("Union Size = {}", union.size());

		// Ranking the union using the best rank algorithm (modified version of the non dominated
		// sorting algorithm)
		this.rankingFunction.computeRankingAssignment(union, this.goalsManager.getCurrentGoals());

		// let's form the next population using "preference sorting and non-dominated sorting" on the
		// updated set of goals
		int remain = Math.max(Properties.POPULATION, this.rankingFunction.getSubfront(0).size());
		
		if(Properties.AVOID_REPLICAS_OF_INDIVIDUALS) { /*GIO: Prevent multiple copies of individuals*/
			remain = Math.min(remain, union.size());
		}

		int index = 0;
		this.population.clear();
		// Obtain the first front
		List<TestChromosome> front = this.rankingFunction.getSubfront(index);

		// Successively iterate through the fronts (starting with the first non-dominated front)
		// and insert their members into the population for the next generation. This is done until
		// all fronts have been processed or we hit a front that is too big to fit into the next
		// population as a whole.
		while ((remain > 0) && (remain >= front.size()) && !front.isEmpty()) {

			// Assign crowding distance to individuals
			this.distance.fastEpsilonDominanceAssignment(front, this.goalsManager.getCurrentGoals());

			// Add the individuals of this front
			addToPopulation(front); //GIO: population.addAll(front);

			// Decrement remain
			remain = remain - front.size();

			// Obtain the next front
			index++;

			if (remain > 0) {
				front = this.rankingFunction.getSubfront(index);
			} 
		} 

		// In case the population for the next generation has not been filled up completely yet,
		// we insert the best individuals from the current front (the one that was too big to fit
		// entirely) until there are no more free places left. To this end, and in an effort to
		// promote diversity, we consider those individuals with a higher crowding distance as
		// being better.
		if (remain > 0 && !front.isEmpty()) { // front contains individuals to insert
			this.distance.fastEpsilonDominanceAssignment(front, this.goalsManager.getCurrentGoals());
			front.sort(new OnlyCrowdingComparator<>());
			for (int k = 0; k < remain; k++) {
				addToPopulation(front.get(k)); //GIO: this.population.add(front.get(k));	
			}
		}

		if(Properties.NO_CHANGE_ITERATIONS_BEFORE_RESET > 0) { /*GIO: Reset*/ 
			boolean someChanges = goodOffsprings > goodCrosoversAtBegin;
			handleReset(someChanges); 
		}

		this.currentIteration++;
		
		// Console output each 50 iterations
		if (currentIteration % 50 == 0) {
			LoggingUtils.getEvoLogger().info("\n***ITERATION: {}", currentIteration);
			//LoggingUtils.getEvoLogger().info("Population size= {}", population.size());
			//LoggingUtils.getEvoLogger().info("N. fronts = {}", ranking.getNumberOfSubfronts());
			LoggingUtils.getEvoLogger().info("* Covered goals = {}", goalsManager.getCoveredGoals().size());
			LoggingUtils.getEvoLogger().info("* Current goals = {}", goalsManager.getCurrentGoals().size());
			int numOfPCGoals = 0;
			for (FitnessFunction<TestChromosome> g : goalsManager.getCurrentGoals()) {
				if (g instanceof PathConditionCoverageGoalFitness) ++numOfPCGoals;
			}
			LoggingUtils.getEvoLogger().info("* Current PC goals = {}", numOfPCGoals);
			if(ArrayUtil.contains(Properties.CRITERION, Criterion.BRANCH_WITH_AIDING_PATH_CONDITIONS)) {
				int satisfied = ((AidingPathConditionManager) goalsManager).getCoveredPathConditions().size();
				LoggingUtils.getEvoLogger().info("* Satisfied PC goals = {}", satisfied);
			}

			//LoggingUtils.getEvoLogger().info("Uncovered goals = {}", goalsManager.getUncoveredGoals().size());
			LoggingUtils.getEvoLogger().info("* {} no change iterations,  {} resets, {} good offsprings ({} mutation only)", unchangedPopulationIterations, resets, goodOffsprings, goodOffspringsMutationOnly);
			LoggingUtils.getEvoLogger().info("* Top front includes {} individuals:", rankingFunction.getSubfront(0).size());
			for (TestChromosome c : rankingFunction.getSubfront(0)) {
				printInfo(c);			
			}
			/*for (int f = 1; f < rankingFunction.getNumberOfSubfronts(); f++) {
				LoggingUtils.getEvoLogger().info("* {} front includes {} individuals:", f, rankingFunction.getSubfront(f).size());
				for (TestChromosome c : rankingFunction.getSubfront(f)) {
					printInfo(c);			
				}	
			}*/
		}
		//logger.debug("N. fronts = {}", ranking.getNumberOfSubfronts());
		//logger.debug("1* front size = {}", ranking.getSubfront(0).size());
		logger.debug("Covered goals = {}", goalsManager.getCoveredGoals().size());
		logger.debug("Current goals = {}", goalsManager.getCurrentGoals().size());
		logger.debug("Uncovered goals = {}", goalsManager.getUncoveredGoals().size());
	}


	private int resets = 0;
	private void printInfo(TestChromosome c) {
		String fits = "";
		for (FitnessFunction<TestChromosome> g : goalsManager.getCurrentGoals()) {
			if (!(g instanceof PathConditionCoverageGoalFitness)) continue;
			fits += "Pc" + ((PathConditionCoverageGoalFitness) g).getPathConditionId();
			fits += "=" + g.getFitness(c) + ",";
		} 
		fits += " from it " + c.getAge();
		LoggingUtils.getEvoLogger().info("* id = {}, PC fits = {}, fitness = {}", System.identityHashCode(c), fits, c.getFitness());			
		LoggingUtils.getEvoLogger().info("TEST CASE = {}", ((TestChromosome)c).getTestCase().toString());			
	}
	

	private void logFrontierBranches() {
		LoggingUtils.getEvoLogger().info("============= LOG FRONTIER BRANCHES INFO ===============");			

		Map<FitnessFunction<TestChromosome>, TestChromosome> bestIndividuals = new LinkedHashMap<>();

		Set<TestFitnessFunction> frontier = goalsManager.getCurrentGoals();		
		for (TestFitnessFunction goal : frontier) {
			if (!(goal instanceof BranchCoverageTestFitness)) continue;
			BranchCoverageTestFitness branch = (BranchCoverageTestFitness) goal;
			if (branch.getBranch() == null) continue;

			double maxFiteness = Double.MAX_VALUE;
			TestChromosome bestTest = null;
			for (TestChromosome c : rankingFunction.getSubfront(0)) {
				if (goal.getFitness(c) < maxFiteness) {
					maxFiteness = goal.getFitness(c) ;
					bestTest = c;
				}
			}
			bestIndividuals.put(goal, bestTest);
		}
		
		for (Entry<FitnessFunction<TestChromosome>, TestChromosome> entry : bestIndividuals.entrySet()) {
			FitnessFunction<TestChromosome> goal = entry.getKey();
			TestChromosome bestTest = entry.getValue();
			LoggingUtils.getEvoLogger().info("* Test for frontier branch: {}, during search: {} ", goal.toString(), System.identityHashCode(this));
			if (bestTest != null) {
				LoggingUtils.getEvoLogger().info("  Fitness: {} from iter {}", goal.getFitness(bestTest), bestTest.getAge());			
				LoggingUtils.getEvoLogger().info("  TEST CASE = {}", ((TestChromosome)bestTest).getTestCase().toString());
			} else {
				LoggingUtils.getEvoLogger().info("  NO BEST TEST, CHECK THIS");
			}
		}
		
		
		LoggingUtils.getEvoLogger().info("=== NUM. FRONTIER BRANCHES: {}", bestIndividuals.size());			
		LoggingUtils.getEvoLogger().info("* FRONTIER BRANCHES:");			
		for (FitnessFunction<TestChromosome> goal : bestIndividuals.keySet()) {
			BranchCoverageTestFitness branch = (BranchCoverageTestFitness) goal;
			LoggingUtils.getEvoLogger().info("frontier, {}, {}, {}, {}", branch.getBranchGoal().getId(), goal.getFitness(bestIndividuals.get(goal)), System.identityHashCode(this), goal.toString());
		}
		LoggingUtils.getEvoLogger().info("");

		for (TestFitnessFunction goal : goalsManager.getCoveredGoals()) {
			if (!(goal instanceof BranchCoverageTestFitness)) continue;
			BranchCoverageTestFitness branch = (BranchCoverageTestFitness) goal;
			if (branch.getBranch() == null) continue;
			LoggingUtils.getEvoLogger().info("covered, {}, {}, {}, {}", branch.getBranchGoal().getId(), 0.0, System.identityHashCode(this), goal.toString());
		}
		LoggingUtils.getEvoLogger().info("");

		for (TestFitnessFunction goal : goalsManager.getCoveredGoals()) {
			if (!(goal instanceof BranchCoverageTestFitness)) continue;
			BranchCoverageTestFitness branch = (BranchCoverageTestFitness) goal;
			if (branch.getBranch() == null) continue;
			LoggingUtils.getEvoLogger().info("* Test for covered branch: {}, during search: {}", branch.toString(), System.identityHashCode(this));
			LoggingUtils.getEvoLogger().info("  TEST CASE = {}", Archive.getArchiveInstance().getSolution(branch).getTestCase().toString());
		}
		LoggingUtils.getEvoLogger().info("");
		
	}

	
	private boolean handleReset(boolean changed) { /*GIO: Reset*/
		if (changed) unchangedPopulationIterations = 0;
		else unchangedPopulationIterations++;

		if (goalsManager.getCurrentGoals().isEmpty()) {
			return false; // search finished
		}

		if (unchangedPopulationIterations >  Properties.NO_CHANGE_ITERATIONS_BEFORE_RESET) {
			long time = System.currentTimeMillis();
			
			List<TestChromosome> newPopulation = new ArrayList<>();
			if (goalsManager.getCurrentGoals().size() == 1) {
				newPopulation.addAll(elitism());

			} else {
				newPopulation.addAll(rankingFunction.getSubfront(0));
			}
			
			//initializePopulation(); NB: prefer explicit setup to avoid reset of all search params
			population.clear();
			generateInitialPopulation(Properties.POPULATION - newPopulation.size());
		
			for (TestChromosome c : population) {
				c.updateAge(currentIteration);
				c.setChanged(true);
			}
			calculateFitness();
		
			population.addAll(0, newPopulation);
			
		
			// Calculate dominance ranks
			rankingFunction.computeRankingAssignment(population, goalsManager.getCurrentGoals());

			time = System.currentTimeMillis() - time;
			resets++;
			unchangedPopulationIterations = 0;
			
			LoggingUtils.getEvoLogger().info("*******************************");
			LoggingUtils.getEvoLogger().info("* Population reset at iteration " + currentIteration + " (took " + time + " msec)" + "elite size is {}", newPopulation.size());
			LoggingUtils.getEvoLogger().info("******************************* ");
			
			return true;
		}
		return false;
	}


	private void addToPopulation(List<TestChromosome> front) {
		for (TestChromosome individual : front) {
			addToPopulation(individual); 
		}
	}

	private void addToPopulation(TestChromosome t) {
		population.add(t);
		notifyInNextGeneration(t);
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void generateSolution() {
		logger.debug("executing generateSolution function");

		// Set up the targets to cover, which are initially free of any control dependencies.
		// We are trying to optimize for multiple targets at the same time.
		if (ArrayUtil.contains(Properties.CRITERION, Criterion.PATHCONDITION)){
			goalsManager = new PathConditionManager(fitnessFunctions, this, Properties.CRITERION.length == 1, false /*TODO: set by an option*/);
		} else if (ArrayUtil.contains(Properties.CRITERION, Criterion.BRANCH_WITH_AIDING_PATH_CONDITIONS)) {
			Properties.PATH_CONDITION_TARGET = PathConditionTarget.LAST_ONLY; //This option is mandatory with the selected criterion
			goalsManager = new AidingPathConditionManager(fitnessFunctions, this);			
		} else if (ArrayUtil.contains(Properties.CRITERION, Criterion.SEEPEP)){ /*SEEPEP: DAG coverage*/
			ExecutionTracer.enableSeepepTracing();
			goalsManager = new SeepepManager(fitnessFunctions);
		} else {
			this.goalsManager = new MultiCriteriaManager(this.fitnessFunctions);
		}
		
		LoggingUtils.getEvoLogger().info("* Initial Number of Goals in DynaMOSA = " +
				this.goalsManager.getCurrentGoals().size() +" / "+ this.getUncoveredGoals().size());

		logger.debug("Initial Number of Goals = " + this.goalsManager.getCurrentGoals().size());

		if (this.population.isEmpty()) {
			// Initialize the population by creating solutions at random.
			this.initializePopulation();
		}

		// Compute the fitness for each population member, update the coverage information and the
		// set of goals to cover. Finally, update the archive.
		// this.calculateFitness(); // Not required, already done by this.initializePopulation();

		// Calculate dominance ranks and crowding distance. This is required to decide which
		// individuals should be used for mutation and crossover in the first iteration of the main
		// search loop.
		this.rankingFunction.computeRankingAssignment(this.population, this.goalsManager.getCurrentGoals());
		for (int i = 0; i < this.rankingFunction.getNumberOfSubfronts(); i++){
			this.distance.fastEpsilonDominanceAssignment(this.rankingFunction.getSubfront(i), this.goalsManager.getCurrentGoals());
		}

		// Evolve the population generation by generation until all gaols have been covered or the
		// search budget has been consumed.
		while (!isFinished() && this.goalsManager.getUncoveredGoals().size() > 0) {
			this.evolve();
			this.notifyIteration();
		}
		completeCalculateFitness();
		//logFrontierBranches();
		this.notifySearchFinished();
	}

	protected void completeCalculateFitness() {
		if (goalsManager instanceof PathConditionManager) {
			((PathConditionManager) goalsManager).restoreInstrumentationForAllGoals(); //Needed for path conditions, since covered ones were removed
		}
		logger.debug("Calculating fitness for " + population.size() + " individuals");
		for (TestFitnessFunction goal: goalsManager.getCoveredGoals()){
			TestChromosome c = Archive.getArchiveInstance().getSolution(goal);
			completeCalculateFitness(c);
		}
	}

	/**
	 * Calculates the fitness for the given individual. Also updates the list of targets to cover,
	 * as well as the population of best solutions in the archive.
	 *
	 * @param c the chromosome whose fitness to compute
	 */
	@Override
	protected void calculateFitness(TestChromosome c) {
		if (!isFinished()) {
			// this also updates the archive and the targets
			this.goalsManager.calculateFitness(c, this);
			this.notifyEvaluation(c);
		}
	}

	/** 
	 * This method computes the fitness scores for all (covered and uncovered) goals
	 * @param c chromosome
	 */
	protected void completeCalculateFitness(TestChromosome c) {
		for (TestFitnessFunction fitnessFunction : this.goalsManager.getCoveredGoals()) {
			if (!c.getFitnessValues().containsKey(fitnessFunction))
				c.getFitness(fitnessFunction);
			//notifyEvaluation(c);
		}
		for (TestFitnessFunction fitnessFunction : this.goalsManager.getCurrentGoals()) {
			if (!c.getFitnessValues().containsKey(fitnessFunction))
				c.getFitness(fitnessFunction);
			//notifyEvaluation(c);
		}
	}

	@Override
	public List<? extends FitnessFunction<TestChromosome>> getFitnessFunctions() {
		if (goalsManager == null) {
			return super.getFitnessFunctions();
		}
		List<TestFitnessFunction> testFitnessFunctions = new ArrayList<>(goalsManager.getCoveredGoals());
		testFitnessFunctions.addAll(goalsManager.getUncoveredGoals());
		return testFitnessFunctions;
	}

	@Override /*GIO: Prevent multiple copies of individuals*/
	protected boolean keepOffspring(TestChromosome offspring, TestChromosome parent1, TestChromosome parent2) {
		for (FitnessFunction<TestChromosome> g : goalsManager.getCurrentGoals()){
			double newFitness = g.getFitness(offspring);
			double p1Fitness = g.getFitness(parent1);
			double p2Fitness = g.getFitness(parent2);
			if (newFitness < p1Fitness && newFitness < p2Fitness) { 
				return true;
			}
		}
		return false;
	}

	@Override /*GIO: Prevent multiple copies of individuals*/
	protected boolean checkForRemove(TestChromosome parent, TestChromosome offspring1, TestChromosome offspring2) {
		for (FitnessFunction<TestChromosome> g : goalsManager.getCurrentGoals()){
			double newFitness1 = offspring1 != null ? g.getFitness(offspring1) : Double.MAX_VALUE;
			double newFitness2 = offspring2 != null ? g.getFitness(offspring2) : Double.MAX_VALUE;
			double pFitness = g.getFitness(parent);
			if (pFitness < newFitness1 && pFitness < newFitness2) { 
				return false; //do not remove
			}
		}
		return true;//population.remove(parent);
	}

}
