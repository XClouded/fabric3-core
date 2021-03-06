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
package org.fabric3.implementation.reflection.jdk;

import java.lang.reflect.Field;
import java.util.function.Supplier;

import org.fabric3.api.host.Fabric3Exception;
import org.fabric3.implementation.pojo.supplier.MultiplicitySupplier;
import org.fabric3.spi.container.injection.InjectionAttributes;
import org.fabric3.spi.container.injection.Injector;

/**
 * Injects a value created by a Supplier on a given field.
 */
public class FieldInjector implements Injector<Object> {
    private final Field field;

    private Supplier<?> supplier;

    /**
     * Create an injector and have it use the given Supplier to inject a value on the instance using the reflected
     * <code>Field</code>
     *
     * @param field         target field to inject on
     * @param supplier the object factor that creates the value to inject
     */
    public FieldInjector(Field field, Supplier<?> supplier) {
        this.field = field;
        this.field.setAccessible(true);
        this.supplier = supplier;
    }

    /**
     * Inject a new value on the given instance
     */
    public void inject(Object instance) throws Fabric3Exception {
        try {
            Object target;
            if (supplier == null) {
                // this can happen if a value is removed such as a reference being un-wired
                target = null;
            } else {
                target = supplier.get();
                if (target == null) {
                    // The Supplier is "empty", e.g. a reference has not been wired yet. Avoid injecting onto the instance.
                    // Note this is a correct assumption as there is no mechanism for configuring null values in SCA
                    return;
                }
            }            
            field.set(instance, target);
        } catch (IllegalAccessException e) {
            String id = field.getName();
            throw new AssertionError("Field is not accessible:" + id);
        }
    }

    public void setSupplier(Supplier<?> newSupplier, InjectionAttributes attributes) {

        if (this.supplier instanceof MultiplicitySupplier<?>) {
            ((MultiplicitySupplier<?>) this.supplier).addSupplier(newSupplier, attributes);
        } else {
            this.supplier = newSupplier;
        }

    }

    public void clearSupplier() {
        if (this.supplier instanceof MultiplicitySupplier<?>) {
            ((MultiplicitySupplier<?>) this.supplier).clear();
        } else {
            supplier = null;
        }
    }
}
