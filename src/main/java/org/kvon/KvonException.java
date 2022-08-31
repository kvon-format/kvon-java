package org.kvon;

public class KvonException extends Exception {
	public static class KvonBaseExceptionType { }

	public static class UnexpectedCharacter extends KvonBaseExceptionType {}
	public static class UnclosedString extends KvonBaseExceptionType {}
	public static class Expected extends KvonBaseExceptionType {
		final String expected;

		public Expected(String expected) {
			this.expected = expected;
		}
	}
	// indention
	public static class InconsistentIndention extends KvonBaseExceptionType {
		public final Indention expected, found;

		public InconsistentIndention(Indention expected, Indention found) {
			this.expected = expected;
			this.found = found;
		}
	}
	public static class InvalidIndention extends KvonBaseExceptionType {}
	public static class MultipleTabIndent extends KvonBaseExceptionType {}
	public static class MixedTabsAndSpaces extends KvonBaseExceptionType {}
	public static class SpacesNotMultipleOfIndent extends KvonBaseExceptionType {}

	public final int lineNumber;
	public final int columnNumber;
	public final String line;
	public final KvonBaseExceptionType type;

	public KvonException(int lineNumber, int columnNumber, String line, KvonBaseExceptionType type) {
		this.lineNumber = lineNumber;
		this.columnNumber = columnNumber;
		this.line = line;
		this.type = type;
	}
}
