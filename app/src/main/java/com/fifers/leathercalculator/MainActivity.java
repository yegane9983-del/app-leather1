package com.fifers.leathercalculator;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private static final String PREF_NAME = "leather_calculator_prefs";
    private static final String KEY_PRODUCTS = "products";
    private static final String KEY_CALCULATIONS = "calculation_records";

    private static final int TAB_ADD_PRODUCT = 0;
    private static final int TAB_CALCULATE = 1;

    private final ArrayList<Product> products = new ArrayList<>();
    private ProductListAdapter listAdapter;
    private ArrayAdapter<String> productSearchAdapter;
    private List<String> yearOptions;

    private EditText productNameEditText;
    private EditText defaultUsageEditText;
    private EditText quantityEditText;
    private EditText actualLeatherEditText;
    private TextView resultTextView;
    private TextView resultStatusTextView;
    private TextView resultDifferenceTextView;
    private TextView resultProductTextView;
    private TextView resultExpectedTextView;
    private TextView resultActualTextView;
    private TextView dashboardProductCountTextView;
    private TextView dashboardRecordCountTextView;
    private TextView catalogCountTextView;
    private TextView emptyProductsTextView;
    private View resultCard;
    private AutoCompleteTextView productSearchEditText;
    private Spinner monthSpinner;
    private Spinner yearSpinner;
    private ListView productsListView;
    private SharedPreferences preferences;

    private TextView tabAddProductButton;
    private TextView tabCalculateButton;
    private TextView tabReportsButton;
    private View calculateSectionContainer;
    private View addProductSectionContainer;
    private int selectedTab = TAB_CALCULATE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Utils.applyBottomSystemInset(findViewById(R.id.mainScreenRoot));

        preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        bindViews();
        loadProducts();
        setupAdapters();
        setupActions();
        selectTab(selectedTab);
        updateDashboardStats();
        updateProductEmptyState();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (preferences != null) {
            updateDashboardStats();
        }
    }

    private void bindViews() {
        productNameEditText = findViewById(R.id.productNameEditText);
        defaultUsageEditText = findViewById(R.id.defaultUsageEditText);
        quantityEditText = findViewById(R.id.quantityEditText);
        actualLeatherEditText = findViewById(R.id.actualLeatherEditText);
        resultCard = findViewById(R.id.resultCard);
        resultTextView = findViewById(R.id.resultTextView);
        resultStatusTextView = findViewById(R.id.resultStatusTextView);
        resultDifferenceTextView = findViewById(R.id.resultDifferenceTextView);
        resultProductTextView = findViewById(R.id.resultProductTextView);
        resultExpectedTextView = findViewById(R.id.resultExpectedTextView);
        resultActualTextView = findViewById(R.id.resultActualTextView);
        dashboardProductCountTextView = findViewById(R.id.dashboardProductCountTextView);
        dashboardRecordCountTextView = findViewById(R.id.dashboardRecordCountTextView);
        catalogCountTextView = findViewById(R.id.catalogCountTextView);
        emptyProductsTextView = findViewById(R.id.emptyProductsTextView);
        productSearchEditText = findViewById(R.id.productSearchEditText);
        monthSpinner = findViewById(R.id.monthSpinner);
        yearSpinner = findViewById(R.id.yearSpinner);
        productsListView = findViewById(R.id.productsListView);

        tabAddProductButton = findViewById(R.id.tabAddProductButton);
        tabCalculateButton = findViewById(R.id.tabCalculateButton);
        tabReportsButton = findViewById(R.id.tabReportsButton);
        calculateSectionContainer = findViewById(R.id.calculateSectionContainer);
        addProductSectionContainer = findViewById(R.id.addProductSectionContainer);

        Button addProductButton = findViewById(R.id.addProductButton);
        Button calculateButton = findViewById(R.id.calculateButton);

        addProductButton.setOnClickListener(v -> addProduct());
        calculateButton.setOnClickListener(v -> calculateDifference());

        tabAddProductButton.setOnClickListener(v -> selectTab(TAB_ADD_PRODUCT));
        tabCalculateButton.setOnClickListener(v -> selectTab(TAB_CALCULATE));
        tabReportsButton.setOnClickListener(v -> startActivity(new Intent(this, MonthlyResultsActivity.class)));
    }

    private void selectTab(int tab) {
        selectedTab = tab;
        boolean showAddProduct = tab == TAB_ADD_PRODUCT;
        addProductSectionContainer.setVisibility(showAddProduct ? View.VISIBLE : View.GONE);
        calculateSectionContainer.setVisibility(showAddProduct ? View.GONE : View.VISIBLE);
        styleTab(tabAddProductButton, showAddProduct);
        styleTab(tabCalculateButton, !showAddProduct);
        styleTab(tabReportsButton, false);
    }

    private void styleTab(TextView tabView, boolean isSelected) {
        tabView.setBackgroundResource(isSelected ? R.drawable.bg_tab_selected : R.drawable.bg_tab_unselected);
        tabView.setTextColor(isSelected ? getColor(R.color.text_on_primary) : getColor(R.color.text_secondary));
    }

    private void setupAdapters() {
        listAdapter = new ProductListAdapter();
        productsListView.setAdapter(listAdapter);

        productSearchAdapter = new ArrayAdapter<>(this, R.layout.autocomplete_dropdown_item,
                R.id.dropdownText, productNames());
        productSearchEditText.setAdapter(productSearchAdapter);
        productSearchEditText.setThreshold(1);
        productSearchEditText.setOnFocusChangeListener((view, hasFocus) -> {
            if (hasFocus && !products.isEmpty()) {
                productSearchEditText.showDropDown();
            }
        });
        productSearchEditText.setOnClickListener(view -> {
            if (!products.isEmpty()) {
                productSearchEditText.showDropDown();
            }
        });

        ArrayAdapter<String> monthAdapter = new ArrayAdapter<>(this, R.layout.spinner_selected_item,
                R.id.spinnerText, Utils.MONTHS);
        monthAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        monthSpinner.setAdapter(monthAdapter);

        yearOptions = Utils.jalaliYearOptions();
        ArrayAdapter<String> yearAdapter = new ArrayAdapter<>(this, R.layout.spinner_selected_item,
                R.id.spinnerText, yearOptions);
        yearAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        yearSpinner.setAdapter(yearAdapter);
        yearSpinner.setSelection(Utils.currentJalaliYearIndexIn(yearOptions));
    }

    private void setupActions() {
        productsListView.setOnItemLongClickListener((parent, view, position, id) -> {
            Product product = products.get(position);
            new AlertDialog.Builder(this)
                    .setTitle("حذف محصول")
                    .setMessage("محصول «" + product.name + "» حذف شود؟")
                    .setPositiveButton("حذف", (dialog, which) -> {
                        products.remove(position);
                        saveProducts();
                        refreshAdapters();
                        Toast.makeText(this, "محصول حذف شد.", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("لغو", null)
                    .show();
            return true;
        });
    }

    private void addProduct() {
        String name = productNameEditText.getText().toString().trim();
        double defaultUsage = Utils.parseNumber(defaultUsageEditText.getText().toString());

        if (name.isEmpty()) {
            showToast("نام محصول را وارد کنید.");
            return;
        }
        if (findProductByName(name) != null) {
            showToast("محصولی با این نام قبلاً ثبت شده است.");
            return;
        }
        if (Double.isNaN(defaultUsage) || defaultUsage <= 0) {
            showToast("مصرف استاندارد باید عددی بزرگ‌تر از صفر باشد.");
            return;
        }

        products.add(new Product(name, defaultUsage));
        saveProducts();
        refreshAdapters();

        productNameEditText.setText("");
        defaultUsageEditText.setText("");
        productSearchEditText.setText(name, false);
        showToast("محصول با موفقیت ثبت شد.");
    }

    private void calculateDifference() {
        if (products.isEmpty()) {
            showToast("ابتدا حداقل یک محصول اضافه کنید.");
            return;
        }

        String searchedProductName = productSearchEditText.getText().toString().trim();
        Product product = findProductByName(searchedProductName);
        if (product == null) {
            showToast("یک محصول معتبر از نتایج جستجو انتخاب کنید.");
            return;
        }

        String year = String.valueOf(yearSpinner.getSelectedItem());
        String month = String.valueOf(monthSpinner.getSelectedItem());
        double quantity = Utils.parseNumber(quantityEditText.getText().toString());
        double actualLeather = Utils.parseNumber(actualLeatherEditText.getText().toString());

        if (Double.isNaN(quantity) || quantity <= 0) {
            showToast("تعداد محصول را درست وارد کنید.");
            return;
        }
        if (Double.isNaN(actualLeather) || actualLeather < 0) {
            showToast("چرم مصرفی واقعی را درست وارد کنید.");
            return;
        }

        double expectedLeather = quantity * product.defaultUsage;
        double difference = actualLeather - expectedLeather;
        String status = cuttingStatus(difference);

        showCalculationResult(year, month, product, quantity, actualLeather, expectedLeather, difference, status);
        saveCalculation(year, month, product, quantity, actualLeather, expectedLeather, difference, status);
        showToast("نتیجه در سوابق «" + month + " " + year + "» ذخیره شد.");
    }

    private void showCalculationResult(String year, String month, Product product, double quantity, double actualLeather,
                                       double expectedLeather, double difference, String status) {
        resultCard.setVisibility(View.VISIBLE);
        resultStatusTextView.setText(status);
        resultDifferenceTextView.setText(Utils.formatSignedNumber(difference) + " پا");
        resultProductTextView.setText("سال «" + year + "»  •  ماه «" + month + "»  •  " + product.name);
        resultExpectedTextView.setText(Utils.formatNumber(expectedLeather) + " پا");
        resultActualTextView.setText(Utils.formatNumber(actualLeather) + " پا");
        applyStatusStyle(resultStatusTextView, resultDifferenceTextView, difference);

        String details = "مصرف هر عدد: " + Utils.formatNumber(product.defaultUsage) + " پا"
                + "   •   تعداد تولید: " + Utils.formatNumber(quantity)
                + "\nنتیجه ثبت شد و در گزارش ماهانه قابل پیگیری است.";
        resultTextView.setText(details);
    }

    private void applyStatusStyle(TextView statusView, TextView differenceView, double difference) {
        if (difference < 0) {
            resultCard.setBackgroundResource(R.drawable.bg_result_card_good);
            statusView.setBackgroundResource(R.drawable.bg_badge_good);
            statusView.setTextColor(getColor(R.color.success_dark));
            differenceView.setTextColor(getColor(R.color.success_dark));
        } else if (difference > 0) {
            resultCard.setBackgroundResource(R.drawable.bg_result_card_bad);
            statusView.setBackgroundResource(R.drawable.bg_badge_bad);
            statusView.setTextColor(getColor(R.color.danger_dark));
            differenceView.setTextColor(getColor(R.color.danger_dark));
        } else {
            resultCard.setBackgroundResource(R.drawable.bg_result_card_neutral);
            statusView.setBackgroundResource(R.drawable.bg_badge_neutral);
            statusView.setTextColor(getColor(R.color.neutral_dark));
            differenceView.setTextColor(getColor(R.color.text_primary));
        }
    }

    private String cuttingStatus(double difference) {
        if (difference > 0) {
            return "برش بد";
        } else if (difference < 0) {
            return "برش خوب";
        }
        return "برش بدون اختلاف";
    }

    private Product findProductByName(String name) {
        for (Product product : products) {
            if (product.name.equalsIgnoreCase(name.trim())) {
                return product;
            }
        }
        return null;
    }

    private void saveCalculation(String year, String month, Product product, double quantity, double actualLeather,
                                 double expectedLeather, double difference, String status) {
        JSONArray records = readJsonArray(KEY_CALCULATIONS);
        JSONObject object = new JSONObject();
        try {
            object.put("year", year);
            object.put("month", month);
            object.put("productName", product.name);
            object.put("defaultUsage", product.defaultUsage);
            object.put("quantity", quantity);
            object.put("actualLeather", actualLeather);
            object.put("expectedLeather", expectedLeather);
            object.put("difference", difference);
            object.put("status", status);
            object.put("createdAt", System.currentTimeMillis());
            records.put(object);
            preferences.edit().putString(KEY_CALCULATIONS, records.toString()).apply();
            updateDashboardStats();
        } catch (JSONException ignored) {
            showToast("ذخیره سابقه انجام نشد.");
        }
    }

    private JSONArray readJsonArray(String key) {
        String json = preferences.getString(key, "[]");
        try {
            return new JSONArray(json);
        } catch (JSONException e) {
            return new JSONArray();
        }
    }

    private void loadProducts() {
        products.clear();
        String savedJson = preferences.getString(KEY_PRODUCTS, null);

        if (savedJson == null) {
            products.add(new Product("دلار", 4));
            saveProducts();
            return;
        }

        try {
            JSONArray array = new JSONArray(savedJson);
            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.getJSONObject(i);
                String name = object.optString("name", "").trim();
                double defaultUsage = object.optDouble("defaultUsage", 0);
                if (!name.isEmpty() && defaultUsage > 0) {
                    products.add(new Product(name, defaultUsage));
                }
            }
        } catch (JSONException e) {
            products.clear();
            products.add(new Product("دلار", 4));
            saveProducts();
        }
    }

    private void saveProducts() {
        JSONArray array = new JSONArray();
        for (Product product : products) {
            JSONObject object = new JSONObject();
            try {
                object.put("name", product.name);
                object.put("defaultUsage", product.defaultUsage);
                array.put(object);
            } catch (JSONException ignored) {
            }
        }
        preferences.edit().putString(KEY_PRODUCTS, array.toString()).apply();
    }

    private void refreshAdapters() {
        listAdapter.notifyDataSetChanged();
        productSearchAdapter.clear();
        productSearchAdapter.addAll(productNames());
        productSearchAdapter.notifyDataSetChanged();
        updateDashboardStats();
        updateProductEmptyState();
    }

    private void updateDashboardStats() {
        if (dashboardProductCountTextView == null || dashboardRecordCountTextView == null || catalogCountTextView == null) {
            return;
        }
        int recordCount = readJsonArray(KEY_CALCULATIONS).length();
        dashboardProductCountTextView.setText(Utils.toPersianDigits(String.valueOf(products.size())));
        dashboardRecordCountTextView.setText(Utils.toPersianDigits(String.valueOf(recordCount)));
        catalogCountTextView.setText(Utils.toPersianDigits(String.valueOf(products.size())) + " محصول");
    }

    private void updateProductEmptyState() {
        if (emptyProductsTextView == null || productsListView == null) {
            return;
        }
        boolean isEmpty = products.isEmpty();
        emptyProductsTextView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        productsListView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    private List<String> productNames() {
        ArrayList<String> names = new ArrayList<>();
        for (Product product : products) {
            names.add(product.name);
        }
        return names;
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private class ProductListAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return products.size();
        }

        @Override
        public Product getItem(int position) {
            return products.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = LayoutInflater.from(MainActivity.this).inflate(R.layout.row_product, parent, false);
            }
            Product product = getItem(position);
            TextView nameView = view.findViewById(R.id.productRowNameTextView);
            TextView usageView = view.findViewById(R.id.productRowUsageTextView);
            nameView.setText(product.name);
            usageView.setText("مصرف استاندارد: " + Utils.formatNumber(product.defaultUsage) + " پا برای هر عدد");
            return view;
        }
    }

    private static class Product {
        final String name;
        final double defaultUsage;

        Product(String name, double defaultUsage) {
            this.name = name;
            this.defaultUsage = defaultUsage;
        }
    }
}
