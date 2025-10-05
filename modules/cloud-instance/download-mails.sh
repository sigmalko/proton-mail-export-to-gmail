#!/bin/bash
wget https://proton.me/download/export-tool/proton-mail-export-cli-linux_x86_64.tar.gz
pwd
tar -xvzf proton-mail-export-cli-linux_x86_64.tar.gz
mkdir -p /home/ubuntu/proton-cli
tar -zxf proton-mail-export-cli-linux_x86_64.tar.gz -C /home/ubuntu/proton-cli
