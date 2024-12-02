package org.evosuite.ga.metaheuristics.mosa.jbse;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import jbse.bc.Classpath;
//import sushi.configure.SignatureHandler;

public final class JBSEOptions implements Cloneable {
	private boolean help = false;

/*	@Option(name = "-initial_test",
			usage = "Java signature of the initial test case method for seeding concolic exploration",
			handler = SignatureHandler.class)
	private List<String> initialTestCaseSignature;*/
	
	private Path initialTestCasePath = Paths.get(".", "out");
	
	private String targetClassName;
	
/*	@Option(name = "-target_method",
			usage = "Java signature of the target method (the method to test)",
			handler = SignatureHandler.class)
	private List<String> targetMethodSignature;*/
	
	private int maxDepth;
	
	private int numOfThreads;
	
	private List<Path> classesPath = Collections.singletonList(Paths.get(".", "bin"));
	
	private Path tmpDirBase = Paths.get(".", "tmp");
	
	private String tmpDirName = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss").format(new Date());
	
	private Path outDir = Paths.get(".", "out");
	
	private Path z3Path;
	
	private Path jbsePath = Paths.get(".", "lib", "jbse.jar");

	private Path evosuitePath = Paths.get(".", "lib", "evosuite.jar");
	
	private Path sushiPath = Paths.get(".", "lib", "sushi-lib.jar");
	
	private int evosuiteTimeBudgetDuration = 180;
	
	private TimeUnit evosuiteTimeBudgetUnit = TimeUnit.SECONDS;
	
	private long globalTimeBudgetDuration = 10;
	
	private TimeUnit globalTimeBudgetUnit = TimeUnit.MINUTES;
	
	private long timeoutMOSATaskCreationDuration = 5;
	
	private TimeUnit timeoutMOSATaskCreationUnit = TimeUnit.SECONDS;

	private int numMOSATargets = 1;
	
	private boolean useMOSA = false;
	
	private Map<String, Integer> heapScope;
	
	private int countScope = 0;

/*	@Option(name = "-uninterpreted",
			usage = "List of signatures of uninterpreted methods",
			handler = MultiSignatureOptionHandler.class)
	private List<List<String>> uninterpreted = new ArrayList<>();*/
	
	public boolean getHelp() {
		return this.help;
	}
	
	public void setHelp(boolean help) {
		this.help = help;
	}
		
/*	public List<String> getInitialTestCase() {
		return (this.initialTestCaseSignature == null ? null : Collections.unmodifiableList(this.initialTestCaseSignature));
	}
	
	public void setInitialTestCase(String... signature) {
		if (signature.length != 3) {
			return;
		}
		this.initialTestCaseSignature = Arrays.asList(signature.clone());
	}
	
	public void setInitialTestCaseNone() {
		this.initialTestCaseSignature = null;
	}*/
	
	public Path getInitialTestCasePath() {
		return this.initialTestCasePath;
	}
	
	public void setInitialTestCasePath(Path initialTestCasePath) {
		this.initialTestCasePath = initialTestCasePath;
	}
	
	public String getTargetClass() {
		return this.targetClassName;
	}
	
/*	public void setTargetClass(String targetClassName) {
		this.targetClassName = targetClassName;
		this.targetMethodSignature = null;
	}
	
	public List<String> getTargetMethod() {
		return (this.targetMethodSignature == null ? null : Collections.unmodifiableList(this.targetMethodSignature));
	}
	
	public void setTargetMethod(String... signature) {
		if (signature.length != 3) {
			return;
		}
		this.targetMethodSignature = Arrays.asList(signature.clone());
		this.targetClassName = null;
	}*/
	
	public int getMaxDepth() {
		return this.maxDepth;
	}
	
	public void setMaxDepth(int maxdepth) {
		this.maxDepth = maxdepth;
	}
	
	public int getNumOfThreads() {
		return this.numOfThreads;
	}
	
	public void setNumOfThreads(int numOfThreads) {
		this.numOfThreads = numOfThreads;
	}
	
	public List<Path> getClassesPath() {
		return this.classesPath;
	}
	
	public void setClassesPath(Path... paths) {
		this.classesPath = Arrays.asList(paths);
	}
	
	public Path getTmpDirectoryBase() {
		return this.tmpDirBase;
	}
	
	public void setTmpDirectoryBase(Path base) {
		this.tmpDirBase = base;
	}
	
	public String getTmpDirectoryName() {
		return this.tmpDirName;
	}
	
	public void setTmpDirectoryName(String name) {
		this.tmpDirName = name;
	}
	
	public Path getTmpDirectoryPath() {
		if (this.tmpDirName == null) {
			return this.tmpDirBase;
		} else {
			return this.tmpDirBase.resolve(this.tmpDirName);
		}
	}
	
	public Path getTmpBinTestsDirectoryPath() {
		return getTmpDirectoryPath().resolve("bin");
	}
	
	public Path getOutDirectory() {
		return this.outDir;
	}
	
	public void setOutDirectory(Path dir) {
		this.outDir = dir;
	}
	
	public Path getZ3Path() {
		return this.z3Path;
	}
	
	public void setZ3Path(Path z3Path) {
		this.z3Path = z3Path;
	}
	
	public Path getJBSELibraryPath() {
		return this.jbsePath;
	}

	public void setJBSELibraryPath(Path jbsePath) {
		this.jbsePath = jbsePath;
	}
	
	public Path getEvosuitePath() {
		return this.evosuitePath;
	}
	
	public void setEvosuitePath(Path evosuitePath) {
		this.evosuitePath = evosuitePath;
	}
	
	public Path getSushiLibPath() {
		return this.sushiPath;
	}
	
	public void setSushiLibPath(Path sushiPath) {
		this.sushiPath = sushiPath;
	}
	
	public Classpath getClasspath() throws IOException {
		final ArrayList<Path> userClasspath = new ArrayList<>();
		userClasspath.add(getJBSELibraryPath());
		userClasspath.addAll(getClassesPath());
		return new Classpath(this.jbsePath, Paths.get(System.getProperty("java.home")), Collections.emptyList(), userClasspath);

	}
	
	public int getEvosuiteTimeBudgetDuration() {
		return this.evosuiteTimeBudgetDuration;
	}
	
	public void setEvosuiteTimeBudgetDuration(int evosuiteTimeBudgetDuration) {
		this.evosuiteTimeBudgetDuration = evosuiteTimeBudgetDuration;
	}
	
	public TimeUnit getEvosuiteTimeBudgetUnit() {
		return this.evosuiteTimeBudgetUnit;
	}
	
	public void setEvosuiteTimeBudgetUnit(TimeUnit evosuiteTimeBudgetUnit) {
		this.evosuiteTimeBudgetUnit = evosuiteTimeBudgetUnit;
	}
	
	public long getGlobalTimeBudgetDuration() {
		return this.globalTimeBudgetDuration;
	}
	
	public void setGlobalTimeBudgetDuration(long globalTimeBudgetDuration) {
		this.globalTimeBudgetDuration = globalTimeBudgetDuration;
	}
	
	public TimeUnit getGlobalTimeBudgetUnit() {
		return this.globalTimeBudgetUnit;
	}
	
	public void setGlobalTimeBudgetUnit(TimeUnit globalTimeBudgetUnit) {
		this.globalTimeBudgetUnit = globalTimeBudgetUnit;
	}
	
	public long getTimeoutMOSATaskCreationDuration() {
		return this.timeoutMOSATaskCreationDuration;
	}
	
	public void setTimeoutMOSATaskCreationDuration(long timeoutMOSATaskCreationDuration) {
		this.timeoutMOSATaskCreationDuration = timeoutMOSATaskCreationDuration;
	}
	
	public TimeUnit getTimeoutMOSATaskCreationUnit() {
		return this.timeoutMOSATaskCreationUnit;
	}
	
	public void setTimeoutMOSATaskCreationUnit(TimeUnit timeoutMOSATaskCreationUnit) {
		this.timeoutMOSATaskCreationUnit = timeoutMOSATaskCreationUnit;
	}
	
	public int getNumMOSATargets() {
		return this.numMOSATargets;
	}
	
	public void setNumMOSATargets(int numMOSATargets) {
		this.numMOSATargets = numMOSATargets;
	}
	
	public boolean getUseMOSA() {
		return this.useMOSA;
	}
	
	public void setUseMOSA(boolean useMOSA) {
		this.useMOSA = useMOSA;
	}
	
	public void setHeapScope(String className, int scope) {
		if (className == null) {
			return;
		}
		if (this.heapScope == null) {
			this.heapScope = new HashMap<>();
		}
		this.heapScope.put(className, Integer.valueOf(scope));
	}
	
	public void setHeapScopeUnlimited(String className) {
		if (this.heapScope == null) {
			return;
		}
		this.heapScope.remove(className);
	}
	
	public void setHeapScopeUnlimited() {
		this.heapScope = new HashMap<>();
	}
	
	public Map<String, Integer> getHeapScope() {
		return (this.heapScope == null ? null : Collections.unmodifiableMap(this.heapScope));
	}
	
	public void setCountScope(int countScope) {
		this.countScope = countScope;
	}
	
	public int getCountScope() {
		return this.countScope;
	}
	
/*	public List<List<String>> getUninterpreted() {
		return this.uninterpreted;
	}*/
	
	public static List<String> sig(String className, String descriptor, String name) {
		return Arrays.asList(className, descriptor, name);
	}
	
/*	@SafeVarargs
	public final void setUninterpreted(List<String>... signatures) {
		this.uninterpreted = Arrays.asList(signatures);
	}*/
	
	@Override
	public JBSEOptions clone() {
		try {
			final JBSEOptions theClone = (JBSEOptions) super.clone();
			if (this.heapScope != null) {
				theClone.heapScope = new HashMap<>(this.heapScope);
			}
			return theClone;
		} catch (CloneNotSupportedException e) {
			//this should never happen
			throw new AssertionError("super.clone() raised CloneNotSupportedException");
		}
	}
}
