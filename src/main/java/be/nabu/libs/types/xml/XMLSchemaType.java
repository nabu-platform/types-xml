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

import java.util.LinkedHashMap;
import java.util.Map;

import be.nabu.libs.property.ValueUtils;
import be.nabu.libs.property.api.Property;
import be.nabu.libs.property.api.PropertyWithDefault;
import be.nabu.libs.property.api.Value;
import be.nabu.libs.types.base.BaseType;

public class XMLSchemaType<T> extends BaseType<T> {

	private XMLSchema schema;
	private String name;
	private Map<Property<?>, Value<?>> properties = new LinkedHashMap<Property<?>, Value<?>>();
	
	XMLSchemaType(XMLSchema schema, String name) {
		this.schema = schema;
		this.name = name;
	}
	
	@Override
	public String getName(Value<?>... values) {
		return name;
	}

	@Override
	public String getNamespace(Value<?>... values) {
		return schema.getNamespace();
	}

	@SuppressWarnings("unchecked")
	public <S> Value<S> getProperty(Property<S> property) {
		return (Value<S>) ValueUtils.getValue(property, getProperties());
	}
	
	@Override
	public void setProperty(Value<?>...values) {
		for (Value<?> value : values) {
			if (value.getProperty() instanceof PropertyWithDefault && value.getValue() != null && value.getValue().equals(((PropertyWithDefault<?>) value.getProperty()).getDefault()))
				properties.remove(value.getProperty());
			else
				properties.put(value.getProperty(), value);
		}
	}
	
	public Value<?>[] getProperties() {
		return properties.values().toArray(new Value<?>[properties.size()]);
	}
	
	public XMLSchema getSchema() {
		return schema;
	}
	
	protected void clear() {
		properties.clear();
	}
}
