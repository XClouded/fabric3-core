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
package org.fabric3.monitor.impl.proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.Map;

import org.fabric3.api.annotation.monitor.MonitorLevel;
import org.fabric3.host.monitor.Monitorable;
import org.fabric3.monitor.impl.router.MonitorEventEntry;
import org.fabric3.monitor.impl.router.RingBufferDestinationRouter;
import org.fabric3.monitor.impl.writer.MonitorEntryWriter;
import org.fabric3.monitor.impl.writer.TimestampWriter;
import org.fabric3.spi.monitor.DispatchInfo;

/**
 *
 */
public class AbstractMonitorHandler implements InvocationHandler {
    private RingBufferDestinationRouter router;
    private TimestampWriter timestampWriter;
    private boolean asyncEnabled;
    private int destinationIndex;
    private String runtimeName;
    private Monitorable monitorable;
    private String source;
    private Map<String, DispatchInfo> infos;

    private MonitorLevel level;
    private String template;

    public AbstractMonitorHandler(int destinationIndex,
                                  String runtimeName,
                                  Monitorable monitorable,
                                  RingBufferDestinationRouter router,
                                  Map<String, DispatchInfo> infos,
                                  TimestampWriter timestampWriter,
                                  boolean asyncEnabled) {
        this.destinationIndex = destinationIndex;
        this.runtimeName = runtimeName;
        this.monitorable = monitorable;
        this.router = router;
        this.timestampWriter = timestampWriter;
        this.asyncEnabled = asyncEnabled;
        this.source = monitorable.getName();
        this.infos = infos;
        if (infos.size() == 1) {
            // optimization if the monitor interface has only one method: avoid a map lookup on dispatch
            DispatchInfo info = infos.values().iterator().next();
            this.level = info.getLevel();
            this.template = info.getMessage();
        }
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (args != null && args.length > 10) {
            throw new UnsupportedOperationException("Unsupported number of monitor arguments: " + args.length);
        }
        MonitorLevel currentLevel;
        String currentMessage;
        if (level != null) {
            currentLevel = level;
            currentMessage = template;
        } else {
            // FIXME convert to index-based dispatch
            DispatchInfo info = infos.get(method.getName());
            currentLevel = info.getLevel();
            currentMessage = info.getMessage();
        }
        if (currentLevel == null || currentLevel.intValue() < monitorable.getLevel().intValue()) {
            // monitoring is off
            return null;
        }
        long timestamp = System.currentTimeMillis();
        if (asyncEnabled) {
            send(currentLevel, timestamp, currentMessage, args);
        } else {
            router.send(level, destinationIndex, runtimeName, timestamp, source, template, args);
        }
        return null;

    }

    private void send(MonitorLevel level, long timestamp, String template, Object[] args) {
        MonitorEventEntry entry = null;
        try {
            entry = router.get();
            entry.setDestinationIndex(destinationIndex);

            ByteBuffer buffer = entry.getBuffer();

            MonitorEntryWriter.write(level, timestamp, template, buffer, timestampWriter, args);
        } finally {
            if (entry != null) {
                router.publish(entry);
            }
        }
    }


}