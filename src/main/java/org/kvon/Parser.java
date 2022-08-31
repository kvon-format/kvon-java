package org.kvon;

import jdk.nashorn.internal.runtime.ParserException;
import org.kvon.context.MultiLineStringContext;
import org.kvon.context.ParsingContext;

import javax.naming.OperationNotSupportedException;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Stack;

public class Parser {
	int lineNumber;
	Indention indention;
	Stack<ParsingContext> contextStack;

	public Parser() {
		contextStack = new Stack<>();
		contextStack.push(ParsingContext.objectContext(0, ""));
		lineNumber = 0;
		indention = null;
	}

	/// Calculates the indent and auto detects it if it has not been set yet.
	int calculateIndent(
		LineParser lineParser,
		int tabsCount,
		int spacesCount
		) throws KvonException {
		if (tabsCount > 0 || spacesCount > 0) {
			// mixed tabs and spaces are not allowed
			if (tabsCount > 0 && spacesCount > 0)
				throw lineParser.generateError(new KvonException.MixedTabsAndSpaces());

			// calculate the indent level
			if (indention != null) {
				// check that the space and tab count makes a valid indention
				// and return the indent level
				if (indention.isTabs()) {
					if (spacesCount > 0) {
						throw lineParser.generateError(new KvonException.InconsistentIndention(indention, Indention.spaces(spacesCount)));
					} else {
						return tabsCount;
					}
				}

				if (indention.isSpaces()) {
					int spaces = ((Indention.Spaces) indention).count;

					if (spacesCount > 0) {
						if (spacesCount % spaces == 0) {
							throw lineParser.generateError(new KvonException.SpacesNotMultipleOfIndent());
						} else {
							return spacesCount / spaces;
						}
					} else {
						throw lineParser.generateError(new KvonException.InconsistentIndention(
								indention,
								Indention.tabs()
							)
						);
					}
				}

				// this should never happen
				return 0;
			} else {
				// process initial indention
				// set indention to spaces
				if (spacesCount > 0) {
					indention = Indention.spaces(spacesCount);
				}

				// initial indention of more than one tabs is not allowed
				if (tabsCount > 1) {
					throw lineParser.generateError(new KvonException.MultipleTabIndent());
				}

				// set indention to tabs
				indention = Indention.tabs();

				return 1;
			}
		} else {
			return 0;
		}
	}

	/// Removes the top context from the stack and merges it to the context
	/// below it.
	void popStack() {
		// remove the top context
		ParsingContext context = contextStack.pop();

		// add it to the context underneath
		try {
			contextStack
				.lastElement()
				.pushV(context.toValue());
		} catch (OperationNotSupportedException e) {
			throw new RuntimeException(e);
		}
	}

	// Collapses context from the top of the stack until the indent of the top
	// context doesn't exceed the given indent.
	void collapseContextToIndent(int indent) {
		while (contextStack.lastElement().getIndent() > indent)
			popStack();
	}

	/// Collapses all contexts from the stack until only one remains - the root
	/// object context.
	public void collapseContext() {
		collapseContextToIndent(0);
	}

	/// Processes a line whose indention has been consumed in the context of an
	/// object.
	void processPostIndentObject(LineParser lineParser, int indent) throws KvonException {
		// key
		String key = lineParser.parseKey();

		// whitespace
		lineParser.consumeWhitespaces();

		// array
		if (lineParser.have(":--")) {
			if (!lineParser.seeEndOrComment()) {
				throw lineParser.generateError(new KvonException.UnexpectedCharacter());
			}

			// set the key to the current context
			ParsingContext last = contextStack.lastElement();
			try {
				last.setPendingKey(key);
			} catch (OperationNotSupportedException e) {
				throw new RuntimeException(e);
			}

			// push the array context
			contextStack.push(ParsingContext.arrayContext(indent + 1));
			return;
		}

		// object or value
		if (lineParser.have(":")) {
			lineParser.consumeWhitespaces();

			ParsingContext last = contextStack.lastElement();
			try {
				last.setPendingKey(key);
			} catch (OperationNotSupportedException e) {
				throw new RuntimeException(e);
			}

			// object - push a new context
			if (lineParser.seeEndOrComment()) {
				contextStack
					.push(ParsingContext.objectContext(indent + 1, ""));
				return;
			}

			Optional<Value> value;
			Optional<PrimitiveValue> primitive;

			if ((value = lineParser.parseInlineArray()).isPresent()) {
				// inlined array
				try {
					last.pushV(value.get());
				} catch (OperationNotSupportedException e) {
					throw new RuntimeException(e);
				}
			} else if ( (primitive = lineParser.parsePrimitive()).isPresent() ) {
				// value
				try {
					last.pushV(Value.primitive(primitive.get()));
				} catch (OperationNotSupportedException e) {
					throw new RuntimeException(e);
				}
			} else if(lineParser.have("|") ){
				// multi-line string
				contextStack.push(ParsingContext.multiLineStringContext(indent + 1));
			}

			// expected to reach end of line
			if (lineParser.seeEndOrComment()) {
				return;
			} else {
				throw lineParser.generateError(new KvonException.UnexpectedCharacter());
			}
		}

		// if found something other than the end of line or a comment,
		// return an error
		if (!lineParser.seeEndOrComment()) {
			throw lineParser.generateError(new KvonException.UnexpectedCharacter());
		}

		try {
			contextStack
				.lastElement()
				.pushKV(key, Value.none());
		} catch (OperationNotSupportedException e) {
			throw new RuntimeException(e);
		}
	}

	/// Processes a line whose indention has been consumed in the context of an
	/// array.
	void processPostIndentArray(LineParser lineParser, int indent) throws KvonException {
		// sub array
		if (lineParser.have("--")) {
			if (!lineParser.seeEndOrComment()) {
				throw lineParser.generateError(new KvonException.UnexpectedCharacter());
			}
			contextStack.push(ParsingContext.arrayContext(indent + 1));
			return;
		}

		// array entries must start with `-`
		if (!lineParser.have("-")) {
			throw lineParser.generateError(new KvonException.Expected("-"));
		}
		lineParser.consumeWhitespaces();

		// object with more than one key
		if (lineParser.seeEndOrComment()) {
			contextStack.push(ParsingContext.objectContext(indent + 1, ""));
			return;
		}

		// object with one key
		String key = lineParser.parseKeyWithColon();
		if (key.length() > 0) {
			lineParser.consumeWhitespaces();

			ParsingContext last = contextStack.lastElement();

			// object context with single root
			if (lineParser.seeEndOrComment()) {
				contextStack.push(ParsingContext.objectContext(indent + 1, key));
				contextStack.push(ParsingContext.objectContext(indent + 1, ""));
				return;
			}

			Optional<Value> value;
			Optional<PrimitiveValue> primitive;

			if ((value = lineParser.parseInlineArray()).isPresent()) {
				// inlined array
				try {
					last.pushV(Value.keyValuePair(key, value.get()));
				} catch (OperationNotSupportedException e) {
					throw new RuntimeException(e);
				}
			} else if ((primitive = lineParser.parsePrimitive()).isPresent()) {
				// primitive
				try {
					last.pushV(Value.keyValuePair(key, Value.primitive(primitive.get())));
				} catch (OperationNotSupportedException e) {
					throw new RuntimeException(e);
				}
			} else if (lineParser.have("|")) {
				// object context with single root and multi line string value
				contextStack.push(ParsingContext.objectContext(indent + 1, key));
				contextStack.push(ParsingContext.multiLineStringContext(indent + 1));
			}

			// expected to reach end of line
			if (lineParser.seeEndOrComment()) {
				return;
			} else {
				throw lineParser.generateError(new KvonException.UnexpectedCharacter());
			}
		}

		// multi-line string
		if (lineParser.have("|")) {
			contextStack.push(ParsingContext.multiLineStringContext(indent + 1));
			return;
		}

		// iterate over all the values on the line
		while (true) {
			lineParser.consumeWhitespaces();
			if (lineParser.seeEndOrComment()) {
				break;
			}

			Optional<Value> value;
			Optional<PrimitiveValue> primitive;

			// inlined array
			if ((value= lineParser.parseInlineArray()).isPresent()) {
				try {
					contextStack.lastElement().pushV(value.get());
				} catch (OperationNotSupportedException e) {
					throw new RuntimeException(e);
				}
				continue;
			}

			// value
			if ((primitive = lineParser.parsePrimitive()).isPresent()) {
				try {
					contextStack.lastElement().pushV(Value.primitive(primitive.get()));
				} catch (OperationNotSupportedException e) {
					throw new RuntimeException(e);
				}
				continue;
			}

			throw lineParser.generateError(new KvonException.UnexpectedCharacter());
		}

		// if found something other than the end of line or a comment,
		// return an error
		if (!lineParser.seeEndOrComment()) {
			throw lineParser.generateError(new KvonException.UnexpectedCharacter());
		}
	}

	/// Returns true if the line belongs to the multi-line string. Returns false
	/// if it doesn't and the context has been popped or the top context isn't
	/// a multi-line string.
	boolean processMultiLineStringLine(LineParser lineParser) throws KvonException {
		ParsingContext last = contextStack.lastElement();
		int indent = last.getIndent();
		if (last instanceof MultiLineStringContext) {
			MultiLineStringContext mls = (MultiLineStringContext) last;
			ArrayList<String> lines = mls.lines;

			// if the indention isn't defined yet, analyze the line and define
			// it.
			if (indention != null) {
				// consume the leading indention
				if (!lineParser.haveIndentions(indention, indent)) {
					// there weren't enough leading indents - the multi line
					// string ended.
					popStack();
					return false;
				}
			} else {
				// analyzing the first indention in the entire file
				if (lineParser.have("\t")) {
					// since indentions cannot be multiple tabs, if the first
					// seen character is a tab, then the indention must be a tab
					indention = Indention.tabs();
				} else {
					// parse whitespaces
					LineParser.IndentCount count = lineParser.nextWhitespaces();

					// mixed tabs and spaces are not allowed
					if (count.tabs > 0 && count.spaces > 0) {
						throw lineParser.generateError(new KvonException.MixedTabsAndSpaces());
					}

					// no indentions
					if (count.spaces == 0) {
						popStack();
						return false;
					}

					// set the indention to the counted spaces
					indention = Indention.spaces(count.spaces);
				}
			}

			// the rest of the line belongs to the screen
			lines.add(lineParser.consumeRest());
			return true;
		} else {
			return false;
		}
	}

	/// Calculates indention and then calls any of the `process_post_indent`
	/// methods.
	void processLine(String line) throws KvonException {
		// wrap the line in a line parser
		LineParser lineParser = new LineParser(lineNumber, line);

		// handle multi-line strings
		if (processMultiLineStringLine(lineParser)) {
			return;
		}

		// check if line has no content
		if (lineParser.seeEndOrComment()) {
			return;
		}

		// parse whitespaces
		LineParser.IndentCount count = lineParser.nextWhitespaces();

		// calculate indent level
		int indent = calculateIndent(lineParser, count.tabs, count.spaces);

		// calculate the maximum indent the next item is allowed to be in
		int maxIndent = 0;
		if (contextStack.size() > 0) {
			maxIndent = contextStack.lastElement().getIndent();
		}

		// if the indent is invalid, return an error
		if (indent > maxIndent) {
			throw lineParser.generateError(new KvonException.InvalidIndention());
		}

		// pop contexts to match the indent
		collapseContextToIndent(indent);

		// if the top context is an object, handle the rest of the line as an
		// object's line
		if (contextStack.lastElement().isObjectContext()) {
			processPostIndentObject(lineParser, indent);
			return;
		}

		// if the top context is an array, handle the rest of the line as an
		// array's line
		if (contextStack.lastElement().isArrayContext()) {
			processPostIndentArray(lineParser, indent);
		}
	}

	/// Parses another line.
	public void nextLine(String line) throws KvonException {
		processLine(line);
		lineNumber += 1;
	}

	/// Parses a string into a [value::Value].
	public static Value parseString(String s) throws KvonException {
		Parser parser = new Parser();
		for (String line: s.split("\n\r?")) {
			parser.nextLine(line);
		}

		parser.collapseContext();

		try {
			return Value.obj(parser.contextStack.lastElement().getObjects());
		} catch (OperationNotSupportedException e) {
			throw new RuntimeException(e);
		}
	}
}
