package org.kvon.context;

import org.kvon.PrimitiveValue;
import org.kvon.Value;

import javax.naming.OperationNotSupportedException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class MultiLineStringContext extends ParsingContext {
	public ArrayList<String> lines;

	public MultiLineStringContext(int indent, ArrayList<String> lines) {
		this.indent = indent;
		this.lines = lines;
	}

	@Override
	public boolean isObjectContext() {
		return false;
	}

	@Override
	public boolean isArrayContext() {
		return false;
	}

	@Override
	public boolean isMultiLineStringContext() {
		return true;
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
	public void pushV(Value value) throws OperationNotSupportedException {
		throw new OperationNotSupportedException();
	}

	@Override
	public void pushKV(String key, Value value) throws OperationNotSupportedException {
		throw new OperationNotSupportedException();
	}

	@Override
	public Value toValue() {
		return Value.primitive(PrimitiveValue.text(joinLines()));
	}

	public String joinLines() {
		Iterator<String> iter = lines.iterator();

		if (!iter.hasNext())
			return "";

		StringBuilder sb = new StringBuilder();
		sb.append(iter.next());
		while (iter.hasNext())
			sb.append("\n").append(iter.next());
		return sb.toString();
	}

	public void pushLine(String line) {
		lines.add(line);
	}
}
