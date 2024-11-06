/*
* Copyright (C) 2015 Alexander Verbruggen
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU Lesser General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public License
* along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

package be.nabu.libs.types.xml;

import java.io.IOException;
import java.text.ParseException;
import java.util.Date;

import javax.xml.parsers.ParserConfigurationException;

import junit.framework.TestCase;

import org.xml.sax.SAXException;

import be.nabu.libs.types.base.RootElement;
import be.nabu.libs.types.java.BeanInstance;
import be.nabu.libs.types.java.BeanType;

public class TestXMLWithJavaBeans extends TestCase {
	public void testJavaBeans() throws SAXException, ParseException, ParserConfigurationException, IOException {
		XMLContent content = new XMLContent(new RootElement(new BeanType<Test3>(Test3.class), "root"));
		content.set("test", new BeanInstance<Test2>(new Test2("o ye", 666)));
		String xml = TestXMLContent.toString(content.getElement());
		assertTrue(xml.matches("(?s).*<testString>o ye</testString>.*"));
		assertTrue(xml.matches("(?s).*<testDate>.*?</testDate>.*"));
		// it should not contain the int because this is part of the Test2 definition, not of Test1 as defined in Test3
		assertFalse(xml.matches("(?s).*<testInt>.*?</testInt>.*"));
	}	
	
	public static class Test1 {
		private String testString;
		private Date testDate;
		public String getTestString() {
			return testString;
		}
		public void setTestString(String testString) {
			this.testString = testString;
		}
		public Date getTestDate() {
			return testDate;
		}
		public void setTestDate(Date testDate) {
			this.testDate = testDate;
		}
	}
	
	public static class Test2 extends Test1 {
		private int testInt;
		
		public Test2(String testString, int testInt) {
			setTestString(testString);
			setTestInt(testInt);
			setTestDate(new Date());
		}

		public int getTestInt() {
			return testInt;
		}

		public void setTestInt(int testInt) {
			this.testInt = testInt;
		}
	}

	public static class Test3 {
		private Test1 test;

		public Test1 getTest() {
			return test;
		}

		public void setTest(Test1 test) {
			this.test = test;
		}
	}
}
