package com.example.fuelbook;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class MainActivity extends Activity {
    private static final int REQUEST_IMPORT = 501, REQUEST_EXPORT = 502, REQUEST_BACKUP_DB = 503;
    private static final int HISTORY_PREVIEW_LIMIT = 5;

    private FuelDb db;
    private SharedPreferences prefs;
    private boolean dark;
    private int bg, panel, primary, secondary, accent, tint, border, field, warn, danger;
    private long selectedVehicle = -1;
    private int activeTab = 0; // 0 = General Expenses (Tab 1), 1 = Vehicle Expenses (Tab 2)

    // Form states
    private long selectedGenDate = System.currentTimeMillis();
    private long selectedFillDate = System.currentTimeMillis();
    private long selectedExpenseDate = System.currentTimeMillis();
    private String selectedCategory = "Food";

    // Filter & Search states for Tab 1 (General Expenses)
    private int genFilterMode = 0; // 0 = All Time, 1 = Month-wise, 2 = Custom Interval
    private String genSelectedMonth = ""; // YYYY-MM
    private long genStartMillis = 0;
    private long genEndMillis = System.currentTimeMillis();
    private String genSearchQuery = "";
    private String genCategoryFilter = "ALL";
    private boolean genHistoryExpanded = false;

    // Filter & Search states for Tab 2 (Vehicle Expenses & Mileage)
    private int vehFilterMode = 0; // 0 = All Time, 1 = Month-wise, 2 = Custom Interval
    private String vehSelectedMonth = ""; // YYYY-MM
    private long vehStartMillis = 0;
    private long vehEndMillis = System.currentTimeMillis();
    private String vehSearchQuery = "";
    private boolean fillHistoryExpanded = false;
    private boolean vehMiscHistoryExpanded = false;

    private LinearLayout root;
    private EditText odoInput, litersInput, amountInput, estimateInput;
    private EditText expTitleInput, expAmountInput;
    private EditText genTitleInput, genAmountInput;
    private final DecimalFormat fmt = new DecimalFormat("#,##0.##");
    private final SimpleDateFormat ymFmt = new SimpleDateFormat("yyyy-MM", Locale.US);
    private final SimpleDateFormat ymLabelFmt = new SimpleDateFormat("MMMM yyyy", Locale.US);
    private final SimpleDateFormat shortMonthFmt = new SimpleDateFormat("MMM yy", Locale.US);

    @Override public void onCreate(Bundle state) {
        prefs = getSharedPreferences("settings", MODE_PRIVATE);
        dark = prefs.getBoolean("dark", false);
        setTheme(dark ? R.style.AppTheme_Dark : R.style.AppTheme);
        super.onCreate(state);
        db = new FuelDb(this);
        selectedVehicle = prefs.getLong("selected_vehicle", -1);

        Calendar cal = Calendar.getInstance();
        String curYm = ymFmt.format(cal.getTime());
        genSelectedMonth = curYm;
        vehSelectedMonth = curYm;
        cal.add(Calendar.DAY_OF_MONTH, -30);
        genStartMillis = startOfDay(cal.getTimeInMillis());
        vehStartMillis = startOfDay(cal.getTimeInMillis());
        genEndMillis = endOfDay(System.currentTimeMillis());
        vehEndMillis = endOfDay(System.currentTimeMillis());

        colors(); render();
    }

    private long startOfDay(long millis) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(millis);
        c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0); c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }
    private long endOfDay(long millis) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(millis);
        c.set(Calendar.HOUR_OF_DAY, 23); c.set(Calendar.MINUTE, 59); c.set(Calendar.SECOND, 59); c.set(Calendar.MILLISECOND, 999);
        return c.getTimeInMillis();
    }

    private boolean matchesFilter(long timestamp, int mode, String ym, long startMs, long endMs) {
        if (mode == 0) return true;
        if (mode == 1) {
            return ymFmt.format(new Date(timestamp)).equals(ym);
        }
        return timestamp >= startOfDay(startMs) && timestamp <= endOfDay(endMs);
    }

    private void colors() {
        bg = Color.parseColor(dark ? "#101416" : "#F7F8FA");
        panel = Color.parseColor(dark ? "#1B2225" : "#FFFFFF");
        primary = Color.parseColor(dark ? "#F3F7F7" : "#162B30");
        secondary = Color.parseColor(dark ? "#A2B1B4" : "#64777D");
        accent = Color.parseColor(dark ? "#65DCC7" : "#0D796A");
        tint = Color.parseColor(dark ? "#233D39" : "#E7F6F1");
        border = Color.parseColor(dark ? "#354246" : "#E6ECEE");
        field = Color.parseColor(dark ? "#263034" : "#F4F7F8");
        warn = Color.parseColor("#F59E0B");
        danger = Color.parseColor("#EF4444");
        getWindow().setStatusBarColor(bg);
        getWindow().setNavigationBarColor(bg);
        getWindow().getDecorView().setSystemUiVisibility(dark ? 0 : View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    }
    private int d(float n) { return (int) (n * getResources().getDisplayMetrics().density + 0.5f); }
    private GradientDrawable shape(int color, int radius, int stroke) {
        GradientDrawable s = new GradientDrawable(); s.setColor(color); s.setCornerRadius(d(radius));
        if (stroke != 0) s.setStroke(d(1), stroke);
        return s;
    }
    private LinearLayout col() { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); return l; }
    private LinearLayout row() { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.HORIZONTAL); l.setGravity(Gravity.CENTER_VERTICAL); return l; }
    private LinearLayout.LayoutParams lp(int width, int height) { return new LinearLayout.LayoutParams(width < 0 ? width : d(width), height < 0 ? height : d(height)); }
    private void addSpace(LinearLayout target, int height) { target.addView(new View(this), lp(1, height)); }
    private TextView text(String value, int size, int color, boolean bold) {
        TextView v = new TextView(this); v.setText(value); v.setTextColor(color); v.setTextSize(size);
        if (bold) v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        v.setIncludeFontPadding(false); return v;
    }
    private LinearLayout card(LinearLayout target) {
        LinearLayout l = col(); l.setPadding(d(18), d(18), d(18), d(18)); l.setBackground(shape(panel, 20, border));
        target.addView(l, lp(-1, -2)); return l;
    }
    private Button button(String title, boolean filled) {
        Button v = new Button(this); v.setText(title); v.setAllCaps(false); v.setTextSize(12);
        v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        v.setTextColor(filled ? (dark ? Color.parseColor("#09251E") : Color.WHITE) : accent);
        v.setBackground(shape(filled ? accent : tint, 12, 0));
        v.setPadding(d(10), d(6), d(10), d(6)); v.setMinHeight(d(42)); return v;
    }
    private EditText edit(String hint, String value, boolean numeric) {
        EditText e = new EditText(this); e.setSingleLine(true); e.setTextSize(16); e.setTextColor(primary); e.setHintTextColor(secondary);
        e.setHint(hint); e.setText(value); e.setSelectAllOnFocus(false);
        if (numeric) e.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        e.setPadding(d(14), 0, d(14), 0); e.setBackground(shape(field, 11, 0));
        e.setLayoutParams(lp(-1, 52)); return e;
    }
    private EditText input(LinearLayout target, String label, String hint, String initial, boolean numeric) {
        target.addView(text(label, 12, secondary, true)); addSpace(target, 7);
        EditText e = edit(hint, initial, numeric); target.addView(e); addSpace(target, 14); return e;
    }
    private double number(EditText e, String title, boolean zeroAllowed) {
        String s = e.getText().toString().trim();
        if (s.isEmpty()) throw new IllegalArgumentException("Enter " + title.toLowerCase(Locale.ROOT));
        double val;
        try { val = Double.parseDouble(s); } catch (Exception ex) { throw new IllegalArgumentException("Invalid " + title.toLowerCase(Locale.ROOT)); }
        if (!Double.isFinite(val) || val < 0 || (!zeroAllowed && val == 0)) throw new IllegalArgumentException("Invalid " + title.toLowerCase(Locale.ROOT));
        return val;
    }
    private String n(double value) { return fmt.format(value); }
    private String date(long time) { return DateFormat.getDateInstance(DateFormat.MEDIUM).format(new Date(time)); }
    private void alert(String message) { new AlertDialog.Builder(this).setMessage(message).setPositiveButton("OK", null).show(); }
    private void toast(String message) { Toast.makeText(this, message, Toast.LENGTH_SHORT).show(); }
    private FuelDb.Vehicle selected(List<FuelDb.Vehicle> vehicles) {
        for (FuelDb.Vehicle v : vehicles) if (v.id == selectedVehicle) return v;
        if (!vehicles.isEmpty()) { selectedVehicle = vehicles.get(0).id; prefs.edit().putLong("selected_vehicle", selectedVehicle).apply(); return vehicles.get(0); }
        selectedVehicle = -1; return null;
    }
    private void pickDate(long initialMillis, OnDatePicked listener) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(initialMillis);
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            Calendar chosen = Calendar.getInstance();
            chosen.set(year, month, dayOfMonth, 12, 0, 0);
            listener.onPicked(chosen.getTimeInMillis());
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }
    private interface OnDatePicked { void onPicked(long millis); }

    // Budget Progress Bar Helper
    private void renderBudgetBar(LinearLayout container, String label, double spent, double budgetLimit) {
        if (budgetLimit <= 0) return;
        addSpace(container, 12);
        double pct = (spent / budgetLimit) * 100.0;
        int barColor = pct >= 100 ? danger : (pct >= 80 ? warn : accent);

        LinearLayout top = row();
        container.addView(top, lp(-1, -2));
        top.addView(text(label, 11, secondary, true), new LinearLayout.LayoutParams(0, -2, 1));
        String status = "₹" + n(spent) + " / ₹" + n(budgetLimit) + " (" + Math.round(pct) + "%)";
        top.addView(text(status, 11, barColor, true));
        addSpace(container, 6);

        ProgressBar pb = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        pb.setMax(100);
        pb.setProgress((int) Math.min(100, Math.round(pct)));
        pb.getProgressDrawable().setColorFilter(barColor, android.graphics.PorterDuff.Mode.SRC_IN);
        container.addView(pb, lp(-1, 8));

        if (pct >= 100) {
            addSpace(container, 4);
            container.addView(text("⚠ Over budget by ₹" + n(spent - budgetLimit), 11, danger, true));
        } else if (pct >= 80) {
            addSpace(container, 4);
            container.addView(text("Approaching monthly budget limit", 11, warn, true));
        }
    }

    // Reusable Filter Card Builder (All Time | Month-wise | Custom Interval) with Date Range Validation
    private void renderFilterBar(LinearLayout parent, boolean isGeneral, List<Long> allTimestamps) {
        LinearLayout filterCard = card(parent);
        filterCard.addView(text("TIME PERIOD FILTER (CHARTS & HISTORY)", 11, secondary, true));
        addSpace(filterCard, 10);

        int currentMode = isGeneral ? genFilterMode : vehFilterMode;
        LinearLayout modeRow = row();
        filterCard.addView(modeRow, lp(-1, -2));

        Button bAll = button("All Time", currentMode == 0);
        Button bMonth = button("Month-wise", currentMode == 1);
        Button bRange = button("Interval", currentMode == 2);

        LinearLayout.LayoutParams p1 = new LinearLayout.LayoutParams(0, d(40), 1); p1.setMargins(0, 0, d(4), 0);
        LinearLayout.LayoutParams p2 = new LinearLayout.LayoutParams(0, d(40), 1); p2.setMargins(d(4), 0, d(4), 0);
        LinearLayout.LayoutParams p3 = new LinearLayout.LayoutParams(0, d(40), 1); p3.setMargins(d(4), 0, 0, 0);

        modeRow.addView(bAll, p1);
        modeRow.addView(bMonth, p2);
        modeRow.addView(bRange, p3);

        bAll.setOnClickListener(v -> {
            if (isGeneral) { genFilterMode = 0; genHistoryExpanded = false; }
            else { vehFilterMode = 0; fillHistoryExpanded = false; vehMiscHistoryExpanded = false; }
            render();
        });
        bMonth.setOnClickListener(v -> {
            if (isGeneral) { genFilterMode = 1; genHistoryExpanded = false; }
            else { vehFilterMode = 1; fillHistoryExpanded = false; vehMiscHistoryExpanded = false; }
            render();
        });
        bRange.setOnClickListener(v -> {
            if (isGeneral) { genFilterMode = 2; genHistoryExpanded = false; }
            else { vehFilterMode = 2; fillHistoryExpanded = false; vehMiscHistoryExpanded = false; }
            render();
        });

        if (currentMode == 1) {
            addSpace(filterCard, 12);
            TreeMap<String, String> availableMonths = new TreeMap<>(Collections.reverseOrder());
            String curYm = ymFmt.format(new Date());
            availableMonths.put(curYm, ymLabelFmt.format(new Date()));
            for (Long ts : allTimestamps) {
                Date dObj = new Date(ts);
                availableMonths.put(ymFmt.format(dObj), ymLabelFmt.format(dObj));
            }
            List<String> ymKeys = new ArrayList<>(availableMonths.keySet());
            List<String> ymLabels = new ArrayList<>(availableMonths.values());

            String selectedYm = isGeneral ? genSelectedMonth : vehSelectedMonth;
            int selIdx = ymKeys.indexOf(selectedYm);
            if (selIdx < 0) {
                selIdx = 0;
                if (isGeneral) genSelectedMonth = ymKeys.get(0);
                else vehSelectedMonth = ymKeys.get(0);
            }

            Spinner mSpinner = new Spinner(this);
            ArrayAdapter<String> mAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, ymLabels);
            mSpinner.setAdapter(mAdapter);
            mSpinner.setBackground(shape(field, 10, 0));
            mSpinner.setSelection(selIdx);
            filterCard.addView(mSpinner, lp(-1, 48));
            mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                boolean init = true;
                @Override public void onItemSelected(AdapterView<?> parentView, View view, int position, long id) {
                    if (init) { init = false; return; }
                    if (isGeneral) { genSelectedMonth = ymKeys.get(position); genHistoryExpanded = false; }
                    else { vehSelectedMonth = ymKeys.get(position); fillHistoryExpanded = false; vehMiscHistoryExpanded = false; }
                    render();
                }
                @Override public void onNothingSelected(AdapterView<?> parentView) {}
            });
        } else if (currentMode == 2) {
            addSpace(filterCard, 12);
            LinearLayout rangeRow = row();
            filterCard.addView(rangeRow, lp(-1, -2));

            long startMs = isGeneral ? genStartMillis : vehStartMillis;
            long endMs = isGeneral ? genEndMillis : vehEndMillis;

            Button startBtn = button("From: " + date(startMs), false);
            Button endBtn = button("To: " + date(endMs), false);

            LinearLayout.LayoutParams r1 = new LinearLayout.LayoutParams(0, d(44), 1); r1.setMargins(0, 0, d(4), 0);
            LinearLayout.LayoutParams r2 = new LinearLayout.LayoutParams(0, d(44), 1); r2.setMargins(d(4), 0, 0, 0);
            rangeRow.addView(startBtn, r1);
            rangeRow.addView(endBtn, r2);

            // Auto-swap validation if From > To
            startBtn.setOnClickListener(v -> pickDate(startMs, picked -> {
                long newStart = startOfDay(picked);
                long curEnd = isGeneral ? genEndMillis : vehEndMillis;
                if (newStart > curEnd) {
                    toast("Start date was after end date; dates swapped automatically.");
                    if (isGeneral) { genStartMillis = startOfDay(curEnd); genEndMillis = endOfDay(picked); genHistoryExpanded = false; }
                    else { vehStartMillis = startOfDay(curEnd); vehEndMillis = endOfDay(picked); fillHistoryExpanded = false; vehMiscHistoryExpanded = false; }
                } else {
                    if (isGeneral) { genStartMillis = newStart; genHistoryExpanded = false; }
                    else { vehStartMillis = newStart; fillHistoryExpanded = false; vehMiscHistoryExpanded = false; }
                }
                render();
            }));
            endBtn.setOnClickListener(v -> pickDate(endMs, picked -> {
                long newEnd = endOfDay(picked);
                long curStart = isGeneral ? genStartMillis : vehStartMillis;
                if (newEnd < curStart) {
                    toast("End date was before start date; dates swapped automatically.");
                    if (isGeneral) { genEndMillis = endOfDay(curStart); genStartMillis = startOfDay(picked); genHistoryExpanded = false; }
                    else { vehEndMillis = endOfDay(curStart); vehStartMillis = startOfDay(picked); fillHistoryExpanded = false; vehMiscHistoryExpanded = false; }
                } else {
                    if (isGeneral) { genEndMillis = newEnd; genHistoryExpanded = false; }
                    else { vehEndMillis = newEnd; fillHistoryExpanded = false; vehMiscHistoryExpanded = false; }
                }
                render();
            }));
        }
        addSpace(parent, 18);
    }

    private void render() {
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true); scroll.setBackgroundColor(bg);
        root = col(); root.setPadding(d(18), d(22), d(18), d(36)); scroll.addView(root);
        scroll.setOnApplyWindowInsetsListener((view, insets) -> {
            root.setPadding(d(18), d(22) + insets.getSystemWindowInsetTop(), d(18), d(36) + insets.getSystemWindowInsetBottom());
            return insets.consumeSystemWindowInsets();
        });
        setContentView(scroll);

        // Header
        LinearLayout header = row(); root.addView(header, lp(-1, -2));
        LinearLayout names = col(); header.addView(names, new LinearLayout.LayoutParams(0, -2, 1));
        names.addView(text("Fuelbook", 28, primary, true)); addSpace(names, 5);
        names.addView(text("Offline general & vehicle expense manager.", 13, secondary, false));
        Button theme = button(dark ? "☀ Light" : "☾ Dark", false); header.addView(theme, lp(88, 44));
        theme.setOnClickListener(v -> { prefs.edit().putBoolean("dark", !dark).apply(); recreate(); });
        addSpace(root, 18);

        // 2-Tab Navigation Bar: Tab 1 = General Expenses, Tab 2 = Vehicle Expenses
        LinearLayout tabBar = row();
        tabBar.setPadding(d(4), d(4), d(4), d(4));
        tabBar.setBackground(shape(panel, 14, border));
        root.addView(tabBar, lp(-1, -2));

        Button tabGeneral = button("General Expenses", activeTab == 0);
        Button tabVehicle = button("Vehicle Expenses", activeTab == 1);

        LinearLayout.LayoutParams tLp1 = new LinearLayout.LayoutParams(0, d(46), 1); tLp1.setMargins(0, 0, d(4), 0);
        LinearLayout.LayoutParams tLp2 = new LinearLayout.LayoutParams(0, d(46), 1); tLp2.setMargins(d(4), 0, 0, 0);

        tabBar.addView(tabGeneral, tLp1);
        tabBar.addView(tabVehicle, tLp2);

        tabGeneral.setOnClickListener(v -> { if (activeTab != 0) { activeTab = 0; render(); } });
        tabVehicle.setOnClickListener(v -> { if (activeTab != 1) { activeTab = 1; render(); } });
        addSpace(root, 18);

        if (activeTab == 0) {
            generalExpensesTabView();
        } else {
            vehicleExpensesAndMileageTabView();
        }

        addSpace(root, 14); filesCard();
        addSpace(root, 15);
        TextView foot = text("100% Offline  ·  Mileage assumes a full tank at each fill-up.\nDistance since previous fill ÷ litres added now.", 12, secondary, false);
        foot.setGravity(Gravity.CENTER); foot.setLineSpacing(d(3), 1); root.addView(foot);
    }

    // =========================================================
    // TAB 1: GENERAL EXPENSES (BUDGETS, CHIPS, EDIT, SEARCH)
    // =========================================================
    private void generalExpensesTabView() {
        List<FuelDb.GeneralExpense> allGen = db.generalExpenses();
        List<Long> allTs = new ArrayList<>();
        String curYm = ymFmt.format(new Date());
        double currentMonthGenSpend = 0;
        Map<String, Double> currentMonthCatSpend = new LinkedHashMap<>();

        for (FuelDb.GeneralExpense g : allGen) {
            allTs.add(g.time);
            if (ymFmt.format(new Date(g.time)).equals(curYm)) {
                currentMonthGenSpend += g.amount;
                Double mCur = currentMonthCatSpend.get(g.category);
                currentMonthCatSpend.put(g.category, (mCur == null ? 0 : mCur) + g.amount);
            }
        }

        // Time Filter Bar
        renderFilterBar(root, true, allTs);

        // Apply time filter for charts & overview
        List<FuelDb.GeneralExpense> timeFilteredGen = new ArrayList<>();
        double totalGeneral = 0;
        Map<String, Double> catTotals = new LinkedHashMap<>();

        for (FuelDb.GeneralExpense g : allGen) {
            if (matchesFilter(g.time, genFilterMode, genSelectedMonth, genStartMillis, genEndMillis)) {
                timeFilteredGen.add(g);
                totalGeneral += g.amount;
                Double cur = catTotals.get(g.category);
                catTotals.put(g.category, (cur == null ? 0 : cur) + g.amount);
            }
        }

        // 1. Overview Card, Monthly Budget Bar & Offline Category Doughnut Chart
        LinearLayout overview = card(root);
        LinearLayout ovTop = row(); overview.addView(ovTop, lp(-1, -2));
        String periodTitle = genFilterMode == 0 ? "ALL-TIME GENERAL EXPENSES"
                : (genFilterMode == 1 ? "EXPENSES · " + genSelectedMonth : "FILTERED INTERVAL EXPENSES");
        ovTop.addView(text(periodTitle, 12, secondary, true), new LinearLayout.LayoutParams(0, -2, 1));
        Button budgetBtn = button("Set Budget", false);
        ovTop.addView(budgetBtn, lp(96, 36));
        budgetBtn.setOnClickListener(v -> generalBudgetDialog());

        addSpace(overview, 8);
        overview.addView(text("₹" + n(totalGeneral), 31, accent, true)); addSpace(overview, 6);
        overview.addView(text(timeFilteredGen.size() + " entries across " + catTotals.size() + " categories", 13, secondary, false));

        // Monthly Overall Budget Progress (always evaluated against current month or selected month)
        double overallMonthlyBudget = prefs.getFloat("gen_monthly_budget", 0f);
        String evalYm = (genFilterMode == 1) ? genSelectedMonth : curYm;
        double evalMonthSpend = 0;
        Map<String, Double> evalMonthCatSpend = new LinkedHashMap<>();
        for (FuelDb.GeneralExpense g : allGen) {
            if (ymFmt.format(new Date(g.time)).equals(evalYm)) {
                evalMonthSpend += g.amount;
                Double cVal = evalMonthCatSpend.get(g.category);
                evalMonthCatSpend.put(g.category, (cVal == null ? 0 : cVal) + g.amount);
            }
        }
        renderBudgetBar(overview, "MONTHLY BUDGET (" + evalYm + ")", evalMonthSpend, overallMonthlyBudget);

        if (!catTotals.isEmpty()) {
            addSpace(overview, 16);
            overview.addView(text("CATEGORY BREAKDOWN", 12, secondary, true)); addSpace(overview, 10);

            StringBuilder catsJson = new StringBuilder("[");
            StringBuilder valsJson = new StringBuilder("[");
            for (Map.Entry<String, Double> entry : catTotals.entrySet()) {
                if (catsJson.length() > 1) {
                    catsJson.append(",");
                    valsJson.append(",");
                }
                catsJson.append("'").append(entry.getKey().replace("'", "\\'")).append("'");
                valsJson.append(String.format(Locale.US, "%.2f", entry.getValue()));
            }
            catsJson.append("]");
            valsJson.append("]");

            WebView chartWeb = new WebView(this);
            chartWeb.setLayoutParams(lp(-1, 220));
            WebSettings settings = chartWeb.getSettings();
            settings.setJavaScriptEnabled(true);
            settings.setAllowFileAccess(true);

            String bgHex = dark ? "#1B2225" : "#FFFFFF";
            String txtHex = dark ? "#A2B1B4" : "#64777D";

            String html = "<!DOCTYPE html><html><head>" +
                    "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                    "<script src='file:///android_asset/chart.min.js'></script>" +
                    "<style>body { background-color: " + bgHex + "; margin: 0; padding: 0; } .box { position: relative; height: 210px; width: 100%; }</style>" +
                    "</head><body><div class='box'><canvas id='gc'></canvas></div><script>" +
                    "const palette = ['#0D796A','#f59e0b','#3b82f6','#ec4899','#8b5cf6','#10b981','#ef4444','#14b8a6','#f97316','#6366f1'];" +
                    "new Chart(document.getElementById('gc').getContext('2d'), {" +
                    "  type: 'doughnut'," +
                    "  data: {" +
                    "    labels: " + catsJson.toString() + "," +
                    "    datasets: [{ data: " + valsJson.toString() + ", backgroundColor: palette, borderWidth: 1, borderColor: '" + bgHex + "' }]" +
                    "  }," +
                    "  options: {" +
                    "    responsive: true, maintainAspectRatio: false," +
                    "    plugins: { legend: { position: 'right', labels: { color: '" + txtHex + "', font: { size: 11 }, boxWidth: 12 } } }" +
                    "  }" +
                    "});" +
                    "</script></body></html>";

            chartWeb.loadDataWithBaseURL("file:///android_asset/", html, "text/html", "UTF-8", null);
            overview.addView(chartWeb);
        }
        addSpace(root, 18);

        // 2. Add General Expense Form (with Manage Categories + Quick-Fill Chips)
        LinearLayout form = card(root);
        form.addView(text("New general expense", 20, primary, true)); addSpace(form, 6);
        form.addView(text("Log food, groceries, travel, commute, hotel, or custom expenses.", 13, secondary, false)); addSpace(form, 16);

        LinearLayout catHeader = row(); form.addView(catHeader, lp(-1, -2));
        catHeader.addView(text("CATEGORY", 12, secondary, true), new LinearLayout.LayoutParams(0, -2, 1));
        Button manageCatsBtn = button("Manage", false);
        Button newCatBtn = button("+ New", false);
        catHeader.addView(manageCatsBtn, lp(78, 36));
        catHeader.addView(new View(this), lp(6, 1));
        catHeader.addView(newCatBtn, lp(74, 36));
        addSpace(form, 7);

        List<String> cats = db.categories();
        if (!cats.contains(selectedCategory) && !cats.isEmpty()) selectedCategory = cats.get(0);
        Spinner catSpinner = new Spinner(this);
        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, cats);
        catSpinner.setAdapter(catAdapter);
        catSpinner.setBackground(shape(field, 10, 0));
        int catIdx = cats.indexOf(selectedCategory);
        if (catIdx >= 0) catSpinner.setSelection(catIdx);
        form.addView(catSpinner, lp(-1, 50));

        catSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedCategory = cats.get(position);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        manageCatsBtn.setOnClickListener(v -> manageCategoriesDialog());
        newCatBtn.setOnClickListener(v -> {
            LinearLayout dlgContent = col(); dlgContent.setPadding(d(22), d(10), d(22), 0);
            EditText catEdit = edit("e.g. Medical / Rent / Subscriptions", "", false);
            dlgContent.addView(catEdit);
            new AlertDialog.Builder(this)
                    .setTitle("Add Expense Category")
                    .setView(dlgContent)
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Add", (d, w) -> {
                        String newCat = catEdit.getText().toString().trim();
                        if (!newCat.isEmpty()) {
                            db.addCategory(newCat);
                            selectedCategory = newCat;
                            toast("Category added");
                            render();
                        }
                    }).show();
        });
        addSpace(form, 14);

        form.addView(text("DATE OF EXPENSE", 12, secondary, true)); addSpace(form, 7);
        Button genDateBtn = button(date(selectedGenDate), false);
        genDateBtn.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        genDateBtn.setPadding(d(14), 0, d(14), 0);
        form.addView(genDateBtn, lp(-1, 50));
        genDateBtn.setOnClickListener(v -> pickDate(selectedGenDate, millis -> {
            selectedGenDate = millis;
            genDateBtn.setText(date(selectedGenDate));
        }));
        addSpace(form, 14);

        genTitleInput = input(form, "DESCRIPTION / NOTE (OPTIONAL)", "e.g. Lunch / Metro Card / Supermarket", "", false);
        genAmountInput = input(form, "AMOUNT SPENT (₹)", "e.g. 450", "", true);

        // Quick-Fill Amount Chips (+50, +100, +250, +500, +1000)
        LinearLayout chipRow = row();
        form.addView(chipRow, lp(-1, -2));
        int[] quickAmts = {50, 100, 250, 500, 1000};
        for (int i = 0; i < quickAmts.length; i++) {
            final int addVal = quickAmts[i];
            Button chip = button("+₹" + addVal, false);
            chip.setTextSize(11);
            chip.setMinHeight(d(34));
            chip.setPadding(d(4), d(2), d(4), d(2));
            LinearLayout.LayoutParams cLp = new LinearLayout.LayoutParams(0, d(34), 1);
            if (i < quickAmts.length - 1) cLp.setMargins(0, 0, d(4), 0);
            chipRow.addView(chip, cLp);
            chip.setOnClickListener(v -> {
                double cur = 0;
                try {
                    String txt = genAmountInput.getText().toString().trim();
                    if (!txt.isEmpty()) cur = Double.parseDouble(txt);
                } catch (Exception ignored) {}
                genAmountInput.setText(nPlain(cur + addVal));
            });
        }
        addSpace(form, 14);

        Button saveGen = button("Save general expense", true);
        form.addView(saveGen, lp(-1, 49));
        saveGen.setOnClickListener(v -> {
            try {
                double amt = number(genAmountInput, "amount spent", false);
                String note = genTitleInput.getText().toString().trim();
                if (note.isEmpty()) note = selectedCategory;
                db.addGeneralExpense(selectedGenDate, selectedCategory, note, amt);
                selectedGenDate = System.currentTimeMillis();
                toast("General expense saved");
                render();
            } catch (Exception e) {
                alert(e.getMessage() == null ? "Could not save expense." : e.getMessage());
            }
        });
        addSpace(root, 18);

        // 3. Search & Category Filter + Category-Grouped Expense History (with Edit, Delete & Readability Cap)
        LinearLayout section = col(); root.addView(section, lp(-1, -2));
        LinearLayout heading = row(); section.addView(heading, lp(-1, -2));
        heading.addView(text("Expenses by Category", 20, primary, true), new LinearLayout.LayoutParams(0, -2, 1));
        heading.addView(text(timeFilteredGen.size() + " in period", 12, secondary, false)); addSpace(section, 10);

        // Search & Category Filter Card
        LinearLayout searchCard = card(section);
        LinearLayout searchRow = row();
        searchCard.addView(searchRow, lp(-1, -2));

        EditText searchBox = edit("Search notes or category...", genSearchQuery, false);
        searchBox.setLayoutParams(new LinearLayout.LayoutParams(0, d(44), 1));
        searchRow.addView(searchBox);

        Button searchBtn = button("Filter", false);
        LinearLayout.LayoutParams sbLp = lp(68, 44); sbLp.setMargins(d(6), 0, 0, 0);
        searchRow.addView(searchBtn, sbLp);
        searchBtn.setOnClickListener(v -> {
            genSearchQuery = searchBox.getText().toString().trim();
            genHistoryExpanded = false;
            render();
        });

        // Category Quick Filter Spinner
        addSpace(searchCard, 10);
        List<String> filterCats = new ArrayList<>();
        filterCats.add("ALL CATEGORIES");
        filterCats.addAll(cats);
        Spinner catFilterSpinner = new Spinner(this);
        ArrayAdapter<String> cfAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, filterCats);
        catFilterSpinner.setAdapter(cfAdapter);
        catFilterSpinner.setBackground(shape(field, 10, 0));
        int cfIdx = genCategoryFilter.equals("ALL") ? 0 : filterCats.indexOf(genCategoryFilter);
        if (cfIdx < 0) cfIdx = 0;
        catFilterSpinner.setSelection(cfIdx);
        searchCard.addView(catFilterSpinner, lp(-1, 44));
        catFilterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            boolean init = true;
            @Override public void onItemSelected(AdapterView<?> parentView, View view, int position, long id) {
                if (init) { init = false; return; }
                genCategoryFilter = (position == 0) ? "ALL" : filterCats.get(position);
                genHistoryExpanded = false;
                render();
            }
            @Override public void onNothingSelected(AdapterView<?> parentView) {}
        });
        addSpace(section, 12);

        // Apply Search + Category Filter to build grouped map
        Map<String, List<FuelDb.GeneralExpense>> groupedByCategory = new LinkedHashMap<>();
        Map<String, Double> filteredCatTotals = new LinkedHashMap<>();
        int matchingCount = 0;
        String qLower = genSearchQuery.toLowerCase(Locale.ROOT);

        for (FuelDb.GeneralExpense g : timeFilteredGen) {
            if (!genCategoryFilter.equals("ALL") && !g.category.equalsIgnoreCase(genCategoryFilter)) continue;
            if (!qLower.isEmpty() && !g.title.toLowerCase(Locale.ROOT).contains(qLower) && !g.category.toLowerCase(Locale.ROOT).contains(qLower)) continue;
            matchingCount++;
            List<FuelDb.GeneralExpense> list = groupedByCategory.get(g.category);
            if (list == null) { list = new ArrayList<>(); groupedByCategory.put(g.category, list); }
            list.add(g);
            Double cSum = filteredCatTotals.get(g.category);
            filteredCatTotals.put(g.category, (cSum == null ? 0 : cSum) + g.amount);
        }

        if (matchingCount == 0) {
            LinearLayout empty = card(section);
            empty.addView(text("No matching expenses", 15, primary, true)); addSpace(empty, 6);
            empty.addView(text("Adjust your time/search filters above or add a new expense.", 13, secondary, false));
            return;
        }

        Map<String, Double> catBudgetMap = new LinkedHashMap<>();
        for (FuelDb.CategoryInfo ci : db.categoryInfos()) catBudgetMap.put(ci.name, ci.monthlyBudget);

        int shownCount = 0;
        int limit = genHistoryExpanded ? Integer.MAX_VALUE : HISTORY_PREVIEW_LIMIT;

        for (Map.Entry<String, List<FuelDb.GeneralExpense>> groupEntry : groupedByCategory.entrySet()) {
            if (shownCount >= limit) break;

            String catName = groupEntry.getKey();
            List<FuelDb.GeneralExpense> items = groupEntry.getValue();
            double catSum = filteredCatTotals.get(catName);

            LinearLayout groupCard = card(section);
            LinearLayout groupHeader = row();
            groupCard.addView(groupHeader, lp(-1, -2));
            groupHeader.addView(text(catName.toUpperCase(Locale.ROOT) + " (" + items.size() + ")", 12, accent, true), new LinearLayout.LayoutParams(0, -2, 1));
            groupHeader.addView(text("₹" + n(catSum), 14, primary, true));

            // Show Category Monthly Budget Bar if set
            Double cBudget = catBudgetMap.get(catName);
            Double cMonthSpent = evalMonthCatSpend.get(catName);
            if (cBudget != null && cBudget > 0) {
                renderBudgetBar(groupCard, "Monthly " + catName + " budget (" + evalYm + ")", cMonthSpent == null ? 0 : cMonthSpent, cBudget);
            }
            addSpace(groupCard, 12);

            for (int idx = 0; idx < items.size(); idx++) {
                if (shownCount >= limit) break;
                FuelDb.GeneralExpense g = items.get(idx);
                shownCount++;

                if (idx > 0) {
                    View divider = new View(this);
                    divider.setBackgroundColor(border);
                    groupCard.addView(divider, lp(-1, 1));
                    addSpace(groupCard, 10);
                }

                LinearLayout itemRow = row();
                groupCard.addView(itemRow, lp(-1, -2));

                LinearLayout left = col();
                itemRow.addView(left, new LinearLayout.LayoutParams(0, -2, 1));
                left.addView(text(g.title, 14, primary, true)); addSpace(left, 4);
                left.addView(text(date(g.time) + "  ·  ₹" + n(g.amount), 12, secondary, false));

                Button editBtn = button("Edit", false);
                editBtn.setMinHeight(d(36));
                itemRow.addView(editBtn, lp(58, 36));
                editBtn.setOnClickListener(v -> editGeneralExpenseDialog(g));

                itemRow.addView(new View(this), lp(6, 1));

                Button delete = button("Del", false);
                delete.setMinHeight(d(36));
                itemRow.addView(delete, lp(54, 36));
                delete.setOnClickListener(v -> new AlertDialog.Builder(this).setTitle("Delete expense?")
                        .setMessage("Remove '" + g.title + "' (₹" + n(g.amount) + ")?")
                        .setNegativeButton("Cancel", null)
                        .setPositiveButton("Delete", (dialog, which) -> { db.deleteGeneralExpense(g.id); render(); }).show());

                if (idx < items.size() - 1 && shownCount < limit) {
                    addSpace(groupCard, 10);
                }
            }
            addSpace(section, 12);
        }

        if (matchingCount > HISTORY_PREVIEW_LIMIT) {
            Button toggleBtn = button(
                    genHistoryExpanded ? "Show fewer entries" : "Show all " + matchingCount + " entries (" + (matchingCount - shownCount) + " hidden)",
                    false
            );
            section.addView(toggleBtn, lp(-1, 44));
            toggleBtn.setOnClickListener(v -> {
                genHistoryExpanded = !genHistoryExpanded;
                render();
            });
        }
    }

    // =========================================================================
    // TAB 2: VEHICLE EXPENSES (COST/KM, PRICE/L, BUDGETS, EDIT, SEARCH)
    // =========================================================================
    private void vehicleExpensesAndMileageTabView() {
        List<FuelDb.Vehicle> vehicles = db.vehicles();
        FuelDb.Vehicle current = selected(vehicles);

        // Vehicle Selector Card
        LinearLayout selector = card(root);
        LinearLayout pickerHeader = row(); selector.addView(pickerHeader, lp(-1, -2));
        pickerHeader.addView(text("VEHICLE", 12, secondary, true), new LinearLayout.LayoutParams(0, -2, 1));
        Button add = button("+ Add", false); pickerHeader.addView(add, lp(82, 41)); add.setOnClickListener(v -> vehicleDialog(null));
        addSpace(selector, 10);

        if (current == null) {
            selector.addView(text("Add a vehicle to start tracking mileage, fuel, and vehicle expenses.", 14, secondary, false));
            addSpace(root, 18);
            return;
        }

        Spinner spinner = new Spinner(this);
        ArrayAdapter<FuelDb.Vehicle> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, vehicles);
        spinner.setAdapter(adapter); spinner.setBackground(shape(field, 10, 0));
        int index = 0; for (int i = 0; i < vehicles.size(); i++) if (vehicles.get(i).id == current.id) index = i;
        spinner.setSelection(index); selector.addView(spinner, lp(-1, 50));
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            boolean initial = true;
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (initial) { initial = false; return; }
                selectedVehicle = vehicles.get(position).id; prefs.edit().putLong("selected_vehicle", selectedVehicle).apply(); render();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
        addSpace(selector, 11);
        LinearLayout tools = row(); selector.addView(tools, lp(-1, -2));
        String metaInfo = (current.estimate > 0 ? "Est: " + n(current.estimate) + " km/L" : "Est: not set")
                + (current.monthlyBudget > 0 ? "  ·  Budget: ₹" + n(current.monthlyBudget) + "/mo" : "");
        tools.addView(text(metaInfo, 12, secondary, false), new LinearLayout.LayoutParams(0, -2, 1));
        Button manage = button("Edit vehicle", false); tools.addView(manage, lp(112, 42)); manage.setOnClickListener(v -> vehicleDialog(current));
        addSpace(root, 18);

        // Fetch all fills and misc expenses for selected vehicle
        List<FuelDb.Fill> allFillsDesc = db.fills(current.id);
        List<FuelDb.Expense> allMiscDesc = db.expenses(current.id);

        List<Long> allTs = new ArrayList<>();
        for (FuelDb.Fill f : allFillsDesc) allTs.add(f.time);
        for (FuelDb.Expense e : allMiscDesc) allTs.add(e.time);

        // Time Filter Bar
        renderFilterBar(root, false, allTs);

        // Compute Overall Mileage across valid sequential fill pairs
        List<FuelDb.Fill> sortedFills = new ArrayList<>(allFillsDesc);
        Collections.reverse(sortedFills); // chronological: oldest -> newest

        double totalValidDist = 0;
        double totalValidFuel = 0;
        StringBuilder trendDatesJson = new StringBuilder("[");
        StringBuilder trendMileageJson = new StringBuilder("[");

        for (int i = 1; i < sortedFills.size(); i++) {
            FuelDb.Fill prev = sortedFills.get(i - 1);
            FuelDb.Fill curr = sortedFills.get(i);
            double dist = curr.odometer - prev.odometer;
            if (dist > 0 && curr.liters > 0) {
                double kml = dist / curr.liters;
                if (matchesFilter(curr.time, vehFilterMode, vehSelectedMonth, vehStartMillis, vehEndMillis)) {
                    totalValidDist += dist;
                    totalValidFuel += curr.liters;
                    if (trendDatesJson.length() > 1) {
                        trendDatesJson.append(",");
                        trendMileageJson.append(",");
                    }
                    trendDatesJson.append("'").append(date(curr.time)).append("'");
                    trendMileageJson.append(String.format(Locale.US, "%.2f", kml));
                }
            }
        }
        trendDatesJson.append("]");
        trendMileageJson.append("]");
        double overallAvgMileage = totalValidFuel > 0 ? totalValidDist / totalValidFuel : 0;

        // Filter Fills & Misc Expenses
        List<FuelDb.Fill> filteredFillsDesc = new ArrayList<>();
        List<FuelDb.Expense> filteredMiscDesc = new ArrayList<>();
        double totalFuelSpend = 0;
        double totalFuelLitersAll = 0;
        double totalMiscSpend = 0;

        String curYm = ymFmt.format(new Date());
        String evalYm = (vehFilterMode == 1) ? vehSelectedMonth : curYm;
        double evalMonthVehSpend = 0;

        TreeMap<String, double[]> monthlyMap = new TreeMap<>();
        TreeMap<String, String> monthLabels = new TreeMap<>();

        for (FuelDb.Fill f : allFillsDesc) {
            if (ymFmt.format(new Date(f.time)).equals(evalYm)) evalMonthVehSpend += f.amount;
            if (matchesFilter(f.time, vehFilterMode, vehSelectedMonth, vehStartMillis, vehEndMillis)) {
                filteredFillsDesc.add(f);
                totalFuelSpend += f.amount;
                totalFuelLitersAll += f.liters;
                Date dObj = new Date(f.time);
                String k = ymFmt.format(dObj);
                monthLabels.put(k, shortMonthFmt.format(dObj));
                double[] arr = monthlyMap.get(k);
                if (arr == null) { arr = new double[]{0, 0}; monthlyMap.put(k, arr); }
                arr[0] += f.amount;
            }
        }
        for (FuelDb.Expense e : allMiscDesc) {
            if (ymFmt.format(new Date(e.time)).equals(evalYm)) evalMonthVehSpend += e.amount;
            if (matchesFilter(e.time, vehFilterMode, vehSelectedMonth, vehStartMillis, vehEndMillis)) {
                filteredMiscDesc.add(e);
                totalMiscSpend += e.amount;
                Date dObj = new Date(e.time);
                String k = ymFmt.format(dObj);
                monthLabels.put(k, shortMonthFmt.format(dObj));
                double[] arr = monthlyMap.get(k);
                if (arr == null) { arr = new double[]{0, 0}; monthlyMap.put(k, arr); }
                arr[1] += e.amount;
            }
        }
        double grandTotalVehicleExpense = totalFuelSpend + totalMiscSpend;
        double avgFuelPricePerLiter = totalFuelLitersAll > 0 ? totalFuelSpend / totalFuelLitersAll : 0;
        double costPerKm = totalValidDist > 0 ? grandTotalVehicleExpense / totalValidDist : 0;

        // 1. Unified Overview Card: Overall Mileage + Total Expenses + Cost/km + Avg Fuel Price + Charts
        LinearLayout glance = card(root);
        glance.addView(text("MILEAGE & VEHICLE EXPENSES AT A GLANCE", 12, secondary, true)); addSpace(glance, 12);

        LinearLayout kpiRow = row();
        glance.addView(kpiRow, lp(-1, -2));

        LinearLayout leftKpi = col();
        kpiRow.addView(leftKpi, new LinearLayout.LayoutParams(0, -2, 1));
        leftKpi.addView(text("OVERALL MILEAGE", 10, secondary, true)); addSpace(leftKpi, 4);
        leftKpi.addView(text(overallAvgMileage > 0 ? n(overallAvgMileage) + " km/L" : "— km/L", 24, accent, true)); addSpace(leftKpi, 4);
        leftKpi.addView(text(n(totalValidDist) + " km  ·  " + n(totalValidFuel) + " L", 12, secondary, false));

        LinearLayout rightKpi = col();
        kpiRow.addView(rightKpi, new LinearLayout.LayoutParams(0, -2, 1));
        rightKpi.addView(text("TOTAL EXPENSES", 10, secondary, true)); addSpace(rightKpi, 4);
        rightKpi.addView(text("₹" + n(grandTotalVehicleExpense), 24, primary, true)); addSpace(rightKpi, 4);
        rightKpi.addView(text("Fuel ₹" + n(totalFuelSpend) + " · Misc ₹" + n(totalMiscSpend), 12, secondary, false));

        // Secondary Efficiency & Cost Metrics Row (Cost per km & Avg Fuel Price)
        addSpace(glance, 14);
        LinearLayout subKpiRow = row();
        subKpiRow.setPadding(d(12), d(10), d(12), d(10));
        subKpiRow.setBackground(shape(field, 12, 0));
        glance.addView(subKpiRow, lp(-1, -2));

        LinearLayout cpkCol = col();
        subKpiRow.addView(cpkCol, new LinearLayout.LayoutParams(0, -2, 1));
        cpkCol.addView(text("RUNNING COST / KM", 10, secondary, true)); addSpace(cpkCol, 3);
        cpkCol.addView(text(costPerKm > 0 ? "₹" + n(costPerKm) + " / km" : "—", 14, primary, true));

        LinearLayout pplCol = col();
        subKpiRow.addView(pplCol, new LinearLayout.LayoutParams(0, -2, 1));
        pplCol.addView(text("AVG FUEL PRICE", 10, secondary, true)); addSpace(pplCol, 3);
        pplCol.addView(text(avgFuelPricePerLiter > 0 ? "₹" + n(avgFuelPricePerLiter) + " / L" : "—", 14, primary, true));

        if (overallAvgMileage > 0 && current.estimate > 0) {
            addSpace(glance, 10);
            double delta = (overallAvgMileage - current.estimate) / current.estimate * 100;
            glance.addView(text("Vs default estimate (" + n(current.estimate) + " km/L): " + (delta >= 0 ? "+" : "") + n(delta) + "%", 12, primary, true));
        }

        // Vehicle Monthly Budget Bar if configured
        renderBudgetBar(glance, "VEHICLE MONTHLY BUDGET (" + evalYm + ")", evalMonthVehSpend, current.monthlyBudget);

        // Offline Interactive Graph Switcher (Mileage Trend vs Expense Breakdown)
        if (overallAvgMileage > 0 || !monthlyMap.isEmpty()) {
            addSpace(glance, 16);

            StringBuilder monthsJson = new StringBuilder("[");
            StringBuilder fuelExpJson = new StringBuilder("[");
            StringBuilder miscExpJson = new StringBuilder("[");
            for (Map.Entry<String, double[]> entry : monthlyMap.entrySet()) {
                if (monthsJson.length() > 1) {
                    monthsJson.append(",");
                    fuelExpJson.append(",");
                    miscExpJson.append(",");
                }
                monthsJson.append("'").append(monthLabels.get(entry.getKey())).append("'");
                fuelExpJson.append(String.format(Locale.US, "%.2f", entry.getValue()[0]));
                miscExpJson.append(String.format(Locale.US, "%.2f", entry.getValue()[1]));
            }
            monthsJson.append("]");
            fuelExpJson.append("]");
            miscExpJson.append("]");

            WebView chartWeb = new WebView(this);
            chartWeb.setLayoutParams(lp(-1, 255));
            WebSettings settings = chartWeb.getSettings();
            settings.setJavaScriptEnabled(true);
            settings.setAllowFileAccess(true);

            String bgHex = dark ? "#1B2225" : "#FFFFFF";
            String txtHex = dark ? "#A2B1B4" : "#64777D";
            String accHex = dark ? "#65DCC7" : "#0D796A";
            String tabBg = dark ? "#263034" : "#E7F6F1";
            String activeTxt = dark ? "#09251E" : "#FFFFFF";

            String defaultChart = overallAvgMileage > 0 ? "mileage" : "expense";

            String html = "<!DOCTYPE html><html><head>" +
                    "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                    "<script src='file:///android_asset/chart.min.js'></script>" +
                    "<style>" +
                    "body { background-color: " + bgHex + "; margin: 0; padding: 0; font-family: sans-serif; }" +
                    ".tabs { display: flex; gap: 6px; margin-bottom: 8px; }" +
                    "button { flex: 1; background: " + tabBg + "; color: " + accHex + "; border: none; padding: 7px; border-radius: 8px; font-size: 11px; font-weight: bold; }" +
                    "button.active { background: " + accHex + "; color: " + activeTxt + "; }" +
                    ".box { position: relative; height: 210px; width: 100%; }" +
                    "</style></head><body>" +
                    "<div class='tabs'>" +
                    "<button id='b1' onclick='renderChart(\"mileage\")'>Mileage Trend</button>" +
                    "<button id='b2' onclick='renderChart(\"expense\")'>Expense Breakdown</button>" +
                    "</div>" +
                    "<div class='box'><canvas id='vc'></canvas></div>" +
                    "<script>" +
                    "const tLabels = " + trendDatesJson.toString() + ";" +
                    "const tData = " + trendMileageJson.toString() + ";" +
                    "const avgLine = tLabels.map(() => " + String.format(Locale.US, "%.2f", overallAvgMileage) + ");" +
                    "const mLabels = " + monthsJson.toString() + ";" +
                    "const fData = " + fuelExpJson.toString() + ";" +
                    "const miscData = " + miscExpJson.toString() + ";" +
                    "let ctx = document.getElementById('vc').getContext('2d');" +
                    "let chart = null;" +
                    "function renderChart(type) {" +
                    "  document.getElementById('b1').classList.toggle('active', type === 'mileage');" +
                    "  document.getElementById('b2').classList.toggle('active', type === 'expense');" +
                    "  if (chart) chart.destroy();" +
                    "  if (type === 'mileage') {" +
                    "    chart = new Chart(ctx, { type: 'line', data: { labels: tLabels, datasets: [" +
                    "      { label: 'Mileage (km/L)', data: tData, borderColor: '" + accHex + "', backgroundColor: '" + accHex + "', tension: 0.25, borderWidth: 2.5, pointRadius: 4 }," +
                    "      { label: 'Overall Avg', data: avgLine, borderColor: '#f59e0b', borderDash: [5, 5], borderWidth: 1.5, pointRadius: 0 }" +
                    "    ] }, options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { labels: { color: '" + txtHex + "', font: { size: 11 } } } }, scales: { x: { ticks: { color: '" + txtHex + "', font: { size: 10 } } }, y: { ticks: { color: '" + txtHex + "', font: { size: 10 } } } } } });" +
                    "  } else {" +
                    "    chart = new Chart(ctx, { type: 'bar', data: { labels: mLabels, datasets: [" +
                    "      { label: 'Fuel (₹)', data: fData, backgroundColor: '" + accHex + "' }," +
                    "      { label: 'Misc (₹)', data: miscData, backgroundColor: '#f59e0b' }" +
                    "    ] }, options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { labels: { color: '" + txtHex + "', font: { size: 11 } } } }, scales: { x: { stacked: true, ticks: { color: '" + txtHex + "', font: { size: 10 } } }, y: { stacked: true, ticks: { color: '" + txtHex + "', font: { size: 10 } } } } } });" +
                    "  }" +
                    "}" +
                    "renderChart('" + defaultChart + "');" +
                    "</script></body></html>";

            chartWeb.loadDataWithBaseURL("file:///android_asset/", html, "text/html", "UTF-8", null);
            glance.addView(chartWeb);
        }
        addSpace(root, 18);

        // 2. New Fill-up Form (with Date Picker)
        LinearLayout form = card(root);
        form.addView(text("New fuel fill-up", 20, primary, true)); addSpace(form, 6);
        form.addView(text("Record fuel purchase, date, and odometer.", 13, secondary, false)); addSpace(form, 16);

        form.addView(text("DATE OF FILL-UP", 12, secondary, true)); addSpace(form, 7);
        Button dateBtn = button(date(selectedFillDate), false);
        dateBtn.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        dateBtn.setPadding(d(14), 0, d(14), 0);
        form.addView(dateBtn, lp(-1, 50));
        dateBtn.setOnClickListener(v -> pickDate(selectedFillDate, millis -> {
            selectedFillDate = millis;
            dateBtn.setText(date(selectedFillDate));
        }));
        addSpace(form, 14);

        odoInput = input(form, "ODOMETER (KM)", "e.g. 18540", "", true);
        litersInput = input(form, "FUEL QUANTITY (LITRES)", "e.g. 25.5", "", true);
        amountInput = input(form, "AMOUNT SPENT (₹)", "e.g. 2500", "", true);
        estimateInput = input(form, "ESTIMATED MILEAGE (KM/L) · OPTIONAL", "e.g. 18", current.estimate > 0 ? nPlain(current.estimate) : "", true);
        Button save = button("Save fill-up", true); form.addView(save, lp(-1, 49)); save.setOnClickListener(v -> saveFill(current));
        addSpace(root, 18);

        // 3. Add Vehicle Misc Expense Form
        LinearLayout miscForm = card(root);
        miscForm.addView(text("New vehicle misc expense", 20, primary, true)); addSpace(miscForm, 6);
        miscForm.addView(text("Track maintenance, insurance, tolls, car wash, or accessories.", 13, secondary, false)); addSpace(miscForm, 16);

        miscForm.addView(text("DATE OF EXPENSE", 12, secondary, true)); addSpace(miscForm, 7);
        Button expDateBtn = button(date(selectedExpenseDate), false);
        expDateBtn.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        expDateBtn.setPadding(d(14), 0, d(14), 0);
        miscForm.addView(expDateBtn, lp(-1, 50));
        expDateBtn.setOnClickListener(v -> pickDate(selectedExpenseDate, millis -> {
            selectedExpenseDate = millis;
            expDateBtn.setText(date(selectedExpenseDate));
        }));
        addSpace(miscForm, 14);

        expTitleInput = input(miscForm, "EXPENSE DESCRIPTION", "e.g. Oil Change / Insurance / Toll", "", false);
        expAmountInput = input(miscForm, "AMOUNT SPENT (₹)", "e.g. 1200", "", true);

        Button saveExp = button("Save vehicle expense", true);
        miscForm.addView(saveExp, lp(-1, 49));
        saveExp.setOnClickListener(v -> {
            try {
                String title = expTitleInput.getText().toString().trim();
                if (title.isEmpty()) throw new IllegalArgumentException("Enter an expense description.");
                double amt = number(expAmountInput, "amount spent", false);
                db.addExpense(current.id, selectedExpenseDate, title, amt);
                selectedExpenseDate = System.currentTimeMillis();
                toast("Vehicle expense saved"); render();
            } catch (Exception e) {
                alert(e.getMessage() == null ? "Could not save expense." : e.getMessage());
            }
        });
        addSpace(root, 18);

        // 4. Fill-up History (with Edit, Delete & Readability Cap)
        LinearLayout fillSection = col(); root.addView(fillSection, lp(-1, -2));
        LinearLayout fHeading = row(); fillSection.addView(fHeading, lp(-1, -2));
        fHeading.addView(text("Fill-up history", 20, primary, true), new LinearLayout.LayoutParams(0, -2, 1));
        fHeading.addView(text(filteredFillsDesc.size() + " entries", 12, secondary, false)); addSpace(fillSection, 12);

        if (filteredFillsDesc.isEmpty()) {
            LinearLayout empty = card(fillSection);
            empty.addView(text("No fill-ups in selected period", 15, primary, true)); addSpace(empty, 6);
            empty.addView(text("Adjust your filter above or save a fill-up.", 13, secondary, false));
        } else {
            int fLimit = fillHistoryExpanded ? filteredFillsDesc.size() : Math.min(filteredFillsDesc.size(), HISTORY_PREVIEW_LIMIT);
            for (int i = 0; i < fLimit; i++) {
                FuelDb.Fill f = filteredFillsDesc.get(i);
                int fullIdx = allFillsDesc.indexOf(f);
                FuelDb.Fill prev = (fullIdx >= 0 && fullIdx + 1 < allFillsDesc.size()) ? allFillsDesc.get(fullIdx + 1) : null;

                LinearLayout item = card(fillSection);
                LinearLayout titleRow = row(); item.addView(titleRow, lp(-1, -2));
                LinearLayout left = col(); titleRow.addView(left, new LinearLayout.LayoutParams(0, -2, 1));
                left.addView(text(date(f.time), 14, primary, true)); addSpace(left, 5);
                left.addView(text(n(f.odometer) + " km on odometer", 12, secondary, false));

                Button editFillBtn = button("Edit", false);
                editFillBtn.setMinHeight(d(36));
                titleRow.addView(editFillBtn, lp(58, 36));
                editFillBtn.setOnClickListener(v -> editFillDialog(f));

                titleRow.addView(new View(this), lp(6, 1));

                Button delete = button("Del", false);
                delete.setMinHeight(d(36));
                titleRow.addView(delete, lp(54, 36));
                delete.setOnClickListener(v -> new AlertDialog.Builder(this).setTitle("Delete fill-up?")
                        .setMessage("This cannot be undone. Mileage will be recalculated from the remaining entries.")
                        .setNegativeButton("Cancel", null).setPositiveButton("Delete", (dialog, which) -> { db.deleteFill(f.id); render(); }).show());
                addSpace(item, 14);
                LinearLayout nums = row(); item.addView(nums, lp(-1, -2));
                metric(nums, "FUEL", n(f.liters) + " L"); metric(nums, "SPENT", "₹" + n(f.amount));
                metric(nums, "ACTUAL", prev == null ? "—" : n((f.odometer - prev.odometer) / f.liters) + " km/L");
                addSpace(fillSection, 10);
            }
            if (filteredFillsDesc.size() > HISTORY_PREVIEW_LIMIT) {
                Button toggleFills = button(
                        fillHistoryExpanded ? "Show fewer fill-ups" : "Show all " + filteredFillsDesc.size() + " fill-ups (" + (filteredFillsDesc.size() - fLimit) + " hidden)",
                        false
                );
                fillSection.addView(toggleFills, lp(-1, 44));
                toggleFills.setOnClickListener(v -> { fillHistoryExpanded = !fillHistoryExpanded; render(); });
            }
        }
        addSpace(root, 18);

        // 5. Vehicle Misc Expense History (with Search, Edit, Delete & Readability Cap)
        LinearLayout miscSection = col(); root.addView(miscSection, lp(-1, -2));
        LinearLayout mHeading = row(); miscSection.addView(mHeading, lp(-1, -2));
        mHeading.addView(text("Vehicle misc history", 20, primary, true), new LinearLayout.LayoutParams(0, -2, 1));
        mHeading.addView(text(filteredMiscDesc.size() + " in period", 12, secondary, false)); addSpace(miscSection, 10);

        // Search Bar for Vehicle Misc History
        LinearLayout mSearchCard = card(miscSection);
        LinearLayout mSearchRow = row();
        mSearchCard.addView(mSearchRow, lp(-1, -2));
        EditText mSearchBox = edit("Search service, toll, insurance...", vehSearchQuery, false);
        mSearchBox.setLayoutParams(new LinearLayout.LayoutParams(0, d(44), 1));
        mSearchRow.addView(mSearchBox);
        Button mSearchBtn = button("Filter", false);
        LinearLayout.LayoutParams msbLp = lp(68, 44); msbLp.setMargins(d(6), 0, 0, 0);
        mSearchRow.addView(mSearchBtn, msbLp);
        mSearchBtn.setOnClickListener(v -> {
            vehSearchQuery = mSearchBox.getText().toString().trim();
            vehMiscHistoryExpanded = false;
            render();
        });
        addSpace(miscSection, 12);

        List<FuelDb.Expense> searchedMiscDesc = new ArrayList<>();
        String vqLower = vehSearchQuery.toLowerCase(Locale.ROOT);
        for (FuelDb.Expense e : filteredMiscDesc) {
            if (vqLower.isEmpty() || e.title.toLowerCase(Locale.ROOT).contains(vqLower)) {
                searchedMiscDesc.add(e);
            }
        }

        if (searchedMiscDesc.isEmpty()) {
            LinearLayout empty = card(miscSection);
            empty.addView(text("No matching vehicle misc expenses", 15, primary, true)); addSpace(empty, 6);
            empty.addView(text("Adjust your filter/search or add maintenance/toll expenses above.", 13, secondary, false));
        } else {
            int mLimit = vehMiscHistoryExpanded ? searchedMiscDesc.size() : Math.min(searchedMiscDesc.size(), HISTORY_PREVIEW_LIMIT);
            for (int i = 0; i < mLimit; i++) {
                FuelDb.Expense e = searchedMiscDesc.get(i);
                LinearLayout item = card(miscSection);
                LinearLayout titleRow = row(); item.addView(titleRow, lp(-1, -2));
                LinearLayout left = col(); titleRow.addView(left, new LinearLayout.LayoutParams(0, -2, 1));
                left.addView(text(e.title, 15, primary, true)); addSpace(left, 4);
                left.addView(text(date(e.time) + "  ·  ₹" + n(e.amount), 13, accent, true));

                Button editMiscBtn = button("Edit", false);
                editMiscBtn.setMinHeight(d(36));
                titleRow.addView(editMiscBtn, lp(58, 36));
                editMiscBtn.setOnClickListener(v -> editVehicleExpenseDialog(e));

                titleRow.addView(new View(this), lp(6, 1));

                Button delete = button("Del", false);
                delete.setMinHeight(d(36));
                titleRow.addView(delete, lp(54, 36));
                delete.setOnClickListener(v -> new AlertDialog.Builder(this).setTitle("Delete expense?")
                        .setMessage("Remove '" + e.title + "' (₹" + n(e.amount) + ")?")
                        .setNegativeButton("Cancel", null)
                        .setPositiveButton("Delete", (dialog, which) -> { db.deleteExpense(e.id); render(); }).show());
                addSpace(miscSection, 10);
            }
            if (searchedMiscDesc.size() > HISTORY_PREVIEW_LIMIT) {
                Button toggleMisc = button(
                        vehMiscHistoryExpanded ? "Show fewer misc expenses" : "Show all " + searchedMiscDesc.size() + " misc expenses (" + (searchedMiscDesc.size() - mLimit) + " hidden)",
                        false
                );
                miscSection.addView(toggleMisc, lp(-1, 44));
                toggleMisc.setOnClickListener(v -> { vehMiscHistoryExpanded = !vehMiscHistoryExpanded; render(); });
            }
        }
    }

    // ==========================================
    // EDIT DIALOGS & CATEGORY / BUDGET MANAGERS
    // ==========================================
    private void editGeneralExpenseDialog(FuelDb.GeneralExpense item) {
        LinearLayout content = col(); content.setPadding(d(22), d(12), d(22), 0);
        final long[] chosenDate = {item.time};

        content.addView(text("CATEGORY", 12, secondary, true)); addSpace(content, 6);
        List<String> cats = db.categories();
        Spinner sp = new Spinner(this);
        sp.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, cats));
        sp.setBackground(shape(field, 10, 0));
        int idx = cats.indexOf(item.category);
        if (idx >= 0) sp.setSelection(idx);
        content.addView(sp, lp(-1, 48));
        addSpace(content, 12);

        content.addView(text("DATE", 12, secondary, true)); addSpace(content, 6);
        Button dBtn = button(date(chosenDate[0]), false);
        content.addView(dBtn, lp(-1, 46));
        dBtn.setOnClickListener(v -> pickDate(chosenDate[0], m -> { chosenDate[0] = m; dBtn.setText(date(m)); }));
        addSpace(content, 12);

        EditText noteEd = input(content, "DESCRIPTION / NOTE", "Note", item.title, false);
        EditText amtEd = input(content, "AMOUNT SPENT (₹)", "Amount", nPlain(item.amount), true);

        new AlertDialog.Builder(this)
                .setTitle("Edit General Expense")
                .setView(content)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", (d, w) -> {
                    try {
                        double amt = number(amtEd, "amount", false);
                        String cat = cats.get(sp.getSelectedItemPosition());
                        String note = noteEd.getText().toString().trim();
                        if (note.isEmpty()) note = cat;
                        db.updateGeneralExpense(item.id, chosenDate[0], cat, note, amt);
                        toast("Expense updated");
                        render();
                    } catch (Exception e) {
                        alert(e.getMessage() == null ? "Invalid input." : e.getMessage());
                    }
                }).show();
    }

    private void editFillDialog(FuelDb.Fill item) {
        LinearLayout content = col(); content.setPadding(d(22), d(12), d(22), 0);
        final long[] chosenDate = {item.time};

        content.addView(text("DATE OF FILL-UP", 12, secondary, true)); addSpace(content, 6);
        Button dBtn = button(date(chosenDate[0]), false);
        content.addView(dBtn, lp(-1, 46));
        dBtn.setOnClickListener(v -> pickDate(chosenDate[0], m -> { chosenDate[0] = m; dBtn.setText(date(m)); }));
        addSpace(content, 12);

        EditText odoEd = input(content, "ODOMETER (KM)", "Odometer", nPlain(item.odometer), true);
        EditText litEd = input(content, "FUEL QUANTITY (LITRES)", "Litres", nPlain(item.liters), true);
        EditText amtEd = input(content, "AMOUNT SPENT (₹)", "Amount", nPlain(item.amount), true);

        new AlertDialog.Builder(this)
                .setTitle("Edit Fill-up")
                .setView(content)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", (d, w) -> {
                    try {
                        double odo = number(odoEd, "odometer", true);
                        double lit = number(litEd, "fuel quantity", false);
                        double amt = number(amtEd, "amount spent", true);
                        db.updateFill(item.id, chosenDate[0], odo, lit, amt, item.estimate);
                        toast("Fill-up updated");
                        render();
                    } catch (Exception e) {
                        alert("Could not update fill-up. Ensure odometer is unique for this vehicle.");
                    }
                }).show();
    }

    private void editVehicleExpenseDialog(FuelDb.Expense item) {
        LinearLayout content = col(); content.setPadding(d(22), d(12), d(22), 0);
        final long[] chosenDate = {item.time};

        content.addView(text("DATE OF EXPENSE", 12, secondary, true)); addSpace(content, 6);
        Button dBtn = button(date(chosenDate[0]), false);
        content.addView(dBtn, lp(-1, 46));
        dBtn.setOnClickListener(v -> pickDate(chosenDate[0], m -> { chosenDate[0] = m; dBtn.setText(date(m)); }));
        addSpace(content, 12);

        EditText titleEd = input(content, "DESCRIPTION", "Description", item.title, false);
        EditText amtEd = input(content, "AMOUNT SPENT (₹)", "Amount", nPlain(item.amount), true);

        new AlertDialog.Builder(this)
                .setTitle("Edit Vehicle Expense")
                .setView(content)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", (d, w) -> {
                    try {
                        String title = titleEd.getText().toString().trim();
                        if (title.isEmpty()) throw new IllegalArgumentException("Enter description.");
                        double amt = number(amtEd, "amount", false);
                        db.updateExpense(item.id, chosenDate[0], title, amt);
                        toast("Vehicle expense updated");
                        render();
                    } catch (Exception e) {
                        alert(e.getMessage() == null ? "Invalid input." : e.getMessage());
                    }
                }).show();
    }

    private void generalBudgetDialog() {
        LinearLayout content = col(); content.setPadding(d(22), d(12), d(22), 0);
        float curBudget = prefs.getFloat("gen_monthly_budget", 0f);
        EditText bEd = input(content, "MONTHLY OVERALL GENERAL BUDGET (₹) · 0 TO DISABLE", "e.g. 15000", curBudget > 0 ? nPlain(curBudget) : "", true);

        new AlertDialog.Builder(this)
                .setTitle("Monthly General Budget")
                .setView(content)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", (d, w) -> {
                    try {
                        String txt = bEd.getText().toString().trim();
                        double val = txt.isEmpty() ? 0 : number(bEd, "budget", true);
                        prefs.edit().putFloat("gen_monthly_budget", (float) val).apply();
                        toast(val > 0 ? "Monthly budget saved" : "Monthly budget disabled");
                        render();
                    } catch (Exception e) {
                        alert(e.getMessage() == null ? "Invalid budget." : e.getMessage());
                    }
                }).show();
    }

    private void manageCategoriesDialog() {
        List<FuelDb.CategoryInfo> infos = db.categoryInfos();
        String[] labels = new String[infos.size()];
        for (int i = 0; i < infos.size(); i++) {
            FuelDb.CategoryInfo ci = infos.get(i);
            labels[i] = ci.name
                    + (ci.isDefault ? " (Default)" : " (Custom)")
                    + (ci.monthlyBudget > 0 ? " · Budget: ₹" + n(ci.monthlyBudget) + "/mo" : "");
        }
        new AlertDialog.Builder(this)
                .setTitle("Manage Categories & Budgets")
                .setItems(labels, (dialog, which) -> {
                    FuelDb.CategoryInfo chosen = infos.get(which);
                    categoryActionDialog(chosen);
                })
                .setNegativeButton("Close", null)
                .show();
    }

    private void categoryActionDialog(FuelDb.CategoryInfo ci) {
        LinearLayout content = col(); content.setPadding(d(22), d(12), d(22), 0);
        EditText nameEd = null;
        if (!ci.isDefault) {
            nameEd = input(content, "CATEGORY NAME", "Category name", ci.name, false);
        } else {
            content.addView(text("Built-in default category", 12, secondary, false));
            addSpace(content, 12);
        }
        EditText budEd = input(content, "MONTHLY CATEGORY BUDGET (₹) · OPTIONAL", "e.g. 4000", ci.monthlyBudget > 0 ? nPlain(ci.monthlyBudget) : "", true);

        final EditText finalNameEd = nameEd;
        AlertDialog.Builder b = new AlertDialog.Builder(this)
                .setTitle(ci.name)
                .setView(content)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", (d, w) -> {
                    try {
                        String bTxt = budEd.getText().toString().trim();
                        double bVal = bTxt.isEmpty() ? 0 : number(budEd, "budget", true);
                        String targetName = ci.name;
                        if (finalNameEd != null) {
                            String newNm = finalNameEd.getText().toString().trim();
                            if (!newNm.isEmpty() && !newNm.equals(ci.name)) {
                                db.renameCategory(ci.name, newNm);
                                if (selectedCategory.equalsIgnoreCase(ci.name)) selectedCategory = newNm;
                                targetName = newNm;
                            }
                        }
                        db.setCategoryBudget(targetName, bVal);
                        toast("Category updated");
                        render();
                    } catch (Exception e) {
                        alert(e.getMessage() == null ? "Could not update category." : e.getMessage());
                    }
                });
        if (!ci.isDefault) {
            b.setNeutralButton("Delete", (d, w) -> {
                new AlertDialog.Builder(this)
                        .setTitle("Delete '" + ci.name + "'?")
                        .setMessage("Existing entries in this category will be moved to 'Other'.")
                        .setNegativeButton("Cancel", null)
                        .setPositiveButton("Delete", (x, y) -> {
                            db.deleteCustomCategory(ci.name);
                            if (selectedCategory.equalsIgnoreCase(ci.name)) selectedCategory = "Other";
                            toast("Category deleted");
                            render();
                        }).show();
            });
        }
        b.show();
    }

    private String nPlain(double x) {
        if (x == (long) x) return String.valueOf((long) x);
        return String.valueOf(x);
    }
    private void saveFill(FuelDb.Vehicle vehicle) {
        try {
            double odo = number(odoInput, "odometer", true);
            double liters = number(litersInput, "fuel quantity", false);
            double amount = number(amountInput, "amount spent", true);
            String estimateText = estimateInput.getText().toString().trim();
            double estimate = estimateText.isEmpty() ? 0 : number(estimateInput, "estimated mileage", true);
            FuelDb.Fill last = db.newest(vehicle.id);
            if (last != null && odo <= last.odometer) throw new IllegalArgumentException("Odometer must be above the previous reading (" + n(last.odometer) + " km).");
            db.addFill(vehicle.id, selectedFillDate, odo, liters, amount, estimate);
            if (estimate != vehicle.estimate) db.updateVehicle(vehicle.id, vehicle.name, estimate, vehicle.monthlyBudget);
            selectedFillDate = System.currentTimeMillis();
            toast("Fill-up saved"); render();
        } catch (Exception e) { alert(e.getMessage() == null ? "Could not save fill-up." : e.getMessage()); }
    }

    private void metric(LinearLayout row, String label, String value) {
        LinearLayout block = col(); row.addView(block, new LinearLayout.LayoutParams(0, -2, 1));
        block.addView(text(label, 10, secondary, true)); addSpace(block, 6);
        block.addView(text(value, 14, primary, true));
    }

    // Data Card: CSV Export/Import (with Pre-Import Preview) + Raw SQLite Snapshot Backup
    private void filesCard() {
        LinearLayout card = card(root);
        card.addView(text("Your data & backups", 20, primary, true)); addSpace(card, 7);
        card.addView(text("Export or import CSV records, or save a full SQLite database snapshot.", 13, secondary, false)); addSpace(card, 16);

        LinearLayout actions = row(); card.addView(actions, lp(-1, -2));
        Button importBtn = button("Import CSV", false), exportBtn = button("Export CSV", true);
        LinearLayout.LayoutParams a = new LinearLayout.LayoutParams(0, d(46), 1); a.setMargins(0, 0, d(6), 0);
        LinearLayout.LayoutParams b = new LinearLayout.LayoutParams(0, d(46), 1); b.setMargins(d(6), 0, 0, 0);
        actions.addView(importBtn, a); actions.addView(exportBtn, b);

        addSpace(card, 10);
        Button dbBackupBtn = button("Export SQLite DB Snapshot (.db)", false);
        card.addView(dbBackupBtn, lp(-1, 44));

        importBtn.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE); intent.setType("*/*");
            intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"text/csv", "text/comma-separated-values", "text/plain", "application/csv", "application/vnd.ms-excel"});
            startActivityForResult(intent, REQUEST_IMPORT);
        });
        exportBtn.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE); intent.setType("text/csv");
            intent.putExtra(Intent.EXTRA_TITLE, "fuelbook-export.csv");
            startActivityForResult(intent, REQUEST_EXPORT);
        });
        dbBackupBtn.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE); intent.setType("application/octet-stream");
            intent.putExtra(Intent.EXTRA_TITLE, "fuelbook-backup.db");
            startActivityForResult(intent, REQUEST_BACKUP_DB);
        });
    }

    private void vehicleDialog(FuelDb.Vehicle vehicle) {
        LinearLayout content = col(); content.setPadding(d(22), d(10), d(22), 0);
        content.addView(text("VEHICLE NAME", 12, secondary, true)); addSpace(content, 7);
        EditText name = edit("e.g. City / Activa", vehicle == null ? "" : vehicle.name, false);
        content.addView(name); addSpace(content, 14);
        content.addView(text("DEFAULT ESTIMATED MILEAGE (KM/L)", 12, secondary, true)); addSpace(content, 7);
        EditText estimate = edit("Optional", vehicle != null && vehicle.estimate > 0 ? nPlain(vehicle.estimate) : "", true);
        content.addView(estimate); addSpace(content, 14);
        content.addView(text("MONTHLY VEHICLE BUDGET (₹) · OPTIONAL", 12, secondary, true)); addSpace(content, 7);
        EditText budgetEd = edit("e.g. 5000", vehicle != null && vehicle.monthlyBudget > 0 ? nPlain(vehicle.monthlyBudget) : "", true);
        content.addView(budgetEd);

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(vehicle == null ? "Add vehicle" : "Edit vehicle")
                .setView(content)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", null);
        if (vehicle != null) builder.setNeutralButton("Delete", null);
        AlertDialog dialog = builder.create(); dialog.setOnShowListener(ignored -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                try {
                    String label = name.getText().toString().trim();
                    if (label.isEmpty()) throw new IllegalArgumentException("Enter a vehicle name.");
                    double est = estimate.getText().toString().trim().isEmpty() ? 0 : number(estimate, "estimated mileage", true);
                    double bud = budgetEd.getText().toString().trim().isEmpty() ? 0 : number(budgetEd, "monthly budget", true);
                    if (vehicle == null) selectedVehicle = db.addVehicle(label, est, bud);
                    else { db.updateVehicle(vehicle.id, label, est, bud); selectedVehicle = vehicle.id; }
                    prefs.edit().putLong("selected_vehicle", selectedVehicle).apply();
                    dialog.dismiss(); render();
                } catch (Exception e) { alert("Could not save vehicle. Check that its name is unique and numbers are valid."); }
            });
            if (vehicle != null) dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
                new AlertDialog.Builder(this).setTitle("Delete " + vehicle.name + "?")
                        .setMessage("This will permanently delete the vehicle and all its fill-up entries.")
                        .setNegativeButton("Cancel", null)
                        .setPositiveButton("Delete", (x, y) -> {
                            db.deleteVehicle(vehicle.id); selectedVehicle = -1;
                            prefs.edit().remove("selected_vehicle").apply(); dialog.dismiss(); render();
                        }).show();
            });
        }); dialog.show();
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        if (requestCode == REQUEST_EXPORT) {
            try (OutputStreamWriter writer = new OutputStreamWriter(getContentResolver().openOutputStream(uri, "wt"), StandardCharsets.UTF_8)) {
                CsvIO.export(db, writer); toast("CSV exported successfully");
            } catch (Exception e) { alert("Export failed: " + e.getMessage()); }
        } else if (requestCode == REQUEST_BACKUP_DB) {
            try {
                db.close();
                java.io.File dbFile = getDatabasePath("fuelbook.db");
                try (InputStream in = new java.io.FileInputStream(dbFile);
                     OutputStream out = getContentResolver().openOutputStream(uri, "wt")) {
                    byte[] buf = new byte[4096];
                    int len;
                    while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
                }
                db = new FuelDb(this);
                toast("SQLite database backup saved");
            } catch (Exception e) {
                db = new FuelDb(this);
                alert("Database backup failed: " + e.getMessage());
            }
        } else if (requestCode == REQUEST_IMPORT) {
            // Confirm before executing transactional CSV import
            new AlertDialog.Builder(this)
                    .setTitle("Import CSV Data?")
                    .setMessage("This will transactionally merge vehicles, fill-ups, vehicle expenses, and general expenses from the selected CSV file (duplicate fill-ups are automatically skipped).")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Import Now", (dialog, which) -> {
                        try (InputStreamReader reader = new InputStreamReader(getContentResolver().openInputStream(uri), StandardCharsets.UTF_8)) {
                            CsvIO.Result result = CsvIO.importCsv(db, reader);
                            render();
                            alert("Import complete:\n• " + result.generalExpenses + " general expenses\n• " + result.vehicles + " new vehicles\n• " + result.fills + " new fill-ups\n• " + result.expenses + " vehicle expenses\n• " + result.duplicates + " duplicates skipped");
                        } catch (Exception e) {
                            alert("Import failed (nothing was changed): " + e.getMessage());
                        }
                    }).show();
        }
    }
}
