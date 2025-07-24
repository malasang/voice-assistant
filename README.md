# 语音助手应用 - 聚合文档

## 🎯 项目概述

这是一个基于Java开发的Android语音识别应用，采用可插拔架构设计，支持多种语音识别引擎。

## ✅ 已完成的工作

### 1. 项目架构设计
- **可插拔架构**: 通过`SpeechRecognizer`接口实现引擎切换
- **MVVM模式**: 使用ViewModel和LiveData进行数据绑定
- **模块化设计**: 音频处理、语音识别、UI展示分离

### 2. 核心功能实现
- **语音识别**: Vosk离线识别 + Google Speech-to-Text在线识别
- **音频处理**: 实时音频录制和处理
- **多语言支持**: 中文、英文、日文、韩文
- **历史记录**: 自动保存识别历史

### 3. 用户界面
- **Material Design**: 现代化UI设计
- **响应式布局**: 适配不同屏幕尺寸
- **状态反馈**: 实时显示识别状态和进度
- **错误处理**: 友好的错误提示

### 4. 技术特性
- **Java开发**: 完全使用Java语言
- **性能优化**: 后台线程处理，内存管理
- **权限管理**: 自动请求必要权限
- **资源管理**: 及时释放系统资源

## 🔍 功能特性

- 🎤 **实时语音识别**: 支持中文、英文、日文、韩文
- 🔄 **可插拔架构**: 轻松切换不同的语音识别引擎
- 📱 **现代化UI**: Material Design风格界面
- 📝 **历史记录**: 保存识别历史，支持清除
- 🔒 **隐私保护**: 支持离线识别模式
- ⚡ **高性能**: 优化的音频处理和识别流程

## 🔧 支持的语音识别引擎

### 1. Vosk (默认)
- ✅ 完全免费开源
- ✅ 支持离线识别
- ✅ 中文识别准确率85-90%
- ✅ 轻量级，模型文件约50MB

### 2. Google Speech-to-Text (示例)
- ✅ 高准确率识别
- ✅ 需要网络连接
- ✅ 每月免费额度
- ✅ 支持更多语言

## 📁 项目架构

```
voice-assistant/
├── app/
│   ├── build.gradle                 # 应用构建配置
│   └── src/main/
│       ├── java/com/voiceassistant/app/
│       │   ├── MainActivity.java    # 主界面
│       │   ├── speech/              # 语音识别模块
│       │   │   ├── SpeechRecognizer.java      # 识别器接口
│       │   │   ├── VoskSpeechRecognizer.java  # Vosk实现
│       │   │   ├── GoogleSpeechRecognizer.java # Google实现
│       │   │   └── SpeechManager.java         # 识别管理器
│       │   ├── audio/               # 音频处理模块
│       │   │   └── AudioRecorder.java         # 音频录制器
│       │   ├── viewmodel/           # 视图模型
│       │   │   └── MainViewModel.java         # 视图模型
│       │   ├── adapter/             # 适配器
│       │   │   └── HistoryAdapter.java        # 历史记录适配器
│       │   └── utils/               # 工具类
│       ├── res/                     # 资源文件
│       │   ├── layout/              # 布局文件
│       │   ├── values/              # 值文件
│       │   └── drawable/            # 图片资源
│       └── AndroidManifest.xml      # 应用清单
├── build.gradle                     # 项目构建配置
├── settings.gradle                  # 项目设置
├── gradle.properties                # Gradle属性
├── gradlew                          # Gradle Wrapper脚本
├── build.sh                         # 快速构建脚本
├── BUILD_GUIDE.md                   # 构建指南
├── README.md                        # 项目说明
└── PROJECT_SUMMARY.md               # 项目总结
```

## 🔌 可插拔架构设计

### 核心接口
```java
public interface SpeechRecognizer {
    boolean initialize(RecognitionListener listener);
    boolean startRecognition();
    void stopRecognition();
    void processAudio(byte[] audioData, int length);
    void setLanguage(String language);
    List<String> getSupportedLanguages();
    String getRecognizerName();
    boolean isOfflineSupported();
    void release();
}
```

### 切换识别引擎
```java
// 在SpeechManager中切换识别器
speechManager.switchRecognizer(new VoskSpeechRecognizer(context));
// 或
speechManager.switchRecognizer(new GoogleSpeechRecognizer(context));
```

## 🚀 构建和部署指南

### 项目状态
✅ 所有源代码文件已创建完成
✅ 项目配置文件已配置
✅ 资源文件已准备就绪
✅ **构建验证成功**: 使用 `gradle assembleDebug` 命令可以成功构建APK

### 环境要求
- Android Studio 4.0+
- Android SDK 21+ (Android 5.0)
- Java 8+

### 构建APK的方法

#### 方法1：使用Android Studio（推荐）

1. **安装Android Studio**
   - 下载地址：https://developer.android.com/studio
   - 选择适合macOS的版本
   - 安装时会自动下载Java和Android SDK

2. **打开项目**
   - 启动Android Studio
   - 选择 "Open an existing project"
   - 选择项目目录：`/Users/malasang/project/voice-assistant`

3. **构建APK**
   - 等待项目同步完成
   - 选择菜单：`Build` → `Build Bundle(s) / APK(s)` → `Build APK(s)`
   - 或者使用快捷键：`Cmd+Shift+F9`

4. **查找APK文件**
   - APK文件位置：`app/build/outputs/apk/debug/app-debug.apk`

#### 方法2：命令行构建（推荐）

如果你已经安装了Java和Android SDK：

```bash
# 进入项目目录
cd /Users/malasang/project/voice-assistant

# 方法2a: 使用Gradle Wrapper（推荐）
./gradlew assembleDebug

# 方法2b: 使用系统Gradle（如果Gradle Wrapper有问题）
gradle assembleDebug

# 查找APK文件
find . -name "*.apk"
```

**✅ 已验证成功**: 使用 `gradle assembleDebug` 命令可以成功构建APK

#### 方法3：安装开发环境

如果系统没有Java和Android SDK：

```bash
# 安装Homebrew（如果没有）
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"

# 安装Java
brew install openjdk@11

# 设置Java环境
echo 'export JAVA_HOME=/opt/homebrew/opt/openjdk@11' >> ~/.zshrc
echo 'export PATH=$JAVA_HOME/bin:$PATH' >> ~/.zshrc
source ~/.zshrc

# 下载Android Command Line Tools
# 访问：https://developer.android.com/studio#command-tools
# 下载并解压到合适位置

# 设置Android环境
echo 'export ANDROID_HOME=/path/to/android-sdk' >> ~/.zshrc
echo 'export PATH=$ANDROID_HOME/tools:$ANDROID_HOME/platform-tools:$PATH' >> ~/.zshrc
source ~/.zshrc
```

## ❓ 常见问题及解决方案

### 1. "识别器未初始化" / "启动识别失败"

**问题描述：**
点击录音按钮时，应用提示"识别器未初始化"或"启动识别失败"。

**原因分析：**
这通常是因为Vosk模型文件没有正确包含在APK中，或者模型文件损坏。

**解决方案：**

#### 方法1：重新构建APK（推荐）
1. 确保模型文件已正确放置在 `app/src/main/assets/models/` 目录中
2. 重新构建APK：`gradle assembleDebug`
3. 重新安装APK到设备

#### 方法2：手动添加模型文件
1. 访问 [Vosk模型下载页面](https://alphacephei.com/vosk/models)
2. 下载中文模型：`vosk-model-small-cn-0.22.zip`
3. 解压到项目的 `app/src/main/assets/models/` 目录
4. 重新构建APK

#### 方法3：检查模型文件完整性
```bash
# 检查模型文件是否存在
ls -la app/src/main/assets/models/vosk-model-small-cn-0.22/

# 检查模型文件大小（应该约50MB）
du -sh app/src/main/assets/models/vosk-model-small-cn-0.22/
```

### 2. 麦克风权限被拒绝

**问题描述：**
应用无法录制音频，提示需要麦克风权限。

**解决方案：**
1. 打开系统设置 → 应用管理 → 语音助手
2. 找到"权限"选项
3. 开启"麦克风"权限
4. 重启应用

### 3. 网络连接问题

**问题描述：**
使用Google Speech-to-Text时网络连接失败。

**解决方案：**
1. 检查网络连接是否稳定
2. 尝试使用WiFi而不是移动数据
3. 切换到离线模式（Vosk）进行识别

### 4. 存储空间不足

**问题描述：**
应用安装或运行时提示存储空间不足。

**解决方案：**
1. 检查设备存储空间，确保有至少100MB可用空间
2. 清理不必要的文件和应用
3. 卸载不需要的应用释放空间

### 5. 应用崩溃

**问题描述：**
应用启动或使用过程中崩溃。

**解决方案：**
1. 清除应用数据和缓存
2. 重新安装应用
3. 检查Android版本是否支持（需要Android 5.0+）

## 📊 调试信息

### 查看日志
```bash
# 查看应用日志
adb logcat | grep "VoiceAssistant"

# 查看Vosk相关日志
adb logcat | grep "VoskSpeechRecognizer"

# 查看音频录制日志
adb logcat | grep "AudioRecorder"
```

### 检查模型文件
```bash
# 检查模型文件是否存在
adb shell ls -la /storage/emulated/0/Android/data/com.voiceassistant.app/files/models/

# 检查模型文件大小
adb shell du -sh /storage/emulated/0/Android/data/com.voiceassistant.app/files/models/"
```

## ⚡ 性能优化建议

1. 使用最新版本的Android Studio和Gradle
2. 确保设备有足够的存储空间和内存
3. 避免在后台运行过多应用
4. 定期清理应用缓存
5. 使用离线模式时，确保模型文件完整且最新