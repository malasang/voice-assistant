package com.voiceassistant.app.speech;

import android.content.Context;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.vosk.Model;
import org.vosk.Recognizer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.List;

/**
 * Vosk语音识别器实现
 */
public class VoskSpeechRecognizer implements SpeechRecognizer {
    
    private static final String TAG = "VoskSpeechRecognizer";
    
    private Context context;
    private Model model;
    private Recognizer recognizer;
    private RecognitionListener listener;
    private boolean isInitialized = false;
    private String currentLanguage = "zh-CN";
    
    // 支持的语言列表
    private static final List<String> SUPPORTED_LANGUAGES = Arrays.asList(
        "zh-CN", "en-US", "ja-JP", "ko-KR"
    );
    
    public VoskSpeechRecognizer(Context context) {
        this.context = context;
    }
    
    @Override
    public boolean initialize(RecognitionListener listener) {
        this.listener = listener;
        
        try {
            // 首先检查assets中的文件
            Log.d(TAG, "=== Assets Check ===");
            String modelName = getModelName(currentLanguage);
            String assetPath = "models/" + modelName;
            
            try {
                String[] assetFiles = context.getAssets().list(assetPath);
                Log.d(TAG, "Assets path: " + assetPath);
                Log.d(TAG, "Assets files count: " + (assetFiles != null ? assetFiles.length : "null"));
                
                if (assetFiles != null) {
                    for (String file : assetFiles) {
                        Log.d(TAG, "  - " + file);
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "Error listing assets", e);
            }
            Log.d(TAG, "=== End Assets Check ===");
            
            // 从assets中复制模型文件到内部存储
            String modelPath = getModelPath(currentLanguage);
            if (modelPath == null) {
                Log.e(TAG, "Model not found for language: " + currentLanguage);
                if (listener != null) {
                    listener.onError("模型文件未找到，请确保模型文件已包含在APK中");
                }
                return false;
            }
            
            // 验证模型文件完整性
            if (!validateModelFiles(modelPath)) {
                Log.e(TAG, "Model files validation failed for path: " + modelPath);
                if (listener != null) {
                    listener.onError("模型文件验证失败，请重新安装应用");
                }
                return false;
            }
            
            Log.d(TAG, "Creating Vosk model from path: " + modelPath);
            model = new Model(modelPath);
            recognizer = new Recognizer(model, 16000.0f);
            isInitialized = true;
            
            Log.d(TAG, "Vosk initialized successfully with language: " + currentLanguage);
            return true;
            
        } catch (IOException e) {
            Log.e(TAG, "Failed to initialize Vosk", e);
            String errorMsg = "初始化失败: " + e.getMessage();
            if (e.getMessage() != null && e.getMessage().contains("Failed to create a model")) {
                errorMsg = "模型创建失败，请检查模型文件完整性";
            }
            if (listener != null) {
                listener.onError(errorMsg);
            }
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error during Vosk initialization", e);
            if (listener != null) {
                listener.onError("初始化过程中发生未知错误: " + e.getMessage());
            }
            return false;
        }
    }
    
    @Override
    public boolean startRecognition() {
        if (!isInitialized) {
            Log.e(TAG, "Vosk not initialized");
            if (listener != null) {
                listener.onError("识别器未初始化");
            }
            return false;
        }
        
        try {
            // 重置识别器
            recognizer = new Recognizer(model, 16000.0f);
            
            if (listener != null) {
                listener.onRecognitionStarted();
            }
            
            Log.d(TAG, "Vosk recognition started");
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to start recognition", e);
            if (listener != null) {
                listener.onError("开始识别失败: " + e.getMessage());
            }
            return false;
        }
    }
    
    @Override
    public void stopRecognition() {
        if (recognizer != null) {
            try {
                // 获取最终结果
                String result = recognizer.getFinalResult();
                if (listener != null && result != null && !result.isEmpty()) {
                    String text = extractTextFromResult(result);
                    if (text != null && !text.isEmpty()) {
                        listener.onFinalResult(text);
                    }
                }
                
                listener.onRecognitionEnded();
                Log.d(TAG, "Vosk recognition stopped");
                
            } catch (Exception e) {
                Log.e(TAG, "Error stopping recognition", e);
                if (listener != null) {
                    listener.onError("停止识别失败: " + e.getMessage());
                }
            }
        }
    }
    
    @Override
    public void processAudio(byte[] audioData, int length) {
        if (!isInitialized || recognizer == null) {
            return;
        }
        
        try {
            // 处理音频数据
            if (recognizer.acceptWaveForm(audioData, length)) {
                // 获取最终结果
                String result = recognizer.getResult();
                String text = extractTextFromResult(result);
                if (text != null && !text.isEmpty() && listener != null) {
                    listener.onFinalResult(text);
                }
            } else {
                // 获取部分结果
                String partialResult = recognizer.getPartialResult();
                String text = extractTextFromResult(partialResult);
                if (text != null && !text.isEmpty() && listener != null) {
                    listener.onPartialResult(text);
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error processing audio", e);
            if (listener != null) {
                listener.onError("音频处理失败: " + e.getMessage());
            }
        }
    }
    
    @Override
    public void setLanguage(String language) {
        if (!SUPPORTED_LANGUAGES.contains(language)) {
            Log.w(TAG, "Unsupported language: " + language);
            return;
        }
        
        this.currentLanguage = language;
        
        // 如果已经初始化，需要重新初始化以加载新语言模型
        if (isInitialized) {
            release();
            initialize(listener);
        }
    }
    
    @Override
    public List<String> getSupportedLanguages() {
        return SUPPORTED_LANGUAGES;
    }
    
    @Override
    public String getRecognizerName() {
        return "Vosk";
    }
    
    @Override
    public boolean isOfflineSupported() {
        return true;
    }
    
    @Override
    public void release() {
        try {
            if (recognizer != null) {
                recognizer.close();
                recognizer = null;
            }
            if (model != null) {
                model.close();
                model = null;
            }
            isInitialized = false;
            Log.d(TAG, "Vosk resources released");
        } catch (Exception e) {
            Log.e(TAG, "Error releasing Vosk resources", e);
        }
    }
    
    /**
     * 获取模型路径
     */
    private String getModelPath(String language) {
        // 从assets中复制模型文件到内部存储
        String modelName = getModelName(language);
        String internalPath = context.getFilesDir() + "/models/" + modelName;
        
        Log.d(TAG, "=== Model Path Debug Info ===");
        Log.d(TAG, "Language: " + language);
        Log.d(TAG, "Model Name: " + modelName);
        Log.d(TAG, "Internal Path: " + internalPath);
        Log.d(TAG, "Context Files Dir: " + context.getFilesDir());
        
        // 检查内部存储中是否已有模型文件
        File modelDir = new File(internalPath);
        Log.d(TAG, "Model directory exists: " + modelDir.exists());
        Log.d(TAG, "Model directory is directory: " + modelDir.isDirectory());
        
        if (modelDir.exists()) {
            Log.d(TAG, "Model directory size: " + modelDir.length() + " bytes");
            String[] files = modelDir.list();
            if (files != null) {
                Log.d(TAG, "Files in model directory: " + files.length);
                for (String file : files) {
                    File f = new File(modelDir, file);
                    Log.d(TAG, "  - " + file + " (dir: " + f.isDirectory() + ", size: " + f.length() + ")");
                }
            }
        }
        
        if (!modelDir.exists()) {
            Log.d(TAG, "Model directory does not exist, copying from assets...");
            
            // 直接测试assets文件访问
            try {
                String assetPath = "models/" + modelName;
                String[] assetFiles = context.getAssets().list(assetPath);
                Log.d(TAG, "Assets files found: " + (assetFiles != null ? assetFiles.length : "null"));
                
                if (assetFiles == null || assetFiles.length == 0) {
                    Log.e(TAG, "No files found in assets: " + assetPath);
                    return null;
                }
                
                // 测试访问一个具体文件
                String testFile = assetPath + "/am/final.mdl";
                try (InputStream is = context.getAssets().open(testFile)) {
                    int size = is.available();
                    Log.d(TAG, "Test file accessible: " + testFile + ", size: " + size);
                } catch (IOException e) {
                    Log.e(TAG, "Cannot access test file: " + testFile, e);
                    return null;
                }
                
            } catch (IOException e) {
                Log.e(TAG, "Error accessing assets", e);
                return null;
            }
            
            // 从assets复制模型文件
            if (!copyModelFromAssets(modelName, internalPath)) {
                Log.e(TAG, "Failed to copy model from assets: " + modelName);
                return null;
            }
        }
        
        // 再次检查复制后的状态
        if (modelDir.exists()) {
            Log.d(TAG, "After copy - Model directory exists: " + modelDir.exists());
            Log.d(TAG, "After copy - Model directory size: " + modelDir.length() + " bytes");
            
            // 检查关键文件是否存在
            File amFile = new File(modelDir, "am/final.mdl");
            File grFile = new File(modelDir, "graph/Gr.fst");
            Log.d(TAG, "AM file exists: " + amFile.exists() + ", size: " + amFile.length());
            Log.d(TAG, "Graph file exists: " + grFile.exists() + ", size: " + grFile.length());
        }
        
        Log.d(TAG, "=== End Model Path Debug Info ===");
        
        return internalPath;
    }
    
    /**
     * 获取模型名称
     */
    private String getModelName(String language) {
        switch (language) {
            case "zh-CN":
                return "vosk-model-small-cn-0.22";
            case "en-US":
                return "vosk-model-small-en-us-0.15";
            case "ja-JP":
                return "vosk-model-small-ja-0.22";
            case "ko-KR":
                return "vosk-model-small-ko-0.22";
            default:
                return "vosk-model-small-cn-0.22"; // 默认中文
        }
    }
    
    /**
     * 兼容所有Android的assets递归复制方法
     */
    private boolean copyModelFromAssets(String modelName, String targetPath) {
        try {
            String assetRoot = "models/" + modelName;
            List<String> allFiles = listAllAssetFiles(assetRoot);
            Log.d(TAG, "[CopyModel] 需要复制的文件数: " + allFiles.size());
            for (String assetFile : allFiles) {
                String relativePath = assetFile.substring(assetRoot.length() + 1); // 去掉前缀
                String targetFile = targetPath + "/" + relativePath;
                File target = new File(targetFile);
                File parent = target.getParentFile();
                if (!parent.exists()) parent.mkdirs();
                Log.d(TAG, "[CopyModel] 复制: " + assetFile + " -> " + targetFile);
                copyAssetFile(assetFile, targetFile);
            }
            return true;
        } catch (Exception e) {
            Log.e(TAG, "[CopyModel] Error copying model from assets", e);
            return false;
        }
    }

    /**
     * 列出assets目录下所有文件（包括子目录，返回全路径）
     */
    private List<String> listAllAssetFiles(String path) throws IOException {
        List<String> fileList = new ArrayList<>();
        String[] files = context.getAssets().list(path);
        if (files == null || files.length == 0) {
            // 是文件
            fileList.add(path);
        } else {
            // 是目录
            for (String file : files) {
                fileList.addAll(listAllAssetFiles(path + "/" + file));
            }
        }
        return fileList;
    }
    
    /**
     * 复制单个asset文件
     */
    private void copyAssetFile(String assetPath, String targetPath) throws IOException {
        Log.d(TAG, "Copying asset file: " + assetPath + " to " + targetPath);
        
        File targetFile = new File(targetPath);
        File parentDir = targetFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            boolean created = parentDir.mkdirs();
            Log.d(TAG, "Created parent directory: " + parentDir.getAbsolutePath() + " - success: " + created);
        }
        
        try (InputStream inputStream = context.getAssets().open(assetPath);
             FileOutputStream outputStream = new FileOutputStream(targetFile)) {
            
            // 获取文件大小
            int available = inputStream.available();
            Log.d(TAG, "Asset file size: " + available + " bytes");
            
            byte[] buffer = new byte[8192];
            int bytesRead;
            int totalBytesRead = 0;
            
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
                totalBytesRead += bytesRead;
            }
            
            // 验证复制结果
            long targetFileSize = targetFile.length();
            Log.d(TAG, "File copied successfully. Expected: " + available + " bytes, Actual: " + targetFileSize + " bytes");
            
            if (targetFileSize != available) {
                Log.e(TAG, "File size mismatch! Expected: " + available + ", Got: " + targetFileSize);
                throw new IOException("File size mismatch after copy");
            }
            
            Log.d(TAG, "File copy completed successfully: " + targetPath);
        } catch (IOException e) {
            Log.e(TAG, "Error copying file: " + assetPath, e);
            // 删除可能部分复制的文件
            if (targetFile.exists()) {
                targetFile.delete();
            }
            throw e;
        }
    }
    
    /**
     * 从JSON结果中提取文本
     */
    private String extractTextFromResult(String jsonResult) {
        try {
            JSONObject json = new JSONObject(jsonResult);
            return json.optString("text", "");
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing JSON result", e);
            return null;
        }
    }
    
    /**
     * 验证模型文件完整性
     */
    private boolean validateModelFiles(String modelPath) {
        try {
            File modelDir = new File(modelPath);
            if (!modelDir.exists() || !modelDir.isDirectory()) {
                Log.e(TAG, "Model directory does not exist: " + modelPath);
                return false;
            }
            
            Log.d(TAG, "Validating model files in: " + modelPath);
            
            // 检查必要的模型文件 - 只检查Vosk真正需要的文件
            String[] requiredFiles = {
                "am/final.mdl",
                "graph/Gr.fst", 
                "graph/HCLr.fst",
                "conf/mfcc.conf",
                "conf/model.conf"
            };
            
            for (String requiredFile : requiredFiles) {
                File file = new File(modelDir, requiredFile);
                Log.d(TAG, "Checking required file: " + requiredFile + " - exists: " + file.exists() + ", size: " + file.length());
                
                if (!file.exists()) {
                    Log.e(TAG, "Required model file missing: " + requiredFile);
                    return false;
                }
                if (file.length() == 0) {
                    Log.e(TAG, "Model file is empty: " + requiredFile);
                    return false;
                }
            }
            
            // 检查模型文件大小
            File amFile = new File(modelDir, "am/final.mdl");
            File grFile = new File(modelDir, "graph/Gr.fst");
            
            if (amFile.length() < 1000000) { // 小于1MB
                Log.e(TAG, "AM model file too small: " + amFile.length() + " bytes");
                return false;
            }
            
            if (grFile.length() < 1000000) { // 小于1MB
                Log.e(TAG, "Graph file too small: " + grFile.length() + " bytes");
                return false;
            }
            
            // 列出所有文件用于调试
            Log.d(TAG, "All files in model directory:");
            listAllFiles(modelDir, "");
            
            Log.d(TAG, "Model files validation passed. AM: " + amFile.length() + 
                      " bytes, Graph: " + grFile.length() + " bytes");
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "Error validating model files", e);
            return false;
        }
    }
    
    /**
     * 递归列出目录中的所有文件
     */
    private void listAllFiles(File dir, String prefix) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                String fileInfo = prefix + file.getName() + " (dir: " + file.isDirectory() + ", size: " + file.length() + ")";
                Log.d(TAG, "  " + fileInfo);
                if (file.isDirectory()) {
                    listAllFiles(file, prefix + "  ");
                }
            }
        }
    }
} 