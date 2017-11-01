package com.wuya.reader;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.wuya.reader.util.PreferenceUtil;

public class SplashActivity extends Activity {
    private static final int TIME=3000;
    private static final int GO_MAIN=100;
    private static final int GO_GUIDE=101;

    Handler mhandler=new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case GO_MAIN:
                    goMain();
                    break;
                case GO_GUIDE:
                    goGuide();
                    break;
            }
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        init();
    }

    private void init() {
        SharedPreferences sf=getSharedPreferences(PreferenceUtil.PREFERENCE_NAME, MODE_PRIVATE);//判断是否是第一次进入
        boolean isFirstIn=sf.getBoolean("isFirstIn", true);
        SharedPreferences.Editor editor=sf.edit();
        if(isFirstIn) {     //若为true，则是第一次进入
            editor.putBoolean("isFirstIn", false);
            mhandler.sendEmptyMessageDelayed(GO_GUIDE, TIME);//将欢迎页停留5秒，并且将message设置为跳转到                                                             引导页SplashActivity，跳转在goGuide中实现
        }else{
            mhandler.sendEmptyMessageDelayed(GO_MAIN, TIME);//将欢迎页停留5秒，并且将message设置文跳转到                                                                   MainActivity，跳转功能在goMain中实现
        }
        editor.commit();

        }

    private void goMain() {
        Intent intent=new Intent(SplashActivity.this,WuyaActivity.class);
        startActivity(intent);
        finish();

    }
    private void goGuide() {
        Intent intent=new Intent(SplashActivity.this,WuyaActivity.class);
        startActivity(intent);
        finish();
    }
}
