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
package org.evosuite.coverage.seepep;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.evosuite.coverage.TestCoverageGoal;
import org.evosuite.testcase.execution.EvosuiteError;
import org.evosuite.testcase.execution.ExecutionResult;
import org.evosuite.utils.LoggingUtils;

/**
 * A single path condition coverage goal
 * 
 * @author G. Denaro
 */
public class SeepepCoverageGoal implements Serializable {   /*SEEPEP: DAG coverage*/
	
	private static final long serialVersionUID = -6546075846720333956L;

	private final int seepepDagId;
	private final String targetDag;
	private final String[] targetDagSplit;
	private final List<Integer> coveringPCs_sorted;
	private final Map<Integer, List<Integer>> coveringPcHitPositions_sorted;

	/**
	 * @param className
	 *            a {@link java.lang.String} object.
	 * @param methodName
	 *            a {@link java.lang.String} object.
	 */
	public SeepepCoverageGoal(int seepepDagId, String dag, Map<Integer, List<Integer>> coveringPCsWithHitPositions) {
		if (dag == null || coveringPCsWithHitPositions == null)
			throw new IllegalArgumentException("null given");
		this.seepepDagId = seepepDagId;
		this.targetDag = dag;
		this.targetDagSplit = dag.split(":");
		this.coveringPCs_sorted = new ArrayList<>(coveringPCsWithHitPositions.keySet());
		Collections.sort(this.coveringPCs_sorted);
		this.coveringPcHitPositions_sorted = coveringPCsWithHitPositions;
		for (List<Integer> hitPositions: this.coveringPcHitPositions_sorted.values()) {
			Collections.sort(hitPositions);
		}
	}


	/**
	 * @return the seepep-dag id
	 */
	public int getSeepepDagId() {
		return seepepDagId;
	}

	/**
	 * @return the label computed with seepep for this dag
	 */
	public String getTargetDag() {
		return targetDag;
	}

	private int countEmptyDatasetsInTracePrefix(List<SeepepTraceItem> trace, int pathCondition) {
		int countEmpty = Integer.MAX_VALUE;
		List<Integer> hitPositions_sorted = this.coveringPcHitPositions_sorted.get(pathCondition);
		int alreadySearchedIndexOfTrace = 0;
		for (int targetDagHitPosition: hitPositions_sorted) {
			int countEmptyCurrent = 0;
			int numItemsConsidered = 0;
			int actionPrefixSize = targetDagHitPosition;
			for (SeepepTraceItem item: trace) {
				if (actionPrefixSize < 0) {
					break;
				}
				++numItemsConsidered;
				if (item.isAction() && !item.getIdentifier().startsWith("saveAs")) { //TODO
					--actionPrefixSize;
				} 
			}
			if (numItemsConsidered == 0) {
				LoggingUtils.getEvoLogger().info("*** PROBLEM some transformation  in target dag {}\n    is not in hitting trace {}    \n up to position {}", 
						targetDag, Arrays.toString(trace.toArray()), targetDagHitPosition);
				return -1; //TODO: throw exception 
			} 
			int nextExpectedPositionInTarget = targetDagSplit.length - 2; // we look only to transformations, thus skip the last "action" item 
			for (int i = numItemsConsidered - 1; i >= alreadySearchedIndexOfTrace && nextExpectedPositionInTarget >= 0; --i) {
				SeepepTraceItem itemInTrace = trace.get(i);
				if (itemInTrace.isTransformation() && itemInTrace.getIdentifier().equals(targetDagSplit[nextExpectedPositionInTarget])) {
					--nextExpectedPositionInTarget;
					if ((int) itemInTrace.getValue() == 0) {
						++countEmptyCurrent;
					}
				}
			}
			if (nextExpectedPositionInTarget >= 0) {
				LoggingUtils.getEvoLogger().info("*** PROBLEM some transformation  in target dag {}\n    is not in hitting trace {}    \n up to position {}", 
						targetDag, Arrays.toString(trace.toArray()), targetDagHitPosition);
			}
			if (countEmptyCurrent < countEmpty) {
				countEmpty = countEmptyCurrent;
			}
			alreadySearchedIndexOfTrace = numItemsConsidered - 1;
		}
		return countEmpty; 

	}

	private static Set<Integer> firstSeen = new HashSet<>(); 
	private static Set<Integer> seenGoal = new HashSet<>(); 
	private static Set<Integer> seenGoal_someEmpty = new HashSet<>(); 
	private int getPathCondition(Map<String, Object> knownValues, List<SeepepTraceItem> trace) {
		Map<?, ?> validPC = SeepepCoverageFactory._I().executeGuardEvaluator(knownValues);
		if (validPC.isEmpty()) {
			LoggingUtils.getEvoLogger().info("Trace with 0 path conds: {}", validPC.keySet().toString());
			LoggingUtils.getEvoLogger().info("Trace is {}", trace.toString());				
			//throw new EvosuiteError("Trace with 0 path conds: " + validPC.toString() + " -- " + knownValues.toString());		
		} else if (validPC.size() == 1) {
			Integer pcId = (Integer) validPC.keySet().iterator().next();
			if (!firstSeen.contains(pcId)) {
				LoggingUtils.getEvoLogger().info("Seen PC: {}", pcId);
				firstSeen.add((Integer) pcId);
			} 
			return pcId;
		} else {
			LoggingUtils.getEvoLogger().info("Trace with multiple path conds: {}", validPC.keySet().toString());				
			LoggingUtils.getEvoLogger().info("Trace: {}", trace.toString());	
			//throw new EvosuiteError("Trace with multiple path conds: " + validPC.toString() + " -- " + knownValues.toString());		
		}
		return -1; //TODO: throw exeception 
	}
	
	private static Map<String, String> renamings = new HashMap<>();
	private static Map<Map<String, Object>, Integer> seenPCs = new HashMap<>();
	private Map<Integer, List<SeepepTraceItem>> getExecutedPathConditions(ExecutionResult result) {
		Map<Integer, List<SeepepTraceItem>> ret = new HashMap<>();
		List<SeepepTraceItem> traversedActionRetValueSeq = result.getTrace().getTraversedSeepepItems();
		Map<String, Object> knownValues = new HashMap<>();
		List<SeepepTraceItem> currentTrace = new ArrayList<>();
		boolean firstStartMarkerFound = false;
		for (SeepepTraceItem entry: traversedActionRetValueSeq) {
			if (entry.isTraceStartMarker()) {
				if (!knownValues.isEmpty()) {
					Integer pathConditionId = getPathCondition(knownValues, currentTrace);
					if (pathConditionId >= 0)
						ret.put(pathConditionId, new ArrayList<>(currentTrace));
				}
				knownValues.clear();
				currentTrace.clear();
				/*if (firstStartMarkerFound) {
					break; //check only the first trace for best performance
				}*/
				firstStartMarkerFound = true;
			} else if (firstStartMarkerFound) {
				currentTrace.add(entry);
				if (entry.isInputParameter() || entry.isAction()) {
					String identifier = entry.getIdentifier();
					if (entry.isAction()) {
						if (renamings.containsKey(identifier)) {
							identifier = renamings.get(identifier);
						} else {
							String originalIdentifier = identifier;
							String[] parts = identifier.split("_&_");
							identifier = parts[0] + "_" + parts[1].substring(parts[1].lastIndexOf('.') + 1) + "_java_" + parts[2];
							//LoggingUtils.getEvoLogger().info("Action renamed for guard evaluator: {} -> {}", entry, identifier);				
							renamings.put(originalIdentifier, identifier);
						}
					}
					knownValues.put(identifier, entry.getValue());
				} 
			} //else: did not yet find the start of first trace
		}
		if (!knownValues.isEmpty()) {
			Integer pathConditionId = getPathCondition(knownValues, currentTrace);
			ret.put(pathConditionId, new ArrayList<>(currentTrace));
		}

		return ret;
	}
	
	/**
	 * <p>
	 * getDistance
	 * </p>
	 * 
	 * @param result
	 *            a {@link org.evosuite.testcase.ExecutionResult} object.
	 * @return a {@link org.evosuite.coverage.ControlFlowDistance} object.
	 */
	public double getDistance(ExecutionResult result) {
		if (result == null)
			throw new IllegalArgumentException("null given");
		
		// handle timeout in ExecutionResult
		if (TestCoverageGoal.hasTimeout(result))
			return Double.MAX_VALUE;

		double distance = 2.5; // does not hit either target dag or target dag's action: maximum distance 2.5
		Map<Integer, List<SeepepTraceItem>> executedPathConds = getExecutedPathConditions(result);
		for (Integer pathCond: executedPathConds.keySet()) {
			List<SeepepTraceItem> trace = executedPathConds.get(pathCond);
			double distanceForPathCond = Double.MAX_VALUE;
			if (coveringPCs_sorted.contains(pathCond)) { 
				/* hit covering path condition: distance in range [0 <= d < 1] 
				 * depending on whether or not there are empty datasets
				 */
				String[] dagsCurrent = SeepepCoverageFactory._I().getDags(pathCond);
				int countEmpty = countEmptyDatasetsInTracePrefix(trace, pathCond);
				//LoggingUtils.getEvoLogger().info("1) Hitting PC {} for dag {} -> with {} empty datasets", pathCond, seepepDagId, countEmpty); //we got it!	
				if (countEmpty == 0) { 
					if (!seenGoal.contains(seepepDagId)) {
						seenGoal.add(seepepDagId);
						LoggingUtils.getEvoLogger().info("***Hit goal id:{} in path cond {}", seepepDagId, pathCond); //we got it!			
					}
				}  else if (!seenGoal_someEmpty.contains(seepepDagId)) {
					seenGoal_someEmpty.add(seepepDagId);					
					LoggingUtils.getEvoLogger().info("- Hit goal id:{} in path cond {} but {} empty datasets", seepepDagId, pathCond, countEmpty); //we got it!			
				}
				distanceForPathCond = ((double) countEmpty) / (1 + (double) countEmpty); 
				
			} else { // did not covering path condition, lets' check if trace hits at least the target dag's action
				boolean hitTargetAction = false;
				for (SeepepTraceItem item: trace) {
					if (item.isAction() && targetDag.endsWith(item.getIdentifier().substring(0, item.getIdentifier().lastIndexOf('_')))) {
						hitTargetAction = true;
						break;
					}
				}
				if (hitTargetAction) { 
					/* hit target dag's action: distance in range [1 <= d < 2] 
					 * depending on the distance from the range of covering path conditions
					 */
					int distanceForPathCondNonNormalized;
					if (pathCond < coveringPCs_sorted.get(0)) { // extern wrt the range
						distanceForPathCondNonNormalized = 3 * (coveringPCs_sorted.get(0) - pathCond);
						//LoggingUtils.getEvoLogger().info("2) Action-hitting PC {} for dag {} -> with (low-extern ) distance={}", pathCond, seepepDagId, distanceForPathCondNonNormalized); //we got it!	
					} else if (pathCond > coveringPCs_sorted.get(coveringPCs_sorted.size() - 1)) { //extern  wrt the range
						distanceForPathCondNonNormalized = 3 * (pathCond - coveringPCs_sorted.get(coveringPCs_sorted.size() - 1));				
						//LoggingUtils.getEvoLogger().info("2) Action-hitting PC {} for dag {} -> with (high-extern) distance={}", pathCond, seepepDagId, distanceForPathCondNonNormalized); //we got it!	
					} else { //in the range
						int lower = 0;
						int upper = coveringPCs_sorted.size() - 1;
						while (lower < upper - 1) {
							int mid = (lower + upper) / 2;
							if (coveringPCs_sorted.get(mid) < pathCond) {
								lower = mid;
							} else {
								upper = mid;
							}
						}
						int distanceFromLower = pathCond - coveringPCs_sorted.get(lower);
						int distanceFromUpper = coveringPCs_sorted.get(upper) - pathCond;
						if (distanceFromLower < distanceFromUpper) {
							distanceForPathCondNonNormalized = distanceFromLower;
						} else {
							distanceForPathCondNonNormalized = distanceFromUpper;
						}
						//LoggingUtils.getEvoLogger().info("2) Action-hitting PC {} for dag {} -> with (internal) distance={}", pathCond, seepepDagId, distanceForPathCondNonNormalized); //we got it!	
					}
					distanceForPathCond = 1 + (((double) distanceForPathCondNonNormalized) / (1 + (double) distanceForPathCondNonNormalized));	
				} 
				//else: this trace does not hit either target dag or target dag's action, then distance does not change
			}
			
			if (distanceForPathCond < distance) {
				distance = distanceForPathCond;
			}			
		}
		
		return distance;
	}

	// inherited from Object

	/**
	 * {@inheritDoc}
	 * 
	 * Readable representation
	 */
	@Override
	public String toString() {
		String s = "Goal:" + seepepDagId + ", DAG=: " + targetDag + ", pathConds=" + Arrays.toString(coveringPCs_sorted.toArray());
		return s;
	}

	/** {@inheritDoc} */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + seepepDagId;
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
		SeepepCoverageGoal other = (SeepepCoverageGoal) obj;
		return this.seepepDagId == other.seepepDagId;
	}

}


//======
//======
/*private static final long serialVersionUID = -6546075846720333956L;

private final int seepepDagId;
private final String[] targetSrcLineSequence;
private final Set<String> relevantLines;

/**
 * @param className
 *            a {@link java.lang.String} object.
 * @param methodName
 *            a {@link java.lang.String} object.
 * /
public SeepepCoverageGoal(int seepepDagId, String[] targetSrcLineSequence, List<String[]> allOtherSequences) {
	if (targetSrcLineSequence == null || targetSrcLineSequence.length == 0)
		throw new IllegalArgumentException("null given");
	this.seepepDagId = seepepDagId;
	this.targetSrcLineSequence = targetSrcLineSequence;
	relevantLines = new HashSet<>();
	for (String[] seq: allOtherSequences) {
		if (seq.length > 0 && targetSrcLineSequence.length > 0) {
			String lastInSeq = seq[seq.length - 1];
			String lastInTarget = targetSrcLineSequence[targetSrcLineSequence.length - 1];
			if (lastInSeq != null && lastInSeq.equals(lastInTarget)) {
				relevantLines.addAll(Arrays.asList(seq));
			}
		}
	}
}


/**
 * @return the seepep-dag id
 * /
public int getSeepepDagId() {
	return seepepDagId;
}

/**
 * @return the spark methods of this seepep dag
 * /
public String[] getTargetSrcLineSequence() {
	return targetSrcLineSequence;
}


/**
 * <p>
 * getDistance
 * </p>
 * 
 * @param result
 *            a {@link org.evosuite.testcase.ExecutionResult} object.
 * @return a {@link org.evosuite.coverage.ControlFlowDistance} object.
 * /
public double getDistance(ExecutionResult result) {
	if (result == null || targetSrcLineSequence == null)
		throw new IllegalArgumentException("null given");
	
	// handle timeout in ExecutionResult
	if (TestCoverageGoal.hasTimeout(result))
		return Double.MAX_VALUE;
	
	boolean log1 = false;
	boolean log2 = false;
	if (targetSrcLineSequence[targetSrcLineSequence.length - 1].endsWith("302")) {
		log1 = true;
	}

	Set<String> targetSrcLineSequenceAsSet = new HashSet<>(Arrays.asList(targetSrcLineSequence));
	int traversed = 0;
	int bestTraversed = 0;
	List<String> traversedLineSequence = result.getTrace().getTraversedLineSequence();
	for (int lineNum = traversedLineSequence.size() - 1; lineNum >= 0; lineNum--) { //inspect the sequence of traversed lines backwardly
		String srcLine = traversedLineSequence.get(lineNum);
		if (!relevantLines.contains(srcLine)) {
			if (log1 && log2) {
				LoggingUtils.getEvoLogger().info("--traversing line: {} -- irrelevant for {}", srcLine, Arrays.toString(targetSrcLineSequence));
			}
			continue;
		} 
		if (!targetSrcLineSequenceAsSet.contains(srcLine)) { //reset
			if (log1 && log2) {
				LoggingUtils.getEvoLogger().info("--traversing line: {} -- relevant but contained in {}", srcLine, Arrays.toString(targetSrcLineSequence));
			}
			if (traversed > bestTraversed) {
				bestTraversed = traversed; 
			}
			traversed = 0;
		} else {
			log2 = true;
			if (traversed == targetSrcLineSequence.length) { 
				if (srcLine.equals(targetSrcLineSequence[targetSrcLineSequence.length - 1])) {
					LoggingUtils.getEvoLogger().info("**Hit goal id={}, {}, {}", seepepDagId, Arrays.toString(this.targetSrcLineSequence));
					return 0d; //we got it!		
				} else {
					if (log1 && log2) {
						LoggingUtils.getEvoLogger().info("--traversing line: {} -- is last in {} -- reset", srcLine, Arrays.toString(targetSrcLineSequence));
					}
					traversed = 0; //no update of best, since this sequence is longer than target				
				}
			} else if (srcLine.equals(targetSrcLineSequence[targetSrcLineSequence.length - 1 - traversed])) { //found, we progress
				if (log1 && log2) {
					LoggingUtils.getEvoLogger().info("--traversing line: {} -- is next in {}", srcLine, Arrays.toString(targetSrcLineSequence));
				}
				++traversed;
			} else { //mismatched line: reset
				if (log1 && log2) {
					LoggingUtils.getEvoLogger().info("--traversing line: {} -- is mismatched in {} -- reset", srcLine, Arrays.toString(targetSrcLineSequence));
				}
				if (traversed > bestTraversed) {
					bestTraversed = traversed; 
				}				
				traversed = 0;
			}
		}
	}
	if (traversed == targetSrcLineSequence.length) { 
		LoggingUtils.getEvoLogger().info("**Hit goal id={}, {}, {}", seepepDagId, Arrays.toString(this.targetSrcLineSequence));
		return 0d; // we got it!
	} else {
		return targetSrcLineSequence.length - bestTraversed;
	}
		
}

// inherited from Object

/**
 * {@inheritDoc}
 * 
 * Readable representation
 * /
@Override
public String toString() {
	String name = "Seepep-dag: " + Arrays.toString(targetSrcLineSequence) + " (id = " + seepepDagId + ")";
	return name;
}

/** {@inheritDoc} * /
@Override
public int hashCode() {
	final int prime = 31;
	int result = 1;
	result = prime * result + seepepDagId;
	return result;
}

/** {@inheritDoc}  * /
@Override
public boolean equals(Object obj) {
	if (this == obj)
		return true;
	if (obj == null)
		return false;
	if (getClass() != obj.getClass())
		return false;
	SeepepCoverageGoal other = (SeepepCoverageGoal) obj;
	return this.seepepDagId == other.seepepDagId;
}
*/