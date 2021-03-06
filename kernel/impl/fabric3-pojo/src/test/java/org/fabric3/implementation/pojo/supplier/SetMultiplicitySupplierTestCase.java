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
package org.fabric3.implementation.pojo.supplier;

import java.util.Iterator;
import java.util.Set;
import java.util.function.Supplier;

import junit.framework.TestCase;
import org.easymock.EasyMock;
import org.fabric3.spi.container.injection.InjectionAttributes;

/**
 *
 */
public class SetMultiplicitySupplierTestCase extends TestCase {
    private SetMultiplicitySupplier supplier = new SetMultiplicitySupplier();

    public void testReinjection() throws Exception {
        Supplier<?> mockFactory1 = EasyMock.createMock(Supplier.class);
        Supplier<?> mockFactory2 = EasyMock.createMock(Supplier.class);
        EasyMock.expect(mockFactory2.get()).andReturn(new Object());
        Supplier<?> mockFactory3 = EasyMock.createMock(Supplier.class);
        EasyMock.expect(mockFactory3.get()).andReturn(new Object());

        EasyMock.replay(mockFactory1, mockFactory2, mockFactory3);

        supplier.startUpdate();
        supplier.addSupplier(mockFactory1, InjectionAttributes.EMPTY_ATTRIBUTES);
        supplier.endUpdate();

        supplier.startUpdate();
        supplier.addSupplier(mockFactory2, InjectionAttributes.EMPTY_ATTRIBUTES);
        supplier.endUpdate();
        Set<Object> set = supplier.get();
        assertEquals(1, set.size());

        supplier.startUpdate();
        supplier.addSupplier(mockFactory3, InjectionAttributes.EMPTY_ATTRIBUTES);
        supplier.endUpdate();
        set = supplier.get();
        assertEquals(1, set.size());

        EasyMock.verify(mockFactory1, mockFactory2, mockFactory3);
    }

    public void testSort() throws Exception {
        Supplier<?> mockFactory1 = EasyMock.createMock(Supplier.class);
        Object object1 = new Object();
        EasyMock.expect(mockFactory1.get()).andReturn(object1).times(1);
        InjectionAttributes attributes1 = new InjectionAttributes(null, 2);

        Supplier<?> mockFactory2 = EasyMock.createMock(Supplier.class);
        Object object2 = new Object();
        EasyMock.expect(mockFactory2.get()).andReturn(object2).times(1);
        InjectionAttributes attributes2 = new InjectionAttributes(null, 0);


        EasyMock.replay(mockFactory1, mockFactory2);

        supplier.startUpdate();
        supplier.addSupplier(mockFactory2, attributes2);
        supplier.addSupplier(mockFactory1, attributes1);
        supplier.endUpdate();

        Set<Object> set = supplier.get();

        Iterator<Object> iterator = set.iterator();
        assertSame(object2, iterator.next());
        assertSame(object1, iterator.next());

        EasyMock.verify(mockFactory1, mockFactory2);
    }

    public void testNoUpdates() throws Exception {
        Supplier<?> mockFactory = EasyMock.createMock(Supplier.class);
        EasyMock.expect(mockFactory.get()).andReturn(new Object()).times(1);
        EasyMock.replay(mockFactory);

        supplier.startUpdate();
        supplier.addSupplier(mockFactory, InjectionAttributes.EMPTY_ATTRIBUTES);
        supplier.endUpdate();

        supplier.startUpdate();
        // no update
        supplier.endUpdate();

        Set<Object> instance = supplier.get();
        assertEquals(1, instance.size());

        EasyMock.verify(mockFactory);
    }

}
