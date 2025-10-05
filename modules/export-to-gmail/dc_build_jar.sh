#!/bin/bash
{
    mvn clean package
} || {
    echo "ERROR COMPILING"
    exit -1
}

{
    mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Dspring.profiles.active=dev -Dlogging.config=classpath:logback-dev.xml"
} || {
    echo "ERROR SPRING BOOT RUN"
    exit -1
}

