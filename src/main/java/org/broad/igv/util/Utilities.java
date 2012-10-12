/*
 * Copyright (c) 2007-2011 by The Broad Institute of MIT and Harvard.  All Rights Reserved.
 *
 * This software is licensed under the terms of the GNU Lesser General Public License (LGPL),
 * Version 2.1 which is available at http://www.opensource.org/licenses/lgpl-2.1.php.
 *
 * THE SOFTWARE IS PROVIDED "AS IS." THE BROAD AND MIT MAKE NO REPRESENTATIONS OR
 * WARRANTES OF ANY KIND CONCERNING THE SOFTWARE, EXPRESS OR IMPLIED, INCLUDING,
 * WITHOUT LIMITATION, WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE, NONINFRINGEMENT, OR THE ABSENCE OF LATENT OR OTHER DEFECTS, WHETHER
 * OR NOT DISCOVERABLE.  IN NO EVENT SHALL THE BROAD OR MIT, OR THEIR RESPECTIVE
 * TRUSTEES, DIRECTORS, OFFICERS, EMPLOYEES, AND AFFILIATES BE LIABLE FOR ANY DAMAGES
 * OF ANY KIND, INCLUDING, WITHOUT LIMITATION, INCIDENTAL OR CONSEQUENTIAL DAMAGES,
 * ECONOMIC DAMAGES OR INJURY TO PROPERTY AND LOST PROFITS, REGARDLESS OF WHETHER
 * THE BROAD OR MIT SHALL BE ADVISED, SHALL HAVE OTHER REASON TO KNOW, OR IN FACT
 * SHALL KNOW OF THE POSSIBILITY OF THE FOREGOING.
 */
package org.broad.igv.util;

//~--- non-JDK imports --------------------------------------------------------

import biz.source_code.base64Coder.Base64Coder;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 *
 */
public class Utilities {

    private static Logger log = Logger.getLogger(Utilities.class);

    public static String base64Encode(String str) {
        return Base64Coder.encodeString(str);

    }

    public static String base64Decode(String str) {
        return Base64Coder.decodeString(str);
    }


    /**
     * Changes a files extension.
     */
    final public static File changeFileExtension(File file, String extension) {

        if ((file == null) || (extension == null) || extension.trim().equals("")) {
            return null;
        }

        String path = file.getAbsolutePath();
        String newPath = "";
        String filename = file.getName().trim();

        if ((filename != null) && !filename.equals("")) {

            int periodIndex = path.lastIndexOf(".");

            newPath = path.substring(0, periodIndex);
            newPath += extension;
        }

        return new File(newPath);
    }

    /**
     * Reads an xml from an input stream and creates DOM document.
     *
     * @param inStream
     * @return
     * @throws ParserConfigurationException
     * @throws IOException
     * @throws SAXException
     */
    public static Document createDOMDocumentFromXmlStream(InputStream inStream)
            throws ParserConfigurationException, IOException, SAXException {

        DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document xmlDocument = documentBuilder.parse(inStream);

        return xmlDocument;
    }

    public static String getString(Document document) {
        StreamResult streamResult;
        try {

            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer transformer = factory.newTransformer();

            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

            streamResult = new StreamResult(new StringWriter());
            DOMSource source = new DOMSource(document);
            transformer.transform(source, streamResult);
        } catch (TransformerException e) {
            return null;
        }

        return streamResult.getWriter().toString();
    }

    static public Comparator getNumericStringComparator() {

        Comparator comparator = new Comparator<String>() {

            public int compare(String s1, String s2) {
                StringTokenizer st1 = new StringTokenizer(s1, " ");
                StringTokenizer st2 = new StringTokenizer(s2, " ");

                while (st1.hasMoreTokens() && st2.hasMoreTokens()) {
                    String t1 = st1.nextToken();
                    String t2 = st2.nextToken();

                    int c;

                    try {
                        Integer i1 = new Integer(t1);
                        Integer i2 = new Integer(t2);

                        c = i1.compareTo(i2);
                    } catch (NumberFormatException e) {
                        c = t1.compareTo(t2);
                    }

                    if (c != 0) {
                        return c;
                    }
                }

                return 0;
            }
        };

        return comparator;
    }

    /**
     * @param buffer
     * @return
     * @throws java.io.IOException
     */
    static private long getCrc(byte[] buffer) throws IOException {

        CRC32 crc = new CRC32();

        crc.reset();
        crc.update(buffer, 0, buffer.length);
        return crc.getValue();
    }

    /**
     * Parses the file name from a URL.
     *
     * @param url
     * @return
     */
    static public String getFileNameFromURL(String url) {

        int lastIndexOfSlash = url.lastIndexOf("/");

        String fileName = null;

        if (lastIndexOfSlash > -1) {
            fileName = url.substring(lastIndexOfSlash, url.length());
        } else {
            fileName = url;
        }

        return fileName;
    }



}
