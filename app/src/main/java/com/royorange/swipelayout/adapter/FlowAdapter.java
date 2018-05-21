package com.royorange.swipelayout.adapter;

import android.content.res.Resources;
import android.databinding.DataBindingUtil;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.royorange.lib.swipelayoutlib.SwipeLayout;
import com.royorange.swipeoptionlayout.R;
import com.royorange.swipeoptionlayout.databinding.ItemFlowBinding;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Roy on 2018/4/19.
 */

public class FlowAdapter extends RecyclerView.Adapter<BindingViewHolder<ItemFlowBinding>> {
    private List<FlowViewModel> dataList = new ArrayList<>();


    public FlowAdapter() {
        for(int i =0;i<20;i++){
            FlowViewModel flowViewModel = new FlowViewModel();
            dataList.add(flowViewModel);
        }
    }

    @Override
    public BindingViewHolder<ItemFlowBinding> onCreateViewHolder(ViewGroup parent, int viewType) {
        ItemFlowBinding binding = DataBindingUtil.inflate(LayoutInflater.from(parent.getContext()), R.layout.item_flow, parent,false);
        return initHolder(binding);
    }

    @Override
    public void onBindViewHolder(BindingViewHolder<ItemFlowBinding> holder, int position) {
        holder.getBinding().title.setText("This is title" + position);
        holder.getBinding().summary.setText("item" + position);
    }

    @Override
    public int getItemCount() {
        return dataList.size();
    }

    private BindingViewHolder initHolder(final ItemFlowBinding binding){
        final BindingViewHolder holder = new BindingViewHolder(binding);
        binding.container.setListener(new SwipeLayout.SwipeListener() {
            boolean isActionFinished = true;
            @Override
            public void onSwipe(float percent) {
                float alpha = percent+0.25f;
                if(alpha>1){
                    alpha = 1;
                }
                binding.like.setAlpha(alpha);
            }

            @Override
            public void onExpanded() {
                if(isActionFinished){
                    isActionFinished = false;
                    int position = holder.getAdapterPosition();
                    if(position<0){
                        return;
                    }
                    FlowViewModel viewModel = dataList.get(position);
                    viewModel.setLike(!viewModel.isLike());
                    binding.like.setText(viewModel.isLike()?"liked":"unlike");
                    binding.like.setCompoundDrawablesWithIntrinsicBounds(0, viewModel.isLike()?R.drawable.vd_star_filled:R.drawable.vd_star,0,0);
                }
            }

            @Override
            public void onCollapsed() {
                isActionFinished = true;
                int position = holder.getAdapterPosition();
                if(position<0){
                    return;
                }
                FlowViewModel viewModel = dataList.get(position);
                binding.like.setText(viewModel.isLike()?"unlike":"like");
                binding.like.setCompoundDrawablesWithIntrinsicBounds(0, viewModel.isLike()?R.drawable.vd_star_filled:R.drawable.vd_star,0,0);
                Resources resources = binding.getRoot().getResources();
                binding.like.setBackgroundColor(viewModel.isLike()? Color.parseColor("#E85063"):Color.parseColor("#38C499"));
            }
        });
        return holder;
    }
}
