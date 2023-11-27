package com.example.mtls;

import org.springframework.boot.ssl.SslBundle;
import org.springframework.stereotype.Component;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Moritz Halbritter
 */
@Component
class SslContextFactory {
    SSLContext build(SslBundle... bundles) throws KeyManagementException, NoSuchAlgorithmException {
        if (bundles.length == 0) {
            return createEmptySslContext();
        }
        SSLContext sslContext = SSLContext.getInstance(bundles[0].getProtocol());
        sslContext.init(getKeyManagers(bundles), getTrustManagers(bundles), null);
        return sslContext;
    }

    private SSLContext createEmptySslContext() throws NoSuchAlgorithmException, KeyManagementException {
        SSLContext context = SSLContext.getInstance(SslBundle.DEFAULT_PROTOCOL);
        context.init(new KeyManager[0], new TrustManager[0], null);
        return context;
    }

    private KeyManager[] getKeyManagers(SslBundle... bundles) {
        List<KeyManager> keyManagers = new ArrayList<>();
        for (SslBundle bundle : bundles) {
            keyManagers.addAll(Arrays.asList(bundle.getManagers().getKeyManagers()));
        }
        return keyManagers.toArray(KeyManager[]::new);
    }

    private TrustManager[] getTrustManagers(SslBundle... bundles) {
        List<X509TrustManager> trustManagers = new ArrayList<>();
        for (SslBundle bundle : bundles) {
            for (TrustManager trustManager : bundle.getManagers().getTrustManagers()) {
                if (trustManager instanceof X509TrustManager x509TrustManager) {
                    trustManagers.add(x509TrustManager);
                } else {
                    throw new IllegalStateException("Unsupported trust manager type: " + trustManager.getClass());
                }
            }
        }
        return new TrustManager[]{new CompositeX509TrustManager(trustManagers)};
    }

    // Although javax.net.ssl.SSLContext.init allows to pass in multiple TrustManagers, it will only use the
    // first X509TrustManager. This is a X509TrustManager implementation, which delegates to multiple X509TrustManagers.
    private static class CompositeX509TrustManager implements X509TrustManager {
        private final List<X509TrustManager> trustManagers;

        CompositeX509TrustManager(List<X509TrustManager> trustManagers) {
            this.trustManagers = trustManagers;
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            for (X509TrustManager trustManager : this.trustManagers) {
                try {
                    trustManager.checkClientTrusted(chain, authType);
                    return;
                } catch (CertificateException e) {
                    // Ignore
                }
            }
            throw new CertificateException("None of the TrustManagers trust this certificate chain");
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            for (X509TrustManager trustManager : this.trustManagers) {
                try {
                    trustManager.checkServerTrusted(chain, authType);
                    return;
                } catch (CertificateException e) {
                    // Ignore
                }
            }
            throw new CertificateException("None of the TrustManagers trust this certificate chain");
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            List<X509Certificate> certificates = new ArrayList<>();
            for (X509TrustManager trustManager : this.trustManagers) {
                certificates.addAll(Arrays.asList(trustManager.getAcceptedIssuers()));
            }
            return certificates.toArray(X509Certificate[]::new);
        }
    }

}
