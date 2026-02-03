package com.voiceassistant.app.audio;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 音频录制管理器
 */
public class AudioRecorder {
    
    private static final String TAG = "AudioRecorder";
    
    // 音频配置
    private static final int SAMPLE_RATE = 16000; // 16kHz
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    
    private AudioRecord audioRecord;
    private Thread recordingThread;
    private AtomicBoolean isRecording = new AtomicBoolean(false);
    private AudioDataListener audioDataListener;
    
    // 缓冲区大小
    private int bufferSize;
    
    /**
     * 音频数据监听器
     */
    public interface AudioDataListener {
        /**
         * 接收到音频数据
         * @param audioData 音频数据
         * @param length 数据长度
         */
        void onAudioData(byte[] audioData, int length);
        
        /**
         * 录制错误
         * @param error 错误信息
         */
        void onError(String error);
    }
    
    public AudioRecorder() {
        // 计算缓冲区大小
        bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
        if (bufferSize == AudioRecord.ERROR_BAD_VALUE || bufferSize == AudioRecord.ERROR) {
            bufferSize = SAMPLE_RATE * 2; // 默认2秒缓冲区
        }
    }
    
    /**
     * 设置音频数据监听器
     */
    public void setAudioDataListener(AudioDataListener listener) {
        this.audioDataListener = listener;
    }
    
    /**
     * 开始录制
     * @return 是否成功开始
     */
    public boolean startRecording() {
        if (isRecording.get()) {
            Log.w(TAG, "Already recording");
            return false;
        }
        
        try {
            // 如果AudioRecord不存在或已释放，则创建新的
            if (audioRecord == null || audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                // 创建AudioRecord
                audioRecord = new AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT,
                    bufferSize
                );
            
                if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                    Log.e(TAG, "AudioRecord initialization failed");
                    if (audioDataListener != null) {
                        audioDataListener.onError("音频录制器初始化失败");
                    }
                    audioRecord = null;
                    return false;
                }
            }
            
            // 开始录制
            audioRecord.startRecording();
            isRecording.set(true);
            
            // 启动录制线程
            startRecordingThread();
            
            Log.d(TAG, "Audio recording started");
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to start recording", e);
            if (audioDataListener != null) {
                audioDataListener.onError("开始录制失败: " + e.getMessage());
            }
            return false;
        }
    }
    
    /**
     * 停止录制
     */
    public void stopRecording() {
        if (!isRecording.get()) {
            return;
        }
        
        isRecording.set(false);
        
        try {
            // 停止录制线程
            if (recordingThread != null) {
                recordingThread.interrupt();
                recordingThread.join(1000); // 等待线程结束
                recordingThread = null;
            }
            
            // 停止AudioRecord但不释放，准备复用
            if (audioRecord != null) {
                audioRecord.stop();
                // 不释放，保留实例用于下次复用
                Log.d(TAG, "AudioRecord stopped (instance kept for reuse)");
            }
            
            Log.d(TAG, "Audio recording stopped");
            
        } catch (Exception e) {
            Log.e(TAG, "Error stopping recording", e);
        }
    }
    
    /**
     * 启动录制线程
     */
    private void startRecordingThread() {
        recordingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                byte[] buffer = new byte[bufferSize];
                
                while (isRecording.get() && !Thread.currentThread().isInterrupted()) {
                    try {
                        // 读取音频数据
                        int readSize = audioRecord.read(buffer, 0, buffer.length);
                        
                        if (readSize > 0 && audioDataListener != null) {
                            // 创建数据副本
                            byte[] audioData = new byte[readSize];
                            System.arraycopy(buffer, 0, audioData, 0, readSize);
                            
                            // 回调音频数据
                            audioDataListener.onAudioData(audioData, readSize);
                        } else if (readSize == AudioRecord.ERROR_INVALID_OPERATION) {
                            Log.e(TAG, "AudioRecord read error: ERROR_INVALID_OPERATION");
                            break;
                        } else if (readSize == AudioRecord.ERROR_BAD_VALUE) {
                            Log.e(TAG, "AudioRecord read error: ERROR_BAD_VALUE");
                            break;
                        }
                        
                    } catch (Exception e) {
                        Log.e(TAG, "Error reading audio data", e);
                        if (audioDataListener != null) {
                            audioDataListener.onError("读取音频数据失败: " + e.getMessage());
                        }
                        break;
                    }
                }
            }
        });
        
        recordingThread.start();
    }
    
    /**
     * 检查是否正在录制
     */
    public boolean isRecording() {
        return isRecording.get();
    }
    
    /**
     * 获取采样率
     */
    public int getSampleRate() {
        return SAMPLE_RATE;
    }
    
    /**
     * 获取缓冲区大小
     */
    public int getBufferSize() {
        return bufferSize;
    }
    
    /**
     * 释放资源
     */
    public void release() {
        stopRecording();

        // 真正释放AudioRecord资源
        if (audioRecord != null) {
            try {
                if (audioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
                    audioRecord.release();
                    Log.d(TAG, "AudioRecord released");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error releasing AudioRecord", e);
            } finally {
                audioRecord = null;
            }
        }
    }
} 