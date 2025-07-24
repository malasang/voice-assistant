package com.voiceassistant.app.speech;

import java.util.List;

/**
 * 语音识别器接口 - 实现可插拔架构
 * 所有语音识别实现都需要实现此接口
 */
public interface SpeechRecognizer {
    
    /**
     * 识别结果回调接口
     */
    interface RecognitionListener {
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
    
    /**
     * 初始化识别器
     * @param listener 识别结果监听器
     * @return 是否初始化成功
     */
    boolean initialize(RecognitionListener listener);
    
    /**
     * 开始识别
     * @return 是否开始成功
     */
    boolean startRecognition();
    
    /**
     * 停止识别
     */
    void stopRecognition();
    
    /**
     * 处理音频数据
     * @param audioData 音频数据
     * @param length 数据长度
     */
    void processAudio(byte[] audioData, int length);
    
    /**
     * 设置识别语言
     * @param language 语言代码，如"zh-CN", "en-US"
     */
    void setLanguage(String language);
    
    /**
     * 获取支持的语言列表
     * @return 支持的语言列表
     */
    List<String> getSupportedLanguages();
    
    /**
     * 获取识别器名称
     * @return 识别器名称
     */
    String getRecognizerName();
    
    /**
     * 检查是否支持离线识别
     * @return 是否支持离线
     */
    boolean isOfflineSupported();
    
    /**
     * 释放资源
     */
    void release();
} 