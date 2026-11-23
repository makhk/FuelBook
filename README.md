# Fuelbook — Offline Personal & Vehicle Expense Manager

A lightweight, 100% offline, single-activity Android app written in Java with the Android SDK. Track everyday personal expenses alongside multi-vehicle fuel mileage, fuel costs, running cost per km, and maintenance expenses with zero network permissions.

## Features

### 1. General Expenses (Tab 1)
- **Category Management & Budgets:** Pre-loaded with 10 default categories (*Food, Groceries, Travel, Commute, Hotel, Shopping, Bills & Utilities, Entertainment, Health, Other*), plus support for creating, renaming, deleting custom categories, and setting monthly budgets (overall and per-category) with color-coded progress alerts.
- **Offline Category Breakdown Chart:** Crisp, locally bundled HTML5 Canvas Doughnut chart (`assets/chart.min.js`) visualizing spending distribution with zero internet access.
- **Fast Entry & Quick-Fill Chips:** Log expenses with a native date picker, optional note, amount (`₹`), and one-tap quick-fill amount chips (`+₹50`, `+₹100`, `+₹250`, `+₹500`, `+₹1000`).
- **Search, Filter & Edit:** Search notes/categories, filter by category, edit any existing entry, and browse category-grouped history with category subtotals and a **Show all / Show fewer** readability toggle.

### 2. Vehicle Expenses & Mileage (Tab 2)
- **Multi-Vehicle Support & Budgets:** Create, select, rename, edit, and delete multiple vehicles with optional default estimated mileage (`km/L`) and monthly vehicle expense budgets.
- **Unified Efficiency & Expense Overview:** View **Overall Average Mileage** (`Total Distance ÷ Total Fuel`), **Total Vehicle Expenses** (`Fuel + Misc`), **Running Cost per km** (`₹/km`), and **Average Fuel Price** (`₹/L`) at a glance.
- **Offline Interactive Charts:** Toggle between the **Mileage Trend** line chart (with overall average reference line) and the monthly **Expense Breakdown** stacked bar chart (`Fuel` vs. `Misc`).
- **Full CRUD & Search:** Add, edit, and delete fuel fill-ups and vehicle-specific miscellaneous expenses (maintenance, insurance, tolls, car wash) with search filtering and **Show all / Show fewer** history toggles.

### 3. Time Period Filtering, Backups & Appearance
- **Flexible Time Filters with Date Validation:** Filter charts, metrics, and histories across both tabs by **All Time**, **Month-wise** (month selector), or **Interval** (`From` and `To` date pickers with automatic date-order swap protection).
- **Local SQLite Persistence (v4):** Fast, reliable local storage (`fuelbook.db`) with confirmation dialogs before deleting records.
- **CSV Export/Import & SQLite Snapshot:** Export/import all general expenses, vehicles, fill-ups, and vehicle misc expenses in a single transactional CSV file (with import confirmation), or export a raw `.db` SQLite snapshot.
- **Light & Dark Themes:** Theme toggle persisted across app restarts.

## Mileage & Efficiency Formulas
- **Per-Fill Mileage:** `(current odometer − previous odometer) / litres added at the current fill-up` (in `km/L`), calculated starting from the second fill-up for each vehicle.
- **Overall Average Mileage:** `total valid distance tracked / total fuel consumed` across the selected period.
- **Running Cost per km:** `total vehicle expenses (fuel + misc) / total valid distance tracked` (`₹/km`).
- **Average Fuel Price:** `total fuel spend / total fuel litres` (`₹/L`).

## Open and build
1. Install Android Studio (with Android SDK Platform 35, Build Tools, and JDK 17).
2. Open the `FuelBook-main` directory as a project.
3. Sync Gradle using Android Gradle Plugin 8.7.3 and Gradle 8.9. (If the IDE prompts for a Gradle distribution, select/install Gradle 8.9; this source archive does not bundle Gradle binaries.)
4. Run the `app` configuration on an Android device/emulator (Android 6.0+), or select **Build > Build APK(s)**.

## CSV schema

`type,vehicle,estimated_km_per_l,epoch_millis,odometer_km,liters,amount`

- **`general_expense` rows:** Store general personal expenses (`vehicle` column holds the category name, `odometer_km` holds the description/note, `amount` holds the expense amount).
- **`vehicle` rows:** Define vehicle profiles and default estimated mileage.
- **`fill` rows:** Define fuel fill-ups (`odometer_km`, `liters`, `amount`).
- **`expense` rows:** Define vehicle-specific miscellaneous expenses (`odometer_km` column holds the expense description, `amount` holds the expense amount).

## Privacy & Permissions
All expense records, vehicle data, budgets, and theme preferences live 100% locally on your device. Charts render offline from bundled assets (`app/src/main/assets/chart.min.js`), requiring **zero** permissions (no internet or external storage permissions).
