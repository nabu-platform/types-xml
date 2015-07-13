package be.nabu.libs.types.xml;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.xml.sax.SAXException;

import be.nabu.libs.property.ValueUtils;
import be.nabu.libs.property.api.Value;
import be.nabu.libs.types.ParsedPath;
import be.nabu.libs.types.TypeUtils.ComplexTypeValidator;
import be.nabu.libs.types.api.Attribute;
import be.nabu.libs.types.api.ComplexContent;
import be.nabu.libs.types.api.ComplexType;
import be.nabu.libs.types.api.Element;
import be.nabu.libs.types.api.Group;
import be.nabu.libs.types.api.Type;
import be.nabu.libs.types.base.RootElement;
import be.nabu.libs.types.properties.AttributeQualifiedDefaultProperty;
import be.nabu.libs.types.properties.ElementQualifiedDefaultProperty;
import be.nabu.libs.validator.MultipleValidator;
import be.nabu.libs.validator.api.Validator;

public class XMLSchemaComplexType extends XMLSchemaType<XMLContent> implements ComplexType {

	/**
	 * Ordering is important
	 */
	private Map<String, Element<?>> children = new LinkedHashMap<String, Element<?>>();
	private List<Group> groups = new ArrayList<Group>();
	
	XMLSchemaComplexType(XMLSchema schema) {
		super(schema, null);
	}
	
	XMLSchemaComplexType(XMLSchema schema, String name) {
		super(schema, name);
	}

	@Override
	public Iterator<Element<?>> iterator() {
		return children.values().iterator();
	}

	void addChild(Element<?>...children) {
		for (Element<?> child : children)
			this.children.put((child instanceof Attribute ? "@" : "") + child.getName(), child);
	}

	@Override
	public Element<?> get(String path) {
		ParsedPath parsed = new ParsedPath(path);
		Element<?> child = children.get(parsed.getName());
		return parsed.getChildPath() != null
			? ((ComplexType) child.getType()).get(parsed.getChildPath().toString())
			: child;
	}

	@Override
	public ComplexContent newInstance() {
		// TODO: the collection support in XMLContent has to be revisited
		// with it, we should look at implementing xsd any in the same way
		try {
			return new XMLContent(this);
		}
		catch (SAXException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Boolean isAttributeQualified(Value<?>...values) {
		Boolean result = ValueUtils.getValue(new AttributeQualifiedDefaultProperty(), values);
		if (result == null)
			ValueUtils.getValue(new AttributeQualifiedDefaultProperty(), getProperties());
		return result;
	}

	@Override
	public Boolean isElementQualified(Value<?>... values) {
		Boolean result = ValueUtils.getValue(new ElementQualifiedDefaultProperty(), values);
		if (result == null)
			ValueUtils.getValue(new ElementQualifiedDefaultProperty(), getProperties());
		return result;
	}

	@Override
	public Group[] getGroups() {
		return groups.toArray(new Group[groups.size()]);
	}
	
	@Override
	protected void setSuperType(Type superType) {
		super.setSuperType(superType);
	}
	
	void addGroup(Group...groups) {
		this.groups.addAll(Arrays.asList(groups));
	}
	
	@Override
	public String toString() {
		return "complexType " + getSchema().getNamespace() + " # " + getName();
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public Validator<XMLContent> createValidator(Value<?>... values) {
		List<Validator> validators = new ArrayList<Validator>();
		validators.add(super.createValidator(values));
		validators.add(new XMLSchemaComplexTypeValidator(this));
		for (Group group : groups)
			validators.add(group.createValidator(values));
		return new MultipleValidator<XMLContent>(validators.toArray(new Validator[validators.size()]));
	}
	
	public class XMLSchemaComplexTypeValidator extends ComplexTypeValidator {

		public XMLSchemaComplexTypeValidator(ComplexType type) {
			super(type);
		}

		@Override
		protected ComplexContent convert(Object instance) {
			return new XMLContent((org.w3c.dom.Element) instance, new RootElement(XMLSchemaComplexType.this));
		}
	}
}
