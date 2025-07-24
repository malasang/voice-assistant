package com.voiceassistant.app.speech;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;

import java.util.Arrays;
import java.util.List;

/**
 * Google Speech-to-Text识别器实现
 * 这是一个示例实现，展示如何替换语音识别引擎
 */
public class GoogleSpeechRecognizer implements com.voiceassistant.app.speech.SpeechRecognizer {
    
    private static final String TAG = "GoogleSpeechRecognizer";
    
    private Context context;
    private android.speech.SpeechRecognizer speechRecognizer;
    private com.voiceassistant.app.speech.SpeechRecognizer.RecognitionListener listener;
    private boolean isInitialized = false;
    private String currentLanguage = "zh-CN";
    
    // 支持的语言列表
    private static final List<String> SUPPORTED_LANGUAGES = Arrays.asList(
        "zh-CN", "en-US", "ja-JP", "ko-KR"
    );
    
    public GoogleSpeechRecognizer(Context context) {
        this.context = context;
    }
    
    @Override
    public boolean initialize(com.voiceassistant.app.speech.SpeechRecognizer.RecognitionListener listener) {
        this.listener = listener;
        
        try {
            // 检查设备是否支持语音识别
            if (!android.speech.SpeechRecognizer.isRecognitionAvailable(context)) {
                Log.e(TAG, "Speech recognition not available on this device");
                if (listener != null) {
                    listener.onError("设备不支持语音识别");
                }
                return false;
            }
            
            // 创建SpeechRecognizer
            speechRecognizer = android.speech.SpeechRecognizer.createSpeechRecognizer(context);
            speechRecognizer.setRecognitionListener(new android.speech.RecognitionListener() {
                @Override
                public void onReadyForSpeech(Bundle params) {
                    Log.d(TAG, "Ready for speech");
                    if (listener != null) {
                        listener.onRecognitionStarted();
                    }
                }
                
                @Override
                public void onBeginningOfSpeech() {}
                
                @Override
                public void onRmsChanged(float rmsdB) {}
                
                @Override
                public void onBufferReceived(byte[] buffer) {}
                
                @Override
                public void onEndOfSpeech() {}
                
                @Override
                public void onError(int error) {
                    String errorMessage = getErrorMessage(error);
                    Log.e(TAG, "Recognition error: " + errorMessage);
                    if (listener != null) {
                        listener.onError(errorMessage);
                    }
                }
                
                @Override
                public void onResults(Bundle results) {
                    List<String> matches = results.getStringArrayList(android.speech.SpeechRecognizer.RESULTS_RECOGNITION);
                    if (matches != null && !matches.isEmpty()) {
                        String result = matches.get(0);
                        Log.d(TAG, "Final result: " + result);
                        if (listener != null) {
                            listener.onFinalResult(result);
                        }
                    }
                    
                    if (listener != null) {
                        listener.onRecognitionEnded();
                    }
                }
                
                @Override
                public void onPartialResults(Bundle partialResults) {
                    List<String> matches = partialResults.getStringArrayList(android.speech.SpeechRecognizer.RESULTS_RECOGNITION);
                    if (matches != null && !matches.isEmpty()) {
                        String result = matches.get(0);
                        Log.d(TAG, "Partial result: " + result);
                        if (listener != null) {
                            listener.onPartialResult(result);
                        }
                    }
                }
                
                @Override
                public void onEvent(int eventType, Bundle params) {}
            });
            
            isInitialized = true;
            Log.d(TAG, "Google Speech Recognizer initialized successfully");
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize Google Speech Recognizer", e);
            if (listener != null) {
                listener.onError("初始化失败: " + e.getMessage());
            }
            return false;
        }
    }
    
    @Override
    public boolean startRecognition() {
        if (!isInitialized || speechRecognizer == null) {
            Log.e(TAG, "Google Speech Recognizer not initialized");
            if (listener != null) {
                listener.onError("识别器未初始化");
            }
            return false;
        }
        
        try {
            // 创建识别意图
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, currentLanguage);
            intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
            intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
            
            // 开始识别
            speechRecognizer.startListening(intent);
            Log.d(TAG, "Google Speech recognition started");
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to start Google Speech recognition", e);
            if (listener != null) {
                listener.onError("开始识别失败: " + e.getMessage());
            }
            return false;
        }
    }
    
    @Override
    public void stopRecognition() {
        if (speechRecognizer != null) {
            try {
                speechRecognizer.stopListening();
                Log.d(TAG, "Google Speech recognition stopped");
            } catch (Exception e) {
                Log.e(TAG, "Error stopping Google Speech recognition", e);
                if (listener != null) {
                    listener.onError("停止识别失败: " + e.getMessage());
                }
            }
        }
    }
    
    @Override
    public void processAudio(byte[] audioData, int length) {
        // Google Speech-to-Text使用系统API，不需要手动处理音频数据
        Log.w(TAG, "processAudio not supported for Google Speech-to-Text");
    }
    
    @Override
    public void setLanguage(String language) {
        if (!SUPPORTED_LANGUAGES.contains(language)) {
            Log.w(TAG, "Unsupported language: " + language);
            return;
        }
        
        this.currentLanguage = language;
        Log.d(TAG, "Language set to: " + language);
    }
    
    @Override
    public List<String> getSupportedLanguages() {
        return SUPPORTED_LANGUAGES;
    }
    
    @Override
    public String getRecognizerName() {
        return "Google Speech-to-Text";
    }
    
    @Override
    public boolean isOfflineSupported() {
        return false; // Google Speech-to-Text需要网络连接
    }
    
    @Override
    public void release() {
        try {
            if (speechRecognizer != null) {
                speechRecognizer.destroy();
                speechRecognizer = null;
            }
            isInitialized = false;
            Log.d(TAG, "Google Speech Recognizer resources released");
        } catch (Exception e) {
            Log.e(TAG, "Error releasing Google Speech Recognizer resources", e);
        }
    }
    
    /**
     * 获取错误信息
     */
    private String getErrorMessage(int error) {
        switch (error) {
            case SpeechRecognizer.ERROR_AUDIO:
                return "音频错误";
            case SpeechRecognizer.ERROR_CLIENT:
                return "客户端错误";
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                return "权限不足";
            case SpeechRecognizer.ERROR_NETWORK:
                return "网络错误";
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                return "网络超时";
            case SpeechRecognizer.ERROR_NO_MATCH:
                return "无匹配结果";
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                return "识别器忙碌";
            case SpeechRecognizer.ERROR_SERVER:
                return "服务器错误";
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                return "语音超时";
            default:
                return "未知错误";
        }
    }
} 