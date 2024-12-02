package org.evosuite.coverage.pathcondition;

import java.util.Set;

import org.evosuite.coverage.branch.BranchCoverageGoal;
import org.evosuite.graphs.cfg.ActualControlFlowGraph;
import org.evosuite.graphs.cfg.BytecodeInstruction;
import org.evosuite.graphs.cfg.ControlDependency;
import org.evosuite.graphs.cfg.ControlFlowEdge;

public abstract class PathConditionTraversedBranch {
	public final String className;
	public final String methodName;
	private final int hash;
	
	PathConditionTraversedBranch(String className, String methodDescriptor, String methodName) {
		if (className == null || methodDescriptor == null || methodName == null) {
			throw new NullPointerException();
		}
		this.className = className;
		this.methodName = methodName + methodDescriptor;
		final int prime = 31;
		int result = 1;
		result = prime * result + this.className.hashCode();
		result = prime * result + this.methodName.hashCode();
		this.hash = result;
	}
	
	@Override
	public int hashCode() {
		return this.hash;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final PathConditionTraversedBranch other = (PathConditionTraversedBranch) obj;
		if (!this.className.equals(other.className)) {
			return false;
		}
		if (!this.methodName.equals(other.methodName)) {
			return false;
		}
		return true;
	}
	
	public static PathConditionTraversedBranch makeMethodEntryPoint(String className, String methodDescriptor, String methodName) {
		return new MethodEntryPoint(className, methodDescriptor, methodName);
	}

	public static PathConditionTraversedBranch makeRealBranch(String className, String methodDescriptor, String methodName, int bytecodeFrom, int bytecodeTo) {
		return new RealBranch(className, methodDescriptor, methodName, bytecodeFrom, bytecodeTo);
	}

	static class MethodEntryPoint extends PathConditionTraversedBranch {
		MethodEntryPoint(String className, String methodDescriptor, String methodName) {
			super(className, methodDescriptor, methodName);
		}

		@Override
		public String toString() {
			return "MethodEntryPoint: " + className + "." + methodName + ": root-branch";
		}
	}
	

	static class RealBranch extends PathConditionTraversedBranch {
		public final int bytecodeFrom;
		public final int bytecodeTo;
		private final int hash;
		
		RealBranch(String className, String methodDescriptor, String methodName, int bytecodeFrom, int bytecodeTo) {
			super(className, methodDescriptor, methodName);
			this.bytecodeFrom = bytecodeFrom;
			this.bytecodeTo = bytecodeTo;
			final int prime = 59;
			int result = super.hashCode();
			result = prime * result + this.bytecodeFrom;
			result = prime * result + this.bytecodeTo;
			this.hash = result;
		}
		
		@Override
		public String toString() {
			return "Branch: " + className + "." + methodName + ":" +
					bytecodeFrom + "-->" + bytecodeTo;
		}

		@Override
		public int hashCode() {
			return this.hash;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (!super.equals(obj)) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			final RealBranch other = (RealBranch) obj;
			if (this.bytecodeFrom != other.bytecodeFrom) {
				return false;
			}
			if (this.bytecodeTo != other.bytecodeTo) {
				return false;
			}
			return true;
		}
	}


	public static class NeverTraversedException extends Exception {
		
	}
	
	public static PathConditionTraversedBranch makeFromOtherFormat(BranchCoverageGoal b) throws NeverTraversedException {
		String className = b.getClassName().replace('.', '/');
		String methodName = b.getMethodName();

		if (b.getBranch() == null) {
			MethodEntryPoint ret = new MethodEntryPoint(className, "", methodName);
			return ret;
		} else if (b.getBranch().isSwitchCaseBranch() && b.getValue() == false) {
			throw new NeverTraversedException(); // EvoSuite considers this branches but path conditions will never traverse them
		} else {	
			BytecodeInstruction instr = b.getBranch().getInstruction();
			ActualControlFlowGraph cfg = instr.getActualCFG();
			
			Set<ControlFlowEdge> outgoingEdges = cfg.outgoingEdgesOf(instr.getBasicBlock());
			for (ControlFlowEdge edge : outgoingEdges) {
				ControlDependency cdep = edge.getControlDependency();
				if (cdep.getBranch().equals(b.getBranch()) && b.getValue() == cdep.getBranchExpressionValue()) {
					RealBranch ret = new RealBranch(className, "", methodName, instr.getBytecodeOffset(), cfg.getEdgeTarget(edge).getFirstInstruction().getBytecodeOffset());
					return ret;
				}
			}
						
			throw new RuntimeException("Failure to find taget bytecode offset for branch: " + b);
		}
	}
	
}