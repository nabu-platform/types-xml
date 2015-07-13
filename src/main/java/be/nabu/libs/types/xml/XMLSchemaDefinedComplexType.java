package be.nabu.libs.types.xml;

import be.nabu.libs.types.api.DefinedType;

public class XMLSchemaDefinedComplexType extends XMLSchemaComplexType implements DefinedType {
	
	XMLSchemaDefinedComplexType(XMLSchema schema, String name) {
		super(schema, name);
	}

	@Override
	public String getId() {
		return getSchema().getId() + "." + getName();
	}

}
