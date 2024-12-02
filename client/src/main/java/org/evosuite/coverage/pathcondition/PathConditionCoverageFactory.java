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

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.evosuite.Properties;
import org.evosuite.coverage.branch.BranchCoverageGoal;
import org.evosuite.coverage.pathcondition.PathConditionTraversedBranch.NeverTraversedException;
import org.evosuite.rmi.ClientServices;
import org.evosuite.testcase.TestFitnessFunction;
import org.evosuite.testsuite.AbstractFitnessFactory;
import org.evosuite.testsuite.TestSuiteFitnessFunction;
import org.evosuite.utils.LoggingUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * PathCondtionCoverageFactory class.
 * </p>
 * 
 * @author G. Denaro
 */
public class PathConditionCoverageFactory extends AbstractFitnessFactory<PathConditionCoverageGoalFitness> { /*SUSHI: Path condition fitness*/

	private static final Logger logger = LoggerFactory.getLogger(PathConditionCoverageFactory.class);

	private Map<PathConditionCoverageGoalFitness, Set<BranchCoverageGoal>> coverageGoals  = new HashMap<>(); //path conditions with branch coverage information
	private Map<PathConditionTraversedBranch, BranchCoverageGoal> searchRelevantBranchesAllFormats = null; //

	private static PathConditionCoverageFactory _I = null; /* the singleton instance */
	
	public static PathConditionCoverageFactory _I() {
		if (_I == null) {
			_I = new PathConditionCoverageFactory();
		}
		return _I;
	}

	private PathConditionCoverageFactory() {
		super();
		long start = System.currentTimeMillis();
		addGoals(Properties.PATH_CONDITION);
		goalComputationTime = System.currentTimeMillis() - start;	
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.evosuite.coverage.TestCoverageFactory#getCoverageGoals()
	 */
	/** {@inheritDoc} */
	@Override
	public List<PathConditionCoverageGoalFitness> getCoverageGoals() {
		return new LinkedList<>(coverageGoals.keySet());
	}
	
	public List<PathConditionCoverageGoalFitness>  getNewlyInjectedCoverageGoals() {
		String retrievedFromMaster = ClientServices.getInstance().getClientNode().retrieveInjectedFitnessFunctions();
		if (retrievedFromMaster != null) {
			String[] goalSpecs = retrievedFromMaster.split(":");
			return addGoals(goalSpecs);			
		} else {
			return null;
		}
	}
	
	private List<PathConditionCoverageGoalFitness> addGoals(String[] goalSpecs) {
		final ArrayList<PathConditionCoverageGoalFitness> ret = new ArrayList<>();
		int id = coverageGoals.size(); //first not-yet taken id
		for (String spec : goalSpecs) {
			String parts[] = Properties.pathConditionSplitClassMethodEvaluator(spec);
			if (parts.length != 3) {
				LoggingUtils.getEvoLogger().info("Wrongly specified path condition goal. Skipping this goal: " + spec);
				continue;
			}
			PathConditionCoverageGoalFitness goal = createPathConditionCoverageTestFitness(id, parts[0], parts[1], parts[2]);
			coverageGoals.put(goal, new HashSet<>());
			ret.add(goal);
			id++;
		}		
		return ret;
	}
	public Set<BranchCoverageGoal> getBranchCovInfo(PathConditionCoverageGoalFitness pc, List<BranchCoverageGoal> searchRelevantBranches) {
		if (Properties.PATH_CONDITION_SUSHI_BRANCH_COVERAGE_INFO == null) {
			return new HashSet<>(); 
		}
		
		Set<BranchCoverageGoal> covInfo = coverageGoals.get(pc);
				
		if (covInfo != null && covInfo.isEmpty() && searchRelevantBranches != null) {
			if (searchRelevantBranchesAllFormats == null) { /* search relevant branches are cached at the first invocation */
				searchRelevantBranchesAllFormats = new HashMap<>(searchRelevantBranches.size());
				for (BranchCoverageGoal b : searchRelevantBranches) {
					try {
						searchRelevantBranchesAllFormats.put(PathConditionTraversedBranch.makeFromOtherFormat(b), b);
					} catch (NeverTraversedException e) {
						/* ignore this objectives since path conditions will never traverse it */
					}
				}
			}
			
			String coverageTracesFileName = null, branchesFileName = null;
			try {				
				coverageTracesFileName = Properties.PATH_CONDITION_SUSHI_BRANCH_COVERAGE_INFO[0];
				branchesFileName = Properties.PATH_CONDITION_SUSHI_BRANCH_COVERAGE_INFO[1];	
				Set<BranchCoverageGoal> branches = loadBranchCovInfo(pc.getPathConditionGoal().getEvaluatorName(), coverageTracesFileName, branchesFileName, searchRelevantBranchesAllFormats);
				covInfo.addAll(branches);
			} catch (Exception e) {
				LoggingUtils.getEvoLogger().info("Bad coverage information for path condition " + pc + " in files " + coverageTracesFileName + ", " + branchesFileName + 
						" - Exception is: " + Arrays.toString(e.getStackTrace()));
			}
		}
		
		return covInfo;
	}


	/**
	 * Returns the SUSHI trace identifier for a given path condition evaluator.
	 * 
	 * @param evaluatorName The name of a path condition evaluator file (possibly with paths).
	 * @param searchRelevantBranchesAllFormats 
	 * @return an {@code int[]} that will have always two members: The first is the number of the method, 
	 *         the second is the number of the trace for that method.
	 * @throws NumberFormatException if {@code evaluatorName} has not the right format.
	 */
	private Set<BranchCoverageGoal> loadBranchCovInfo(String evaluatorName, String coverageTracesFileName, String branchesFileName, Map<PathConditionTraversedBranch, BranchCoverageGoal> searchRelevantBranchesAllFormats) throws IOException {
		
		// Extract the trace id from the evaluator name
		// - We assume that the evaluator name has shape path/to/file/EvosuiteWrapper_<methodNumber>_<traceNumber>.java
		final String[] evaluatorNameSplit = evaluatorName.split("_");
		final String[] traceNumberSplit = evaluatorNameSplit[evaluatorNameSplit.length - 1].split("\\.");
		int methodId = Integer.parseInt(evaluatorNameSplit[evaluatorNameSplit.length - 2].trim());
		int traceId = Integer.parseInt(traceNumberSplit[0].trim());

		final int[] coverageTrace = extractTheCovTrace(methodId, traceId, coverageTracesFileName);
		
		if (coverageTrace == null) {
			LoggingUtils.getEvoLogger().info("No coverage information for path condition " + evaluatorName + " in file " + coverageTracesFileName);
		}
		
		final Set<BranchCoverageGoal> coverageInfo = loadTheBranchesOfTheTrace(coverageTrace, branchesFileName, searchRelevantBranchesAllFormats);
		
		return coverageInfo;
	}

	/**
	 * Returns the coverage information for a trace as a list of SUSHI branch identifiers.
	 * 
	 * @param methodId an {@code int} that stands for the number of the method
	 * @param traceId an {@code int} that stands for the number of the trace for the method.
	 * @param coverageTracesFileName a {@link String}, a valid pathname to the coverage file
	 *        {@code coverage.txt}.
	 * @return an {@code int[]} containing all the identifying numbers of the branches covered by 
	 *         that trace, or {@code null} if no trace with identifier {@code traceId} exists in the
	 *         file pointed by {@code coverageTracesFileName}.
	 * @throws IOException if by some reason reading from the file fails (e.g., because the file 
	 *         does not exist).
	 * @throws NumberFormatException if the file pointed by {@code coverageTracesFileName} has not 
	 *         the right format.
	 */
	private int[] extractTheCovTrace(int methodId, int traceId, String coverageTracesFileName) throws IOException {
		try (final BufferedReader r = Files.newBufferedReader(Paths.get(coverageTracesFileName))) {
			String line;
			while ((line = r.readLine()) != null) {
				final String[] fields = line.split(",");
				final int methodNumber = Integer.parseInt(fields[0].trim());
				final int traceNumber = Integer.parseInt(fields[1].trim());
				if (methodNumber == methodId && traceNumber == traceId) {
					final int[] retVal = new int[fields.length - 3];
					for (int i = 0; i < retVal.length; ++i) {
						retVal[i] = Integer.parseInt(fields[i + 3].trim());
					}
					return retVal;
				}
			}
			return null;
		}
	}

	/**
	 * Translates the coverage information for a trace returned by {@link #extractTheCovTrace(int[], String)}.
	 * 
	 * @param coverageTrace an {@code int[]} containing all the identifying numbers of the branches covered by 
	 *         some trace.
	 * @param branchesFileName a {@link String}, a valid pathname to the branches file {@code branches.txt}.
	 * @param searchRelevantBranchesAllFormats 
	 * @return a {@link Set}{@code <}{@link PathConditionTraversedBranch}{@code >} whose members contain the information about
	 *         the branches in {@code coverageTrace}.
	 * @throws IOException if by some reason reading from the file fails (e.g., because the file 
	 *         does not exist).
	 * @throws RuntimeException if {@code coverageTrace} contains a branch identifier that does not exist in the file 
	 *         pointed by {@code branchesFileName}.
	 */
	private Set<BranchCoverageGoal> loadTheBranchesOfTheTrace(int[] coverageTrace, String branchesFileName, Map<PathConditionTraversedBranch, BranchCoverageGoal> searchRelevantBranchesAllFormats) throws IOException {
		final HashSet<BranchCoverageGoal> retVal = new HashSet<>();
		int nextBranchIndex = 0;
		
		List<PathConditionTraversedBranch> toDebug1 = new ArrayList<>();
		List<PathConditionTraversedBranch> toDebug2 = new ArrayList<>();
		for (PathConditionTraversedBranch br : searchRelevantBranchesAllFormats.keySet()) {
			if (br.className.equals("com/google/javascript/jscomp/RemoveUnusedVars") && 
					br.methodName.startsWith("traverseNode")) {
				toDebug1.add(br);
			}
		}
		
reloadFile:
		while (true) {
			try (final BufferedReader r = Files.newBufferedReader(Paths.get(branchesFileName))) {
				String line;
				int lineCounter = 0;
				while ((line = r.readLine()) != null) {
					if (lineCounter == coverageTrace[nextBranchIndex]) {
						final String[] fields = line.split(":");
						final String className = fields[0].trim();
						final String methodDescriptor = fields[1].trim();
						final String methodName = fields[2].trim();
						final int bytecodeFrom = Integer.parseInt(fields[3].trim());
						final PathConditionTraversedBranch b;
						if (bytecodeFrom == 0) {
							b = PathConditionTraversedBranch.makeMethodEntryPoint(className, methodDescriptor, methodName);
						} else {
							final int bytecodeTo = Integer.parseInt(fields[4].trim());
							b = PathConditionTraversedBranch.makeRealBranch(className, methodDescriptor, methodName, bytecodeFrom, bytecodeTo);
						}
						if (b.className.equals("com/google/javascript/jscomp/RemoveUnusedVars") && 
								b.methodName.startsWith("traverseNode")) {
							toDebug2.add(b);
						}

						if (searchRelevantBranchesAllFormats.keySet().contains(b)) {
							retVal.add(searchRelevantBranchesAllFormats.get(b));
						}
						++nextBranchIndex;
						if (nextBranchIndex == coverageTrace.length) {
							for (PathConditionTraversedBranch br : toDebug2) {
								if (!toDebug1.contains(br)) {
									System.out.println("CHECK");
								}
							}
							return retVal;
						}
						//I'm not quite sure that a row in the branches.txt file always contains consecutive
						//branch numbers, so we handle the situation in the case it doesn't.
						if (coverageTrace[nextBranchIndex - 1] >= coverageTrace[nextBranchIndex]) {
							continue reloadFile;
						}
					}
					++lineCounter;
				}
				//if we hit this line it means that we scanned the file
				//from the start past the next branch without finding it
				throw new RuntimeException("branch " + nextBranchIndex + " does not exist in " + branchesFileName);
			}
		}
	}
	
	
	public TestFitnessFunction getPathConditionCoverageTestFitness() {
		List<PathConditionCoverageGoalFitness> goals = getCoverageGoals();
		return new PathConditionCoverageTestFitness(goals);
	}

	public TestSuiteFitnessFunction getPathConditionCoverageSuiteFitness() {
		List<PathConditionCoverageGoalFitness> goals = getCoverageGoals();
		return new PathConditionCoverageSuiteFitness(goals);
	}


	/**
	 * Create a fitness function for path condition coverage aimed at executing the
	 * path condition the identified path condition.
	 * 
	 * @return a {@link org.evosuite.coverage.pathcondition.PathConditionCoverageGoalFitness}
	 *         object.
	 */
	private PathConditionCoverageGoalFitness createPathConditionCoverageTestFitness(int pathConditionId, String className, String methodName, String evaluatorClassName) {

		return new PathConditionCoverageGoalFitness(new PathConditionCoverageGoal(pathConditionId, className, methodName, evaluatorClassName));
	}
	
}
