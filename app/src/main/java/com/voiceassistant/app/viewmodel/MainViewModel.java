package com.voiceassistant.app.viewmodel;

import android.app.Application;
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
    
    public MainViewModel(@NonNull Application application) {
        super(application);
        
        // 初始化语音管理器
        speechManager = new SpeechManager(application);
        speechManager.setListener(new SpeechManager.SpeechManagerListener() {
            @Override
            public void onRecognitionStarted() {
                Log.d(TAG, "Recognition started");
                isRecognizing.postValue(true);
                partialText.postValue("");
                errorMessage.postValue("");
            }
            
            @Override
            public void onPartialResult(String partialResult) {
                Log.d(TAG, "Partial result: " + partialResult);
                partialText.postValue(partialResult);
            }
            
            @Override
            public void onFinalResult(String finalResult) {
                Log.d(TAG, "Final result: " + finalResult);
                
                // 更新当前文本
                currentText.postValue(finalResult);
                partialText.postValue("");
                
                // 添加到历史记录
                addToHistory(finalResult);
                
                // 停止识别
                stopRecognition();
            }
            
            @Override
            public void onError(String error) {
                Log.e(TAG, "Recognition error: " + error);
                errorMessage.postValue(error);
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
            }
        });
        
        // 设置初始值
        recognizerName.postValue(speechManager.getRecognizerName());
    }
    
    /**
     * 开始语音识别
     */
    public void startRecognition() {
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
        if (speechManager != null) {
            speechManager.release();
            speechManager = null;
        }
        
        Log.d(TAG, "MainViewModel cleared");
    }
} 