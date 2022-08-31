package org.kvon;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LineParser {
	private static class State {
		int i;
		String left;

		State(int i, String left) {
			this.i = i;
			this.left = left;
		}
	}

	public static class IndentCount {
		public int spaces;
		public int tabs;

		public IndentCount(int spaces, int tabs) {
			this.spaces = spaces;
			this.tabs = tabs;
		}

		public static IndentCount zero() {
			return new IndentCount(0, 0);
		}
	}

	private final int lineNumber;
	private final String line;
	private State state;
	private final Stack<State> recorded;

	public LineParser(int lineNumber, String line) {
		this.lineNumber = lineNumber;
		this.line = line;
		this.state = new State(0, line);
		recorded = new Stack<>();
	}

	public KvonException generateError(KvonException.KvonBaseExceptionType type) {
		return new KvonException(lineNumber, state.i, line, type);
	}

	/// Return the remaining str of the line.
	public String consumeRest() {
		String ret = this.state.left;
		this.state.i = this.line.length();
		this.state.left = "";

		return ret;
	}

	/// Record the current state of the line parser.
	private void record() {
		this.recorded.push(new State(this.state.i, this.state.left));
	}

	/// Restore the last recorded state of the line parser.
	private void restore() {
		this.state = this.recorded.pop();
	}

	/// Remove the last recorded state without changing the current one.
	private void cancelRestore() {
		this.recorded.pop();
	}

	/// Returns whether or not the end of the line has been reached.
	public boolean reached_end() {
		return this.state.left.length() == 0;
	}

	/// Returns true if the remaining part of the line starts with `s`.
	public boolean see(String s){
		return this.state.left.startsWith(s);
	}

	/// If sees `s` returns true and advances the parser by the length of `s`.
	/// Otherwise returns false.
	public boolean have(String s) {
		if (see(s)) {
			state.i += s.length();
			state.left = this.state.left.substring(s.length());
			return true;
		} else {
			return false;
		}
	}

	public boolean seeAny(List<String> strings) {
		for (String s: strings) {
			if (see(s)) {
				return true;
			}
		}
		return false;
	}

	public boolean seeAny(String[] strings) {
		for (String s: strings) {
			if (see(s)) {
				return true;
			}
		}
		return false;
	}

	public boolean seeEndOrComment() {
		String left = trimStart(state.left);
		return left.length() == 0 || left.startsWith("#");
	}

	/// Consumes a single character.
	public void advance() {
		this.state.left = this.state.left.substring(1);
		this.state.i += 1;
	}

	/// Consumes `amount` of characters.
	public void advance_by(int amount) {
		this.state.left = this.state.left.substring(amount);
		this.state.i += amount;
	}

	/// Consumes the whitespaces and returns the tuple
	/// (tabs count, spaces count).
	public IndentCount nextWhitespaces() {
		IndentCount indentCount = IndentCount.zero();

		// counts how many tabs and spaces were seen until the next non
		// whitespace character, or the end of the file
		while (this.state.left.length() > 0) {
			if (this.state.left.startsWith(" ")) {
				indentCount.spaces += 1;
				this.advance();
			} else if (this.state.left.startsWith("\t")) {
				indentCount.tabs += 1;
				this.advance();
			} else {
				break;
			}
		}
		return indentCount;
	}

	/// Advances past all the leading whitespaces.
	public void consumeWhitespaces() {
		int startLen = this.state.left.length();
		this.state.left = trimStart(this.state.left);
		this.state.i += startLen - this.state.left.length();
	}

	// helper function for `parse_string_literal`
	private String parseStringLiteralWith(String escape) throws KvonException {
		int start = this.state.i;
		while (true) {
			if (this.reached_end()) {
				throw generateError(new KvonException.UnclosedString());
			}

			if (see(escape)) {
				String s = this.line.substring(start, this.state.i);
				advance_by(escape.length());
				return s;
			}

			this.advance();
		}
	}

	/// Tries parsing a string literal, returns `None` if no literal found.
	/// Returns and error if the string literal is invalid.
	public Optional<String> parseStringLiteral() throws KvonException {
		if (see("'")) {
			int start = this.state.i;
			while (this.have("'"));
			String escape = this.line.substring(start, this.state.i);

			return Optional.of(this.parseStringLiteralWith(escape));
		} else if (see("\"")) {
			int start = this.state.i;
			while (this.have("\""));
			String escape = this.line.substring(start, this.state.i);

			return Optional.of(this.parseStringLiteralWith(escape));
		} else {
			return Optional.empty();
		}
	}

	public String parseKey() throws KvonException {
		Optional<String> literal = parseStringLiteral();
		if (literal.isPresent()) {
			return literal.get();
		} else {
			int startLen = this.state.left.length();
			String source = this.state.left;

			while (state.left.length() > 0) {
				if (!seeAny(new String[]{" ", "\t", ":", "#", ";"})) {
					advance();
				} else {
					break;
				}
			}

			return source.substring(0, startLen - this.state.left.length());
		}
	}

	public String parseKeyWithColon() throws KvonException {
		this.record();

		String key = this.parseKey();

		this.consumeWhitespaces();
		if (have(":")) {
			this.cancelRestore();
			return key;
		} else {
			this.restore();
			return "";
		}
	}

	public Optional<Float> parseNumericalLiteral() {
		String r = "^(-?[0-9]*(?:\\.[0-9]+)?).*$";
		Pattern pattern = Pattern.compile(r);

		// if the regex captures, and the the value can be unwrapped, advance
		// and return
		Matcher m = pattern.matcher(this.state.left);
		if (m.find()) {
			if (m.groupCount() > 0) {
				String value = m.group(1);
				this.advance_by(value.length());
				try {
					return Optional.of(Float.parseFloat(value));
				} catch (NumberFormatException ignored) {
				}
			}
		}

		return Optional.empty();
	}

	public Optional<Boolean> parseBooleanLiteral() {
		if (have("true")) {
			return Optional.of(true);
		} else if (have("false")) {
			return Optional.of(false);
		} else {
			return Optional.empty();
		}
	}

	public boolean parseNullLiteral() {
		return have("null");
	}

	/// Helper for `parse_inline_array`
	private Value nextInlineArray() throws KvonException {
		ArrayList<Value> values = new ArrayList<>();
		while(true) {
			consumeWhitespaces();

			// end of array
			if (have("]") ){
				break;
			}

			// new sub array
			if (have("[")) {
				values.add(nextInlineArray());
				continue;
			}

			// next value
			Optional<PrimitiveValue> p = parsePrimitive();
			if (p.isPresent()) {
				values.add(Value.primitive(p.get()));
				continue;
			}

			throw generateError(new KvonException.UnexpectedCharacter());
		}

		return Value.arr(values);
	}

	public Optional<Value> parseInlineArray() throws KvonException {
		if (have("[")) {
			return Optional.of(this.nextInlineArray());
		} else {
			return Optional.empty();
		}
	}

	public Optional<PrimitiveValue> parsePrimitive() throws KvonException {
		Optional<String> text;
		Optional<Float> number;
		Optional<Boolean> bool;

		if ((text = parseStringLiteral()).isPresent()) {
			return Optional.of(PrimitiveValue.text(text.get()));
		} else if ((number = parseNumericalLiteral()).isPresent()) {
			return Optional.of(PrimitiveValue.number(number.get()));
		} else if ((bool = parseBooleanLiteral()).isPresent()) {
			return Optional.of(PrimitiveValue.bool(bool.get()));
		} else if (parseNullLiteral()) {
			return Optional.of(PrimitiveValue.none());
		} else {
			return Optional.empty();
		}
	}

	/// Helper for `haveIndentions`
	private boolean haveIndentionsHelper(Indention indention, int amount) {
		if (indention instanceof Indention.Tabs) {
			for (int i = 0; i < amount; ++i) {
				if (see(" ")) {
					return false;
				}

				if (!have("\t")) {
					return false;
				}
			}
		}

		if (indention instanceof Indention.Spaces) {
			final Indention.Spaces spaces = (Indention.Spaces) indention;
			int spacesCount = spaces.count;

			for (int i = 0; i < amount; ++i) {
				for (int j = 0; j < spacesCount; ++j) {
					if (see("\t")) {
						return false;
					}
					if (!have(" ")) {
						return false;
					}
				}
			}
		}

		return true;
	}

	/// If sees a specific amount of a certain indention, returns true and
	/// consumes it. Otherwise returns false.
	public boolean haveIndentions(Indention indention, int amount) {
		this.record();
		if (haveIndentionsHelper(indention, amount) ){
			this.cancelRestore();
			return true;
		} else {
			this.restore();
			return false;
		}
	}










	private static String trimStart(String s) {
		while (s.startsWith(" ") || s.startsWith("\t"))
			s = s.substring(1);
		return s;
	}
}
