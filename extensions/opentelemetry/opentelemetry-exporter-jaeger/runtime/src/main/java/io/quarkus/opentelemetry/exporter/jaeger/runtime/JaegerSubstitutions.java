package io.quarkus.opentelemetry.exporter.jaeger.runtime;

import static java.util.Objects.requireNonNull;

import javax.net.ssl.SSLException;
import javax.net.ssl.X509TrustManager;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

import io.grpc.ManagedChannelBuilder;
import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NettyChannelBuilder;
import io.opentelemetry.exporter.otlp.internal.grpc.ManagedChannelUtil;

/**
 * Replace the {@link ManagedChannelUtil#setTrustedCertificatesPem(ManagedChannelBuilder, byte[])} method in native
 * because the method implementation tries to look for grpc-netty-shaded dependencies, which we don't support.
 *
 * Check:
 * https://github.com/open-telemetry/opentelemetry-java/blob/v1.9.1/exporters/otlp/common/src/main/java/io/opentelemetry/exporter/otlp/internal/grpc/ManagedChannelUtil.java#L56-L89
 */
final class JaegerSubstitutions {
    @TargetClass(ManagedChannelUtil.class)
    static final class Target_ManagedChannelUtil {
        @Substitute
        public static void setTrustedCertificatesPem(
                ManagedChannelBuilder<?> managedChannelBuilder, byte[] trustedCertificatesPem)
                throws SSLException {
            requireNonNull(managedChannelBuilder, "managedChannelBuilder");
            requireNonNull(trustedCertificatesPem, "trustedCertificatesPem");

            X509TrustManager tm = io.opentelemetry.exporter.otlp.internal.TlsUtil.trustManager(trustedCertificatesPem);

            // gRPC does not abstract TLS configuration so we need to check the implementation and act
            // accordingly.
            if (managedChannelBuilder.getClass().getName().equals("io.grpc.netty.NettyChannelBuilder")) {
                NettyChannelBuilder nettyBuilder = (NettyChannelBuilder) managedChannelBuilder;
                nettyBuilder.sslContext(GrpcSslContexts.forClient().trustManager(tm).build());
            } else {
                throw new SSLException(
                        "TLS certificate configuration not supported for unrecognized ManagedChannelBuilder "
                                + managedChannelBuilder.getClass().getName());
            }
        }
    }
}
