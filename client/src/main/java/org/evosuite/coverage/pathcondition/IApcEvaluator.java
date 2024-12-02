package org.evosuite.coverage.pathcondition;

import java.util.ArrayList;

public interface IApcEvaluator {
    int[] getConverging();
    void setDisabled(int i, boolean val);
    void updtDisabled(boolean[] disabled);
    void disableAllButSome(int[] some);
    boolean isAllDisabled();
    boolean[] getDisabled();
    int[] getAlignedSpinObs();
    int[] getReverseSpinObs();
    String getSimilarityHandlerSrc(int i);
    String getDependencyEvaluatorClassName();
    int[] getDependencyEvaluatorClauseId();
	void processFeedback(ArrayList<Object> feedback);
	String[] getSeenLocations();
}
