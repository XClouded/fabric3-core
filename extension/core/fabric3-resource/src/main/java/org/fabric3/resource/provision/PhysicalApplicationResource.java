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
 */
package org.fabric3.resource.provision;

import java.util.function.Supplier;

import org.fabric3.spi.model.physical.PhysicalResource;

/**
 *
 */
public class PhysicalApplicationResource extends PhysicalResource {
    private String name;
    private Supplier<?> supplier;

    public PhysicalApplicationResource(String name, Supplier<?> supplier) {
        this.name = name;
        this.supplier = supplier;
    }

    public String getName() {
        return name;
    }

    public Supplier<?> getSupplier() {
        return supplier;
    }
}