package org.evosuite.ga.operators.crossover;

import org.evosuite.ga.Chromosome;
import org.evosuite.ga.metaheuristics.SearchListener;

public interface CrossOverListener extends SearchListener {

	public void crossOverBegin(Chromosome parent1, Chromosome parent2);

	public void crossOverDone(Chromosome parent1, Chromosome parent2);

	public void crossOverException(Chromosome parent1, Chromosome parent2);

	public void inNextGeneration(Chromosome chromosome);
}
