#!/bin/sh
# Docker entrypoint: start the game server with configurable options
# Usage: docker run -p 9090:9090 actiongame-server
#        docker run -e SERVER_PORT=8080 -p 8080:8080 actiongame-server

set -e

: "${SERVER_PORT:=9090}"
: "${METRICS_PORT:=9091}"
: "${JAVA_OPTS:=-Xms256m -Xmx512m -XX:+UseG1GC -XX:MaxGCPauseMillis=50}"
: "${NODE_ID:=local}"

echo "=========================================="
echo "  ActionGameDemo Server (Docker)"
echo "=========================================="
echo "  Node:       ${NODE_ID}"
echo "  Port:       ${SERVER_PORT}"
echo "  Metrics:    ${METRICS_PORT}"
echo "  Java Opts:  ${JAVA_OPTS}"
echo "  Started:    $(date)"
echo "=========================================="

exec java ${JAVA_OPTS} -jar app.jar "${SERVER_PORT}"
