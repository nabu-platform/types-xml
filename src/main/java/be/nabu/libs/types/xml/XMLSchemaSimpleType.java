package be.nabu.libs.types.xml;

import be.nabu.libs.property.api.Value;
import be.nabu.libs.types.api.SimpleType;
import be.nabu.libs.types.api.Type;
import be.nabu.libs.validator.api.Validator;

public class XMLSchemaSimpleType<T> extends XMLSchemaType<T> implements SimpleType<T> {

	private String baseTypeName;

	XMLSchemaSimpleType(XMLSchema schema, Type superType, String baseTypeName) {
		this(schema, null, superType, baseTypeName);
	}

	XMLSchemaSimpleType(XMLSchema schema, String name, Type superType, String baseTypeName) {
		super(schema, name);
		if (superType != null)
			super.setSuperType(superType);
		this.baseTypeName = baseTypeName;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public Class<T> getInstanceClass() {
		return ((SimpleType) getSuperType()).getInstanceClass();
	}

	String getBaseTypeName() {
		return baseTypeName;
	}
	
	@Override
	public String toString() {
		return "simpleType " + getSchema().getNamespace() + " # " + getName();
	}

	/**
	 * Always extends a basic type which has its own validator
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Validator<T> createValidator(Value<?>...values) {
		Type type = this;
		while (type.getSuperType() != null)
			type = type.getSuperType();
		return (Validator<T>) type.createValidator(values);
	}
}
