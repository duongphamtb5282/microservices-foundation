#!/bin/bash

# Memory-optimized startup script for auth-service
# This script addresses the OutOfMemoryError issues

echo "🚀 Starting auth-service with memory optimizations..."

# Set JVM memory settings to prevent OutOfMemoryError
export JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+UseStringDeduplication"

# Set logback properties to reduce memory usage
export LOG_ASYNC_QUEUE_SIZE=32
export LOG_ASYNC_DISCARDING_THRESHOLD=5
export LOG_ASYNC_INCLUDE_CALLER_DATA=false

# Disable debug logging to prevent memory issues
export LOGGING_LEVEL_COM_DEMO_LOGBACK=WARN
export LOGGING_LEVEL_COM_DEMO_LOGBACK_LOGMASKINGPROPERTIES=WARN

# Start the application
echo "📊 Memory settings: $JAVA_OPTS"
echo "📝 Logging settings: Queue size=$LOG_ASYNC_QUEUE_SIZE, Discard threshold=$LOG_ASYNC_DISCARDING_THRESHOLD"

./gradlew bootRun --args="--spring.profiles.active=custom" --no-daemon
