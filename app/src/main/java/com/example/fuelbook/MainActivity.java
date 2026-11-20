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
import android.text.InputType;
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
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import java.io.InputStreamReader;
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
    private static final int REQUEST_IMPORT = 501, REQUEST_EXPORT = 502;
    private FuelDb db;
    private SharedPreferences prefs;
    private boolean dark;
    private int bg, panel, primary, secondary, accent, tint, border, field;
    private long selectedVehicle = -1;
    private int activeTab = 0; // 0 = General Expenses (Tab 1), 1 = Mileage & Fill-ups (Tab 2), 2 = Vehicle Expenses (Tab 3)
    private long selectedGenDate = System.currentTimeMillis();
    private long selectedFillDate = System.currentTimeMillis();
    private long selectedExpenseDate = System.currentTimeMillis();
    private String selectedCategory = "Food";

    private LinearLayout root;
    private EditText odoInput, litersInput, amountInput, estimateInput;
    private EditText expTitleInput, expAmountInput;
    private EditText genTitleInput, genAmountInput;
    private final DecimalFormat fmt = new DecimalFormat("#,##0.##");

    @Override public void onCreate(Bundle state) {
        prefs = getSharedPreferences("settings", MODE_PRIVATE);
        dark = prefs.getBoolean("dark", false);
        setTheme(dark ? R.style.AppTheme_Dark : R.style.AppTheme);
        super.onCreate(state);
        db = new FuelDb(this);
        selectedVehicle = prefs.getLong("selected_vehicle", -1);
        colors(); render();
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
        v.setPadding(d(8), d(6), d(8), d(6)); v.setMinHeight(d(46)); return v;
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
        names.addView(text("Personal & vehicle expense manager.", 13, secondary, false));
        Button theme = button(dark ? "☀ Light" : "☾ Dark", false); header.addView(theme, lp(88, 44));
        theme.setOnClickListener(v -> { prefs.edit().putBoolean("dark", !dark).apply(); recreate(); });
        addSpace(root, 18);

        // 3-Tab Navigation Bar: Tab 1 = General Expenses, Tab 2 = Mileage, Tab 3 = Vehicle Expenses
        LinearLayout tabBar = row();
        tabBar.setPadding(d(4), d(4), d(4), d(4));
        tabBar.setBackground(shape(panel, 14, border));
        root.addView(tabBar, lp(-1, -2));

        Button tabGeneral = button("General", activeTab == 0);
        Button tabMileage = button("Mileage", activeTab == 1);
        Button tabVehExp = button("Vehicle Exp.", activeTab == 2);

        LinearLayout.LayoutParams tLp1 = new LinearLayout.LayoutParams(0, d(44), 1); tLp1.setMargins(0, 0, d(3), 0);
        LinearLayout.LayoutParams tLp2 = new LinearLayout.LayoutParams(0, d(44), 1); tLp2.setMargins(d(3), 0, d(3), 0);
        LinearLayout.LayoutParams tLp3 = new LinearLayout.LayoutParams(0, d(44), 1); tLp3.setMargins(d(3), 0, 0, 0);

        tabBar.addView(tabGeneral, tLp1);
        tabBar.addView(tabMileage, tLp2);
        tabBar.addView(tabVehExp, tLp3);

        tabGeneral.setOnClickListener(v -> { if (activeTab != 0) { activeTab = 0; render(); } });
        tabMileage.setOnClickListener(v -> { if (activeTab != 1) { activeTab = 1; render(); } });
        tabVehExp.setOnClickListener(v -> { if (activeTab != 2) { activeTab = 2; render(); } });
        addSpace(root, 18);

        if (activeTab == 0) {
            generalExpensesTabView();
        } else {
            // Vehicle Selector shown on Vehicle-specific tabs (Tab 2 & Tab 3)
            List<FuelDb.Vehicle> vehicles = db.vehicles();
            FuelDb.Vehicle current = selected(vehicles);
            LinearLayout selector = card(root);
            LinearLayout pickerHeader = row(); selector.addView(pickerHeader, lp(-1, -2));
            pickerHeader.addView(text("VEHICLE", 12, secondary, true), new LinearLayout.LayoutParams(0, -2, 1));
            Button add = button("+ Add", false); pickerHeader.addView(add, lp(82, 41)); add.setOnClickListener(v -> vehicleDialog(null));
            addSpace(selector, 10);
            if (current != null) {
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
                tools.addView(text("Estimated: " + (current.estimate > 0 ? n(current.estimate) + " km/L" : "not set"), 13, secondary, false), new LinearLayout.LayoutParams(0, -2, 1));
                Button manage = button("Edit vehicle", false); tools.addView(manage, lp(112, 42)); manage.setOnClickListener(v -> vehicleDialog(current));
            } else {
                selector.addView(text("Add a vehicle to start logging fuel & vehicle expenses.", 14, secondary, false));
            }
            addSpace(root, 18);

            if (current != null) {
                if (activeTab == 1) {
                    mileageSummary(current);
                    entryForm(current);
                    history(current);
                } else {
                    vehicleExpensesTabView(current);
                }
            }
        }

        addSpace(root, 14); filesCard();
        addSpace(root, 15);
        TextView foot = text("Mileage assumes a full tank at each fill-up.\nDistance since previous fill ÷ litres added now.", 12, secondary, false);
        foot.setGravity(Gravity.CENTER); foot.setLineSpacing(d(3), 1); root.addView(foot);
    }

    // ==========================================
    // TAB 1: GENERAL EXPENSE MANAGER
    // ==========================================
    private void generalExpensesTabView() {
        List<FuelDb.GeneralExpense> genList = db.generalExpenses();
        double totalGeneral = 0;
        Map<String, Double> catTotals = new LinkedHashMap<>();

        for (FuelDb.GeneralExpense g : genList) {
            totalGeneral += g.amount;
            Double cur = catTotals.get(g.category);
            catTotals.put(g.category, (cur == null ? 0 : cur) + g.amount);
        }

        // 1. Overview Card & Category Doughnut Chart
        LinearLayout overview = card(root);
        overview.addView(text("TOTAL GENERAL EXPENSES", 12, secondary, true)); addSpace(overview, 10);
        overview.addView(text("₹" + n(totalGeneral), 31, accent, true)); addSpace(overview, 6);
        overview.addView(text(genList.size() + " recorded expenses across " + catTotals.size() + " categories", 13, secondary, false));

        if (!catTotals.isEmpty()) {
            addSpace(overview, 16);
            overview.addView(text("SPENDING BY CATEGORY", 12, secondary, true)); addSpace(overview, 10);

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
            chartWeb.setLayoutParams(lp(-1, 230));
            WebSettings settings = chartWeb.getSettings();
            settings.setJavaScriptEnabled(true);
            settings.setDomStorageEnabled(true);

            String bgHex = dark ? "#1B2225" : "#FFFFFF";
            String txtHex = dark ? "#A2B1B4" : "#64777D";

            String html = "<!DOCTYPE html><html><head>" +
                    "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                    "<script src='https://cdn.jsdelivr.net/npm/chart.js'></script>" +
                    "<style>body { background-color: " + bgHex + "; margin: 0; padding: 0; } .box { position: relative; height: 220px; width: 100%; }</style>" +
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

            chartWeb.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
            overview.addView(chartWeb);
        }
        addSpace(root, 18);

        // 2. Add General Expense Form
        LinearLayout form = card(root);
        form.addView(text("New general expense", 20, primary, true)); addSpace(form, 6);
        form.addView(text("Log food, groceries, travel, commute, hotel, or custom expenses.", 13, secondary, false)); addSpace(form, 16);

        // Category Row with "+ New" button
        LinearLayout catHeader = row(); form.addView(catHeader, lp(-1, -2));
        catHeader.addView(text("CATEGORY", 12, secondary, true), new LinearLayout.LayoutParams(0, -2, 1));
        Button newCatBtn = button("+ New Category", false);
        catHeader.addView(newCatBtn, lp(115, 36));
        addSpace(form, 7);

        List<String> cats = db.categories();
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

        // Date Picker
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

        // 3. General Expense History
        LinearLayout section = col(); root.addView(section, lp(-1, -2));
        LinearLayout heading = row(); section.addView(heading, lp(-1, -2));
        heading.addView(text("General expense history", 20, primary, true), new LinearLayout.LayoutParams(0, -2, 1));
        heading.addView(text(genList.size() + " entries", 12, secondary, false)); addSpace(section, 12);

        if (genList.isEmpty()) {
            LinearLayout empty = card(section);
            empty.addView(text("No general expenses yet", 15, primary, true)); addSpace(empty, 6);
            empty.addView(text("Log your everyday expenses above to track category spending.", 13, secondary, false));
            return;
        }

        for (FuelDb.GeneralExpense g : genList) {
            LinearLayout item = card(section);
            LinearLayout titleRow = row(); item.addView(titleRow, lp(-1, -2));
            LinearLayout left = col(); titleRow.addView(left, new LinearLayout.LayoutParams(0, -2, 1));

            // Category Badge + Title
            TextView badge = text(g.category.toUpperCase(Locale.ROOT), 10, accent, true);
            left.addView(badge); addSpace(left, 4);
            left.addView(text(g.title, 15, primary, true)); addSpace(left, 4);
            left.addView(text(date(g.time) + "  ·  ₹" + n(g.amount), 13, secondary, false));

            Button delete = button("Delete", false);
            titleRow.addView(delete, lp(78, 42));
            delete.setOnClickListener(v -> new AlertDialog.Builder(this).setTitle("Delete expense?")
                    .setMessage("Remove '" + g.title + "' (₹" + n(g.amount) + ")?")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Delete", (dialog, which) -> { db.deleteGeneralExpense(g.id); render(); }).show());
            addSpace(section, 10);
        }
    }

    // ==========================================
    // TAB 2: MILEAGE AT A GLANCE & FILL-UPS
    // ==========================================
    private void mileageSummary(FuelDb.Vehicle vehicle) {
        List<FuelDb.Fill> list = db.fills(vehicle.id);
        LinearLayout summary = card(root);
        summary.addView(text("MILEAGE AT A GLANCE", 12, secondary, true)); addSpace(summary, 12);

        if (list.size() < 2) {
            summary.addView(text("— km/L", 29, accent, true)); addSpace(summary, 5);
            summary.addView(text(list.isEmpty() ? "Log your first fill-up to get started." : "Add one more fill-up to calculate mileage.", 13, secondary, false));
            addSpace(root, 18);
            return;
        }

        List<FuelDb.Fill> sorted = new ArrayList<>(list);
        Collections.reverse(sorted);

        double totalDistance = 0;
        double totalFuel = 0;

        StringBuilder datesJson = new StringBuilder("[");
        StringBuilder mileageJson = new StringBuilder("[");

        for (int i = 1; i < sorted.size(); i++) {
            FuelDb.Fill prev = sorted.get(i - 1);
            FuelDb.Fill curr = sorted.get(i);
            double dist = curr.odometer - prev.odometer;
            if (dist > 0 && curr.liters > 0) {
                totalDistance += dist;
                totalFuel += curr.liters;
                double kml = dist / curr.liters;

                if (datesJson.length() > 1) {
                    datesJson.append(",");
                    mileageJson.append(",");
                }
                datesJson.append("'").append(date(curr.time)).append("'");
                mileageJson.append(String.format(Locale.US, "%.2f", kml));
            }
        }
        datesJson.append("]");
        mileageJson.append("]");

        double overallAvg = totalFuel > 0 ? totalDistance / totalFuel : 0;

        summary.addView(text(n(overallAvg) + " km/L", 31, accent, true)); addSpace(summary, 5);
        summary.addView(text("Overall Average  ·  " + n(totalDistance) + " km tracked  ·  " + n(totalFuel) + " L fuel", 13, secondary, false));

        if (vehicle.estimate > 0) {
            addSpace(summary, 8);
            double delta = (overallAvg - vehicle.estimate) / vehicle.estimate * 100;
            summary.addView(text("Vs default estimate (" + n(vehicle.estimate) + "): " + (delta >= 0 ? "+" : "") + n(delta) + "%", 13, primary, true));
        }

        addSpace(summary, 16);
        summary.addView(text("MILEAGE TREND", 12, secondary, true)); addSpace(summary, 10);

        WebView chartWeb = new WebView(this);
        chartWeb.setLayoutParams(lp(-1, 220));
        WebSettings settings = chartWeb.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);

        String bgHex = dark ? "#1B2225" : "#FFFFFF";
        String txtHex = dark ? "#A2B1B4" : "#64777D";
        String accHex = dark ? "#65DCC7" : "#0D796A";

        String html = "<!DOCTYPE html><html><head>" +
                "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                "<script src='https://cdn.jsdelivr.net/npm/chart.js'></script>" +
                "<style>body { background-color: " + bgHex + "; margin: 0; padding: 0; } .box { position: relative; height: 210px; width: 100%; }</style>" +
                "</head><body><div class='box'><canvas id='c'></canvas></div><script>" +
                "const labels = " + datesJson.toString() + ";" +
                "const data = " + mileageJson.toString() + ";" +
                "const avgLine = labels.map(() => " + String.format(Locale.US, "%.2f", overallAvg) + ");" +
                "new Chart(document.getElementById('c').getContext('2d'), {" +
                "  type: 'line'," +
                "  data: {" +
                "    labels: labels," +
                "    datasets: [" +
                "      { label: 'Mileage (km/L)', data: data, borderColor: '" + accHex + "', backgroundColor: '" + accHex + "', tension: 0.25, borderWidth: 2.5, pointRadius: 4 }," +
                "      { label: 'Overall Avg', data: avgLine, borderColor: '#f59e0b', borderDash: [5, 5], borderWidth: 1.5, pointRadius: 0 }" +
                "    ]" +
                "  }," +
                "  options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { labels: { color: '" + txtHex + "', font: { size: 11 } } } }, scales: { x: { ticks: { color: '" + txtHex + "', font: { size: 10 } } }, y: { ticks: { color: '" + txtHex + "', font: { size: 10 } } } } }" +
                "});" +
                "</script></body></html>";

        chartWeb.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
        summary.addView(chartWeb);
        addSpace(root, 18);
    }

    private void entryForm(FuelDb.Vehicle vehicle) {
        LinearLayout form = card(root);
        form.addView(text("New fill-up", 20, primary, true)); addSpace(form, 6);
        form.addView(text("Record your fuel purchase, date, and current odometer.", 13, secondary, false)); addSpace(form, 16);

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
        estimateInput = input(form, "ESTIMATED MILEAGE (KM/L) · OPTIONAL", "e.g. 18", vehicle.estimate > 0 ? nPlain(vehicle.estimate) : "", true);
        TextView info = text("A mileage result appears after the second fill-up for this vehicle.", 12, secondary, false);
        form.addView(info); addSpace(form, 14);
        Button save = button("Save fill-up", true); form.addView(save, lp(-1, 49)); save.setOnClickListener(v -> saveFill(vehicle));
        addSpace(root, 18);
    }
    private String nPlain(double x) { return String.valueOf(x); }
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
            if (estimate != vehicle.estimate) db.updateVehicle(vehicle.id, vehicle.name, estimate);
            selectedFillDate = System.currentTimeMillis();
            toast("Fill-up saved"); render();
        } catch (Exception e) { alert(e.getMessage() == null ? "Could not save fill-up." : e.getMessage()); }
    }

    private void history(FuelDb.Vehicle vehicle) {
        LinearLayout section = col(); root.addView(section, lp(-1, -2));
        LinearLayout heading = row(); section.addView(heading, lp(-1, -2));
        heading.addView(text("Fill-up history", 20, primary, true), new LinearLayout.LayoutParams(0, -2, 1));
        List<FuelDb.Fill> fills = db.fills(vehicle.id);
        heading.addView(text(fills.size() + " entries", 12, secondary, false)); addSpace(section, 12);
        if (fills.isEmpty()) {
            LinearLayout empty = card(section); empty.addView(text("No fill-ups yet", 15, primary, true)); addSpace(empty, 6);
            empty.addView(text("Saved entries will appear here.", 13, secondary, false)); return;
        }
        for (int i = 0; i < fills.size(); i++) {
            FuelDb.Fill f = fills.get(i);
            FuelDb.Fill prev = i + 1 < fills.size() ? fills.get(i + 1) : null;
            LinearLayout item = card(section);
            LinearLayout titleRow = row(); item.addView(titleRow, lp(-1, -2));
            LinearLayout left = col(); titleRow.addView(left, new LinearLayout.LayoutParams(0, -2, 1));
            left.addView(text(date(f.time), 14, primary, true)); addSpace(left, 5);
            left.addView(text(n(f.odometer) + " km on odometer", 12, secondary, false));
            Button delete = button("Delete", false); titleRow.addView(delete, lp(78, 42));
            delete.setOnClickListener(v -> new AlertDialog.Builder(this).setTitle("Delete fill-up?")
                    .setMessage("This cannot be undone. Mileage will be recalculated from the remaining entries.")
                    .setNegativeButton("Cancel", null).setPositiveButton("Delete", (dialog, which) -> { db.deleteFill(f.id); render(); }).show());
            addSpace(item, 14);
            LinearLayout nums = row(); item.addView(nums, lp(-1, -2));
            metric(nums, "FUEL", n(f.liters) + " L"); metric(nums, "SPENT", "₹" + n(f.amount));
            metric(nums, "ACTUAL", prev == null ? "—" : n((f.odometer - prev.odometer) / f.liters) + " km/L");
            if (f.estimate > 0) { addSpace(item, 11); item.addView(text("Estimated: " + n(f.estimate) + " km/L", 12, secondary, false)); }
            addSpace(section, 10);
        }
    }

    // ==========================================
    // TAB 3: VEHICLE EXPENSES (FUEL + MISC)
    // ==========================================
    private void vehicleExpensesTabView(FuelDb.Vehicle vehicle) {
        List<FuelDb.Fill> fills = db.fills(vehicle.id);
        List<FuelDb.Expense> miscList = db.expenses(vehicle.id);

        double totalFuelSpend = 0;
        double totalMiscSpend = 0;

        TreeMap<String, double[]> monthlyMap = new TreeMap<>();
        SimpleDateFormat keyFmt = new SimpleDateFormat("yyyy-MM", Locale.US);
        SimpleDateFormat labelFmt = new SimpleDateFormat("MMM yy", Locale.US);
        TreeMap<String, String> monthLabels = new TreeMap<>();

        for (FuelDb.Fill f : fills) {
            totalFuelSpend += f.amount;
            Date dObj = new Date(f.time);
            String k = keyFmt.format(dObj);
            monthLabels.put(k, labelFmt.format(dObj));
            double[] arr = monthlyMap.get(k);
            if (arr == null) { arr = new double[]{0, 0}; monthlyMap.put(k, arr); }
            arr[0] += f.amount;
        }
        for (FuelDb.Expense e : miscList) {
            totalMiscSpend += e.amount;
            Date dObj = new Date(e.time);
            String k = keyFmt.format(dObj);
            monthLabels.put(k, labelFmt.format(dObj));
            double[] arr = monthlyMap.get(k);
            if (arr == null) { arr = new double[]{0, 0}; monthlyMap.put(k, arr); }
            arr[1] += e.amount;
        }

        double grandTotal = totalFuelSpend + totalMiscSpend;

        LinearLayout overview = card(root);
        overview.addView(text("TOTAL VEHICLE EXPENSES", 12, secondary, true)); addSpace(overview, 10);
        overview.addView(text("₹" + n(grandTotal), 31, accent, true)); addSpace(overview, 6);
        overview.addView(text("Fuel: ₹" + n(totalFuelSpend) + "   ·   Misc: ₹" + n(totalMiscSpend), 13, secondary, false));

        if (!monthlyMap.isEmpty()) {
            addSpace(overview, 16);
            overview.addView(text("MONTHLY VEHICLE EXPENSE BREAKDOWN", 12, secondary, true)); addSpace(overview, 10);

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

            WebView expWeb = new WebView(this);
            expWeb.setLayoutParams(lp(-1, 230));
            WebSettings settings = expWeb.getSettings();
            settings.setJavaScriptEnabled(true);
            settings.setDomStorageEnabled(true);

            String bgHex = dark ? "#1B2225" : "#FFFFFF";
            String txtHex = dark ? "#A2B1B4" : "#64777D";
            String accHex = dark ? "#65DCC7" : "#0D796A";

            String html = "<!DOCTYPE html><html><head>" +
                    "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                    "<script src='https://cdn.jsdelivr.net/npm/chart.js'></script>" +
                    "<style>body { background-color: " + bgHex + "; margin: 0; padding: 0; } .box { position: relative; height: 220px; width: 100%; }</style>" +
                    "</head><body><div class='box'><canvas id='ec'></canvas></div><script>" +
                    "new Chart(document.getElementById('ec').getContext('2d'), {" +
                    "  type: 'bar'," +
                    "  data: {" +
                    "    labels: " + monthsJson.toString() + "," +
                    "    datasets: [" +
                    "      { label: 'Fuel (₹)', data: " + fuelExpJson.toString() + ", backgroundColor: '" + accHex + "' }," +
                    "      { label: 'Misc (₹)', data: " + miscExpJson.toString() + ", backgroundColor: '#f59e0b' }" +
                    "    ]" +
                    "  }," +
                    "  options: {" +
                    "    responsive: true, maintainAspectRatio: false," +
                    "    plugins: { legend: { labels: { color: '" + txtHex + "', font: { size: 11 } } } }," +
                    "    scales: { x: { stacked: true, ticks: { color: '" + txtHex + "', font: { size: 10 } } }, y: { stacked: true, ticks: { color: '" + txtHex + "', font: { size: 10 } } } }" +
                    "  }" +
                    "});" +
                    "</script></body></html>";

            expWeb.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
            overview.addView(expWeb);
        }
        addSpace(root, 18);

        LinearLayout form = card(root);
        form.addView(text("Add vehicle misc expense", 20, primary, true)); addSpace(form, 6);
        form.addView(text("Track maintenance, insurance, tolls, car wash, or accessories.", 13, secondary, false)); addSpace(form, 16);

        form.addView(text("DATE OF EXPENSE", 12, secondary, true)); addSpace(form, 7);
        Button expDateBtn = button(date(selectedExpenseDate), false);
        expDateBtn.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        expDateBtn.setPadding(d(14), 0, d(14), 0);
        form.addView(expDateBtn, lp(-1, 50));
        expDateBtn.setOnClickListener(v -> pickDate(selectedExpenseDate, millis -> {
            selectedExpenseDate = millis;
            expDateBtn.setText(date(selectedExpenseDate));
        }));
        addSpace(form, 14);

        expTitleInput = input(form, "EXPENSE DESCRIPTION", "e.g. Oil Change / Insurance / Toll", "", false);
        expAmountInput = input(form, "AMOUNT SPENT (₹)", "e.g. 1200", "", true);

        Button saveExp = button("Save vehicle expense", true);
        form.addView(saveExp, lp(-1, 49));
        saveExp.setOnClickListener(v -> {
            try {
                String title = expTitleInput.getText().toString().trim();
                if (title.isEmpty()) throw new IllegalArgumentException("Enter an expense description.");
                double amt = number(expAmountInput, "amount spent", false);
                db.addExpense(vehicle.id, selectedExpenseDate, title, amt);
                selectedExpenseDate = System.currentTimeMillis();
                toast("Vehicle expense saved"); render();
            } catch (Exception e) {
                alert(e.getMessage() == null ? "Could not save expense." : e.getMessage());
            }
        });
        addSpace(root, 18);

        LinearLayout section = col(); root.addView(section, lp(-1, -2));
        LinearLayout heading = row(); section.addView(heading, lp(-1, -2));
        heading.addView(text("Vehicle misc history", 20, primary, true), new LinearLayout.LayoutParams(0, -2, 1));
        heading.addView(text(miscList.size() + " entries", 12, secondary, false)); addSpace(section, 12);

        if (miscList.isEmpty()) {
            LinearLayout empty = card(section);
            empty.addView(text("No vehicle misc expenses yet", 15, primary, true)); addSpace(empty, 6);
            empty.addView(text("Added expenses will appear here and sum into your vehicle total.", 13, secondary, false));
            return;
        }

        for (FuelDb.Expense e : miscList) {
            LinearLayout item = card(section);
            LinearLayout titleRow = row(); item.addView(titleRow, lp(-1, -2));
            LinearLayout left = col(); titleRow.addView(left, new LinearLayout.LayoutParams(0, -2, 1));
            left.addView(text(e.title, 15, primary, true)); addSpace(left, 4);
            left.addView(text(date(e.time) + "  ·  ₹" + n(e.amount), 13, accent, true));

            Button delete = button("Delete", false);
            titleRow.addView(delete, lp(78, 42));
            delete.setOnClickListener(v -> new AlertDialog.Builder(this).setTitle("Delete expense?")
                    .setMessage("Remove '" + e.title + "' (₹" + n(e.amount) + ")?")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Delete", (dialog, which) -> { db.deleteExpense(e.id); render(); }).show());
            addSpace(section, 10);
        }
    }

    private void metric(LinearLayout row, String label, String value) {
        LinearLayout block = col(); row.addView(block, new LinearLayout.LayoutParams(0, -2, 1));
        block.addView(text(label, 10, secondary, true)); addSpace(block, 6);
        block.addView(text(value, 14, primary, true));
    }
    private void filesCard() {
        LinearLayout card = card(root);
        card.addView(text("Your data", 20, primary, true)); addSpace(card, 7);
        card.addView(text("Import or export all general expenses, vehicles, fill-ups, and vehicle expenses as one CSV file.", 13, secondary, false)); addSpace(card, 16);
        LinearLayout actions = row(); card.addView(actions, lp(-1, -2));
        Button importBtn = button("Import CSV", false), exportBtn = button("Export CSV", true);
        LinearLayout.LayoutParams a = new LinearLayout.LayoutParams(0, d(48), 1); a.setMargins(0, 0, d(7), 0);
        LinearLayout.LayoutParams b = new LinearLayout.LayoutParams(0, d(48), 1); b.setMargins(d(7), 0, 0, 0);
        actions.addView(importBtn, a); actions.addView(exportBtn, b);
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
    }
    private void vehicleDialog(FuelDb.Vehicle vehicle) {
        LinearLayout content = col(); content.setPadding(d(22), d(10), d(22), 0);
        content.addView(text("VEHICLE NAME", 12, secondary, true)); addSpace(content, 7);
        EditText name = edit("e.g. City / Activa", vehicle == null ? "" : vehicle.name, false);
        content.addView(name); addSpace(content, 16);
        content.addView(text("DEFAULT ESTIMATED MILEAGE (KM/L)", 12, secondary, true)); addSpace(content, 7);
        EditText estimate = edit("Optional", vehicle != null && vehicle.estimate > 0 ? nPlain(vehicle.estimate) : "", true);
        content.addView(estimate);
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
                    if (vehicle == null) selectedVehicle = db.addVehicle(label, est);
                    else { db.updateVehicle(vehicle.id, label, est); selectedVehicle = vehicle.id; }
                    prefs.edit().putLong("selected_vehicle", selectedVehicle).apply();
                    dialog.dismiss(); render();
                } catch (Exception e) { alert("Could not save vehicle. Check that its name is unique and mileage is valid."); }
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
        } else if (requestCode == REQUEST_IMPORT) {
            try (InputStreamReader reader = new InputStreamReader(getContentResolver().openInputStream(uri), StandardCharsets.UTF_8)) {
                CsvIO.Result result = CsvIO.importCsv(db, reader);
                render(); alert("Import complete: " + result.generalExpenses + " general expenses, " + result.vehicles + " new vehicles, " + result.fills + " new fill-ups, " + result.expenses + " vehicle expenses, " + result.duplicates + " duplicates skipped.");
            } catch (Exception e) { alert("Import failed (nothing was changed): " + e.getMessage()); }
        }
    }
}
