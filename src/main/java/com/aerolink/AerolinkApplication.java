package com.aerolink;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class AerolinkApplication {

    public static void main(String[] args) {
        SpringApplication.run(AerolinkApplication.class, args);
    }

}
