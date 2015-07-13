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
