#!/bin/bash
# Load environment variables from .env.dev
set -a
source <(grep -v "^#" .env.dev | grep -v "^$" | sed "s/=/=\"/;s/$/\"/")
set +a

# Set the required DB_PASSWORD
export DB_PASSWORD=auth_password

echo "Environment variables loaded:"
env | grep -E "(SPRING|DB_|SERVER_|KEYCLOAK)" | head -10

# Run the application
./gradlew bootRun --args="--spring.profiles.active=dev"
