package be.nabu.libs.types.xml;

import java.io.IOException;
import java.text.ParseException;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import junit.framework.TestCase;

public class TestSchema extends TestCase {
	public void testParse() throws ParserConfigurationException, SAXException, IOException, ParseException {
		XMLSchema schema = new XMLSchema(TestXMLContent.load("parseTest.xml"));
		schema.parse();
	}
}
