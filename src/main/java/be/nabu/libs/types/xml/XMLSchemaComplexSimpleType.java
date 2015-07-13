package be.nabu.libs.types.xml;

import be.nabu.libs.types.api.ComplexType;
import be.nabu.libs.types.api.Element;
import be.nabu.libs.types.api.SimpleType;
import be.nabu.libs.types.base.SimpleElementImpl;

public class XMLSchemaComplexSimpleType<T> extends XMLSchemaComplexType implements SimpleType<T> {

	private String baseTypeName;
	
	XMLSchemaComplexSimpleType(XMLSchema schema) {
		super(schema, null);
	}
	
	XMLSchemaComplexSimpleType(XMLSchema schema, String name) {
		super(schema, name);
	}

	@SuppressWarnings("unchecked")
	@Override
	public Class<T> getInstanceClass() {
		return (Class<T>) ((SimpleType<?>) getSuperType()).getInstanceClass();
	}

	void setSuperType(SimpleType<?> superType, String baseTypeName) {
		super.setSuperType(superType);
		this.baseTypeName = baseTypeName;
	}
	
	public String getBaseTypeName() {
		return baseTypeName;
	}
	
	@Override
	public String toString() {
		return "complex simpleType " + getSchema().getNamespace() + " # " + getName();
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public Element<?> get(String path) {
		if (path.equals(ComplexType.SIMPLE_TYPE_VALUE))
			return new SimpleElementImpl((SimpleType) getSuperType(), null, getProperties());
		else
			return super.get(path);
	}
	
}
