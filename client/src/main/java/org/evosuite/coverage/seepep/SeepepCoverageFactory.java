package org.evosuite.coverage.seepep;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.evosuite.Properties;
import org.evosuite.utils.LoggingUtils;
import org.evosuite.testcase.TestFitnessFunction;
import org.evosuite.testcase.execution.EvosuiteError;
import org.evosuite.testsuite.AbstractFitnessFactory;
import org.evosuite.testsuite.TestSuiteFitnessFunction;

/**
 * <p>
 * SeepepCoverageFactory class.
 * </p>
 * 
 * @author G. Denaro
 */
public class SeepepCoverageFactory extends AbstractFitnessFactory<SeepepCoverageGoalFitness> { /*SEEPEP: DAG coverage*/
	//private static final Logger logger = LoggerFactory.getLogger(SeepepCoverageFactory.class);
	
	private final List<SeepepCoverageGoalFitness> coverageGoals  = new ArrayList<>();
	private final Object seepepGuardEvaluator;
	private final Map<Integer, String[]> seepepDagsOfPathCondition = new HashMap<>();
	
	private static SeepepCoverageFactory _I = null; /* the singleton instance */

	public static SeepepCoverageFactory _I() {
		if (_I == null) {
			_I = new SeepepCoverageFactory();
		}
		return _I;
	}
	
	public Map<?, ?> executeGuardEvaluator(Map<String, Object> knownValues) {
		try {
			Method providerMethod = seepepGuardEvaluator.getClass().getDeclaredMethod("evaluateGuardsAndGetLabels", new Class<?>[]{Map.class});
			providerMethod.setAccessible(true);
			return (Map<?, ?>) providerMethod.invoke(seepepGuardEvaluator, knownValues);
		} catch (NoSuchMethodException | SecurityException | 
				IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			throw new EvosuiteError("Cannot execute SEEPEP guard evaluator, problem with method evaluateGuards in SEEPEP_GOAL_PROVIDER class: " 
					+ Properties.SEEPEP_GOAL_PROVIDER + ": " + e + 
					"\n -- Exception on values:" + knownValues +
					"\n -- Causing exception is: " + (e.getCause() == null ? null : Arrays.toString(e.getCause().getStackTrace())));
		} 
	}
	
	public String[] getDags(int pathCondtionId) {
		return seepepDagsOfPathCondition.get(pathCondtionId);
	}
	
	private SeepepCoverageFactory() {
		super();
		long start = System.currentTimeMillis();
		int id = 0;
		
		try {
			Class<?> seepepGuardEvaluatorClass = Class.forName(Properties.SEEPEP_GOAL_PROVIDER);
			seepepGuardEvaluator = seepepGuardEvaluatorClass.getMethod("_I", null).invoke(null, null);
			Method dagLabelProviderMethod = seepepGuardEvaluatorClass.getMethod("getDagLabelOfAllPathConditions", (Class<?>[]) null);
			Map<?, ?> dagSequences = (Map<?, ?>) dagLabelProviderMethod.invoke(seepepGuardEvaluator, (Object[]) null);
			int dagCountAllPCs = 0;
			int numOutBoundPCs = 0;
			HashMap<String, Map<Integer, List<Integer>>> pathConditionsOfDag = 
					new HashMap<>();		
			for (Object key: dagSequences.keySet()) {
				Integer pathConditionId = (Integer) key; 
				String dagSeq = (String) dagSequences.get(pathConditionId);
				String[] dagSeqSplit = dagSeq.split("::");
				dagCountAllPCs += dagSeqSplit.length;					
				boolean isOutOfScopePath = dagSeqSplit[dagSeqSplit.length - 1].startsWith("OUT_OF_BOUNDS");
				if (isOutOfScopePath) {
					++numOutBoundPCs;	
					dagSeqSplit = Arrays.copyOf(dagSeqSplit, dagSeqSplit.length - 2);
				} 
				seepepDagsOfPathCondition.put(pathConditionId, dagSeqSplit);

				for (int hitPosition = 0; hitPosition < dagSeqSplit.length; ++hitPosition) {
					String dag = dagSeqSplit[hitPosition];
					if (!pathConditionsOfDag.containsKey(dag)) {
						pathConditionsOfDag.put(dag, new HashMap<>());
					}
					if (!isOutOfScopePath || Properties.SEEPEP_TEST_OUTOFBOUND_PATHS) {
						if (!pathConditionsOfDag.get(dag).containsKey(pathConditionId)) {
							pathConditionsOfDag.get(dag).put(pathConditionId, new ArrayList<>());
						}
						pathConditionsOfDag.get(dag).get(pathConditionId).add(hitPosition);
					}
				}
			}
			LoggingUtils.getEvoLogger().info("* Seepep-goal-provider: {} unique SEEPEP-DAGS, out of {} in {} path conditions ({} related to out-of-bounds paths)", pathConditionsOfDag.size(), dagCountAllPCs, dagSequences.size(), numOutBoundPCs);
			LoggingUtils.getEvoLogger().info("* Seepep-goal-provider: test cases of out-of-bounds path conditions {} be considered,", (Properties.SEEPEP_TEST_OUTOFBOUND_PATHS ? "WILL " : "WON'T"));
			for (String dag: pathConditionsOfDag.keySet()) {
				id++;
				Map<Integer, List<Integer>> coveringPCs = pathConditionsOfDag.get(dag);
				if (coveringPCs.isEmpty()) {
					LoggingUtils.getEvoLogger().info("* WARNING: Seepep-goal-provider: dag (id={}): {} " + 
							"is not covered in any IN-BOUNDS path, you may want to consider also test cases that " +
							"correspond to OUT-OF-BOUNDS paths to be able to cover all dags", id, dag);
					continue;
				}
				//LoggingUtils.getEvoLogger().info("* Seepep-goal-provider: path conditions {} refer to DAG {}", Arrays.toString(pathConditionsOfDag.get(dag).keySet().toArray()), dag);
				SeepepCoverageGoalFitness goal = createSeepepCoverageTestFitness(id, dag, coveringPCs);
				coverageGoals.add(goal);
			}
		} catch (ClassNotFoundException e) {
			throw new EvosuiteError("Cannot load SEEPEP goals, since cannot load SEEPEP_GOAL_PROVIDER class: " + Properties.SEEPEP_GOAL_PROVIDER + ": " + e);
		} catch (SecurityException | NoSuchMethodException e) {
			throw new EvosuiteError("Cannot load SEEPEP goals, missing method getDagLabelOfAllPathConditions in SEEPEP_GOAL_PROVIDER class: " + Properties.SEEPEP_GOAL_PROVIDER + ": " + e);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | ClassCastException e) {
			throw new EvosuiteError("Cannot load SEEPEP goals, cannot call method getDagLabelOfAllPathConditions for new instance SEEPEP_GOAL_PROVIDER class, or unexpected return value: " + Properties.SEEPEP_GOAL_PROVIDER + ": " + e);			
		}

		goalComputationTime = System.currentTimeMillis() - start;	
	}

	public TestSuiteFitnessFunction getSeepepCoverageSuiteFitness() {
		List<SeepepCoverageGoalFitness> goals = getCoverageGoals();
		return new SeepepCoverageSuiteFitness(goals);
	}

	public TestFitnessFunction getSeepepCoverageTestFitness() {
		List<SeepepCoverageGoalFitness> goals = getCoverageGoals();
		return new SeepepCoverageTestFitness(goals);
	}

	@Override
	public List<SeepepCoverageGoalFitness> getCoverageGoals() {
		return coverageGoals;
	}
	
	/**
	 * Create a fitness function for seepep-dag coverage aimed at executing the
	 * given sequence of spark methods
	 * 
	 * @return a {@link org.evosuite.coverage.seepep.}
	 *         object.
	 */
	private SeepepCoverageGoalFitness createSeepepCoverageTestFitness(int seepepDagId, String dag, Map<Integer, List<Integer>> coveringPCs) {
		return new SeepepCoverageGoalFitness(new SeepepCoverageGoal(seepepDagId, dag, coveringPCs));
	}
}


//======
//======
/*private List<SeepepCoverageGoalFitness> coverageGoals  = new ArrayList<>();
private Set<String> relevantSrcLines  = new HashSet<>();
private static final String sparkMethodLabelSeparator = "_&_";

private static SeepepCoverageFactory _I = null; /* the singleton instance * /

public static SeepepCoverageFactory _I() {
	if (_I == null) {
		_I = new SeepepCoverageFactory();
	}
	return _I;
}

private List<String> extractSrcLineSequence(String[] sparkMethods) {
	List<String> ret = new ArrayList<>();
	for (String m: sparkMethods) {
		String[] data = m.split(sparkMethodLabelSeparator);
		if (data.length < 2) {
			throw new EvosuiteError("Malformed SEEPEP goal (required separator is " + sparkMethodLabelSeparator + ") with less than 2 data: " + m);
		}
		String classname = data[data.length - 2];
		try {
			int srcLine = Integer.parseInt(data[data.length - 1]);
			relevantSrcLines.add(classname + ":" + srcLine);
			ret.add(classname + ":" + srcLine);
		} catch (NumberFormatException e) {
			throw new EvosuiteError("Malformed SEEPEP goal (required separator is " + sparkMethodLabelSeparator + ") with non numeric src line: " + m);
		}
	}
	return ret;
}

private SeepepCoverageFactory() {
	super();
	long start = System.currentTimeMillis();
	int id = 0;
	
	try {
		Class<?> dagProviderClass = Class.forName(Properties.SEEPEP_GOAL_PROVIDER);
		Method providerMethod = dagProviderClass.getMethod("getDagLabelOfAllPathConditions", (Class<?>[]) null);
		Map<?, ?> dagsByPC = (Map<?, ?>) providerMethod.invoke(dagProviderClass.newInstance(), (Object[]) null);
		int dagCountAllPCs = 0;
		Set<String> uniqueDags = new HashSet<>();		
		for (Object obj: dagsByPC.values()) {
			String dagSeqInPC = (String) obj; 
			String[] dagSeqSplit = dagSeqInPC.split("::");
			dagCountAllPCs += dagSeqSplit.length;
			uniqueDags.addAll(Arrays.asList(dagSeqSplit));
		}
		LoggingUtils.getEvoLogger().info("* Seepep-goal-provider: {} unique SEEPEP-DAGS, out of {} in {} path conditions", uniqueDags.size(), dagCountAllPCs, dagsByPC.size());
		List<String[]> allSrcLineSequences = new ArrayList<>();
		for (String seepepDag : uniqueDags) {
			String sparkMethods[] = seepepDag.split(":");
			List<String> srcLineSequence = extractSrcLineSequence(sparkMethods);
			allSrcLineSequences.add(srcLineSequence.toArray(new String[0]));
		}
		for (String[] srcLineSequence: allSrcLineSequences) {
			id++;
			SeepepCoverageGoalFitness goal = createSeepepCoverageTestFitness(id, srcLineSequence, allSrcLineSequences);
			coverageGoals.add(goal);
		}
	} catch (ClassNotFoundException e) {
		throw new EvosuiteError("Cannot load SEEPEP goals, since cannot load SEEPEP_GOAL_PROVIDER class: " + Properties.SEEPEP_GOAL_PROVIDER);
	} catch (SecurityException | NoSuchMethodException e) {
		throw new EvosuiteError("Cannot load SEEPEP goals, missing method getDagLabelOfAllPathConditions in SEEPEP_GOAL_PROVIDER class: " + Properties.SEEPEP_GOAL_PROVIDER);
	} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | InstantiationException | ClassCastException e) {
		throw new EvosuiteError("Cannot load SEEPEP goals, cannot call method getDagLabelOfAllPathConditions for new instance SEEPEP_GOAL_PROVIDER class, or unexpected return value: " + Properties.SEEPEP_GOAL_PROVIDER);			
	}

	goalComputationTime = System.currentTimeMillis() - start;	
}

public TestSuiteFitnessFunction getSeepepCoverageSuiteFitness() {
	List<SeepepCoverageGoalFitness> goals = getCoverageGoals();
	return new SeepepCoverageSuiteFitness(goals);
}

public TestFitnessFunction getSeepepCoverageTestFitness() {
	List<SeepepCoverageGoalFitness> goals = getCoverageGoals();
	return new SeepepCoverageTestFitness(goals);
}

@Override
public List<SeepepCoverageGoalFitness> getCoverageGoals() {
	return coverageGoals;
}

public  boolean isRelevantSrcLine(String classname, int line) {
	return relevantSrcLines.contains(classname + ":" + line);
}


/**
 * Create a fitness function for seepep-dag coverage aimed at executing the
 * given sequence of spark methods
 * 
 * @return a {@link org.evosuite.coverage.seepep.}
 *         object.
 * /
private SeepepCoverageGoalFitness createSeepepCoverageTestFitness(int seepepDagId, String[] srcLineSequence, List<String[]> allSrcLineSequences) {
	return new SeepepCoverageGoalFitness(new SeepepCoverageGoal(seepepDagId, srcLineSequence, allSrcLineSequences));
}

public boolean isRelevantAction(String classname, String methodname) {
	boolean relevant = methodname.contains("count") || methodname.contains("reduce") || methodname.contains("saveAsTextFile"); 
	if (relevant)
		LoggingUtils.getEvoLogger().info("* relevant?: {} = {}", methodname, relevant);
	return relevant;
}
*/
