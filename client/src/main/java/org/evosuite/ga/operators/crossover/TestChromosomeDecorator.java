package org.evosuite.ga.operators.crossover;

import java.util.LinkedHashMap;
import java.util.Map;

import org.evosuite.coverage.mutation.Mutation;
import org.evosuite.coverage.mutation.MutationExecutionResult;
import org.evosuite.ga.Chromosome;
import org.evosuite.ga.ConstructionFailedException;
import org.evosuite.ga.FitnessFunction;
import org.evosuite.ga.localsearch.LocalSearchObjective;
import org.evosuite.ga.operators.mutation.MutationHistory;
import org.evosuite.testcase.ExecutableChromosome;
import org.evosuite.testcase.TestCase;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.testcase.TestFactory;
import org.evosuite.testcase.TestMutationHistoryEntry;
import org.evosuite.testcase.execution.ExecutionResult;
import org.evosuite.testsuite.TestSuiteFitnessFunction;

public class TestChromosomeDecorator extends TestChromosome {
	
	private static final long serialVersionUID = 4270058621466074700L;

	private final TestChromosome embeddedTestChromosome;

	public TestChromosomeDecorator(TestChromosome embeddedTestChromosome) {
		super();
		if (embeddedTestChromosome == null) {
			throw new RuntimeException("cannot decorate a null TestChromosome");
		}
		this.embeddedTestChromosome = embeddedTestChromosome;
	}

	public TestChromosome getEmbeddedTestChromosome() {
		return embeddedTestChromosome;
	}

	@Override
	public void setTestCase(TestCase testCase) {
		embeddedTestChromosome.setTestCase(testCase);
	}

	@Override
	public TestCase getTestCase() {
		return embeddedTestChromosome.getTestCase();
	}

	@Override
	public void setLastExecutionResult(ExecutionResult lastExecutionResult) {
		embeddedTestChromosome.setLastExecutionResult(lastExecutionResult);
	}

	@Override
	public void setChanged(boolean changed) {
		embeddedTestChromosome.setChanged(changed);
	}

	@Override
	public TestChromosome clone() {
		return embeddedTestChromosome.clone();
	}

	@Override
	public void copyCachedResults(TestChromosome other) {
		embeddedTestChromosome.copyCachedResults(other);
	}

	@Override
	public void crossOver(TestChromosome other, int position1, int position2) throws ConstructionFailedException {
		embeddedTestChromosome.crossOver(other, position1, position2);
	}

	@Override
	public boolean equals(Object obj) {
		return embeddedTestChromosome.equals(obj);
	}

	@Override
	public int hashCode() {
		return embeddedTestChromosome.hashCode();
	}

	@Override
	public MutationHistory<TestMutationHistoryEntry> getMutationHistory() {
		return embeddedTestChromosome.getMutationHistory();
	}

	@Override
	public void clearMutationHistory() {
		embeddedTestChromosome.clearMutationHistory();
	}

	@Override
	public boolean hasRelevantMutations() {
		return embeddedTestChromosome.hasRelevantMutations();
	}

	@Override
	public boolean localSearch(LocalSearchObjective<TestChromosome> objective) {
		return embeddedTestChromosome.localSearch(objective);
	}

	@Override
	public void mutate() {
		embeddedTestChromosome.mutate();
	}

	@Override
	protected boolean deleteStatement(TestFactory testFactory, int num) {
		throw new RuntimeException("Cannot call this method for a the TestChromosome decorator");
	}

	@Override
	public int size() {
		return embeddedTestChromosome.size();
	}

	@Override
	public int compareTo(TestChromosome o) {
		return embeddedTestChromosome.compareTo(o);
	}

	@Override
	public String toString() {
		return embeddedTestChromosome.toString();
	}

	@Override
	public boolean hasException() {
		return embeddedTestChromosome.hasException();
	}

	@Override
	public ExecutionResult executeForFitnessFunction(TestSuiteFitnessFunction testSuiteFitnessFunction) {
		return embeddedTestChromosome.executeForFitnessFunction(testSuiteFitnessFunction);
	}

	@Override
	public int compareSecondaryObjective(TestChromosome o) {
		return embeddedTestChromosome.compareSecondaryObjective(o);
	}

	@Override
	public ExecutionResult getLastExecutionResult() {
		return embeddedTestChromosome.getLastExecutionResult();
	}

	@Override
	public void setLastExecutionResult(MutationExecutionResult lastExecutionResult, Mutation mutation) {
		embeddedTestChromosome.setLastExecutionResult(lastExecutionResult, mutation);
	}

	@Override
	public MutationExecutionResult getLastExecutionResult(Mutation mutation) {
		return embeddedTestChromosome.getLastExecutionResult(mutation);
	}

	@Override
	public void clearCachedResults() {
		embeddedTestChromosome.clearCachedResults();
	}

	@Override
	public void clearCachedMutationResults() {
		embeddedTestChromosome.clearCachedMutationResults();
	}

	@Override
	public double getFitness() {
		return embeddedTestChromosome.getFitness();
	}

	@Override
	public double getFitness(FitnessFunction ff) {
		return embeddedTestChromosome.getFitness(ff);
	}

	@Override
	public Map getFitnessValues() {
		return embeddedTestChromosome.getFitnessValues();
	}

	@Override
	public Map getPreviousFitnessValues() {
		return embeddedTestChromosome.getPreviousFitnessValues();
	}

	@Override
	public boolean hasExecutedFitness(FitnessFunction ff) {
		return embeddedTestChromosome.hasExecutedFitness(ff);
	}

	@Override
	public void setFitnessValues(Map fits) {
		embeddedTestChromosome.setFitnessValues(fits);
	}

	@Override
	public void setPreviousFitnessValues(Map lastFits) {
		embeddedTestChromosome.setPreviousFitnessValues(lastFits);
	}

/*	@Override
	public boolean isToBeUpdated() {
		return embeddedTestChromosome.isToBeUpdated();
	}

	@Override
	public void isToBeUpdated(boolean toBeUpdated) {
		embeddedTestChromosome.isToBeUpdated(toBeUpdated);
	}*/

	@Override
	public void addFitness(FitnessFunction ff) {
		embeddedTestChromosome.addFitness(ff);
	}

	@Override
	public void addFitness(FitnessFunction ff, double fitnessValue) {
		embeddedTestChromosome.addFitness(ff, fitnessValue);
	}

	@Override
	public void addFitness(FitnessFunction ff, double fitnessValue, double coverage) {
		embeddedTestChromosome.addFitness(ff, fitnessValue, coverage);
	}

	@Override
	public void addFitness(FitnessFunction ff, double fitnessValue, double coverage, int numCoveredGoals) {
		embeddedTestChromosome.addFitness(ff, fitnessValue, coverage, numCoveredGoals);
	}

	@Override
	public void setFitness(FitnessFunction ff, double value) throws IllegalArgumentException {
		embeddedTestChromosome.setFitness(ff, value);
	}

	@Override
	public boolean hasFitnessChanged() {
		return embeddedTestChromosome.hasFitnessChanged();
	}

	@Override
	public void crossOver(TestChromosome other, int position) throws ConstructionFailedException {
		embeddedTestChromosome.crossOver(other, position);
	}

	@Override
	public boolean isChanged() {
		return embeddedTestChromosome.isChanged();
	}

	@Override
	public boolean hasLocalSearchBeenApplied() {
		return embeddedTestChromosome.hasLocalSearchBeenApplied();
	}

	@Override
	public void setLocalSearchApplied(boolean localSearchApplied) {
		embeddedTestChromosome.setLocalSearchApplied(localSearchApplied);
	}

	@Override
	public double getCoverage() {
		return embeddedTestChromosome.getCoverage();
	}

	@Override
	public int getNumOfCoveredGoals() {
		return embeddedTestChromosome.getNumOfCoveredGoals();
	}

	@Override
	public int getNumOfNotCoveredGoals() {
		return embeddedTestChromosome.getNumOfNotCoveredGoals();
	}

	@Override
	public void setNumsOfCoveredGoals(Map fits) {
		embeddedTestChromosome.setNumsOfCoveredGoals(fits);
	}

	@Override
	public void setNumsOfNotCoveredGoals(Map fits) {
		embeddedTestChromosome.setNumsOfNotCoveredGoals(fits);
	}

	@Override
	public void setNumOfNotCoveredGoals(FitnessFunction ff, int numCoveredGoals) {
		embeddedTestChromosome.setNumOfNotCoveredGoals(ff, numCoveredGoals);
	}

	@Override
	public Map getNumsOfCoveredGoals() {
		return embeddedTestChromosome.getNumsOfCoveredGoals();
	}

	@Override
	public LinkedHashMap getNumsNotCoveredGoals() {
		return embeddedTestChromosome.getNumsNotCoveredGoals();
	}

	@Override
	public Map getCoverageValues() {
		return embeddedTestChromosome.getCoverageValues();
	}

	@Override
	public void setCoverageValues(Map coverages) {
		embeddedTestChromosome.setCoverageValues(coverages);
	}

	@Override
	public double getCoverage(FitnessFunction ff) {
		return embeddedTestChromosome.getCoverage(ff);
	}

	@Override
	public void setCoverage(FitnessFunction ff, double coverage) {
		embeddedTestChromosome.setCoverage(ff, coverage);
	}

	@Override
	public int getNumOfCoveredGoals(FitnessFunction<?> ff) {
		return embeddedTestChromosome.getNumOfCoveredGoals(ff);
	}

	@Override
	public int getNumOfNotCoveredGoals(FitnessFunction<?> ff) {
		return embeddedTestChromosome.getNumOfNotCoveredGoals(ff);
	}

	@Override
	public void setNumOfCoveredGoals(FitnessFunction ff, int numCoveredGoals) {
		embeddedTestChromosome.setNumOfCoveredGoals(ff, numCoveredGoals);
	}

	@Override
	public void updateAge(int generation) {
		embeddedTestChromosome.updateAge(generation);
	}

	@Override
	public int getAge() {
		return embeddedTestChromosome.getAge();
	}

	@Override
	public int getRank() {
		return embeddedTestChromosome.getRank();
	}

	@Override
	public void setRank(int r) {
		embeddedTestChromosome.setRank(r);
	}

	@Override
	public double getDistance() {
		return embeddedTestChromosome.getDistance();
	}

	@Override
	public void setDistance(double d) {
		embeddedTestChromosome.setDistance(d);
	}

	@Override
	public double getFitnessInstanceOf(Class<?> clazz) {
		return embeddedTestChromosome.getFitnessInstanceOf(clazz);
	}

	@Override
	public double getCoverageInstanceOf(Class<?> clazz) {
		return embeddedTestChromosome.getCoverageInstanceOf(clazz);
	}

	@Override
	protected void finalize() throws Throwable {
		throw new RuntimeException("Cannot call this method for a the TestChromosome decorator");
	}

}
