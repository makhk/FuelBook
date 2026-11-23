# Fuelbook — Personal & Vehicle Expense Manager

A lightweight, single-activity Android app written in Java with the Android SDK. Track everyday personal expenses alongside multi-vehicle fuel mileage, fuel costs, and maintenance expenses in one place.

## Features

### 1. General Expenses (Tab 1)
- **Category Management:** Pre-loaded with default categories (*Food, Groceries, Travel, Commute, Hotel, Shopping, Bills & Utilities, Entertainment, Health, Other*) plus support for creating custom categories on the fly.
- **Category Breakdown Chart:** Interactive Doughnut chart visualizing spending across categories.
- **Custom Date Entry:** Log expenses with a native date picker, optional description/note, and amount (`₹`).
- **Category-Grouped History:** Automatically groups expenses by category with category subtotals and a **Show all / Show fewer** toggle (previewing 5 entries by default for readability).

### 2. Vehicle Expenses & Mileage (Tab 2)
- **Multi-Vehicle Support:** Create, select, rename, and delete multiple vehicles with optional default estimated mileage (`km/L`).
- **Unified Overview & Interactive Charts:** View **Overall Average Mileage** (`Total Distance ÷ Total Fuel`), total distance/fuel tracked, and **Total Vehicle Expenses** (`Fuel + Misc`) side-by-side. Switch seamlessly between the **Mileage Trend** line chart (with overall average reference line) and the monthly **Expense Breakdown** stacked bar chart (`Fuel` vs. `Misc`).
- **Fuel Fill-ups & Misc Expenses:** Log fuel fill-ups (date, odometer, litres, cost) and vehicle-specific miscellaneous expenses (maintenance, insurance, tolls, car wash, accessories) with custom date pickers.
- **Readable Histories:** Separate fill-up and vehicle misc expense histories with a **Show all / Show fewer** preview toggle.

### 3. Time Period Filtering, Backup & Appearance
- **Flexible Time Filters:** Filter charts, totals, and expense histories across both tabs by **All Time**, **Month-wise** (month selector), or **Custom Interval** (`From` and `To` date pickers).
- **Local SQLite Persistence (v3):** Fast, reliable local storage (`fuelbook.db`) with confirmation dialogs before deleting records.
- **Unified CSV Export / Import:** Export and import all general expenses, custom categories, vehicles, fill-ups, and vehicle misc expenses in a single CSV file via Android's system document picker.
- **Light & Dark Themes:** Theme toggle persisted across app restarts.

## Mileage calculation
- **Per-Fill Mileage:** `(current odometer − previous odometer) / litres added at the current fill-up` (in km/L), calculated starting from the second fill-up for each vehicle.
- **Overall Average Mileage:** `total valid distance tracked / total fuel consumed` across the selected period.
- Accurate for the **full-tank-to-full-tank** method; entries are ordered by odometer per vehicle.

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

`epoch_millis` is Unix time in milliseconds; amounts use `₹` for display only and are stored as plain decimal numbers in CSV. CSV import is transactional (invalid rows roll back the entire import) and skips duplicate vehicle/odometer fill-ups.

## Privacy & Permissions
All expense records, vehicle data, and theme preferences are stored locally on your device in SQLite. Import/export uses Android's system document picker without requiring external storage permissions. `android.permission.INTERNET` is used solely to load Chart.js inside embedded `WebView` components for rendering graphs.
