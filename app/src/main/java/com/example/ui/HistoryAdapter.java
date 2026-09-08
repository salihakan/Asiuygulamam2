package com.example.ui;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.example.R;
import com.example.model.InjectionPlan;
import com.example.util.DoseCalculator;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    public interface OnHistoryToggleListener {
        void onToggle(InjectionPlan plan);
    }

    private final List<InjectionPlan> items = new ArrayList<>();
    private final OnHistoryToggleListener listener;
    private final String todayKey;

    public HistoryAdapter(OnHistoryToggleListener listener) {
        this.listener = listener;
        this.todayKey = DoseCalculator.formatDateKey(Calendar.getInstance());
    }

    public void submitList(List<InjectionPlan> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        InjectionPlan plan = items.get(position);
        Context context = holder.itemView.getContext();

        holder.tvDate.setText(plan.getFormattedDate());
        holder.ivIcon.setImageResource(plan.getLimb().getIconResId());

        String limbName = plan.getLimb().getLocalizedName(context);
        String details = String.format(Locale.getDefault(), "%.1f ünite • %s", plan.getDose(), limbName);
        holder.tvDetails.setText(details);

        boolean isToday = plan.getDateKey().equals(todayKey);

        if (plan.isCompleted()) {
            holder.tvStatus.setText("Yapıldı ✓");
            holder.tvStatus.setBackgroundResource(R.drawable.bg_badge_done);
            holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.on_success));
        } else if (isToday) {
            holder.tvStatus.setText("Bekliyor");
            holder.tvStatus.setBackgroundResource(R.drawable.bg_badge_pending);
            holder.tvStatus.setTextColor(Color.parseColor("#92400E"));
        } else {
            // Past day and not completed
            holder.tvStatus.setText("Atlandı");
            holder.tvStatus.setBackgroundResource(R.drawable.bg_badge_missed);
            holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.danger));
        }

        holder.btnToggle.setOnClickListener(v -> {
            if (listener != null) listener.onToggle(plan);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivIcon;
        TextView tvDate;
        TextView tvDetails;
        TextView tvStatus;
        Button btnToggle;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.history_item_icon);
            tvDate = itemView.findViewById(R.id.history_item_tv_date);
            tvDetails = itemView.findViewById(R.id.history_item_tv_details);
            tvStatus = itemView.findViewById(R.id.history_item_tv_status);
            btnToggle = itemView.findViewById(R.id.history_item_btn_toggle);
        }
    }
}
