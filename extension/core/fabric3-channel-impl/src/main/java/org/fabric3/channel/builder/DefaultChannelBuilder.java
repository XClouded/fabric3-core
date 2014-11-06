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
package org.fabric3.channel.builder;

import javax.xml.namespace.QName;
import java.net.URI;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import org.fabric3.channel.impl.AsyncFanOutHandler;
import org.fabric3.channel.impl.DefaultChannelImpl;
import org.fabric3.channel.impl.FanOutHandler;
import org.fabric3.channel.impl.SyncFanOutHandler;
import org.fabric3.spi.container.ContainerException;
import org.fabric3.spi.container.builder.channel.ChannelBuilder;
import org.fabric3.spi.container.builder.component.ChannelBindingBuilder;
import org.fabric3.spi.container.channel.Channel;
import org.fabric3.spi.model.physical.PhysicalChannelBindingDefinition;
import org.fabric3.spi.model.physical.PhysicalChannelDefinition;
import org.oasisopen.sca.annotation.Reference;

/**
 * Creates and disposes default channel implementations.
 */
public class DefaultChannelBuilder implements ChannelBuilder {

    private ExecutorService executorService;

    private Map<Class<? extends PhysicalChannelBindingDefinition>, ChannelBindingBuilder> bindingBuilders = Collections.emptyMap();

    public DefaultChannelBuilder(@Reference(name = "executorService") ExecutorService executorService) {
        this.executorService = executorService;
    }

    @Reference(required = false)
    public void setBindingBuilders(Map<Class<? extends PhysicalChannelBindingDefinition>, ChannelBindingBuilder> builders) {
        this.bindingBuilders = builders;
    }

    public Channel build(PhysicalChannelDefinition definition) throws ContainerException {
        URI uri = definition.getUri();
        QName deployable = definition.getDeployable();

        FanOutHandler fanOutHandler;
        if (definition.getBindingDefinition() != null) {
            // if a binding is set on the channel, make the channel is synchronous since async behavior will be provided by the binding
            fanOutHandler = new SyncFanOutHandler();
        } else {
            // the channel is local, have it implement asynchrony
            fanOutHandler = new AsyncFanOutHandler(executorService);
        }

        Channel channel = new DefaultChannelImpl(uri, deployable, fanOutHandler, definition.getChannelSide());
        PhysicalChannelBindingDefinition bindingDefinition = definition.getBindingDefinition();
        buildBinding(channel, bindingDefinition);

        return channel;
    }

    public void dispose(PhysicalChannelDefinition definition, Channel channel) throws ContainerException {
        disposeBinding(channel, definition.getBindingDefinition());
    }

    @SuppressWarnings({"unchecked"})
    private void buildBinding(Channel channel, PhysicalChannelBindingDefinition bindingDefinition) throws ContainerException {
        if (bindingDefinition != null) {
            ChannelBindingBuilder builder = getBuilder(bindingDefinition);
            builder.build(bindingDefinition, channel);
        }
    }

    @SuppressWarnings({"unchecked"})
    private void disposeBinding(Channel channel, PhysicalChannelBindingDefinition bindingDefinition) throws ContainerException {
        if (bindingDefinition != null) {
            ChannelBindingBuilder builder = getBuilder(bindingDefinition);
            builder.dispose(bindingDefinition, channel);
        }
    }

    private ChannelBindingBuilder getBuilder(PhysicalChannelBindingDefinition definition) throws ContainerException {
        ChannelBindingBuilder<?> builder = bindingBuilders.get(definition.getClass());
        if (builder == null) {
            throw new ContainerException("Channel binding builder not found for type " + definition.getClass());
        }
        return builder;
    }

}
