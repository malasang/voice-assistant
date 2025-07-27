package com.voiceassistant.app.speech;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.voiceassistant.app.speech.SpeechRecognizer;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

/**
 * 服务端语音识别器实现
 * 通过WebSocket连接服务端进行流式识别
 */
public class ServerSpeechRecognizer implements SpeechRecognizer {

    private static final String TAG = "ServerSpeechRecognizer";
    private static final String HEALTH_CHECK_URL = "http://127.0.0.1:8000/health";
    private static final String RECOGNIZE_STREAM_URL = "ws://127.0.0.1:8000/recognize/stream";

    private Context context;
    private RecognitionListener listener;
    private boolean isInitialized = false;
    private boolean isRecognizing = false;
    private String currentLanguage = "zh-CN";
    private WebSocket webSocket;
    private OkHttpClient client;
    private ExecutorService executorService;

    // 支持的语言列表
    private static final List<String> SUPPORTED_LANGUAGES = Arrays.asList(
            "zh-CN", "en-US", "ja-JP", "ko-KR"
    );

    public ServerSpeechRecognizer(Context context) {
        this.context = context;
        this.client = new OkHttpClient();
        this.executorService = Executors.newSingleThreadExecutor();
    }

    @Override
    public boolean initialize(RecognitionListener listener) {
        this.listener = listener;

        // 检查服务端是否可用
        boolean isServerAvailable = checkServerHealth();
        if (!isServerAvailable) {
            Log.e(TAG, "Server is not available");
            if (listener != null) {
                listener.onError("服务端不可用，请检查网络连接");
            }
            return false;
        }

        isInitialized = true;
        Log.d(TAG, "ServerSpeechRecognizer initialized successfully");
        return true;
    }

    /**
     * 检查服务端健康状态
     */
    private boolean checkServerHealth() {
        try {
            Request request = new Request.Builder()
                    .url(HEALTH_CHECK_URL)
                    .get()
                    .build();

            Response response = client.newCall(request).execute();
            if (response.isSuccessful() && response.body() != null) {
                String responseBody = response.body().string();
                Log.d(TAG, "Health check response: " + responseBody);
                // 简单检查响应是否包含成功状态
                return responseBody.contains("{\"status\": \"ok\"") ||
                        responseBody.contains("服务正常运行中");
            }
        } catch (IOException e) {
            Log.e(TAG, "Failed to check server health", e);
        }
        return false;
    }

    @Override
    public boolean startRecognition() {
        if (!isInitialized || isRecognizing) {
            return false;
        }

        // 检查服务端是否可用
        boolean isServerAvailable = checkServerHealth();
        if (!isServerAvailable) {
            if (listener != null) {
                listener.onError("服务端不可用，无法开始识别");
            }
            return false;
        }

        // 建立WebSocket连接
        Request request = new Request.Builder()
                .url(RECOGNIZE_STREAM_URL + "?language=" + currentLanguage)
                .build();

        webSocket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(@NonNull WebSocket webSocket, @NonNull Response response) {
                super.onOpen(webSocket, response);
                Log.d(TAG, "WebSocket connection opened");
                isRecognizing = true;
                if (listener != null) {
                    listener.onRecognitionStarted();
                }
            }

            @Override
            public void onMessage(@NonNull WebSocket webSocket, @NonNull String text) {
                super.onMessage(webSocket, text);
                Log.d(TAG, "Received message: " + text);
                // 处理服务端返回的识别结果
                // 假设返回的JSON格式包含type字段，用于区分部分结果和最终结果
                if (text.contains("\"type\": \"partial\"")) {
                    // 提取部分结果
                    String partialResult = extractResult(text, "partialResult");
                    if (listener != null) {
                        listener.onPartialResult(partialResult);
                    }
                } else if (text.contains("\"type\": \"final\"")) {
                    // 提取最终结果
                    String finalResult = extractResult(text, "finalResult");
                    if (listener != null) {
                        listener.onFinalResult(finalResult);
                    }
                }
            }

            @Override
            public void onFailure(@NonNull WebSocket webSocket, @NonNull Throwable t, Response response) {
                super.onFailure(webSocket, t, response);
                Log.e(TAG, "WebSocket failure", t);
                isRecognizing = false;
                if (listener != null) {
                    listener.onError("识别失败: " + t.getMessage());
                    listener.onRecognitionEnded();
                }
            }

            @Override
            public void onClosed(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
                super.onClosed(webSocket, code, reason);
                Log.d(TAG, "WebSocket closed: " + reason);
                isRecognizing = false;
                if (listener != null) {
                    listener.onRecognitionEnded();
                }
            }
        });

        return true;
    }

    /**
     * 从JSON字符串中提取识别结果
     */
    private String extractResult(String json, String fieldName) {
        // 简单的JSON解析，实际项目中可以使用JSON库
        try {
            int startIndex = json.indexOf("\"" + fieldName + "\": ");
            if (startIndex == -1) return "";

            startIndex += ("\"" + fieldName + "\": \"").length();
            int endIndex = json.indexOf("\"", startIndex);
            if (endIndex == -1) return "";

            return json.substring(startIndex, endIndex);
        } catch (Exception e) {
            Log.e(TAG, "Failed to extract result", e);
            return "";
        }
    }

    @Override
    public void stopRecognition() {
        if (webSocket != null) {
            webSocket.close(1000, "Recognition stopped by user");
            webSocket = null;
        }
        isRecognizing = false;
    }

    @Override
    public void processAudio(byte[] audioData, int length) {
        if (isRecognizing && webSocket != null) {
            // 发送音频数据到服务端
            webSocket.send(ByteString.of(audioData, 0, length));
        }
    }

    @Override
    public void setLanguage(String language) {
        if (SUPPORTED_LANGUAGES.contains(language)) {
            this.currentLanguage = language;
            Log.d(TAG, "Language set to: " + language);
        } else {
            Log.e(TAG, "Unsupported language: " + language);
        }
    }

    @Override
    public List<String> getSupportedLanguages() {
        return SUPPORTED_LANGUAGES;
    }

    @Override
    public String getRecognizerName() {
        return "Server Speech Recognizer";
    }

    @Override
    public boolean isOfflineSupported() {
        return false; // 服务端识别需要网络连接
    }

    @Override
    public void release() {
        stopRecognition();
        executorService.shutdown();
        isInitialized = false;
    }
}