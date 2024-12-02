package org.evosuite.ga.metaheuristics.mosa.jbse;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.evosuite.Properties;
import org.evosuite.graphs.cfg.BytecodeInstruction;
import org.evosuite.utils.LoggingUtils;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

public class JBSEBytecodeRelocationRegistry {

	private final Map<String, List<Integer>> relocationTable = new HashMap<>();
	private final Map<String, byte[]> classesWithComputedRelocation = new HashMap<>();
	
	private static JBSEBytecodeRelocationRegistry _instance = null;
	public static JBSEBytecodeRelocationRegistry _I() {
		if (_instance == null) {
			_instance = new JBSEBytecodeRelocationRegistry();
		}
		return _instance;
	}

	private JBSEBytecodeRelocationRegistry() {}
	
	public boolean wannaLogInstrumentedBytecodeClassFiles() {
		return false;
	}

	public boolean wannaCollectBytecodeRelocation(String className) {
		className = className.replace('.', '/');
		if (className.startsWith(Properties.TARGET_CLASS.replace('.', '/'))) {
			if (!classesWithComputedRelocation.keySet().contains(className)) {
				LoggingUtils.getEvoLogger().info("[JBSE] Collecting bytecode offset relocations for class {}", className);
			}
			return !classesWithComputedRelocation.keySet().contains(className);
		} else {
			return false;
		}
	}
	public void notifyEndOfTransformations(String className) {
		className = className.replace('.', '/');
		classesWithComputedRelocation.put(className, null);
	}

	public int getRelocatedOffset(BytecodeInstruction bytecodeInstruction) {
		if (wannaLogInstrumentedBytecodeClassFiles()) {
			dumpRelocatedBytecode();
		}
		
		String className = bytecodeInstruction.getClassName().replace('.', '/');
		String methodName = bytecodeInstruction.getMethodName();
		
		int relocatedOffset = bytecodeInstruction.getBytecodeOffset();
		List<Integer> relocations = relocationTable.get(className  + '.' + methodName);
		LoggingUtils.getEvoLogger().info("[JBSE] THERE EXIST RELOCATIONS FOR METHODS {}", relocationTable.keySet());
		LoggingUtils.getEvoLogger().info("[JBSE] INSTR {}, RELOCATIONS {}", bytecodeInstruction.getInstructionId(), relocations);
		if (relocations != null) {
			Integer deltaOffset = relocations.get(bytecodeInstruction.getInstructionId());
			if (deltaOffset == null) {
				LoggingUtils.getEvoLogger().info("[JBSE] WARNING: MISSING RELOCATION INFO FOR INSTRUCTION ID={} IN {}.{} -- Check this, since when we collect relocations for a class method, we should track all instructions of the method (though relocation can be 0 for some instructions)", bytecodeInstruction.getInstructionId(), className, methodName);
			} else if (deltaOffset == -1) {   
				LoggingUtils.getEvoLogger().info("[JBSE] WARNING: RELOCATION REFERS TO NON ORIGINAL INSTRUCTION ID={} IN {}.{} -- Chack this, since we do not expect that the tracked branches refer to non original instructions", bytecodeInstruction.getInstructionId(), className, methodName);
			} else {			
				LoggingUtils.getEvoLogger().info("[JBSE] RELOCATION OF -{} STEPS", deltaOffset);
				relocatedOffset -= deltaOffset;
			}
		}
		return relocatedOffset;
	}
		
	public static class InsnListDecorator extends InsnList {
		
		private final String className;
		private final String methodName;
		private final InsnList localInstructions;
		
		public InsnListDecorator(String className, String methodName, InsnList instructions) {
			this.className = className;
			this.methodName = methodName;
			this.localInstructions = instructions;
		}
		
		@Override
		public int size() {
			return localInstructions.size();
		}

		@Override
		public AbstractInsnNode getFirst() {
			return localInstructions.getFirst();
		}

		@Override
		public AbstractInsnNode getLast() {
			return localInstructions.getLast();
		}

		@Override
		public AbstractInsnNode get(int index) {
			return localInstructions.get(index);
		}

		@Override
		public boolean contains(AbstractInsnNode insn) {
			return localInstructions.contains(insn);
		}

		@Override
		public int indexOf(AbstractInsnNode insn) {
			return localInstructions.indexOf(insn);
		}

		@Override
		public void accept(MethodVisitor mv) {
			localInstructions.accept(mv);
		}

		@Override
		public ListIterator iterator() {
			return localInstructions.iterator();
		}

		@Override
		public ListIterator iterator(int index) {
			return localInstructions.iterator(index);
		}

		@Override
		public AbstractInsnNode[] toArray() {
			return localInstructions.toArray();
		}

		@Override
		public void set(AbstractInsnNode location, AbstractInsnNode insn) {
			updateRelocationInsert(className, methodName, localInstructions.indexOf(location), new AbstractInsnNode[] { insn }, false);
			updateRelocationRemove(className, methodName, localInstructions.indexOf(location), insn);
			localInstructions.set(location, insn);
		}

		@Override
		public void add(AbstractInsnNode insn) {
			updateRelocationInsert(className, methodName, localInstructions.size() - 1, new AbstractInsnNode[] { insn }, false);
			localInstructions.add(insn);
		}

		@Override
		public void add(InsnList insns) {
			updateRelocationInsert(className, methodName, localInstructions.size() - 1, insns.toArray(), false);
			localInstructions.add(insns);
		}

		@Override
		public void insert(AbstractInsnNode insn) {
			updateRelocationInsert(className, methodName, 0, new AbstractInsnNode[] { insn }, true);
			localInstructions.insert(insn);
		}

		@Override
		public void insert(InsnList insns) {
			updateRelocationInsert(className, methodName, 0, insns.toArray(), true);
			localInstructions.insert(insns);
		}

		@Override
		public void insert(AbstractInsnNode location, AbstractInsnNode insn) {
			updateRelocationInsert(className, methodName, localInstructions.indexOf(location), new AbstractInsnNode[] { insn }, false);
			localInstructions.insert(location, insn);
		}

		@Override
		public void insert(AbstractInsnNode location, InsnList insns) {
			updateRelocationInsert(className, methodName, localInstructions.indexOf(location), insns.toArray(), false);
			localInstructions.insert(location, insns);
		}

		@Override
		public void insertBefore(AbstractInsnNode location, AbstractInsnNode insn) {
			updateRelocationInsert(className, methodName, localInstructions.indexOf(location), new AbstractInsnNode[] { insn }, true);
			localInstructions.insertBefore(location, insn);
		}

		@Override
		public void insertBefore(AbstractInsnNode location, InsnList insns) {
			updateRelocationInsert(className, methodName, localInstructions.indexOf(location), insns.toArray(), true);
			localInstructions.insertBefore(location, insns);
		}

		@Override
		public void remove(AbstractInsnNode insn) {
			updateRelocationRemove(className, methodName, localInstructions.indexOf(insn), insn);
			localInstructions.remove(insn);
		}

		@Override
		public void clear() {
			localInstructions.clear();
		}

		@Override
		public void resetLabels() {
			localInstructions.resetLabels();
		}
		
		private void updateRelocationInsert(String className, String methodName, int nextInstructionIndex, AbstractInsnNode[] insertedInstructions, boolean insertBefore) {
			if (JBSEBytecodeRelocationRegistry._I().classesWithComputedRelocation.keySet().contains(className)) {
				return;
			}
			
			List<Integer> currentRelocations = JBSEBytecodeRelocationRegistry._I().relocationTable.get(className + '.' + methodName);
			if (currentRelocations == null) {
				currentRelocations = new ArrayList<Integer>();
				JBSEBytecodeRelocationRegistry._I().relocationTable.put(className + '.' + methodName, currentRelocations);
				for (int i = 0; i < localInstructions.size(); i++) {
					currentRelocations.add(0);
				}
			}
			
			int relocationIndex = insertBefore ? nextInstructionIndex : nextInstructionIndex + 1;
			
			int deltaOffset = 0;
			for (int i = 0; i < insertedInstructions.length; i++) {
				deltaOffset += getBytecodeIncrement(insertedInstructions[i]);
				if (!(insertedInstructions[i] instanceof LabelNode) && !(insertedInstructions[i] instanceof LineNumberNode) && !(insertedInstructions[i] instanceof FrameNode)) {
					deltaOffset++;
				}
				currentRelocations.add(relocationIndex++, -1);
			}
			
			for (int i = relocationIndex; i < currentRelocations.size(); i++) {
				int currentOffset = currentRelocations.remove(i); 
				currentRelocations.add(i, currentOffset + deltaOffset);
			}		
		}

		private static void updateRelocationRemove(String className, String methodName, int removeInstructionIndex, AbstractInsnNode removed) {
			if (JBSEBytecodeRelocationRegistry._I().classesWithComputedRelocation.keySet().contains(className)) {
				return;
			}
			
			List<Integer> currentRelocations = JBSEBytecodeRelocationRegistry._I().relocationTable.get(className + '.' + methodName);
			if (currentRelocations == null || removeInstructionIndex >= currentRelocations.size()) {
				return;
			}			
			
			currentRelocations.remove(removeInstructionIndex);

			int deltaOffset = getBytecodeIncrement(removed);
			if (!(removed instanceof LabelNode) && !(removed instanceof LineNumberNode) && !(removed instanceof FrameNode)) {
				deltaOffset++;
			}
			if (removed instanceof TableSwitchInsnNode || removed instanceof LookupSwitchInsnNode) { //Computation for switch
				int padding = 0; //Problem: how can compute the padding for relocation. Hopefully Switch instruction are never removed.
				deltaOffset += padding;
				throw new RuntimeException("Relocation for removing of switch-instructions: not impelemnted yet");
			}
			
			for (int i = removeInstructionIndex; i < currentRelocations.size(); i++) {
				int currentOffset = currentRelocations.remove(i); 
				currentRelocations.add(i, currentOffset - deltaOffset);
			}		
		}

		/**
		 * Determine how many bytes the current instruction occupies together with
		 * its operands
		 * 
		 * @return
		 */
		private static int getBytecodeIncrement(AbstractInsnNode instructionNode) {
			int opcode = instructionNode.getOpcode();
			switch (opcode) {
			case Opcodes.ALOAD: // index
			case Opcodes.ASTORE: // index
			case Opcodes.DLOAD:
			case Opcodes.DSTORE:
			case Opcodes.FLOAD:
			case Opcodes.FSTORE:
			case Opcodes.ILOAD:
			case Opcodes.ISTORE:
			case Opcodes.LLOAD:
			case Opcodes.LSTORE:
				VarInsnNode varNode = (VarInsnNode) instructionNode;
				if (varNode.var > 3)
					return 1;
				else
					return 0;
			case Opcodes.BIPUSH: // byte
			case Opcodes.NEWARRAY:
			case Opcodes.RET:
				return 1;
			case Opcodes.LDC:
				LdcInsnNode ldcNode = (LdcInsnNode)instructionNode;
				if(ldcNode.cst instanceof Double || ldcNode.cst instanceof Long)
					return 2; // LDC2_W
				else
					return 1;
			case 19: //LDC_W
			case 20: //LDC2_W
				return 2;		
			case Opcodes.ANEWARRAY: // indexbyte1, indexbyte2
			case Opcodes.CHECKCAST: // indexbyte1, indexbyte2
			case Opcodes.GETFIELD:
			case Opcodes.GETSTATIC:
			case Opcodes.GOTO:
			case Opcodes.IF_ACMPEQ:
			case Opcodes.IF_ACMPNE:
			case Opcodes.IF_ICMPEQ:
			case Opcodes.IF_ICMPNE:
			case Opcodes.IF_ICMPGE:
			case Opcodes.IF_ICMPGT:
			case Opcodes.IF_ICMPLE:
			case Opcodes.IF_ICMPLT:
			case Opcodes.IFLE:
			case Opcodes.IFLT:
			case Opcodes.IFGE:
			case Opcodes.IFGT:
			case Opcodes.IFNE:
			case Opcodes.IFEQ:
			case Opcodes.IFNONNULL:
			case Opcodes.IFNULL:
			case Opcodes.IINC:
			case Opcodes.INSTANCEOF:
			case Opcodes.INVOKESPECIAL:
			case Opcodes.INVOKESTATIC:
			case Opcodes.INVOKEVIRTUAL:
			case Opcodes.JSR:
			case Opcodes.NEW:
			case Opcodes.PUTFIELD:
			case Opcodes.PUTSTATIC:
			case Opcodes.SIPUSH:
				// case Opcodes.LDC_W
				// case Opcodes.LDC2_W

				return 2;
			case Opcodes.MULTIANEWARRAY:
				return 3;
			case Opcodes.INVOKEDYNAMIC:
			case Opcodes.INVOKEINTERFACE:
				return 4;

			case Opcodes.LOOKUPSWITCH:
				int numCasesLS = ((LookupSwitchInsnNode) instructionNode).labels.size();
				return 8 + (8 * numCasesLS); //4 default label, 4 npairs, 8*numCases
			case Opcodes.TABLESWITCH:
				int numCasesTS = ((TableSwitchInsnNode) instructionNode).labels.size();
				return 12 + (4 * numCasesTS); //4 default label, 4 low, 4 high, 4*numCases
			// case Opcodes.GOTO_W 
			// case Opcodes.JSR_W
			}
			return 0;
		}
		
	}
	
	public void logRelocatedBytecodeClassFile(String className, byte[] clazz) {
		className = className.replace('.', '/');
		if (classesWithComputedRelocation.keySet().contains(className)) {
			classesWithComputedRelocation.put(className, clazz);
		}
	}
	
	public void dumpRelocatedBytecode() {
		for (String className : classesWithComputedRelocation.keySet()) {
			byte[] clazz = classesWithComputedRelocation.get(className);
			if (clazz != null) {
				final Path classFilePath = Paths.get(Properties.TMP_TEST_DIR).resolve(className.replace('.', '/') + ".class");
				try (final OutputStream w = new BufferedOutputStream(Files.newOutputStream(classFilePath))) {
					w.write(clazz);
				} catch (IOException e) {
					LoggingUtils.getEvoLogger().info("[JBSE] exception while storing instrumented class {} : {}", className, e);
				}
				classesWithComputedRelocation.put(className, null); 
			}
		}
	}	
		
}
