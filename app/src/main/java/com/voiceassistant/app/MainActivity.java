package com.voiceassistant.app;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.voiceassistant.app.adapter.HistoryAdapter;
import com.voiceassistant.app.viewmodel.MainViewModel;

/**
 * 主Activity
 */
public class MainActivity extends AppCompatActivity {
    
    private static final int PERMISSION_REQUEST_CODE = 1001;
    
    private MainViewModel viewModel;
    private HistoryAdapter historyAdapter;
    
    // UI组件
    private TextView tvCurrentText;
    private TextView tvPartialText;
    private TextView tvStatus;
    private TextView tvLanguage;
    private TextView tvError;
    private MaterialButton btnRecord;
    private MaterialButton btnClearHistory;
    private RecyclerView rvHistory;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // 初始化ViewModel
        viewModel = new ViewModelProvider(this).get(MainViewModel.class);
        
        // 初始化UI
        initViews();
        setupObservers();
        setupClickListeners();
        
        // 检查权限
        checkPermissions();
    }
    
    /**
     * 初始化UI组件
     */
    private void initViews() {
        tvCurrentText = findViewById(R.id.tvCurrentText);
        tvPartialText = findViewById(R.id.tvPartialText);
        tvStatus = findViewById(R.id.tvStatus);
        tvLanguage = findViewById(R.id.tvLanguage);
        tvError = findViewById(R.id.tvError);
        btnRecord = findViewById(R.id.btnRecord);
        btnClearHistory = findViewById(R.id.btnClearHistory);
        rvHistory = findViewById(R.id.rvHistory);
        
        // 设置RecyclerView
        historyAdapter = new HistoryAdapter();
        rvHistory.setLayoutManager(new LinearLayoutManager(this));
        rvHistory.setAdapter(historyAdapter);
    }
    
    /**
     * 设置观察者
     */
    private void setupObservers() {
        // 观察识别状态
        viewModel.getIsRecognizing().observe(this, isRecognizing -> {
            updateRecordButton(isRecognizing);
            updateStatus(isRecognizing);
        });
        
        // 观察当前文本
        viewModel.getCurrentText().observe(this, text -> {
            tvCurrentText.setText(text.isEmpty() ? "点击下方按钮开始语音识别..." : text);
        });
        
        // 观察部分文本
        viewModel.getPartialText().observe(this, text -> {
            if (text != null && !text.isEmpty()) {
                tvPartialText.setText("正在识别: " + text);
                tvPartialText.setVisibility(View.VISIBLE);
            } else {
                tvPartialText.setVisibility(View.GONE);
            }
        });
        
        // 观察错误信息
        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                tvError.setText(error);
                tvError.setVisibility(View.VISIBLE);
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            } else {
                tvError.setVisibility(View.GONE);
            }
        });
        
        // 观察历史记录
        viewModel.getRecognitionHistory().observe(this, history -> {
            historyAdapter.setHistory(history);
        });
        
        // 观察当前语言
        viewModel.getCurrentLanguage().observe(this, language -> {
            String languageText = getLanguageDisplayName(language);
            tvLanguage.setText(languageText);
        });
        
        // 观察识别器名称
        viewModel.getRecognizerName().observe(this, name -> {
            // 可以在这里显示识别器信息
        });
    }
    
    /**
     * 设置点击监听器
     */
    private void setupClickListeners() {
        // 录音按钮
        btnRecord.setOnClickListener(v -> {
            if (viewModel.getIsRecognizing().getValue() != null && 
                viewModel.getIsRecognizing().getValue()) {
                // 正在识别，停止识别
                viewModel.stopRecognition();
            } else {
                // 开始识别
                viewModel.startRecognition();
            }
        });
        
        // 清除历史按钮
        btnClearHistory.setOnClickListener(v -> {
            viewModel.clearHistory();
            Toast.makeText(this, "历史记录已清除", Toast.LENGTH_SHORT).show();
        });
    }
    
    /**
     * 更新录音按钮状态
     */
    private void updateRecordButton(boolean isRecognizing) {
        if (isRecognizing) {
            btnRecord.setIcon(getDrawable(R.drawable.ic_stop));
            btnRecord.setBackgroundTintList(getColorStateList(R.color.error));
        } else {
            btnRecord.setIcon(getDrawable(R.drawable.ic_mic));
            btnRecord.setBackgroundTintList(getColorStateList(R.color.primary));
        }
    }
    
    /**
     * 更新状态显示
     */
    private void updateStatus(boolean isRecognizing) {
        if (isRecognizing) {
            tvStatus.setText("正在识别...");
        } else {
            tvStatus.setText("准备就绪");
        }
    }
    
    /**
     * 获取语言显示名称
     */
    private String getLanguageDisplayName(String languageCode) {
        switch (languageCode) {
            case "zh-CN":
                return "中文";
            case "en-US":
                return "English";
            case "ja-JP":
                return "日本語";
            case "ko-KR":
                return "한국어";
            default:
                return languageCode;
        }
    }
    
    /**
     * 检查权限
     */
    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) 
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, 
                new String[]{Manifest.permission.RECORD_AUDIO}, 
                PERMISSION_REQUEST_CODE);
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, 
                                         @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "麦克风权限已授予", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "需要麦克风权限才能使用语音识别功能", Toast.LENGTH_LONG).show();
            }
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // ViewModel会自动清理资源
    }
} 