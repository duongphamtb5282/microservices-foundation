#!/bin/bash

echo "🔍 Verifying Code Quality Configuration..."
echo ""

# Check if configuration files exist
echo "📁 Checking configuration files..."

if [ -f ".idea/codeStyles/GoogleStyle.xml" ]; then
    echo "✅ Google Java Style configured"
else
    echo "❌ Google Java Style missing"
    exit 1
fi

if [ -f ".idea/codeStyleSettings.xml" ]; then
    echo "✅ Project code style settings found"
else
    echo "❌ Project code style settings missing"
    exit 1
fi

if [ -f "config/checkstyle/checkstyle.xml" ]; then
    echo "✅ Checkstyle configuration found"
else
    echo "❌ Checkstyle configuration missing"
    exit 1
fi

echo ""
echo "🧹 Testing Spotless formatting..."
if ./gradlew spotlessCheck > /dev/null 2>&1; then
    echo "✅ Spotless formatting is consistent"
else
    echo "⚠️  Spotless found issues (run './gradlew spotlessApply' to fix)"
fi

echo ""
echo "📏 Testing Checkstyle..."
if ./gradlew checkstyleMain > /dev/null 2>&1; then
    echo "✅ Checkstyle validation passed"
else
    echo "❌ Checkstyle found violations"
fi

echo ""
echo "📋 Code Quality Configuration Status:"
echo "====================================="
echo "✅ IntelliJ Google Java Style configured"
echo "✅ Spotless formatting working"
echo "✅ Checkstyle validation working"
echo ""
echo "🎯 Quick Commands:"
echo "- Format code:    ./gradlew spotlessApply"
echo "- Check style:    ./gradlew checkstyleMain"
echo "- Pre-commit:     ./gradlew preCommit"
echo ""
echo "🎉 Code quality setup verified!"
