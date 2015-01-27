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
 * Portions originally based on Apache Tuscany 2007
 * licensed under the Apache 2.0 license.
 */
package org.fabric3.databinding.jaxb.introspection;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSchema;
import javax.xml.bind.annotation.XmlType;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import java.awt.Image;
import java.beans.Introspector;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.util.Calendar;
import java.util.Date;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.fabric3.api.model.type.contract.DataType;
import org.fabric3.api.model.type.contract.Operation;
import org.fabric3.databinding.jaxb.mapper.JAXBQNameMapper;
import org.fabric3.spi.introspection.IntrospectionContext;
import org.fabric3.spi.introspection.java.contract.TypeIntrospector;
import org.fabric3.spi.model.type.java.JavaType;
import org.oasisopen.sca.annotation.Reference;
import static javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI;

/**
 * Introspects operations for the presence of JAXB types. If a parameter is a JAXB type, the JAXB intent is added to the operation.
 */
public class JAXBTypeIntrospector implements TypeIntrospector {
    private static final String JAXB = "JAXB";
    private static final String DEFAULT = "##default";
    private static final Map<Class, QName> JAXB_MAPPING;

    static {
        JAXB_MAPPING = new IdentityHashMap<>();
        JAXB_MAPPING.put(Boolean.TYPE, new QName(W3C_XML_SCHEMA_NS_URI, "boolean"));
        JAXB_MAPPING.put(Byte.TYPE, new QName(W3C_XML_SCHEMA_NS_URI, "byte"));
        JAXB_MAPPING.put(Short.TYPE, new QName(W3C_XML_SCHEMA_NS_URI, "short"));
        JAXB_MAPPING.put(Integer.TYPE, new QName(W3C_XML_SCHEMA_NS_URI, "int"));
        JAXB_MAPPING.put(Long.TYPE, new QName(W3C_XML_SCHEMA_NS_URI, "long"));
        JAXB_MAPPING.put(Float.TYPE, new QName(W3C_XML_SCHEMA_NS_URI, "float"));
        JAXB_MAPPING.put(Double.TYPE, new QName(W3C_XML_SCHEMA_NS_URI, "double"));
        JAXB_MAPPING.put(Boolean.class, new QName(W3C_XML_SCHEMA_NS_URI, "boolean"));
        JAXB_MAPPING.put(Byte.class, new QName(W3C_XML_SCHEMA_NS_URI, "byte"));
        JAXB_MAPPING.put(Short.class, new QName(W3C_XML_SCHEMA_NS_URI, "short"));
        JAXB_MAPPING.put(Integer.class, new QName(W3C_XML_SCHEMA_NS_URI, "int"));
        JAXB_MAPPING.put(Long.class, new QName(W3C_XML_SCHEMA_NS_URI, "long"));
        JAXB_MAPPING.put(Float.class, new QName(W3C_XML_SCHEMA_NS_URI, "float"));
        JAXB_MAPPING.put(Double.class, new QName(W3C_XML_SCHEMA_NS_URI, "double"));
        JAXB_MAPPING.put(String.class, new QName(W3C_XML_SCHEMA_NS_URI, "string"));
        JAXB_MAPPING.put(BigInteger.class, new QName(W3C_XML_SCHEMA_NS_URI, "integer"));
        JAXB_MAPPING.put(BigDecimal.class, new QName(W3C_XML_SCHEMA_NS_URI, "decimal"));
        JAXB_MAPPING.put(Calendar.class, new QName(W3C_XML_SCHEMA_NS_URI, "dateTime"));
        JAXB_MAPPING.put(Date.class, new QName(W3C_XML_SCHEMA_NS_URI, "dateTime"));
        JAXB_MAPPING.put(QName.class, new QName(W3C_XML_SCHEMA_NS_URI, "QName"));
        JAXB_MAPPING.put(URI.class, new QName(W3C_XML_SCHEMA_NS_URI, "string"));
        JAXB_MAPPING.put(XMLGregorianCalendar.class, new QName(W3C_XML_SCHEMA_NS_URI, "anySimpleType"));
        JAXB_MAPPING.put(Duration.class, new QName(W3C_XML_SCHEMA_NS_URI, "duration"));
        JAXB_MAPPING.put(Object.class, new QName(W3C_XML_SCHEMA_NS_URI, "anyType"));
        JAXB_MAPPING.put(Image.class, new QName(W3C_XML_SCHEMA_NS_URI, "base64Binary"));
        JAXB_MAPPING.put(javax.activation.DataHandler.class, new QName(W3C_XML_SCHEMA_NS_URI, "base64Binary"));
        JAXB_MAPPING.put(Source.class, new QName(W3C_XML_SCHEMA_NS_URI, "base64Binary"));
        JAXB_MAPPING.put(UUID.class, new QName(W3C_XML_SCHEMA_NS_URI, "string"));
        JAXB_MAPPING.put(byte[].class, new QName(W3C_XML_SCHEMA_NS_URI, "base64Binary"));
    }

    private JAXBQNameMapper mapper;

    public JAXBTypeIntrospector(@Reference JAXBQNameMapper mapper) {
        this.mapper = mapper;
    }

    public void introspect(Operation operation, Method method, IntrospectionContext context) {
        // TODO perform error checking, e.g. mixing of databindings
        List<DataType> inputTypes = operation.getInputTypes();
        for (DataType type : inputTypes) {
            if (!(type instanceof JavaType)) {
                // programming error
                throw new AssertionError("Java contracts must use " + JavaType.class);
            }
            introspect(type);
        }
        for (DataType type : operation.getFaultTypes()) {
            // FIXME need to process fault beans
            if (!(type instanceof JavaType)) {
                // programming error
                throw new AssertionError("Java contracts must use " + JavaType.class);
            }
            introspect(type);
        }
        DataType outputType = operation.getOutputType();
        if (!(outputType instanceof JavaType)) {
            // programming error
            throw new AssertionError("Java contracts must use " + JavaType.class);
        }
        introspect(outputType);

    }

    public void introspect(DataType dataType) {
        Class<?> type = dataType.getType();
        XmlRootElement annotation = type.getAnnotation(XmlRootElement.class);
        if (annotation != null) {
            String namespace = annotation.namespace();
            if (DEFAULT.equals(namespace)) {
                namespace = getDefaultNamespace(type);
            }
            dataType.setDatabinding(JAXB);
            return;
        }
        XmlType typeAnnotation = type.getAnnotation(XmlType.class);
        if (typeAnnotation != null) {
            String namespace = typeAnnotation.namespace();
            if (DEFAULT.equals(namespace)) {
                namespace = getDefaultNamespace(type);
            }
            String name = typeAnnotation.name();
            if (DEFAULT.equals(namespace)) {
                // as per the JAXB specification
                name = Introspector.decapitalize(type.getSimpleName());
            }
            dataType.setDatabinding(JAXB);
        }
    }

    private String getDefaultNamespace(Class clazz) {
        Package pkg = clazz.getPackage();
        // as per the JAXB specification
        if (pkg != null) {
            XmlSchema schemaAnnotation = pkg.getAnnotation(XmlSchema.class);
            if (schemaAnnotation != null) {
                return schemaAnnotation.namespace();
            }
            return pkg.getName();
        }
        return "";
    }
}
