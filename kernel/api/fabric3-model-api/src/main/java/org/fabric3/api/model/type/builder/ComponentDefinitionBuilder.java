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
package org.fabric3.api.model.type.builder;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import java.util.Collections;
import java.util.Iterator;

import org.fabric3.api.Namespaces;
import org.fabric3.api.model.type.component.BindingDefinition;
import org.fabric3.api.model.type.component.ComponentDefinition;
import org.fabric3.api.model.type.component.ComponentService;
import org.fabric3.api.model.type.component.PropertyValue;

/**
 * Base builder for {@link ComponentDefinition}s.
 */
public abstract class ComponentDefinitionBuilder<T extends ComponentDefinitionBuilder> extends AbstractBuilder {

    /**
     * Adds a binding configuration to a service provided by the component.
     *
     * @param serviceName       the service name
     * @param bindingDefinition the binding definition
     * @return the builder
     */
    public T binding(String serviceName, BindingDefinition bindingDefinition) {
        checkState();
        ComponentDefinition<?> definition = getDefinition();
        ComponentService service = definition.getServices().get(serviceName);
        if (service == null) {
            service = new ComponentService(serviceName);
            definition.add(service);
        }
        service.addBinding(bindingDefinition);
        return builder();
    }

    /**
     * Adds a property value.
     *
     * @param name  the property name
     * @param value the value
     * @return the builder
     */
    public T property(String name, Object value) {
        checkState();
        PropertyValue propertyValue = new PropertyValue(name, value);
        getDefinition().add(propertyValue);
        return builder();
    }

    /**
     * Adds a property value sourced from the XPath expression.
     *
     * @param name  the property name
     * @param xpath the XPath expression
     * @return the builder
     */
    public T propertyExpression(String name, String xpath) {
        checkState();
        PropertyValue propertyValue = new PropertyValue(name, xpath);
        propertyValue.setNamespaceContext(new NamespaceContextImpl());
        getDefinition().add(propertyValue);
        return builder();
    }

    /**
     * Sets the wire key for a component for use with Map-based reference.
     *
     * @param key the key
     * @return the builder
     */
    public T key(Object key) {
        checkState();
        getDefinition().setKey(key.toString());
        return builder();
    }

    /**
     * Sets the wire order for a component for use with multiplicity reference.
     *
     * @param order the order
     * @return the builder
     */
    public T order(int order) {
        checkState();
        getDefinition().setOrder(order);
        return builder();
    }

    /**
     * Adds an intent to the implementation.
     *
     * @param intent the intent
     * @return the builder
     */
    public T implementationIntent(QName intent) {
        checkState();
        getDefinition().getImplementation().addIntent(intent);
        return builder();
    }

    /**
     * Adds an intent to the component.
     *
     * @param intent the intent
     * @return the builder
     */
    public T componentIntent(QName intent) {
        checkState();
        getDefinition().addIntent(intent);
        return builder();
    }

    protected abstract ComponentDefinition<?> getDefinition();

    @SuppressWarnings("unchecked")
    private T builder() {
        return (T) this;
    }

    private class NamespaceContextImpl implements NamespaceContext {

        public String getNamespaceURI(String prefix) {
            return prefix.equals("f3") ? Namespaces.F3 : null;
        }

        public String getPrefix(String namespaceURI) {
            return namespaceURI.equals(Namespaces.F3) ? "f3" : null;
        }

        public Iterator getPrefixes(String namespaceURI) {
            if (namespaceURI.equals(Namespaces.F3)) {
                return Collections.singletonList("f3").iterator();
            }
            return Collections.emptyList().iterator();
        }
    }

}