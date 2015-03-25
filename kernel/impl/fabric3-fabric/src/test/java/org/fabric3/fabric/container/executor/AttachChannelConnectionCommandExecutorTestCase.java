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
package org.fabric3.fabric.container.executor;

import java.net.URI;

import junit.framework.TestCase;
import org.easymock.EasyMock;
import org.fabric3.fabric.container.command.AttachChannelConnectionCommand;
import org.fabric3.spi.container.builder.ChannelConnector;
import org.fabric3.spi.container.executor.CommandExecutorRegistry;
import org.fabric3.spi.model.physical.PhysicalChannelConnectionDefinition;

/**
 *
 */
public class AttachChannelConnectionCommandExecutorTestCase extends TestCase {

    public void testExecute() throws Exception {
        CommandExecutorRegistry executorRegistry = EasyMock.createMock(CommandExecutorRegistry.class);
        ChannelConnector connector = EasyMock.createMock(ChannelConnector.class);
        executorRegistry.register(EasyMock.eq(AttachChannelConnectionCommand.class), EasyMock.isA(AttachChannelConnectionCommandExecutor.class));
        connector.connect(EasyMock.isA(PhysicalChannelConnectionDefinition.class));
        EasyMock.replay(executorRegistry, connector);

        AttachChannelConnectionCommandExecutor executor = new AttachChannelConnectionCommandExecutor(executorRegistry, connector);
        executor.init();
        URI uri = URI.create("testChannel");
        PhysicalChannelConnectionDefinition definition = new PhysicalChannelConnectionDefinition(uri, null, null, null, false);
        AttachChannelConnectionCommand command = new AttachChannelConnectionCommand(definition);
        executor.execute(command);
        EasyMock.verify(executorRegistry, connector);

    }

}
