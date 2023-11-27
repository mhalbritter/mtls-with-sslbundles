package com.example.mtls;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import javax.net.ssl.SSLContext;
import java.net.http.HttpClient;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

/**
 * @author Moritz Halbritter
 */
@Component
class Client {

    private static final Logger LOGGER = LoggerFactory.getLogger(Client.class);

    private final RestClient client;

    Client(SslBundles sslBundles, SslContextFactory sslContextFactory, RestClient.Builder builder) throws KeyManagementException, NoSuchAlgorithmException {
        SslBundle testBundle = sslBundles.getBundle("client");
        SslBundle caCerts = sslBundles.getBundle("cacerts");

        SSLContext sslContext = sslContextFactory.build(testBundle, caCerts);
        HttpClient httpClient = HttpClient.newBuilder().sslContext(sslContext).build();
        this.client = builder.requestFactory(new JdkClientHttpRequestFactory(httpClient)).build();
    }

    @Scheduled(fixedDelay = 1_000)
    void run() {
        String localhost = this.client.get().uri("https://localhost:8443").retrieve().body(String.class);
        LOGGER.info("Got '{}'", localhost);
        String exampleCom = this.client.get().uri("https://example.com").retrieve().body(String.class);
        LOGGER.info("Got '{}'", exampleCom);
    }
}
