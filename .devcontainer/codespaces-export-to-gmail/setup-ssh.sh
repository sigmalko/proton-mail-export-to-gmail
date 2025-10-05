#!/bin/bash
# set -e
echo "ENV - USER_CODESPACE_PROTONMAILEXPORT_ENV_PRODUCTION_USER: $USER_CODESPACE_PROTONMAILEXPORT_ENV_PRODUCTION_USER"
echo "ENV - USER_CODESPACE_PROTONMAILEXPORT_ENV_PRODUCTION_HOST: $USER_CODESPACE_PROTONMAILEXPORT_ENV_PRODUCTION_HOST"
echo "ENV - USER_CODESPACE_PROTONMAILEXPORT_ENV_PRODUCTION_GMAIL_APP_USERNAME: $USER_CODESPACE_PROTONMAILEXPORT_ENV_PRODUCTION_GMAIL_APP_USERNAME"

required_ssh_secrets=(
  USER_CODESPACE_PROTONMAILEXPORT_ENV_PRODUCTION_USER
  USER_CODESPACE_PROTONMAILEXPORT_ENV_PRODUCTION_HOST
  USER_CODESPACE_PROTONMAILEXPORT_ENV_PRODUCTION_RSA_PRIVATE
  USER_CODESPACE_PROTONMAILEXPORT_ENV_PRODUCTION_RSA_PUBLIC
)

missing_secrets=()
for secret in "${required_ssh_secrets[@]}"; do
  if [ -z "${!secret}" ]; then
    missing_secrets+=("$secret")
  fi
done

if [ ${#missing_secrets[@]} -gt 0 ]; then
  echo "Skipping SSH setup because the following secrets are missing or empty: ${missing_secrets[*]}"
  exit 0
fi

mkdir -p ~/.ssh
chmod 700 ~/.ssh
printf '%s\n' "$USER_CODESPACE_PROTONMAILEXPORT_ENV_PRODUCTION_RSA_PRIVATE" > ~/.ssh/id_rsa
chmod 600 ~/.ssh/id_rsa
printf '%s\n' "$USER_CODESPACE_PROTONMAILEXPORT_ENV_PRODUCTION_RSA_PUBLIC" > ~/.ssh/id_rsa.pub
chmod 600 ~/.ssh/id_rsa.pub
touch ~/.ssh/config
cat <<EOF > ~/.ssh/config
Host prod
        HostName ${USER_CODESPACE_PROTONMAILEXPORT_ENV_PRODUCTION_HOST}
        User ${USER_CODESPACE_PROTONMAILEXPORT_ENV_PRODUCTION_USER}
        IdentityFile ~/.ssh/id_rsa
        IdentitiesOnly yes
EOF
chmod 600 ~/.ssh/config

