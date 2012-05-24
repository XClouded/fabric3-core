package org.fabric3.binding.ws.metro.runtime.core;

import java.lang.reflect.Method;
import java.net.SocketTimeoutException;
import java.util.HashMap;

import javax.xml.ws.BindingProvider;
import javax.xml.ws.WebServiceException;

import junit.framework.TestCase;
import org.easymock.EasyMock;
import org.oasisopen.sca.ServiceUnavailableException;

import org.fabric3.spi.binding.handler.BindingHandlerRegistry;
import org.fabric3.spi.invocation.MessageImpl;
import org.fabric3.spi.objectfactory.ObjectCreationException;
import org.fabric3.spi.objectfactory.ObjectFactory;

/**
 * @version $Rev$ $Date$
 */
public class MetroJavaTargetInterceptorTestCase extends TestCase {

    private Service proxy;
    private MockProxyFactory proxyFactory;
    private Method method;
    private InterceptorMonitor monitor;
	private BindingHandlerRegistry handlerRegistry;

    public void testRetries() throws Exception {
        MetroJavaTargetInterceptor interceptor = new MetroJavaTargetInterceptor(proxyFactory, method, false, null, null, 1, handlerRegistry, monitor);

        proxy.invoke();
        EasyMock.expectLastCall().andThrow(new WebServiceException(new SocketTimeoutException()));
        proxy.invoke();
        EasyMock.expect(proxy.getRequestContext()).andReturn(new HashMap<String, Object>());

        EasyMock.replay(proxy);

        interceptor.invoke(new MessageImpl());
        EasyMock.verify(proxy);
    }

    public void testNoRetry() throws Exception {
        MetroJavaTargetInterceptor interceptor = new MetroJavaTargetInterceptor(proxyFactory, method, false, null, null, 0, handlerRegistry , monitor);

        proxy.invoke();
        EasyMock.expectLastCall().andThrow(new WebServiceException(new SocketTimeoutException()));
        EasyMock.expect(proxy.getRequestContext()).andReturn(new HashMap<String, Object>());
        EasyMock.replay(proxy);

        try {
            interceptor.invoke(new MessageImpl());
            fail();
        } catch (ServiceUnavailableException e) {
            // expected
        }
        EasyMock.verify(proxy);
    }

    @Override
    public void setUp() throws Exception {
        method = Service.class.getMethod("invoke");
        proxy = EasyMock.createMock(Service.class);
        proxyFactory = new MockProxyFactory(proxy);
        monitor = EasyMock.createMock(InterceptorMonitor.class);
        handlerRegistry = EasyMock.createMock(BindingHandlerRegistry.class);
        super.setUp();
    }

    private class MockProxyFactory implements ObjectFactory {
        private Service service;

        private MockProxyFactory(Service service) {
            this.service = service;
        }

        public Object getInstance() throws ObjectCreationException {
            return service;
        }
    }

    private interface Service extends BindingProvider {

        void invoke();
    }
}