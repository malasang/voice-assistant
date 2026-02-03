#!/bin/bash

# 构建并自动对齐APK以支持16KB页面大小的脚本

echo "开始构建..."
gradle clean

if [ $? -ne 0 ]; then
    echo "清理失败！"
    exit 1
fi

echo "构建Debug APK..."
gradle assembleDebug

if [ $? -ne 0 ]; then
    echo "构建失败！"
    exit 1
fi

echo "对齐native库以支持16KB页面大小..."
python3 align_native_libs.py app/build/outputs/apk/debug/app-debug.apk

if [ $? -ne 0 ]; then
    echo "对齐失败！"
    exit 1
fi

echo "========================================"
echo "构建完成！"
echo "原始APK: app/build/outputs/apk/debug/app-debug.apk"
echo "对齐APK: app/build/outputs/apk/debug/app-debug_aligned.apk"
echo ""
echo "请使用 app-debug_aligned.apk 进行安装，该APK已支持16KB页面大小。"
echo "========================================"

