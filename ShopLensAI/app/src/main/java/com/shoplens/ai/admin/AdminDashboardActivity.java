package com.shoplens.ai.admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.shoplens.ai.R;
import com.shoplens.ai.adapter.OrderAdapter;
import com.shoplens.ai.adapter.ProductAdapter;
import com.shoplens.ai.databinding.ActivityAdminDashboardBinding;
import com.shoplens.ai.model.Order;
import com.shoplens.ai.utils.Constants;
import com.shoplens.ai.viewmodel.AdminDashboardViewModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AdminDashboardActivity extends AppCompatActivity {

    private ActivityAdminDashboardBinding binding;
    private AdminDashboardViewModel viewModel;
    private OrderAdapter recentAdapter;
    private ProductAdapter lowStockAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAdminDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(AdminDashboardViewModel.class);

        setupStatCards();
        setupLists();
        setupChart();
        setupBottomNav();
        observeViewModel();

        viewModel.loadDashboard();
    }

    private void setupStatCards() {
        binding.cardRevenue.tvLabel.setText(R.string.total_revenue);
        binding.cardRevenue.ivIcon.setImageResource(R.drawable.ic_trending);
        binding.cardOrders.tvLabel.setText(R.string.total_orders);
        binding.cardOrders.ivIcon.setImageResource(R.drawable.ic_receipt);
        binding.cardPending.tvLabel.setText(R.string.pending_orders);
        binding.cardPending.ivIcon.setImageResource(R.drawable.ic_notifications);
        binding.cardLowstock.tvLabel.setText(R.string.low_stock_count);
        binding.cardLowstock.ivIcon.setImageResource(R.drawable.ic_inventory);
    }

    private void setupLists() {
        recentAdapter = new OrderAdapter(false, new OrderAdapter.OnOrderActionListener() {
            @Override
            public void onStatusChange(Order order, String newStatus) {
            }

            @Override
            public void onOrderClick(Order order) {
                startActivity(new Intent(AdminDashboardActivity.this, OrderManagementActivity.class));
            }
        });
        binding.rvRecentOrders.setLayoutManager(new LinearLayoutManager(this));
        binding.rvRecentOrders.setAdapter(recentAdapter);

        lowStockAdapter = new ProductAdapter(true, false, product ->
                startActivity(new Intent(this, BarcodeStockActivity.class)));
        binding.rvLowStock.setLayoutManager(new LinearLayoutManager(this));
        binding.rvLowStock.setAdapter(lowStockAdapter);
    }

    private void setupChart() {
        LineChart chart = binding.lineChart;
        Description description = new Description();
        description.setEnabled(false);
        chart.setDescription(description);
        chart.getLegend().setEnabled(false);
        chart.setDrawGridBackground(false);
        chart.getAxisRight().setEnabled(false);
        chart.setTouchEnabled(false);

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
    }

    private void setupBottomNav() {
        binding.bottomNav.setSelectedItemId(R.id.nav_dashboard);
        binding.bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_dashboard) {
                return true;
            } else if (id == R.id.nav_products) {
                startActivity(new Intent(this, ProductManagementActivity.class));
                return false;
            } else if (id == R.id.nav_orders_admin) {
                startActivity(new Intent(this, OrderManagementActivity.class));
                return false;
            } else if (id == R.id.nav_trends) {
                startActivity(new Intent(this, SearchTrendActivity.class));
                return false;
            }
            return false;
        });
    }

    private void observeViewModel() {
        viewModel.getIsLoading().observe(this, loading ->
                binding.progress.setVisibility(loading ? View.VISIBLE : View.GONE));

        viewModel.getDashboardStats().observe(this, this::bindStats);

        viewModel.getRecentOrders().observe(this, orders -> {
            recentAdapter.submit(orders);
            binding.tvNoRecent.setVisibility(
                    (orders == null || orders.isEmpty()) ? View.VISIBLE : View.GONE);
        });

        viewModel.getLowStockProducts().observe(this, products -> {
            lowStockAdapter.submit(products);
            bindLowStockCount(products == null ? 0 : products.size());
            binding.tvNoLowStock.setVisibility(
                    (products == null || products.isEmpty()) ? View.VISIBLE : View.GONE);
        });

        viewModel.getAllOrders().observe(this, this::bindChart);
    }

    private void bindStats(Map<String, Object> stats) {
        if (stats == null) {
            return;
        }
        double todayRevenue = asDouble(stats.get("todayRevenue"));
        int totalOrders = asInt(stats.get("totalOrders"));
        int pending = asInt(stats.get("pendingOrders"));
        binding.cardRevenue.tvValue.setText(String.format(Locale.getDefault(),
                getString(R.string.price_format), todayRevenue));
        binding.cardOrders.tvValue.setText(String.valueOf(totalOrders));
        binding.cardPending.tvValue.setText(String.valueOf(pending));
        // Low stock value is set in low-stock observer below for accuracy.
    }

    private void bindChart(List<Order> orders) {
        // Build last-7-days revenue buckets.
        double[] revenue = new double[7];
        String[] labels = new String[7];
        SimpleDateFormat dayFormat = new SimpleDateFormat("EEE", Locale.getDefault());

        Calendar cal = Calendar.getInstance();
        long[] dayStarts = new long[8];
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        // dayStarts[6] = today start, [0] = 6 days ago
        for (int i = 6; i >= 0; i--) {
            dayStarts[i] = cal.getTimeInMillis();
            labels[i] = dayFormat.format(cal.getTime());
            cal.add(Calendar.DAY_OF_YEAR, -1);
        }
        dayStarts[7] = Long.MAX_VALUE; // not used; guard

        if (orders != null) {
            for (Order o : orders) {
                if (o.getCreatedAt() == null
                        || Constants.STATUS_CANCELLED.equals(o.getStatus())) {
                    continue;
                }
                long t = o.getCreatedAt().toDate().getTime();
                for (int i = 0; i < 7; i++) {
                    long start = dayStarts[i];
                    long end = (i < 6) ? dayStarts[i + 1] : Long.MAX_VALUE;
                    if (t >= start && t < end) {
                        revenue[i] += o.getTotalPrice();
                        break;
                    }
                }
            }
        }

        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            entries.add(new Entry(i, (float) revenue[i]));
        }

        LineDataSet dataSet = new LineDataSet(entries, "Revenue");
        dataSet.setColor(getColor(R.color.primary));
        dataSet.setCircleColor(getColor(R.color.primary));
        dataSet.setLineWidth(2.5f);
        dataSet.setCircleRadius(4f);
        dataSet.setDrawValues(false);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(getColor(R.color.primary_light));
        dataSet.setFillAlpha(60);

        binding.lineChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        binding.lineChart.setData(new LineData(dataSet));
        binding.lineChart.invalidate();
        binding.lineChart.animateX(600);
    }

    private void bindLowStockCount(int count) {
        binding.cardLowstock.tvValue.setText(String.valueOf(count));
    }

    private static double asDouble(Object o) {
        return (o instanceof Number) ? ((Number) o).doubleValue() : 0.0;
    }

    private static int asInt(Object o) {
        return (o instanceof Number) ? ((Number) o).intValue() : 0;
    }

    @Override
    protected void onResume() {
        super.onResume();
        binding.bottomNav.setSelectedItemId(R.id.nav_dashboard);
        viewModel.loadDashboard();
    }
}
