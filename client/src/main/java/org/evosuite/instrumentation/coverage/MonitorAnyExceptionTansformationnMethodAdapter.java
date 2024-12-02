/*
 * Copyright (C) 2010-2018 Gordon Fraser, Andrea Arcuri and EvoSuite
 * contributors
 *
 * This file is part of EvoSuite.
 *
 * EvoSuite is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3.0 of the License, or
 * (at your option) any later version.
 *
 * EvoSuite is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with EvoSuite. If not, see <http://www.gnu.org/licenses/>.
 */
package org.evosuite.instrumentation.coverage;

import org.evosuite.PackageInfo;
import org.evosuite.classpath.ResourceList;
import org.evosuite.testcase.execution.ExecutionTracer;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by giovanni on 17/02/2022.
 * 
 * Add a try-catch(Throwable) block that encloses the method body, and thus intercepts any 
 * exception that propagates to callers. The block simply reflects the exception, thus making
 * the execution continuing as expected, but at the same time allowing for a location 
 * where we can introduce instrumentation related to the happening of the exception. For example,
 * this is exploited for evaluating fitness related to satisfying post-conditions in PathConditionFitness.
 */
public class MonitorAnyExceptionTansformationnMethodAdapter extends GeneratorAdapter {

    protected final static Logger logger = LoggerFactory.getLogger(MonitorAnyExceptionTansformationnMethodAdapter.class);

    private final String classNameWithDots;
    private final String methodName;
    private final MethodVisitor next;
   
    private final List<TryCatchBlock> tryCatchBlocks = new LinkedList<>();

    private static class TryCatchBlock {
        Label start;
        Label end;
        Label handler;
        String type;
        public TryCatchBlock(Label start, Label end, Label handler, String type) {
            this.start = start;
            this.end = end;
            this.handler = handler;
            this.type = type;
        }
    }

    public MonitorAnyExceptionTansformationnMethodAdapter(MethodVisitor mv, String className,
                                                String methodName, int access, String desc) {
        super(Opcodes.ASM9, mv, access, methodName, desc);
        //super(Opcodes.ASM9,
        //        new AnnotatedMethodNode(access, methodName, desc, null, null), access,
        //        methodName, desc);
    	if (methodName.equals("<init>")) {
    		instrumentingConstructor = true;
    	}
        this.classNameWithDots = ResourceList.getClassNameFromResourcePath(className);
        this.methodName = methodName + desc;
        next = mv;
    }

	private Label start = newLabel();
	private Label end   = newLabel();
	private Label catchLabel  = newLabel();
    int exceptionInstanceVar;
    boolean instrumentingConstructor = false;
    boolean startAlreadyMarked = false;
    int newInsnMetSoFar = 0;
	
    @Override
    public void visitCode() {
    	if (!instrumentingConstructor) {
    		addStartOfDominantExceptionHandlingBlock() ;
    	}
        super.visitCode();
    }    
	    
    @Override
    public void visitTypeInsn(final int opcode, final String type) {
    	if (instrumentingConstructor && !startAlreadyMarked && opcode == Opcodes.NEW) {
    		++newInsnMetSoFar;
    	}
    	super.visitTypeInsn(opcode, type);
    }

    @Override
	public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
		super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
		if (instrumentingConstructor && !startAlreadyMarked && name.equals("<init>")) { 
			//we are looking for the call to super(...) or this(...)
			if (newInsnMetSoFar > 0) {
				--newInsnMetSoFar; //this is not call to super(...), it is the constructor for an object instantiated thereby
			} else { //as there is no matching new insn beforehand, this was the call to super
				addStartOfDominantExceptionHandlingBlock();
			}
		}
	}

    private void addStartOfDominantExceptionHandlingBlock() {
        // Insert start of try block label
    	if (!startAlreadyMarked) { //add only once
    		mark(start);    	
    		startAlreadyMarked = true;
    	}
    }

    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
		addEndOfDominantExceptionHandlingBlock();
		super.visitMaxs(maxStack + 1, maxLocals + 1);
    }

    private void addEndOfDominantExceptionHandlingBlock() {
        // Insert end of try block label
        mark(end);
        // Skip catch block if no exception was thrown
        Label afterCatch = newLabel();
        goTo(afterCatch);
        mark(catchLabel);
        addInstrumentationForCallbackToEvosuite();
        // reflect the exception.
        throwException();
        mark(afterCatch);
    }

    private void addInstrumentationForCallbackToEvosuite() {
        dup();
        this.visitLdcInsn(classNameWithDots);
        this.visitLdcInsn(methodName);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                PackageInfo.getNameWithSlash(ExecutionTracer.class),
                "passedExceptionHandler", "(Ljava/lang/Throwable;Ljava/lang/String;Ljava/lang/String;)V", false);
    }
    
    private boolean shallInstrumentExceptionHandler = false;
    
    @Override
    public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
        TryCatchBlock block = new TryCatchBlock(start, end, handler, type);
        tryCatchBlocks.add(block); //remember the blocks, to instrument exception handlers
        super.visitTryCatchBlock(start, end, handler, type);
    }

    @Override
    public void visitLabel(final Label label) {
    	super.visitLabel(label);
        for (TryCatchBlock tryCatchBlock : tryCatchBlocks) {
        	if (label == tryCatchBlock.handler) {
                shallInstrumentExceptionHandler = true; //next block is an exception handler        		
        	}
        }
    }
    
    @Override
    public void visitVarInsn(final int opcode, final int var) {
        if (shallInstrumentExceptionHandler && opcode == Opcodes.ASTORE) {
            shallInstrumentExceptionHandler = false; 
            // we notify the caught exception to Execution tracker
            addInstrumentationForCallbackToEvosuite();
        }
        super.visitVarInsn(opcode, var); //postponed because astore consumes the objref that we shall dup first  	
    }
    
    @Override
	public void visitEnd() {
    	super.visitTryCatchBlock(start, end, catchLabel, "java/lang/Throwable"); //set body-enclosing TryCatchBlock as last TryCatchBlock
		super.visitEnd();
	}	
}
