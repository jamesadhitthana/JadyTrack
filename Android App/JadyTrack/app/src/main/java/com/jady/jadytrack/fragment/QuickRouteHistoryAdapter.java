package com.jady.jadytrack.fragment;

import android.graphics.Color;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.jady.jadytrack.OnRecordClick;
import com.jady.jadytrack.R;
import com.jady.jadytrack.entity.QuickRouteHistory;

import java.util.ArrayList;


public class QuickRouteHistoryAdapter extends RecyclerView.Adapter<QuickRouteHistoryAdapter.QuickRouteHistoryViewHolder> {

    private ArrayList<QuickRouteHistory> dataList;
    private OnRecordClick onRecordClick;
    private int selected = -1;

    public QuickRouteHistoryAdapter(ArrayList<QuickRouteHistory> dataList, OnRecordClick onRecordClick){
        this.dataList = dataList;
        this.onRecordClick = onRecordClick;
    }

    @Override
    public QuickRouteHistoryViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View view = layoutInflater.inflate(R.layout.quick_route_card, parent, false);
        return new QuickRouteHistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(QuickRouteHistoryViewHolder holder, final int position) {
        holder.quickRouteName.setText(dataList.get(position).getNameQuickRoute());

        if(selected == position){
            holder.cardView.setCardBackgroundColor(Color.parseColor("#4EB9BF"));
            holder.quickRouteName.setTextColor(Color.parseColor("#ffffff"));

        } else {
            holder.cardView.setCardBackgroundColor(Color.WHITE);
            holder.quickRouteName.setTextColor(Color.BLACK);
        }

        holder.cardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(selected == position){
                    selected = -1;
                    onRecordClick.onClick(selected);
                    notifyDataSetChanged();
                    return;
                }
                selected = position;
                onRecordClick.onClick(selected);
                notifyDataSetChanged();
            }
        });

    }

    @Override
    public int getItemCount() {
        return (dataList != null) ? dataList.size() : 0;
    }

    public class QuickRouteHistoryViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private TextView quickRouteName;
        private CardView cardView;

        public QuickRouteHistoryViewHolder(View itemView) {
            super(itemView);
            quickRouteName = (TextView) itemView.findViewById(R.id.quick_route_name);
            cardView = (CardView) itemView.findViewById(R.id.card_view);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {

            //Toast.makeText(v.getContext(), quickRouteName.getText(), Toast.LENGTH_SHORT).show();

        }
    }

}
