package be.nabu.libs.types.xml;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import be.nabu.libs.types.api.TypeRegistry;

public class URLResourceResolver implements ResourceResolver {

	@Override
	public InputStream resolve(URI uri) throws IOException {
		return uri.toURL().openStream();
	}

	@Override
	public TypeRegistry resolve(String namespace) throws IOException {
		throw new IOException("Can not resolve namespaces in a pure XML Schema context, you need to set the schemaLocation");
	}
}
