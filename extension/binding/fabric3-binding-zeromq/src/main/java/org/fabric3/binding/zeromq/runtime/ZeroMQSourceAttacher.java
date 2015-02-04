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
package org.fabric3.binding.zeromq.runtime;

import java.net.URI;
import java.util.List;

import org.fabric3.api.binding.zeromq.model.ZeroMQMetadata;
import org.fabric3.binding.zeromq.provision.ZeroMQWireSourceDefinition;
import org.fabric3.spi.container.builder.component.SourceWireAttacher;
import org.fabric3.spi.container.wire.InvocationChain;
import org.fabric3.spi.container.wire.Wire;
import org.fabric3.spi.model.physical.PhysicalWireTargetDefinition;
import org.oasisopen.sca.annotation.Reference;

/**
 *
 */
public class ZeroMQSourceAttacher implements SourceWireAttacher<ZeroMQWireSourceDefinition> {
    private ZeroMQWireBroker broker;

    public ZeroMQSourceAttacher(@Reference ZeroMQWireBroker broker) {
        this.broker = broker;
    }

    public void attach(ZeroMQWireSourceDefinition source, PhysicalWireTargetDefinition target, Wire wire) {
        URI uri = source.getCallbackUri() != null ? source.getCallbackUri() : target.getUri();
        ClassLoader loader = target.getClassLoader();
        List<InvocationChain> chains = ZeroMQAttacherHelper.sortChains(wire);
        ZeroMQMetadata metadata = source.getMetadata();
        broker.connectToReceiver(uri, chains, metadata, loader);
    }

    public void detach(ZeroMQWireSourceDefinition source, PhysicalWireTargetDefinition target) {
        URI uri = source.getCallbackUri() != null ? source.getCallbackUri() : target.getUri();
        broker.releaseReceiver(uri);
    }

}
