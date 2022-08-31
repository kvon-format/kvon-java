package org.kvon;

public interface PrimitiveValue {
	class Number implements PrimitiveValue {
		public float number;

		public Number(float number) {
			this.number = number;
		}

		@Override
		public boolean isNumber() {
			return true;
		}

		@Override
		public Float getNumber() {
			return number;
		}
	}

	class Text implements PrimitiveValue {
		public String text;

		public Text(String text) {
			this.text = text;
		}

		@Override
		public boolean isText() {
			return true;
		}

		@Override
		public String getText() {
			return text;
		}
	}

	class Boolean implements PrimitiveValue {
		public boolean b;

		public Boolean(boolean b) {
			this.b = b;
		}

		@Override
		public boolean isBoolean() {
			return true;
		}

		@Override
		public java.lang.Boolean getBoolean() {
			return b;
		}
	}

	class None implements PrimitiveValue {
		@Override
		public boolean isNone() {
			return PrimitiveValue.super.isNone();
		}
	}

	default boolean isNumber() { return false; }

	default boolean isText() { return false; }

	default boolean isBoolean() { return false; }

	default boolean isNone() { return false; }

	default Float getNumber() { return null; }

	default String getText() { return null; }

	default java.lang.Boolean getBoolean() { return null; }

	static PrimitiveValue number(float number) {
		return new Number(number);
	}

	static PrimitiveValue text(String s) {
		return new Text(s);
	}

	static PrimitiveValue bool(boolean b) {
		return new Boolean(b);
	}

	static PrimitiveValue none() {
		return new None();
	}
}
