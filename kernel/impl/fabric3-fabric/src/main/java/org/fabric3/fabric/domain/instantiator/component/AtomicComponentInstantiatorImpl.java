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
package org.fabric3.fabric.domain.instantiator.component;

import java.net.URI;

import org.fabric3.api.model.type.component.BindingDefinition;
import org.fabric3.api.model.type.component.ComponentDefinition;
import org.fabric3.api.model.type.component.ComponentType;
import org.fabric3.api.model.type.component.ConsumerDefinition;
import org.fabric3.api.model.type.component.ProducerDefinition;
import org.fabric3.api.model.type.component.ReferenceDefinition;
import org.fabric3.api.model.type.component.ResourceReferenceDefinition;
import org.fabric3.api.model.type.component.ServiceDefinition;
import org.fabric3.fabric.domain.instantiator.AtomicComponentInstantiator;
import org.fabric3.fabric.domain.instantiator.InstantiationContext;
import org.fabric3.spi.model.instance.LogicalBinding;
import org.fabric3.spi.model.instance.LogicalComponent;
import org.fabric3.spi.model.instance.LogicalCompositeComponent;
import org.fabric3.spi.model.instance.LogicalConsumer;
import org.fabric3.spi.model.instance.LogicalProducer;
import org.fabric3.spi.model.instance.LogicalReference;
import org.fabric3.spi.model.instance.LogicalResourceReference;
import org.fabric3.spi.model.instance.LogicalService;

/**
 *
 */
public class AtomicComponentInstantiatorImpl extends AbstractComponentInstantiator implements AtomicComponentInstantiator {

    @SuppressWarnings({"unchecked"})
    public LogicalComponent instantiate(ComponentDefinition<?> definition, LogicalCompositeComponent parent, InstantiationContext context) {
        URI uri = URI.create(parent.getUri() + "/" + definition.getName());
        LogicalComponent<?> component = new LogicalComponent(uri, definition, parent);
        if (parent.getComponent(uri) != null) {
            DuplicateComponent error = new DuplicateComponent(uri, parent);
            context.addError(error);
        }
        parent.addComponent(component);

        ComponentType componentType = definition.getComponentType();
        if (componentType == null) {
            return component;
        }
        initializeProperties(component, definition, context);
        createServices(definition, component, componentType);
        createReferences(definition, component, componentType);
        createProducers(definition, component, componentType);
        createConsumers(definition, component, componentType);
        createResourceReferences(component, componentType);
        return component;
    }

    private void createServices(ComponentDefinition<?> definition, LogicalComponent<?> component, ComponentType componentType) {
        for (ServiceDefinition<ComponentType> service : componentType.getServices().values()) {
            String name = service.getName();
            URI serviceUri = component.getUri().resolve('#' + name);
            LogicalService logicalService = new LogicalService(serviceUri, service, component);

            for (BindingDefinition binding : service.getBindings()) {
                LogicalBinding<BindingDefinition> logicalBinding = new LogicalBinding<>(binding, logicalService);
                logicalService.addBinding(logicalBinding);
            }

            for (BindingDefinition binding : service.getCallbackBindings()) {
                LogicalBinding<BindingDefinition> logicalBinding = new LogicalBinding<>(binding, logicalService);
                logicalService.addCallbackBinding(logicalBinding);
            }

            // service is configured in the component definition
            ServiceDefinition<ComponentDefinition> componentService = definition.getServices().get(name);
            if (componentService != null) {
                for (BindingDefinition binding : componentService.getBindings()) {
                    LogicalBinding<BindingDefinition> logicalBinding = new LogicalBinding<>(binding, logicalService);
                    logicalService.addBinding(logicalBinding);
                }
                for (BindingDefinition binding : componentService.getCallbackBindings()) {
                    LogicalBinding<BindingDefinition> logicalBinding = new LogicalBinding<>(binding, logicalService);
                    logicalService.addCallbackBinding(logicalBinding);
                }
            }
            component.addService(logicalService);
        }
    }

    private void createReferences(ComponentDefinition<?> definition, LogicalComponent<?> component, ComponentType componentType) {
        for (ReferenceDefinition<ComponentType> reference : componentType.getReferences().values()) {
            String name = reference.getName();
            URI referenceUri = component.getUri().resolve('#' + name);
            LogicalReference logicalReference = new LogicalReference(referenceUri, reference, component);

            ReferenceDefinition<ComponentDefinition> componentReference = definition.getReferences().get(name);
            if (componentReference != null) {
                // reference is configured in the component definition
                for (BindingDefinition binding : componentReference.getBindings()) {
                    LogicalBinding<BindingDefinition> logicalBinding = new LogicalBinding<>(binding, logicalReference);
                    logicalReference.addBinding(logicalBinding);
                }
                for (BindingDefinition binding : componentReference.getCallbackBindings()) {
                    LogicalBinding<BindingDefinition> logicalBinding = new LogicalBinding<>(binding, logicalReference);
                    logicalReference.addCallbackBinding(logicalBinding);
                }
            } else {
                // check if reference is configured with bindings in the component type
                for (BindingDefinition binding : reference.getBindings()) {
                    LogicalBinding<BindingDefinition> logicalBinding = new LogicalBinding<>(binding, logicalReference);
                    logicalReference.addBinding(logicalBinding);
                }
                for (BindingDefinition binding : reference.getCallbackBindings()) {
                    LogicalBinding<BindingDefinition> logicalBinding = new LogicalBinding<>(binding, logicalReference);
                    logicalReference.addCallbackBinding(logicalBinding);
                }
            }
            component.addReference(logicalReference);
        }
    }

    private void createConsumers(ComponentDefinition<?> definition, LogicalComponent<?> component, ComponentType componentType) {
        for (ConsumerDefinition<ComponentType> consumer : componentType.getConsumers().values()) {
            String name = consumer.getName();
            URI consumerUri = component.getUri().resolve('#' + name);
            LogicalConsumer logicalConsumer = new LogicalConsumer(consumerUri, consumer, component);

            // producer is configured in the component definition
            ConsumerDefinition<ComponentDefinition> componentConsumer = definition.getConsumers().get(name);
            if (componentConsumer != null) {
                for (URI uri : componentConsumer.getSources()) {
                    addSource(logicalConsumer, uri, component);
                }
            } else {
                for (URI uri : consumer.getSources()) {
                    addSource(logicalConsumer, uri, component);
                }
            }
            component.addConsumer(logicalConsumer);
        }
    }

    private void createProducers(ComponentDefinition<?> definition, LogicalComponent<?> component, ComponentType componentType) {
        for (ProducerDefinition<ComponentType> producer : componentType.getProducers().values()) {
            String name = producer.getName();
            URI producerUri = component.getUri().resolve('#' + name);
            LogicalProducer logicalProducer = new LogicalProducer(producerUri, producer, component);

            // producer is configured in the component definition
            ProducerDefinition<ComponentDefinition> componentProducer = definition.getProducers().get(name);
            if (componentProducer != null) {
                for (URI uri : componentProducer.getTargets()) {
                    addTarget(logicalProducer, uri, component);
                }
            } else {
                for (URI uri : producer.getTargets()) {
                    addTarget(logicalProducer, uri, component);
                }
            }
            component.addProducer(logicalProducer);
        }
    }

    private void addSource(LogicalConsumer logicalConsumer, URI uri, LogicalComponent<?> component) {
        if (uri.isAbsolute()) {
            LogicalComponent<?> domain = component.getParent();
            while (domain.getParent() != null) {
                domain = domain.getParent();
            }
            logicalConsumer.addSource(URI.create(domain.getUri().toString() + "/" + uri.getAuthority()));
        } else {
            logicalConsumer.addSource(URI.create(component.getParent().getUri().toString() + "/" + uri.toString()));
        }
    }

    private void addTarget(LogicalProducer logicalProducer, URI uri, LogicalComponent<?> component) {
        if (uri.isAbsolute()) {
            LogicalComponent<?> domain = component.getParent();
            while (domain.getParent() != null) {
                domain = domain.getParent();
            }
            logicalProducer.addTarget(URI.create(domain.getUri().toString() + "/" + uri.getAuthority()));
        } else {
            logicalProducer.addTarget(URI.create(component.getParent().getUri().toString() + "/" + uri.toString()));
        }
    }

    private void createResourceReferences(LogicalComponent<?> component, ComponentType componentType) {
        for (ResourceReferenceDefinition resourceReference : componentType.getResourceReferences().values()) {
            URI resourceUri = component.getUri().resolve('#' + resourceReference.getName());
            LogicalResourceReference<ResourceReferenceDefinition> logicalResourceReference = new LogicalResourceReference<>(
                    resourceUri,
                    resourceReference,
                    component);
            component.addResource(logicalResourceReference);
        }
    }

}
