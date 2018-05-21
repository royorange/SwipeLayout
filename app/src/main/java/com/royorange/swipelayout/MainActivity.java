package com.royorange.swipeoptionlayout;

import android.databinding.DataBindingUtil;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;

import com.royorange.swipeoptionlayout.adapter.FlowAdapter;
import com.royorange.swipeoptionlayout.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {
    ActivityMainBinding binding;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this,R.layout.activity_main);
        initRecyclerView();
    }

    private void initRecyclerView(){
        FlowAdapter adapter = new FlowAdapter();
        LinearLayoutManager manager = new LinearLayoutManager(this);
        binding.recyclerView.setAdapter(adapter);
        binding.recyclerView.setLayoutManager(manager);
        adapter.notifyDataSetChanged();
    }
}
