package org.evosuite.coverage.seepep;

import java.util.Arrays;
import java.util.HashSet;

import jbse.bc.Signature;

public class SparkMethodSignatures {
	private class SignatureDots {
		final String classname;
		final String methodname;
		public SignatureDots(String classname, String methodname) {
			this.classname = classname;
			this.methodname = methodname;
		}
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + classname.hashCode();
			result = prime * result + methodname.hashCode();
			return result;
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			SignatureDots other = (SignatureDots) obj;
			if (!classname.equals(other.classname))
				return false;
			if (!methodname.equals(other.methodname))
				return false;
			return true;
		}
		
		private SparkMethodSignatures getOuterType() {
			return SparkMethodSignatures.this;
		}
		
	}
	private static SparkMethodSignatures _I;
	private SparkMethodSignatures() {
		for (Signature s: sparkActionSignatures) {
			sparkActionSignaturesDots.add(new SignatureDots(s.getClassName().replace('/', '.'), s.getName() + s.getDescriptor()));
		}
		for (Signature s: sparkTransformationSignatures) {
			sparkTransformationSignaturesDots.add(new SignatureDots(s.getClassName().replace('/', '.'), s.getName() + s.getDescriptor()));
		}
		for (Signature s: sparkRootRddMethodSignatures) {
			sparkRootRddMethodSignaturesDots.add(new SignatureDots(s.getClassName().replace('/', '.'), s.getName() + s.getDescriptor()));
		}
	}
	public static SparkMethodSignatures _I() {
		if (_I == null) {
			_I = new SparkMethodSignatures();
		}
		return _I;
	}
	private final HashSet<SignatureDots> sparkActionSignaturesDots = new HashSet<>();
	private final HashSet<SignatureDots> sparkTransformationSignaturesDots = new HashSet<>();
	private final HashSet<SignatureDots> sparkRootRddMethodSignaturesDots = new HashSet<>();
	private final HashSet<Signature> sparkActionSignatures = new HashSet<>(Arrays.asList(
			new Signature[] {
					new Signature("org/apache/spark/api/java/JavaRDD", "()J", "count"),
					new Signature("org/apache/spark/api/java/JavaRDD", "(Lscala/Function2;)Ljava/lang/Object;", "reduce"),
					new Signature("org/apache/spark/api/java/JavaRDD", "(Ljava/lang/String;)V", "saveAsTextFile_deactivated, but should do"), //TODO
			}));

	private final HashSet<Signature> sparkRootRddMethodSignatures = new HashSet<>(Arrays.asList(
			new Signature[] { 
					new Signature("org/apache/spark/api/java/JavaSparkContext", "(Ljava/util/List;)Lorg/apache/spark/api/java/JavaRDD;", "parallelize"),
					new Signature("org/apache/spark/api/java/JavaSparkContext", "(Ljava/util/List;I)Lorg/apache/spark/api/java/JavaRDD;", "parallelize"),
					new Signature("org/apache/spark/api/java/JavaSparkContext", "(Ljava/lang/String;)Lorg/apache/spark/api/java/JavaRDD;", "textFile"),
					new Signature("org/apache/spark/api/java/JavaSparkContext", "(Ljava/lang/String;I)Lorg/apache/spark/api/java/JavaRDD;", "textFile")
			}));
	private final HashSet<Signature> sparkTransformationSignatures = new HashSet<>(Arrays.asList(
			new Signature[] { //TODO: review the corrispondence with framework
					new Signature("org/apache/spark/api/java/JavaRDD", "(Lorg/apache/spark/api/java/function/Function;Lscala/reflect/ClassTag;)Lorg/apache/spark/api/java/JavaRDD;", "map"),
					new Signature("org/apache/spark/api/java/JavaRDD", "(Lorg/apache/spark/api/java/function/Function;)Lorg/apache/spark/api/java/JavaRDD;", "map"),
					new Signature("org/apache/spark/api/java/JavaRDD", "(Lscala/Function1;)Lorg/apache/spark/api/java/JavaRDD;", "filter"),
					new Signature("org/apache/spark/api/java/JavaRDD", "(Lorg/apache/spark/api/java/function/PairFunction;)Lorg/apache/spark/api/java/JavaPairRDD;", "mapToPair"),
					new Signature("org/apache/spark/api/java/JavaRDD", "(Lorg/apache/spark/api/java/function/FlatMapFunction;)Lorg/apache/spark/api/java/JavaRDD;", "flatMap"),

					new Signature("org/apache/spark/api/java/JavaPairRDD", "(Lorg/apache/spark/api/java/function/Function2;)Lorg/apache/spark/api/java/JavaPairRDD;", "reduceByKey"),
					new Signature("org/apache/spark/api/java/JavaPairRDD", "(Lorg/apache/spark/api/java/JavaPairRDD;)Lorg/apache/spark/api/java/JavaPairRDD;", "join"),
					new Signature("org/apache/spark/api/java/JavaPairRDD", "(Lorg/apache/spark/api/java/JavaPairRDD;)Lorg/apache/spark/api/java/JavaPairRDD;", "leftOuterJoin"),
					new Signature("org/apache/spark/api/java/JavaPairRDD", "(Lorg/apache/spark/api/java/JavaPairRDD;)Lorg/apache/spark/api/java/JavaPairRDD;", "leftOuterJoinWithOptionInsteadOfOptional"),

					new Signature("org/apache/spark/graphx/EdgeRDD", "(Lorg/apache/spark/api/java/JavaRDD;Lscala/reflect/ClassTag;Lscala/reflect/ClassTag;)Lorg/apache/spark/graphx/EdgeRDD;", "fromEdges"),

					new Signature("org/apache/spark/graphx/VertexRDD", "(Lorg/apache/spark/api/java/JavaRDD;Lscala/reflect/ClassTag;)Lorg/apache/spark/graphx/VertexRDD;", "apply"),
					new Signature("org/apache/spark/graphx/VertexRDD", "(Lorg/apache/spark/api/java/JavaRDD;Lscala/Function3;Lscala/reflect/ClassTag;Lscala/reflect/ClassTag;)Lorg/apache/spark/graphx/VertexRDD;", "innerJoin"),
					new Signature("org/apache/spark/graphx/VertexRDD", "(Lorg/apache/spark/api/java/JavaRDD;Lscala/Function3;)Lorg/apache/spark/graphx/VertexRDD;", "leftOuterJoin"),
					new Signature("org/apache/spark/graphx/VertexRDD", "(Lscala/Function1;Lscala/reflect/ClassTag;)Lorg/apache/spark/api/java/JavaRDD;", "map"),
					new Signature("org/apache/spark/graphx/VertexRDD", "(Lscala/Function1;)Lorg/apache/spark/api/java/JavaRDD;", "map"),

					new Signature("org/apache/spark/graphx/Graph", "(Lorg/apache/spark/graphx/VertexRDD;Lorg/apache/spark/graphx/EdgeRDD;)Lorg/apache/spark/api/java/JavaRDD;", "computeTriplets"),
					new Signature("org/apache/spark/graphx/Graph", "(Lscala/Function1;Lscala/Function2;Lorg/apache/spark/graphx/TripletFields;Lscala/reflect/ClassTag;)Lorg/apache/spark/graphx/VertexRDD;", "aggregateMessages"),
					new Signature("org/apache/spark/graphx/Graph", "(Lorg/apache/spark/api/java/JavaRDD;Lscala/Function1;Lscala/Function2;)Lorg/apache/spark/graphx/VertexRDD;", "aggregateMessages_helper_computeVerticesFromTriplets"),
					//TODO: new Signature("org/apache/spark/graphx/Graph", "(Lorg/apache/spark/api/java/JavaRDD;Lscala/Function3;Lscala/reflect/ClassTag;Lscala/reflect/ClassTag;Ljava/lang/Object;)Lorg/apache/spark/graphx/Graph;", "outerJoinVertices"),
					new Signature("org/apache/spark/graphx/Graph", "(Lorg/apache/spark/graphx/EdgeRDD;Lscala/Function2;)Lorg/apache/spark/api/java/JavaRDD;", "groupEdges_helper_computeGroupedEdgesFromEdges"),
					new Signature("org/apache/spark/graphx/Graph", "(Lorg/apache/spark/api/java/JavaRDD;Ljava/lang/Object;)Lorg/apache/spark/api/java/JavaPairRDD;", "fromEdges_helper_computeVerticesFromEdges"),
					//TODO: new Signature("org/apache/spark/graphx/Graph", "(Lorg/apache/spark/graphx/VertexRDD;Lorg/apache/spark/graphx/EdgeRDD;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Lscala/reflect/ClassTag;Lscala/reflect/ClassTag;)Lorg/apache/spark/graphx/Graph;", "apply"),
			}));

	public boolean isSparkAction(Signature method) {
		return sparkActionSignatures.contains(method);
	}

	public boolean isSparkTransformation(Signature method) {
		return sparkTransformationSignatures.contains(method);
	}
	
	public boolean isSparkRootRddMethod(Signature method) {
		return sparkRootRddMethodSignatures.contains(method);
	}
	
	public boolean isSparkMethod(Signature method) {
		return isSparkAction(method) || isSparkRootRddMethod(method) || isSparkTransformation(method);
	}

	public boolean isSparkActionDots(String classname, String method) {
		return sparkActionSignaturesDots.contains(new SignatureDots(classname, method));
	}

	public boolean isSparkTransformationDots(String classname, String method) {
		return sparkTransformationSignaturesDots.contains(new SignatureDots(classname, method));
	}
	
	public boolean isSparkRootRddMethodDots(String classname, String method) {
		return sparkRootRddMethodSignaturesDots.contains(new SignatureDots(classname, method));
	}
	
	public boolean isSparkMethodDots(String classname, String method) {
		return isSparkActionDots(classname, method) || isSparkRootRddMethodDots(classname, method) || isSparkTransformationDots(classname, method);
	}
}
