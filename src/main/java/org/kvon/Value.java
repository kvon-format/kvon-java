package org.kvon;

import java.util.ArrayList;
import java.util.HashMap;

public interface Value {
	class Primitive implements Value {
		public PrimitiveValue primitive;

		public Primitive(PrimitiveValue primitive) {
			this.primitive = primitive;
		}

		@Override
		public boolean isPrimitive() {
			return true;
		}

		@Override
		public PrimitiveValue getPrimitive() {
			return primitive;
		}
	}

	class Obj implements Value {
		public HashMap<String, Value> obj;

		public Obj(HashMap<String, Value> obj) {
			this.obj = obj;
		}

		@Override
		public boolean isObject() {
			return true;
		}

		@Override
		public HashMap<String, Value> getObject() {
			return obj;
		}
	}

	class Arr implements Value {
		public ArrayList<Value> arr;

		public Arr(ArrayList<Value> arr) {
			this.arr = arr;
		}

		@Override
		public boolean isArray() {
			return true;
		}

		@Override
		public ArrayList<Value> getArray() {
			return arr;
		}
	}

	default boolean isPrimitive() {
		return false;
	}

	default boolean isObject() {
		return false;
	}

	default boolean isArray() {
		return false;
	}

	default PrimitiveValue getPrimitive() {
		return null;
	}

	default HashMap<String, Value> getObject() {
		return null;
	}

	default ArrayList<Value> getArray() {
		return null;
	}

	static Value primitive(PrimitiveValue primitive) {
		return new Primitive(primitive);
	}

	static Value obj(HashMap<String, Value> obj) {
		return new Obj(obj);
	}

	static Value arr(ArrayList<Value> arr) {
		return new Arr(arr);
	}

	static Value number(float number) {
		return primitive(new PrimitiveValue.Number(number));
	}

	static Value text(String s) {
		return primitive(new PrimitiveValue.Text(s));
	}

	static Value bool(boolean b) {
		return primitive(new PrimitiveValue.Boolean(b));
	}

	static Value none() {
		return primitive(new PrimitiveValue.None());
	}

	static Value keyValuePair(String key, Value value) {
		HashMap<String, Value> m = new HashMap<>();
		m.put(key, value);
		return obj(m);
	}
}
