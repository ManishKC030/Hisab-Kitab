package com.example.hisabkitab;

public class AddExpenseActivity {
    TextView catOther = findViewById(R.id.catOther);
    EditText edtNewCategory = findViewById(R.id.edtNewCategory);

    TextView[] categories = {
            findViewById(R.id.catFood),
            findViewById(R.id.catTransport),
            findViewById(R.id.catShopping),
            findViewById(R.id.catBills),
            findViewById(R.id.catHealth),
            findViewById(R.id.catOther)
    };

for (TextView cat : categories) {
        cat.setOnClickListener(v -> {

            for (TextView c : categories) {
                c.setBackgroundResource(R.drawable.bg_category_unselected);
            }

            cat.setBackgroundResource(R.drawable.bg_category_selected);

            if (cat.getId() == R.id.catOther) {
                edtNewCategory.setVisibility(View.VISIBLE);
            } else {
                edtNewCategory.setVisibility(View.GONE);
            }
        });
    }

}
