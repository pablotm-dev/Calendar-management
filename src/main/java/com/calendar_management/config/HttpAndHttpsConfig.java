package com.calendar_management.config;

import org.apache.catalina.connector.Connector;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class that adds an additional HTTP connector.
 * This allows the application to accept HTTP requests on both server.port and server.http.port.
 */
@Configuration
public class HttpAndHttpsConfig {

    @Value("${server.http.port:8080}")
    private int httpPort;

    @Value("${server.port:8081}")
    private int mainPort;

    @Bean
    public ServletWebServerFactory servletContainer() {
        TomcatServletWebServerFactory tomcat = new TomcatServletWebServerFactory();

        // Add HTTP connector only
        // Spring Boot will handle the main HTTP connector based on server.port
        tomcat.addAdditionalTomcatConnectors(createHttpConnector());

        return tomcat;
    }

    private Connector createHttpConnector() {
        Connector connector = new Connector("org.apache.coyote.http11.Http11NioProtocol");
        connector.setScheme("http");
        connector.setPort(httpPort);
        connector.setSecure(false);
        return connector;
    }
}
