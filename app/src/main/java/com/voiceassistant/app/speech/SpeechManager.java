package com.voiceassistant.app.speech;

import android.content.Context;
import android.util.Log;

import com.voiceassistant.app.audio.AudioRecorder;

/**
 * 语音识别管理器
 * 整合音频录制和语音识别功能
 */
public class SpeechManager {
    
    private static final String TAG = "SpeechManager";
    
    private Context context;
    private AudioRecorder audioRecorder;
    private SpeechRecognizer speechRecognizer;
    private SpeechManagerListener listener;
    
    // 当前状态
    private boolean isInitialized = false;
    private boolean isRecognizing = false;
    
    /**
     * 语音识别管理器监听器
     */
    public interface SpeechManagerListener {
        /**
         * 识别开始
         */
        void onRecognitionStarted();
        
        /**
         * 识别进行中，返回部分结果
         * @param partialResult 部分识别结果
         */
        void onPartialResult(String partialResult);
        
        /**
         * 识别完成，返回最终结果
         * @param finalResult 最终识别结果
         */
        void onFinalResult(String finalResult);
        
        /**
         * 识别错误
         * @param error 错误信息
         */
        void onError(String error);
        
        /**
         * 识别结束
         */
        void onRecognitionEnded();
    }
    
    public SpeechManager(Context context) {
        this.context = context;
        this.audioRecorder = new AudioRecorder();
        this.speechRecognizer = new VoskSpeechRecognizer(context);
        
        // 设置音频数据监听器
        audioRecorder.setAudioDataListener(new AudioRecorder.AudioDataListener() {
            @Override
            public void onAudioData(byte[] audioData, int length) {
                // 将音频数据传递给语音识别器
                if (speechRecognizer != null && isRecognizing) {
                    speechRecognizer.processAudio(audioData, length);
                }
            }
            
            @Override
            public void onError(String error) {
                Log.e(TAG, "Audio recorder error: " + error);
                if (listener != null) {
                    listener.onError("音频录制错误: " + error);
                }
            }
        });
        
        // 设置语音识别监听器
        speechRecognizer.initialize(new SpeechRecognizer.RecognitionListener() {
            @Override
            public void onRecognitionStarted() {
                Log.d(TAG, "Recognition started");
                if (listener != null) {
                    listener.onRecognitionStarted();
                }
            }
            
            @Override
            public void onPartialResult(String partialResult) {
                Log.d(TAG, "Partial result: " + partialResult);
                if (listener != null) {
                    listener.onPartialResult(partialResult);
                }
            }
            
            @Override
            public void onFinalResult(String finalResult) {
                Log.d(TAG, "Final result: " + finalResult);
                if (listener != null) {
                    listener.onFinalResult(finalResult);
                }
            }
            
            @Override
            public void onError(String error) {
                Log.e(TAG, "Recognition error: " + error);
                if (listener != null) {
                    listener.onError("识别错误: " + error);
                }
            }
            
            @Override
            public void onRecognitionEnded() {
                Log.d(TAG, "Recognition ended");
                isRecognizing = false;
                if (listener != null) {
                    listener.onRecognitionEnded();
                }
            }
        });
        
        isInitialized = true;
    }
    
    /**
     * 设置监听器
     */
    public void setListener(SpeechManagerListener listener) {
        this.listener = listener;
    }
    
    /**
     * 开始语音识别
     * @return 是否成功开始
     */
    public boolean startRecognition() {
        if (!isInitialized) {
            Log.e(TAG, "Speech manager not initialized");
            if (listener != null) {
                listener.onError("语音管理器未初始化");
            }
            return false;
        }
        
        if (isRecognizing) {
            Log.w(TAG, "Already recognizing");
            return false;
        }
        
        try {
            // 开始语音识别
            if (!speechRecognizer.startRecognition()) {
                Log.e(TAG, "Failed to start speech recognition");
                return false;
            }
            
            // 开始音频录制
            if (!audioRecorder.startRecording()) {
                Log.e(TAG, "Failed to start audio recording");
                speechRecognizer.stopRecognition();
                return false;
            }
            
            isRecognizing = true;
            Log.d(TAG, "Speech recognition started successfully");
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "Error starting recognition", e);
            if (listener != null) {
                listener.onError("开始识别失败: " + e.getMessage());
            }
            return false;
        }
    }
    
    /**
     * 停止语音识别
     */
    public void stopRecognition() {
        if (!isRecognizing) {
            return;
        }
        
        try {
            // 停止音频录制
            audioRecorder.stopRecording();
            
            // 停止语音识别
            speechRecognizer.stopRecognition();
            
            isRecognizing = false;
            Log.d(TAG, "Speech recognition stopped");
            
        } catch (Exception e) {
            Log.e(TAG, "Error stopping recognition", e);
            if (listener != null) {
                listener.onError("停止识别失败: " + e.getMessage());
            }
        }
    }
    
    /**
     * 检查是否正在识别
     */
    public boolean isRecognizing() {
        return isRecognizing;
    }
    
    /**
     * 设置识别语言
     * @param language 语言代码
     */
    public void setLanguage(String language) {
        if (speechRecognizer != null) {
            speechRecognizer.setLanguage(language);
        }
    }
    
    /**
     * 获取支持的语言列表
     */
    public java.util.List<String> getSupportedLanguages() {
        if (speechRecognizer != null) {
            return speechRecognizer.getSupportedLanguages();
        }
        return java.util.Collections.emptyList();
    }
    
    /**
     * 获取当前识别器名称
     */
    public String getRecognizerName() {
        if (speechRecognizer != null) {
            return speechRecognizer.getRecognizerName();
        }
        return "Unknown";
    }
    
    /**
     * 检查是否支持离线识别
     */
    public boolean isOfflineSupported() {
        if (speechRecognizer != null) {
            return speechRecognizer.isOfflineSupported();
        }
        return false;
    }
    
    /**
     * 切换语音识别器
     * @param recognizer 新的语音识别器
     */
    public void switchRecognizer(SpeechRecognizer recognizer) {
        if (isRecognizing) {
            stopRecognition();
        }
        
        if (speechRecognizer != null) {
            speechRecognizer.release();
        }
        
        this.speechRecognizer = recognizer;
        
        // 重新初始化
        if (recognizer != null) {
            recognizer.initialize(new SpeechRecognizer.RecognitionListener() {
                @Override
                public void onRecognitionStarted() {
                    if (listener != null) {
                        listener.onRecognitionStarted();
                    }
                }
                
                @Override
                public void onPartialResult(String partialResult) {
                    if (listener != null) {
                        listener.onPartialResult(partialResult);
                    }
                }
                
                @Override
                public void onFinalResult(String finalResult) {
                    if (listener != null) {
                        listener.onFinalResult(finalResult);
                    }
                }
                
                @Override
                public void onError(String error) {
                    if (listener != null) {
                        listener.onError(error);
                    }
                }
                
                @Override
                public void onRecognitionEnded() {
                    isRecognizing = false;
                    if (listener != null) {
                        listener.onRecognitionEnded();
                    }
                }
            });
        }
        
        Log.d(TAG, "Switched to recognizer: " + (recognizer != null ? recognizer.getRecognizerName() : "null"));
    }
    
    /**
     * 释放资源
     */
    public void release() {
        stopRecognition();
        
        if (audioRecorder != null) {
            audioRecorder.release();
            audioRecorder = null;
        }
        
        if (speechRecognizer != null) {
            speechRecognizer.release();
            speechRecognizer = null;
        }
        
        isInitialized = false;
        Log.d(TAG, "Speech manager released");
    }
} 