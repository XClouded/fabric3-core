/*
 * Fabric3
 * Copyright (c) 2009-2015 Metaform Systems
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Portions originally based on Apache Tuscany 2007
 * licensed under the Apache 2.0 license.
 */
package org.fabric3.introspection.xml.composite;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import java.io.ByteArrayInputStream;
import java.net.URI;

import junit.framework.TestCase;
import org.fabric3.api.model.type.component.Property;
import org.fabric3.introspection.xml.DefaultLoaderHelper;
import org.fabric3.introspection.xml.LoaderRegistryImpl;
import org.fabric3.spi.introspection.DefaultIntrospectionContext;
import org.fabric3.spi.introspection.IntrospectionContext;
import org.fabric3.spi.introspection.xml.LoaderHelper;
import org.fabric3.spi.introspection.xml.LoaderRegistry;

/**
 *
 */
public class ComponentLoaderDuplicatePropertyTestCase extends TestCase {
    private static final QName IMPLEMENTATION_TESTING = new QName(org.fabric3.api.Namespaces.F3, "implementation.testing");

    private static String XML = "<component xmlns='http://docs.oasis-open.org/ns/opencsa/sca/200912' name='component' " + "xmlns:f3='"
                                + org.fabric3.api.Namespaces.F3 + "'>" + "<f3:implementation.testing/>" + "<property name='prop'><value>val</value></property>"
                                + "<property name='prop'><value>val</value></property>" + "</component>";

    private ComponentLoader loader;
    private XMLStreamReader reader;
    private IntrospectionContext ctx;

    /**
     * Verifies an exception is thrown if an attempt is made to configure a property twice.
     *
     * @throws Exception on test failure
     */
    public void testDuplicateProperty() throws Exception {
        loader.load(reader, ctx);
        assertTrue(ctx.getErrors().get(0) instanceof DuplicateConfiguredProperty);
    }

    protected void setUp() throws Exception {
        super.setUp();
        LoaderRegistry registry = new LoaderRegistryImpl();
        LoaderHelper helper = new DefaultLoaderHelper();
        PropertyValueLoader pvLoader = new PropertyValueLoader(registry, helper);
        pvLoader.init();

        MockImplementationLoader implLoader = new MockImplementationLoader();
        implLoader.setProperties(new Property("prop"));
        registry.registerLoader(IMPLEMENTATION_TESTING, implLoader);
        loader = new ComponentLoader(registry, helper);

        reader = XMLInputFactory.newFactory().createXMLStreamReader(new ByteArrayInputStream(XML.getBytes()));
        reader.nextTag();
        ctx = new DefaultIntrospectionContext(URI.create("parent"), getClass().getClassLoader(), null, "foo");
    }

}