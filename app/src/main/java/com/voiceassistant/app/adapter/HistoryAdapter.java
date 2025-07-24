package com.voiceassistant.app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.voiceassistant.app.R;
import com.voiceassistant.app.viewmodel.MainViewModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 历史记录适配器
 */
public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {
    
    private List<MainViewModel.RecognitionResult> history = new ArrayList<>();
    private SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
    
    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_history, parent, false);
        return new HistoryViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        MainViewModel.RecognitionResult result = history.get(position);
        holder.bind(result);
    }
    
    @Override
    public int getItemCount() {
        return history.size();
    }
    
    /**
     * 设置历史记录数据
     */
    public void setHistory(List<MainViewModel.RecognitionResult> history) {
        this.history = history != null ? history : new ArrayList<>();
        notifyDataSetChanged();
    }
    
    /**
     * 添加新的识别结果
     */
    public void addResult(MainViewModel.RecognitionResult result) {
        history.add(0, result);
        notifyItemInserted(0);
        
        // 限制历史记录数量
        if (history.size() > 100) {
            int removeCount = history.size() - 100;
            for (int i = 0; i < removeCount; i++) {
                history.remove(history.size() - 1);
            }
            notifyItemRangeRemoved(100, removeCount);
        }
    }
    
    /**
     * 清除所有历史记录
     */
    public void clearHistory() {
        int size = history.size();
        history.clear();
        notifyItemRangeRemoved(0, size);
    }
    
    /**
     * ViewHolder
     */
    static class HistoryViewHolder extends RecyclerView.ViewHolder {
        
        private TextView tvText;
        private TextView tvTime;
        private TextView tvLanguage;
        
        public HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvText = itemView.findViewById(R.id.tvText);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvLanguage = itemView.findViewById(R.id.tvLanguage);
        }
        
        public void bind(MainViewModel.RecognitionResult result) {
            tvText.setText(result.getText());
            
            // 格式化时间
            String time = new SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                    .format(new Date(result.getTimestamp()));
            tvTime.setText(time);
            
            // 设置语言标签
            String language = getLanguageDisplayName(result.getLanguage());
            tvLanguage.setText(language);
        }
        
        private String getLanguageDisplayName(String languageCode) {
            switch (languageCode) {
                case "zh-CN":
                    return "中文";
                case "en-US":
                    return "EN";
                case "ja-JP":
                    return "JP";
                case "ko-KR":
                    return "KR";
                default:
                    return languageCode;
            }
        }
    }
} 