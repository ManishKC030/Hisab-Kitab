# 🧾 HisabKitab – Personal Finance Manager

A **mobile Android app** to manage personal finance efficiently. Track **income, expenses**, and visualize **analytics** with predictions and insights.

---

## 🌟 Features

### Core Functionalities
- Add **Expense** and **Income**
- **Category selection** (with “Other” option)
- **Date picker** with **future date restriction**
- Offline-first database support with **Firebase sync**
- **User authentication** via Firebase

### Smart UX Enhancements
- **Real-time form validation** for all input fields
- **Currency formatting** (₹ / Rs) for amounts
- Optional **description** with character limit
- **Error handling** for invalid or missing inputs
- Visual selection highlight for categories

### Analytics & Insights
- **Pie Chart**: Shows category-wise income or expense
- **Bar Chart**: Monthly expense trends
- **Prediction Algorithm**: Estimates next month’s spending based on average monthly expenses
- **Insights Algorithm**: Finds top spending category and amount

---

## ⚙️ Algorithms Used

1. **Aggregation Algorithm (Pie Chart)**
    - Groups expenses/incomes by category and sums amounts
    - Input: All transactions of user
    - Output: Pie chart entries per category

2. **Monthly Summation (Bar Chart)**
    - Groups expenses per month and sums totals per month
    - Input: Date + amount
    - Output: Bar chart showing monthly expense

3. **Budget Prediction**
    - Computes average monthly spending
    - Formula: `prediction = totalAmount / totalMonths`

4. **Top Category Insights**
    - Finds maximum spending among categories
    - Input: Category → amount map
    - Output: Category with highest total spending

5. **Real-time Validation Algorithm**
    - Listens to text changes in input fields
    - Validates amount > 0, title length >= 3, category not empty
    - Updates UI error messages instantly

6. **Currency Formatting Algorithm**
    - Formats numbers as `Rs 1,000` dynamically while typing
    - Uses `NumberFormat.getInstance(Locale("en","IN"))`

---

## 💡 Smaller UX Features
- Instant **error feedback**
- Automatic **category field visibility** for "Other"
- **Future dates blocked** in date picker
- Max character limit for **description** (100)
- Auto-sync with server when network is available
- Simple offline-first **SQLite database** integration

---

## 🔮 Future Improvements / Algorithms
- **Linear Regression / Moving Average** for better budget prediction
- **Category-based trend prediction** for future spending
- **Anomaly Detection** for unusual transactions
- **Enhanced Data Visualization**
    - Stacked bar charts for income vs expense
    - Line chart with trendlines for prediction
- **Expense Forecasting** using past 6–12 months data with ML

---

## 📂 Tech Stack
- **Language:** Java
- **Database:** SQLite (Offline) + Firebase Firestore (Cloud sync)
- **Authentication:** Firebase Auth
- **Charts:** MPAndroidChart (PieChart & BarChart)
- **UI:** Android XML, GridLayout for category selection
- **Validation & Formatting:** TextWatcher & NumberFormat

---

## 📝 How to Use
1. Sign in with Firebase account.
2. Add **Income** or **Expense**:
    - Fill **amount, title, date, category**
    - Optional description
3. Analytics:
    - View category-wise **Pie chart**
    - View monthly **Bar chart**
    - Check **prediction** & **top category insights**
4. All data is synced when internet is available.

---
