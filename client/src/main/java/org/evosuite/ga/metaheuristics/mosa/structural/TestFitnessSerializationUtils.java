package org.evosuite.ga.metaheuristics.mosa.structural;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.evosuite.coverage.ControlFlowDistance;
import org.evosuite.coverage.branch.Branch;
import org.evosuite.coverage.branch.BranchCoverageGoal;
import org.evosuite.coverage.branch.BranchCoverageTestFitness;
import org.evosuite.graphs.cfg.BytecodeInstruction;
import org.evosuite.testcase.TestFitnessFunction;
import org.evosuite.testcase.execution.ExecutionResult;
import org.objectweb.asm.tree.LabelNode;

public class TestFitnessSerializationUtils {

	public static TestFitnessFunction makeSerializableForNonEvosuiteClients(TestFitnessFunction ff) {
		if (ff instanceof BranchCoverageTestFitness) {
			BranchCoverageTestFitness branchFitness = (BranchCoverageTestFitness) ff;
			if (branchFitness.getBranchGoal() != null && branchFitness.getBranchGoal().getBranch() != null) {
				return new BranchCoverageTestFitness(
						new SerializableForExternalClients_BranchCoverageGoal(branchFitness.getBranchGoal()));
			} 
		}
		return ff; //no need of adaptation
    }
	
    private static class SerializableForExternalClients_BranchCoverageGoal extends BranchCoverageGoal {
    	private static final long serialVersionUID = -1013673054550302015L;

    	private BranchCoverageGoal theBranchCoverageGoal;
    	
    	public SerializableForExternalClients_BranchCoverageGoal(BranchCoverageGoal goal) {
    		super(null, true, "", "", -1); // fake state (not used) just to make constructor happy
    		this.theBranchCoverageGoal = goal;
    	}
    	
    	//delegates everything to target, just defines the serialization logic to properly rebuild the actualBranchCoverageGoal  
    	
    	@Override
		public int getId() {
			return theBranchCoverageGoal.getId();
		}
		@Override
		public Branch getBranch() {
			return theBranchCoverageGoal.getBranch();
		}
		@Override
		public boolean getValue() {
			return theBranchCoverageGoal.getValue();
		}
		@Override
		public String getClassName() {
			return theBranchCoverageGoal.getClassName();
		}
		@Override
		public String getMethodName() {
			return theBranchCoverageGoal.getMethodName();
		}
		@Override
		public int getLineNumber() {
			return theBranchCoverageGoal.getLineNumber();
		}
		@Override
		public boolean isConnectedTo(BranchCoverageGoal goal) {
			throw new IllegalStateException("Method is not allowed when using this decorator");
		}
		@Override
		public ControlFlowDistance getDistance(ExecutionResult result) {
			throw new IllegalStateException("Method is not allowed when using this decorator");
		}
		@Override
		public int hashCodeWithoutValue() {
			return theBranchCoverageGoal.hashCodeWithoutValue();
		}
		@Override
		public String toString() {
			return theBranchCoverageGoal.toString();
		}
		@Override
		public int hashCode() {
			return theBranchCoverageGoal.hashCode();
		}
		@Override
		public boolean equals(Object obj) {
			return theBranchCoverageGoal.equals(obj);
		}
		@Override
		public int compareTo(BranchCoverageGoal o) {
			throw new IllegalStateException("Method is not allowed when using this decorator");
		}

		private void writeObject(ObjectOutputStream oos) throws IOException {
    		oos.defaultWriteObject();
    		oos.writeInt(theBranchCoverageGoal.getBranch().getActualBranchId());
    		oos.writeInt(theBranchCoverageGoal.getBranch().getInstruction().getInstructionId());
    		oos.writeInt(theBranchCoverageGoal.getBranch().getInstruction().getBytecodeOffset());
    		oos.writeInt(theBranchCoverageGoal.getBranch().getInstruction().getLineNumber());
    		oos.writeUTF(theBranchCoverageGoal.getBranch().getInstruction().getInstructionType());
    		oos.writeBoolean(theBranchCoverageGoal.getBranch().isSwitchCaseBranch());
    		if (theBranchCoverageGoal.getBranch().isSwitchCaseBranch()) {
        		oos.writeObject(theBranchCoverageGoal.getBranch().getTargetCaseValue());
    		}
    	}

		private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
    		ois.defaultReadObject();
            int branchId = ois.readInt();
            int instructionId = ois.readInt();
            int bytecodeOffset = ois.readInt();
            int lineNumber = ois.readInt();
            String instructionType = ois.readUTF();
			BytecodeInstruction instruction = new BytecodeInstruction(null, theBranchCoverageGoal.getClassName(), 
					theBranchCoverageGoal.getMethodName(), instructionId, bytecodeOffset, new LabelNode(), lineNumber) {
				private static final long serialVersionUID = 2795384470763443155L;
				@Override
				public String getInstructionType() {
					return instructionType; //to avoid calls to internal asnNode (which is not serialized)
				}
				@Override
				public boolean isBranch() {
					return true;  //to make constructor of class Branch (called below) happy
				}
				@Override
				public boolean isSwitch() {
					return true;  //to make constructor of class Branch (called below) happy
				}
			};
            Branch branch;
            boolean isSwitch = ois.readBoolean();
    		if (isSwitch) {
    			Integer targetCaseValue = (Integer) ois.readObject();
    			branch = new Branch(instruction, targetCaseValue, new LabelNode(), branchId);
    		} else {
    			branch = new Branch(instruction, branchId);
    		}
    		theBranchCoverageGoal = new BranchCoverageGoal(branch, theBranchCoverageGoal.getValue(), theBranchCoverageGoal.getClassName(), theBranchCoverageGoal.getMethodName(), theBranchCoverageGoal.getLineNumber());
    	}
    }
}
