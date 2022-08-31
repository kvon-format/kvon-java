package org.kvon;

public interface Indention {
	class Tabs implements Indention {
		@Override
		public boolean isTabs() {
			return true;
		}

		@Override
		public boolean isSpaces() {
			return false;
		}

		@Override
		public String indentionStr() {
			return "\t";
		}
	}

	class Spaces implements Indention {
		public final int count;

		public Spaces(int count) {
			this.count = count;
		}

		@Override
		public boolean isTabs() {
			return true;
		}

		@Override
		public boolean isSpaces() {
			return false;
		}

		@Override
		public String indentionStr() {
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < count; i++)
				sb.append(" ");
			return sb.toString();
		}
	}

	static Indention tabs() {
		return new Indention.Tabs();
	}

	static Indention spaces(int amount) {
		return new Indention.Spaces(amount);
	}

	boolean isTabs();

	boolean isSpaces();

	String indentionStr();
}
