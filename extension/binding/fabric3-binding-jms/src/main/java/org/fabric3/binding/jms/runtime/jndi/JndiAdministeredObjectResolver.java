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
package org.fabric3.binding.jms.runtime.jndi;

import javax.jms.ConnectionFactory;
import javax.naming.NamingException;
import java.util.Optional;

import org.fabric3.api.binding.jms.model.ConnectionFactoryDefinition;
import org.fabric3.api.binding.jms.model.Destination;
import org.fabric3.api.host.Fabric3Exception;
import org.fabric3.binding.jms.spi.runtime.manager.ConnectionFactoryManager;
import org.fabric3.binding.jms.spi.runtime.provider.ConnectionFactoryResolver;
import org.fabric3.binding.jms.spi.runtime.provider.DestinationResolver;
import org.fabric3.jndi.spi.JndiContextManager;
import org.oasisopen.sca.annotation.Reference;

/**
 * Resolves administered objects against JNDI contexts managed by the runtime {@link JndiContextManager}.
 */
public class JndiAdministeredObjectResolver implements ConnectionFactoryResolver, DestinationResolver {
    private JndiContextManager contextManager;
    private ConnectionFactoryManager factoryManager;

    public JndiAdministeredObjectResolver(@Reference JndiContextManager contextManager, @Reference ConnectionFactoryManager factoryManager) {
        this.contextManager = contextManager;
        this.factoryManager = factoryManager;
    }

    public Optional<ConnectionFactory> resolve(ConnectionFactoryDefinition definition) throws Fabric3Exception {
        try {
            String name = definition.getName();
            ConnectionFactory factory = contextManager.lookup(ConnectionFactory.class, name);
            if (factory == null) {
                return Optional.empty();
            }
            return Optional.ofNullable(factoryManager.register(name, factory, definition.getProperties()));
        } catch (NamingException e) {
            throw new Fabric3Exception(e);
        }
    }

    public Optional<javax.jms.Destination> resolve(Destination definition) throws Fabric3Exception {
        try {
            return Optional.ofNullable(contextManager.lookup(javax.jms.Destination.class, definition.getName()));
        } catch (NamingException e) {
            throw new Fabric3Exception(e);
        }
    }

}
