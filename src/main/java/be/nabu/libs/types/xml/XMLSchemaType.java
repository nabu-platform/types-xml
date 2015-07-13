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
	
	XMLSchema getSchema() {
		return schema;
	}
}
