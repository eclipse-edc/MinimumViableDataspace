#!/bin/bash

set -euxo pipefail

# Check installed version of Ubuntu
Var=$(lsb_release -r)
NumOnly=$(cut -f2 <<< "$Var")

if [ "$NumOnly" != "20.04" ]; then
  echo "WARNING: This curl upgrade script is only verified on Ubuntu 20.04, please check if you are running this on the correct version of Ubuntu"
#  exit 1
fi

echo "Current installed version of cURL is: $(curl -V)"
echo "Removing old curl..."
apt remove -y curl && apt purge curl
echo "Updating apt cache..."
apt-get update
echo "Installing build-tools..."
apt-get install -y libssl-dev autoconf libtool make
echo "Download & extract curl version: $VERSION"
cd /usr/local/src
rm -rf curl*
wget -v https://curl.haxx.se/download/curl-$VERSION.zip && unzip curl-$VERSION.zip && cd curl-$VERSION
echo "Building & Configuring curl..."
./buildconf && ./configure --with-ssl && make && make install
echo "Moving curl to /usr/bin/curl and linking"
cp /usr/local/bin/curl /usr/bin/curl
ldconfig
echo "New upgraded version of curl is: $(curl -V)"
echo "curl upgraded successfully to version $VERSION!"
