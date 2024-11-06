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

import be.nabu.libs.types.api.DefinedSimpleType;
import be.nabu.libs.types.api.Type;

public class XMLSchemaDefinedSimpleType<T> extends XMLSchemaSimpleType<T> implements DefinedSimpleType<T> {

	XMLSchemaDefinedSimpleType(XMLSchema schema, String name, Type superType, String baseTypeName) {
		super(schema, name, superType, baseTypeName);
	}

	@Override
	public String getId() {
		return getSchema().getId() + "." + getName();
	}
}
