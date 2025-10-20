#!/bin/bash
echo "#########################################################"
echo "Starting proton-mail-export-to-gmail application..."
echo "#########################################################"
echo "Environment variables:"
echo "#########################################################"
echo "DOCKER_TAG_VERSION: "$DOCKER_TAG_VERSION
echo "GITHUB_RUN_NUMBER: "$GITHUB_RUN_NUMBER
env
echo "#########################################################"
java -version
pwd
ls -al
ls -al *.jar
java -server -Dspring.profiles.active=prod -Dlogging.config=classpath:logback-prod.xml -jar *.jar -Duser.timezone=$TZ -Djava.security.egd=file:/dev/./urandom