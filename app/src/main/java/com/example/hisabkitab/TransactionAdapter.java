package com.example.hisabkitab;

import android.view.*;
import android.widget.*;
import androidx.recyclerview.widget.RecyclerView;
import java.util.*;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.ViewHolder> {

    List<TransactionItem> list;

    public TransactionAdapter(List<TransactionItem> list){
        this.list = list;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder{

        TextView title, category, date, amount;

        public ViewHolder(View v){
            super(v);

            title = v.findViewById(R.id.txtTitle);
            category = v.findViewById(R.id.txtCategory);
            date = v.findViewById(R.id.txtDate);
            amount = v.findViewById(R.id.txtAmount);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent,int viewType){

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_transaction,parent,false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder,int position){

        TransactionItem t = list.get(position);

        holder.title.setText(t.title);
        holder.category.setText(t.category);
        if (holder.date != null) {
            holder.date.setText(t.date);
        }

        if(t.isIncome){
            holder.amount.setText("+ Rs " + t.amount);
            holder.amount.setTextColor(androidx.core.content.ContextCompat.getColor(holder.itemView.getContext(), R.color.income_green));
        }else{
            holder.amount.setText("- Rs " + t.amount);
            holder.amount.setTextColor(androidx.core.content.ContextCompat.getColor(holder.itemView.getContext(), R.color.expense_red));
        }
    }

    @Override
    public int getItemCount(){
        return list.size();
    }
}