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
 *
 * ----------------------------------------------------
 *
 * Portions originally based on Apache Tuscany 2007
 * licensed under the Apache 2.0 license.
 *
 */
package org.fabric3.introspection.java.annotation;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import org.fabric3.api.annotation.runtime.DataDirectory;
import org.fabric3.api.model.type.component.Implementation;
import org.fabric3.api.model.type.java.Injectable;
import org.fabric3.api.model.type.java.InjectingComponentType;
import org.fabric3.api.model.type.java.InjectionSite;
import org.fabric3.spi.introspection.IntrospectionContext;
import org.fabric3.spi.introspection.java.IntrospectionHelper;
import org.fabric3.spi.introspection.java.annotation.AbstractAnnotationProcessor;
import org.fabric3.spi.model.type.java.FieldInjectionSite;
import org.fabric3.spi.model.type.java.MethodInjectionSite;
import org.oasisopen.sca.annotation.Reference;

/**
 * Processes {@link DataDirectory} annotations.
 */
public class DataDirectoryProcessor<I extends Implementation<? extends InjectingComponentType>> extends AbstractAnnotationProcessor<DataDirectory> {
    private final IntrospectionHelper helper;

    public DataDirectoryProcessor(@Reference IntrospectionHelper helper) {
        super(DataDirectory.class);
        this.helper = helper;
    }

    public void visitField(DataDirectory annotation, Field field, Class<?> implClass, InjectingComponentType componentType, IntrospectionContext context) {
        Type type = field.getGenericType();
        FieldInjectionSite site = new FieldInjectionSite(field);
        visit(type, componentType, site, field.getDeclaringClass(), field, context);
    }

    public void visitMethod(DataDirectory annotation, Method method, Class<?> implClass, InjectingComponentType componentType, IntrospectionContext context) {
        Type type = helper.getGenericType(method);
        MethodInjectionSite site = new MethodInjectionSite(method, 0);
        visit(type, componentType, site, method.getDeclaringClass(), method, context);
    }

    private void visit(Type type, InjectingComponentType componentType, InjectionSite site, Class<?> clazz, Member member, IntrospectionContext context) {
        if (!(type instanceof Class)) {
            context.addError(new InvalidContextType("Data directory must by type File: " + type + " in " + clazz.getName(), member, componentType));
        } else if (File.class.isAssignableFrom((Class<?>) type)) {
            componentType.addInjectionSite(site, Injectable.DATA_DIRECTORY_CONTEXT);
        } else {
            context.addError(new InvalidContextType("Data directory must by type File: " + type + " in " + clazz.getName(), member, componentType));
        }
    }
}