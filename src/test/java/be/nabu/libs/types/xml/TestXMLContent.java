package be.nabu.libs.types.xml;

import java.io.IOException;
import java.io.StringWriter;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Date;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import junit.framework.TestCase;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import be.nabu.types.evaluator.EvaluationException;
import be.nabu.types.evaluator.Operation;
import be.nabu.types.evaluator.PathAnalyzer;
import be.nabu.types.evaluator.QueryParser;

public class TestXMLContent extends TestCase {
	
	public void testSchemaLessContent() throws ParserConfigurationException, SAXException, IOException, ParseException, EvaluationException {
		Document document = load("myData.xml");
		XMLContent content = new XMLContent(document.getDocumentElement());
		assertEquals("test2", content.get("myRecord[0]/myField2[1]"));
		
		// test evaluation
		Operation operation = PathAnalyzer.analyze(QueryParser.getInstance().parse("myRecord[myField1 > 5]/myField2"));
		assertEquals(Arrays.asList(new String[] { "wee1", "wee2", "wee3" }), operation.evaluate(content));
		
		// test methods
		operation = PathAnalyzer.analyze(QueryParser.getInstance().parse("myRecord[exists(@myIndex)]/@myIndex"));
		assertEquals(Arrays.asList(new String[] { "1" }), operation.evaluate(content));
		
		// nested method
		operation = PathAnalyzer.analyze(QueryParser.getInstance().parse("myRecord[not(exists(@myIndex))]/myField4/myChild1"));
		assertEquals(Arrays.asList(new String[] { "childTest1" }), operation.evaluate(content));
	}
	
	public void testSchemaExample() throws SAXException, ParserConfigurationException, IOException, ParseException {
		XMLSchema schema = new XMLSchema(load("mySchema.xsd"));
		schema.parse();
		XMLContent content = new XMLContent(load("mySchemaExample.xml").getDocumentElement(), schema);
		assertTrue(content.get("someDates[1]") instanceof Date);
		assertTrue(content.get("someRecord/specialDateTime/$value") instanceof Date);
		assertTrue(content.get("someRecord/specialDateTime/@myAttribute") instanceof Integer);
		assertTrue(content.get("someRecord/theOther") instanceof Boolean);
		
		content.set("someDates[5]", new Date());
		
		// should've inserted a null value for 4
		assertNull(content.get("someDates[4]"));
		assertTrue(content.get("someDates[5]") instanceof Date);
	}
	
	public static Document load(String name) throws ParserConfigurationException, SAXException, IOException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		factory.setValidating(false);
		DocumentBuilder builder = factory.newDocumentBuilder();
		return builder.parse(new InputSource(Thread.currentThread().getContextClassLoader().getResourceAsStream(name)));
	}
	
	public static String toString(Node document) {
		try {
			StringWriter writer = new StringWriter();
			TransformerFactory factory = TransformerFactory.newInstance();
			Transformer transformer = factory.newTransformer();
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
			transformer.setOutputProperty(OutputKeys.METHOD, "xml");
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
			transformer.transform(new DOMSource(document), new StreamResult(writer));
			return writer.toString();
		}
		catch (Exception ex) {
			throw new RuntimeException("Error converting to String", ex);
		}
	}
}