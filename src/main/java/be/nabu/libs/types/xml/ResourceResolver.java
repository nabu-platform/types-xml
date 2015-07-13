package be.nabu.libs.types.xml;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import be.nabu.libs.types.api.TypeRegistry;

public interface ResourceResolver {
	public InputStream resolve(URI uri) throws IOException;
	public TypeRegistry resolve(String namespace) throws IOException;
}
