package ch.supertomcat.updaterxml;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.xml.sax.SAXException;

import ch.supertomcat.updaterxml.update.xml.ObjectFactory;
import ch.supertomcat.updaterxml.update.xml.Update;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;

/**
 * Class for reading and writing the Update XML File
 */
public class UpdateXmlIO {
	/**
	 * Unmarshaller
	 */
	private final Unmarshaller unmarshaller;

	/**
	 * Unmarshaller
	 */
	private final Unmarshaller unmarshallerValidated;

	/**
	 * Marshaller
	 */
	private final Marshaller marshaller;

	/**
	 * Constructor
	 * 
	 * @throws IOException
	 * @throws SAXException
	 * @throws JAXBException
	 */
	public UpdateXmlIO() throws IOException, SAXException, JAXBException {
		JAXBContext jaxbContext = JAXBContext.newInstance(ObjectFactory.class);
		Schema schema;
		try (InputStream schemaIn = getClass().getResourceAsStream("/ch/supertomcat/updaterxml/update/update.xsd")) {
			SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
			Source schemaSource = new StreamSource(schemaIn);
			schema = sf.newSchema(schemaSource);
		}

		unmarshallerValidated = jaxbContext.createUnmarshaller();
		unmarshallerValidated.setSchema(schema);

		unmarshaller = jaxbContext.createUnmarshaller();

		marshaller = jaxbContext.createMarshaller();
		marshaller.setSchema(schema);
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
	}

	/**
	 * Read updates from XML file
	 * 
	 * @param file XML File
	 * @param validate True if XSD validation should be done, false otherwise
	 * @return Updates
	 * @throws IOException
	 * @throws JAXBException
	 */
	public Update readUpdate(Path file, boolean validate) throws IOException, JAXBException {
		try (InputStream in = Files.newInputStream(file)) {
			return readUpdate(in, validate);
		}
	}

	/**
	 * Read updates from XML file
	 * 
	 * @param in Input Stream
	 * @param validate True if XSD validation should be done, false otherwise
	 * @return Updates
	 * @throws JAXBException
	 */
	public Update readUpdate(InputStream in, boolean validate) throws JAXBException {
		if (validate) {
			synchronized (unmarshallerValidated) {
				return unmarshallerValidated.unmarshal(new StreamSource(in), Update.class).getValue();
			}
		} else {
			synchronized (unmarshaller) {
				return unmarshaller.unmarshal(new StreamSource(in), Update.class).getValue();
			}
		}
	}

	/**
	 * Saves the updates to the XML-File
	 * 
	 * @param file XML File
	 * @param updates Updates
	 * 
	 * @throws IOException
	 * @throws JAXBException
	 */
	public void writeUpdate(Path file, Update updates) throws IOException, JAXBException {
		Path directory = file.toAbsolutePath().getParent();
		Files.createDirectories(directory);

		try (OutputStream out = Files.newOutputStream(file)) {
			synchronized (marshaller) {
				marshaller.marshal(updates, out);
			}
		}
	}
}
