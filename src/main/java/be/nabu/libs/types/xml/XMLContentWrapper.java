package be.nabu.libs.types.xml;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import be.nabu.libs.types.api.ComplexContent;
import be.nabu.libs.types.api.ComplexContentWrapper;

public class XMLContentWrapper implements ComplexContentWrapper<Node> {

	@Override
	public ComplexContent wrap(Node instance) {
		if (instance instanceof Document) {
			return new XMLContent(((Document) instance).getDocumentElement());
		}
		else if (instance instanceof Element) {
			return new XMLContent((Element) instance);
		}
		return null;
	}

	@Override
	public Class<Node> getInstanceClass() {
		return Node.class;
	}

}
