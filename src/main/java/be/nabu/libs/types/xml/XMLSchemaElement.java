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
import be.nabu.libs.types.api.ComplexType;
import be.nabu.libs.types.api.Element;
import be.nabu.libs.types.api.Type;
import be.nabu.libs.types.base.ElementImpl;
import be.nabu.libs.types.base.ValueImpl;
import be.nabu.libs.types.properties.NamespaceProperty;

public class XMLSchemaElement<T> extends ElementImpl<T> implements Element<T> {

	private Class<T> valueClass;
	private XMLSchema schema;
	
	XMLSchemaElement(XMLSchema schema, Type type, ComplexType parent, Class<T> valueClass, Value<?>...values) {
		super(type, parent, values);
		this.schema = schema;
		this.valueClass = valueClass;
		setProperty(new ValueImpl<String>(new NamespaceProperty(), schema.getNamespace()));
	}

	@Override
	public Class<T> getValueClass() {
		return valueClass;
	}

	public XMLSchema getSchema() {
		return schema;
	}
	
	@Override
	public String toString() {
		return "element " + schema.getNamespace() + " # " + getName();
	}

}
