package be.nabu.libs.types.xml;

import be.nabu.libs.types.api.DefinedType;
import be.nabu.libs.types.api.Type;

public class XMLSchemaDefinedSimpleType<T> extends XMLSchemaSimpleType<T> implements DefinedType {

	XMLSchemaDefinedSimpleType(XMLSchema schema, String name, Type superType, String baseTypeName) {
		super(schema, name, superType, baseTypeName);
	}

	@Override
	public String getId() {
		return getSchema().getId() + "." + getName();
	}
}
