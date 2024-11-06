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
import be.nabu.libs.types.api.CollectionHandlerProvider;
import be.nabu.libs.types.api.ComplexType;
import be.nabu.libs.types.base.StringMapCollectionHandlerProvider;
import be.nabu.libs.types.base.ValueImpl;
import be.nabu.libs.types.java.BeanType;
import be.nabu.libs.types.properties.CollectionHandlerProviderProperty;
import be.nabu.libs.types.properties.NameProperty;

public class XMLSchemaAnyElement extends XMLSchemaElement<Object> {

	@SuppressWarnings("rawtypes")
	XMLSchemaAnyElement(boolean isAttribute, XMLSchema schema, ComplexType parent, Value<?>...values) {
		super(schema, new BeanType<Object>(Object.class), parent, Object.class, values);
		setProperty(new ValueImpl<String>(new NameProperty(), isAttribute ? NameProperty.ANY_ATTRIBUTE : NameProperty.ANY));
		setProperty(new ValueImpl<CollectionHandlerProvider>(new CollectionHandlerProviderProperty(), new StringMapCollectionHandlerProvider<Object>()));
	}

}
