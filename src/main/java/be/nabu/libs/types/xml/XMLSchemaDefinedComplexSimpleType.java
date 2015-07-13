package be.nabu.libs.types.xml;

import be.nabu.libs.types.api.DefinedType;

public class XMLSchemaDefinedComplexSimpleType<T> extends XMLSchemaComplexSimpleType<T> implements DefinedType {

	XMLSchemaDefinedComplexSimpleType(XMLSchema schema, String name) {
		super(schema, name);
	}

	@Override
	public String getId() {
		return getSchema().getId() + "." + getName();
	}

}
