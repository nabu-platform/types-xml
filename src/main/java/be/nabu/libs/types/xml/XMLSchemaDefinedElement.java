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
