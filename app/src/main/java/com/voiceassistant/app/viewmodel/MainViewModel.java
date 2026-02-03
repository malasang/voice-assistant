package com.voiceassistant.app.viewmodel;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.voiceassistant.app.speech.SpeechManager;

import java.util.ArrayList;
import java.util.List;

/**
 * 主界面ViewModel
 */
public class MainViewModel extends AndroidViewModel {
    
    private static final String TAG = "MainViewModel";
    
    private SpeechManager speechManager;
    
    // LiveData for UI updates
    private MutableLiveData<Boolean> isRecognizing = new MutableLiveData<>(false);
    private MutableLiveData<String> currentText = new MutableLiveData<>("");
    private MutableLiveData<String> partialText = new MutableLiveData<>("");
    private MutableLiveData<String> errorMessage = new MutableLiveData<>("");
    private MutableLiveData<List<RecognitionResult>> recognitionHistory = new MutableLiveData<>(new ArrayList<>());
    private MutableLiveData<String> currentLanguage = new MutableLiveData<>("zh-CN");
    private MutableLiveData<String> recognizerName = new MutableLiveData<>("");
    // 是否应持续保持识别（由按钮切换控制）
    private volatile boolean shouldContinueRecognition = false;
    // 会话缓冲区：聚合一次会话内的所有最终结果
    private StringBuilder sessionBuffer = new StringBuilder();
    private final Object sessionBufferLock = new Object();
    
    // 超时检测
    private static final long NO_SPEECH_TIMEOUT_MS = 30000; // 30秒无语音输入超时
    private Handler timeoutHandler;
    private Runnable timeoutRunnable;

    /**
     * 识别结果数据类
     */
    public static class RecognitionResult {
        private String text;
        private long timestamp;
        private String language;
        
        public RecognitionResult(String text, String language) {
            this.text = text;
            this.timestamp = System.currentTimeMillis();
            this.language = language;
        }
        
        public String getText() {
            return text;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
        
        public String getLanguage() {
            return language;
        }
    }
    
    /**
     * 为识别结果添加标点符号
     * @param text 原始识别文本
     * @param language 语言代码
     * @return 带标点符号的文本
     */
    private String addPunctuation(String text, String language) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        // 如果文本已经包含标点符号，则不处理
        if (hasPunctuation(text)) {
            return text;
        }

        // 根据语言添加不同的标点符号
        if ("zh-CN".equals(language)) {
            // 中文标点符号
            return addChinesePunctuation(text);
        } else {
            // 英文标点符号
            return addEnglishPunctuation(text);
        }
    }

    /**
     * 检查文本是否已经包含标点符号
     */
    private boolean hasPunctuation(String text) {
        String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            return false;
        }

        // 检查是否以标点符号结尾
        char lastChar = trimmed.charAt(trimmed.length() - 1);
        String punctuation = "。！？.!?，,；;：:";
        return punctuation.indexOf(lastChar) >= 0;
    }

    /**
     * 为中文文本添加标点符号
     */
    private String addChinesePunctuation(String text) {
        String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            return text;
        }

        // 简单的标点符号添加规则
        // 问句：包含疑问词
        String[] questionWords = {"吗", "呢", "什么", "哪里", "谁", "为什么", "怎么", "多少", "几", "哪"};
        for (String word : questionWords) {
            if (trimmed.contains(word)) {
                return trimmed + "？";
            }
        }

        // 感叹句：包含感叹词
        String[] exclamationWords = {"啊", "呀", "哦", "哇", "嘿", "好", "太", "真", "非常"};
        for (String word : exclamationWords) {
            if (trimmed.contains(word)) {
                return trimmed + "！";
            }
        }

        // 默认使用句号
        return trimmed + "。";
    }

    /**
     * 为英文文本添加标点符号
     */
    private String addEnglishPunctuation(String text) {
        String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            return text;
        }

        String lowerText = trimmed.toLowerCase();

        // 问句：包含疑问词
        String[] questionWords = {"what", "where", "who", "why", "how", "when", "which", "whom"};
        for (String word : questionWords) {
            if (lowerText.contains(word)) {
                // 首字母大写
                return capitalizeFirstLetter(trimmed) + "?";
            }
        }

        // 感叹句：包含感叹词或强烈的语气词
        String[] exclamationWords = {"wow", "hey", "great", "amazing", "awesome", "so", "really", "very"};
        for (String word : exclamationWords) {
            if (lowerText.contains(word)) {
                return capitalizeFirstLetter(trimmed) + "!";
            }
        }

        // 默认使用句号
        return capitalizeFirstLetter(trimmed) + ".";
    }

    /**
     * 将文本首字母大写
     */
    private String capitalizeFirstLetter(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return text.substring(0, 1).toUpperCase() + text.substring(1);
    }

    public MainViewModel(@NonNull Application application) {
        super(application);
        
        // 初始化超时检测Handler
        timeoutHandler = new Handler(Looper.getMainLooper());
        timeoutRunnable = new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "No speech timeout reached, stopping recognition");

                // 在停止识别前，将当前识别结果保存到历史记录
                String currentResult = currentText.getValue();
                if (currentResult != null && !currentResult.isEmpty() &&
                    !currentResult.equals("点击下方按钮开始语音识别...")) {
                    // 添加到历史记录
                    String aggregated;
                    synchronized (sessionBufferLock) {
                        aggregated = sessionBuffer.toString();
                        sessionBuffer.setLength(0);
                    }
                    if (!aggregated.isEmpty()) {
                        addToHistory(aggregated);
                    }
                }

                stopRecognition();

                // 清空识别结果
                currentText.postValue("点击下方按钮开始语音识别...");
                partialText.postValue("");

                errorMessage.postValue("30秒无语音输入，已自动停止识别");
            }
        };

        // 初始化语音管理器
        speechManager = new SpeechManager(application);
        speechManager.setListener(new SpeechManager.SpeechManagerListener() {
            @Override
            public void onRecognitionStarted() {
                Log.d(TAG, "Recognition started");
                isRecognizing.postValue(true);
                partialText.postValue("");
                errorMessage.postValue("");
                // 启动超时检测
                startTimeoutDetection();
            }
            
            @Override
            public void onPartialResult(String partialResult) {
                Log.d(TAG, "Partial result: " + partialResult);
                partialText.postValue(partialResult);
                // 重置超时检测
                resetTimeoutDetection();
            }
            
            @Override
            public void onFinalResult(String finalResult) {
                Log.d(TAG, "Final result: " + finalResult);
                
                // 获取当前语言
                String language = currentLanguage.getValue();

                // 添加标点符号
                String processedText = addPunctuation(finalResult, language);

                // 重置超时检测
                resetTimeoutDetection();

                // 追加到当前文本
                String existing = currentText.getValue();
                if (existing == null || existing.isEmpty()) {
                    currentText.postValue(processedText);
                } else {
                    currentText.postValue(existing + "\n" + processedText);
                }
                partialText.postValue("");
                
                // 追加到会话缓冲（使用处理后的文本）
                synchronized (sessionBufferLock) {
                    if (sessionBuffer.length() > 0) {
                        sessionBuffer.append('\n');
                    }
                    sessionBuffer.append(processedText);
                }
            }
            
            @Override
            public void onError(String error) {
                Log.e(TAG, "Recognition error: " + error);
                errorMessage.postValue(error);
                // 取消超时检测
                stopTimeoutDetection();
                // 使用postValue确保在主线程中执行
                isRecognizing.postValue(false);
                // 在主线程中停止识别
                new android.os.Handler(android.os.Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        stopRecognition();
                    }
                });
            }
            
            @Override
            public void onRecognitionEnded() {
                Log.d(TAG, "Recognition ended");
                isRecognizing.postValue(false);
                // 取消超时检测
                stopTimeoutDetection();
                // 若仍需持续识别，则自动重启
                if (shouldContinueRecognition) {
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            startRecognition();
                        }
                    });
                } else {
                    // 会话结束，若有内容则一次性写入历史
                    String aggregated;
                    synchronized (sessionBufferLock) {
                        aggregated = sessionBuffer.toString();
                        sessionBuffer.setLength(0);
                    }
                    if (!aggregated.isEmpty()) {
                        addToHistory(aggregated);
                    }
                }
            }
        });
        
        // 设置初始值
        recognizerName.postValue(speechManager.getRecognizerName());
    }
    
    /**
     * 启动超时检测
     */
    private void startTimeoutDetection() {
        if (timeoutHandler != null && timeoutRunnable != null) {
            timeoutHandler.removeCallbacks(timeoutRunnable);
            timeoutHandler.postDelayed(timeoutRunnable, NO_SPEECH_TIMEOUT_MS);
            Log.d(TAG, "Timeout detection started");
        }
    }

    /**
     * 重置超时检测
     */
    private void resetTimeoutDetection() {
        if (timeoutHandler != null && timeoutRunnable != null) {
            timeoutHandler.removeCallbacks(timeoutRunnable);
            timeoutHandler.postDelayed(timeoutRunnable, NO_SPEECH_TIMEOUT_MS);
            Log.d(TAG, "Timeout detection reset");
        }
    }

    /**
     * 停止超时检测
     */
    private void stopTimeoutDetection() {
        if (timeoutHandler != null && timeoutRunnable != null) {
            timeoutHandler.removeCallbacks(timeoutRunnable);
            Log.d(TAG, "Timeout detection stopped");
        }
    }

    /**
     * 开始语音识别
     */
    public void startRecognition() {
        boolean wasContinuing = shouldContinueRecognition;
        shouldContinueRecognition = true;
        // 仅在用户新发起会话时清空显示并重置缓冲
        if (!wasContinuing) {
            currentText.postValue("");
            partialText.postValue("");
            synchronized (sessionBufferLock) {
                sessionBuffer.setLength(0);
            }
        }
        if (speechManager != null) {
            boolean success = speechManager.startRecognition();
            if (!success) {
                errorMessage.postValue("启动识别失败");
            }
        }
    }
    
    /**
     * 停止语音识别
     */
    public void stopRecognition() {
        shouldContinueRecognition = false;
        stopTimeoutDetection();
        if (speechManager != null) {
            speechManager.stopRecognition();
        }
    }
    
    /**
     * 切换识别语言
     */
    public void setLanguage(String language) {
        if (speechManager != null) {
            speechManager.setLanguage(language);
            currentLanguage.postValue(language);
        }
    }
    
    /**
     * 清除当前文本
     */
    public void clearCurrentText() {
        currentText.postValue("");
        partialText.postValue("");
    }
    
    /**
     * 清除历史记录
     */
    public void clearHistory() {
        recognitionHistory.postValue(new ArrayList<>());
    }
    
    /**
     * 添加识别结果到历史记录
     */
    private void addToHistory(String text) {
        List<RecognitionResult> history = recognitionHistory.getValue();
        if (history == null) {
            history = new ArrayList<>();
        }
        
        RecognitionResult result = new RecognitionResult(text, currentLanguage.getValue());
        history.add(0, result); // 添加到开头
        
        // 限制历史记录数量
        if (history.size() > 100) {
            history = history.subList(0, 100);
        }
        
        recognitionHistory.postValue(history);
    }
    
    /**
     * 获取支持的语言列表
     */
    public List<String> getSupportedLanguages() {
        if (speechManager != null) {
            return speechManager.getSupportedLanguages();
        }
        return new ArrayList<>();
    }
    
    /**
     * 检查是否支持离线识别
     */
    public boolean isOfflineSupported() {
        return speechManager != null && speechManager.isOfflineSupported();
    }
    
    // Getters for LiveData
    public LiveData<Boolean> getIsRecognizing() {
        return isRecognizing;
    }
    
    public LiveData<String> getCurrentText() {
        return currentText;
    }
    
    public LiveData<String> getPartialText() {
        return partialText;
    }
    
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }
    
    public LiveData<List<RecognitionResult>> getRecognitionHistory() {
        return recognitionHistory;
    }
    
    public LiveData<String> getCurrentLanguage() {
        return currentLanguage;
    }
    
    public LiveData<String> getRecognizerName() {
        return recognizerName;
    }
    
    @Override
    protected void onCleared() {
        super.onCleared();
        
        // 释放资源
        stopTimeoutDetection();
        if (timeoutHandler != null) {
            timeoutHandler.removeCallbacksAndMessages(null);
            timeoutHandler = null;
        }
        if (speechManager != null) {
            speechManager.release();
            speechManager = null;
        }
        
        Log.d(TAG, "MainViewModel cleared");
    }
} 