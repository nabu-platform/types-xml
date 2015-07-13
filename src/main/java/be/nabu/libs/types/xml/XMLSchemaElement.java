package be.nabu.libs.types.xml;

import be.nabu.libs.property.api.Value;
import be.nabu.libs.types.api.ComplexType;
import be.nabu.libs.types.api.Element;
import be.nabu.libs.types.api.Type;
import be.nabu.libs.types.base.ElementImpl;
import be.nabu.libs.types.base.ValueImpl;
import be.nabu.libs.types.properties.NamespaceProperty;

public class XMLSchemaElement<T> extends ElementImpl<T> implements Element<T> {

	private Class<T> valueClass;
	private XMLSchema schema;
	
	XMLSchemaElement(XMLSchema schema, Type type, ComplexType parent, Class<T> valueClass, Value<?>...values) {
		super(type, parent, values);
		this.schema = schema;
		this.valueClass = valueClass;
		setProperty(new ValueImpl<String>(new NamespaceProperty(), schema.getNamespace()));
	}

	@Override
	public Class<T> getValueClass() {
		return valueClass;
	}

	public XMLSchema getSchema() {
		return schema;
	}
	
	@Override
	public String toString() {
		return "element " + schema.getNamespace() + " # " + getName();
	}

}
