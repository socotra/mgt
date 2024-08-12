#!/bin/bash

# Sample script to create a deployable config from (assumed global) plugin code
# and the contents of the socotra-config directory

mkdir config
cp -r ./socotra-config/* ./config/
mkdir -p ./config/plugins/java
cp -r ./src/main/java/com/socotra/deployment/customer/* ./config/plugins/java
cd config; zip -r ../config.zip * 
