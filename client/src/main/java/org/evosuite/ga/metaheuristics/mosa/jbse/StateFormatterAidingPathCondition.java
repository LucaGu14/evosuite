package org.evosuite.ga.metaheuristics.mosa.jbse;

import static jbse.common.Type.className;
import static jbse.common.Type.isReference;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.evosuite.coverage.pathcondition.IApcEvaluator;

import jbse.apps.Formatter;
import jbse.common.Type;
import jbse.common.exc.InvalidInputException;
import jbse.common.exc.UnexpectedInternalException;
import jbse.mem.Clause;
import jbse.mem.ClauseAssume;
import jbse.mem.ClauseAssumeAliases;
import jbse.mem.ClauseAssumeExpands;
import jbse.mem.ClauseAssumeNull;
import jbse.mem.Objekt;
import jbse.mem.State;
import jbse.mem.exc.FrozenStateException;
import jbse.rewr.CalculatorRewriting;
import jbse.val.Any;
import jbse.val.Expression;
import jbse.val.NarrowingConversion;
import jbse.val.Operator;
import jbse.val.Primitive;
import jbse.val.PrimitiveSymbolic;
import jbse.val.PrimitiveSymbolicApply;
import jbse.val.PrimitiveSymbolicAtomic;
import jbse.val.PrimitiveSymbolicHashCode;
import jbse.val.PrimitiveSymbolicLocalVariable;
import jbse.val.PrimitiveSymbolicMemberArray;
import jbse.val.PrimitiveSymbolicMemberArrayLength;
import jbse.val.PrimitiveSymbolicMemberField;
import jbse.val.PrimitiveVisitor;
import jbse.val.ReferenceSymbolic;
import jbse.val.Simplex;
import jbse.val.Symbolic;
import jbse.val.Term;
import jbse.val.Value;
import jbse.val.WideningConversion;
import jbse.val.exc.InvalidOperandException;
import jbse.val.exc.InvalidOperatorException;
import jbse.val.exc.InvalidTypeException;

/**
 * A {@link Formatter} used by Sushi (check of path condition
 * clauses).
 * 
 * @author Pietro Braione, Giovanni Denaro
 * 
 */
public final class StateFormatterAidingPathCondition implements Formatter {
	private final long methodNumber;
	private final Supplier<Long> traceCounterSupplier;
	private final Supplier<State> initialStateSupplier;
	private StringBuilder output = new StringBuilder();
	private int testCounter = 0;
	private String packagePath;
	private final CalculatorRewriting calc;
	private final HashMap<Long, String> stringLiterals;

	public StateFormatterAidingPathCondition(long methodNumber,
			                                Supplier<Long> traceCounterSupplier,
			                                Supplier<State> initialStateSupplier, CalculatorRewriting calc, HashMap<Long, String> stringLiterals) {
		this.calc = calc;
		this.methodNumber = methodNumber;
		this.traceCounterSupplier = traceCounterSupplier;
		this.initialStateSupplier = initialStateSupplier;
		this.stringLiterals = stringLiterals;
	}

	public StateFormatterAidingPathCondition(int methodNumber, String packagePath,
			                                Supplier<State> initialStateSupplier, CalculatorRewriting calc, HashMap<Long, String> stringLiterals) {
		this(methodNumber, (Supplier<Long>) null, initialStateSupplier, calc, stringLiterals);
		if (packagePath != null) {
			this.packagePath = packagePath.replace('/', '.');
		}
	}

	private List<Clause> subformula = new ArrayList<>();

	public void refineFormula(State finalState) {
		
		Collection<Clause> pathCondition = finalState.getPathCondition();
		
		Map<String, NumericRange> collectedRangeAssumptions = new HashMap<>();
		Set<Clause> alreadySeen = new HashSet<>();

		System.out.println("Entire path condition: " + pathCondition);
		for (final Clause clause : pathCondition) {
			if (alreadySeen.contains(clause)) {
				continue;
			}
			alreadySeen.add(clause);
			
			if (clause instanceof ClauseAssume) { 
				//System.out.println("clause: " + clause);
				ClauseAssume assumption = (ClauseAssume) clause;
				if (!excludeClause(assumption)) {
					//Primitive expr = tryToCollectAsRangeAssumptions(assumption.getCondition(), collectedRangeAssumptions, false);
					//if (expr != null) {
					//	try {
					//		assumption = new ClauseAssume(expr);
					//	} catch (InvalidInputException e) {
					//		//this should not happen
					//	}
						//System.out.println("--DEBUG: directly added numeric clause: " + clause);
						subformula.add(assumption);
					//}
				}
			} else if (/*clause instanceof ClauseAssumeExpands ||*/ clause instanceof ClauseAssumeNull || clause instanceof ClauseAssumeAliases) {
				//System.out.println("clause: " + clause);
				/* ClauseAssumeReferenceSymbolic assumptionRef = (ClauseAssumeReferenceSymbolic) clause;
				final Set<Symbolic> assumptionSymbols = new HashSet<>();
				ReferenceSymbolic ref = assumptionRef.getReference();
				assumptionSymbols.add(ref);
				
				if (assumptionRef instanceof ClauseAssumeAliases) {
					final ClauseAssumeAliases clauseAliases = (ClauseAssumeAliases) assumptionRef;
					final long targetHeapPosition = clauseAliases.getHeapPosition();
					
					final Collection<Clause> path = finalState.getPathCondition();
					for (Clause c : path) {
						if (c instanceof ClauseAssumeExpands) { // == Obj fresh
							final ClauseAssumeExpands clauseExpands = (ClauseAssumeExpands) c;
							final long heapPosCurrent = clauseExpands.getHeapPosition();
							if (heapPosCurrent == targetHeapPosition) {
								assumptionSymbols.add(clauseExpands.getReference());
								break;
							}
						}
					}
				} */
				subformula.add(clause);
			}
		}
		
		/*for (NumericRange r : collectedRangeAssumptions.values()) {
			for (ClauseAssume assumption[] : r.getAsAssumptions()) {
				System.out.println("--DEBUG: range-simplified numeric clause: " + Arrays.toString(assumption));
				subformula.addAll(Arrays.asList(assumption));				
			}
		}*/
		
		System.out.println("Considering the sub-formula: " + Arrays.toString(subformula.toArray()));
		
	}
	
	private boolean excludeClause(ClauseAssume assumption) {
		final boolean[] exclude = {false};

		final PrimitiveVisitor v = new PrimitiveVisitor() {

			@Override
			public void visitWideningConversion(WideningConversion x) throws Exception { }

			@Override
			public void visitTerm(Term x) throws Exception { }

			@Override
			public void visitSimplex(Simplex x) throws Exception { }

			@Override
			public void visitNarrowingConversion(NarrowingConversion x) throws Exception { }

			@Override
			public void visitPrimitiveSymbolicApply(PrimitiveSymbolicApply x) throws Exception { }

			@Override
			public void visitExpression(Expression e) throws Exception {
				if (e.isUnary()) { 
					e.getOperand().accept(this);
				} else {
					Primitive o1 = e.getFirstOperand();
					Primitive o2 = e.getSecondOperand();

					if (o1 instanceof Expression || o2 instanceof Expression) {
						o1.accept(this);
						o2.accept(this);
					} else {
						//Exclude clauses with self-referential conditions, e.g., {V1}=={V1}, {V1}>{V1}, etc.
						if (o1.equals(o2)) {
							exclude[0] = true;
						}

						//Exclude clauses on hashCode
						boolean isAboutHashCode = 
								(o1 instanceof PrimitiveSymbolicAtomic &&
										((PrimitiveSymbolicAtomic) o1).asOriginString().contains("ashCode")) ||
								(o2 instanceof PrimitiveSymbolicAtomic &&
										((PrimitiveSymbolicAtomic) o2).asOriginString().contains("ashCode"));
						if (isAboutHashCode) {
							exclude[0] = true;
						}
					}
				}
			}

			@Override
			public void visitAny(Any x) { }

			@Override
			public void visitPrimitiveSymbolicHashCode(PrimitiveSymbolicHashCode x) throws Exception {
			}

			@Override
			public void visitPrimitiveSymbolicLocalVariable(PrimitiveSymbolicLocalVariable x) throws Exception {
			}

			@Override
			public void visitPrimitiveSymbolicMemberArray(PrimitiveSymbolicMemberArray x) throws Exception {
			}

			@Override
			public void visitPrimitiveSymbolicMemberArrayLength(PrimitiveSymbolicMemberArrayLength x) throws Exception {
			}

			@Override
			public void visitPrimitiveSymbolicMemberField(PrimitiveSymbolicMemberField x) throws Exception {
			}
		};

		try {
			assumption.getCondition().accept(v);
		} catch (Exception exc) {
			//this should never happen
			throw new AssertionError(exc);
		}
		return exclude[0];
	}


	private static class NumericRange {
		private final PrimitiveSymbolic var;
		private Simplex inf = null; // null is for (+/-) infinite
		private boolean infIncluded = false; 
		private Simplex sup = null; // null is for (+/-) infinite
		private boolean supIncluded = false;
		private Simplex EQValue = null;
		private final Set<Simplex> NEValues = new HashSet<Simplex>(); 
		private final CalculatorRewriting calc;
		
		public NumericRange(PrimitiveSymbolic var, CalculatorRewriting calc) {
			//System.out.println("--DEBUG: adding range for var " + var);
			this.var = var;
			this.calc = calc;
		}

		public List<ClauseAssume[]> getAsAssumptions() {
			System.out.print("Synthesizing range clauses for: " + var + " ORIGINS: ");
			
			//update inf/sup according to the equality constraint
			if (EQValue != null) {
				try {
					if (sup == null) {
						if (inf != null) {
							sup = EQValue;
							supIncluded = true;
						}
					} else if (calc.push(EQValue).eq(sup).pop().surelyTrue()) {
						supIncluded = true;						
					}
					if (inf == null) {
						if (sup != null) {
							infIncluded = true;
							inf = EQValue;
						}
					} else if (calc.push(EQValue).eq(inf).pop().surelyTrue()) {
						infIncluded = true;
					}
				} catch (InvalidOperandException | InvalidTypeException e) {
					e.printStackTrace();
				}
			}

			//Size-wise-range optimization: removing constraints wrt 0 
			boolean naturalVars = false;
			for (PrimitiveSymbolic s: symbolsIn(var)) {
				String origin = s.asOriginString();
				naturalVars = naturalVars || origin.endsWith("size") || origin.endsWith("length") || origin.endsWith("count");				
				System.out.print(origin + ", ");
			}
			System.out.println();
			naturalVars = naturalVars && isSummation(var);
			if (naturalVars) {
				System.out.println("Range clauses for this var are on Natural numbers");
				if (inf != null && inf.isZeroOne(true)) {
					inf = null;
				}
			} 
			
			//synthsize inf and sup clauses
			ClauseAssume condOnInf = null;
			if (inf != null) {
				try {
					if (infIncluded) {
						condOnInf = new ClauseAssume(calc.push(var).ge(inf).pop());
					} else {
						condOnInf = new ClauseAssume(calc.push(var).gt(inf).pop());					
					}
				} catch (InvalidOperandException | InvalidTypeException e) {
					e.printStackTrace();
				} catch (InvalidInputException e) {
					e.printStackTrace();
				}
			}
			ClauseAssume condOnSup = null;
			if (sup != null) {
				try {
					if (supIncluded) {
						condOnSup = new ClauseAssume(calc.push(var).le(sup).pop());
					} else {
						condOnSup = new ClauseAssume(calc.push(var).lt(sup).pop());					
					}
				} catch (InvalidOperandException | InvalidTypeException e) {
					e.printStackTrace();
				} catch (InvalidInputException e) {
					e.printStackTrace();
				}
			}

			ArrayList<ClauseAssume[]> assumptions = new ArrayList<>();

			//generate clauses by pairing sup clause to NEValues inside or outside the range
			ArrayList<Simplex> NEValuesSorted = new ArrayList<>(NEValues);
			Collections.sort(NEValuesSorted, new Comparator<Simplex>() {
				@Override
				public int compare(Simplex o1, Simplex o2) {
					try {
						if (calc.push(o1).gt(o2).pop().surelyTrue()) {
							return 1;
						} else if (calc.push(o1).lt(o2).pop().surelyTrue()) {
							return -1;
						} else {
							return 0;
						}
					} catch (InvalidOperandException | InvalidTypeException e) {
						e.printStackTrace();
					}
					return 0;
				}
			});
			boolean condOnInfAlreadyIncluded = false;
			boolean condOnSupAlreadyIncluded = false;
			Primitive currentSeq = null;
			boolean currentSeqIsLessThanInf = false;
			boolean currentSeqIsGreaterThanSup = false;
			Simplex prevNEVal = inf;
			for (Simplex NEVal : NEValuesSorted) {
				try {
					if (naturalVars && NEVal.isZeroOne(true)) {
						//Size-wise-range optimization: removing constraints wrt 0 
						continue;
					}
					boolean lessThanInf = false;
					boolean greaterThanSup = false;
					if (inf != null && calc.push(NEVal).le(inf).pop().surelyTrue()) {
						if (calc.push(NEVal).eq(inf).pop().surelyTrue()) {
							infIncluded = false;
							continue;
						} else {
							lessThanInf = true;
						}
					} else if (sup != null && calc.push(NEVal).ge(sup).pop().surelyTrue()) {
						if (calc.push(NEVal).eq(sup).pop().surelyTrue()) {
							supIncluded = false;
							continue;
						} else {
							greaterThanSup = true;
						}
					} 
					boolean inSeq = false;
					if (currentSeq != null && prevNEVal != null) {
						Primitive delta = calc.push(NEVal).sub(prevNEVal).pop();
						inSeq = calc.push(delta).mul(delta).eq(calc.pushVal(1).pop()).pop().surelyTrue();
					}
					if (inSeq) {
						currentSeq = calc.push(currentSeq).and(calc.push(var).ne(NEVal).pop()).pop();
					} else {
						//add current seq if any
						if (currentSeq != null) {
							if (currentSeqIsLessThanInf) {
								assumptions.add(0, new ClauseAssume[] {condOnInf, new ClauseAssume(currentSeq)});
								condOnInfAlreadyIncluded = true;
								currentSeqIsLessThanInf = false;
							} else if (currentSeqIsGreaterThanSup) {
								assumptions.add(0, new ClauseAssume[] {condOnSup, new ClauseAssume(currentSeq)});
								condOnSupAlreadyIncluded = true;
								currentSeqIsGreaterThanSup = false;
							} else {
								if (!naturalVars) {
									//Size-wise-range optimization: removing in range NE constraints likely due to scanning
									assumptions.add(new ClauseAssume[] {new ClauseAssume(currentSeq)});
								}
							}
						}
						//start collecting the next seq
						currentSeqIsLessThanInf = lessThanInf;
						currentSeqIsGreaterThanSup = greaterThanSup;
						currentSeq = calc.push(var).ne(NEVal).pop();
					}
				} catch (InvalidOperandException | InvalidTypeException e) {
					e.printStackTrace();
				} catch (InvalidInputException e) {
					e.printStackTrace();
				}
				prevNEVal = NEVal;
			} 

			//handle last seq if any
			if (currentSeq != null) {
				try {
					if (currentSeqIsLessThanInf) {
						assumptions.add(0, new ClauseAssume[] {condOnInf, new ClauseAssume(currentSeq)});
						condOnInfAlreadyIncluded = true;
					} else if (currentSeqIsGreaterThanSup) {
						assumptions.add(0, new ClauseAssume[] {condOnSup, new ClauseAssume(currentSeq)});
						condOnSupAlreadyIncluded = true;
					} else {
						if (!naturalVars) {
							assumptions.add(new ClauseAssume[] {new ClauseAssume(currentSeq)});
						}
					}
				} catch (InvalidInputException e) {
					e.printStackTrace();
				}
			}
			//include conds on inf and sup if not already included along with NEValues
			if (condOnInf != null && !condOnInfAlreadyIncluded) {
				assumptions.add(0, new ClauseAssume[] {condOnInf});
			} 
			if (condOnSup != null && !condOnSupAlreadyIncluded) {
				assumptions.add(0, new ClauseAssume[] {condOnSup});
			} 
			
			if (assumptions.isEmpty() && EQValue != null) {
				try {
					assumptions.add(0, new ClauseAssume[] {new ClauseAssume(calc.push(var).eq(EQValue).pop())});				
				} catch (InvalidOperandException | InvalidTypeException e) {
					e.printStackTrace();
				} catch (InvalidInputException e) {
					e.printStackTrace();
				}
			}
			System.out.println("--DEBUG: computed range-assumptions for var " + var + ": " + Arrays.toString(assumptions.toArray()));
				
			return assumptions;
		}

		public void addAssumption(Expression rangeAssumption, boolean constTermInRightOperand, Operator op) {
			System.out.println("--DEBUG: updating range for " + var + ": " + rangeAssumption + "...");
		
			Simplex constTerm = (Simplex) (constTermInRightOperand ? rangeAssumption.getSecondOperand() : rangeAssumption.getFirstOperand());
		
			if (op.equals(Operator.EQ)) {
				try {//Defensive check
					if (EQValue != null && calc.push(EQValue).eq(constTerm).pop().surelyFalse()) {
						throw new RuntimeException("CHECK THIS: path include strict equality constraints of a variable and distinct constants");
					}
				} catch (InvalidOperandException | InvalidTypeException e) {
					e.printStackTrace();
				}
				EQValue = constTerm;
				System.out.println("  ... stored as EQ assumption, currently: " + EQValue);
				return;
			}

			if (op.equals(Operator.NE)) {
				NEValues.add(constTerm);
				System.out.println("  ... stored as NE assumption, currently: " + Arrays.toString(NEValues.toArray()));
				return;
			}
			
			boolean operatorWithEquality = op.equals(Operator.LE) || op.equals(Operator.GE);
			
			boolean updateInf = 
					(constTermInRightOperand && 
							(op.equals(Operator.GT) || op.equals(Operator.GE))) || 
					(!constTermInRightOperand && 
							(op.equals(Operator.LT) || op.equals(Operator.LE)));
			if (updateInf) {				
				try {
					System.out.println("  ... going to update inf - before was: " + inf + (infIncluded?" (extreme included)":" (extreme excluded)"));
					if (inf == null || calc.push(constTerm).lt(inf).pop().surelyTrue()) {
						inf = constTerm;
						infIncluded = operatorWithEquality;
					} else if (calc.push(constTerm).eq(inf).pop().surelyTrue()) {
						infIncluded = infIncluded || operatorWithEquality;
					}
					System.out.println("  ... - now is: " + inf + (infIncluded?" (extreme included)":" (extreme excluded)"));
				} catch (InvalidOperandException | InvalidTypeException e) {
					e.printStackTrace();
				}
			} else { //updateSup
				try {
					System.out.println("  ... going to update sup - before was: " + sup + (supIncluded?" (extreme included)":" (extreme excluded)"));
					if (sup == null || calc.push(constTerm).gt(sup).pop().surelyTrue()) {
						sup = constTerm;
						supIncluded = operatorWithEquality;
					} else if (calc.push(constTerm).eq(sup).pop().surelyTrue()) {
						supIncluded = supIncluded || operatorWithEquality;
					}
					System.out.println("  ... - now is: " + sup + (supIncluded?" (extreme included)":" (extreme excluded)"));
				} catch (InvalidOperandException | InvalidTypeException e) {
					e.printStackTrace();
				}
				
			}
			
		}	
	}
	
	private Primitive tryToCollectAsRangeAssumptions(Primitive cond, Map<String, NumericRange> rangeAssumptions, boolean negated) {
		if (cond instanceof Simplex) { //true or false
			return null;
		}
		if (!(cond instanceof Expression)) {
			return cond;
		}
		Expression condExpr = (Expression) cond;
		Operator op = condExpr.getOperator();

		if (op == Operator.NOT) {
			return tryToCollectAsRangeAssumptions(condExpr.getSecondOperand(), rangeAssumptions, !negated);
		} 
		
		if (condExpr.isUnary()) { 
			return condExpr;
		}

		Primitive o1 = condExpr.getFirstOperand();
		Primitive o2 = condExpr.getSecondOperand();		
		if (op == Operator.AND || op == Operator.OR){
			try {
				o1 = tryToCollectAsRangeAssumptions(o1, rangeAssumptions, negated);
				o2 = tryToCollectAsRangeAssumptions(o2, rangeAssumptions, negated);
				if (o1 != null && negated) {
					o1 = Expression.makeExpressionUnary(op, o1);
				}
				if (o2 != null && negated) {
					o2 = Expression.makeExpressionUnary(op, o2);
				}
				if (o1 == null) {
					if (o2 == null) {
						return null;
					} else {
						return o2;
					}
				} else if (o2 == null) {
					return o1;
				} else {
					return Expression.makeExpressionBinary(o1, op, o2);
				}
			} catch (InvalidOperatorException | InvalidOperandException | InvalidTypeException e) {
				return condExpr; //work as condExpr cannot be handled (though this should not happen)
			}
		}
		
		boolean isNumericComparison = 
				op == Operator.EQ || op == Operator.NE || op == Operator.GE || op == Operator.GT || 
				op == Operator.LE || op == Operator.LT;
		boolean isSimpleRangeAssumption = isNumericComparison && (o1 instanceof Simplex || o2 instanceof Simplex) &&
				(o1 instanceof PrimitiveSymbolic || o2 instanceof PrimitiveSymbolic);
		if (!isSimpleRangeAssumption) {
			return condExpr;
		}
		
		//handle as a range expression
		Primitive var = o1 instanceof Simplex ? o2 : o1;
		String origin = var.toString();
		NumericRange range = rangeAssumptions.get(origin);
		if (range == null) {
			range = new NumericRange((PrimitiveSymbolic) var, calc);
			rangeAssumptions.put(origin, range);
		}
		if (negated) {
			if (op == Operator.EQ) {
				op = Operator.NE;
			} else if (op == Operator.NE) {
				op = Operator.EQ;
			} else if (op == Operator.GE) {
				op = Operator.LT;
			} else if (op == Operator.GT) {
				op = Operator.LE;
			} else if (op == Operator.LE) {
				op = Operator.GT;
			} else if (op == Operator.LT) {
				op = Operator.GE;
			}
		}
		range.addAssumption(condExpr, o2 instanceof Simplex, op);
		//System.out.println("Range clause: " + assumption);
		return null;
	}

	public void formatPrologue(int idSuffix) {
		this.output.append("package " + packagePath + ";\n\n");
		this.output.append(PROLOGUE_1);
		this.output.append('_');
		this.output.append(this.methodNumber);
		this.output.append('_');
		this.output.append(idSuffix);
		String id = "_" + this.methodNumber + "_" + idSuffix;
		if (this.traceCounterSupplier != null) {
			this.output.append('_');
			this.output.append(this.traceCounterSupplier.get());
			id += "_" + this.traceCounterSupplier.get();
		}
		this.output.append(" implements IApcEvaluator");
		this.output.append(PROLOGUE_2);
		//declare the literal strings
        for (Map.Entry<Long, String> lit : this.stringLiterals.entrySet()) {
            this.output.append(INDENT_1);
            this.output.append("private static final String CONST_");
            this.output.append(lit.getKey());
            this.output.append(" = \"");
            this.output.append(escapedString(lit.getValue()));
            this.output.append("\";\n");
        }
        
        this.output.append(INDENT_1 + "public APCFitnessEvaluator");
        this.output.append(id); 
        this.output.append("(ClassLoader classLoader) {\n"); 
        this.output.append(INDENT_2 + "this.classLoader = classLoader;\n");
        for (long heapPos : new TreeSet<Long>(stringLiterals.keySet())) {
            this.output.append(INDENT_2);
            this.output.append("this.constants.put(");
            this.output.append(heapPos);
            this.output.append("L, CONST_");
            this.output.append(heapPos);
            this.output.append(");\n");
        }
		this.output.append(INDENT_1 + "}\n");

	}

	private String escapedString(String s) {
		return s.replace("\\", "\\\\").replace("\"", "\\\"");
	}

	public void formatStringLiterals(Set<String> stringLiterals) {
		int i = 0;
		for (String lit : stringLiterals) {
			this.output.append("    private static final String STRING_LITERAL_");
			this.output.append(i);
			this.output.append(" = \"");
			this.output.append(lit);
			this.output.append("\";\n");
			++i;
		}
		this.output.append("\n");
	}
	public void formatState(State state) {
		
	}
	public void formatState(State state, HashMap<Clause, String> clauseLocations, String evaluatorDependencySpec) {
		try {
			MethodUnderTest m = new MethodUnderTest(this.output, this.initialStateSupplier.get(), state, this.testCounter, subformula, clauseLocations, calc, stringLiterals, evaluatorDependencySpec);
			this.output.append(INDENT_1);
			this.output.append("public final boolean[] disabled = new boolean[");
			this.output.append(m.numOfAppendedEvaluators);
			this.output.append("];\n");
			this.output.append(INDENT_1);
			this.output.append("public final boolean[] subsumed = new boolean[");
			this.output.append(m.numOfAppendedEvaluators);
			this.output.append("];\n");
			this.output.append(INDENT_1);
			this.output.append("public final double[] minDistance = new double[");
			this.output.append(m.numOfAppendedEvaluators + 1);
			this.output.append("];\n");
			this.output.append(INDENT_1);
			this.output.append("public final int[] convergingObs = new int[");
			this.output.append(m.numOfAppendedEvaluators);
			this.output.append("];\n");
			this.output.append(INDENT_1);
			this.output.append("public final int[] alignedSpinObs = new int[");
			this.output.append(m.numOfAppendedEvaluators);
			this.output.append("];\n");
			this.output.append(INDENT_1);
			this.output.append("public final int[] reverseSpinObs = new int[");
			this.output.append(m.numOfAppendedEvaluators);
			this.output.append("];\n");
		} catch (FrozenStateException e) {
			this.output.delete(0, this.output.length());
		}
		++this.testCounter;
	}

	@Override
	public void formatEpilogue() {
		this.output.append(EPILOGUE_1);
		this.output.append("}\n");
	}

	@Override
	public String emit() {
		return this.output.toString();
	}

	@Override
	public void cleanup() {
		this.output = new StringBuilder();
		this.testCounter = 0;
	}

	private static final String INDENT_1 = "    ";
	private static final String INDENT_2 = INDENT_1 + INDENT_1;
	private static final String INDENT_3 = INDENT_1 + INDENT_2;
	private static final String INDENT_4 = INDENT_1 + INDENT_3;
	private static final String INDENT_5 = INDENT_1 + INDENT_4;
	private static final String INDENT_6 = INDENT_1 + INDENT_5;
	private static final String PROLOGUE_1 =
			"import static sushi.compile.path_condition_distance.DistanceBySimilarityWithPathCondition.distance;\n" +
			"\n" +
			"import static java.lang.Double.*;\n" +
			"import static java.lang.Math.*;\n" +
			"\n" +
			"import sushi.compile.path_condition_distance.*;\n" +
			"import sushi.logging.Level;\n" +
			"import sushi.logging.Logger;\n" +
			"import " + IApcEvaluator.class.getName() + ";\n" +
			"import java.util.ArrayList;\n" +
			"import java.util.HashMap;\n" +
			"import java.util.List;\n" +
			"import java.util.Arrays;\n" +
			"\n" +
			"public class APCFitnessEvaluator";
	private static final String PROLOGUE_2 = 
			" {\n" +
			INDENT_1 + "private static final double SMALL_DISTANCE = 1;\n" +
			INDENT_1 + "private static final double BIG_DISTANCE = 1E300;\n" +
			"\n" +
			INDENT_1 + "private final SushiLibCache parsedOrigins = new SushiLibCache();\n" +	//SushiLibCache()
			INDENT_1 + "private final HashMap<Long, String> constants = new HashMap<>();\n" +	//constants
			INDENT_1 + "private final ClassLoader classLoader;\n" + 
			INDENT_1 + "private ArrayList<ClauseSimilarityHandler> pathConditionHandler = null;\n" +
			INDENT_1 + "private ArrayList<String> pathConditionHandlerSrc = null;\n" +
			INDENT_1 + "private ArrayList<ClauseSimilarityHandler> preConditionHandler = null;\n" +
			INDENT_1 + "private String dependencyEvaluatorClassName = null;\n" + 
			INDENT_1 + "private int[] dependencyEvaluatorClauseId = null;\n" + 
			INDENT_1 + "private ArrayList<String> seenLocations = new ArrayList<>();\n" + 
			INDENT_1 + "private HashMap<Long, double[]> threadLocalDistanceSample = new HashMap<>();\n" + 
			"\n"; 
	private static final String EPILOGUE_1 = 
			INDENT_1 + "private void initSubsumed() {\n" + 
			INDENT_2 + "Arrays.fill(subsumed, true);\n" +
			INDENT_2 + "Arrays.fill(minDistance, Double.MAX_VALUE);\n" + 
			INDENT_2 + "Arrays.fill(alignedSpinObs, -1);\n" +
			INDENT_2 + "Arrays.fill(reverseSpinObs, -1);\n" +
			INDENT_1 + "}\n" + 
			INDENT_1 + "public int[] getConverging() {\n" + 
			INDENT_2 + "int count = 0;\n" + 
			INDENT_2 + "for (int i = 0; i < convergingObs.length; ++i) {\n" + 
			INDENT_3 + "if (!disabled[i] && !subsumed[i] && convergingObs[i] > 0) ++count;\n" + 
			INDENT_2 + "}\n" + 
			INDENT_2 + "int ret[] = new int[count];\n" + 
			INDENT_2 + "count = 0;\n" + 
			INDENT_2 + "for (int i = 0; i < convergingObs.length; ++i) {\n" + 
			INDENT_3 + "if (!disabled[i] && !subsumed[i] && convergingObs[i] > 0) {\n" + 
			INDENT_4 + "ret[count++] = i;\n" + 
			INDENT_3 + "}\n" + 
			INDENT_2 + "}\n" + 
			INDENT_2 + "return ret;\n" + 
			INDENT_1 + "}\n" + 
			INDENT_1 + "public void processFeedback(ArrayList<Object> feedback) {\n" + 
			INDENT_2 + "if (feedback == null || feedback.isEmpty()) return;\n" + 
			INDENT_2 + "double[] distanceSample = (double[]) feedback.get(0);\n" + 
			INDENT_2 + "if (distanceSample == null || distanceSample.length != alignedSpinObs.length + 1) return;\n" + 
			INDENT_2 + "double currentDistance = distanceSample[distanceSample.length - 1];\n" + 
			INDENT_2 + "double bestDistance = minDistance[minDistance.length - 1];\n" + 
			INDENT_2 + "for (int i = 0; i < minDistance.length - 1; ++i) {\n" + 
			INDENT_3 + "if (disabled[i] || subsumed[i]) continue;\n" + 
			INDENT_3 + "if (currentDistance <= bestDistance) { //fitness is good or even better\n" + 
			INDENT_4 + "if (distanceSample[i] <= minDistance[i]) ++alignedSpinObs[i]; \n" + 
			INDENT_4 + "else if (distanceSample[i] > minDistance[i]) ++reverseSpinObs[i]; \n" + 
			INDENT_3 + "} else /*if (currentDistance >= bestDistance)*/ { //fitness is worse\n" + 
			INDENT_4 + "if (distanceSample[i] > minDistance[i]) ++alignedSpinObs[i]; \n" + 
			INDENT_4 + "else if (distanceSample[i] < minDistance[i]) ++reverseSpinObs[i]; //TODO: shall we add == ?\n" + 
			INDENT_3 + "} \n" + 
			INDENT_3 + "if (distanceSample[i] == 0) ++convergingObs[i];\n" + 
			INDENT_2 + "}\n" + 
			INDENT_2 + "if (currentDistance <= bestDistance) {\n" + 
			INDENT_3 + "// Update best values of clauses, if we observe bestDistance\n" + 
			INDENT_3 + "minDistance[minDistance.length - 1] = currentDistance;\n" + 
			INDENT_3 + "for (int i = 0; i < minDistance.length - 1; ++i) {\n" + 
			INDENT_4 + "if (disabled[i] || subsumed[i]) continue;\n" + 
			INDENT_4 + "if (distanceSample[i] < minDistance[i]) {\n" + 
			INDENT_5 + "minDistance[i] = distanceSample[i];\n" + 
			INDENT_4 + "}\n" + 
			INDENT_3 + "}\n" + 
			INDENT_2 + "}\n" + 
			INDENT_1 + "}\n" +
			INDENT_1 + "/* public void processFeedbackOld(ArrayList<Object> feedback) {\n" + 
			INDENT_2 + "if (feedback == null || feedback.isEmpty()) return;\n" + 
			INDENT_2 + "double[] distanceSample = (double[]) feedback.get(0);\n" + 
			INDENT_2 + "if (distanceSample == null || distanceSample.length != minDistanceOfHandler.length + 1) return;\n" + 
			INDENT_2 + "double totalDistance = distanceSample[distanceSample.length - 1];\n" + 
			INDENT_2 + "for (int i = 0; i < minDistanceOfHandler.length; ++i) {\n" + 
			INDENT_3 + "if (totalDistance < minDistance) {\n" + 
			INDENT_4 + "minDistanceOfHandler[i] = distanceSample[i];\n" + 
			INDENT_3 + "} else if (totalDistance > minDistance) {\n" + 
			INDENT_4 + "if (distanceSample[i] < minDistanceOfHandler[i]) ++accordanceObs[i]; \n" + 
			INDENT_4 + "else if (distanceSample[i] > minDistanceOfHandler[i]) ++disagreementObs[i]; \n" + 
			INDENT_4 + "//else: distanceSample[i] == minDistanceOfHandler[i];\n" + 
			INDENT_3 + "} //else: totalDistance == minDistance;\n" + 
			INDENT_3 + "if (distanceSample[i] == 0) ++convergingObs[i];\n" + 
			INDENT_2 + "}\n" + 
			INDENT_2 + "if (totalDistance < minDistance) minDistance = totalDistance;\n" + 
			INDENT_1 + "} */\n" + 
			INDENT_1 + "public void setDisabled(int i, boolean val) {\n" + 
			INDENT_2 + "if (i >= 0 && i < disabled.length) disabled[i] = val;\n" + 
			INDENT_1 + "}\n" +
			INDENT_1 + "public void updtDisabled(boolean[] disabled) {\n" + 
			INDENT_2 + "if (disabled == null || disabled.length != this.disabled.length) return;\n" + 
			INDENT_2 + "for (int i = 0; i < disabled.length; ++i) {\n" + 
			INDENT_3 + "this.disabled[i] =  disabled[i];\n" + 
			INDENT_2 + "}\n" + 
			INDENT_1 + "}\n" + 
			INDENT_1 + "public void disableAllButSome(int[] some) {\n" + 
			INDENT_2 + "int keep = (some.length > 0) ? some[0] : -1;\n" + 
			INDENT_2 + "int cursor = 1;\n" + 
			INDENT_2 + "for (int i = 0; i < disabled.length; ++i) {\n" + 
			INDENT_3 + "if (i != keep) {\n" + 
			INDENT_4 + "disabled[i] = true;           		\n" + 
			INDENT_3 + "} else {\n" + 
			INDENT_4 + "disabled[i] = false;\n" +
			INDENT_4 + "keep = (cursor < some.length) ? some[cursor] : -1;\n" + 
			INDENT_4 + "++cursor;\n" + 
			INDENT_3 + "}\n" + 
			INDENT_2 + "}\n" + 
			INDENT_1 + "}\n" + 
			INDENT_1 + "public boolean[] getDisabled() {\n" + 
			INDENT_2 + "return disabled;\n" + 
			INDENT_1 + "}\n" +
			INDENT_1 + "public int[] getAlignedSpinObs() {\n" + 
			INDENT_2 + "return alignedSpinObs;\n" + 
			INDENT_1 + "}\n" + 
			INDENT_1 + "public int[] getReverseSpinObs() {\n" + 
			INDENT_2 + "return reverseSpinObs;\n" + 
			INDENT_1 + "}\n" + 
			INDENT_1 + "public boolean isAllDisabled() {\n" + 
			INDENT_2 + "for (int i = 0; i < disabled.length; ++i) {\n" + 
			INDENT_3 + "if (!disabled[i]) return false;\n" + 
			INDENT_2 + "}\n" + 
			INDENT_2 + "return true;\n" + 
			INDENT_1 + "}\n" +
			INDENT_1 + "public String getSimilarityHandlerSrc(int i) {\n" + 
			INDENT_2 + "if (i >= 0 && i < pathConditionHandlerSrc.size()) \n" + 
			INDENT_3 + "return pathConditionHandlerSrc.get(i);\n" + 
			INDENT_2 + "else return \"\";\n" + 
			INDENT_1 + "}\n" +
			INDENT_1 + "public String getDependencyEvaluatorClassName() {\n" + 
			INDENT_2 + "return dependencyEvaluatorClassName;\n" + 
			INDENT_1 + "}\n" + 
			INDENT_1 + "public int[] getDependencyEvaluatorClauseId() {\n" + 
			INDENT_2 + "return dependencyEvaluatorClauseId;\n" + 
			INDENT_1 + "}\n" +
			INDENT_1 + "public String[] getSeenLocations() {\n" + 
			INDENT_2 + "return seenLocations.toArray(new String[0]);\n" + 
			INDENT_1 + "}\n";
	
	private static List<PrimitiveSymbolic> symbolsIn(Primitive e) {
		final ArrayList<PrimitiveSymbolic> symbols = new ArrayList<>();
		final PrimitiveVisitor v = new PrimitiveVisitor() {

			@Override
			public void visitWideningConversion(WideningConversion x) throws Exception {
				x.getArg().accept(this);
			}

			@Override
			public void visitTerm(Term x) throws Exception { }

			@Override
			public void visitSimplex(Simplex x) throws Exception { }

			@Override
			public void visitNarrowingConversion(NarrowingConversion x) throws Exception {
				x.getArg().accept(this);
			}

			@Override
			public void visitPrimitiveSymbolicApply(PrimitiveSymbolicApply x) throws Exception {
				if (symbols.contains(x)) {
                    return; //surely its args have been processed
                }
                symbols.add(x);
			}

			@Override
			public void visitExpression(Expression e) throws Exception {
				if (e.isUnary()) {
					e.getOperand().accept(this);
				} else {
					e.getFirstOperand().accept(this);
					e.getSecondOperand().accept(this);
				}
			}

			@Override
			public void visitAny(Any x) { }

			@Override
			public void visitPrimitiveSymbolicHashCode(PrimitiveSymbolicHashCode x) throws Exception {
				if (symbols.contains(x)) {
					return;
				}
				symbols.add(x);
			}

			@Override
			public void visitPrimitiveSymbolicLocalVariable(PrimitiveSymbolicLocalVariable x) throws Exception {
				if (symbols.contains(x)) {
					return;
				}
				symbols.add(x);
			}

			@Override
			public void visitPrimitiveSymbolicMemberArray(PrimitiveSymbolicMemberArray x) throws Exception {
				if (symbols.contains(x)) {
					return;
				}
				symbols.add(x);
			}

			@Override
			public void visitPrimitiveSymbolicMemberArrayLength(PrimitiveSymbolicMemberArrayLength x) throws Exception {
				if (symbols.contains(x)) {
					return;
				}
				symbols.add(x);
			}

			@Override
			public void visitPrimitiveSymbolicMemberField(PrimitiveSymbolicMemberField x) throws Exception {
				if (symbols.contains(x)) {
					return;
				}
				symbols.add(x);
			}
		};

		try {
			e.accept(v);
		} catch (Exception exc) {
			//this should never happen
			throw new AssertionError(exc);
		}
		return symbols;
	}
	
	private static boolean isSummation(Primitive e) {
		final boolean[] ret = new boolean[]{true};
		final PrimitiveVisitor v = new PrimitiveVisitor() {
			@Override
			public void visitWideningConversion(WideningConversion x) throws Exception { }
			@Override
			public void visitTerm(Term x) throws Exception { }
			@Override
			public void visitSimplex(Simplex x) throws Exception { }
			@Override
			public void visitNarrowingConversion(NarrowingConversion x) throws Exception { }
			@Override
			public void visitPrimitiveSymbolicApply(PrimitiveSymbolicApply x) throws Exception { }
			@Override
			public void visitExpression(Expression e) throws Exception {
				if (e.isUnary()) {
					e.getOperand().accept(this);
				} else {
					e.getFirstOperand().accept(this);
					e.getSecondOperand().accept(this);
					if (!e.getOperator().equals(Operator.ADD)) {
						ret[0] = false;
					}
				}
			}
			@Override
			public void visitAny(Any x) { }
			@Override
			public void visitPrimitiveSymbolicHashCode(PrimitiveSymbolicHashCode x) throws Exception {
			}
			@Override
			public void visitPrimitiveSymbolicLocalVariable(PrimitiveSymbolicLocalVariable x) throws Exception {
			}
			@Override
			public void visitPrimitiveSymbolicMemberArray(PrimitiveSymbolicMemberArray x) throws Exception {
			}
			@Override
			public void visitPrimitiveSymbolicMemberArrayLength(PrimitiveSymbolicMemberArrayLength x) throws Exception {
			}
			@Override
			public void visitPrimitiveSymbolicMemberField(PrimitiveSymbolicMemberField x) throws Exception {
			}
		};

		try {
			e.accept(v);
		} catch (Exception exc) {
			//this should never happen
			throw new AssertionError(exc);
		}
		return ret[0];
	}
	
	private static class MethodUnderTest {
		private final StringBuilder s;
		private final HashMap<Symbolic, String> symbolsToVariables = new HashMap<>();
        private final HashMap<String, Symbolic> variablesToSymbols = new HashMap<>();
		private final ArrayList<String> evoSuiteInputVariables = new ArrayList<>();
		private boolean panic = false;
		private final List<Clause> formula;
		private final CalculatorRewriting calc;
		private int numOfAppendedEvaluators = 0;
		
		MethodUnderTest(StringBuilder s, State initialState, State finalState, int testCounter, List<Clause> formula, HashMap<Clause, String> clauseLocations, CalculatorRewriting calc, HashMap<Long, String> stringLiterals, String evaluatorDependencySpec) 
		throws FrozenStateException {
			this.s = s;
			this.formula = formula;
			this.calc = calc;
			//makeVariables(finalState);
			appendMethodDeclaration(initialState, finalState, testCounter);
			appendPreCondition(finalState, testCounter, evaluatorDependencySpec);
			appendPathCondition(finalState, clauseLocations, testCounter);
			appendIfStatement(initialState, finalState, testCounter);
			appendMethodEnd(finalState, testCounter);
		}

		private void appendMethodDeclaration(State initialState, State finalState, int testCounter) {
			if (this.panic) {
				return;
			}

			final List<Symbolic> inputs;
			try {
				inputs = initialState.getStack().get(0).localVariables().values().stream()
						.filter((v) -> v.getValue() instanceof Symbolic)
						.map((v) -> (Symbolic) v.getValue())
						.collect(Collectors.toList());
			} catch (IndexOutOfBoundsException | FrozenStateException e) {
				throw new UnexpectedInternalException(e);
			}

			this.s.append(INDENT_1);
			this.s.append("public double test");
			this.s.append(testCounter);
			this.s.append("(");
			//boolean firstDone = false;
			for (Symbolic symbol : inputs) {
				makeVariableFor(symbol);
				final String varName = getVariableFor(symbol);
				this.evoSuiteInputVariables.add(varName);
				/*if (firstDone) {
					this.s.append(", ");
				} else {
					firstDone = true;
				}*/
                final String type = javaType(symbol);
                this.s.append(type);
                this.s.append(' ');
                this.s.append(varName);
                this.s.append(", ");
			}
			this.s.append("ArrayList<Object> feedbackSink) throws Exception {\n");
			this.s.append(INDENT_2);
			this.s.append("//generated for state ");
			this.s.append(finalState.getBranchIdentifier());
			this.s.append('[');
			this.s.append(finalState.getSequenceNumber());
			this.s.append("]\n");
			this.s.append(INDENT_2);
			this.s.append("Logger.setLevel(Level.FATAL);\n");
			this.s.append(INDENT_2);
			this.s.append("double[] distanceSample = new double[minDistance.length];\n");
			this.s.append(INDENT_2);
			this.s.append("Arrays.fill(distanceSample, Double.MAX_VALUE);\n");
			this.s.append(INDENT_2);
			this.s.append("threadLocalDistanceSample.put(Thread.currentThread().getId(), distanceSample);\n");
			this.s.append(INDENT_2);
			this.s.append("if (feedbackSink != null) feedbackSink.add(distanceSample);\n\n");
		}

		private void appendPreCondition(State finalState, int testCounter, String evaluatorDependencySpec) 
		throws FrozenStateException {
			if (this.panic) {
				return;
			}
			this.s.append(INDENT_2);
			this.s.append("if (preConditionHandler == null) {\n"); 
			this.s.append(INDENT_3);
			this.s.append("initSubsumed();\n"); 
			this.s.append(INDENT_3);
			this.s.append("preConditionHandler = new ArrayList<>();\n"); 
			this.s.append(INDENT_3);
			this.s.append("ClauseSimilarityHandler similarityHandler;\n");
			if (evaluatorDependencySpec.indexOf(':') > 0) {
				try {
					String[] precond = evaluatorDependencySpec.split(":");
					System.out.println("DEBUG: DEPENDENCY SPEC: " + evaluatorDependencySpec + "::" + Arrays.toString(precond));
					if (precond.length < 2) {
						throw new RuntimeException();
					}
					String dependencyEvaluatorClassName = precond[0];
					this.s.append(INDENT_3);
					this.s.append("dependencyEvaluatorClassName = \"");
					this.s.append(dependencyEvaluatorClassName);
					this.s.append("\";\n");
					int[] dependencyEvaluatorClauseId = new int[precond.length - 1];
					this.s.append(INDENT_3);
					this.s.append("dependencyEvaluatorClauseId = new int[] {");
					for (int i = 1; i < precond.length; ++i) {
						int n = Integer.parseInt(precond[i]);
						dependencyEvaluatorClauseId[i - 1] = n;
						this.s.append(n);
						if (i < precond.length - 1) this.s.append(", ");
					}
					this.s.append("};\n");
					appendPreConditionClauses(dependencyEvaluatorClassName, dependencyEvaluatorClauseId);
				} catch (Exception e) {
					System.out.println("ERROR WHILE PARSING EVALUATOR DEPENDENCY SPEC: " + evaluatorDependencySpec + ": " + e);
					throw new RuntimeException("ERROR WHILE PARSING EVALUATOR DEPENDENCY SPEC: " + evaluatorDependencySpec, e);
				}
			} else {
				System.out.println("THERE IS NO EVALUATOR DEPENDENCY SPEC");
			}
			this.s.append(INDENT_2);
			this.s.append("}\n"); 
			this.s.append("\n");
		}
		
		IApcEvaluator evaluator = null;
		
		private void appendPreConditionClauses(String dependencyEvaluatorClassName, int[] dependencyEvaluatorClauseId) {
			try {
				Class<?> evaluatorClass = Class.forName(dependencyEvaluatorClassName);
				System.out.println("NEW EVALUATOR DEPENDS ON: " + evaluatorClass.getName() + "::" + Arrays.toString(dependencyEvaluatorClauseId));
				evaluator = (IApcEvaluator) evaluatorClass.getConstructor(ClassLoader.class).newInstance((ClassLoader) null);
				for (Method m: evaluatorClass.getMethods()) {
					if (m.getName().equals("test0")) {
							m.invoke(evaluator, new Object[m.getParameterCount()]);
						break;
					}
				}
				this.s.append(INDENT_3);
				this.s.append("// Dependency on: "); //comment
				this.s.append(dependencyEvaluatorClassName);
				this.s.append(Arrays.toString(dependencyEvaluatorClauseId));
				this.s.append("\n");
				for (int i = 0; i < dependencyEvaluatorClauseId.length; ++i) {
					int n = dependencyEvaluatorClauseId[i];
					String handlerSrc = evaluator.getSimilarityHandlerSrc(n);
					this.s.append(handlerSrc);
					this.s.append(INDENT_3);
					this.s.append("preConditionHandler.add(similarityHandler);\n");
				}
				this.s.append(INDENT_3);
				this.s.append("// END Dependency on: "); //comment
				this.s.append(dependencyEvaluatorClassName);
				this.s.append(Arrays.toString(dependencyEvaluatorClauseId));
				this.s.append("\n");
				String nextDependencyEvaluatorClassName = evaluator.getDependencyEvaluatorClassName();
				if (nextDependencyEvaluatorClassName != null) {
					int[] nextDependencyEvaluatorClauseId = evaluator.getDependencyEvaluatorClauseId();
					appendPreConditionClauses(nextDependencyEvaluatorClassName, nextDependencyEvaluatorClauseId);
				}
			} catch (Exception e) {
				System.out.println("ERROR WHILE RETRIEVING EVALUATOR DEPENDENCY SPEC: " + dependencyEvaluatorClassName + ":" + Arrays.toString(dependencyEvaluatorClauseId) + " - " + e);
				throw new RuntimeException("ERROR WHILE RETRIEVING EVALUATOR DEPENDENCY SPEC: " + dependencyEvaluatorClassName + ":" + Arrays.toString(dependencyEvaluatorClauseId) , e);
			} 
		}

		private void appendPathCondition(State finalState, HashMap<Clause, String> clauseLocations, int testCounter) 
		throws FrozenStateException {
			if (this.panic) {
				return;
			}
			this.s.append(INDENT_2);
			this.s.append("if (pathConditionHandler == null) {\n"); 
			//final Collection<Clause> pathCondition = finalState.getPathCondition();
			final ArrayList<Clause> pathCondition = new ArrayList<>();
			
			// Select one clasue (the latest) among the clauses observed at the same bytecode context-sensitive location
			final HashSet<String> seenLocations = new HashSet<>();
			if (evaluator != null) { //...and avoid any clause that correspond to locations that were already considered in the evaluator on which this one depends
				String[] locations = evaluator.getSeenLocations();
				for (String l: locations) {
					seenLocations.add(l);
				}
			}
			for (int i = formula.size() - 1; i >= 0; --i) {
				Clause clause = formula.get(i);
				String location = clauseLocations.get(clause);
				if ( seenLocations.add(location)) {
					pathCondition.add(0, clause);
					System.out.println("KEEPING CLAUSE: " + clause + " -- collected at location = " + location);
				} else {
					System.out.println("DISCARDING CLAUSE: " + clause + " -- collected at location = " + location);
				}
			} for (String location: seenLocations) {
				this.s.append(INDENT_3);
				this.s.append("seenLocations.add(\"");
				this.s.append(location);
				this.s.append("\");\n");				
			}
			System.out.println("Considering the sub-sub-formula: " + Arrays.toString(pathCondition.toArray()));
			this.s.append(INDENT_3);
			this.s.append("pathConditionHandler = new ArrayList<>();\n"); 
			this.s.append(INDENT_3);
			this.s.append("pathConditionHandlerSrc = new ArrayList<>();\n"); 
			//this.s.append("final ArrayList<ClauseSimilarityHandler> pathConditionHandler = new ArrayList<>();\n");
			this.s.append(INDENT_3);
			this.s.append("ClauseSimilarityHandler similarityHandler;\n");
			this.s.append(INDENT_3);
			this.s.append("String similarityHandlerSrc;\n");
			for (Iterator<Clause> iterator = pathCondition.iterator(); iterator.hasNext(); ) {
				final Clause clause = iterator.next();

				if (clause instanceof ClauseAssumeExpands) {
					this.s.append(INDENT_3);
					this.s.append("// Search for inputs with fresh object: "); //comment
					this.s.append(clause.toString());
					this.s.append("\n");

					final ClauseAssumeExpands clauseExpands = (ClauseAssumeExpands) clause;
					final Symbolic symbol = clauseExpands.getReference();
					final long heapPosition = clauseExpands.getHeapPosition();
					setWithFreshObjectAnyClass(symbol, numOfAppendedEvaluators, false);//TODO: setWithFreshObject(finalState, symbol, heapPosition);
					setWithFreshObjectAnyClass(symbol, numOfAppendedEvaluators, true);//TODO: setWithFreshObject(finalState, symbol, heapPosition);
					numOfAppendedEvaluators++;
				} else if (clause instanceof ClauseAssumeNull) {					
					this.s.append(INDENT_3);
					this.s.append("// Search for inputs diverse (not null) from: "); //comment
					this.s.append(clause.toString());
					this.s.append("\n");

					final ClauseAssumeNull clauseNull = (ClauseAssumeNull) clause;
					final ReferenceSymbolic symbol = clauseNull.getReference();
					setWithFreshObjectAnyClass(symbol, numOfAppendedEvaluators, false);//TODO: setWithNotNull(symbol);
					setWithFreshObjectAnyClass(symbol, numOfAppendedEvaluators, true);
					numOfAppendedEvaluators++;
				} else if (clause instanceof ClauseAssumeAliases) {
					this.s.append(INDENT_3);
					this.s.append("// Search for inputs diverse (not alias) from: "); //comment
					this.s.append(clause.toString());
					this.s.append("\n");

					final ClauseAssumeAliases clauseAliases = (ClauseAssumeAliases) clause;
					final Symbolic symbol = clauseAliases.getReference();
					final long heapPosition = clauseAliases.getHeapPosition();
					setWithFreshObjectAnyClass(symbol, numOfAppendedEvaluators, false);//TODO: setWithNotAlias(finalState, symbol, heapPosition);
					setWithFreshObjectAnyClass(symbol, numOfAppendedEvaluators, true);//TODO: setWithNotAlias(finalState, symbol, heapPosition);
					numOfAppendedEvaluators++;
				} else if (clause instanceof ClauseAssume) {
					this.s.append(INDENT_3);
					this.s.append("// Search for inputs diverse from: "); //comment
					this.s.append(clause.toString());
					this.s.append("\n");

					final ClauseAssume clauseAssume = (ClauseAssume) clause;
					final Primitive assumption = clauseAssume.getCondition();
					//System.out.println(assumption);
					try {
						final Primitive not_assumption = calc.push(assumption).not().pop();
						setNumericAssumption(not_assumption, numOfAppendedEvaluators, false);
						setNumericAssumption(not_assumption, numOfAppendedEvaluators, true);
						//System.out.println("-->" + not_assumption);
						numOfAppendedEvaluators++;
					} catch (InvalidTypeException e) {
						throw new RuntimeException(e);
					} catch (InvalidOperandException e) {
						e.printStackTrace();
					}
				}
				this.s.append(INDENT_3);
				this.s.append("pathConditionHandler.add(similarityHandler);\n");
				this.s.append(INDENT_3);
				this.s.append("pathConditionHandlerSrc.add(similarityHandlerSrc);\n");	
			}
			this.s.append(INDENT_2);
			this.s.append("}\n"); 

			this.s.append("\n");
		}

		private void appendIfStatement(State initialState, State finalState, int testCounter) {
			if (this.panic) {
				return;
			}
			this.s.append(INDENT_2);
			this.s.append("final HashMap<String, Object> candidateObjects = new HashMap<>();\n");
			for (String inputVariable : this.evoSuiteInputVariables) {
				this.s.append(INDENT_2);
				this.s.append("candidateObjects.put(\"");
				this.s.append(getSymbolFor(inputVariable).asOriginString());
				this.s.append("\", ");
				this.s.append(inputVariable);
				this.s.append(");\n");
			}
			this.s.append('\n');
			this.s.append(INDENT_2);
			this.s.append("double d0 = distance(preConditionHandler, candidateObjects, constants, classLoader, parsedOrigins);\n");
			this.s.append(INDENT_2);
			this.s.append("if (d0 > 0.0d) {\n");
			this.s.append(INDENT_3);
			this.s.append("threadLocalDistanceSample.remove(Thread.currentThread().getId());\n");
			this.s.append(INDENT_3);
			this.s.append("if (feedbackSink != null) feedbackSink.clear();\n");
			this.s.append(INDENT_3);
			this.s.append("return BIG_DISTANCE;\n");
			this.s.append(INDENT_2);
			this.s.append("}\n\n");
			this.s.append(INDENT_2);
			this.s.append("double d = distance(pathConditionHandler, candidateObjects, constants, classLoader, parsedOrigins);\n");
			this.s.append(INDENT_2);
			this.s.append("if (d == 0.0d)\n");
			this.s.append(INDENT_3);
			this.s.append("System.out.println(\"test");
			this.s.append(testCounter);
			this.s.append(" 0 distance\");\n\n");
			this.s.append(INDENT_2);
			this.s.append("distanceSample[distanceSample.length - 1] = d;\n");
			this.s.append(INDENT_2);
			this.s.append("threadLocalDistanceSample.remove(Thread.currentThread().getId());\n\n");
			this.s.append(INDENT_2);
			this.s.append("return d;\n");
		}

		private void appendMethodEnd(State finalState, int testCounter) {
			if (this.panic) {
				this.s.delete(0, s.length());

				this.s.append(INDENT_1);
				this.s.append("//Unable to generate test case ");
				this.s.append(testCounter);
				this.s.append(" for state ");
				this.s.append(finalState.getBranchIdentifier());
				this.s.append('[');
				this.s.append(finalState.getSequenceNumber());
				this.s.append("]\n");
			} else {
				this.s.append(INDENT_1);
				this.s.append("}\n");
			}
		}

		private void setWithFreshObject(State finalState, Symbolic symbol, long heapPosition, boolean forGeneratingSourceCode) 
		throws FrozenStateException {
			final String expansionClass = javaClass(getTypeOfObjectInHeap(finalState, heapPosition), false);
			
			if (forGeneratingSourceCode) this.s.append(INDENT_4);
			if (forGeneratingSourceCode) this.s.append("\"");
			this.s.append(INDENT_3);
			this.s.append("similarityHandler = new SimilarityWithRefToFreshObject(");
			appendQuote(forGeneratingSourceCode);
			this.s.append(symbol.asOriginString());
			appendQuote(forGeneratingSourceCode);
			this.s.append(", Class.forName(");
			appendQuote(forGeneratingSourceCode);
			this.s.append(expansionClass); //TODO arrays
			appendQuote(forGeneratingSourceCode);
			this.s.append("));");
			appendEol(forGeneratingSourceCode);
			if (forGeneratingSourceCode) this.s.append("\";\n"); 
			//TODO: handle disabled, converged and subsumed	
		}
		private void setWithFreshObjectAnyClass(Symbolic symbol, int num, boolean forGeneratingSourceCode) 
		throws FrozenStateException {
			if (forGeneratingSourceCode) this.s.append(INDENT_3);
			if (forGeneratingSourceCode) this.s.append("similarityHandlerSrc = \n");
			if (forGeneratingSourceCode) this.s.append(INDENT_4);
			if (forGeneratingSourceCode) this.s.append("\"");
			this.s.append(INDENT_3);
			this.s.append("similarityHandler = new SimilarityWithRefToFreshObjectAnyClass(");
			appendQuote(forGeneratingSourceCode);
			this.s.append(symbol.asOriginString());
			appendQuote(forGeneratingSourceCode);
			this.s.append(") {");
			appendEol(forGeneratingSourceCode);
			if (forGeneratingSourceCode) this.s.append("\" + \n");

			if (forGeneratingSourceCode) this.s.append(INDENT_4);
			if (forGeneratingSourceCode) this.s.append("\"");
			this.s.append(INDENT_4);
			this.s.append("@Override");
			appendEol(forGeneratingSourceCode);
			
			if (forGeneratingSourceCode) this.s.append("\" + \n");
			
			if (forGeneratingSourceCode) this.s.append(INDENT_4);
			if (forGeneratingSourceCode) this.s.append("\"");
			this.s.append(INDENT_4);
			this.s.append("protected double evaluateSimilarity(CandidateBackbone backbone, Object referredObject) {"); 
			appendEol(forGeneratingSourceCode);
			if (forGeneratingSourceCode) this.s.append("\" + \n");
			
			if (!forGeneratingSourceCode) {
				this.s.append(INDENT_5);
				this.s.append("if (disabled[");
				this.s.append(num);
				this.s.append("]) return 1;\n"); 
			}

			if (forGeneratingSourceCode) this.s.append(INDENT_4);
			if (forGeneratingSourceCode) this.s.append("\"");
			this.s.append(INDENT_5);
			this.s.append("double similarity = super.evaluateSimilarity(backbone, referredObject);");
			appendEol(forGeneratingSourceCode);
			if (forGeneratingSourceCode) this.s.append("\" + \n");
			
			if (!forGeneratingSourceCode) {
				this.s.append(INDENT_5);
				this.s.append("if (similarity != 1) subsumed["); 
				this.s.append(num);
				this.s.append("] = false;\n"); 
				this.s.append(INDENT_5);
				this.s.append("threadLocalDistanceSample.get(Thread.currentThread().getId())["); 
				this.s.append(num);
				this.s.append("] = 1 - similarity;\n"); 
			}

			if (forGeneratingSourceCode) this.s.append(INDENT_4);
			if (forGeneratingSourceCode) this.s.append("\"");
			this.s.append(INDENT_5);
			this.s.append("return similarity;");
			appendEol(forGeneratingSourceCode);
			if (forGeneratingSourceCode) this.s.append("\" + \n");
			
			if (forGeneratingSourceCode) this.s.append(INDENT_4);
			if (forGeneratingSourceCode) this.s.append("\"");
			this.s.append(INDENT_4);
			this.s.append("}");			
			appendEol(forGeneratingSourceCode);
			if (forGeneratingSourceCode) this.s.append("\" + \n");
			
			if (forGeneratingSourceCode) this.s.append(INDENT_4);
			if (forGeneratingSourceCode) this.s.append("\"");
			this.s.append(INDENT_3);
			this.s.append("};");
			appendEol(forGeneratingSourceCode);
			if (forGeneratingSourceCode) this.s.append("\";\n");		
		}
		
		private void appendQuote(boolean forGeneratingSourceCode) {
			if (forGeneratingSourceCode) {
				this.s.append("\\\"");
			} else {
				this.s.append("\"");
			}
		}
		private void appendEol(boolean forGeneratingSourceCode) {
			if (forGeneratingSourceCode) {
				this.s.append("\\n");
			} else {
				this.s.append("\n");
			}
		}

		private void setWithNotNull(ReferenceSymbolic symbol, boolean forGeneratingSourceCode) {
			if (forGeneratingSourceCode) this.s.append(INDENT_3);
			if (forGeneratingSourceCode) this.s.append("similarityHandlerSrc = \n");
			if (forGeneratingSourceCode) this.s.append(INDENT_4);
			if (forGeneratingSourceCode) this.s.append("\"");
			this.s.append(INDENT_3);
			this.s.append("similarityHandler = new SimilarityWithRefNotNull(");
			appendQuote(forGeneratingSourceCode);
			this.s.append(symbol.asOriginString());
			appendQuote(forGeneratingSourceCode);
			this.s.append(");");
			appendEol(forGeneratingSourceCode);
			if (forGeneratingSourceCode) this.s.append("\";\n");			
			//TODO: handle disabled, converged and subsumed
		}

		private void setWithNotAlias(State finalState, Symbolic symbol, long heapPosition, boolean forGeneratingSourceCode) {
			final String target = getOriginOfObjectInHeap(finalState, heapPosition);
			if (forGeneratingSourceCode) this.s.append(INDENT_3);
			if (forGeneratingSourceCode) this.s.append("similarityHandlerSrc = \n");
			if (forGeneratingSourceCode) this.s.append(INDENT_4);
			if (forGeneratingSourceCode) this.s.append("\"");
			this.s.append(INDENT_3);
			this.s.append("similarityHandler = new SimilarityWithRefNotAlias(");
			appendQuote(forGeneratingSourceCode);
			this.s.append(symbol.asOriginString());
			appendQuote(forGeneratingSourceCode);
			this.s.append(", ");
			appendQuote(forGeneratingSourceCode);
			this.s.append(target);
			appendQuote(forGeneratingSourceCode);
			this.s.append(");");
			appendEol(forGeneratingSourceCode);
			if (forGeneratingSourceCode) this.s.append("\";\n");
			//TODO: handle disabled, converged and subsumed
		}

        private String javaType(Symbolic symbol) {
            if (symbol instanceof Primitive) { //either PrimitiveSymbolic or Term (however, it should never be the case of a Term)
                final char type = ((Primitive) symbol).getType();
                return javaPrimitiveType(type);
            } else if (symbol instanceof ReferenceSymbolic) {
                final String className = javaClass(((ReferenceSymbolic) symbol).getStaticType(), true);
                return className;
            } else {
                //this should never happen
                throw new RuntimeException("Reached unreachable branch while calculating the Java type of a symbol: Perhaps some type of symbol is not handled yet.");
            }
        }

        private String javaPrimitiveType(char type) {
			if (type == Type.BOOLEAN) {
				return "boolean";
			} else if (type == Type.BYTE) {
				return "byte";
			} else if (type == Type.CHAR) {
				return "char";
			} else if (type == Type.DOUBLE) {
				return "double";
			} else if (type == Type.FLOAT) {
				return "float";
			} else if (type == Type.INT) {
				return "int";
			} else if (type == Type.LONG) {
				return "long";
			} else if (type == Type.SHORT) {
				return "short";
			} else {
				return null;
			}
		}

		private String javaClass(String type, boolean forDeclaration) {
			if (type == null) {
				return null;
			}
			final String a = type.replace('/', '.');
			final String s = (forDeclaration ? a.replace('$', '.') : a);

			if (forDeclaration) {
				final char[] tmp = s.toCharArray();
				int arrayNestingLevel = 0;
				boolean hasReference = false;
				int start = 0;
				for (int i = 0; i < tmp.length ; ++i) {
					if (tmp[i] == '[') {
						++arrayNestingLevel;
					} else if (tmp[i] == 'L') {
						hasReference = true;
					} else {
						start = i;
						break;
					}
				}
				final String t = hasReference ? s.substring(start, tmp.length - 1) : javaPrimitiveType(s.charAt(start));
				final StringBuilder retVal = new StringBuilder(t);
				for (int k = 1; k <= arrayNestingLevel; ++k) {
					retVal.append("[]");
				}
				return retVal.toString();
			} else {
				return (isReference(s) ? className(s) : s);
			}
		}

        private int varCounter = 0;
		private String generateVarNameFromOrigin(String name) {
            return "V" + this.varCounter++;
		}

		private String generateOriginFromVarName(String name) {
			return name.replace("__ROOT_", "{ROOT}:");
		}

		private void makeVariableFor(Symbolic symbol) {
			if (!this.symbolsToVariables.containsKey(symbol)) {
				final String origin = symbol.asOriginString();
				final String varName = generateVarNameFromOrigin(origin);
				this.symbolsToVariables.put(symbol, varName);
				this.variablesToSymbols.put(varName, symbol);
			}
		}

		private String getVariableFor(Symbolic symbol) {
			return this.symbolsToVariables.get(symbol);
		}

        private Symbolic getSymbolFor(String varName) {
            return this.variablesToSymbols.get(varName);
        }

		private static String getTypeOfObjectInHeap(State finalState, long num) throws FrozenStateException {
			final Map<Long, Objekt> heap = finalState.getHeap();
			final Objekt o = heap.get(num);
			return o.getType().getClassName();
		}

		private String getOriginOfObjectInHeap(State finalState, long heapPos){
			final Collection<Clause> path = finalState.getPathCondition();
			for (Clause clause : path) {
				if (clause instanceof ClauseAssumeExpands) { // == Obj fresh
					final ClauseAssumeExpands clauseExpands = (ClauseAssumeExpands) clause;
					final long heapPosCurrent = clauseExpands.getHeapPosition();
					if (heapPosCurrent == heapPos) {
						return generateOriginFromVarName(getVariableFor(clauseExpands.getReference()));
					}
				}
			}
			return null;
		}

		private void setNumericAssumption(Primitive assumption, int num, boolean forGeneratingSourceCode) {
			final List<PrimitiveSymbolic> symbols = symbolsIn(assumption);			
			
			if (forGeneratingSourceCode) this.s.append(INDENT_3);
			if (forGeneratingSourceCode) this.s.append("similarityHandlerSrc = \n");
			if (forGeneratingSourceCode) this.s.append(INDENT_4);
			if (forGeneratingSourceCode) this.s.append("\"");
			this.s.append(INDENT_3);
			this.s.append("similarityHandler = new SimilarityWithNumericExpression(new ValueCalculator() {");
			appendEol(forGeneratingSourceCode);
			if (forGeneratingSourceCode) this.s.append("\" + \n");

			if (forGeneratingSourceCode) this.s.append(INDENT_4);
			if (forGeneratingSourceCode) this.s.append("\"");
			this.s.append(INDENT_4);
			this.s.append("@Override public Iterable<String> getVariableOrigins() {");
			appendEol(forGeneratingSourceCode);
			if (forGeneratingSourceCode) this.s.append("\" + \n");
			
			if (forGeneratingSourceCode) this.s.append(INDENT_4);
			if (forGeneratingSourceCode) this.s.append("\"");
			this.s.append(INDENT_5);
			this.s.append("ArrayList<String> retVal = new ArrayList<>();");       
			appendEol(forGeneratingSourceCode);
			if (forGeneratingSourceCode) this.s.append("\" + \n");
			
			for (PrimitiveSymbolic symbol: symbols) {
				if (forGeneratingSourceCode) this.s.append(INDENT_4);
				if (forGeneratingSourceCode) this.s.append("\"");
				this.s.append(INDENT_5);
				this.s.append("retVal.add(");
				appendQuote(forGeneratingSourceCode);
				this.s.append(symbol.asOriginString());
				appendQuote(forGeneratingSourceCode);
				this.s.append(");");
				appendEol(forGeneratingSourceCode);
				if (forGeneratingSourceCode) this.s.append("\" + \n");
			}

			if (forGeneratingSourceCode) this.s.append(INDENT_4);
			if (forGeneratingSourceCode) this.s.append("\"");
			this.s.append(INDENT_5);
			this.s.append("return retVal;");  
			appendEol(forGeneratingSourceCode);
			if (forGeneratingSourceCode) this.s.append("\" + \n");
			
			if (forGeneratingSourceCode) this.s.append(INDENT_4);
			if (forGeneratingSourceCode) this.s.append("\"");
			this.s.append(INDENT_4);
			this.s.append("}");       
			appendEol(forGeneratingSourceCode);
			if (forGeneratingSourceCode) this.s.append("\" + \n");
			
			if (forGeneratingSourceCode) this.s.append(INDENT_4);
			if (forGeneratingSourceCode) this.s.append("\"");
			this.s.append(INDENT_4);
			this.s.append("@Override public double calculate(List<Object> variables) {");
			appendEol(forGeneratingSourceCode);
			if (forGeneratingSourceCode) this.s.append("\" + \n");
			
			if (!forGeneratingSourceCode) {
				this.s.append(INDENT_5);
				this.s.append("if (disabled[");
				this.s.append(num);
				this.s.append("]) return 0;\n"); 
			}
			for (int i = 0; i < symbols.size(); ++i) {
				final Symbolic symbol = symbols.get(i);
				makeVariableFor(symbol);
				if (forGeneratingSourceCode) this.s.append(INDENT_4);
				if (forGeneratingSourceCode) this.s.append("\"");
				this.s.append(INDENT_5);
				this.s.append("final ");
				this.s.append(javaType(symbol));
				this.s.append(" ");
				this.s.append(getVariableFor(symbol));
				this.s.append(" = (");
				this.s.append(javaType(symbol));
				this.s.append(") variables.get(");
				this.s.append(i);
				this.s.append(");");
				appendEol(forGeneratingSourceCode);
				if (forGeneratingSourceCode) this.s.append("\" + \n");
			}
			
			if (forGeneratingSourceCode) this.s.append(INDENT_4);
			if (forGeneratingSourceCode) this.s.append("\"");
			this.s.append(INDENT_5);
			this.s.append("double retVal = ");
			this.s.append(javaAssumptionCheck(assumption));
			this.s.append(";");
			appendEol(forGeneratingSourceCode);
			if (forGeneratingSourceCode) this.s.append("\" + \n");

			if (!forGeneratingSourceCode) {
				this.s.append(INDENT_5);
				this.s.append("if (retVal != 0) subsumed["); 
				this.s.append(num);
				this.s.append("] = false;\n"); 
				this.s.append(INDENT_5);
				this.s.append("threadLocalDistanceSample.get(Thread.currentThread().getId())["); 
				this.s.append(num);
				this.s.append("] = retVal;\n"); 
			}

			if (forGeneratingSourceCode) this.s.append(INDENT_4);
			if (forGeneratingSourceCode) this.s.append("\"");
			this.s.append(INDENT_5);
			this.s.append("return retVal;");
			appendEol(forGeneratingSourceCode);
			if (forGeneratingSourceCode) this.s.append("\" + \n");

			if (forGeneratingSourceCode) this.s.append(INDENT_4);
			if (forGeneratingSourceCode) this.s.append("\"");
			this.s.append(INDENT_4);
			this.s.append("}");
			appendEol(forGeneratingSourceCode);
			if (forGeneratingSourceCode) this.s.append("\" + \n");
			
			if (forGeneratingSourceCode) this.s.append(INDENT_4);
			if (forGeneratingSourceCode) this.s.append("\"");
			this.s.append(INDENT_3);
			this.s.append("});");
			appendEol(forGeneratingSourceCode);
			if (forGeneratingSourceCode) this.s.append("\";\n");
		}

		

		private String javaVariable(PrimitiveSymbolic symbol) {
			return symbol.toString().replaceAll("[\\{\\}]", "");
		}

		private Operator dual(Operator op) {
			switch (op) {
			case AND:
				return Operator.OR;
			case OR:
				return Operator.AND;
			case GT:
				return Operator.LE;
			case GE:
				return Operator.LT;
			case LT:
				return Operator.GE;
			case LE:
				return Operator.GT;
			case EQ:
				return Operator.NE;
			case NE:
				return Operator.EQ;
			default:
				return null;
			}
		}

		private String javaAssumptionCheck(Primitive assumption) {
			//first pass: Eliminate negation
			final ArrayList<Primitive> assumptionWithNoNegation = new ArrayList<Primitive>(); //we use only one element as it were a reference to a String variable            
			final PrimitiveVisitor negationEliminator = new PrimitiveVisitor() {

				@Override
				public void visitAny(Any x) throws Exception {
					assumptionWithNoNegation.add(x);
				}

				@Override
				public void visitExpression(Expression e) throws Exception {
					if (e.getOperator().equals(Operator.NOT)) {
						final Primitive operand = e.getOperand();
						if (operand instanceof Simplex) {
							//true or false
							assumptionWithNoNegation.add(calc.push(operand).not().pop());
						} else if (operand instanceof Expression) {
							final Expression operandExp = (Expression) operand;
							final Operator operator = operandExp.getOperator();
							if (operator.equals(Operator.NOT)) {
								//double negation
								operandExp.getOperand().accept(this);
							} else if (operator.equals(Operator.AND) || operator.equals(Operator.OR)) {
								calc.push(operandExp.getFirstOperand()).not().pop().accept(this);
								final Primitive first = assumptionWithNoNegation.remove(0);
								calc.push(operandExp.getSecondOperand()).not().pop().accept(this);
								final Primitive second = assumptionWithNoNegation.remove(0);
								assumptionWithNoNegation.add(Expression.makeExpressionBinary(first, dual(operator), second));
							} else if (operator.equals(Operator.GT) || operator.equals(Operator.GE) ||
									operator.equals(Operator.LT) || operator.equals(Operator.LE) ||
									operator.equals(Operator.EQ) || operator.equals(Operator.NE)) {
								assumptionWithNoNegation.add(Expression.makeExpressionBinary(operandExp.getFirstOperand(), dual(operator), operandExp.getSecondOperand()));
							} else {
								//can't do anything for this expression
								assumptionWithNoNegation.add(e);
							}
						} else {
							//can't do anything for this expression
							assumptionWithNoNegation.add(e);
						}
					} else if (e.isUnary()) {
						//in this case the operator can only be NEG
						assumptionWithNoNegation.add(e);
					} else {
						//binary operator
						final Operator operator = e.getOperator();
						e.getFirstOperand().accept(this);
						final Primitive first = assumptionWithNoNegation.remove(0);
						e.getSecondOperand().accept(this);
						final Primitive second = assumptionWithNoNegation.remove(0);
						assumptionWithNoNegation.add(Expression.makeExpressionBinary(first, operator, second));
					}
				}

				@Override
				public void visitPrimitiveSymbolicApply(PrimitiveSymbolicApply x) throws Exception {
					final ArrayList<Value> newArgs = new ArrayList<>(); 
					for (Value arg : x.getArgs()) {
						if (arg instanceof Primitive) {
							((Primitive) arg).accept(this);
							newArgs.add(assumptionWithNoNegation.remove(0));
						} else {
							newArgs.add(arg);
						}
					}
					assumptionWithNoNegation.add(new PrimitiveSymbolicApply(x.getType(), x.historyPoint(), x.getOperator(), newArgs.toArray(new Value[0])));
				}

				@Override
				public void visitSimplex(Simplex x) throws Exception {
					assumptionWithNoNegation.add(x);
				}

				@Override
				public void visitTerm(Term x) throws Exception {
					assumptionWithNoNegation.add(x);
				}

				@Override
				public void visitNarrowingConversion(NarrowingConversion x) throws Exception {
					assumptionWithNoNegation.add(x);
				}

				@Override
				public void visitWideningConversion(WideningConversion x) throws Exception {
					assumptionWithNoNegation.add(x);
				}

				@Override
				public void visitPrimitiveSymbolicHashCode(PrimitiveSymbolicHashCode x) throws Exception {
					assumptionWithNoNegation.add(x);
				}

				@Override
				public void visitPrimitiveSymbolicLocalVariable(PrimitiveSymbolicLocalVariable x) throws Exception {
					assumptionWithNoNegation.add(x);
				}

				@Override
				public void visitPrimitiveSymbolicMemberArray(PrimitiveSymbolicMemberArray x) throws Exception {
					assumptionWithNoNegation.add(x);
				}

				@Override
				public void visitPrimitiveSymbolicMemberArrayLength(PrimitiveSymbolicMemberArrayLength x)
						throws Exception {
					assumptionWithNoNegation.add(x);
				}

				@Override
				public void visitPrimitiveSymbolicMemberField(PrimitiveSymbolicMemberField x) throws Exception {
					assumptionWithNoNegation.add(x);
				}

			};
			try {
				assumption.accept(negationEliminator);
			} catch (Exception exc) {
				//this may happen if Any appears in assumption
				throw new RuntimeException(exc);
			}

			//second pass: translate
			final ArrayList<String> translation = new ArrayList<String>(); //we use only one element as it were a reference to a String variable            
			final PrimitiveVisitor translator = new PrimitiveVisitor() {

				@Override
				public void visitWideningConversion(WideningConversion x) throws Exception {
					x.getArg().accept(this);
					final char argType = x.getArg().getType();
					final char type = x.getType();
					if (argType == Type.BOOLEAN && type == Type.INT) {
						//operand stack widening of booleans
						final String arg = translation.remove(0);
						translation.add("((" + arg + ") == false ? 0 : 1)");
					}
				}

				@Override
				public void visitTerm(Term x) {
					translation.add(x.toString());
				}

				@Override
				public void visitSimplex(Simplex x) {
					String literalTypeDiscriminator;
					if (x.getType() == Type.LONG) {
						literalTypeDiscriminator = "L";
					} else if (x.getType() == Type.FLOAT) {
						literalTypeDiscriminator = "F";						
					} else if (x.getType() == Type.DOUBLE) {
						literalTypeDiscriminator = "D";
					} else {
						literalTypeDiscriminator = "";
					}
					translation.add(x.getActualValue().toString() + literalTypeDiscriminator);
				}

				@Override
				public void visitNarrowingConversion(NarrowingConversion x)
				throws Exception {
					x.getArg().accept(this);
					final String arg = translation.remove(0);
					final StringBuilder b = new StringBuilder();
					b.append("(");
					b.append(javaPrimitiveType(x.getType()));
					b.append(") (");
					b.append(arg);
					b.append(")");
					translation.add(b.toString());
				}

				@Override
				public void visitPrimitiveSymbolicApply(PrimitiveSymbolicApply x)
				throws Exception {
                    makeVariableFor(x);
                    translation.add(getVariableFor(x));
				}

				@Override
				public void visitExpression(Expression e) throws Exception {
					final StringBuilder b = new StringBuilder();
					final Operator op = e.getOperator();
					if (e.isUnary()) {
						e.getOperand().accept(this);
						final String arg = translation.remove(0);
						b.append(op == Operator.NEG ? "-" : op.toString());
						b.append("(");
						b.append(arg);
						b.append(")");
					} else { 
						e.getFirstOperand().accept(this);
						final String firstArg = translation.remove(0);
						e.getSecondOperand().accept(this);
						final String secondArg = translation.remove(0);
						if (op.equals(Operator.EQ) ||
								op.equals(Operator.GT) ||
								op.equals(Operator.LT) ||
								op.equals(Operator.GE) ||
								op.equals(Operator.LE)) {
							b.append("(");
							b.append(firstArg);
							b.append(") ");
							b.append(op.toString());
							b.append(" (");
							b.append(secondArg);
							b.append(") ? 0 : isNaN((");
							b.append(firstArg);
							b.append(") - (");
							b.append(secondArg);
							b.append(")) ? BIG_DISTANCE : SMALL_DISTANCE + abs((");
							b.append(firstArg);
							b.append(") - (");
							b.append(secondArg);
							b.append("))");
						} else if (op.equals(Operator.NE)) {
							b.append("(");
							b.append(firstArg);
							b.append(") ");
							b.append(op.toString());
							b.append(" (");
							b.append(secondArg);
							b.append(") ? 0 : isNaN((");
							b.append(firstArg);
							b.append(") - (");
							b.append(secondArg);
							b.append(")) ? BIG_DISTANCE : SMALL_DISTANCE");
						} else {
							b.append("(");
							if (firstArg.equals("true")) {
								b.append("0");
							} else if (firstArg.equals("false")) {
								b.append("1");
							} else {
								b.append(firstArg);
							}
							b.append(") ");
							if (op.equals(Operator.AND)) {
								b.append("+");
							} else if (op.equals(Operator.OR)) {
								b.append("*");
							} else {
								b.append(op.toString());
							}
							b.append(" (");
							if (secondArg.equals("true")) {
								b.append("0");
							} else if (secondArg.equals("false")) {
								b.append("1");
							} else {
								b.append(secondArg);
							}
							b.append(")");
						}
					}
					translation.add(b.toString());
				}

				@Override
				public void visitAny(Any x) throws Exception {
					throw new Exception();
				}

				@Override
				public void visitPrimitiveSymbolicHashCode(PrimitiveSymbolicHashCode x) throws Exception {
					makeVariableFor(x);
					translation.add(getVariableFor(x));
				}

				@Override
				public void visitPrimitiveSymbolicLocalVariable(PrimitiveSymbolicLocalVariable x) throws Exception {
					makeVariableFor(x);
					translation.add(getVariableFor(x));
				}

				@Override
				public void visitPrimitiveSymbolicMemberArray(PrimitiveSymbolicMemberArray x) throws Exception {
					makeVariableFor(x);
					translation.add(getVariableFor(x));
				}

				@Override
				public void visitPrimitiveSymbolicMemberArrayLength(PrimitiveSymbolicMemberArrayLength x)
						throws Exception {
					makeVariableFor(x);
					translation.add(getVariableFor(x));
				}

				@Override
				public void visitPrimitiveSymbolicMemberField(PrimitiveSymbolicMemberField x) throws Exception {
					makeVariableFor(x);
					translation.add(getVariableFor(x));
				}
			};
			try {
				assumptionWithNoNegation.get(0).accept(translator);
			} catch (Exception exc) {
				//this may happen if Any appears in assumption
				throw new RuntimeException(exc);
			}

			return translation.get(0);
		}
	}

}
