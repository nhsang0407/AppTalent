package com.shoplens.ai.admin;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.android.material.snackbar.Snackbar;
import com.shoplens.ai.R;
import com.shoplens.ai.adapter.SearchTrendAdapter;
import com.shoplens.ai.databinding.ActivitySearchTrendBinding;
import com.shoplens.ai.repository.SearchLogRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SearchTrendActivity extends AppCompatActivity {

    private ActivitySearchTrendBinding binding;
    private final SearchLogRepository repository = new SearchLogRepository();
    private SearchTrendAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySearchTrendBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.toolbar.setNavigationOnClickListener(v -> finish());

        adapter = new SearchTrendAdapter();
        binding.rvTrends.setLayoutManager(new LinearLayoutManager(this));
        binding.rvTrends.setAdapter(adapter);

        setupChart();
        loadTrends();
    }

    private void setupChart() {
        BarChart chart = binding.barChart;
        Description description = new Description();
        description.setEnabled(false);
        chart.setDescription(description);
        chart.getLegend().setEnabled(false);
        chart.setDrawGridBackground(false);
        chart.getAxisRight().setEnabled(false);
        chart.setFitBars(true);

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setLabelRotationAngle(-30f);
    }

    private void loadTrends() {
        binding.progress.setVisibility(View.VISIBLE);
        repository.getTopSearchLabels(10, new SearchLogRepository.SearchStatsCallback() {
            @Override
            public void onSuccess(List<Map.Entry<String, Integer>> topLabels) {
                binding.progress.setVisibility(View.GONE);
                boolean empty = topLabels == null || topLabels.isEmpty();
                binding.llEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
                adapter.submit(topLabels);
                bindChart(topLabels);
            }

            @Override
            public void onError(Exception e) {
                binding.progress.setVisibility(View.GONE);
                Snackbar.make(binding.getRoot(), e.getMessage(), Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void bindChart(List<Map.Entry<String, Integer>> labels) {
        if (labels == null || labels.isEmpty()) {
            binding.barChart.clear();
            return;
        }
        List<BarEntry> entries = new ArrayList<>();
        List<String> xLabels = new ArrayList<>();
        for (int i = 0; i < labels.size(); i++) {
            entries.add(new BarEntry(i, labels.get(i).getValue()));
            xLabels.add(labels.get(i).getKey());
        }

        BarDataSet dataSet = new BarDataSet(entries, "Searches");
        dataSet.setColor(getColor(R.color.primary));
        dataSet.setValueTextColor(getColor(R.color.text_secondary));
        dataSet.setValueTextSize(10f);

        BarData data = new BarData(dataSet);
        data.setBarWidth(0.6f);
        binding.barChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(xLabels));
        binding.barChart.setData(data);
        binding.barChart.invalidate();
        binding.barChart.animateY(600);
    }
}
