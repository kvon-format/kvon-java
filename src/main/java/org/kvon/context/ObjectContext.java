package org.kvon.context;

import org.kvon.Value;

import javax.naming.OperationNotSupportedException;
import java.util.HashMap;

class ObjectContext extends ParsingContext {
	HashMap<String, Value> values;
	String pendingKey;

	public ObjectContext(int indent, HashMap<String, Value> values, String pendingKey) {
		this.indent = indent;
		this.values = values;
		this.pendingKey =pendingKey;
	}

	@Override
	public boolean isObjectContext() {
		return true;
	}

	@Override
	public boolean isArrayContext() {
		return false;
	}

	@Override
	public boolean isMultiLineStringContext() {
		return false;
	}

	@Override
	public HashMap<String, Value> getObjects() {
		return values;
	}

	@Override
	public void setPendingKey(String pendingKey) throws OperationNotSupportedException {
		this.pendingKey = pendingKey;
	}

	@Override
	public void pushV(Value value) throws OperationNotSupportedException {
		values.put(pendingKey, value);
		pendingKey = "";
	}

	@Override
	public void pushKV(String key, Value value) {
		values.put(key, value);
		pendingKey = "";
	}

	@Override
	public Value toValue() {
		return Value.obj(values);
	}
}
