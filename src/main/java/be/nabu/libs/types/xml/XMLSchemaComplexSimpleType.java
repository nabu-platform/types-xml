/*
* Copyright (C) 2015 Alexander Verbruggen
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU Lesser General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public License
* along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

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
