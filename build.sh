#!/bin/bash

# 语音助手APK构建脚本

echo "🎤 语音助手 - APK构建脚本"
echo "================================"

# 检查Java环境
echo "📋 检查Java环境..."
if command -v java &> /dev/null; then
    echo "✅ Java已安装: $(java -version 2>&1 | head -n 1)"
else
    echo "❌ Java未安装，请先安装Java"
    echo "   推荐使用Android Studio自动安装"
    exit 1
fi

# 检查Android SDK
echo "📋 检查Android SDK..."
if [ -n "$ANDROID_HOME" ]; then
    echo "✅ ANDROID_HOME已设置: $ANDROID_HOME"
elif command -v adb &> /dev/null; then
    echo "✅ Android SDK已安装"
else
    echo "⚠️  Android SDK未检测到，但可以尝试构建"
fi

# 检查Gradle Wrapper
echo "📋 检查Gradle Wrapper..."
if [ -f "./gradlew" ]; then
    echo "✅ Gradle Wrapper文件存在"
    chmod +x gradlew
else
    echo "❌ Gradle Wrapper不存在"
    echo "💡 将使用系统Gradle"
fi

# 清理之前的构建
echo "🧹 清理之前的构建..."
if [ -f "./gradlew" ]; then
    ./gradlew clean 2>/dev/null || {
        echo "⚠️  Gradle Wrapper有问题，使用系统Gradle清理..."
        gradle clean
    }
else
    gradle clean
fi

# 构建APK
echo "🔨 开始构建APK..."

# 尝试使用Gradle Wrapper，失败时自动切换到系统Gradle
if [ -f "./gradlew" ]; then
    echo "📦 尝试使用Gradle Wrapper构建..."
    ./gradlew assembleDebug 2>/dev/null
    BUILD_RESULT=$?
    if [ $BUILD_RESULT -ne 0 ]; then
        echo "⚠️  Gradle Wrapper失败，切换到系统Gradle..."
        gradle assembleDebug
        BUILD_RESULT=$?
    fi
else
    echo "📦 使用系统Gradle构建..."
    gradle assembleDebug
    BUILD_RESULT=$?
fi

# 检查构建结果
if [ $BUILD_RESULT -eq 0 ]; then
    echo "✅ 构建成功！"
    
    # 查找APK文件
    APK_PATH=$(find . -name "app-debug.apk" 2>/dev/null)
    if [ -n "$APK_PATH" ]; then
        echo "📱 APK文件位置: $APK_PATH"
        echo "📏 文件大小: $(ls -lh "$APK_PATH" | awk '{print $5}')"
        
        # 已禁用APK复制到根目录的操作
        echo "📱 APK文件位置: $APK_PATH"
    else
        echo "⚠️  未找到APK文件"
    fi
else
    echo "❌ 构建失败"
    echo "💡 建议使用Android Studio打开项目进行构建"
    exit 1
fi

echo ""
echo "🎉 构建完成！"
echo "📱 你可以将APK安装到Android设备上测试"
echo "💡 首次运行需要下载Vosk模型文件"
echo ""
echo "📝 构建说明："
echo "   - 推荐使用: gradle assembleDebug"
echo "   - APK位置: app/build/outputs/apk/debug/app-debug.apk"
echo "   - 已验证成功构建 ✅"