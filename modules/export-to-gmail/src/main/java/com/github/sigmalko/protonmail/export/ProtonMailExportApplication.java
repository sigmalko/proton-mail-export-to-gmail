package com.github.sigmalko.protonmail.export;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.ApplicationPidFileWriter;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import lombok.extern.slf4j.Slf4j;

@Slf4j(topic = "ProtonMailExportApplication")
@SpringBootApplication
@EnableScheduling
@EnableAsync
@ConfigurationPropertiesScan
public class ProtonMailExportApplication implements WebMvcConfigurer {

    public static void main(String[] args) {
        System.setProperty("file.encoding", "UTF-8");
        System.setProperty("client.encoding.override", "UTF-8");
        System.setProperty("log4j.appender.logfile.encoding", "UTF-8");
        final var app = new SpringApplication(
                ProtonMailExportApplication.class
        );
        app.addListeners(new ApplicationPidFileWriter());
        app.run(args);

        final var out = new StringBuffer();
        out.append("GITHUB_RUN_NUMBER=");
        out.append(System.getenv("GITHUB_RUN_NUMBER"));
        out.append(", DOCKER_TAG_VERSION=");
        out.append(System.getenv("DOCKER_TAG_VERSION")).append("");

        log.info("Application started. Versions: {}", out.toString());
    }
}
