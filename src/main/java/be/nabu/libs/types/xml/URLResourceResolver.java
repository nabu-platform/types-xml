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
