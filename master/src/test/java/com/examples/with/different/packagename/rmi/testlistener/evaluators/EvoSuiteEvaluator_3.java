package com.examples.with.different.packagename.rmi.testlistener.evaluators;

import static java.lang.Double.*;
import static java.lang.Math.*;
import java.util.ArrayList;

public class EvoSuiteEvaluator_3 {

    private static final double SMALL_DISTANCE = 1;

    private static final double BIG_DISTANCE = 1E300;

    private Object ___INTERNAL__retVal_ = null;
    private Object ___INTERNAL__receiverObjectID__ = null;

    private Object[] ___INTERNAL__args__ = null;

    public double test0(java.lang.Object ___receiver__object, java.lang.Object a, java.lang.Object b) throws Exception {
    	___INTERNAL__receiverObjectID__ = ___receiver__object;
    	___INTERNAL__args__ = new Object[] { a, b };
        final ArrayList<ValueCalculator> ___INTERNAL__calculators__ = new ArrayList<>();
        populateCalculators0(___INTERNAL__calculators__);
        double ___INTERNAL__d__ = 0d;
        for (ValueCalculator ___INTERNAL__vc__ : ___INTERNAL__calculators__) {
            try {
                ___INTERNAL__d__ += normalize(___INTERNAL__vc__.calculate());
            } catch (Throwable ___INTERNAL__e__) {
                ___INTERNAL__d__ += 1d;
            }
        }
        if (___INTERNAL__d__ == 0.0d)
            System.out.println("test0 0 distance");
        return ___INTERNAL__d__;
    }

    public double test1(java.lang.Object ___retval, java.lang.Object ___receiver__object, java.lang.Object a, java.lang.Object b) throws Exception {
    	___INTERNAL__retVal_ = ___retval;
    	___INTERNAL__receiverObjectID__ = ___receiver__object;
        ___INTERNAL__args__ = new Object[] { a, b };
        final ArrayList<ValueCalculator> ___INTERNAL__calculators__ = new ArrayList<>();
        populateCalculators1(___INTERNAL__calculators__);
        double ___INTERNAL__d__ = 0d;
        for (ValueCalculator ___INTERNAL__vc__ : ___INTERNAL__calculators__) {
            try {
                ___INTERNAL__d__ += normalize(___INTERNAL__vc__.calculate());
            } catch (Throwable ___INTERNAL__e__) {
                ___INTERNAL__d__ += 1d;
            }
        }
        if (___INTERNAL__d__ == 0.0d)
            System.out.println("test1 0 distance");
        return ___INTERNAL__d__;
    }

    private abstract static class ValueCalculator {

        double calculate() {
            return condition() ? 0 : isNaN(cdistance()) ? BIG_DISTANCE : SMALL_DISTANCE + abs(cdistance());
        }

        abstract boolean condition();

        abstract double cdistance();
    }

    private double normalize(double val) {
        return val / (1 + val);
    }

    private void populateCalculators0(ArrayList<ValueCalculator> calculators) {
        calculators.add(new ValueCalculator() {

            boolean condition() {
                return ((int) ___INTERNAL__args__[0]) > 10000;
            }

            double cdistance() {
                return (((char) ___INTERNAL__args__[0]) > 10000 ? 0.0 : 10000 - ((char) ___INTERNAL__args__[0]));
            }
        });
    }

    private void populateCalculators1(ArrayList<ValueCalculator> calculators) {
        calculators.add(new ValueCalculator() {

            boolean condition() {
                return (___INTERNAL__retVal_ instanceof Throwable);
            }

            double cdistance() {
                return 0.0;
            }

        });
    }

    @Override
	public String toString() {
		return "custom description of EvoSuiteEvaluator_3 [This is for exception]";
	}

}
