package org.kvon.context;

import org.kvon.Value;

import javax.naming.OperationNotSupportedException;
import java.util.ArrayList;
import java.util.HashMap;

public class ArrayContext extends ParsingContext {
	ArrayList<Value> values;

	public ArrayContext(int indent, ArrayList<Value> values) {
		this.indent = indent;
		this.values = values;
	}

	@Override
	public boolean isObjectContext() {
		return false;
	}

	@Override
	public boolean isArrayContext() {
		return true;
	}

	@Override
	public boolean isMultiLineStringContext() {
		return false;
	}

	@Override
	public HashMap<String, Value> getObjects() throws OperationNotSupportedException {
		throw new OperationNotSupportedException();
	}

	@Override
	public void setPendingKey(String pendingKey) throws OperationNotSupportedException {
		throw new OperationNotSupportedException();
	}

	@Override
	public void pushV(Value value) {
		values.add(value);
	}

	@Override
	public void pushKV(String key, Value value) throws OperationNotSupportedException {
		throw new OperationNotSupportedException();
	}

	@Override
	public Value toValue() {
		return Value.arr(values);
	}
}
