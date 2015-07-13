# XML

This library contains an XML-based implementation of the types api. More specifically it contains two things:

- **XML Schema**: can expose XML Schema files as type definitions
- **XML Document**: can expose runtime XML documents as type instances (using DOM Document)

Note that read-support for DOM Document is generally ok but there are still some edge cases for write support in combination with collections. Unless you actually have an XML at runtime, it is adviseable to use a structure instance in combination with the XML Schema definition.
