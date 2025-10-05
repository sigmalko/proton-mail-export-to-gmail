#!/bin/bash
apt-get update
apt-get install -y maven

mkdir -p ~/.m2
cat > ~/.m2/settings.xml <<EOF
<settings>
  <proxies>
    <proxy>
      <id>codexProxy</id>
      <active>true</active>
      <protocol>http</protocol>
      <host>proxy</host>
      <port>8080</port>
    </proxy>
  </proxies>
</settings>
EOF

cd ..
cd ..
set -e
chmod +x *.sh
# cd modules/export-to-gmail
# mvn clean package 
echo "CODEX SYSTEM IS READY v.1.0"
