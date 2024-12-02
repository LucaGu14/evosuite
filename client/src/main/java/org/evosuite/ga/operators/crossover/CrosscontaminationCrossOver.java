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
package org.evosuite.ga.operators.crossover;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.evosuite.Properties;
import org.evosuite.ga.Chromosome;
import org.evosuite.ga.ConstructionFailedException;
import org.evosuite.testcase.statements.Statement;
import org.evosuite.testcase.variable.VariableReference;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.testcase.TestFactory;
import org.evosuite.utils.Randomness;
import org.evosuite.utils.generic.GenericAccessibleObject;
import org.evosuite.utils.generic.GenericMethod;

/**
 * Use methods from a parent in the other parent
 *
 * @author Giovanni Denaro
 */
public class CrosscontaminationCrossOver extends SushiCrossOver { /*SUSHI: Crossover*/

	private static final long serialVersionUID = -3086477941208910388L;
	
	private final MinimizeGoodOffspringsCrossOverStrategy minimizeStrategy = new MinimizeGoodOffspringsCrossOverStrategy("CrosscontaminationMinimizer");
	private final SelectSuccessfulModifiersCrossOverStrategy selectModifiersStrategy = new SelectSuccessfulModifiersCrossOverStrategy();
	private final SuccessCountCrossOverStrategy successCountStrategy = new SuccessCountCrossOverStrategy(getClass().getName());

	public CrosscontaminationCrossOver() {		
		this.addCrossOverListener(successCountStrategy);
		this.addCrossOverListener(selectModifiersStrategy);
		this.addCrossOverListener(minimizeStrategy);
	}

	/**
	 * {@inheritDoc}
	 *
	 */
	@Override
	protected void doCrossOver(Chromosome parent1, Chromosome parent2)
	        throws ConstructionFailedException {

		if (parent1.size() < 1 || parent2.size() < 1) {
			return;
		}

		if (! (parent1 instanceof TestChromosome && parent2 instanceof TestChromosome)) {
			throw new ConstructionFailedException("This crossover works only for chromosomes of TestChromosome type");
		}

		Chromosome t1 = parent1.clone();
		Chromosome t2 = parent2.clone();

		crossOver((TestChromosome)t1, (TestChromosome)parent2);
		crossOver((TestChromosome)t2, (TestChromosome)parent1);
	
	}
	
	public void crossOver(TestChromosome parent1, TestChromosome parent2) throws ConstructionFailedException {
		
		
		//extract method calls of chromosome parent1
		
		Set<GenericMethod> modifierCalls = null;
		if (Randomness.nextDouble() < .33d)
			modifierCalls = selectModifiersStrategy.getSuccessfulModifiers();
		
		if (modifierCalls == null || modifierCalls.isEmpty()) {
			modifierCalls = new HashSet<GenericMethod>();

			for (int i = 0; i < parent1.getTestCase().size(); i++){
				Statement testStmt = parent1.getTestCase().getStatement(i);
				GenericAccessibleObject<?> call = testStmt.getAccessibleObject();
				if (!(call instanceof GenericMethod)) continue;
				GenericMethod modifierCall = (GenericMethod) call;
				if (modifierCall.isStatic()) continue;
				if(modifierCall.getName().startsWith("test")) continue;
				if(modifierCall.getName().startsWith("get")) continue;
				if(modifierCall.getName().startsWith("is")) continue;
				if(modifierCall.getName().startsWith("has")) continue;
				//LoggingUtils.getEvoLogger().info("Found method in parent1: " + modifierCall);

				modifierCalls.add(modifierCall);
			}
		}
		if (modifierCalls.isEmpty())
			throw new ConstructionFailedException("No modifiers from the parent chromosome");
		logger.debug("Possible modifiers for the parent chromosome: " + modifierCalls);

		
		//iteratively add new methods in parent2
		TestChromosome offspring = (TestChromosome) parent2.clone();
		
		TestFactory testFactory = TestFactory.getInstance();
		
		int maxAttempts = 1 + (offspring.size() / 4);
		int attempts = 1 + Randomness.nextInt(0, maxAttempts);

		for (int i = 0; i < attempts; i++) {
			GenericMethod rndMeth = Randomness.choice(modifierCalls);
			
			//extract possible receiver objects out of offspring
			List<VariableReference> receivers = offspring.getTestCase().getObjects(
					rndMeth.getOwnerClass().getType(), 
					offspring.getTestCase().size() - 1);
			if (receivers.isEmpty()) continue;

			
			VariableReference rndReceiver = Randomness.choice(receivers);
			int rndPosition = rndReceiver.getStPosition();
			rndPosition = 1 + Randomness.nextInt(rndPosition, offspring.size());

			//VariableReference retVal = 
			testFactory.addMethodFor(offspring.getTestCase(), rndReceiver, rndMeth, rndPosition);				
	
			selectModifiersStrategy.recordApplieddModifier(parent2, offspring, rndMeth);
		}

		if (Properties.USE_MINIMIZER_DURING_CROSSOVER || !Properties.CHECK_MAX_LENGTH
				|| offspring.getTestCase().size() <= Properties.CHROMOSOME_LENGTH) {
				parent2.setTestCase(offspring.getTestCase());
				parent2.setChanged(true);
		}
	}


	@Override
	public int getNumberOfMinimizerCalls() {
		return minimizeStrategy.getNumberOfMinimizerCalls();
	}

	@Override
	public Map<String, Integer> getIterationBestSuccessCountsCrossOver() {
		return successCountStrategy.getIterationBestSuccessCountsCrossOver();
	}

	@Override
	public Map<String, Integer> getIterationBestSuccessCountsMutation() {
		return successCountStrategy.getIterationBestSuccessCountsMutation();
	}

	@Override
	public Map<String, Integer> getOverallSuccessCountsCrossOver() {
		return successCountStrategy.getOverallSuccessCountsCrossOver();
	}

	@Override
	public Map<String, Integer> getOverallSuccessCountsMutation() {
		return successCountStrategy.getOverallSuccessCountsMutation();
	}	
}
