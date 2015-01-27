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
package org.fabric3.binding.jms.runtime;

import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.Topic;
import javax.transaction.TransactionManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.fabric3.api.binding.jms.model.ConnectionFactoryDefinition;
import org.fabric3.api.binding.jms.model.CorrelationScheme;
import org.fabric3.api.binding.jms.model.DeliveryMode;
import org.fabric3.api.binding.jms.model.DestinationDefinition;
import org.fabric3.api.binding.jms.model.DestinationType;
import org.fabric3.api.binding.jms.model.HeadersDefinition;
import org.fabric3.api.binding.jms.model.JmsBindingMetadata;
import org.fabric3.api.binding.jms.model.OperationPropertiesDefinition;
import org.fabric3.api.model.type.contract.DataType;
import org.fabric3.binding.jms.runtime.resolver.AdministeredObjectResolver;
import org.fabric3.binding.jms.runtime.wire.InterceptorConfiguration;
import org.fabric3.binding.jms.runtime.wire.JmsInterceptor;
import org.fabric3.binding.jms.runtime.wire.ResponseListener;
import org.fabric3.binding.jms.runtime.wire.WireConfiguration;
import org.fabric3.binding.jms.spi.provision.JmsWireTargetDefinition;
import org.fabric3.binding.jms.spi.provision.OperationPayloadTypes;
import org.fabric3.spi.classloader.ClassLoaderRegistry;
import org.fabric3.spi.container.ContainerException;
import org.fabric3.spi.container.binding.handler.BindingHandler;
import org.fabric3.spi.container.binding.handler.BindingHandlerRegistry;
import org.fabric3.spi.container.builder.component.TargetWireAttacher;
import org.fabric3.spi.container.objectfactory.ObjectFactory;
import org.fabric3.spi.container.wire.Interceptor;
import org.fabric3.spi.container.wire.InvocationChain;
import org.fabric3.spi.container.wire.TransformerInterceptorFactory;
import org.fabric3.spi.container.wire.Wire;
import org.fabric3.spi.model.physical.PhysicalBindingHandlerDefinition;
import org.fabric3.spi.model.physical.PhysicalDataTypes;
import org.fabric3.spi.model.physical.PhysicalOperationDefinition;
import org.fabric3.spi.model.physical.PhysicalWireSourceDefinition;
import org.oasisopen.sca.annotation.Reference;

/**
 * Attaches the reference end of a wire to a JMS destination.
 */
public class JmsTargetWireAttacher implements TargetWireAttacher<JmsWireTargetDefinition> {
    private AdministeredObjectResolver resolver;
    private TransactionManager tm;
    private ClassLoaderRegistry classLoaderRegistry;
    private BindingHandlerRegistry handlerRegistry;
    private TransformerInterceptorFactory interceptorFactory;

    public JmsTargetWireAttacher(@Reference AdministeredObjectResolver resolver,
                                 @Reference TransactionManager tm,
                                 @Reference ClassLoaderRegistry classLoaderRegistry,
                                 @Reference BindingHandlerRegistry handlerRegistry,
                                 @Reference TransformerInterceptorFactory interceptorFactory) {
        this.resolver = resolver;
        this.tm = tm;
        this.classLoaderRegistry = classLoaderRegistry;
        this.handlerRegistry = handlerRegistry;
        this.interceptorFactory = interceptorFactory;
    }

    public void attach(PhysicalWireSourceDefinition source, JmsWireTargetDefinition target, Wire wire) throws ContainerException {

        WireConfiguration wireConfiguration = new WireConfiguration();
        ClassLoader targetClassLoader = classLoaderRegistry.getClassLoader(target.getClassLoaderId());
        wireConfiguration.setClassloader(targetClassLoader);
        wireConfiguration.setTransactionManager(tm);
        wireConfiguration.setCorrelationScheme(target.getMetadata().getCorrelationScheme());
        wireConfiguration.setResponseTimeout(target.getMetadata().getResponseTimeout());
        wireConfiguration.setSessionType(target.getSessionType());

        JmsBindingMetadata metadata = target.getMetadata();
        HeadersDefinition headers = metadata.getHeaders();
        boolean persistent = DeliveryMode.PERSISTENT == headers.getDeliveryMode() || headers.getDeliveryMode() == null;
        wireConfiguration.setPersistent(persistent);

        // resolve the connection factories and destinations for the wire
        resolveAdministeredObjects(target, wireConfiguration);

        List<BindingHandler<Message>> handlers = createHandlers(target);

        List<OperationPayloadTypes> types = target.getPayloadTypes();
        for (InvocationChain chain : wire.getInvocationChains()) {
            // setup operation-specific configuration and create an interceptor
            InterceptorConfiguration configuration = new InterceptorConfiguration(wireConfiguration);
            PhysicalOperationDefinition op = chain.getPhysicalOperation();
            String operationName = op.getName();
            configuration.setOperationName(operationName);
            configuration.setOneWay(op.isOneWay());
            processJmsHeaders(configuration, metadata);
            OperationPayloadTypes payloadTypes = resolveOperation(operationName, types);
            configuration.setPayloadType(payloadTypes);
            if (target.getDataTypes().contains(PhysicalDataTypes.JAXB)) {
                addJAXBInterceptor(source, op, chain, targetClassLoader);
            }
            JmsInterceptor interceptor = new JmsInterceptor(configuration, handlers);
            chain.addInterceptor(interceptor);
        }

    }

    public void detach(PhysicalWireSourceDefinition source, JmsWireTargetDefinition target) throws ContainerException {
        resolver.release(target.getMetadata().getConnectionFactory());
    }

    public ObjectFactory<?> createObjectFactory(JmsWireTargetDefinition target) throws ContainerException {
        throw new UnsupportedOperationException();
    }

    /**
     * Processes JMS headers for the interceptor configuration. URI headers are given precedence, followed by header values configured on operation properties
     * and the binding.jms/headers element respectively.
     *
     * @param configuration the interceptor configuration
     * @param metadata      the JMS binding metadata
     * @throws ContainerException if an error processing headers occurs
     */
    private void processJmsHeaders(InterceptorConfiguration configuration, JmsBindingMetadata metadata) throws ContainerException {
        HeadersDefinition uriHeaders = metadata.getUriHeaders();
        HeadersDefinition headers = metadata.getHeaders();
        Map<String, OperationPropertiesDefinition> properties = metadata.getOperationProperties();

        // set the headers configured in the binding uri
        setBindingHeaders(configuration, uriHeaders);

        OperationPropertiesDefinition definition = properties.get(configuration.getOperationName());
        if (definition != null) {
            setOperationHeaders(configuration, definition);
        }

        // set the headers configured in the binding.jms/headers element
        setBindingHeaders(configuration, headers);
    }

    private void setBindingHeaders(InterceptorConfiguration configuration, HeadersDefinition headers) {
        String type = headers.getJmsType();
        if (type != null && configuration.getJmsType() != null) {
            configuration.setJmsType(type);
        }
        DeliveryMode deliveryMode = headers.getDeliveryMode();
        if (deliveryMode != null && configuration.getDeliveryMode() == -1) {
            setDeliveryMode(deliveryMode.toString(), configuration);
        }

        int priority = headers.getPriority();
        if (priority >= 0 && configuration.getPriority() == -1) {
            configuration.setPriority(priority);
        }

        long timeToLive = headers.getTimeToLive();
        if (timeToLive >= 0 && configuration.getTimeToLive() == -1) {
            configuration.setTimeToLive(timeToLive);
        }
        for (Map.Entry<String, String> entry : headers.getProperties().entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (configuration.getProperties().containsKey(key)) {
                // property already defined, skip
                continue;
            }
            configuration.addProperty(key, value);
        }
    }

    private void setOperationHeaders(InterceptorConfiguration configuration, OperationPropertiesDefinition definition) throws ContainerException {
        for (Map.Entry<String, String> entry : definition.getProperties().entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (configuration.getJmsType() == null && "JMSType".equals(key)) {
                if (configuration.getJmsType() != null) {
                    configuration.setJmsType(value);
                }
            } else if (configuration.getDeliveryMode() == -1 && "JMSDeliveryMode".equals(key)) {
                setDeliveryMode(value, configuration);
            } else if (configuration.getTimeToLive() == -1 && "JMSTimeToLive".equals(key)) {
                try {
                    long time = Long.valueOf(value);
                    configuration.setTimeToLive(time);
                } catch (NumberFormatException e) {
                    throw new ContainerException(e);
                }
            } else if (configuration.getPriority() == -1 && "JMSPriority".equals(key)) {
                try {
                    int priority = Integer.valueOf(value);
                    configuration.setPriority(priority);
                } catch (NumberFormatException e) {
                    throw new ContainerException(e);
                }
            } else {
                // user-defined property
                configuration.addProperty(key, value);
            }
        }
    }

    private void setDeliveryMode(String value, InterceptorConfiguration configuration) {
        if ("persistent".equalsIgnoreCase(value)) {
            configuration.setDeliveryMode(javax.jms.DeliveryMode.PERSISTENT);
        } else if ("nonpersistent".equalsIgnoreCase(value)) {
            configuration.setDeliveryMode(javax.jms.DeliveryMode.NON_PERSISTENT);
        }
    }

    private void addJAXBInterceptor(PhysicalWireSourceDefinition source, PhysicalOperationDefinition op, InvocationChain chain, ClassLoader targetClassLoader)
            throws ContainerException {
        ClassLoader sourceClassLoader = classLoaderRegistry.getClassLoader(source.getClassLoaderId());
        List<DataType> jaxTypes = DataTypeHelper.createTypes(op, sourceClassLoader);
        Interceptor jaxbInterceptor = interceptorFactory.createInterceptor(op, jaxTypes, DataTypeHelper.JAXB_TYPES, targetClassLoader, sourceClassLoader);
        chain.addInterceptor(jaxbInterceptor);
    }

    private void resolveAdministeredObjects(JmsWireTargetDefinition target, WireConfiguration wireConfiguration) throws ContainerException {
        JmsBindingMetadata metadata = target.getMetadata();

        ConnectionFactoryDefinition connectionFactoryDefinition = metadata.getConnectionFactory();

        try {
            ConnectionFactory requestConnectionFactory = resolver.resolve(connectionFactoryDefinition);
            DestinationDefinition destinationDefinition = metadata.getDestination();
            Destination requestDestination = resolver.resolve(destinationDefinition, requestConnectionFactory);
            wireConfiguration.setRequestConnectionFactory(requestConnectionFactory);
            wireConfiguration.setRequestDestination(requestDestination);
            validateDestination(requestDestination, destinationDefinition);
            if (metadata.isResponse()) {
                connectionFactoryDefinition = metadata.getResponseConnectionFactory();

                ConnectionFactory responseConnectionFactory = resolver.resolve(connectionFactoryDefinition);
                destinationDefinition = metadata.getResponseDestination();
                Destination responseDestination = resolver.resolve(destinationDefinition, responseConnectionFactory);
                CorrelationScheme scheme = metadata.getCorrelationScheme();
                ResponseListener listener = new ResponseListener(responseDestination, scheme);
                wireConfiguration.setResponseListener(listener);
                validateDestination(responseDestination, destinationDefinition);
            }
            DestinationDefinition callbackDestinationDefinition = target.getCallbackDestination();
            if (callbackDestinationDefinition != null) {
                Destination callbackDestination = resolver.resolve(callbackDestinationDefinition, requestConnectionFactory);
                wireConfiguration.setCallbackDestination(callbackDestination);
                if (callbackDestination != null) {
                    if (callbackDestination instanceof Queue) {
                        String name = ((Queue) callbackDestination).getQueueName();
                        wireConfiguration.setCallbackUri("jms:" + name);
                    } else if (callbackDestination instanceof Topic) {
                        String name = ((Topic) callbackDestination).getTopicName();
                        wireConfiguration.setCallbackUri("jms:" + name);
                    }
                }

            }
        } catch (JMSException e) {
            throw new ContainerException(e);
        }

    }

    private void validateDestination(Destination requestDestination, DestinationDefinition requestDestinationDefinition) throws ContainerException {
        DestinationType requestDestinationType = requestDestinationDefinition.geType();
        if (DestinationType.QUEUE == requestDestinationType && !(requestDestination instanceof Queue)) {
            throw new ContainerException("Destination is not a queue: " + requestDestinationDefinition.getName());
        } else if (DestinationType.TOPIC == requestDestinationType && !(requestDestination instanceof Topic)) {
            throw new ContainerException("Destination is not a topic: " + requestDestinationDefinition.getName());
        }
    }

    private OperationPayloadTypes resolveOperation(String operationName, List<OperationPayloadTypes> payloadTypes) {
        for (OperationPayloadTypes type : payloadTypes) {
            if (type.getName().equals(operationName)) {
                return type;
            }
        }
        // programming error
        throw new AssertionError("Error resolving operation: " + operationName);
    }

    private List<BindingHandler<Message>> createHandlers(JmsWireTargetDefinition target) {
        if (target.getHandlers().isEmpty()) {
            return null;
        }
        List<BindingHandler<Message>> handlers = new ArrayList<>();
        for (PhysicalBindingHandlerDefinition handlerDefinition : target.getHandlers()) {
            BindingHandler<Message> handler = handlerRegistry.createHandler(Message.class, handlerDefinition);
            handlers.add(handler);
        }
        return handlers;
    }

}
