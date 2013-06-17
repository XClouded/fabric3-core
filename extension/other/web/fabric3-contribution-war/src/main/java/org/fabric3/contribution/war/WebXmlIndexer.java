/*
* Fabric3
* Copyright (c) 2009-2013 Metaform Systems
*
* Fabric3 is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as
* published by the Free Software Foundation, either version 3 of
* the License, or (at your option) any later version, with the
* following exception:
*
* Linking this software statically or dynamically with other
* modules is making a combined work based on this software.
* Thus, the terms and conditions of the GNU General Public
* License cover the whole combination.
*
* As a special exception, the copyright holders of this software
* give you permission to link this software with independent
* modules to produce an executable, regardless of the license
* terms of these independent modules, and to copy and distribute
* the resulting executable under terms of your choice, provided
* that you also meet, for each linked independent module, the
* terms and conditions of the license of that module. An
* independent module is a module which is not derived from or
* based on this software. If you modify this software, you may
* extend this exception to your version of the software, but
* you are not obligated to do so. If you do not wish to do so,
* delete this exception statement from your version.
*
* Fabric3 is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty
* of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
* See the GNU General Public License for more details.
*
* You should have received a copy of the
* GNU General Public License along with Fabric3.
* If not, see <http://www.gnu.org/licenses/>.
*/
package org.fabric3.contribution.war;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamReader;

import org.oasisopen.sca.annotation.EagerInit;
import org.oasisopen.sca.annotation.Init;
import org.oasisopen.sca.annotation.Property;
import org.oasisopen.sca.annotation.Reference;

import org.fabric3.spi.contribution.Resource;
import org.fabric3.spi.contribution.ResourceElement;
import org.fabric3.spi.contribution.manifest.QNameSymbol;
import org.fabric3.spi.contribution.xml.XmlIndexer;
import org.fabric3.spi.contribution.xml.XmlIndexerRegistry;
import org.fabric3.spi.introspection.IntrospectionContext;

/**
 * Adds an index entry for the web.xml descriptor to the symbol space of a WAR contribution.
 */
@EagerInit
public class WebXmlIndexer implements XmlIndexer {
    private static final QName WEB_APP_NO_NAMESPACE = new QName(null, "web-app");
    private static final QName WEB_APP_J2EE_NAMESPACE = new QName("http://java.sun.com/xml/ns/j2ee", "web-app");
    private static final QName WEB_APP_JAVAEE_NAMESPACE = new QName("http://java.sun.com/xml/ns/javaee", "web-app");

    private XmlIndexerRegistry registry;
    private QName qname;

    public WebXmlIndexer(@Reference XmlIndexerRegistry registry, @Property(name = "namespace") String namespace) {
        this.registry = registry;
        if ("none".equals(namespace)) {
            qname = WEB_APP_NO_NAMESPACE;
        } else if ("javaee".equals(namespace)) {
            qname = WEB_APP_JAVAEE_NAMESPACE;
        } else if ("j2ee".equals(namespace)) {
            qname = WEB_APP_J2EE_NAMESPACE;
        }
    }

    @Init
    public void init() {
        registry.register(this);
    }

    public QName getType() {
        return qname;
    }

    public void index(Resource resource, XMLStreamReader reader, IntrospectionContext context) {
        QNameSymbol symbol = new QNameSymbol(qname);
        WebXml webXml = new WebXml();
        ResourceElement<QNameSymbol, WebXml> element = new ResourceElement<QNameSymbol, WebXml>(symbol, webXml);
        resource.addResourceElement(element);
    }

}