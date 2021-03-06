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
package org.fabric3.fabric.domain.generator.channel;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;
import org.easymock.EasyMock;
import org.fabric3.api.host.Fabric3Exception;
import org.fabric3.api.model.type.component.Component;
import org.fabric3.api.model.type.component.Consumer;
import org.fabric3.fabric.container.command.BuildChannelCommand;
import org.fabric3.fabric.container.command.ChannelConnectionCommand;
import org.fabric3.fabric.container.command.DisposeChannelCommand;
import org.fabric3.spi.model.instance.LogicalChannel;
import org.fabric3.spi.model.instance.LogicalComponent;
import org.fabric3.spi.model.instance.LogicalCompositeComponent;
import org.fabric3.spi.model.instance.LogicalConsumer;
import org.fabric3.spi.model.instance.LogicalState;
import org.fabric3.spi.model.physical.PhysicalChannel;
import org.fabric3.spi.model.physical.PhysicalChannelConnection;

/**
 *
 */
@SuppressWarnings("unchecked")
public class ConsumerCommandGeneratorTestCase extends TestCase {
    private static final URI CONTRIBUTION = URI.create("test");

    private URI uri = URI.create("testChannel");

    private BuildChannelCommand buildChannelCommand;
    private DisposeChannelCommand disposeChannelCommand;

    public void testGenerateChannelNotFound() throws Exception {
        ConnectionGenerator connectionGenerator = EasyMock.createMock(ConnectionGenerator.class);

        ChannelCommandGenerator channelGenerator = EasyMock.createMock(ChannelCommandGenerator.class);
        EasyMock.replay(connectionGenerator, channelGenerator);

        ConsumerCommandGenerator generator = new ConsumerCommandGenerator(connectionGenerator, channelGenerator);

        LogicalCompositeComponent parent = new LogicalCompositeComponent(URI.create("domain"), null, null);
        URI channelUri = URI.create("NotFoundChannel");
        Component definition = new Component("component");
        LogicalComponent<?> component = new LogicalComponent(URI.create("component"), definition, parent);
        definition.setContributionUri(CONTRIBUTION);
        LogicalConsumer consumer = new LogicalConsumer(URI.create("component#consumer"), new Consumer("consumer"), component);
        component.addConsumer(consumer);

        consumer.addSource(channelUri);

        try {
            generator.generate(component);
            fail();
        } catch (Fabric3Exception e) {
            // expected;
        }
        EasyMock.verify(connectionGenerator, channelGenerator);
    }

    public void testGenerateBuildChannel() throws Exception {
        ConnectionGenerator connectionGenerator = EasyMock.createMock(ConnectionGenerator.class);
        List<PhysicalChannelConnection> list = Collections.singletonList(new PhysicalChannelConnection(uri, URI.create("test"), null, null, null, false));
        EasyMock.expect(connectionGenerator.generateConsumer(EasyMock.isA(LogicalConsumer.class), EasyMock.isA(Map.class))).andReturn(list);

        ChannelCommandGenerator channelGenerator = EasyMock.createMock(ChannelCommandGenerator.class);
        EasyMock.expect(channelGenerator.generateBuild(EasyMock.isA(LogicalChannel.class),
                                                       EasyMock.isA(URI.class),
                                                       EasyMock.isA(ChannelDirection.class))).andReturn(buildChannelCommand);
        EasyMock.replay(connectionGenerator, channelGenerator);

        ConsumerCommandGenerator generator = new ConsumerCommandGenerator(connectionGenerator, channelGenerator);
        LogicalComponent<?> component = createComponent();

        ChannelConnectionCommand command = generator.generate(component).get();

        assertNotNull(command);
        List<BuildChannelCommand> buildChannelCommands = command.getBuildChannelCommands();
        assertEquals(1, buildChannelCommands.size());
        assertFalse(command.getAttachCommands().isEmpty());
        assertTrue(command.getDetachCommands().isEmpty());
        EasyMock.verify(connectionGenerator, channelGenerator);
    }

    public void testGenerateAttach() throws Exception {
        ConnectionGenerator connectionGenerator = EasyMock.createMock(ConnectionGenerator.class);
        PhysicalChannelConnection connection = new PhysicalChannelConnection(uri, URI.create("test"), null, null, null, false);
        List<PhysicalChannelConnection> list = Collections.singletonList(connection);
        EasyMock.expect(connectionGenerator.generateConsumer(EasyMock.isA(LogicalConsumer.class), EasyMock.isA(Map.class))).andReturn(list);

        ChannelCommandGenerator channelGenerator = EasyMock.createMock(ChannelCommandGenerator.class);
        EasyMock.expect(channelGenerator.generateBuild(EasyMock.isA(LogicalChannel.class),
                                                       EasyMock.isA(URI.class),
                                                       EasyMock.isA(ChannelDirection.class))).andReturn(buildChannelCommand);
        EasyMock.replay(connectionGenerator, channelGenerator);

        ConsumerCommandGenerator generator = new ConsumerCommandGenerator(connectionGenerator, channelGenerator);
        LogicalComponent<?> component = createComponent();
        ChannelConnectionCommand command = generator.generate(component).get();

        assertNotNull(command);
        assertFalse(command.getAttachCommands().isEmpty());
        assertTrue(command.getDetachCommands().isEmpty());
        EasyMock.verify(connectionGenerator, channelGenerator);
    }

    public void testGenerateDetach() throws Exception {
        ConnectionGenerator connectionGenerator = EasyMock.createMock(ConnectionGenerator.class);
        List<PhysicalChannelConnection> list = Collections.singletonList(new PhysicalChannelConnection(uri, URI.create("test"), null, null, null, false));
        EasyMock.expect(connectionGenerator.generateConsumer(EasyMock.isA(LogicalConsumer.class), EasyMock.isA(Map.class))).andReturn(list);

        ChannelCommandGenerator channelGenerator = EasyMock.createMock(ChannelCommandGenerator.class);
        EasyMock.expect(channelGenerator.generateDispose(EasyMock.isA(LogicalChannel.class),
                                                         EasyMock.isA(URI.class),
                                                         EasyMock.isA(ChannelDirection.class))).andReturn(disposeChannelCommand);

        EasyMock.replay(connectionGenerator, channelGenerator);

        ConsumerCommandGenerator generator = new ConsumerCommandGenerator(connectionGenerator, channelGenerator);
        LogicalComponent<?> component = createComponent();
        component.setState(LogicalState.MARKED);
        ChannelConnectionCommand command = generator.generate(component).get();

        List<DisposeChannelCommand> disposeChannelCommands = command.getDisposeChannelCommands();
        assertEquals(1, disposeChannelCommands.size());

        assertNotNull(command);
        assertFalse(command.getDetachCommands().isEmpty());
        assertTrue(command.getAttachCommands().isEmpty());
        EasyMock.verify(connectionGenerator, channelGenerator);
    }

    public void testGenerateFullDetach() throws Exception {
        ConnectionGenerator connectionGenerator = EasyMock.createMock(ConnectionGenerator.class);
        List<PhysicalChannelConnection> list = Collections.singletonList(new PhysicalChannelConnection(uri, URI.create("test"), null, null, null, false));
        EasyMock.expect(connectionGenerator.generateConsumer(EasyMock.isA(LogicalConsumer.class), EasyMock.isA(Map.class))).andReturn(list);

        ChannelCommandGenerator channelGenerator = EasyMock.createMock(ChannelCommandGenerator.class);
        EasyMock.expect(channelGenerator.generateDispose(EasyMock.isA(LogicalChannel.class),
                                                         EasyMock.isA(URI.class),
                                                         EasyMock.isA(ChannelDirection.class))).andReturn(disposeChannelCommand);
        EasyMock.replay(connectionGenerator, channelGenerator);

        ConsumerCommandGenerator generator = new ConsumerCommandGenerator(connectionGenerator, channelGenerator);
        LogicalComponent<?> component = createComponent();
        component.setState(LogicalState.MARKED);
        ChannelConnectionCommand command = generator.generate(component).get();

        assertNotNull(command);
        assertTrue(command.getAttachCommands().isEmpty());
        assertFalse(command.getDetachCommands().isEmpty());
        EasyMock.verify(connectionGenerator, channelGenerator);
    }

    public void testGenerateNothing() throws Exception {
        ConnectionGenerator connectionGenerator = EasyMock.createMock(ConnectionGenerator.class);
        ChannelCommandGenerator channelGenerator = EasyMock.createMock(ChannelCommandGenerator.class);
        EasyMock.replay(connectionGenerator, channelGenerator);

        ConsumerCommandGenerator generator = new ConsumerCommandGenerator(connectionGenerator, channelGenerator);
        LogicalComponent<?> component = createComponent();
        component.setState(LogicalState.PROVISIONED);
        assertFalse(generator.generate(component).isPresent());
        EasyMock.verify(connectionGenerator, channelGenerator);
    }

    @SuppressWarnings({"unchecked"})
    private LogicalComponent<?> createComponent() {
        LogicalCompositeComponent parent = new LogicalCompositeComponent(URI.create("domain"), null, null);
        URI channelUri = URI.create("channel");
        LogicalChannel channel = new LogicalChannel(channelUri, null, parent);
        parent.addChannel(channel);
        Component definition = new Component("component");
        definition.setContributionUri(CONTRIBUTION);
        LogicalComponent<?> component = new LogicalComponent(URI.create("component"), definition, parent);
        LogicalConsumer consumer = new LogicalConsumer(URI.create("component#consumer"), new Consumer("consumer"), component);
        component.addConsumer(consumer);

        consumer.addSource(channelUri);

        return component;
    }

    protected void setUp() throws Exception {
        PhysicalChannel physicalChannel = new PhysicalChannel(URI.create("test"), URI.create("bar"));
        buildChannelCommand = new BuildChannelCommand(physicalChannel);
        disposeChannelCommand = new DisposeChannelCommand(physicalChannel);
    }
}