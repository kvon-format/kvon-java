package org.kvon.context;

import org.kvon.Value;

import javax.naming.OperationNotSupportedException;
import java.util.ArrayList;
import java.util.HashMap;

public abstract class ParsingContext {
	int indent;

	public static ParsingContext objectContext(int indent, String pendingKey) {
		return new ObjectContext(indent, new HashMap<>(), pendingKey);
	}

	public static ParsingContext arrayContext(int indent) {
		return new ArrayContext(indent, new ArrayList<>());
	}

	public static ParsingContext multiLineStringContext(int indent) {
		return new MultiLineStringContext(indent, new ArrayList<>());
	}

	public abstract boolean isObjectContext();

	public abstract boolean isArrayContext();

	public abstract boolean isMultiLineStringContext();

	public int getIndent() {
		return indent;
	}

	public abstract HashMap<String, Value> getObjects() throws OperationNotSupportedException;

	public abstract void setPendingKey(String pendingKey) throws OperationNotSupportedException;

	public abstract void pushV(Value value) throws OperationNotSupportedException;

	public abstract void pushKV(String key, Value value) throws OperationNotSupportedException;

	public abstract Value toValue();
}
