package org.kvon;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.kvon.encoders.ExpandedEncoder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;

public class ParseTest extends TestCase {
	public ParseTest(String testName ) {
		super( testName );
	}

	public static Test suite() {
		return new TestSuite(ParseTest.class);
	}

	public void testFloatParsing() {
		LineParser lineParser = new LineParser(0, "1.1 abc");
		Optional<Float> literal = lineParser.parseNumericalLiteral();
		assertTrue(literal.isPresent());
		assertEquals(literal.get(), 1.1f);
		assertTrue(true);
	}

	public void testMixedObjectsArrays() {
		// create the test object
		StringBuilder sb = new StringBuilder();
		sb.append("a:--").append("\n");
		sb.append("\t").append("- 1 2 3").append("\n");
		sb.append("\t").append("- 4 5 6").append("\n");
		sb.append("\t").append("- b:").append("\n");
		sb.append("\t").append("\t").append("c: 1").append("\n");
		sb.append("\t").append("\t").append("d: 2").append("\n");
		sb.append("\t").append("--").append("\n");
		sb.append("\t").append("\t").append("- [true false]").append("\n");
		sb.append("\t").append("\t").append("- 'abc'").append("\n");

		// parse
		Value root;
		try {
			root = Parser.parseString(sb.toString());
		} catch (KvonException e) {
			throw new RuntimeException(e);
		}
		assertNotNull(root);

		// check
		assertTrue(root.getObject().containsKey("a"));
		Value a = root.getObject().get("a");
		ArrayList<Value> aArray = a.getArray();
		assertEquals(aArray.get(5).getPrimitive().getNumber(), 6.0f);

		HashMap<String, Value> obj = aArray.get(6).getObject();
		assertEquals(obj.get("b").getObject().get("d").getPrimitive().getNumber(), 2.0f);

		ArrayList<Value> bArray = aArray.get(7).getArray();
		assertEquals(bArray.get(0).getArray().get(1).getPrimitive().getBoolean(), Boolean.FALSE);
	}
}
