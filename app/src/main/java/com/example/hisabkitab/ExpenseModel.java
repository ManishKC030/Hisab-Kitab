package com.example.hisabkitab;

public class ExpenseModel {

    public String firebase_id, user_uid, title, category, description, date;
    public double amount;

    public ExpenseModel() {}

    public ExpenseModel(String firebase_id, String user_uid,
                        String title, double amount,
                        String category, String description,
                        String date) {

        this.firebase_id = firebase_id;
        this.user_uid = user_uid;
        this.title = title;
        this.amount = amount;
        this.category = category;
        this.description = description;
        this.date = date;
    }
}