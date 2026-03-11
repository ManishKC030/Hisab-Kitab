package com.example.hisabkitab;

public class TransactionItem {
    public String title;
    public double amount;
    public String date;
    public boolean isIncome;
    public int synced;

    public TransactionItem(String title, double amount, String date, boolean isIncome, int synced) {
        this.title = title;
        this.amount = amount;
        this.date = date;
        this.isIncome = isIncome;
        this.synced = synced;
    }
}