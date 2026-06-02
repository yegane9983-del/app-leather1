package com.fifers.leathercalculator;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Locale;

public class MonthlyResultsActivity extends Activity {

    private static final String PREF_NAME = "leather_calculator_prefs";
    private static final String KEY_CALCULATIONS = "calculation_records";
    private static final String[] FILTER_MONTHS = {
            "همه ماه‌ها", "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
            "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"
    };

    private final ArrayList<CalculationRecord> allRecords = new ArrayList<>();
    private final ArrayList<CalculationRecord> visibleRecords = new ArrayList<>();
    private final ArrayList<String> visibleLines = new ArrayList<>();

    private SharedPreferences preferences;
    private Spinner filterMonthSpinner;
    private ListView recordsListView;
    private TextView emptyTextView;
    private ArrayAdapter<String> recordsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_monthly_results);

        preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        filterMonthSpinner = findViewById(R.id.filterMonthSpinner);
        recordsListView = findViewById(R.id.recordsListView);
        emptyTextView = findViewById(R.id.emptyTextView);
        Button backButton = findViewById(R.id.backButton);
        Button clearRecordsButton = findViewById(R.id.clearRecordsButton);

        ArrayAdapter<String> monthAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, FILTER_MONTHS);
        monthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        filterMonthSpinner.setAdapter(monthAdapter);

        recordsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, visibleLines);
        recordsListView.setAdapter(recordsAdapter);

        filterMonthSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                refreshVisibleRecords();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
                refreshVisibleRecords();
            }
        });
        recordsListView.setOnItemLongClickListener((parent, view, position, id) -> {
            CalculationRecord record = visibleRecords.get(position);
            confirmDeleteRecord(record);
            return true;
        });
        backButton.setOnClickListener(view -> finish());
        clearRecordsButton.setOnClickListener(view -> confirmClearRecords());

        loadRecords();
        refreshVisibleRecords();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (preferences != null) {
            loadRecords();
            refreshVisibleRecords();
        }
    }

    private void loadRecords() {
        allRecords.clear();
        String json = preferences.getString(KEY_CALCULATIONS, "[]");
        try {
            JSONArray array = new JSONArray(json);
            for (int i = array.length() - 1; i >= 0; i--) {
                JSONObject object = array.getJSONObject(i);
                String month = object.optString("month", "");
                String productName = object.optString("productName", "");
                if (month.isEmpty() || productName.isEmpty()) continue;
                allRecords.add(new CalculationRecord(
                        object.optLong("createdAt", i),
                        month,
                        productName,
                        object.optDouble("quantity", 0),
                        object.optDouble("actualLeather", 0),
                        object.optDouble("expectedLeather", 0),
                        object.optDouble("difference", 0),
                        object.optString("status", "")
                ));
            }
        } catch (JSONException ignored) {
            Toast.makeText(this, "خواندن سوابق با خطا مواجه شد.", Toast.LENGTH_SHORT).show();
        }
    }

    private void refreshVisibleRecords() {
        if (filterMonthSpinner == null || recordsAdapter == null) return;
        String selectedMonth = String.valueOf(filterMonthSpinner.getSelectedItem());
        visibleRecords.clear();
        visibleLines.clear();
        for (CalculationRecord record : allRecords) {
            if ("همه ماه‌ها".equals(selectedMonth) || selectedMonth.equals(record.month)) {
                visibleRecords.add(record);
                visibleLines.add(recordLine(record));
            }
        }
        recordsAdapter.notifyDataSetChanged();
        emptyTextView.setVisibility(visibleRecords.isEmpty() ? View.VISIBLE : View.GONE);
        recordsListView.setVisibility(visibleRecords.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private String recordLine(CalculationRecord record) {
        return "ماه: " + record.month + " | محصول: " + record.productName + "\n"
                + "تعداد: " + formatNumber(record.quantity)
                + " | واقعی: " + formatNumber(record.actualLeather) + " پا"
                + " | استاندارد: " + formatNumber(record.expectedLeather) + " پا\n"
                + "مابه‌التفاوت: " + formatSignedNumber(record.difference) + " پا"
                + " | وضعیت: " + record.status;
    }

    private void confirmDeleteRecord(CalculationRecord record) {
        new AlertDialog.Builder(this)
                .setTitle("حذف سابقه")
                .setMessage("محاسبه محصول «" + record.productName + "» در ماه «" + record.month + "» حذف شود؟")
                .setPositiveButton("حذف", (dialog, which) -> {
                    removeRecord(record.id);
                    Toast.makeText(this, "سابقه حذف شد.", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("لغو", null)
                .show();
    }

    private void confirmClearRecords() {
        if (allRecords.isEmpty()) {
            Toast.makeText(this, "سابقه‌ای برای حذف وجود ندارد.", Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("پاک کردن سوابق")
                .setMessage("تمام سوابق محاسبات حذف شوند؟")
                .setPositiveButton("حذف همه", (dialog, which) -> {
                    preferences.edit().remove(KEY_CALCULATIONS).apply();
                    loadRecords();
                    refreshVisibleRecords();
                    Toast.makeText(this, "تمام سوابق حذف شد.", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("لغو", null)
                .show();
    }

    private void removeRecord(long id) {
        String json = preferences.getString(KEY_CALCULATIONS, "[]");
        JSONArray remaining = new JSONArray();
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.getJSONObject(i);
                if (object.optLong("createdAt", -1) != id) {
                    remaining.put(object);
                }
            }
            preferences.edit().putString(KEY_CALCULATIONS, remaining.toString()).apply();
        } catch (JSONException ignored) {
            Toast.makeText(this, "حذف سابقه انجام نشد.", Toast.LENGTH_SHORT).show();
        }
        loadRecords();
        refreshVisibleRecords();
    }

    private String formatNumber(double value) {
        DecimalFormat decimalFormat = (DecimalFormat) DecimalFormat.getInstance(Locale.US);
        decimalFormat.applyPattern("#,##0.##");
        return toPersianDigits(decimalFormat.format(value));
    }

    private String formatSignedNumber(double value) {
        String sign = value > 0 ? "+" : "";
        return sign + formatNumber(value);
    }

    private String toPersianDigits(String value) {
        return value
                .replace('0', '۰')
                .replace('1', '۱')
                .replace('2', '۲')
                .replace('3', '۳')
                .replace('4', '۴')
                .replace('5', '۵')
                .replace('6', '۶')
                .replace('7', '۷')
                .replace('8', '۸')
                .replace('9', '۹');
    }

    private static class CalculationRecord {
        final long id;
        final String month;
        final String productName;
        final double quantity;
        final double actualLeather;
        final double expectedLeather;
        final double difference;
        final String status;

        CalculationRecord(long id, String month, String productName, double quantity, double actualLeather,
                          double expectedLeather, double difference, String status) {
            this.id = id;
            this.month = month;
            this.productName = productName;
            this.quantity = quantity;
            this.actualLeather = actualLeather;
            this.expectedLeather = expectedLeather;
            this.difference = difference;
            this.status = status;
        }
    }
}
