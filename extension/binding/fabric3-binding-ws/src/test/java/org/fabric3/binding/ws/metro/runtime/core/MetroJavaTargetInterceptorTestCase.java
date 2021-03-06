package org.fabric3.binding.ws.metro.runtime.core;

import javax.xml.ws.BindingProvider;
import javax.xml.ws.WebServiceException;
import java.lang.reflect.Method;
import java.net.SocketTimeoutException;

import junit.framework.TestCase;
import org.easymock.EasyMock;
import org.fabric3.spi.container.invocation.MessageImpl;
import org.oasisopen.sca.ServiceUnavailableException;

/**
 *
 */
public class MetroJavaTargetInterceptorTestCase extends TestCase {

    private Service proxy;
    private Method method;
    private InterceptorMonitor monitor;

    public void testRetries() throws Exception {
        MetroJavaTargetInterceptor interceptor = new MetroJavaTargetInterceptor(() -> proxy, method, false, 1, monitor);

        proxy.invoke();
        EasyMock.expectLastCall().andThrow(new WebServiceException(new SocketTimeoutException()));
        proxy.invoke();

        EasyMock.replay(proxy);

        interceptor.invoke(new MessageImpl());
        EasyMock.verify(proxy);
    }

    public void testNoRetry() throws Exception {
        MetroJavaTargetInterceptor interceptor = new MetroJavaTargetInterceptor(() -> proxy, method, false, 0, monitor);

        proxy.invoke();
        EasyMock.expectLastCall().andThrow(new WebServiceException(new SocketTimeoutException()));
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
        monitor = EasyMock.createMock(InterceptorMonitor.class);
        super.setUp();
    }

    private interface Service extends BindingProvider {

        void invoke();
    }
}
