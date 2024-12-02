package org.evosuite.coverage.seepep;

public abstract class SeepepTraceItem {
	private String identifier;
	private final Object value;
	
	private SeepepTraceItem(String identifier, Object value) {
		this.identifier = identifier;
		this.value = value;
	}
	
	public static SeepepTraceItem makeTraceStartMarker() {
		return new SeepepTraceStartMarker();
	}

	public String getIdentifier() {
		return identifier;
	}
	
	public Object getValue() {
		return value;
	}

	public void rename(String newIdentifier) {
		identifier = newIdentifier;
	}
	
	public static SeepepTraceItem makeInputParameter(int parameterNum, Object value) {
		return new SeepepInputParameter(parameterNum, value);
	}

	public static SeepepTraceItem makeAction(String action, Object retVal) {
		return new SeepepAction(action, retVal);
	}

	public static SeepepTraceItem makeTransformation(String action, int retValSize) {
		return new SeepepTransformation(action, retValSize);
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + "[" + (identifier == null ? "" : identifier) + "=" + (value == null ? "" : value) + "]";
	}
	
	public boolean isTraceStartMarker() {
		return (this instanceof SeepepTraceStartMarker);
	}

	public boolean isInputParameter() {
		return (this instanceof SeepepInputParameter);
	}

	public boolean isAction() {
		return (this instanceof SeepepAction);
	}
	
	public boolean isTransformation() {
		return (this instanceof SeepepTransformation);
	}

	private static class SeepepTraceStartMarker extends SeepepTraceItem {
		public SeepepTraceStartMarker() {
			super(null, null);
		}
	}

	private static class SeepepInputParameter extends SeepepTraceItem {
		public SeepepInputParameter(int parameterNum, Object value) {
			super("arg" + parameterNum, value);
		}
	}

	private static class SeepepAction extends SeepepTraceItem {
		public SeepepAction(String action, Object retVal) {
			super(action, retVal);
		}
	}

	private static class SeepepTransformation extends SeepepTraceItem {
		public SeepepTransformation(String transformation, int retValSize) {
			super(transformation, retValSize);
		}
	}
}
