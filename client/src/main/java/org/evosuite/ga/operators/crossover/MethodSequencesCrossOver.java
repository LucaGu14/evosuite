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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.evosuite.Properties;
import org.evosuite.ga.Chromosome;
import org.evosuite.ga.ConstructionFailedException;
import org.evosuite.testcase.statements.ConstructorStatement;
import org.evosuite.testcase.statements.FieldStatement;
import org.evosuite.testcase.statements.MethodStatement;
import org.evosuite.testcase.statements.NullStatement;
import org.evosuite.testcase.statements.PrimitiveStatement;
import org.evosuite.testcase.statements.Statement;
import org.evosuite.testcase.variable.VariableReference;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.utils.Randomness;

/**
 * Use methods from a parent in the other parent
 *
 * @author Giovanni Denaro
 */
public class MethodSequencesCrossOver extends SushiCrossOver { /*SUSHI: Crossover*/

	private static final long serialVersionUID = -3086477941208910388L;
	
	private final CrossOverFunction cutpointsCrossOverFunction;
	
	private final MinimizeGoodOffspringsCrossOverStrategy minimizeStrategy = new MinimizeGoodOffspringsCrossOverStrategy("MethodSeqMinimizer");
	private final SuccessCountCrossOverStrategy successCountStrategy = new SuccessCountCrossOverStrategy(getClass().getName());

	public MethodSequencesCrossOver(CrossOverFunction crossover_function) {
		this.cutpointsCrossOverFunction = crossover_function;
		this.addCrossOverListener(minimizeStrategy);
		this.addCrossOverListener(successCountStrategy);
	}

	/**
	 * {@inheritDoc}
	 **/
	@Override 
	protected void doCrossOver(Chromosome parent1, Chromosome parent2)
	        throws ConstructionFailedException {
		
		
		if (! (parent1 instanceof TestChromosome && parent2 instanceof TestChromosome)) {
			throw new ConstructionFailedException("This crossover works only for chromosomes of TestChromosome type");
		}
		
		TestChromosomeWithSequencePreservingCrossover parent1Boxed = 
				new TestChromosomeWithSequencePreservingCrossover((TestChromosome) parent1);
		TestChromosomeWithSequencePreservingCrossover parent2Boxed = 
				new TestChromosomeWithSequencePreservingCrossover((TestChromosome) parent2);

		cutpointsCrossOverFunction.crossOver(parent1Boxed, parent2Boxed); //will eventually reach TestChromosomeWithSequencePreservingCrossover.crossover

	}
	

	private static class TestChromosomeWithSequencePreservingCrossover extends TestChromosomeDecorator {
		private static final long serialVersionUID = 7167488042842983779L;

		public TestChromosomeWithSequencePreservingCrossover(TestChromosome embeddedTestChromosome) {
			super(embeddedTestChromosome);
		}
		
		
		@Override
		public void crossOver(TestChromosome other, int position) throws ConstructionFailedException {
			crossOver(other, position, position);
		}


		@Override
		public void crossOver(TestChromosome other, int position1, int position2)
				throws ConstructionFailedException {
			logger.debug("Crossover starting");
			
			
			if ((position1 == this.size() && position2 == 0) || (position1 == 0 && position2 == other.size())) 
				return;

			
			TestChromosome offspring = new TestChromosome();

			for (int i = 0; i < position1; i++) {
				offspring.getTestCase().addStatement(getTestCase().getStatement(i).clone(offspring.getTestCase()));
			}
			
					
			//HashSet<VariableReference> includedFromPrefix = new HashSet<VariableReference>(); 
			HashMap<VariableReference, VariableReference> cuttedObjs = new HashMap<VariableReference, VariableReference>(); 
			//boolean[] markedForInclusion = new boolean[other.size()];
			//boolean[] inspected = new boolean[other.size()];
			int[] relocation = new int[other.size()];
			for (int i = 0; i < other.size(); i++) {
				//markedForInclusion[i] = false;
				//inspected[i] = false;
				relocation[i] = -1;
				
				if (i < position2) continue;
				//markedForInclusion[i] = true;
				Statement stmt = ((TestChromosome) other).getTestCase().getStatement(i);
				Set<VariableReference> varRefs = stmt.getVariableReferences();
				for (VariableReference var : varRefs) {
					if (var.getStPosition() >= position2) continue;
					List<VariableReference> receivers = offspring.getTestCase().getObjects(var.getType(), position1);
					VariableReference newCallee;
					if (receivers == null || receivers.isEmpty()) {
						newCallee = null;
					}
					else {
						newCallee = Randomness.choice(receivers);
					}
					cuttedObjs.put(var, newCallee);
				}
			}
			
			for (int i = position2; i < other.size(); i++) {
				
				Statement stmt = ((TestChromosome) other).getTestCase().getStatement(i);
				Statement newStmt = null;

				if (stmt instanceof ConstructorStatement) {
					ConstructorStatement constructor = (ConstructorStatement) stmt;
					List<VariableReference> params = 
							relocatedParams(offspring, constructor.getParameterReferences(), relocation, cuttedObjs);
					newStmt = new ConstructorStatement(offspring.getTestCase(), constructor.getConstructor(), params);
				} else if (stmt instanceof MethodStatement) {
					MethodStatement method = (MethodStatement) stmt;
					VariableReference callee = relocatedParam(offspring, method.getCallee(), relocation, cuttedObjs);
					if (callee == null && !method.isStatic()) 
						throw new ConstructionFailedException("No receiver objects in parent1 for this crossover");

					List<VariableReference> params = 
							relocatedParams(offspring, method.getParameterReferences(), relocation, cuttedObjs);
					newStmt = new MethodStatement(offspring.getTestCase(), method.getMethod(), callee, params);
				} else if (stmt instanceof PrimitiveStatement<?>) {
					newStmt = stmt.clone(offspring.getTestCase());
				} else if (stmt instanceof FieldStatement) {
					FieldStatement field = (FieldStatement) stmt;
					VariableReference callee = relocatedParam(offspring, field.getSource(), relocation, cuttedObjs);
					if (callee == null && !field.isStatic()) 
						throw new ConstructionFailedException("No receiver objects in parent1 for this crossover");
					newStmt = new FieldStatement(offspring.getTestCase(), field.getField(), callee);
				} 
				if (newStmt != null) {
					/*VariableReference ret =*/ offspring.getTestCase().addStatement(newStmt, offspring.getTestCase().size());
					relocation[i] =  offspring.getTestCase().size() - 1;
				}
			}

			if (Properties.USE_MINIMIZER_DURING_CROSSOVER || !Properties.CHECK_MAX_LENGTH
			        || offspring.getTestCase().size() <= Properties.CHROMOSOME_LENGTH) {
				setTestCase(offspring.getTestCase());
				setChanged(true);
			}
			
		}
		
		private List<VariableReference> relocatedParams(TestChromosome offspring, List<VariableReference> params, 
				int[] relocationMap, HashMap<VariableReference, VariableReference> relocationRoots) throws ConstructionFailedException {
			List<VariableReference> relocatedParams = new ArrayList<VariableReference>();
			for (VariableReference p : params) {
				VariableReference newP = relocatedParam(offspring, p, relocationMap, relocationRoots);
				if (newP == null) {
					NullStatement nstmt = new NullStatement(offspring.getTestCase(), p.getType());
					newP = offspring.getTestCase().addStatement(nstmt, offspring.size());
				}
				relocatedParams.add(newP);
			}
			return relocatedParams;
		}

		private VariableReference relocatedParam(TestChromosome offspring, VariableReference param, 
				int[] relocationMap, HashMap<VariableReference, VariableReference> relocationRoots) 
						throws ConstructionFailedException {
		
			if (param == null) return null;
			
			VariableReference relocatedP;
			int relocationSite = relocationMap[param.getStPosition()];
			if (relocationSite < 0) {
				if (!relocationRoots.keySet().contains(param))
					throw new ConstructionFailedException("Cannot relocate parameter: " + param.toString() + " in offspring");
				relocatedP = relocationRoots.get(param);
				if (relocatedP == null) 
					return null;//throw new ConstructionFailedException("No receiver objects in parent1 for this crossover");
			} else {
				if (relocationSite >= offspring.size())
					throw new RuntimeException("must not happen");
				Statement parStmt = offspring.getTestCase().getStatement(relocationSite);
				relocatedP = parStmt.getReturnValue();
			}
			return relocatedP;
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
