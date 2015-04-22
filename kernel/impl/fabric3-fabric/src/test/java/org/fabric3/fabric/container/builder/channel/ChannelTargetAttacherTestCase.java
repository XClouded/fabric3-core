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
package org.fabric3.fabric.container.builder.channel;

import java.net.URI;

import junit.framework.TestCase;
import org.easymock.EasyMock;
import org.fabric3.fabric.model.physical.ChannelTarget;
import org.fabric3.spi.container.channel.Channel;
import org.fabric3.spi.container.channel.ChannelConnection;
import org.fabric3.fabric.container.channel.ChannelManager;
import org.fabric3.spi.model.physical.ChannelSide;
import org.fabric3.spi.model.physical.PhysicalConnectionSource;
import org.fabric3.spi.util.Closeable;
import org.oasisopen.sca.annotation.EagerInit;

/**
 *
 */
@EagerInit
public class ChannelTargetAttacherTestCase extends TestCase {

    public void testAttachDetach() throws Exception {
        URI channelUri = URI.create("channel");
        URI targetUri = URI.create("target");

        ChannelManager channelManager = EasyMock.createMock(ChannelManager.class);
        ChannelConnection connection = EasyMock.createMock(ChannelConnection.class);
        connection.setCloseable(EasyMock.isA(Closeable.class));

        Channel channel = EasyMock.createMock((Channel.class));
        channel.attach(connection);
        EasyMock.expectLastCall();
        EasyMock.expect(channelManager.getChannel(channelUri, ChannelSide.CONSUMER)).andReturn(channel);

        EasyMock.replay(channelManager, connection, channel);

        ChannelTargetAttacher attacher = new ChannelTargetAttacher(channelManager);
        MockPhysical source = new MockPhysical(targetUri);
        ChannelTarget target = new ChannelTarget(channelUri, ChannelSide.CONSUMER);

        attacher.attach(source, target, connection);
        attacher.detach(source, target);

        EasyMock.verify(channelManager, connection, channel);
    }

    private class MockPhysical extends PhysicalConnectionSource {
        public MockPhysical(URI uri) {
            setUri(uri);
        }
    }
}