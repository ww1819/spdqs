package com.qs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.qs.config.UploadProperties;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(UploadProperties.class)
public class QsApplication {

    public static void main(String[] args) {
        SpringApplication.run(QsApplication.class, args);
    }
}
