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
echo "#########################################################"