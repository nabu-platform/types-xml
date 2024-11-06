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

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import be.nabu.libs.converter.ConverterFactory;
import be.nabu.libs.converter.api.Converter;
import be.nabu.libs.types.ComplexContentWrapperFactory;
import be.nabu.libs.types.DefinedTypeResolverFactory;
import be.nabu.libs.types.ParsedPath;
import be.nabu.libs.types.TypeUtils;
import be.nabu.libs.types.api.ComplexContent;
import be.nabu.libs.types.api.ComplexType;
import be.nabu.libs.types.api.DefinedTypeResolver;
import be.nabu.libs.types.api.Marshallable;
import be.nabu.libs.types.api.SimpleType;
import be.nabu.libs.types.api.Type;
import be.nabu.libs.types.api.TypeRegistry;
import be.nabu.libs.types.api.Unmarshallable;
import be.nabu.libs.types.base.DynamicElement;
import be.nabu.libs.types.base.RootElement;
import be.nabu.libs.types.properties.NameProperty;

public class XMLContent implements ComplexContent {

	private Element element;
	private be.nabu.libs.types.api.Element<?> definition;
	private XMLContent parent;
	private Boolean isTextElement;
	private DefinedTypeResolver typeResolver = DefinedTypeResolverFactory.getInstance().getResolver();
	
	private List<String> anyElements = new ArrayList<String>();
	
	private Converter converter = ConverterFactory.getInstance().getConverter();
	
	private Map<Element, XMLContent> children = new HashMap<Element, XMLContent>();
	
	private XMLContent(XMLContent parent, Element element, be.nabu.libs.types.api.Element<?> definition) {
		this.parent = parent;
		this.element = element;
		this.definition = definition;		
	}
	
	public XMLContent(be.nabu.libs.types.api.Element<?> definition) throws SAXException {
		Document document = newDocument();
		Element root = document.createElementNS(definition.getNamespace(), definition.getName());
		document.appendChild(root);
		this.element = root;
		this.definition = definition;
	}
	
	public XMLContent(ComplexType complexType) throws SAXException {
		this(new RootElement(complexType));
	}
	
	public XMLContent(Element element, be.nabu.libs.types.api.Element<?> definition) {
		this(null, element, definition);
	}
	
	public XMLContent(Element element, TypeRegistry registry) {
		this(null, element, registry.getElement(element.getNamespaceURI(), getName(element)));
	}
	
	public XMLContent(Element element) {
		this(null, element, null);
	}
	
	@Override
	public ComplexType getType() {
		return definition == null ? null : (ComplexType) definition.getType();
	}
	
	public Element getElement() {
		return element;
	}

	/**
	 * Gets the element before which we should insert the new element to be valid 
	 */
	private Element getInsertBefore(be.nabu.libs.types.api.Element<?> typeToInsert) {
		// append at end if there is no definition to position element
		if (getType() == null)
			return null;
		List<be.nabu.libs.types.api.Element<?>> allChildren = new ArrayList<be.nabu.libs.types.api.Element<?>>(TypeUtils.getAllChildren(getType()));
		int typeIndex = allChildren.indexOf(typeToInsert);
		// last element, always append at end
		if (typeIndex >= allChildren.size() - 1)
			return null;
		for (int i = 0; i < element.getChildNodes().getLength(); i++) {
			if (element.getChildNodes().item(i).getNodeType() == Node.ELEMENT_NODE) {
				Element childElement = (Element) element.getChildNodes().item(i);
				for (int j = typeIndex + 1; j < allChildren.size(); j++) {
					be.nabu.libs.types.api.Element<?> childDefinition = allChildren.get(j);
					if(((childDefinition.getNamespace() == null && childElement.getNamespaceURI() == null) || (childDefinition.getNamespace() != null && childDefinition.getNamespace().equals(childElement.getNamespaceURI())))
							&& childDefinition.getName().equals(getName(childElement)))
						return childElement;
				}
			}
		}
		return null;
	}
	
	/**
	 * TODO: allow other types to be set, don't just toString(), use marshallable or (if types not available) the converter framework
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public void set(String path, Object value) {
		ParsedPath parsed = new ParsedPath(path);
		if (parsed.getName().equals("..")) {
			if (parent != null)
				parent.set(parsed.getChildPath().toString(), value);
			else
				throw new IllegalArgumentException("Can not reference parent from root element");
		}
		else if (parsed.getName().equals("."))
			set(parsed.getChildPath().toString(), value);
		// if there is no child definition and the value is a collection, set it with an index
		else if (parsed.getChildPath() == null && value instanceof Collection) {
			int i = 0;
			for (Object child : (Collection) value)
				set(parsed.getName() + "[" + i++ + "]", child);
		}
		else {
			boolean isAttribute = false;
			String childName = parsed.getName();
			if (childName.startsWith("@")) {
				isAttribute = true;
				childName = childName.substring(1);
			}
			be.nabu.libs.types.api.Element<?> childDefinition = null;
			if (getType() != null) {
				childDefinition = getType().get(childName);
				if (childDefinition == null)
					throw new IllegalArgumentException("The field " + parsed.getName() + " does not exist in " + definition.getName());
			}
			
			// intercept "any" settings
			if (childDefinition != null && childDefinition.getName().equals(NameProperty.ANY)) {
				if (parsed.getIndex() == null) {
					throw new IllegalArgumentException("Need an index when setting an any element");
				}
				childName = parsed.getIndex();
				anyElements.add(childName);
				Type type = value == null ? typeResolver.resolve(String.class.getName()) : typeResolver.resolve(value.getClass().getName());
				childDefinition = new DynamicElement(childDefinition, type, childName);
				if (type instanceof ComplexType && value != null && !(value instanceof ComplexContent)) {
					value = ComplexContentWrapperFactory.getInstance().getWrapper().wrap(value);
				}
				parsed.setIndex(null);
			}			
		
			if (isAttribute) {
				if (parsed.getChildPath() != null)
					throw new IllegalArgumentException("Can not set a child path on an attribute");
				if (value == null)
					element.removeAttribute(childName);
				else {
					Marshallable marshallable = getMarshallable(childDefinition);
					element.setAttribute(childName, marshallable == null ? value.toString() : marshallable.marshal(value, childDefinition.getProperties()));
				}
			}
			else {
				// has a child path
				if (parsed.getChildPath() != null) {
					int index = 0;
					ComplexContent childContent = null;
					for (int i = 0; i < element.getChildNodes().getLength(); i++) {
						Node child = element.getChildNodes().item(i);
						if (child.getNodeType() == Node.ELEMENT_NODE && childName.equals(getName(child)) && (parsed.getIndex() == null || new Integer(parsed.getIndex()) == index++)) {
							Element childElement = (Element) child;
							if (!children.containsKey(childElement))
								children.put(childElement, new XMLContent(this, childElement, childDefinition));
							childContent = children.get(childElement);
							break;
						}
					}
					// we did not find the proper element so we should create it
					if (childContent == null) {
						Element newChildElement = null;
						for (int i = index; i <= (parsed.getIndex() == null ? index : new Integer(parsed.getIndex())); i++) {
							newChildElement = element.getOwnerDocument().createElementNS(childDefinition == null ? null : childDefinition.getNamespace(), childName);
							appendChild(newChildElement, childDefinition);
							children.put(newChildElement, new XMLContent(this, newChildElement, childDefinition));
						}
						childContent = children.get(newChildElement);
					}
					if (childContent == null)
						throw new IllegalArgumentException("Could not create content for " + childName);
					
					// set it in the child
					childContent.set(parsed.getChildPath().toString(), value);
				}
				else {
					if (value instanceof ComplexContent) {
						ComplexContent content = (ComplexContent) value;
						ComplexType contentType = content.getType();
						
						// if you are using typing and the types don't match, throw an exception
						if (childDefinition != null && contentType != null && !childDefinition.getType().equals(contentType) && TypeUtils.getUpcastPath(contentType, childDefinition.getType()).isEmpty())
							throw new IllegalArgumentException("The content you are trying to set for " + childName + " is not of the correct type");

						XMLContent currentElement = (XMLContent) get(parsed.getName());
						// if there is no child definition and the content is of this type we can do a low level XML operation
						// you don't particularly care about the correct end result otherwise you would use typing
						if (childDefinition == null && value instanceof XMLContent) {
							Element importedElement = (Element) element.getOwnerDocument().importNode(((XMLContent) value).element, true);
							if (currentElement != null) {
								element.replaceChild(importedElement, currentElement.element);
								children.remove(currentElement.element);
							}
							else
								appendChild(importedElement, null);
						}
						// in all other cases we should use the type to only set the values that actually belong here, this is based on the definition
						else {
							ComplexType typeToSet = null;
							if (childDefinition != null)
								typeToSet = (ComplexType) childDefinition.getType();
							else if (contentType != null)
								typeToSet = contentType;
							else
								throw new IllegalArgumentException("Can't set a complex type without any definitions to work from");
							
							if (currentElement == null) {
								currentElement = new XMLContent(element.getOwnerDocument().createElementNS(childDefinition == null ? null : childDefinition.getNamespace(), childName), childDefinition);
								children.put(currentElement.element, currentElement);
								appendChild(currentElement.element, childDefinition);								
							}
							for (be.nabu.libs.types.api.Element<?> childToSet : typeToSet)
								currentElement.set(childToSet.getName(), content.get(childToSet.getName()));
						}
					}
					else {
						// marshal the value
						if (value != null) {
							Marshallable marshallable = getMarshallable(childDefinition);
							Object converted = converter.convert(value, marshallable.getInstanceClass());
							if (converted == null) {
								throw new IllegalArgumentException("Can not convert " + value.getClass().getName() + " to " + marshallable.getInstanceClass().getName());
							}
							value = marshallable == null ? value.toString() : marshallable.marshal(converted, childDefinition.getProperties());
						}
						
						int index = 0;
						boolean found = false;
						for (int i = 0; i < element.getChildNodes().getLength(); i++) {
							Node node = element.getChildNodes().item(i);
							if (node.getNodeType() == Node.ELEMENT_NODE && childName.equals(getName(node)) && (parsed.getIndex() == null || new Integer(parsed.getIndex()) == index++)) {
								found = true;
								// if the value is null, remove it
								if (value == null)
									element.removeChild(element.getChildNodes().item(i));
								else
									element.getChildNodes().item(i).setTextContent((String) value);
								break;
							}
						}
						if (!found && value != null) {
							Element newChild = null;
							for (int i = index; i <= (parsed.getIndex() == null ? index : new Integer(parsed.getIndex())); i++) {
								newChild = element.getOwnerDocument().createElementNS(childDefinition != null ? childDefinition.getNamespace() : null, childName);
								appendChild(newChild, childDefinition);
							}
							newChild.setTextContent((String) value);
						}
					}
				}
			}
		}
	}
	
	private Marshallable<?> getMarshallable(be.nabu.libs.types.api.Element<?> definition) {
		if (definition == null)
			return null;
		
		Type type = definition.getType();
		while (type != null && !(type instanceof Marshallable))
			type = type.getSuperType();
		if (type instanceof Marshallable)
			return (Marshallable<?>) type;
		else
			throw new IllegalArgumentException("The definition of " + definition.getName() + " does not allow for marshalling values");
	}

	private void appendChild(Element newElement, be.nabu.libs.types.api.Element<?> newElementDefinition) {
		Element insertBefore = getInsertBefore(newElementDefinition);
		if (insertBefore == null)
			element.appendChild(newElement);
		else
			element.insertBefore(newElement, insertBefore);
	}
	
	/**
	 * TODO: if we have type information, convert it to the proper type!
	 * TODO: better support for lists, make sure it is a list object if the definition states it as a list
	 */
	@Override
	public Object get(String path) {
		ParsedPath parsed = new ParsedPath(path);
		if (parsed.getName().equals("..")) {
			if (parent != null)
				return parent.get(parsed.getChildPath().toString());
			else
				throw new IllegalArgumentException("Can not reference parent from root element");
		}
		else if (parsed.getName().equals("."))
			return get(parsed.getChildPath().toString());
		else if (parsed.getChildPath() == null && parsed.getName().startsWith("@")) {
			be.nabu.libs.types.api.Element<?> childDefinition = getType() != null ? getType().get(parsed.getName()) : null;
			String content = element.hasAttribute(parsed.toString().substring(1))
				? element.getAttribute(parsed.toString().substring(1))
				: null;
			return content == null ? null : unmarshal(content, childDefinition);
		}
		else {
			if (parsed.getName().equals(NameProperty.ANY)) {
				if (parsed.getIndex() != null) {
					if (!anyElements.contains(parsed.getIndex())) {
						return null;
					}
					// redirect
					else {
						parsed.setName(parsed.getIndex());
						parsed.setIndex(null);
					}
				}
				// no proper collection support
//				else {
//					if (parsed.getChildPath() != null) {
//						throw new IllegalArgumentException("If you want to access a child path, you need to provide an index");
//					}
//					Map<String, Object> anyParts = new LinkedHashMap<String, Object>();
//					for (String name : anyElements) {
//						anyParts.put(name, get(name));
//					}
//					return anyParts;
//				}
			}
			
			// has a child path
			if (parsed.getChildPath() != null) {
				int index = 0;
				NodeList childNodes = element.getChildNodes();
				for (int i = 0; i < childNodes.getLength(); i++) {
					Node child = childNodes.item(i);
					if (child.getNodeType() == Node.ELEMENT_NODE && parsed.getName().equals(getName(child))) {
						Element childElement = (Element) child;
						if (!children.containsKey(childElement))
							children.put(childElement, new XMLContent(this, childElement, definition == null ? null : (XMLSchemaElement<?>) getType().get(getName(childElement))));
						if (parsed.getIndex() == null || new Integer(parsed.getIndex()) == index++)
							return children.get(childElement).get(parsed.getChildPath().toString());
					}
				}
			}
			else {
				List<Object> result = new ArrayList<Object>();
				be.nabu.libs.types.api.Element<?> childDefinition = null;
				// if you want the simple type...
				if (getType() instanceof SimpleType && getType() instanceof ComplexType && parsed.getName().equals(ComplexType.SIMPLE_TYPE_VALUE)) {
					String textContent = element.getTextContent();
					result.add(textContent.isEmpty() ? null : unmarshal(textContent, definition));
					childDefinition = definition;
				}
				else {
					childDefinition = getType() != null ? getType().get(parsed.getName()) : null;
					int index = 0;
					NodeList childNodes = element.getChildNodes();
					for (int i = 0; i < childNodes.getLength(); i++) {
						Node child = childNodes.item(i);
						if (child.getNodeType() == Node.ELEMENT_NODE && parsed.getName().equals(getName(child))) {
							Element childElement = (Element) child;
							if (!children.containsKey(childElement))
								children.put(childElement, new XMLContent(this, childElement, definition == null ? null : (be.nabu.libs.types.api.Element<?>) getType().get(getName(childElement))));
							if (parsed.getIndex() == null || new Integer(parsed.getIndex()) == index++) {
								// if it's a complex type, just add the entire child
								if (childDefinition != null && childDefinition.getType() instanceof ComplexType) {
									// you need to reference ComplexType.SIMPLE_TYPE_VALUE to get the simple value
									if (childDefinition.getType() instanceof SimpleType)
										result.add(childElement);
									else
										result.add(children.get(childElement));
								}
								else if (childDefinition != null) {
									String textContent = childElement.getTextContent();
									result.add(textContent.isEmpty() ? null : unmarshal(textContent, childDefinition));
								}
								else {
									if (children.get(childElement).isTextElement()) {
										String textContent = childElement.getTextContent();
										result.add(textContent.isEmpty() ? null : textContent);
									}
									else
										result.add(children.get(childElement));
								}
							}	
						}
					}
				}
				Boolean isList = childDefinition == null ? null : childDefinition.getType().isList(childDefinition.getProperties());
				if (result.size() == 0)
					return null;
				else if (result.size() == 1 || (isList != null && !isList))
					return result.get(0);
				else
					return result;
			}
		}
		return null;
	}
	
	public static String getName(Node node) {
		return node.getLocalName() == null ? node.getNodeName().replaceFirst("^[^:]+:", "") : node.getLocalName();
	}
	
	private Object unmarshal(String string, be.nabu.libs.types.api.Element<?> definition) {
		if (definition == null)
			return string;
		else if (definition.getType() instanceof SimpleType) {
			SimpleType<?> simpleType = (SimpleType<?>) definition.getType();
			do {
				if (simpleType instanceof Unmarshallable)
					return ((Unmarshallable<?>) simpleType).unmarshal(string, definition.getProperties());
				else
					simpleType = (SimpleType<?>) simpleType.getSuperType();
			}
			while (simpleType != null);
		}
		throw new RuntimeException("Can not unmarshal the content");
	}

	private boolean isTextElement() {
		if (isTextElement == null) {
			for (int i = 0; i < element.getChildNodes().getLength(); i++) {
				if (element.getChildNodes().item(i).getNodeType() == Node.ELEMENT_NODE) {
					isTextElement = false;
					break;
				}
			}
			// tiny tweak: if we have attributes, we are considered a non-text element as well!
			if (isTextElement == null)
				isTextElement = element.getAttributes().getLength() == 0;
		}
		return isTextElement;
	}
	
	static Document newDocument() throws SAXException {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setValidating(false);
			factory.setNamespaceAware(true);
			DocumentBuilder builder = factory.newDocumentBuilder();
			return builder.newDocument();
		}
		catch (ParserConfigurationException e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	public String toString() {
		TransformerFactory factory = TransformerFactory.newInstance();
		try {
			Transformer transformer = factory.newTransformer();
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			// as long as the namespace is defined it shouldn't throw an error if not supported but simply ignore this
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
			
			transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
			StringWriter writer = new StringWriter();
			transformer.transform(new DOMSource(element), new StreamResult(writer));
			return writer.toString();
		}
		catch (IllegalArgumentException e) {
			throw new RuntimeException(e);
		}
		catch (TransformerException e) {
			throw new RuntimeException(e);
		}
	}
}
