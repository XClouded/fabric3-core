/*
 * Fabric3
 * Copyright (c) 2009-2011 Metaform Systems
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
package org.fabric3.cache.infinispan.runtime;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import javax.management.MBeanServer;
import javax.transaction.TransactionManager;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.infinispan.Cache;
import org.infinispan.config.Configuration;
import org.infinispan.config.FluentConfiguration;
import org.infinispan.config.FluentGlobalConfiguration;
import org.infinispan.config.GlobalConfiguration;
import org.infinispan.config.InfinispanConfiguration;
import org.infinispan.jmx.MBeanServerLookup;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.transaction.lookup.TransactionManagerLookup;
import org.oasisopen.sca.annotation.Destroy;
import org.oasisopen.sca.annotation.EagerInit;
import org.oasisopen.sca.annotation.Init;
import org.oasisopen.sca.annotation.Reference;

import org.fabric3.cache.infinispan.provision.InfinispanPhysicalResourceDefinition;
import org.fabric3.cache.spi.CacheBuildException;
import org.fabric3.cache.spi.CacheManager;
import org.fabric3.host.runtime.HostInfo;

/**
 * Manages Infinispan cache resources on a runtime.
 *
 * @version $Rev$ $Date$
 */
@EagerInit
public class InfinispanCacheManager implements CacheManager<InfinispanPhysicalResourceDefinition> {
    private TransactionManager tm;
    private MBeanServer mBeanServer;
    private HostInfo info;
    private Fabric3TransactionManagerLookup txLookup;

    private EmbeddedCacheManager cacheManager;
    private Map<String, Cache<?, ?>> caches = new ConcurrentHashMap<String, Cache<?, ?>>();

    public InfinispanCacheManager(@Reference TransactionManager tm, @Reference MBeanServer mBeanServer, @Reference HostInfo info) {
        this.tm = tm;
        this.mBeanServer = mBeanServer;
        this.info = info;
        txLookup = new Fabric3TransactionManagerLookup();
    }

    @Init
    public void init() {
        FluentGlobalConfiguration globalConfig = new GlobalConfiguration().fluent();
        globalConfig.transport().machineId(info.getRuntimeName());
        FluentGlobalConfiguration.GlobalJmxStatisticsConfig jmxStatistics = globalConfig.globalJmxStatistics();
        String authority = info.getDomain().getAuthority();
        Fabric3MBeanServerLookup serverLookup = new Fabric3MBeanServerLookup();
        jmxStatistics.jmxDomain(authority).mBeanServerLookup(serverLookup);
        this.cacheManager = new DefaultCacheManager(globalConfig.build());
    }

    @Destroy
    public void destroy() {
        cacheManager.stop();
    }

    @SuppressWarnings({"unchecked"})
    public <CACHE> CACHE getCache(String name) {
        return (CACHE) caches.get(name);
    }

    public void create(InfinispanPhysicalResourceDefinition resourceDefinition) throws CacheBuildException {
        // Set TCCL for JAXB
        ClassLoader old = Thread.currentThread().getContextClassLoader();
        try {
            Configuration cacheConfiguration = parseConfiguration(resourceDefinition);
            FluentConfiguration fluent = cacheConfiguration.fluent();
            fluent.transaction().transactionManagerLookup(txLookup);

            String cacheName = resourceDefinition.getCacheName();
            cacheManager.defineConfiguration(cacheName, cacheConfiguration);
            Cache<?, ?> cache = cacheManager.getCache(cacheName);
            caches.put(cacheName, cache);
        } finally {
            Thread.currentThread().setContextClassLoader(old);
        }
    }

    public void remove(InfinispanPhysicalResourceDefinition resourceDefinition) throws CacheBuildException {
        String cacheName = resourceDefinition.getCacheName();
        ClassLoader old = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
            Cache<?, ?> cache = caches.get(cacheName);
            if (cache == null) {
                throw new CacheBuildException("Cache not found: " + cacheName);
            }
            cache.stop();
        } finally {
            Thread.currentThread().setContextClassLoader(old);
        }
    }

    private Configuration parseConfiguration(InfinispanPhysicalResourceDefinition resourceDefinition) throws CacheBuildException {
        try {
            StringWriter writer = new StringWriter();
            StreamResult result = new StreamResult(writer);
            Source source = new DOMSource(resourceDefinition.getConfiguration());
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.transform(source, result);
            Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
            StringReader reader = new StringReader(writer.toString());
            return InfinispanConfiguration.newInfinispanConfiguration(reader).parseDefaultConfiguration();
        } catch (TransformerConfigurationException e) {
            throw new CacheBuildException(e);
        } catch (TransformerException e) {
            throw new CacheBuildException(e);
        } catch (TransformerFactoryConfigurationError e) {
            throw new CacheBuildException(e);
        } catch (IOException e) {
            throw new CacheBuildException(e);
        }
    }

    private class Fabric3MBeanServerLookup implements MBeanServerLookup {

        public MBeanServer getMBeanServer(Properties properties) {
            return mBeanServer;
        }
    }

    private class Fabric3TransactionManagerLookup implements TransactionManagerLookup {

        public TransactionManager getTransactionManager() throws Exception {
            return tm;
        }
    }

}



