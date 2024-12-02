package org.evosuite.ga.metaheuristics.mosa.structural;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.evosuite.Properties;
import org.evosuite.coverage.branch.BranchCoverageTestFitness;
import org.evosuite.coverage.pathcondition.ApcGoalFitness;
import org.evosuite.coverage.pathcondition.PathConditionCoverageGoal;
import org.evosuite.coverage.pathcondition.PathConditionCoverageGoalFitness;
import org.evosuite.ga.metaheuristics.GeneticAlgorithm;
import org.evosuite.ga.metaheuristics.mosa.jbse.JBSEManager;
import org.evosuite.ga.operators.ranking.RankingFunction;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.testcase.TestFitnessFunction;
import org.evosuite.testcase.execution.ExecutionTracer;
import org.evosuite.utils.LoggingUtils;

public class AidingPathConditionManager extends PathConditionManager {

	private static final long serialVersionUID = -3206971271103866231L;

	private Map<BranchCoverageTestFitness, ApcGroup> aidingPathConditions = new HashMap<>();	
	
	public AidingPathConditionManager(List<TestFitnessFunction> targets, GeneticAlgorithm<TestChromosome> algo) {
		super(targets, algo, false, true);
	}

	@Override
	public Set<TestFitnessFunction> getCoveredGoals() {
		Set<TestFitnessFunction> covered = new HashSet<>();
		for (TestFitnessFunction goal: super.getCoveredGoals()) {
			if (goal instanceof PathConditionCoverageGoalFitness) {
				continue; // do not count path-conditions as coverage goal: they are dynamically added/removed to aid other goals
			}
			covered.add(goal);
		}
		return covered;
	}

	public Set<TestFitnessFunction> getCoveredPathConditions() {
		Set<TestFitnessFunction> coveredPCs = new HashSet<>();
		for (TestFitnessFunction goal: super.getCoveredGoals()) {
			if (goal instanceof PathConditionCoverageGoalFitness) {
				coveredPCs.add(goal);
			}
		}
		return coveredPCs;
	}

	@Override
	public void calculateFitness(TestChromosome c, GeneticAlgorithm<TestChromosome> ga) {
		super.calculateFitness(c, ga);
		
		/* Search continues while there are coverage goals other than the path-condition goals */
		boolean stopSearch = true;
		for (TestFitnessFunction goal: this.getUncoveredGoals()) {
			if (!(goal instanceof PathConditionCoverageGoalFitness)) {
				stopSearch = false;
				break;
			}
		}
		if (stopSearch) {
			this.currentGoals.clear();
			this.getUncoveredGoals().clear();
		}
	}
	
	@Override
	public void restoreInstrumentationForAllGoals() {
		// we want to optimize the test suite wrt branch coverage only, thus
		// do not restore path condition goals, and remove the ones yet uncovered
		HashSet<TestFitnessFunction> goals = new HashSet<>(this.getUncoveredGoals());
		for (TestFitnessFunction goal: goals) {
			if (goal instanceof PathConditionCoverageGoalFitness) {
				this.doneWithPathCondition((PathConditionCoverageGoalFitness) goal);
			}
		}
	}

	public void manageAidingPathConditions(List<TestChromosome> population, RankingFunction<TestChromosome> rankingFunction, List<TestChromosome> bestFront, int currentIteration) {
		if (Properties.APC_RATE <= 0) {
			return;
		}
		if ((currentIteration + 1) % Properties.APC_RATE != 0) { // +1 to avoid activation at iteration 0 
			return;
		}
		
		// Remove APCs if done
		HashSet<BranchCoverageTestFitness> goalsWithApc = new HashSet<>(aidingPathConditions.keySet());
		for (BranchCoverageTestFitness goal: goalsWithApc) {
			if (!this.getCurrentGoals().contains(goal)) {
				aidingPathConditions.get(goal).stop();
				aidingPathConditions.remove(goal);			
			}
		}
		
		// Update APCs
		HashSet<TestFitnessFunction> currentGoals = new HashSet<>(this.getCurrentGoals()); //do copy because the next part can update the currentGoals by adding new ApcGoals
		for (TestFitnessFunction goal:  currentGoals) {			
			if (!(goal instanceof BranchCoverageTestFitness)) continue; //At the moment we use APCs only for branch-goals
			BranchCoverageTestFitness branchGoal = (BranchCoverageTestFitness) goal;
			if (branchGoal.getBranch() == null) continue; //just a fake branch-fitness for single-branch-method
						
			ApcGroup apcGroup = aidingPathConditions.get(branchGoal);
			if (apcGroup == null) {
				apcGroup = new ApcGroup(branchGoal, this);
				aidingPathConditions.put(branchGoal, apcGroup);
			}
			apcGroup.update(bestFront, currentIteration);
		}
		
		for (int i = 0; i < rankingFunction.getNumberOfSubfronts(); ++i) { //TODO: optimize
			for (TestChromosome tc: rankingFunction.getSubfront(i)) {
				tc.setChanged(true);
			}
		}
		rankingFunction.computeRankingAssignment(population, getCurrentGoals());
	}
	
	@Override
	public void iteration(GeneticAlgorithm<TestChromosome> algorithm) {
		super.iteration(algorithm);
		manageAidingPathConditions(algorithm.getPopulation(), algorithm.getRankingFunction(), algorithm.getRankingFunction().getSubfront(0), algorithm.getAge());
	}
		
	private static class ApcItem {
		final ApcGoalFitness goal;
		final AidingPathConditionManager manager;
		double bestFitness;
		int latestImprovementIteration;
		TestChromosome bestTest;
		boolean dismissed;
		boolean refined;
		
		public static ApcItem startNewItem(AidingPathConditionManager manager, ApcGoalFitness newApcGoal, boolean refined, int currentIteration) {
			ExecutionTracer.addEvaluatorForPathCondition(newApcGoal.getPathConditionGoal(), newApcGoal.getBranchGoal());
			ApcItem apcItem = new ApcItem(newApcGoal, manager, refined, currentIteration);
			manager.getCurrentGoals().add(newApcGoal); 
			return apcItem;
		}

		ApcItem(ApcGoalFitness goal, AidingPathConditionManager manager, boolean refined, int currentIteration) {
			this.goal = goal;
			this.manager = manager;
			bestFitness = Double.MAX_VALUE;
			latestImprovementIteration = currentIteration;
			bestTest = null;
			dismissed = false;
			this.refined = refined;
		}
		
		void dismiss() {
			dismissed = true;
			manager.doneWithPathCondition(goal);
		}

		public boolean isDismissed() {
			return dismissed;
		}

		public void updateTestAndFitness(double currFitness, int currentIteration, TestChromosome bestTest) {
			if (currFitness < bestFitness) {
				bestFitness = currFitness;
				latestImprovementIteration = currentIteration;
			} else if (currFitness != bestFitness) {
				LoggingUtils.getEvoLogger().info("\n\n* WARNING: BEST-FRONT HAS WORSE FITNESS ({} > {}) FOR TEST FOR GOAL: {} -- CHECK THIS", currFitness, bestFitness, goal.toString());	
			}
			this.bestTest = bestTest;
		}

		public void refine(ApcGroup hostApcGroup, int currentIteration) {
			if (isDismissed()) {
				return;
			}
			int[] convergingSoFar = goal.getEvaluator().getConverging();
			if (convergingSoFar == null || convergingSoFar.length == 0) {
				if (refined) {
					// no enabled/unsubsumed APC-clause ever converged. This (likely) means that there is no more useful APC-clause in this APCgoal
					LoggingUtils.getEvoLogger().info("\n\n[EVOSUITE] Dismissing APC, as no still-enabled clause ever converged: {}", goal);
					dismiss();					
				} else {
					refined = true; //remove at next round.
				}
					
				return;
			} 
			refined = false; //something is still converging, thus it makes sense to search for a test case to cover those elements.
			
			int[] alignedSpinObs = goal.getEvaluator().getAlignedSpinObs();
			int[] reverseSpinObs = goal.getEvaluator().getReverseSpinObs();
						
			// Possibly start refined ApCItem wrt converged clauses
			double[] convergedWithBestTest = (double[]) bestTest.getLastExecutionResult().getTrace().getPathConditionFeedbacks().get(
					goal.getPathConditionGoal().getPathConditionId()).get(0);
			String convergedItemsSpec = goal.getPathConditionGoal().getEvaluatorName();
			boolean someConverged = false;
			for (int i = 0; i < convergedWithBestTest.length - 1; ++i) { //ignore last item, which is the aggregated-fitness value
				if (convergedWithBestTest[i] == 0) {
					someConverged = true;
					convergedItemsSpec += ":" + i;
					//goal.getEvaluator().setDisabled(i, true);
					alignedSpinObs[i] = -1; // disable this counter as we already account for this clause among the converged clauses 
					reverseSpinObs[i] = -1; // disable this counter as we already account for this clause among the converged clauses
				}
			}
			if (someConverged) {
				LoggingUtils.getEvoLogger().info("[EVOSUITE] Computing new APC as refinement on {} of goal: {}", convergedItemsSpec, goal);
				LoggingUtils.getEvoLogger().info("[EVOSUITE] ...as best test scored {}", Arrays.toString(convergedWithBestTest));
				hostApcGroup.scheduleNewApcByUsingJBSE(bestTest, goal, convergedItemsSpec, currentIteration); 
				/* This is not needed because we now select all remaining clauses in the refinements below
				 * if (goal.getEvaluator().isAllDisabled()) {
					// no more APC clause is now enabled in this evaluator, thus it is useless to refine it
					LoggingUtils.getEvoLogger().info("\n\n[EVOSUITE] Dismissing APC, as no more clauses are enabled after refinement {}:", goal);
				} */
			} 

			// Collect information on clauses with aligned and reverse spin, respectively
			ArrayList<Integer> alignedSpinClauses = new ArrayList<>();
			ArrayList<Integer> reverseSpinClauses = new ArrayList<>();
			ArrayList<Integer> otherClauses = new ArrayList<>();
			for (int i = 0; i < alignedSpinObs.length; ++i) {
				double total = alignedSpinObs[i] + reverseSpinObs[i];
				if (total < 0) { //i-th clause is disabled
					continue;
				}
				if (total < 30) {
					otherClauses.add(i); //too few observations to make the decision
				} else {
					double alignmentFrequency = alignedSpinObs[i] / total;
					if (alignmentFrequency >= 0.9) { //more than 90% observations have aligned spin wrt fitness
						alignedSpinClauses.add(i);
					} else if (alignmentFrequency < 0.2) { //more than 80% observations have reverse spin wrt fitness
						reverseSpinClauses.add(i);						
					} else {
						otherClauses.add(i); // data are inconclusive for this clause						
					}
				}
			}
			//LoggingUtils.getEvoLogger().info("[EVOSUITE] DEBUG: alignedSpinObs: {}", Arrays.toString(alignedSpinObs));	
			//LoggingUtils.getEvoLogger().info("[EVOSUITE] DEBUG: reverseSpinObs: {}", Arrays.toString(reverseSpinObs));	
			//LoggingUtils.getEvoLogger().info("[EVOSUITE] DEBUG: alignedSpinClauses: {}", alignedSpinClauses);	
			//LoggingUtils.getEvoLogger().info("[EVOSUITE] DEBUG: reverseSpinClauses: {}", reverseSpinClauses);	
			//LoggingUtils.getEvoLogger().info("[EVOSUITE] DEBUG: inconclusiveSpinClauses: {}", otherClauses);	
			
			// Possibly start separate refined ApCItem wrt clauses with aligned spin
			if (!alignedSpinClauses.isEmpty()) {
				int[] alignedSpinClauses0 = new int[alignedSpinClauses.size()];
				for (int i = 0; i < alignedSpinClauses0.length; ++i) {
					alignedSpinClauses0[i] = alignedSpinClauses.get(i);
				}				
				LoggingUtils.getEvoLogger().info("[EVOSUITE] Refining on clauses {} of goal {} -- aligned spin wrt to fitness. GOAL: {}", Arrays.toString(alignedSpinClauses0), goal.getPathConditionGoal().getPathConditionId(), goal.toString());	
				goal.getEvaluator().disableAllButSome(alignedSpinClauses0);
				hostApcGroup.scheduleNewApcByCopy(goal, currentIteration); 
			}

			// Possibly start separate refined ApCItem wrt clauses with reverse spin
			if (!reverseSpinClauses.isEmpty()) {
				int[] reverseSpinClauses0 = new int[reverseSpinClauses.size()];
				for (int i = 0; i < reverseSpinClauses0.length; ++i) {
					reverseSpinClauses0[i] = reverseSpinClauses.get(i);
				}				
				LoggingUtils.getEvoLogger().info("[EVOSUITE] Refining on clauses {} of goal {} -- reverse spin wrt to fitness. GOAL: {}", Arrays.toString(reverseSpinClauses0), goal.getPathConditionGoal().getPathConditionId(), goal.toString());	
				goal.getEvaluator().disableAllButSome(reverseSpinClauses0);
				hostApcGroup.scheduleNewApcByCopy(goal, currentIteration); 
			}
			
			// Possibly start separate refined ApCItem wrt clauses with inconclusive spin
			if (!otherClauses.isEmpty()) {
				int[] other0 = new int[otherClauses.size()];
				for (int i = 0; i < other0.length; ++i) {
					other0[i] = otherClauses.get(i);
				}				
				LoggingUtils.getEvoLogger().info("[EVOSUITE] Refining on clauses {} of goal {} -- 'inconclusive' spin wrt to fitness. GOAL: {}", Arrays.toString(other0), goal.getPathConditionGoal().getPathConditionId(), goal.toString());	
				goal.getEvaluator().disableAllButSome(other0);
				hostApcGroup.scheduleNewApcByCopy(goal, currentIteration); 
			}
			/*if (convergingSoFar != null && convergingSoFar.length > 0)  {
				LoggingUtils.getEvoLogger().info("[EVOSUITE] Refining clauses of goal by keeping only clauses {}, as those clauses converge occasionally but none converges with the best test. GOAL: {}", Arrays.toString(convergingSoFar), goal.toString());	
				goal.getEvaluator().disableAllButSome(convergingSoFar);
				dismiss(); //generate the refined goal and dismiss this one
				hostApcGroup.scheduleNewApcByCopy(goal, currentIteration); 
			}*/

			dismiss(); //Dismiss this ApcItem as now its corresponding refined items are running, and all clauses are being considered thereby
		}

	}
	
	public static class ApcGroup {
		public static final int MAX_NO_IMPROVEMENT_ITERS_BEFORE_COMPUTING_APC = 100; 
		public static final int MAX_ACTIVE_APC_PER_BRANCH = 10; 
		
		private final AidingPathConditionManager manager;
		private final BranchCoverageTestFitness branchGoal;
		private double bestFitnessOfBranchGoal = Double.MAX_VALUE;
		private int latestImprovementIterationOfBranchGoal = 0;
		private TestChromosome bestTestOfBranchGoal = null;
		private ArrayList<ApcItem> apcItems = new ArrayList<>(); //the set of apcGoals managed for the branchGoal	
		private ArrayList<ApcGoalFitness> apcGoalsFromJbse = new ArrayList<>(); //the set of apcGoals managed for the branchGoal	
		private int nextAvaliableApcIndex = 0;
		
		public ApcGroup(BranchCoverageTestFitness branchGoal, AidingPathConditionManager manager) {		
			this.branchGoal = branchGoal;
			this.manager = manager;
		}
		
		public int getNextApcIndex() {
			return nextAvaliableApcIndex++; 
		}

		private void checkSuccess() {
			boolean done = !manager.getCurrentGoals().contains(branchGoal); //branch has been covered --> done
			if (!done) {
				//set done, if any APC succeeded --> i.e., stop aiding until the next local optimum (if any)
				for (ApcItem apcItem: apcItems) {
					if (apcItem.isDismissed()) {
						continue;
					}
					done = !manager.getCurrentGoals().contains(apcItem.goal);
					if (done) {
						break;
					}
				}
			}
			if (done) { 
				stop();
			}
		}
		
		public void stop() {
			for (ApcItem apcItem: apcItems) { //TODO: possible critical raise with apcItems added by threads that nvoke JBSE
				apcItem.dismiss();
			}	
			apcItems.clear();
		}
		
		public void update(List<TestChromosome> bestFront, int currentIteration) {
			checkSuccess();
			
			startApcGoalsFromJbseIfAny(currentIteration);
						
			updateBranchFitness(bestFront,currentIteration);
			if (bestFitnessOfBranchGoal >= 1.0) {
				return; //tests are approaching but did not yet reach this branch, then we do not trigger no APC support yet
			}
			updateApcFitness(bestFront, currentIteration);
			
			// decide if it is time to computes (new) APCs
			int lastImprovementIter = latestImprovementIterationOfBranchGoal;
			for (ApcItem apcGoal: apcItems) {
				if (apcGoal.latestImprovementIteration > lastImprovementIter) {
					lastImprovementIter = apcGoal.latestImprovementIteration;
				}
			}
			if (currentIteration - lastImprovementIter >= MAX_NO_IMPROVEMENT_ITERS_BEFORE_COMPUTING_APC) { 
				LoggingUtils.getEvoLogger().info("\n\n[EVOSUITE] AIDING FRONTIER GOAL: {} - fit {} - ITERATION {}, NO IMPROVEMENT IN THE LAST {} ITERATIONS", branchGoal, bestFitnessOfBranchGoal, currentIteration, MAX_NO_IMPROVEMENT_ITERS_BEFORE_COMPUTING_APC);
				computeAPCs(currentIteration);
			}
		}

		private void updateBranchFitness(List<TestChromosome> bestFront, int currentIteration) {
			//get the test with best fitness for this branchGoal
			double currentBranchGoalFitness = Double.MAX_VALUE;
			TestChromosome bestTest = null;
			for (TestChromosome c : bestFront) {
				if (c.size() == 0) continue; //TODO: understand this better
				if (branchGoal.getFitness(c) < currentBranchGoalFitness) {
					currentBranchGoalFitness = branchGoal.getFitness(c) ;
					bestTest = c;
				}
			}
			if (bestTest == null) {
				// Defensive check: this should never happen
				LoggingUtils.getEvoLogger().info("\n\n[EVOSUITE] * WARNING: NO BEST-FRONT TEST FOR GOAL: {} -- CHECK THIS", branchGoal.toString());
			} 
			if (currentBranchGoalFitness < bestFitnessOfBranchGoal) {
				bestFitnessOfBranchGoal = currentBranchGoalFitness;
				latestImprovementIterationOfBranchGoal = currentIteration;
			} else if (currentBranchGoalFitness != bestFitnessOfBranchGoal) {
				LoggingUtils.getEvoLogger().info("\n\n[EVOSUITE] * WARNING: BEST-FRONT HAS WORSE FITNESS ({} > {}) FOR TEST FOR GOAL: {} -- CHECK THIS", currentBranchGoalFitness, bestFitnessOfBranchGoal, branchGoal.toString());	
			}
			bestTestOfBranchGoal = bestTest;
		}
		
		private void updateApcFitness(List<TestChromosome> bestFront, int currentIteration) {
			for (ApcItem apcItem: apcItems) {
				if (apcItem.isDismissed()) {
					continue;
				}
				// Update info if fitness has improved
				double currFitness = Double.MAX_VALUE;
				TestChromosome bestTest = null;
				for (TestChromosome c : bestFront) {
					if (c.size() == 0) continue; //TODO: understand this better
					double f = apcItem.goal.getFitness(c);
					if (f < currFitness) {
						currFitness = f;
						bestTest = c;
					}
				}
				if (bestTest == null) {
					// Defensive check: this should never happen
					LoggingUtils.getEvoLogger().info("\n\n[EVOSUITE] * WARNING: NO BEST-FRONT TEST FOR GOAL: {} -- CHECK THIS", apcItem.goal.toString());
				} 
				apcItem.updateTestAndFitness(currFitness, currentIteration, bestTest);
			}
		}

		private void computeAPCs(int currentIteration) {
			//Clone and minimize the test case
			/*TestChromosome minimizedTest = (TestChromosome) bestTest.clone();
			TestCaseMinimizer minimizer = new TestCaseMinimizer(branchF);
			minimizer.minimize(minimizedTest);
			if (branchF.getFitness(bestTest) != branchF.getFitness(minimizedTest)) {
				LoggingUtils.getEvoLogger().info("[JBSE] Failed to generate the path condition: mimized test fitness ("+ branchF.getFitness(minimizedTest) +") differs from original one (" + branchF.getFitness(bestTest) + ")");
				return null;
			}*/
			if (apcItems.isEmpty()) {
				LoggingUtils.getEvoLogger().info("[EVOSUITE] Computing first APC for branch goal: {}", branchGoal);
				scheduleNewApcByUsingJBSE(bestTestOfBranchGoal, branchGoal, "-", currentIteration);
				return;
			}
			ArrayList<ApcItem> items = new ArrayList<>(apcItems);
			for (ApcItem apcItem: items) {
				apcItem.refine(this, currentIteration);
			}
		}

		private boolean canScheduleNewApc() {
			return activeApcs() < MAX_ACTIVE_APC_PER_BRANCH;
		}
		
		public void scheduleNewApcByCopy(ApcGoalFitness currentApcGoal, int currentIteration) {
			if (currentApcGoal == null || !canScheduleNewApc()) {//we can add further support at this moment 
				return; 
			}
			PathConditionCoverageGoal pcGoal = currentApcGoal.getPathConditionGoal();
			ApcItem apcItem = scheduleNewApc(getNextApcIndex(), pcGoal.getClassName(), pcGoal.getMethodName(), pcGoal.getEvaluatorName(), true, currentIteration);
			apcItem.goal.getEvaluator().updtDisabled(currentApcGoal.getEvaluator().getDisabled()); //copy the disabled settings of the source evaluator
		}
		
		public void scheduleNewApcByUsingJBSE(TestChromosome bestTest, TestFitnessFunction fitnessFunction, String convergedItemsSpec, int currentIteration) {
			if (!canScheduleNewApc()) {//we can add further support at this moment 
				return; //TODO: here we loose the convergedItemsSpec
			}
			
			//We run JBSE that will run in a separate thread, and will schedule the new goal upon its termination
			JBSEManager.computeAPCGoals(this, fitnessFunction, /* minimizedTest*/(TestChromosome) bestTest.clone(), branchGoal.getBranch(), getNextApcIndex(), convergedItemsSpec);				
		}	
		
		private synchronized void startApcGoalsFromJbseIfAny(int currentIteration) {
			if (!apcGoalsFromJbse.isEmpty()) {
				for (ApcGoalFitness apcGoal: apcGoalsFromJbse) {
					LoggingUtils.getEvoLogger().info("[EVOSUITE] Scheduling new APC: {}", apcGoal.toString());
					ApcItem apcItem = ApcItem.startNewItem(manager, apcGoal, false, currentIteration);
					apcItems.add(apcItem);		
				}
				apcGoalsFromJbse.clear();
			}
		}

		private ApcItem scheduleNewApc(int apcIndex, String className, String methodName, String evaluatorName, boolean refined, int currentIteration) {
			ApcGoalFitness newApcGoal = new ApcGoalFitness(branchGoal, bestFitnessOfBranchGoal,
					new PathConditionCoverageGoal(branchGoal.getBranch().getActualBranchId() * 10000 + apcIndex,
							className, 
							methodName, 
							evaluatorName));
			LoggingUtils.getEvoLogger().info("[EVOSUITE] Scheduling new APC: {}", newApcGoal.toString());
			ApcItem apcItem = ApcItem.startNewItem(manager, newApcGoal, refined, currentIteration);
			apcItems.add(apcItem);	
			return apcItem;
		}
		
		public synchronized void scheduleNewApcFromJbse(int apcIndex, String className, String methodName, String evaluatorName, boolean refined, int currentIteration) {
			ApcGoalFitness newApcGoal = new ApcGoalFitness(branchGoal, bestFitnessOfBranchGoal,
					new PathConditionCoverageGoal(branchGoal.getBranch().getActualBranchId() * 10000 + apcIndex,
							className, 
							methodName, 
							evaluatorName));
			apcGoalsFromJbse.add(newApcGoal);	
			startApcGoalsFromJbseIfAny(currentIteration); //TODO: eliminate if we use threads.
		}

		private int activeApcs() {
			int count = 0;
			for (ApcItem apcItem: apcItems) { 
				if (!apcItem.isDismissed()) {
					++count;
				}
			}
			return count;
		
		}
	}

}
