package com.wuya.reader;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;


/**
 * Created by fujiayi on 2017/9/13.
 * <p>
 * 此类 底层UI实现 无SDK相关逻辑
 */

public class BaseActivity extends AppCompatActivity implements MainHandlerConstant {



    private static final String TAG = "WuyaActivity";

    /*
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wuya);

    }


}
