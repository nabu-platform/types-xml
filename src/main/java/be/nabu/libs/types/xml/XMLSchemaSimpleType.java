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

import be.nabu.libs.property.api.Value;
import be.nabu.libs.types.api.Marshallable;
import be.nabu.libs.types.api.SimpleType;
import be.nabu.libs.types.api.Type;
import be.nabu.libs.types.api.Unmarshallable;
import be.nabu.libs.validator.api.Validator;

public class XMLSchemaSimpleType<T> extends XMLSchemaType<T> implements SimpleType<T>, Marshallable<T>, Unmarshallable<T> {

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
		Class clazz = getSuperType() == null ? String.class : ((SimpleType) getSuperType()).getInstanceClass();
		return (Class<T>) clazz;
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

	@SuppressWarnings("unchecked")
	@Override
	public T unmarshal(String content, Value<?>... values) {
		return ((Unmarshallable<T>) getSuperType()).unmarshal(content, values);
	}

	@SuppressWarnings("unchecked")
	@Override
	public String marshal(T object, Value<?>... values) {
		return ((Marshallable<T>) getSuperType()).marshal(object, values);
	}
}
