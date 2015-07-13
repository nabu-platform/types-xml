package be.nabu.libs.types.xml;

import be.nabu.libs.property.api.Value;
import be.nabu.libs.types.api.Attribute;
import be.nabu.libs.types.api.ComplexType;
import be.nabu.libs.types.api.SimpleType;
import be.nabu.libs.types.api.Type;
import be.nabu.libs.types.base.ElementImpl;

public class XMLSchemaAttribute<T> extends ElementImpl<T> implements Attribute<T> {

	public XMLSchemaAttribute(XMLSchema schema, String name, Type type, ComplexType parent, Value<?>...values) {
		super(name, type, parent, values);
	}

	@SuppressWarnings("unchecked")
	@Override
	public SimpleType<T> getType() {
		return (SimpleType<T>) super.getType();
	}

	@Override
	public Class<T> getValueClass() {
		return getType().getInstanceClass();
	}
}
