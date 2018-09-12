package com.meem.utils;

import com.meem.androidapp.UiContext;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * @author Arun T A
 */

public class XMLDOMParser {
    UiContext mUiCtxt = UiContext.getInstance();
    InputStream ins;

    public Document getDomElement(String xmlFileName) {
        Document doc = null;
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {
            ins = new FileInputStream(new File(xmlFileName));

            DocumentBuilder db = dbf.newDocumentBuilder();

            InputSource is = new InputSource(ins);
            doc = db.parse(is);
        } catch (ParserConfigurationException ex) {
            mUiCtxt.log(UiContext.ERROR, GenUtils.getStackTrace(ex));
            return null;
        } catch (SAXException ex) {
            mUiCtxt.log(UiContext.ERROR, GenUtils.getStackTrace(ex));
            return null;
        } catch (IOException ex) {
            mUiCtxt.log(UiContext.ERROR, GenUtils.getStackTrace(ex));
            return null;
        }
        // return DOM
        return doc;
    }

    public String getValue(Element item, String str) {
        NodeList n = item.getElementsByTagName(str);
        return this.getElementValue(n.item(0));
    }

    public String getAttribute(Element item, String name) {
        return item.getAttribute(name);
    }

    public final String getElementValue(Node elem) {
        Node child;
        if (elem != null) {
            if (elem.hasChildNodes()) {
                for (child = elem.getFirstChild(); child != null; child = child.getNextSibling()) {
                    if (child.getNodeType() == Node.TEXT_NODE) {
                        return child.getNodeValue();
                    }
                }
            }
        }
        return null;
    }
}
