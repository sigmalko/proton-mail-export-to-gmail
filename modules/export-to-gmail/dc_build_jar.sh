#!/bin/bash

export GMAIL_IMAP_FETCH_ENABLED="false"
export EML_READER_ENABLED="true"

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

