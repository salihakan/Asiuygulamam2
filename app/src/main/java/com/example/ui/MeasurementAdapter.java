package com.example.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.R;
import com.example.model.MeasurementRecord;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MeasurementAdapter extends RecyclerView.Adapter<MeasurementAdapter.ViewHolder> {

    public interface OnMeasurementListener {
        void onEdit(MeasurementRecord record);
        void onDelete(MeasurementRecord record);
    }

    private final List<MeasurementRecord> items = new ArrayList<>();
    private final OnMeasurementListener listener;

    public MeasurementAdapter(OnMeasurementListener listener) {
        this.listener = listener;
    }

    public void submitList(List<MeasurementRecord> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_measurement, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MeasurementRecord record = items.get(position);
        holder.tvWeight.setText(String.format(Locale.US, "%.1f", record.getWeightKg()));
        holder.tvDate.setText(record.getFormattedDate());

        String details = String.format(Locale.getDefault(),
                "Boy: %.0f cm • VKİ: %.1f (%s)",
                record.getHeightCm(),
                record.getBmi(),
                record.getBmiCategoryLabel());
        holder.tvHeightBmi.setText(details);

        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) listener.onEdit(record);
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDelete(record);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvWeight;
        TextView tvDate;
        TextView tvHeightBmi;
        ImageButton btnEdit;
        ImageButton btnDelete;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvWeight = itemView.findViewById(R.id.item_tv_weight);
            tvDate = itemView.findViewById(R.id.item_tv_date);
            tvHeightBmi = itemView.findViewById(R.id.item_tv_height_bmi);
            btnEdit = itemView.findViewById(R.id.item_btn_edit);
            btnDelete = itemView.findViewById(R.id.item_btn_delete);
        }
    }
}
