package edu.upenn.cis.cis455.xpathengine;

/**
 This class encapsulates the tokens we care about parsing in XML (or HTML)
 */
public class OccurrenceEvent {
	enum Type {Open, Close, Text};
	
	Type type;
	String value;
	
	public OccurrenceEvent(Type t, String value) {
		this.type = t;
		this.value = value;
	}

	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}
	

	public String toString() {
		if (type == Type.Open) 
			return "<" + value + ">";
		else if (type == Type.Close)
			return "</" + value + ">";
		else
			return value;
	}
}
