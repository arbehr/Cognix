package com.cognitivabrasil.cognix;

import ORG.oclc.oai.server.OAIHandler;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

@ComponentScan(basePackages = "com.cognitivabrasil.cognix")
@SpringBootApplication
public class CognixApplication extends SpringBootServletInitializer {

    public static void main(String[] args) {
        SpringApplication.run(CognixApplication.class, args);
    }

    /**
     * Registra servlet para o Oaicat.
     *
     * @return servlet do oaicat.
     */
    @Bean
    public ServletRegistrationBean servletRegistrationBean() {
        OAIHandler h = new OAIHandler();

        ServletRegistrationBean s = new ServletRegistrationBean(h, "/oai/*");

        s.addInitParameter("properties", "dummy.properties");

        return s;
    }
}
