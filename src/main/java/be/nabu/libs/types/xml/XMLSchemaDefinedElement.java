package be.nabu.libs.types.xml;

import be.nabu.libs.artifacts.api.Artifact;
import be.nabu.libs.property.api.Value;
import be.nabu.libs.types.api.ComplexType;
import be.nabu.libs.types.api.Type;

public class XMLSchemaDefinedElement<T> extends XMLSchemaElement<T> implements Artifact {
	
	XMLSchemaDefinedElement(XMLSchema schema, Type type, ComplexType parent, Class<T> valueClass, Value<?>...values) {
		super(schema, type, parent, valueClass, values);
	}

	@Override
	public String getId() {
		return getSchema().getId() + "." + getName();
	}
}
