#!/bin/bash

# Sample script for Enterprise Core config archive deployment, using a personal access token (PAT)

curl --location "https://api-base-url/config/$1/deployments/deploy" \
--header "Authorization: Bearer $2" \
--form 'file=@"./config.zip"'

