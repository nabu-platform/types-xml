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
