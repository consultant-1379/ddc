package com.ericsson.cifwk.diagmon.e2e;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.powermock.api.mockito.PowerMockito;
import org.w3c.dom.Document;

public class XmlCreatorUtilityTest {

    private XmlCreatorUtility xmlCreatorUtilityMock;
    private DocumentBuilderFactory documentBuilderFactoryMock;

    @Before
    public void setUp() {
        xmlCreatorUtilityMock = PowerMockito.spy(new XmlCreatorUtility());
        documentBuilderFactoryMock = PowerMockito.mock(DocumentBuilderFactory.class);
    }

    @After
    public void tearDown() {
        xmlCreatorUtilityMock = null;
        documentBuilderFactoryMock = null;
    }

    @SuppressWarnings({ "static-access", "unused" })
    @Test
    public void testGetDocumentBuilderPostive() throws ParserConfigurationException {
        DocumentBuilder documentBuilder = null;
        documentBuilder = documentBuilderFactoryMock.newDocumentBuilder();
        assertNotNull(xmlCreatorUtilityMock.getDocumentBuilder());
    }

    @SuppressWarnings("static-access")
    @Test
    public void testGetDocumentPostive() throws ParserConfigurationException {
        final DocumentBuilder documentBuilderMock = documentBuilderFactoryMock.newDocumentBuilder();
        PowerMockito.when(xmlCreatorUtilityMock.getDocumentBuilder()).thenReturn(documentBuilderMock);
        assertNotNull(xmlCreatorUtilityMock.getDocument());

    }

    @SuppressWarnings("static-access")
    @Test
    public void testGetDocumentisNullNegative() throws ParserConfigurationException {
        DocumentBuilder documentBuilder = null;
        final DocumentBuilderFactory documentBuilderFactory = documentBuilderFactoryMock.newInstance();
        documentBuilder = documentBuilderFactory.newDocumentBuilder();
        final Document documentTest = documentBuilder.newDocument();
        PowerMockito.when(xmlCreatorUtilityMock.getDocumentBuilder()).thenReturn(null);

        final Document documentReturned = xmlCreatorUtilityMock.getDocument();
        assertEquals(documentTest.toString(), documentReturned.toString());

    }

    @SuppressWarnings("static-access")
    @Test
    public void testGetTransfomerPostive() throws TransformerConfigurationException {
        assertNotNull(xmlCreatorUtilityMock.getTransformer());
    }

}
