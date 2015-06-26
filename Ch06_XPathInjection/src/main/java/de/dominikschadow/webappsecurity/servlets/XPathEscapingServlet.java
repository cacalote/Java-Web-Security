/*
 * Copyright (C) 2015 Dominik Schadow, info@dominikschadow.de
 *
 * This file is part of the Java-Web-Security project.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.dominikschadow.webappsecurity.servlets;

import org.owasp.esapi.ESAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.annotation.PostConstruct;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Servlet using an XPath expression to query the customer XML document.
 * User input is escaped before being used in the XPath expression.
 * <p>
 * Use <code>' or '1' = '1</code> or <code>'] | /* | /foo[bar='</code> as password.
 *
 * @author Dominik Schadow
 */
@WebServlet(name = "XPathEscapingServlet", urlPatterns = {"/XPathEscapingServlet"})
public class XPathEscapingServlet extends HttpServlet {
    private static final Logger LOGGER = LoggerFactory.getLogger(XPathEscapingServlet.class);
    private static final long serialVersionUID = 1L;
    private Document doc;

    @PostConstruct
    public void init() {
        try {
            File xmlFile = new File("src/main/resources/customer.xml");
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            doc = dBuilder.parse(xmlFile);
        } catch (SAXException | IOException | ParserConfigurationException ex) {
            LOGGER.error(ex.getMessage(), ex);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException {
        String name = request.getParameter("name");
        String password = request.getParameter("password");
        LOGGER.info("Received " + name + " and " + password + " as parameter");

        String safeName = ESAPI.encoder().encodeForXPath(name);
        String safePassword = ESAPI.encoder().encodeForXPath(password);
        LOGGER.info("Using safe name " + safeName + " and " + safePassword);

        StringBuilder xpathExpression = new StringBuilder();
        xpathExpression.append("/customers/customer[name='");
        xpathExpression.append(safeName);
        xpathExpression.append("' and @password='");
        xpathExpression.append(safePassword);
        xpathExpression.append("']/orderLimit");

        printOrderLimit(xpathExpression.toString(), name, response);
    }

    private void printOrderLimit(String xpath, String name, HttpServletResponse response) {
        LOGGER.info("XPath expression is " + xpath);

        try (PrintWriter out = response.getWriter()) {
            XPathExpression expression = XPathFactory.newInstance().newXPath().compile(xpath);
            Object result = expression.evaluate(doc, XPathConstants.NODESET);

            response.setContentType("text/html");

            out.println("<html>");
            out.println("<head><link rel=\"stylesheet\" type=\"text/css\" href=\"styles.css\" /></head>");
            out.println("<body>");
            out.println("<h1>Ch06_XPathInjection</h1>");
            out.println("<h2>Order limit for " + name + "</h2>");

            NodeList nodes = (NodeList) result;
            for (int i = 0; i < nodes.getLength(); i++) {
                out.println("<p>" + nodes.item(i).getTextContent() + "</p>");
            }

            out.println("</body>");
            out.println("</html>");
        } catch (XPathExpressionException | IOException ex) {
            LOGGER.error(ex.getMessage(), ex);
        }
    }
}
