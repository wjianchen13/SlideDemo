package com.cold.slidedemo;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.cold.library.ISlideListener;
import com.cold.library.SlideLayout;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, ISlideListener {

    private ImageView imgvRestore;
    private ImageView imgvClose;
    private ImageView imgvMenu1;
    private SlideLayout mSlideLayout;
    private List<String> datas = new ArrayList<String>();
    int i = 0;
    private MyAdapter adapter;


    private List<String> datas1 = new ArrayList<String>();
    private MyAdapter adapter1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportActionBar().hide();

        imgvRestore = findViewById(R.id.imgv_restore);
        imgvRestore.setOnClickListener(this);

        imgvClose = findViewById(R.id.imgv_close);
        imgvClose.setOnClickListener(this);

        imgvMenu1 = findViewById(R.id.imgv_menu1);
        imgvMenu1.setOnClickListener(this);

        mSlideLayout = findViewById(R.id.slide_layout);
        mSlideLayout.setSlideListener(this);

        initDrawerRv();
        initMenuRv();
    }

    @Override
    public void onClick(View view) {
        if(view.getId() == R.id.imgv_close) {
            finish();
        } else if(view.getId() == R.id.imgv_restore) {
            if(mSlideLayout != null)
                mSlideLayout.restoreContent();
        } else if(view.getId() == R.id.imgv_menu1) {
            String str = "新增消息" + i ++;
            datas.add(str);
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onPositionChanged(float percent) {
        if (imgvRestore != null) {
            if (percent * 100 < 10) {
                imgvRestore.setVisibility(View.GONE);
            } else {
                imgvRestore.setVisibility(View.VISIBLE);
                imgvRestore.setAlpha(percent);
            }
        }
    }

    public void initDrawerRv() {
        RecyclerView recyclerView = findViewById(R.id.rv_test);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(RecyclerView.VERTICAL);
        recyclerView.setLayoutManager(layoutManager);

        for (i = 0; i < 10; i++) {
            String str = "发言聊天 " + i;
            datas.add(str);
        }
        adapter = new MyAdapter(datas);
        recyclerView.setAdapter(adapter);
    }

    private void initMenuRv() {
        RecyclerView recyclerView = findViewById(R.id.tv_menu);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(RecyclerView.VERTICAL);
        recyclerView.setLayoutManager(layoutManager);

        for (i = 0; i < 30; i++) {
            String str = "详细信息 " + i;
            datas1.add(str);
        }
        adapter1 = new MyAdapter(datas1);
        recyclerView.setAdapter(adapter1);
    }
}
