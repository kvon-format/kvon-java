package org.kvon.encoders;

import org.kvon.Indention;
import org.kvon.PrimitiveValue;
import org.kvon.Value;

import java.util.*;

/// Encodes a [value::Value] into a string. This implementation will prefer to
/// expand arrays and strings to multiple lines to improve readability.
public class ExpandedEncoder {

	private interface EncodedValue {
		static EncodedValue mls_from_str(String s) {
			return new MultiLineString(s);
		}

		static EncodedValue inlined(String s) {
			return new Inlined(s);
		}

		static EncodedValue obj(HashMap<String, EncodedValue> values) {
			return new Obj(values);
		}

		static EncodedValue multi_line_array(ArrayList<EncodedValue> values) {
			return new MultiLineArray(values);
		}

		static EncodedValue inline_array(ArrayList<EncodedValue> values) {
			return new InlinedArray(values);
		}

		static EncodedValue fromPrimitive(PrimitiveValue p) {
			if (p.isNumber()) {
				return inlined("" + p.getNumber());
			}

			if (p.isBoolean()) {
				return inlined("" + p.getBoolean());
			}

			if (p.isText()) {
				String s = p.getText();
				if (should_be_multi_line(s)) {
					return mls_from_str(s);
				} else {
					return inlined("'" + s + "'");
				}
			}

			if (p.isNone()) {
				return inlined("null");
			}

			throw new RuntimeException();
		}

		static EncodedValue fromValue(Value v) {
			if (v.isPrimitive()) {
				PrimitiveValue p = v.getPrimitive();
				return fromPrimitive(p);
			}

			if(v.isArray()) {
				// encode all values
				ArrayList<EncodedValue> encoded = new ArrayList<>();
				for (Value kvonValue: v.getArray())
					encoded.add(fromValue(kvonValue));

				// check if at least one of the variables is not inlined
				boolean has_non_inlined = false;
				for (EncodedValue encodedValue: encoded) {
					if (!encodedValue.is_inlined()) {
						has_non_inlined = true;
						break;
					}
				}

				// if there is a non inlined variable, then create a multi
				// line array, otherwise create an inlined array
				if (has_non_inlined) {
					return multi_line_array(encoded);
				} else {
					return inline_array(encoded);
				}
			}

			if (v.isObject()){
				// encode all values
				HashMap<String, EncodedValue> encoded = new HashMap<>();
				for (Map.Entry<String, Value> entry: v.getObject().entrySet()) {
					String key = entry.getKey();
					Value value = entry.getValue();
					encoded.put(key, fromValue(value));
				}

				// construct object
				return obj(encoded);
			}

			throw new RuntimeException();
		}

		boolean is_multi_line_array();

		boolean is_inlined();
	}


	private static class Inlined implements EncodedValue {
		String s;

		public Inlined(String s) {
			this.s = s;
		}

		@Override
		public boolean is_multi_line_array() {
			return false;
		}

		@Override
		public boolean is_inlined() {
			return true;
		}
	}

	private static class MultiLineString implements EncodedValue {
		ArrayDeque<String> lines;

		public MultiLineString(String s) {
			lines = new ArrayDeque<>();
			Collections.addAll(lines, s.split("\n\r?"));
		}

		@Override
		public boolean is_multi_line_array() {
			return false;
		}

		@Override
		public boolean is_inlined() {
			return false;
		}
	}

	private static class Obj implements EncodedValue {
		HashMap<String, EncodedValue> values;

		public Obj(HashMap<String, EncodedValue> values) {
			this.values = values;
		}

		@Override
		public boolean is_multi_line_array() {
			return false;
		}

		@Override
		public boolean is_inlined() {
			return false;
		}
	}

	private static class InlinedArray implements EncodedValue {
		ArrayList<EncodedValue> values;

		public InlinedArray(ArrayList<EncodedValue> values) {
			this.values = values;
		}

		@Override
		public boolean is_multi_line_array() {
			return false;
		}

		@Override
		public boolean is_inlined() {
			return false;
		}
	}

	private static class MultiLineArray implements EncodedValue {
		ArrayList<EncodedValue> values;

		public MultiLineArray(ArrayList<EncodedValue> values) {
			this.values = values;
		}

		@Override
		public boolean is_multi_line_array() {
			return true;
		}

		@Override
		public boolean is_inlined() {
			return false;
		}
	}

	private static boolean should_be_multi_line(String s) {
		return s.contains("'") | s.contains("\"") | s.contains("\n");
	}

	private static void encode_indent(ArrayDeque<StringBuilder> lines, String indent_str, int indent) {
		for (int i = 0; i < indent; ++i) {
			lines.getLast().append(indent_str);
		}
	}

	private static void encoded_to_lines(String indent_str, ArrayDeque<StringBuilder> lines, int indent, EncodedValue v) {
		if (v.is_inlined()) {
			Inlined inlined = (Inlined) v;
			lines.getLast().append(inlined.s);
		}

		if (v instanceof MultiLineString) {
			MultiLineString mls = (MultiLineString) v;
			lines.getLast().append("|");
			for (String line: mls.lines) {
				lines.addLast(new StringBuilder());
				encode_indent(lines, indent_str, indent);
				lines.getLast().append(line);
			}
		}

		if (v instanceof Obj) {
			Obj obj = (Obj) v;

			for (Map.Entry<String, EncodedValue> entry: obj.values.entrySet()) { // (key, value) in v {
				String key = entry.getKey();
				EncodedValue value = entry.getValue();

				lines.addLast(new StringBuilder());

				encode_indent(lines, indent_str, indent);

				// for readability, if the next value is a multi line array,
				// don't add a space after the colon
				if (value.is_multi_line_array()) {
					lines.getLast().append(key).append(":");
				} else {
					lines.getLast().append(key).append(": ");
				}

				// encode the value
				encoded_to_lines(indent_str, lines, indent + 1, value);
			}
		}

		if (v instanceof InlinedArray) {
			ArrayList<EncodedValue> arr = ((InlinedArray) v).values;

			lines.getLast().append("[");
			if (arr.size() > 0) {
				Iterator<EncodedValue> iter = arr.iterator();
				encoded_to_lines(indent_str, lines, indent, iter.next());

				while(iter.hasNext()) {
					lines.getLast().append(" ");
					encoded_to_lines(indent_str, lines, indent, iter.next());
				}
			}
			lines.getLast().append("]");
		}

		if (v instanceof MultiLineArray) {
			ArrayList<EncodedValue> arr = ((MultiLineArray) v).values;

			lines.getLast().append("--");

			for (EncodedValue encodedValue: arr) {
				lines.addLast(new StringBuilder());
				encode_indent(lines, indent_str, indent);

				if (!(encodedValue instanceof MultiLineArray)) {
					lines.getLast().append("- ");
				}

				encoded_to_lines(indent_str, lines, indent + 1, encodedValue);
			}
		}
	}


	public static String encode(Value v, Indention indention) {
		// convert indention to string

		StringBuilder indent_str = new StringBuilder();
		if (indention.isSpaces()) {
			int count = ((Indention.Spaces) indention).count;
			for (int i = 0; i < count; i++)
				indent_str.append(" ");
		}
		if (indention.isTabs()) {
			indent_str.append("\t");
		}

		// encode value
		EncodedValue encoded = EncodedValue.fromValue(v);

		// convert to lines
		ArrayDeque<StringBuilder> lines = new ArrayDeque<>();
		encoded_to_lines(indent_str.toString(), lines, 0, encoded);

		// join lines
		StringBuilder out = new StringBuilder();
		for (StringBuilder sb: lines) {
			System.out.println(sb.toString());
			//out.append(sb.toString()).append("\n");
		}
		return out.toString();
	}
}
