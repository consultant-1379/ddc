package com.ericsson.cifwk.diagmon.e2e;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;

import org.w3c.dom.Document;

/**
 * @author eswavin
 */
public class XmlCreatorUtility {

    protected static DocumentBuilder getDocumentBuilder() throws ParserConfigurationException {
        DocumentBuilder documentBuilder = null;
        final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilder = documentBuilderFactory.newDocumentBuilder();
        return documentBuilder;
    }

    protected static Document getDocument() throws ParserConfigurationException {
        Document document = null;
        final DocumentBuilder documentBuilder = getDocumentBuilder();
        if (documentBuilder != null) {
            document = documentBuilder.newDocument();
        }
        return document;
    }

    protected static Transformer getTransformer() throws TransformerConfigurationException {
        final TransformerFactory transformerFactory = TransformerFactory.newInstance();
        return transformerFactory.newTransformer();
    }

}
