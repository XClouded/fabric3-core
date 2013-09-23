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
package org.fabric3.node.domain;

import javax.xml.namespace.QName;
import java.net.URI;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.fabric3.host.Names;
import org.fabric3.host.Namespaces;
import org.fabric3.host.failure.ValidationFailure;
import org.fabric3.model.type.component.ComponentDefinition;
import org.fabric3.model.type.component.ComponentReference;
import org.fabric3.model.type.component.Composite;
import org.fabric3.model.type.component.Multiplicity;
import org.fabric3.node.nonmanaged.NonManagedImplementation;
import org.fabric3.node.nonmanaged.NonManagedPhysicalSourceDefinition;
import org.fabric3.spi.builder.BuilderException;
import org.fabric3.spi.builder.Connector;
import org.fabric3.spi.generator.GenerationException;
import org.fabric3.spi.generator.WireGenerator;
import org.fabric3.spi.instantiator.AutowireResolver;
import org.fabric3.spi.introspection.DefaultIntrospectionContext;
import org.fabric3.spi.introspection.java.contract.JavaContractProcessor;
import org.fabric3.spi.lcm.LogicalComponentManager;
import org.fabric3.spi.model.instance.LogicalComponent;
import org.fabric3.spi.model.instance.LogicalCompositeComponent;
import org.fabric3.spi.model.instance.LogicalReference;
import org.fabric3.spi.model.instance.LogicalService;
import org.fabric3.spi.model.instance.LogicalWire;
import org.fabric3.spi.model.physical.PhysicalWireDefinition;
import org.fabric3.spi.model.type.java.JavaServiceContract;
import org.oasisopen.sca.ServiceRuntimeException;
import org.oasisopen.sca.annotation.Reference;

/**
 *
 */
public class ServiceResolverImpl implements ServiceResolver {
    private static final QName SYNTHETIC_DEPLOYABLE = new QName(Namespaces.F3, "Synthetic");

    private JavaContractProcessor contractProcessor;
    private LogicalComponentManager lcm;
    private AutowireResolver autowireResolver;
    private WireGenerator wireGenerator;
    private Connector connector;
    private AtomicInteger idCounter = new AtomicInteger();

    public ServiceResolverImpl(@Reference InstanceDeployer deployer,
                               @Reference JavaContractProcessor contractProcessor,
                               @Reference LogicalComponentManager lcm,
                               @Reference AutowireResolver autowireResolver,
                               @Reference WireGenerator wireGenerator,
                               @Reference Connector connector) {
        this.contractProcessor = contractProcessor;
        this.lcm = lcm;
        this.autowireResolver = autowireResolver;
        this.wireGenerator = wireGenerator;
        this.connector = connector;
    }

    public <T> T resolve(Class<T> interfaze) throws ResolverException {
        LogicalWire wire = createWire(interfaze);
        try {
            PhysicalWireDefinition physicalWireDefinition = wireGenerator.generateWire(wire);
            physicalWireDefinition.getTarget().setClassLoaderId(Names.HOST_CONTRIBUTION);
            NonManagedPhysicalSourceDefinition source = (NonManagedPhysicalSourceDefinition) physicalWireDefinition.getSource();
            source.setClassLoaderId(Names.HOST_CONTRIBUTION);
            connector.connect(physicalWireDefinition);
            return interfaze.cast(source.getProxy());
        } catch (GenerationException e) {
            throw new ResolverException(e);
        } catch (BuilderException e) {
            throw new ResolverException(e);
        }
    }

    private <T> LogicalWire createWire(Class<T> interfaze) throws ResolverException {
        JavaServiceContract contract = introspectInterface(interfaze);

        LogicalReference logicalReference = createReference(contract);

        LogicalCompositeComponent domainComponent = lcm.getRootComponent();

        List<LogicalService> services = autowireResolver.resolve(logicalReference, contract, domainComponent);
        if (services.isEmpty()) {
            throw new ServiceRuntimeException("Service not found for type: " + interfaze.getName());
        }
        LogicalService targetService = services.get(0);

        return new LogicalWire(domainComponent, logicalReference, targetService, SYNTHETIC_DEPLOYABLE, true);
    }

    private LogicalReference createReference(JavaServiceContract contract) {
        LogicalCompositeComponent domainComponent = lcm.getRootComponent();

        int id = idCounter.getAndIncrement();
        String name = "Synthetic" + id;
        URI componentUri = URI.create(domainComponent.getUri().toString() + "/" + name);
        URI referenceUri = URI.create(componentUri.toString() + "#reference");
        Composite composite = new Composite(new QName(Namespaces.F3, "SyntheticComposite" + id));

        ComponentDefinition<NonManagedImplementation> componentDefinition = new ComponentDefinition<NonManagedImplementation>(name);
        componentDefinition.setParent(composite);
        NonManagedImplementation implementation = new NonManagedImplementation();
        componentDefinition.setImplementation(implementation);
        ComponentReference reference = new ComponentReference("reference", Multiplicity.ONE_ONE);
        componentDefinition.add(reference);

        LogicalComponent<NonManagedImplementation> logicalComponent = new LogicalComponent<NonManagedImplementation>(componentUri,
                                                                                                                     componentDefinition,
                                                                                                                     domainComponent);
        reference.setServiceContract(contract);
        LogicalReference logicalReference = new LogicalReference(referenceUri, reference, logicalComponent);
        logicalReference.setServiceContract(contract);

        logicalComponent.addReference(logicalReference);
        return logicalReference;
    }

    private <T> JavaServiceContract introspectInterface(Class<T> interfaze) throws ResolverException {
        DefaultIntrospectionContext context = new DefaultIntrospectionContext();
        JavaServiceContract contract = contractProcessor.introspect(interfaze, context);
        StringBuilder builder = new StringBuilder();
        if (context.hasErrors()) {
            for (ValidationFailure failure : context.getErrors()) {
                builder.append(failure.getMessage()).append("\n");
            }
            throw new ResolverException(builder.toString());
        }
        return contract;
    }

}
