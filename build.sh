#!/bin/bash
set -e

docker rmi gcr.io/{your_gcp_zone}/demystify_network_backend
mvn clean package -Dmaven.test.skip=true
docker buildx build --platform linux/amd64 -t gcr.io/{your_gcp_zone}/demystify_network_backend .
docker push gcr.io/{your_gcp_zone}/demystify_network_backend

