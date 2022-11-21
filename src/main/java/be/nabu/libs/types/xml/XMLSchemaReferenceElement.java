package be.nabu.libs.types.xml;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import be.nabu.libs.property.api.Property;
import be.nabu.libs.property.api.Value;
import be.nabu.libs.types.api.ComplexType;
import be.nabu.libs.types.api.Element;
import be.nabu.libs.types.api.Type;
import be.nabu.libs.types.base.ValueImpl;
import be.nabu.libs.types.properties.NameProperty;
import be.nabu.libs.types.properties.NamespaceProperty;
import be.nabu.libs.validator.api.Validation;

public class XMLSchemaReferenceElement<T> implements Element<T> {

	private XMLSchema schema;
	private String referenceNamespace, referenceName;
	private String name;
	private Element<T> reference;
	private Map<Property<?>, Value<?>> properties = new LinkedHashMap<Property<?>, Value<?>>();
	private ComplexType parent;
	private boolean propertiesCopied;
	
	XMLSchemaReferenceElement(ComplexType parent, XMLSchema schema, String referenceNamespace, String referenceName, String name) {
		this.schema = schema;
		this.referenceNamespace = referenceNamespace;
		this.referenceName = referenceName;
		this.name = name == null ? referenceName : name;
		this.parent = parent;
	}
	
	@SuppressWarnings("unchecked")
	public Element<T> getReference() {
		if (reference == null) 
			reference = (Element<T>) schema.getElement(referenceNamespace, referenceName);
		return reference;
	}
	
	@Override
	public List<? extends Validation<?>> validate(T instance) {
		return getReference().validate(instance);
	}

	@Override
	public Class<T> getValueClass() {
		return getReference().getValueClass();
	}

	@Override
	public Type getType() {
		return getReference().getType();
	}

	@Override
	public <S> Value<S> getProperty(Property<S> property) {
		return getReference().getProperty(property);
	}

	@Override
	public Value<?>[] getProperties() {
//		return properties.values().toArray(new Value<?> [properties.size()]);
		// we do this lazily because it might not be available at the start
		if (!propertiesCopied) {
			synchronized (this) {
				if (!propertiesCopied) {
					// inherit the properties
					setProperty(getReference().getProperties());
					// make sure we have the correct name
					setProperty(new ValueImpl<String>(NameProperty.getInstance(), name));
					propertiesCopied = true;
				}
			}
		}
		return properties.values().toArray(new Value<?> [properties.size()]);
	}

	@Override
	public ComplexType getParent() {
		return parent;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getNamespace() {
//		return schema.getNamespace();
		// it "seems" the original namespace is maintained when using a ref
		return referenceNamespace == null ? schema.getNamespace() : referenceNamespace;
	}

	@Override
	public void setProperty(Value<?>...values) {
		for (Value<?> value : values)
			properties.put(value.getProperty(), value);
	}

	@Override
	public Set<Property<?>> getSupportedProperties() {
		return getReference().getSupportedProperties();
	}

	@Override
	public String toString() {
		return "reference element " + schema.getNamespace() + " # " + getName() + " referencing " + referenceName;
	}
}
