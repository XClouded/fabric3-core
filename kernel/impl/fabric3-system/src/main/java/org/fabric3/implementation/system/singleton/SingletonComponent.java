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
package org.fabric3.implementation.system.singleton;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import org.fabric3.api.annotation.monitor.MonitorLevel;
import org.fabric3.api.host.Fabric3Exception;
import org.fabric3.api.model.type.java.Injectable;
import org.fabric3.api.model.type.java.InjectableType;
import org.fabric3.api.model.type.java.InjectionSite;
import org.fabric3.implementation.pojo.supplier.ArrayMultiplicitySupplier;
import org.fabric3.implementation.pojo.supplier.ListMultiplicitySupplier;
import org.fabric3.implementation.pojo.supplier.MapMultiplicitySupplier;
import org.fabric3.implementation.pojo.supplier.MultiplicitySupplier;
import org.fabric3.implementation.pojo.supplier.SetMultiplicitySupplier;
import org.fabric3.spi.container.component.ScopedComponent;
import org.fabric3.spi.container.injection.InjectionAttributes;
import org.fabric3.spi.model.type.java.FieldInjectionSite;
import org.fabric3.spi.model.type.java.MethodInjectionSite;

/**
 * Wraps an object intended to serve as a system component provided to the Fabric3 runtime by the host environment.
 */
public class SingletonComponent implements ScopedComponent {
    private final URI uri;
    private Object instance;
    private Map<Member, Injectable> sites;
    private Map<Supplier, Injectable> reinjectionMappings;
    private URI contributionUri;
    private MonitorLevel level = MonitorLevel.INFO;
    private AtomicBoolean started = new AtomicBoolean(false);

    public SingletonComponent(URI componentId, Object instance, Map<InjectionSite, Injectable> mappings, URI contributionUri) {
        this.uri = componentId;
        this.instance = instance;
        this.contributionUri = contributionUri;
        this.reinjectionMappings = new HashMap<>();
        initializeInjectionSites(mappings);
    }

    public URI getContributionUri() {
        return contributionUri;
    }

    public String getKey() {
        return null;
    }

    public URI getUri() {
        return uri;
    }

    public void start() {
        started.set(true);
    }

    public void stop() {
        started.set(false);
    }

    public void startUpdate() {
        reinjectionMappings.keySet().stream().filter(factory -> factory instanceof MultiplicitySupplier).forEach(factory -> ((MultiplicitySupplier) factory)
                .startUpdate());
    }

    public void endUpdate() {
        reinjectionMappings.keySet().stream().filter(factory -> factory instanceof MultiplicitySupplier).forEach(factory -> ((MultiplicitySupplier) factory)
                .endUpdate());
    }

    public boolean isEagerInit() {
        return true;
    }

    public Object createInstance() {
        return instance;
    }

    public void releaseInstance(Object instance) {
        // no-op
    }

    public Supplier<Object> createSupplier() {
        return () -> instance;
    }

    public Object getInstance() {
        return instance;
    }

    public String getName() {
        return uri.toString();
    }

    public MonitorLevel getLevel() {
        return level;
    }

    public void setLevel(MonitorLevel level) {
        this.level = level;
    }

    public void startInstance(Object instance) {
        // no-op
    }

    public void stopInstance(Object instance) throws Fabric3Exception {
        // no-op
    }

    public void reinject(Object instance) throws Fabric3Exception {
        for (Map.Entry<Supplier, Injectable> entry : reinjectionMappings.entrySet()) {
            inject(entry.getValue(), entry.getKey());
        }
        reinjectionMappings.clear();
    }

    /**
     * Adds a Supplier to be reinjected. Note only String keys are supported for singleton components to avoid a requirement on the transformer infrastructure.
     *
     * @param injectable the InjectableAttribute describing the site to reinject
     * @param supplier   the Supplier responsible for supplying a value to reinject
     * @param attributes the injection attributes
     */
    public void addSupplier(Injectable injectable, Supplier supplier, InjectionAttributes attributes) {
        if (InjectableType.REFERENCE == injectable.getType()) {
            setFactory(injectable, supplier, attributes);
        } else {
            // the factory corresponds to a property or context, which will override previous values if reinjected
            reinjectionMappings.put(supplier, injectable);
        }
    }

    public void removeSupplier(Injectable injectable) {
        for (Iterator<Map.Entry<Supplier, Injectable>> iterator = reinjectionMappings.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry<Supplier, Injectable> entry = iterator.next();
            if (injectable.equals(entry.getValue())) {
                iterator.remove();
                break;
            }
        }
    }

    public String toString() {
        return "[" + uri.toString() + "] in state [" + super.toString() + ']';
    }

    /**
     * Obtain the fields and methods for injection sites associated with the instance
     *
     * @param mappings the mappings of injection sites
     */
    private void initializeInjectionSites(Map<InjectionSite, Injectable> mappings) {
        this.sites = new HashMap<>();
        for (Map.Entry<InjectionSite, Injectable> entry : mappings.entrySet()) {
            InjectionSite site = entry.getKey();
            if (site instanceof FieldInjectionSite) {
                try {
                    Field field = getField(((FieldInjectionSite) site).getName());
                    sites.put(field, entry.getValue());
                } catch (NoSuchFieldException e) {
                    // programming error
                    throw new AssertionError(e);
                }
            } else if (site instanceof MethodInjectionSite) {
                MethodInjectionSite methodInjectionSite = (MethodInjectionSite) site;
                Method method = methodInjectionSite.getMethod();
                sites.put(method, entry.getValue());
            } else {
                // ignore other injection sites
            }
        }
    }

    private void setFactory(Injectable injectable, Supplier supplier, InjectionAttributes attributes) {
        Supplier<?> foundSupplier = findSupplier(injectable);
        if (foundSupplier == null) {
            Class<?> type = getMemberType(injectable);
            if (Map.class.equals(type)) {
                MapMultiplicitySupplier mapFactory = new MapMultiplicitySupplier();
                mapFactory.startUpdate();
                mapFactory.addSupplier(supplier, attributes);
                reinjectionMappings.put(mapFactory, injectable);
            } else if (Set.class.equals(type)) {
                SetMultiplicitySupplier setFactory = new SetMultiplicitySupplier();
                setFactory.startUpdate();
                setFactory.addSupplier(supplier, attributes);
                reinjectionMappings.put(setFactory, injectable);
            } else if (List.class.equals(type)) {
                ListMultiplicitySupplier listFactory = new ListMultiplicitySupplier();
                listFactory.startUpdate();
                listFactory.addSupplier(supplier, attributes);
                reinjectionMappings.put(listFactory, injectable);
            } else if (Collection.class.equals(type)) {
                ListMultiplicitySupplier listFactory = new ListMultiplicitySupplier();
                listFactory.startUpdate();
                listFactory.addSupplier(supplier, attributes);
                reinjectionMappings.put(listFactory, injectable);
            } else if (type.isArray()) {
                ArrayMultiplicitySupplier arrayFactory = new ArrayMultiplicitySupplier(type.getComponentType());
                arrayFactory.startUpdate();
                arrayFactory.addSupplier(supplier, attributes);
                reinjectionMappings.put(arrayFactory, injectable);
            } else {
                reinjectionMappings.put(supplier, injectable);
            }
        } else if (foundSupplier instanceof MultiplicitySupplier) {
            MultiplicitySupplier<?> multiplicitySupplier = (MultiplicitySupplier<?>) foundSupplier;
            multiplicitySupplier.addSupplier(supplier, attributes);
        } else {
            //update or overwrite  the factory
            reinjectionMappings.put(supplier, injectable);
        }
    }

    /**
     * Finds the mapped Supplier for the injectable.
     *
     * @param injectable the injectable
     * @return the Supplier
     */
    private Supplier<?> findSupplier(Injectable injectable) {
        Supplier<?> supplier = null;
        for (Map.Entry<Supplier, Injectable> entry : reinjectionMappings.entrySet()) {
            if (injectable.equals(entry.getValue())) {
                supplier = entry.getKey();
                break;
            }
        }
        return supplier;
    }

    /**
     * Returns the injectable type.
     *
     * @param injectable the injectable
     * @return the type
     */
    private Class<?> getMemberType(Injectable injectable) {
        for (Map.Entry<Member, Injectable> entry : sites.entrySet()) {
            if (injectable.equals(entry.getValue())) {
                Member member = entry.getKey();
                if (member instanceof Method) {
                    return ((Method) member).getParameterTypes()[0];
                } else if (member instanceof Field) {
                    return ((Field) member).getType();
                } else {
                    throw new AssertionError("Unsupported injection site type for singleton components");
                }
            }
        }
        return null;
    }

    private Field getField(String name) throws NoSuchFieldException {
        Class<?> clazz = instance.getClass();
        while (clazz != null) {
            try {
                return clazz.getDeclaredField(name);
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }

    /**
     * Injects a new value on a field or method of the instance.
     *
     * @param attribute the InjectableAttribute defining the field or method
     * @param factory   the Supplier that returns the value to inject
     * @throws Fabric3Exception if an error occurs during injection
     */
    private void inject(Injectable attribute, Supplier factory) throws Fabric3Exception {
        for (Map.Entry<Member, Injectable> entry : sites.entrySet()) {
            if (entry.getValue().equals(attribute)) {
                Member member = entry.getKey();
                if (member instanceof Field) {
                    try {
                        Object param = factory.get();
                        ((Field) member).set(instance, param);
                    } catch (IllegalAccessException e) {
                        // should not happen as accessibility is already set
                        throw new Fabric3Exception(e);
                    }
                } else if (member instanceof Method) {
                    try {
                        Object param = factory.get();
                        Method method = (Method) member;
                        method.invoke(instance, param);
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        // should not happen as accessibility is already set
                        throw new Fabric3Exception(e);
                    }
                } else {
                    // programming error
                    throw new Fabric3Exception("Unsupported member type" + member);
                }
            }
        }
    }

}
