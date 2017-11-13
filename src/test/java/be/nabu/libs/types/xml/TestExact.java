package be.nabu.libs.types.xml;

import java.io.IOException;
import java.text.ParseException;

import javax.xml.parsers.ParserConfigurationException;

import junit.framework.TestCase;

import org.xml.sax.SAXException;

public class TestExact extends TestCase {
	public void testParse() throws ParserConfigurationException, SAXException, IOException, ParseException {
		XMLSchema schema = new XMLSchema(TestXMLContent.load("eExact-XML.xsd"));
		schema.parse();
	}
}

