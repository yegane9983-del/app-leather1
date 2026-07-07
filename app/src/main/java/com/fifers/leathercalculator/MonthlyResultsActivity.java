package com.fifers.leathercalculator;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

public class MonthlyResultsActivity extends Activity {

    private static final String PREF_NAME = "leather_calculator_prefs";
    private static final String KEY_CALCULATIONS = "calculation_records";
    private static final String ALL_MONTHS_LABEL = "همه ماه‌ها";
    private static final String ALL_YEARS_LABEL = "همه سال‌ها";

    private final ArrayList<CalculationRecord> allRecords = new ArrayList<>();
    private final ArrayList<CalculationRecord> visibleRecords = new ArrayList<>();

    private SharedPreferences preferences;
    private Spinner filterMonthSpinner;
    private Spinner filterYearSpinner;
    private AutoCompleteTextView filterProductSearchEditText;
    private ListView recordsListView;
    private TextView emptyTextView;
    private TextView monthlyScopeTextView;
    private TextView monthlyTotalTextView;
    private TextView monthlyTotalStatusTextView;
    private TextView visibleRecordsCountTextView;
    private TextView filteredExpectedTextView;
    private TextView filteredActualTextView;
    private TextView goodCutsCountTextView;
    private TextView badCutsCountTextView;
    private View monthlySummaryCard;
    private RecordListAdapter recordsAdapter;
    private ArrayAdapter<String> productFilterAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_monthly_results);
        Utils.applyBottomSystemInset(findViewById(R.id.monthlyScreenRoot));

        preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        bindViews();
        setupAdaptersAndActions();
        loadRecords();
        refreshVisibleRecords();
    }

    private void bindViews() {
        filterMonthSpinner = findViewById(R.id.filterMonthSpinner);
        filterYearSpinner = findViewById(R.id.filterYearSpinner);
        filterProductSearchEditText = findViewById(R.id.filterProductSearchEditText);
        recordsListView = findViewById(R.id.recordsListView);
        emptyTextView = findViewById(R.id.emptyTextView);
        monthlyScopeTextView = findViewById(R.id.monthlyScopeTextView);
        monthlyTotalTextView = findViewById(R.id.monthlyTotalTextView);
        monthlyTotalStatusTextView = findViewById(R.id.monthlyTotalStatusTextView);
        visibleRecordsCountTextView = findViewById(R.id.visibleRecordsCountTextView);
        filteredExpectedTextView = findViewById(R.id.filteredExpectedTextView);
        filteredActualTextView = findViewById(R.id.filteredActualTextView);
        goodCutsCountTextView = findViewById(R.id.goodCutsCountTextView);
        badCutsCountTextView = findViewById(R.id.badCutsCountTextView);
        monthlySummaryCard = findViewById(R.id.monthlySummaryCard);
    }

    private void setupAdaptersAndActions() {
        Button backButton = findViewById(R.id.backButton);
        Button clearRecordsButton = findViewById(R.id.clearRecordsButton);
        Button clearProductFilterButton = findViewById(R.id.clearProductFilterButton);

        List<String> filterMonths = new ArrayList<>();
        filterMonths.add(ALL_MONTHS_LABEL);
        for (String month : Utils.MONTHS) {
            filterMonths.add(month);
        }
        ArrayAdapter<String> monthAdapter = new ArrayAdapter<>(this, R.layout.spinner_selected_item,
                R.id.spinnerText, filterMonths);
        monthAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        filterMonthSpinner.setAdapter(monthAdapter);
        filterMonthSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                refreshVisibleRecords();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });

        List<String> filterYears = new ArrayList<>();
        filterYears.add(ALL_YEARS_LABEL);
        filterYears.addAll(Utils.jalaliYearOptions());
        ArrayAdapter<String> yearAdapter = new ArrayAdapter<>(this, R.layout.spinner_selected_item,
                R.id.spinnerText, filterYears);
        yearAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        filterYearSpinner.setAdapter(yearAdapter);
        filterYearSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                refreshVisibleRecords();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });

        productFilterAdapter = new ArrayAdapter<>(this, R.layout.autocomplete_dropdown_item,
                R.id.dropdownText, new ArrayList<>());
        filterProductSearchEditText.setAdapter(productFilterAdapter);
        filterProductSearchEditText.setThreshold(0);
        filterProductSearchEditText.setOnClickListener(view -> filterProductSearchEditText.showDropDown());
        filterProductSearchEditText.setOnFocusChangeListener((view, hasFocus) -> {
            if (hasFocus) {
                filterProductSearchEditText.showDropDown();
            }
        });
        filterProductSearchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                refreshVisibleRecords();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        clearProductFilterButton.setOnClickListener(view -> {
            filterProductSearchEditText.setText("");
            filterProductSearchEditText.clearFocus();
        });

        recordsAdapter = new RecordListAdapter();
        recordsListView.setAdapter(recordsAdapter);
        recordsListView.setOnItemLongClickListener((parent, view, position, id) -> {
            confirmDeleteRecord(visibleRecords.get(position));
            return true;
        });
        backButton.setOnClickListener(view -> finish());
        clearRecordsButton.setOnClickListener(view -> confirmClearRecords());
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
                        object.optString("year", ""),
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
        refreshProductFilterSuggestions();
    }

    private void refreshProductFilterSuggestions() {
        if (productFilterAdapter == null) return;
        LinkedHashSet<String> productNames = new LinkedHashSet<>();
        for (CalculationRecord record : allRecords) {
            productNames.add(record.productName);
        }
        productFilterAdapter.clear();
        productFilterAdapter.addAll(productNames);
        productFilterAdapter.notifyDataSetChanged();
    }

    private void refreshVisibleRecords() {
        if (filterMonthSpinner == null || filterYearSpinner == null || recordsAdapter == null
                || filterProductSearchEditText == null) return;
        String selectedMonth = String.valueOf(filterMonthSpinner.getSelectedItem());
        String selectedYear = String.valueOf(filterYearSpinner.getSelectedItem());
        String productQuery = filterProductSearchEditText.getText().toString().trim();
        String normalizedProductQuery = normalizeSearchText(productQuery);
        double totalDifference = 0;
        double totalExpected = 0;
        double totalActual = 0;
        int goodCutCount = 0;
        int badCutCount = 0;
        visibleRecords.clear();

        for (CalculationRecord record : allRecords) {
            boolean matchesMonth = ALL_MONTHS_LABEL.equals(selectedMonth) || selectedMonth.equals(record.month);
            boolean matchesYear = ALL_YEARS_LABEL.equals(selectedYear) || selectedYear.equals(record.year);
            boolean matchesProduct = normalizedProductQuery.isEmpty()
                    || normalizeSearchText(record.productName).contains(normalizedProductQuery);
            if (matchesMonth && matchesYear && matchesProduct) {
                visibleRecords.add(record);
                totalDifference += record.difference;
                totalExpected += record.expectedLeather;
                totalActual += record.actualLeather;
                if (record.difference < 0) {
                    goodCutCount++;
                } else if (record.difference > 0) {
                    badCutCount++;
                }
            }
        }

        monthlyScopeTextView.setText(buildScopeLabel(selectedYear, selectedMonth, productQuery));
        monthlyTotalTextView.setText(Utils.formatSignedNumber(totalDifference) + " پا");
        filteredExpectedTextView.setText(Utils.formatNumber(totalExpected) + " پا");
        filteredActualTextView.setText(Utils.formatNumber(totalActual) + " پا");
        visibleRecordsCountTextView.setText(Utils.toPersianDigits(String.valueOf(visibleRecords.size())));
        goodCutsCountTextView.setText(Utils.toPersianDigits(String.valueOf(goodCutCount)));
        badCutsCountTextView.setText(Utils.toPersianDigits(String.valueOf(badCutCount)));
        applySummaryStatus(totalDifference, visibleRecords.size());
        recordsAdapter.notifyDataSetChanged();
        emptyTextView.setText("هنوز داده‌ای در این فیلتر ثبت نشده است.\nبعد از محاسبه، سوابق اینجا نمایش داده می‌شوند.");
        emptyTextView.setVisibility(visibleRecords.isEmpty() ? View.VISIBLE : View.GONE);
        recordsListView.setVisibility(visibleRecords.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private String buildScopeLabel(String selectedYear, String selectedMonth, String productQuery) {
        String yearDescription = ALL_YEARS_LABEL.equals(selectedYear) ? ALL_YEARS_LABEL : "سال «" + selectedYear + "»";
        String monthDescription = ALL_MONTHS_LABEL.equals(selectedMonth) ? ALL_MONTHS_LABEL : "ماه «" + selectedMonth + "»";
        String productDescription = productQuery.isEmpty() ? "همه محصولات" : "محصول شامل «" + productQuery + "»";
        return yearDescription + "  •  " + monthDescription + "  •  " + productDescription;
    }

    private void applySummaryStatus(double totalDifference, int recordCount) {
        if (recordCount == 0) {
            monthlySummaryCard.setBackgroundResource(R.drawable.bg_total_card);
            monthlyTotalStatusTextView.setText("بدون محاسبه");
            monthlyTotalStatusTextView.setBackgroundResource(R.drawable.bg_badge_neutral);
            monthlyTotalStatusTextView.setTextColor(getColor(R.color.neutral_dark));
            monthlyTotalTextView.setTextColor(getColor(R.color.text_primary));
        } else if (totalDifference < 0) {
            monthlySummaryCard.setBackgroundResource(R.drawable.bg_result_card_good);
            monthlyTotalStatusTextView.setText("برش خوب");
            monthlyTotalStatusTextView.setBackgroundResource(R.drawable.bg_badge_good);
            monthlyTotalStatusTextView.setTextColor(getColor(R.color.success_dark));
            monthlyTotalTextView.setTextColor(getColor(R.color.success_dark));
        } else if (totalDifference > 0) {
            monthlySummaryCard.setBackgroundResource(R.drawable.bg_result_card_bad);
            monthlyTotalStatusTextView.setText("برش بد");
            monthlyTotalStatusTextView.setBackgroundResource(R.drawable.bg_badge_bad);
            monthlyTotalStatusTextView.setTextColor(getColor(R.color.danger_dark));
            monthlyTotalTextView.setTextColor(getColor(R.color.danger_dark));
        } else {
            monthlySummaryCard.setBackgroundResource(R.drawable.bg_total_card);
            monthlyTotalStatusTextView.setText("بدون اختلاف");
            monthlyTotalStatusTextView.setBackgroundResource(R.drawable.bg_badge_neutral);
            monthlyTotalStatusTextView.setTextColor(getColor(R.color.neutral_dark));
            monthlyTotalTextView.setTextColor(getColor(R.color.text_primary));
        }
    }

    private String normalizeSearchText(String value) {
        if (value == null) return "";
        return value.trim().toLowerCase(Locale.ROOT)
                .replace('ي', 'ی')
                .replace('ك', 'ک');
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

    private class RecordListAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return visibleRecords.size();
        }

        @Override
        public CalculationRecord getItem(int position) {
            return visibleRecords.get(position);
        }

        @Override
        public long getItemId(int position) {
            return getItem(position).id;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = LayoutInflater.from(MonthlyResultsActivity.this)
                        .inflate(R.layout.row_calculation_record, parent, false);
            }
            CalculationRecord record = getItem(position);
            TextView titleView = view.findViewById(R.id.recordTitleTextView);
            TextView quantityValueView = view.findViewById(R.id.recordQuantityValueTextView);
            TextView actualValueView = view.findViewById(R.id.recordActualValueTextView);
            TextView expectedValueView = view.findViewById(R.id.recordExpectedValueTextView);
            TextView differenceView = view.findViewById(R.id.recordDifferenceTextView);
            TextView statusView = view.findViewById(R.id.recordStatusTextView);

            String title = record.productName + "  •  " + record.month;
            if (!record.year.isEmpty()) {
                title += "  •  " + record.year;
            }
            titleView.setText(title);
            quantityValueView.setText(Utils.formatNumber(record.quantity));
            actualValueView.setText(Utils.formatNumber(record.actualLeather) + " پا");
            expectedValueView.setText(Utils.formatNumber(record.expectedLeather) + " پا");
            differenceView.setText("مابه‌التفاوت: " + Utils.formatSignedNumber(record.difference) + " پا");
            statusView.setText(record.status);
            applyRecordStatus(statusView, differenceView, record.difference);
            return view;
        }
    }

    private void applyRecordStatus(TextView statusView, TextView differenceView, double difference) {
        if (difference < 0) {
            statusView.setBackgroundResource(R.drawable.bg_badge_good);
            statusView.setTextColor(getColor(R.color.success_dark));
            differenceView.setTextColor(getColor(R.color.success_dark));
        } else if (difference > 0) {
            statusView.setBackgroundResource(R.drawable.bg_badge_bad);
            statusView.setTextColor(getColor(R.color.danger_dark));
            differenceView.setTextColor(getColor(R.color.danger_dark));
        } else {
            statusView.setBackgroundResource(R.drawable.bg_badge_neutral);
            statusView.setTextColor(getColor(R.color.neutral_dark));
            differenceView.setTextColor(getColor(R.color.text_primary));
        }
    }

    private static class CalculationRecord {
        final long id;
        final String year;
        final String month;
        final String productName;
        final double quantity;
        final double actualLeather;
        final double expectedLeather;
        final double difference;
        final String status;

        CalculationRecord(long id, String year, String month, String productName, double quantity, double actualLeather,
                          double expectedLeather, double difference, String status) {
            this.id = id;
            this.year = year;
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
