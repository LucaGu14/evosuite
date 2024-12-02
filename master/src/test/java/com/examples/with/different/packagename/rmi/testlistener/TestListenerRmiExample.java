package com.examples.with.different.packagename.rmi.testlistener;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map.Entry;
import java.util.Properties;

// TODO refer the proper imports if you are using EvoSuite with the shaded jar
import /*shaded.*/org.evosuite.ga.FitnessFunction;
import /*shaded.*/org.evosuite.rmi.UtilsRMI;
import /*shaded.*/org.evosuite.rmi.service.EvosuiteRemote;
import /*shaded.*/org.evosuite.rmi.service.TestListenerRemote;
import /*shaded.*/org.evosuite.utils.Randomness;

public class TestListenerRmiExample implements TestListenerRemote {
	//TODO: set your local paths
	public static final String JAVA_HOME = "/Library/Java/JavaVirtualMachines/jdk1.8.0_25.jdk/Contents/Home/bin/java"; 
	public static final String EVO_FOLDER = "/Users/denaro/git/evosuite";
	
	public static final String appRmiIdentifier = "MyAppToTestRmiInteractionsWithEvosuite";
	
	public static void main(String[] args) throws Exception {
		TestListenerRmiExample app = new TestListenerRmiExample();
		app.startRegistry();
		app.connectToRegistry();
		app.runEvosuite();
		app.executeLocalTask();
	}
	
	private int registryPort = -1;
	private Registry registry;
	EvosuiteRemote evosuiteMasterNode = null;
	
	void startRegistry() {
		int port = 2000;
		port += Randomness.nextInt(20000);

		final int TRIES = 100;
		for (int i = 0; i < TRIES; i++) {
			try {
				int candidatePort = port + i;
				UtilsRMI.ensureRegistryOnLoopbackAddress();
				registry = LocateRegistry.createRegistry(candidatePort);
				registryPort = candidatePort;
			} catch (RemoteException e) {
			}
		}
		if (registry == null) {
			throw new RuntimeException("unable to create registry");
		}
		System.out.println("Started registry on port " + registryPort);
	}

	void connectToRegistry() throws Exception {
		TestListenerRemote stub = (TestListenerRemote) UtilsRMI.exportObject(this);
		registry.rebind(appRmiIdentifier, stub);
		System.out.println("Connected to registry " + registryPort);
	}

	void runEvosuite() {	
		// build command line
		final String TARGET_CLASS = TestListenerRmiExample.class.getPackage().getName() + ".SampleTargetProgram";
		final String TARGET_EVALUATORS_PACKAGE = TestListenerRmiExample.class.getPackage().getName() + ".evaluators";

		final ArrayList<String> evosuiteCommand = new ArrayList<String>();
		evosuiteCommand.add(JAVA_HOME);
		evosuiteCommand.add("-Xmx4G");
		
		//evosuiteCommand.add("-jar");
		//evosuiteCommand.add(EVO_JAR);
		evosuiteCommand.add("-cp");
		evosuiteCommand.add(System.getProperty("java.class.path"));
		evosuiteCommand.add("org.evosuite.EvoSuite");
		
		evosuiteCommand.add("-class");
		evosuiteCommand.add(TARGET_CLASS);
		evosuiteCommand.add("-mem");
		evosuiteCommand.add("2048");
		evosuiteCommand.add("-DCP=" + EVO_FOLDER + "/master/target/test-classes"); 
		evosuiteCommand.add("-Dassertions=false");
		//retVal.add("-Dglobal_timeout=" + configuration.getEvoSuiteBudget());
		evosuiteCommand.add("-Dsearch_budget=60");
		evosuiteCommand.add("-Dreport_dir="  + EVO_FOLDER + "/master/src/test/java/" + TARGET_EVALUATORS_PACKAGE.replace('.', '/'));
		evosuiteCommand.add("-Dtest_dir=" + EVO_FOLDER + "/master/src/test/java");
		evosuiteCommand.add("-Dvirtual_fs=false");
		evosuiteCommand.add("-Dselection_function=ROULETTEWHEEL");
		evosuiteCommand.add("-Dcriterion=PATHCONDITION:BRANCH");		
		evosuiteCommand.add("-Dpath_condition_target=LAST_ONLY");
		evosuiteCommand.add("-Dpost_condition_check=true"); 
		evosuiteCommand.add("-Dsushi_statistics=true");
		evosuiteCommand.add("-Dinline=true");
		evosuiteCommand.add("-Dsushi_modifiers_local_search=true");
		evosuiteCommand.add("-Duse_minimizer_during_crossover=false");
		evosuiteCommand.add("-Davoid_replicas_of_individuals=true"); 
		evosuiteCommand.add("-Dno_change_iterations_before_reset=30");
		evosuiteCommand.add("-Dno_runtime_dependency");
		evosuiteCommand.add("-Dpath_condition_evaluators_dir=" + EVO_FOLDER + "/master/target/test-classes");
		evosuiteCommand.add("-Demit_tests_incrementally=true");
		evosuiteCommand.add("-Dcrossover_function=SUSHI_HYBRID");
		evosuiteCommand.add("-Dalgorithm=DYNAMOSA");
		evosuiteCommand.add("-generateMOSuite");
		evosuiteCommand.add("-Dpath_condition=" + TARGET_CLASS + ",sum(II)I," + TARGET_EVALUATORS_PACKAGE + ".EvoSuiteEvaluator_1:" + TARGET_CLASS + ",sum(II)I," + TARGET_EVALUATORS_PACKAGE + ".EvoSuiteEvaluator_2");
		evosuiteCommand.add("-Dcheck_path_conditions_only_for_direct_calls=true");
		evosuiteCommand.add("-Ddebug=false");
		
		evosuiteCommand.add("-Dexternal_rmi_registry_port=" + registryPort);
		evosuiteCommand.add("-Dtest_listener_rmi_identifier=" + appRmiIdentifier);
		evosuiteCommand.add("-Dinjected_path_conditions_checking_rate=50");
		evosuiteCommand.add("-Ddismiss_path_conditions_no_improve_iterations=50");
		
		

		// Launch EvoSuite
		Thread t = new Thread() {
			@Override
			public void run() {
				final Path evosuiteLogFilePath = Paths.get(EVO_FOLDER + "/master/src/test/java/" + TARGET_EVALUATORS_PACKAGE.replace('.', '/')).resolve("evosuite-log.txt");
				try {
					final ProcessBuilder pb = new ProcessBuilder(evosuiteCommand).redirectErrorStream(true).redirectOutput(evosuiteLogFilePath.toFile());
					final Process processEvosuite = pb.start();
					System.out.println("Launched EvoSuite process, command line: " + evosuiteCommand.stream().reduce("", (s1, s2) -> { return s1 + " " + s2; }));
					try {
						processEvosuite.waitFor();
					} catch (InterruptedException e) {
						//the performer was shut down: kill the EvoSuite job
						System.err.println("Unexpected InterruptedException while running EvoSuite: " + e);
						processEvosuite.destroy();
					}
				} catch (IOException e) {
					System.err.println("Unexpected I/O error while running EvoSuite: " + e);
					throw new RuntimeException(e);
				}
				System.out.println("EvoSuite process finished");
				System.exit(0);
			}
		};
		t.start();
	}

	void executeLocalTask() {
		// some fake task that works with system resources and makes this process busy forever
		//    since we want to test that we can keep receiving RMI notifications while working
		System.out.println("From now on, getting busy in many local activities");
		int count = 0;
		while (true) {
			Properties pp = System.getProperties();
			for (Entry<Object, Object> e: pp.entrySet()) {
				e.getValue();
			}
			
			// at some point we try to send a new goal to EvoSuite
			if (evosuiteMasterNode != null) {
				++count;
			}
			if (count == 1000000) {
				sendPathConditionsToEvosuite();
			}
		}
	}

	void sendPathConditionsToEvosuite() {
		if (evosuiteMasterNode != null) {
			try {
				evosuiteMasterNode.evosuite_injectFitnessFunction("postcond.Example1", "sum(II)I", "postcond.EvoSuiteEvaluator_3");
			} catch (RemoteException e) {
				System.err.println("Error when sending new goals to evosuite process: " + e);
				e.printStackTrace();
			}
			System.out.println("Sent new goal to evosuite process: postcond.EvoSuiteEvaluator_3");
		}
	}

	// Methods below implement interface TestListenerRemote to receive notifications from EvoSuite
	
	@Override
	public void evosuiteServerReady(String evosuiteServerRmiIdentifier) throws RemoteException {
		System.out.println("Evosuite server is ready, name is " + evosuiteServerRmiIdentifier);
		try {
			evosuiteMasterNode = (EvosuiteRemote) registry.lookup(evosuiteServerRmiIdentifier);
		} catch (NotBoundException e) {
			System.err.println("Error when connecting to evosuite server via rmi with identifier " + evosuiteServerRmiIdentifier + ": " + e);
			e.printStackTrace();
		}
		System.out.println("Connected to EvoSuite process with RMI identifier: " + evosuiteServerRmiIdentifier);
	}

	@Override
	public void generatedTest(String evosuiteServerRmiIdentifier, FitnessFunction<?> goal, String testFileName) throws RemoteException {
		System.out.println("Evosuite server " + evosuiteServerRmiIdentifier +  " communicated new test: " + testFileName + " -- It is for goal: " + goal);
	}

	@Override
	public void dismissedFitnessGoal(String evosuiteServerRmiIdentifier, FitnessFunction<?> goal, int iteration,
			double fitnessValue, int[] updateIterations) throws RemoteException {
		System.out.println("Evosuite server " + evosuiteServerRmiIdentifier +  " communicated dismissed goal: " + goal + ", iteration is " + iteration + ", fitness is " + fitnessValue + ", with updates at iterations " + Arrays.toString(updateIterations));
	}

	@Override
	public void evosuiteServerShutdown(String evosuiteServerRmiIdentifier) throws RemoteException {
		System.out.println("Evosuite server " + evosuiteServerRmiIdentifier +  " terminated");
	}

}
