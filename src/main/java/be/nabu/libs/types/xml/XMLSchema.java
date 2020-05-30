package be.nabu.libs.types.xml;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import be.nabu.libs.property.ValueUtils;
import be.nabu.libs.property.api.Value;
import be.nabu.libs.types.SimpleTypeWrapperFactory;
import be.nabu.libs.types.TypeRegistryImpl;
import be.nabu.libs.types.TypeUtils;
import be.nabu.libs.types.api.ComplexType;
import be.nabu.libs.types.api.DefinedTypeRegistry;
import be.nabu.libs.types.api.Element;
import be.nabu.libs.types.api.SimpleType;
import be.nabu.libs.types.api.SimpleTypeWrapper;
import be.nabu.libs.types.api.Type;
import be.nabu.libs.types.api.TypeRegistry;
import be.nabu.libs.types.api.Unmarshallable;
import be.nabu.libs.types.base.Choice;
import be.nabu.libs.types.base.CollectionFormat;
import be.nabu.libs.types.base.Duration;
import be.nabu.libs.types.base.ValueImpl;
import be.nabu.libs.types.properties.ActualTypeProperty;
import be.nabu.libs.types.properties.AttributeQualifiedDefaultProperty;
import be.nabu.libs.types.properties.CollectionFormatProperty;
import be.nabu.libs.types.properties.ElementQualifiedDefaultProperty;
import be.nabu.libs.types.properties.EnumerationProperty;
import be.nabu.libs.types.properties.FormatProperty;
import be.nabu.libs.types.properties.LengthProperty;
import be.nabu.libs.types.properties.MaxExclusiveProperty;
import be.nabu.libs.types.properties.MaxInclusiveProperty;
import be.nabu.libs.types.properties.MaxLengthProperty;
import be.nabu.libs.types.properties.MaxOccursProperty;
import be.nabu.libs.types.properties.MinExclusiveProperty;
import be.nabu.libs.types.properties.MinInclusiveProperty;
import be.nabu.libs.types.properties.MinLengthProperty;
import be.nabu.libs.types.properties.MinOccursProperty;
import be.nabu.libs.types.properties.NameProperty;
import be.nabu.libs.types.properties.NillableProperty;
import be.nabu.libs.types.properties.PatternProperty;
import be.nabu.libs.types.properties.QualifiedProperty;
import be.nabu.libs.types.properties.TokenProperty;

public class XMLSchema implements DefinedTypeRegistry {
	
	private static final String NMTOKEN_REGEX = "[\\s]*[\\w:.-]*[\\s]*";
	private ResourceResolver resolver;
	private String namespace;
	private Document document;
	private static final String NAMESPACE = "http://www.w3.org/2001/XMLSchema";
	
	private String id;
	
	private boolean isElementQualified = false;
	private boolean isAttributeQualified = false;
	private boolean stringsOnly = false;
	// perhaps you want your own mechanism of including/importing other schema's, not necessarily the XML schema way which is pretty limited to file system layouts
	// note that if we ignore a failed include/import _and_ you do not provide your own _and_ types are actually used that are referenced
	private boolean ignoreInclusionFailure = false;
	
	private TypeRegistryImpl registry = new TypeRegistryImpl();
	
	// an internal (temporary) registry solely for groups
	private TypeRegistryImpl groupRegistry = new TypeRegistryImpl();
	
	private SimpleTypeWrapper wrapper = SimpleTypeWrapperFactory.getInstance().getWrapper();
	
	private Logger logger = LoggerFactory.getLogger(getClass());
	
	static class DelayedParse {
		private org.w3c.dom.Element element;
		private String reason;
		
		public DelayedParse(org.w3c.dom.Element element, String reason) {
			this.element = element;
			this.reason = reason;
		}

		public org.w3c.dom.Element getElement() {
			return element;
		}

		public String getReason() {
			return reason;
		}
		
		@Override
		public String toString() {
			return "Delay on " + element + ": " + reason;
		}
	}
	
	public XMLSchema(InputStream container) throws SAXException, IOException {
		this(container, false);
	}
	
	public XMLSchema(InputStream container, boolean stringsOnly) throws SAXException, IOException {
		this(toDocument(container), stringsOnly);
	}
	
	public XMLSchema(Document document) {
		this(document, false);
	}
	
	public XMLSchema(Document document, boolean stringsOnly) {
		this.document = document;
		this.stringsOnly = stringsOnly;
	}

	public boolean isPrioritizeIncludes() {
		return registry.isPrioritizeIncludes();
	}
	public void setPrioritizeIncludes(boolean prioritizeIncludes) {
		registry.setPrioritizeIncludes(prioritizeIncludes);
	}
	
	public void register(TypeRegistry...registries) {
		registry.register(registries);
	}
	
	public void parse() throws SAXException, ParseException, IOException {
		parse(document.getDocumentElement(), new HashMap<String, String>(), null);
	}
	
	private Object parse(org.w3c.dom.Element element, Map<String, String> namespaces, XMLSchemaComplexType parent) throws SAXException, ParseException, IOException {
		logger.debug("Parsing " + element.getLocalName() + (element.hasAttribute("name") ? " " + element.getAttribute("name") : "") + " in " + parent);
		// for each element, retrieve any additional namespaces and put them in a new map that is only valid for this element and child elements
		namespaces = new HashMap<String, String>(namespaces);
		NamedNodeMap attributes = element.getAttributes();
		for (int i = 0; i < attributes.getLength(); i++) {
			if (attributes.item(i).getNodeName().startsWith("xmlns:")) {
				namespaces.put(attributes.item(i).getNodeName().substring("xmlns:".length()), attributes.item(i).getTextContent());
			}
			else if (attributes.item(i).getNodeName().equals("xmlns")) {
				namespaces.put(null, attributes.item(i).getTextContent());
			}
		}
		logger.trace("With namespaces: {}", namespaces);
		// only parse elements in the xsd namespace
		if (element.getNamespaceURI().equals(NAMESPACE)) {
			// loop over all the elements and parse them
			if (element.getLocalName().equalsIgnoreCase("schema"))
				return parseSchema(element, namespaces);
			else if (element.getLocalName().equalsIgnoreCase("include"))
				return parseInclude(element, namespaces);
			else if (element.getLocalName().equalsIgnoreCase("import"))
				return parseImport(element, namespaces);
			else if (element.getLocalName().equalsIgnoreCase("element"))
				return parseElement(element, namespaces, parent);
			else if (element.getLocalName().equalsIgnoreCase("complexType") || element.getLocalName().equalsIgnoreCase("group"))
				return parseComplexType(element, namespaces);
			else if (element.getLocalName().equalsIgnoreCase("attribute"))
				return parseAttribute(element, namespaces, parent);
			else if (element.getLocalName().equalsIgnoreCase("anyAttribute"))
				return createAny(true, element, parent);
			else if (element.getLocalName().equalsIgnoreCase("simpleType"))
				return parseSimpleType(element, namespaces);
			else if (element.getLocalName().equalsIgnoreCase("any"))
				return createAny(false, element, parent);
			// this can occur if for example you have a sequence element in a choice
			else if (element.getLocalName().equalsIgnoreCase("sequence")) {
				return parseSequence(element, namespaces, parent);
			}
			// we currently ignore annotations
			else if (element.getLocalName().equalsIgnoreCase("annotation")) {
				return null;
			}
			else
				throw new ParseException("The parser currently does not support " + element.getLocalName(), 0);
		}
		return null;
	}
	
	private XMLSchema parseSchema(org.w3c.dom.Element element, Map<String, String> namespaces) throws SAXException, ParseException, IOException {
		// get the targetNamespace
		if (element.hasAttribute("targetNamespace"))
			namespace = element.getAttribute("targetNamespace");
		if (element.hasAttribute("elementFormDefault") && element.getAttribute("elementFormDefault").equals("qualified"))
			isElementQualified = true;
		if (element.hasAttribute("attributeFormDefault") && element.getAttribute("attributeFormDefault").equals("qualified"))
			isAttributeQualified = true;

		logger.debug("Schema with targetNamespace " + namespace);
		// the elements and types etc don't have to be in any order so for example the first complexType in the xsd could reference a complexType further down
		// hence the "delayedElements", they will be retried
		List<org.w3c.dom.Element> delayedElements = new ArrayList<org.w3c.dom.Element>();
		logger.debug("Initial parse to pick up items that have no unresolved dependencies");
		for (int i = 0; i < element.getChildNodes().getLength(); i++) {
			if (element.getChildNodes().item(i).getNodeType() == Node.ELEMENT_NODE) {
				Object object = parse((org.w3c.dom.Element) element.getChildNodes().item(i), namespaces, null);
				if (object instanceof DelayedParse)
					delayedElements.add((org.w3c.dom.Element) element.getChildNodes().item(i));
			}
		}
		while(delayedElements.size() > 0) {
			int initialSize = delayedElements.size();
			logger.debug("Parsing {} delayed elements", initialSize);
			Iterator<org.w3c.dom.Element> iterator = delayedElements.iterator();
			while (iterator.hasNext()) {
				org.w3c.dom.Element next = iterator.next();
				logger.trace("Parsing " + next.getNodeName() + ": " + next.getAttribute("name"));
				Object object = parse(next, namespaces, null);
				if (!(object instanceof DelayedParse)) {
					iterator.remove();
				}
				else {
					logger.debug(object.toString());
				}
			}
			// if nothing was parsed in this round, throw an exception
			if (delayedElements.size() == initialSize) {
				throw new ParseException("Can not proceed with parsing, there appear to be some references that can not be resolved: " + toString(delayedElements), 0);
			}
		}
		return this;
	}
	
	static String toString(List<org.w3c.dom.Element> delayedElements) {
		StringBuilder builder = new StringBuilder();
		for (org.w3c.dom.Element element : delayedElements)
			builder.append("\n\t" + element.getNodeName() + ": " + element.getAttribute("name"));
		return builder.toString();
	}
	
	private TypeRegistry parseImport(org.w3c.dom.Element element, Map<String, String> namespaces) throws SAXException, ParseException, IOException {
		String namespace = element.getAttribute("namespace");
		String schemaLocation = element.hasAttribute("schemaLocation") ? element.getAttribute("schemaLocation") : null;
		
		logger.debug("Importing (namespace " + namespace + "): " + schemaLocation);
		try {
			TypeRegistry imported;
			if (schemaLocation == null) {
				imported = getResolver().resolve(namespace);
			}
			else {
				URI uri = new URI(schemaLocation);
				InputStream resolve;
				try {
					resolve = getResolver().resolve(uri);
				}
				catch (Exception e) {
					if (ignoreInclusionFailure) {
						logger.warn("Ignoring failed import: " + schemaLocation);
						return null;
					}
					else {
						logger.error("Failed import: " + schemaLocation);
						throw new IOException("Failed to import: " + schemaLocation, e);
					}
				}
				imported = new XMLSchema(resolve);
				((XMLSchema) imported).setResolver(getResolver());
				((XMLSchema) imported).parse();
			}
			if (imported == null) {
				throw new FileNotFoundException("Could not find import for " + namespace);
			}
			if (imported instanceof XMLSchema && !namespace.equals(((XMLSchema) imported).getNamespace())) {
				throw new ParseException("The imported schema '" + schemaLocation + "' does not have the declared namespace: " + namespace + " != " + ((XMLSchema) imported).getNamespace(), 0);
			}
			if (imported instanceof XMLSchema) {
				for (Map.Entry<String, String> entry : namespaces.entrySet()) {
					// we want the prefix of the namespace
					if (namespace.equals(entry.getValue())) {
						((XMLSchema) imported).setId(id + "." + entry.getKey());
						break;
					}
				}
			}
			registry.register(imported);
			return imported;
		}
		catch (URISyntaxException e) {
			throw new IOException(e);
		}
	}
	
	private TypeRegistry parseInclude(org.w3c.dom.Element element, Map<String, String> namespaces) throws SAXException, ParseException, IOException {
		String schemaLocation = element.getAttribute("schemaLocation");
		
		logger.debug("Including: " + schemaLocation);
		try {
			URI uri = new URI(schemaLocation);
			InputStream resolve;
			try {
				resolve = getResolver().resolve(uri);
			}
			catch (Exception e) {
				if (ignoreInclusionFailure) {
					logger.warn("Ignoring failed include: " + schemaLocation);
					return null;
				}
				else {
					throw new IOException(e);
				}
			}
			TypeRegistry included = new XMLSchema(resolve);
			((XMLSchema) included).setResolver(getResolver());
			// for an include it should be the same namespace, often times it simply has no namespace and everything in it should be imported into your own
			((XMLSchema) included).setNamespace(getNamespace());
			((XMLSchema) included).parse();
			registry.register(included);
			return included;
		}
		catch (URISyntaxException e) {
			throw new IOException(e);
		}
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private Object parseSimpleType(org.w3c.dom.Element tag, Map<String, String> namespaces) {
		String name = tag.hasAttribute("name") ? tag.getAttribute("name") : null;
		org.w3c.dom.Element firstChildElement = getFirstChild(tag);

		if (firstChildElement == null) {
			return null;
		}
		
		XMLSchemaSimpleType<?> simpleType;
		if (firstChildElement.getLocalName().equalsIgnoreCase("list")) {
			String itemType = firstChildElement.hasAttribute("itemType") ? firstChildElement.getAttribute("itemType") : null;
			logger.trace("List of base type " + itemType);
			int index = itemType == null ? -1 : itemType.indexOf(':');
			String itemTypeNamespace = index >= 0 ? namespaces.get(itemType.substring(0, index)) : namespaces.get(null);
			String itemTypeName = index >= 0 ? itemType.substring(index + 1) : itemType;
			SimpleType superType = getSimpleType(itemTypeNamespace, itemTypeName);
			if (superType == null) {
				return new DelayedParse(tag, "Could not find list item type " + itemTypeNamespace + " # " + itemTypeName);
			}
			simpleType = name == null ? new XMLSchemaSimpleType(this, superType, itemTypeName) : new XMLSchemaDefinedSimpleType(this, name, superType, itemTypeName);
			simpleType.setProperty(new ValueImpl<Integer>(MaxOccursProperty.getInstance(), 0));
			simpleType.setProperty(new ValueImpl<CollectionFormat>(CollectionFormatProperty.getInstance(), CollectionFormat.SSV));
		}
		else {
			String base = firstChildElement.hasAttribute("base") ? firstChildElement.getAttribute("base") : null;
			logger.trace("Extends base type " + base);
			int index = base == null ? -1 : base.indexOf(':');
			String baseNamespace = index >= 0 ? namespaces.get(base.substring(0, index)) : namespaces.get(null);
			String baseName = index >= 0 ? base.substring(index + 1) : base;
	
			SimpleType superType = null;
			if (baseName != null) {
				// can only extend another simple type
				superType = getSimpleType(baseNamespace, baseName);
				// if the supertype is not available (yet), return null so it is tried again later
				if (superType == null)
					return new DelayedParse(tag, "Could not find superType " + baseNamespace + " # " + baseName);
			}
			else {
				superType = getNativeSchemaType("string");
			}
			
			SimpleType actualType = null;
			if (stringsOnly && !String.class.isAssignableFrom(superType.getInstanceClass())) {
				logger.debug("Hiding type " + superType + " in a string");
				actualType = superType;
				superType = getNativeSchemaType("string");
			}
			
			simpleType = name == null ? new XMLSchemaSimpleType(this, superType, baseName) : new XMLSchemaDefinedSimpleType(this, name, superType, baseName);
			
			if (NAMESPACE.equals(baseNamespace) && (baseName.equalsIgnoreCase("token") || baseName.equalsIgnoreCase("nmtoken"))) {
				simpleType.setProperty(new ValueImpl<Boolean>(TokenProperty.getInstance(), true));
			}
			
			if (NAMESPACE.equals(baseNamespace) && baseName.equalsIgnoreCase("nmtoken")) {
				simpleType.setProperty(new ValueImpl<String>(PatternProperty.getInstance(), NMTOKEN_REGEX));
			}
			
			if (actualType != null)
				simpleType.setProperty(new ValueImpl(new ActualTypeProperty(), actualType));
			
			// at some point it must reference a native type which has no supertype
			// all native types are implemented with unmarshalling capabilities
			// we need the base type you are extending in case it's a date, because we need to know which type of date
			String baseTypeName = simpleType.getBaseTypeName();
			superType = (SimpleType) simpleType;
			while(superType.getSuperType() != null) {
				if (superType instanceof XMLSchemaSimpleType)
					baseTypeName = ((XMLSchemaSimpleType) superType).getBaseTypeName();
				superType = (SimpleType) superType.getSuperType();
			}
	
			logger.debug("Base super type {} has baseTypeName " + baseTypeName, superType.getClass());
			// date is the only one that needs additional context (the name of the simple type you are extending)
			Value<String> format = new ValueImpl<String>(new FormatProperty(), baseTypeName);
			if (firstChildElement.getNamespaceURI().equals(NAMESPACE) && firstChildElement.getLocalName().equals("restriction")) {
				List enumerationValues = new ArrayList();
				for (int i = 0; i < firstChildElement.getChildNodes().getLength(); i++) {
					if (firstChildElement.getChildNodes().item(i).getNodeType() == Node.ELEMENT_NODE) {
						CollectionFormat collectionFormat = ValueUtils.getValue(CollectionFormatProperty.getInstance(), superType.getProperties());
						
						String value = ((org.w3c.dom.Element) firstChildElement.getChildNodes().item(i)).getAttribute("value"); 
						if (value != null && value.trim().isEmpty()) {
							value = null;
						}
						if (firstChildElement.getChildNodes().item(i).getLocalName().equalsIgnoreCase("length"))
							simpleType.setProperty(new ValueImpl<Integer>(new LengthProperty(), new Integer(value != null ? value : firstChildElement.getChildNodes().item(i).getTextContent())));
						else if (firstChildElement.getChildNodes().item(i).getLocalName().equalsIgnoreCase("minLength")) {
							if (collectionFormat != null) {
								simpleType.setProperty(new ValueImpl<Integer>(MinOccursProperty.getInstance(), new Integer(value != null ? value : firstChildElement.getChildNodes().item(i).getTextContent())));	
							}
							else {
								simpleType.setProperty(new ValueImpl<Integer>(new MinLengthProperty(), new Integer(value != null ? value : firstChildElement.getChildNodes().item(i).getTextContent())));
							}
						}
						else if (firstChildElement.getChildNodes().item(i).getLocalName().equalsIgnoreCase("maxLength")) {
							if (collectionFormat != null) {
								simpleType.setProperty(new ValueImpl<Integer>(MaxOccursProperty.getInstance(), new Integer(value != null ? value : firstChildElement.getChildNodes().item(i).getTextContent())));	
							}
							else {
								simpleType.setProperty(new ValueImpl<Integer>(new MaxLengthProperty(), new Integer(value != null ? value : firstChildElement.getChildNodes().item(i).getTextContent())));
							}
						}
						else if (firstChildElement.getChildNodes().item(i).getLocalName().equalsIgnoreCase("pattern"))
							simpleType.setProperty(new ValueImpl<String>(new PatternProperty(), value != null ? value : firstChildElement.getChildNodes().item(i).getTextContent()));
						else {
							Object parsed = ((Unmarshallable) superType).unmarshal(value, format);
							if (firstChildElement.getChildNodes().item(i).getLocalName().equalsIgnoreCase("minInclusive"))
								simpleType.setProperty(new ValueImpl(new MinInclusiveProperty(), parsed));
							else if (firstChildElement.getChildNodes().item(i).getLocalName().equalsIgnoreCase("maxInclusive"))
								simpleType.setProperty(new ValueImpl(new MaxInclusiveProperty(), parsed));
							else if (firstChildElement.getChildNodes().item(i).getLocalName().equalsIgnoreCase("maxExclusive"))
								simpleType.setProperty(new ValueImpl(new MaxExclusiveProperty(), parsed));
							else if (firstChildElement.getChildNodes().item(i).getLocalName().equalsIgnoreCase("minExclusive"))
								simpleType.setProperty(new ValueImpl(new MinExclusiveProperty(), parsed));
							else if (firstChildElement.getChildNodes().item(i).getLocalName().equalsIgnoreCase("enumeration"))
								enumerationValues.add(parsed);
						}
						// TODO: no support for fractionDigits & totalDigits
					}
				}
				if (!enumerationValues.isEmpty())
					simpleType.setProperty(new ValueImpl(new EnumerationProperty(), enumerationValues));
			}
			// a union allows a simple element to have one or multiple types, it does not actually merge them, it simply states any of its members are valid
			// because we don't support multitype for a single element, we simply make sure the data type is generic enough to allow anything
			else if (firstChildElement.getNamespaceURI().equals(NAMESPACE) && firstChildElement.getLocalName().equals("union")) {
				// unless you specified a base type (not sure if this is even allowed in a union), this should be a string without any additional attributes
				// so simply send that back
			}
			else
				throw new RuntimeException("Only simple restrictions of simple types are currently supported");
	
		}
		
		// inherit the schema settings when at the root
		if (tag.getParentNode().getLocalName().equalsIgnoreCase("schema")) {
			simpleType.setProperty(new ValueImpl<Boolean>(AttributeQualifiedDefaultProperty.getInstance(), isAttributeQualified));
		}
		
		if (name != null)
			registry.register(simpleType);
		
		return simpleType;
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Object parseAttribute(org.w3c.dom.Element tag, Map<String, String> namespaces, ComplexType parent) {
		String name = tag.hasAttribute("name") ? tag.getAttribute("name") : null;
		String use = tag.hasAttribute("use") ? tag.getAttribute("use") : null;
		String type = tag.hasAttribute("type") ? tag.getAttribute("type") : null;
		// TODO: currently unused but can add them later
//		String defaultValue = tag.hasAttribute("default") ? tag.getAttribute("default") : null;
//		String fixedValue = tag.hasAttribute("fixed") ? tag.getAttribute("fixed") : null;
		
		int index = type == null ? -1 : type.indexOf(':');
		String typeNamespace = index >= 0 ? namespaces.get(type.substring(0, index)) : namespaces.get(null);
		String typeName = index >= 0 ? type.substring(index + 1) : type;
		// if type is null, it's a string
		SimpleType simpleType = type == null || NAMESPACE.equals(typeNamespace) 
			? getNativeSchemaType(typeName)
			: getSimpleType(typeNamespace, typeName);
		// try again later
		if (simpleType == null)
			return new DelayedParse(tag, "Could not find simple type: " + typeNamespace + " # " + typeName);
		
		SimpleType actualType = null;
		if (stringsOnly && !String.class.isAssignableFrom(simpleType.getInstanceClass())) {
			actualType = simpleType;
			simpleType = getNativeSchemaType("string");
		}
		
		Element<?> attribute = new XMLSchemaAttribute(this, name, simpleType, parent);
		
		if (typeName != null && (typeName.equalsIgnoreCase("token") || typeName.equalsIgnoreCase("nmtoken"))) {
			attribute.setProperty(new ValueImpl<Boolean>(TokenProperty.getInstance(), true));
		}
		
		// add a regex for nmtokens
		if (typeName != null && typeName.equalsIgnoreCase("nmtoken")) {
			attribute.setProperty(new ValueImpl<String>(PatternProperty.getInstance(), NMTOKEN_REGEX));
		}
		
		if (actualType != null)
			attribute.setProperty(new ValueImpl(new ActualTypeProperty(), actualType));
		
		if (use == null || use.equalsIgnoreCase("optional"))
			attribute.setProperty(new ValueImpl<Integer>(new MinOccursProperty(), 0));
		return attribute;
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private Object parseComplexType(org.w3c.dom.Element tag, Map<String, String> namespaces) throws SAXException, ParseException, IOException {
		String name = tag.hasAttribute("name") ? tag.getAttribute("name") : null;
		XMLSchemaComplexType complexType = getComplexType(tag, name);
		org.w3c.dom.Element firstChildElement = getFirstChild(tag);
		if (firstChildElement == null) {
			return null;
		}
		// a sequence of child elements
		if (firstChildElement.getNamespaceURI().equals(NAMESPACE) && (firstChildElement.getLocalName().equals("sequence") || firstChildElement.getLocalName().equals("all") || firstChildElement.getLocalName().equals("choice"))) {
			if (complexType == null) {
				complexType = name == null ? new XMLSchemaComplexType(this) : new XMLSchemaDefinedComplexType(this, name);
				// register immediately for recursive references
				registerComplexType(tag, name, complexType);
			}
			logger.debug("Parsing sequence for {}", complexType);
			Object returnValue = parseSequence(firstChildElement, namespaces, complexType);
			if (returnValue instanceof DelayedParse) {
				// clear anything we may have parsed, we will reparse in a future run
				// we can't just throw the type away cause there may be circular dependencies between types
				// this means they need each other to exist for parsing
				complexType.clear();
				return returnValue;
			}
		}
		else {
			if (firstChildElement.getNamespaceURI().equals(NAMESPACE) && firstChildElement.getLocalName().equals("complexContent")) {
				if (complexType == null) {
					complexType = name == null ? new XMLSchemaComplexType(this) : new XMLSchemaDefinedComplexType(this, name);
					registerComplexType(tag, name, complexType);
				}
				logger.debug("Parsing complex content for {}", complexType);
				firstChildElement = getFirstChild(firstChildElement);
				if (firstChildElement.getNamespaceURI().equals(NAMESPACE) && firstChildElement.getLocalName().equals("extension")) {
					String base = firstChildElement.hasAttribute("base") ? firstChildElement.getAttribute("base") : null;
					logger.debug("{} extends complex type " + base, complexType);
					int index = base == null ? -1 : base.indexOf(':');
					String baseNamespace = index >= 0 ? namespaces.get(base.substring(0, index)) : namespaces.get(null);
					String baseName = index >= 0 ? base.substring(index + 1) : base;
					// it must be a complex superType because of the complexContent
					ComplexType superType = getComplexType(baseNamespace, baseName);
					// try again later
					if (superType == null)
						return new DelayedParse(tag, "Could not find complex super type: " + baseNamespace + " # " + baseName);
					complexType.setSuperType(superType);
					org.w3c.dom.Element firstChild = getFirstChild(firstChildElement);
					// can have empty extensions...
					if (firstChild != null) {
						Object returnValue = parseSequence(firstChild, namespaces, complexType);
						if (returnValue instanceof DelayedParse) {
							complexType.clear();
							return returnValue;
						}
					}
				}
				else
					throw new ParseException("Only extensions of complex types are currently supported", 0);
			}
			else if (firstChildElement.getNamespaceURI().equals(NAMESPACE) && firstChildElement.getLocalName().equals("simpleContent")) {
				if (complexType == null) {
					complexType = name == null ? new XMLSchemaComplexSimpleType(this) : new XMLSchemaComplexSimpleType(this, name);
					registerComplexType(tag, name, complexType);
				}
				logger.debug("Parsing simple content for {}", complexType);
				firstChildElement = getFirstChild(firstChildElement);
				if (firstChildElement.getNamespaceURI().equals(NAMESPACE) && firstChildElement.getLocalName().equals("extension")) {
					String base = firstChildElement.hasAttribute("base") ? firstChildElement.getAttribute("base") : null;
					logger.debug("{} extends simple type " + base, complexType);
					int index = base == null ? -1 : base.indexOf(':');
					String baseNamespace = index >= 0 ? namespaces.get(base.substring(0, index)) : getNamespace();
					String baseName = index >= 0 ? base.substring(index + 1) : base;
					SimpleType<?> superType = getSimpleType(baseNamespace, baseName);
					// apparently this is also valid: complexType "A" uses simpleContent to extend a string
					// complexType "B" extends "A" and it still uses "simpleContent" to indicate this! so the referenced extension can also actually be a complex type!
					// must a complex simple type then at least, right...?
					if (superType == null) {
						superType = (SimpleType) getComplexType(baseNamespace, baseName);
					}
					// delay
					if (superType == null) {
						unregisterComplexType(tag, name, complexType);
						return new DelayedParse(tag, "Could not find simple super type: " + baseNamespace + " # " + baseName);
					}
					
					SimpleType actualType = null;
					if (stringsOnly && !String.class.isAssignableFrom(superType.getInstanceClass())) {
						actualType = superType;
						superType = getNativeSchemaType("string");
						complexType.setProperty(new ValueImpl(new ActualTypeProperty(), actualType));
					}
					
					((XMLSchemaComplexSimpleType<?>) complexType).setSuperType(superType, baseName);
					// should only contain attributes!
					Object returnValue = parseSequence(firstChildElement, namespaces, complexType);
					if (returnValue instanceof DelayedParse) {
						complexType.clear();
						return returnValue;
					}
				}
				else
					throw new ParseException("Only complex extensions of simple types are currently supported", 0);
			}
			// an empty complex type, perhaps it only has attributes?
			else {
				complexType = name == null ? new XMLSchemaComplexType(this) : new XMLSchemaDefinedComplexType(this, name);
				registerComplexType(tag, name, complexType);
			}
		}
		// after the first element we can have attributes
		org.w3c.dom.Element next = getNextSibling(firstChildElement);
		while (next != null) {
			Object parsed = parse(next, namespaces, complexType);
			if (parsed instanceof DelayedParse) {
				unregisterComplexType(tag, name, complexType);
				return parsed;
			}
			else if (parsed instanceof Element)
				complexType.addChild((Element<?>) parsed);
			else
				throw new RuntimeException("Encountered unrecognized part in complexType " + name);
			next = getNextSibling(next);
		}
		// inherit the schema settings when at the root
		if (tag.getParentNode().getLocalName().equalsIgnoreCase("schema")) {
			complexType.setProperty(new ValueImpl<Boolean>(ElementQualifiedDefaultProperty.getInstance(), isElementQualified));
			complexType.setProperty(new ValueImpl<Boolean>(AttributeQualifiedDefaultProperty.getInstance(), isAttributeQualified));
		}
		return complexType;
	}
	
	private XMLSchemaComplexType getComplexType(org.w3c.dom.Element tag, String name) {
		if (name != null) {
			if (tag.getLocalName().equalsIgnoreCase("group")) {
				return (XMLSchemaComplexType) groupRegistry.getComplexType(getNamespace(), name);
			}
			else {
				return (XMLSchemaComplexType) registry.getComplexType(getNamespace(), name);
			}
		}
		return null;
	}
	
	private void unregisterComplexType(org.w3c.dom.Element tag, String name, XMLSchemaComplexType complexType) {
		if (name != null) {
			if (tag.getLocalName().equalsIgnoreCase("group")) {
				groupRegistry.unregister(complexType);
			}
			else {
				registry.unregister(complexType);
			}
		}
	}

	private void registerComplexType(org.w3c.dom.Element tag, String name, XMLSchemaComplexType complexType) {
		// only register named complex types (anonymous can not be referenced)
		if (name != null) {
			if (tag.getLocalName().equalsIgnoreCase("group")) {
				groupRegistry.register(complexType);
			}
			else {
				registry.register(complexType);
			}
		}
	}

	
	@SuppressWarnings("unchecked")
	private Object parseSequence(org.w3c.dom.Element tag, Map<String, String> namespaces, XMLSchemaComplexType complexType) throws SAXException, ParseException, IOException {
		logger.debug("Parsing " + tag.getLocalName() + " for {}", complexType);
		List<Element<?>> childElements = new ArrayList<Element<?>>();
		for (int i = 0; i < tag.getChildNodes().getLength(); i++) {
			if (tag.getChildNodes().item(i).getNodeType() == Node.ELEMENT_NODE) {
				// you can have a choice inside a list of elements
				if (tag.getChildNodes().item(i).getLocalName().equalsIgnoreCase("choice")) {
					Object returnValue = parseSequence((org.w3c.dom.Element) tag.getChildNodes().item(i), namespaces, complexType);
					if (returnValue instanceof DelayedParse)
						return returnValue;
					else
						childElements.addAll((List<Element<?>>) returnValue);
				}
				else if (tag.getChildNodes().item(i).getLocalName().equalsIgnoreCase("group")) {
					org.w3c.dom.Element childTag = (org.w3c.dom.Element) tag.getChildNodes().item(i);
					String reference = childTag.hasAttribute("ref") ? childTag.getAttribute("ref") : null;
					if (reference == null) {
						throw new ParseException("There is a group element inside a sequence but it has no reference", 0);
					}
					ComplexType group = null;
					if (reference != null) {
						logger.debug("The complex type references group: " + reference);
						int index = reference == null ? -1 : reference.indexOf(':');
						String referenceNamespace = index >= 0 ? namespaces.get(reference.substring(0, index)) : namespaces.get(null);
						String referenceName = index >= 0 ? reference.substring(index + 1) : reference;
						group = groupRegistry.getComplexType(referenceNamespace, referenceName);
					}
					if (group == null) {
						return new DelayedParse(childTag, "Could not find group: " + reference);
					}
					// inject all the children
					childElements.addAll(TypeUtils.getAllChildren(group));
				}
				else {
					Object parsed = parse((org.w3c.dom.Element) tag.getChildNodes().item(i), namespaces, complexType);
					// delay
					if (parsed instanceof DelayedParse)
						return parsed;
					else if (parsed instanceof Element) {
						complexType.addChild((Element<?>) parsed);
						childElements.add((Element<?>) parsed);
					}
				}
			}
		}
		if (tag.getLocalName().equalsIgnoreCase("choice")) {
			logger.debug("Adding choice group with " + childElements.size() + " options to {}", complexType);
			Choice choice = new Choice(childElements.toArray(new Element[childElements.size()]));
			String minOccurs = tag.hasAttribute("minOccurs") ? tag.getAttribute("minOccurs") : null;
			String maxOccurs = tag.hasAttribute("maxOccurs") ? tag.getAttribute("maxOccurs") : null;
			if (minOccurs != null)
				choice.setProperty(new ValueImpl<Integer>(new MinOccursProperty(), new Integer(minOccurs)));
			if (maxOccurs != null)
				choice.setProperty(new ValueImpl<Integer>(new MaxOccursProperty(), maxOccurs.equals("unbounded") ? 0 : new Integer(maxOccurs)));
			complexType.addGroup(choice);
		}
		return childElements;
	}
	
	private org.w3c.dom.Element getFirstChild(org.w3c.dom.Element tag) {
		for (int i = 0; i < tag.getChildNodes().getLength(); i++) {
			if (tag.getChildNodes().item(i).getNodeType() == Node.ELEMENT_NODE && !tag.getChildNodes().item(i).getLocalName().equalsIgnoreCase("annotation"))
				return (org.w3c.dom.Element) tag.getChildNodes().item(i);
		}
		return null;
	}
	
	private org.w3c.dom.Element getNextSibling(org.w3c.dom.Element tag) {
		Node next = tag.getNextSibling();
		while (next != null) {
			if (next.getNodeType() == Node.ELEMENT_NODE && !next.getLocalName().equalsIgnoreCase("annotation"))
				return (org.w3c.dom.Element) next;
			else
				next = next.getNextSibling();
		}
		return null;
	}	
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private Object createAny(boolean isAttribute, org.w3c.dom.Element tag, ComplexType parent) {
		String minOccurs = tag.hasAttribute("minOccurs") ? tag.getAttribute("minOccurs") : null;
		String maxOccurs = tag.hasAttribute("maxOccurs") ? tag.getAttribute("maxOccurs") : null;

		// the casting system should allow you to upcast all the way to a "generic" type (with no children)
		// BeanType<Object> is conceptually the same to a "new Structure()": it has no children.
		// TODO: not entirely sure if BeanType<Object> is a good idea though, might be best to switch to Structure
		Element<?> element = new XMLSchemaAnyElement(isAttribute, this, parent);
		
		if (maxOccurs != null)
			element.setProperty(new ValueImpl(new MaxOccursProperty(), maxOccurs.equals("unbounded") ? 0 : new Integer(maxOccurs)));
		if (minOccurs != null)
			element.setProperty(new ValueImpl(new MinOccursProperty(), new Integer(minOccurs)));

		return element;
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private Object parseElement(org.w3c.dom.Element tag, Map<String, String> namespaces, ComplexType parent) throws SAXException, ParseException, IOException {
		String minOccurs = tag.hasAttribute("minOccurs") ? tag.getAttribute("minOccurs") : null;
		String maxOccurs = tag.hasAttribute("maxOccurs") ? tag.getAttribute("maxOccurs") : null;
		String type = tag.hasAttribute("type") ? tag.getAttribute("type") : null;
		String form = tag.hasAttribute("form") ? tag.getAttribute("form") : null;
		String name = tag.hasAttribute("name") ? tag.getAttribute("name") : null;
		String nillable = tag.hasAttribute("nillable") ? tag.getAttribute("nillable") : null;
		String reference = tag.hasAttribute("ref") ? tag.getAttribute("ref") : null;
		boolean isToken = false;
		boolean isNmToken = false;
		// TODO: currently unused but can add them later
//		String defaultValue = tag.hasAttribute("default") ? tag.getAttribute("default") : null;
//		String fixedValue = tag.hasAttribute("fixed") ? tag.getAttribute("fixed") : null;
		
		Element<?> element = null;
		if (reference != null) {
			logger.debug("The element references " + reference);
			int index = reference == null ? -1 : reference.indexOf(':');
			String referenceNamespace = index >= 0 ? namespaces.get(reference.substring(0, index)) : namespaces.get(null);
			String referenceName = index >= 0 ? reference.substring(index + 1) : reference;
			element = new XMLSchemaReferenceElement(parent, this, referenceNamespace, referenceName, name);
		}
		else {
			// first parse further (might be an anonymous type)
			Object child = null;
			org.w3c.dom.Element firstChildElement = getFirstChild(tag);
			if (firstChildElement != null)
				child = parse(firstChildElement, namespaces, null);
			
			Type elementType = null;
			Value<String> formatProperty = null;
			
			if (child instanceof DelayedParse)
				return child;
			else if (child instanceof Type) {
				logger.debug("Anonymous type found: {}", child);
				elementType = (Type) child;
			}
			else {
				int index = type == null ? -1 : type.indexOf(':');
				String typeNamespace = index >= 0 ? namespaces.get(type.substring(0, index)) : namespaces.get(null);
				String typeName = index >= 0 ? type.substring(index + 1) : type;
				if (type == null || NAMESPACE.equals(typeNamespace)) {
					SimpleType<?> simpleType = getNativeSchemaType(typeName);
					// if it's a date, we need to set the format
					if (Date.class.isAssignableFrom(simpleType.getInstanceClass()))
						formatProperty = new ValueImpl<String>(new FormatProperty(), typeName);
					elementType = simpleType;
					isNmToken = typeName != null && typeName.equalsIgnoreCase("nmtoken");
					isToken = typeName != null && (typeName.equalsIgnoreCase("token") || isNmToken);
				}
				else {
					elementType = getSimpleType(typeNamespace, typeName);
					if (elementType == null)
						elementType = getComplexType(typeNamespace, typeName);
				}
				// delay
				if (elementType == null)
					return new DelayedParse(tag, "Can not find complex or simple type " + typeNamespace + " # " + typeName);
			}
			
			element = tag.getParentNode().getLocalName().equals("schema") && name != null 
				? new XMLSchemaDefinedElement(this, elementType, parent, XMLContent.class)
				: new XMLSchemaElement(this, elementType, parent, XMLContent.class);
				
			// need this to maintain the explicit values for element qualified default etc!
			// in retrospect, the default values on properties were likely a mistake...
			((XMLSchemaElement) element).setMaintainDefaultValues(true);
			
			if (formatProperty != null)
				element.setProperty(formatProperty);
			
			if (name != null)
				element.setProperty(new ValueImpl<String>(new NameProperty(), name));
		}
		// set optional attributes
		if (maxOccurs != null)
			element.setProperty(new ValueImpl(MaxOccursProperty.getInstance(), maxOccurs.equals("unbounded") ? 0 : new Integer(maxOccurs)));
		if (minOccurs != null)
			element.setProperty(new ValueImpl(MinOccursProperty.getInstance(), new Integer(minOccurs)));
		// set fixed attributes
		// xml scheme has nillable false by default
		element.setProperty(new ValueImpl(NillableProperty.getInstance(), nillable != null ? new Boolean(nillable) : false));
		// default is unqualified, we can override it, if we are at the root level, we inherit the schema settings
		// note that the form attribute should not occur when the schema is the parent
		if (tag.getParentNode().getLocalName().equalsIgnoreCase("schema")) {
			element.setProperty(new ValueImpl<Boolean>(ElementQualifiedDefaultProperty.getInstance(), isElementQualified));
			element.setProperty(new ValueImpl<Boolean>(AttributeQualifiedDefaultProperty.getInstance(), isAttributeQualified));
		}
		else if (form != null) {
			element.setProperty(new ValueImpl(QualifiedProperty.getInstance(), form.equalsIgnoreCase("qualified")));
			element.setProperty(new ValueImpl<Boolean>(ElementQualifiedDefaultProperty.getInstance(), form.equalsIgnoreCase("qualified")));
		}
//		element.setProperty(new ValueImpl<Boolean>(ElementQualifiedDefaultProperty.getInstance(), isElementQualified));
//		element.setProperty(new ValueImpl<Boolean>(AttributeQualifiedDefaultProperty.getInstance(), isAttributeQualified));
		if (isToken) {
			element.setProperty(new ValueImpl<Boolean>(TokenProperty.getInstance(), true));
		}
		if (isNmToken) {
			element.setProperty(new ValueImpl<String>(PatternProperty.getInstance(), NMTOKEN_REGEX));
		}
		// only register elements that are at the root of the schema
		if (tag.getParentNode().getLocalName().equalsIgnoreCase("schema"))
			registry.register(element);
		return element;
	}
	
	public Document getDocument() {
		return document;
	}
	
	public SimpleType<?> getNativeSchemaType(String typeName) {
		SimpleType<?> nativeType = getNativeSchemaType(typeName, wrapper);
		if (nativeType == null) {
			throw new IllegalArgumentException("The xml schema type " + typeName + " is currently not supported");
		}
		return nativeType;
	}
	
	public static SimpleType<?> getNativeSchemaType(String typeName, SimpleTypeWrapper wrapper) {
		if (typeName == null || typeName.equalsIgnoreCase("string") || typeName.equalsIgnoreCase("token") || typeName.equalsIgnoreCase("nmtoken"))
			return wrapper.wrap(String.class);
		else if (typeName.equalsIgnoreCase("boolean"))
			return wrapper.wrap(Boolean.class);
		else if (typeName.equalsIgnoreCase("integer") || typeName.equalsIgnoreCase("positiveInteger") || typeName.equalsIgnoreCase("negativeInteger") || typeName.equalsIgnoreCase("nonNegativeInteger") || typeName.equalsIgnoreCase("nonPositiveInteger"))
			return wrapper.wrap(BigInteger.class);
		else if (typeName.equalsIgnoreCase("int") || typeName.equalsIgnoreCase("unsignedShort"))
			return wrapper.wrap(Integer.class);
		else if (typeName.equalsIgnoreCase("float"))
			return wrapper.wrap(Float.class);
		else if (typeName.equalsIgnoreCase("double"))
			return wrapper.wrap(Double.class);
		else if (typeName.equalsIgnoreCase("decimal"))
			return wrapper.wrap(BigDecimal.class);
		else if (typeName.equalsIgnoreCase("byte"))
			return wrapper.wrap(Byte.class);
		else if (typeName.equalsIgnoreCase("short") || typeName.equalsIgnoreCase("unsignedByte"))
			return wrapper.wrap(Short.class);
		else if (typeName.equalsIgnoreCase("long") || typeName.equalsIgnoreCase("unsignedInt"))
			return wrapper.wrap(Long.class);
		else if (typeName.equalsIgnoreCase("anyURI"))
			return wrapper.wrap(URI.class);
		else if (typeName.equalsIgnoreCase("date") || typeName.equalsIgnoreCase("dateTime") || typeName.equalsIgnoreCase("time") || typeName.equalsIgnoreCase("gMonth") || typeName.equalsIgnoreCase("gDay") || typeName.equalsIgnoreCase("gMonthDay") || typeName.equalsIgnoreCase("gYear") || typeName.equalsIgnoreCase("gYearMonth"))
			return wrapper.wrap(Date.class);
		else if (typeName.equalsIgnoreCase("base64Binary"))
			return wrapper.wrap(byte[].class);
		// interpret anySimpleType as string!
		else if (typeName.equalsIgnoreCase("anySimpleType"))
			return wrapper.wrap(String.class);
		else if (typeName.equalsIgnoreCase("duration")) {
			return wrapper.wrap(Duration.class);
		}
		return null;
	}
	
	public String getNamespace() {
		return namespace;
	}
	
	public List<Element<?>> getElements() {
		return null;
	}
	
	public List<SimpleType<?>> getSimpleTypes() {
		return null;
	}
	
	public List<ComplexType> getComplexTypes() {
		return null;
	}
	
	public void setResolver(ResourceResolver resolver) {
		this.resolver = resolver;
	}

	public ResourceResolver getResolver() {
		if (resolver == null)
			resolver = new URLResourceResolver();
		return resolver;
	}

	@Override
	public SimpleType<?> getSimpleType(String namespace, String name) {
		if (NAMESPACE.equals(namespace))
			return getNativeSchemaType(name);
		else
			return registry.getSimpleType(namespace, name);
	}

	@Override
	public ComplexType getComplexType(String namespace, String name) {
		return registry.getComplexType(namespace, name);
	}

	@Override
	public Element<?> getElement(String namespace, String name) {
		return registry.getElement(namespace, name);
	}
	
	public static Document toDocument(InputStream input) throws SAXException, IOException {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(true);
			factory.setValidating(false);
			DocumentBuilder builder = factory.newDocumentBuilder();
			return builder.parse(input);
		}
		catch (ParserConfigurationException e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	public Set<String> getNamespaces() {
		return registry.getNamespaces();
	}

	@Override
	public List<SimpleType<?>> getSimpleTypes(String namespace) {
		return registry.getSimpleTypes(namespace);
	}

	@Override
	public List<ComplexType> getComplexTypes(String namespace) {
		return registry.getComplexTypes(namespace);
	}

	@Override
	public List<Element<?>> getElements(String namespace) {
		return registry.getElements(namespace);
	}

	public void setId(String id) {
		this.id = id;
	}
	@Override
	public String getId() {
		if (id == null) {
			id = namespace;
		}
		return id;
	}

	public boolean isIgnoreInclusionFailure() {
		return ignoreInclusionFailure;
	}

	public void setIgnoreInclusionFailure(boolean ignoreInclusionFailure) {
		this.ignoreInclusionFailure = ignoreInclusionFailure;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}
	
}
