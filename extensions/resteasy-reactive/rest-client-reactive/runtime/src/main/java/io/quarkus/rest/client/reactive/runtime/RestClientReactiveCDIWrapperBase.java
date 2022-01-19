package io.quarkus.rest.client.reactive.runtime;

import java.io.Closeable;
import java.io.IOException;

import javax.annotation.PreDestroy;

import org.jboss.logging.Logger;

import io.quarkus.arc.NoClassInterceptors;

public abstract class RestClientReactiveCDIWrapperBase<T extends Closeable> implements Closeable {
    private static final Logger log = Logger.getLogger(RestClientReactiveCDIWrapperBase.class);

    private final T delegate;

    public RestClientReactiveCDIWrapperBase(Class<T> jaxrsInterface, String baseUriFromAnnotation, String configKey) {
        this.delegate = RestClientCDIDelegateBuilder.createDelegate(jaxrsInterface, baseUriFromAnnotation, configKey);
    }

    @Override
    @NoClassInterceptors
    public void close() throws IOException {
        delegate.close();
    }

    @PreDestroy
    @NoClassInterceptors
    public void destroy() {
        try {
            close();
        } catch (IOException e) {
            log.warn("Failed to close cdi-created rest client instance", e);
        }
    }

    // used by generated code
    @SuppressWarnings("unused")
    @NoClassInterceptors
    public Object getDelegate() {
        return delegate;
    }
}
