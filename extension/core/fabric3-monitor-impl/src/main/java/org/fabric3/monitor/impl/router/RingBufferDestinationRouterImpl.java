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
package org.fabric3.monitor.impl.router;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;

import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import org.fabric3.api.annotation.monitor.MonitorLevel;
import org.fabric3.monitor.impl.destination.MonitorDestinationRegistry;
import org.fabric3.monitor.impl.writer.MonitorEntryWriter;
import org.fabric3.monitor.impl.writer.TimestampWriter;
import org.oasisopen.sca.ServiceRuntimeException;
import org.oasisopen.sca.annotation.Destroy;
import org.oasisopen.sca.annotation.Init;
import org.oasisopen.sca.annotation.Property;
import org.oasisopen.sca.annotation.Reference;

/**
 * Dispatches monitor events to destinations via a ring buffer.
 * <p/>
 * This implementation also supports synchronous dispatch. This mode should only be used in development to avoid startup time associated with pre-allocating
 * ring buffer entries.
 */
public class RingBufferDestinationRouterImpl implements RingBufferDestinationRouter {

    private ExecutorService executorService;
    private MonitorDestinationRegistry registry;
    private Disruptor<MonitorEventEntry> disruptor;

    private int capacity = 2000;
    private int ringSize = 65536;
    private boolean enabled = true;

    private String pattern = "%d:%m:%Y %H:%i:%s.%F";
    private TimeZone timeZone = TimeZone.getDefault();

    private TimestampWriter timestampWriter;

    public RingBufferDestinationRouterImpl(@Reference ExecutorService executorService, @Reference MonitorDestinationRegistry registry) {
        this.executorService = executorService;
        this.registry = registry;
    }

    @Property(required = false)
    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    @Property(required = false)
    public void setRingSize(int ringSize) {
        this.ringSize = ringSize;
    }

    @Property(required = false)
    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    @Property(required = false)
    public void setTimeZone(String id) {
        this.timeZone = TimeZone.getTimeZone(id);
    }

    @Property(required = false)
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Init
    public void init() throws FileNotFoundException {
        timestampWriter = new TimestampWriter(pattern, timeZone);

        if (enabled) {
            MonitorEventEntryFactory factory = new MonitorEventEntryFactory(capacity);
            disruptor = new Disruptor<MonitorEventEntry>(factory, ringSize, executorService);
            MonitorEventHandler handler = new MonitorEventHandler(registry);
            disruptor.handleEventsWith(handler);
            disruptor.start();
        }
    }

    @Destroy
    public void destroy() throws IOException {
        if (disruptor != null) {
            disruptor.shutdown();
        }
    }

    public int getDestinationIndex(String name) {
        return registry.getIndex(name);
    }

    public MonitorEventEntry get() {
        RingBuffer<MonitorEventEntry> ringBuffer = disruptor.getRingBuffer();
        long sequence = ringBuffer.next();
        MonitorEventEntry entry = ringBuffer.getPreallocated(sequence);
        entry.getBuffer().clear();
        entry.setSequence(sequence);
        return entry;
    }

    public void publish(MonitorEventEntry entry) {
        disruptor.getRingBuffer().publish(entry.getSequence());
    }

    public void send(MonitorLevel level, int destinationIndex, String runtimeName, long timestamp, String source, String template, Object... args) {
        // Note this method is not garbage-free as primitives will be auto-boxed. This method should only be called to flush bootstrap events or when the
        // ring buffer is disabled.
        if (enabled) {
            MonitorEventEntry entry = null;
            try {
                entry = get();
                entry.setDestinationIndex(destinationIndex);
                ByteBuffer buffer = entry.getBuffer();
                MonitorEntryWriter.write(level, timestamp, template, buffer, timestampWriter, args);
            } finally {
                if (entry != null) {
                    publish(entry);
                }
            }
        } else {
            // synchronize the write
            synchronized (this) {
                ByteBuffer buffer = ByteBuffer.allocate(capacity);
                MonitorEntryWriter.write(level, timestamp, template, buffer, timestampWriter, args);
                try {
                    registry.write(destinationIndex, buffer);
                } catch (IOException e) {
                    throw new ServiceRuntimeException(e);
                }
            }
        }
    }

}