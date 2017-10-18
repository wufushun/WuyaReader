package com.wuya.reader;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.util.Pair;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import com.baidu.tts.client.SpeechSynthesizer;
import com.baidu.tts.client.SpeechSynthesizerListener;
import com.baidu.tts.client.TtsMode;
import com.wuya.reader.control.InitConfig;
import com.wuya.reader.control.MySyntherizer;
import com.wuya.reader.control.NonBlockSyntherizer;
import com.wuya.reader.listener.UiMessageListener;
import com.wuya.reader.util.FileUtil;
import com.wuya.reader.util.HttpUtil;
import com.wuya.reader.util.OfflineResource;
import com.wuya.reader.util.PreferenceUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WuyaActivity extends AppCompatActivity implements MainHandlerConstant,View.OnClickListener {

    private EditText mInput;
    private TextView mShowText;
    private Handler mainHandler;
    private EditText hrefEdit;
    private ImageButton btnWebPage;
    private ImageButton btnLocalFile;
    private SeekBar processSeekBar;

    private EditText textProgress;

    private BottomNavigationView navigation;

    private final static int BROWSE_FOLDER = 0;
    private final static int BROWSE_WEB = 1;

    private int browseMode = BROWSE_WEB;
    private long skipLength = 0;
    private long fileSize = 0;
    private final int READ_LENGTH = 3000;
    private static String unfinishedContent="";

    private static String finishedContent="";

    private final static int PLAY_NORMAL = 0;
    private final static int PLAY_FORWARD = 1;
    private final static int PLAY_REWIND = 2;
    // ================== 初始化参数设置开始 ==========================
    /**
     * 发布时请替换成自己申请的appId appKey 和 secretKey。注意如果需要离线合成功能,请在您申请的应用中填写包名。
     * 本demo的包名是com.baidu.tts.sample，定义在build.gradle中。
     */
    protected String appId = "10242891";

    protected String appKey = "WlfOtQqoMmQv3CbClDyuQskp";

    protected String secretKey = "87cfb0d49e2f466f318b422e26aa9d74";

    // TtsMode.MIX; 离在线融合，在线优先； TtsMode.ONLINE 纯在线； 没有纯离线
    protected TtsMode ttsMode = TtsMode.MIX;

    // ===============初始化参数设置完毕，更多合成参数请至getParams()方法中设置 =================

    // 主控制类，所有合成控制方法从这个类开始
    protected MySyntherizer synthesizer;

    protected String DESC = "请先看完说明。之后点击“合成并播放”按钮即可正常测试。\n" +
            "测试离线合成功能需要首次联网。\n" +
            "纯在线请修改代码里ttsMode为TtsMode.ONLINE， 没有纯离线。\n" +
            "本Demo的默认参数设置为wifi情况下在线合成, 其它网络（包括4G）使用离线合成。 在线普通女声发音，离线男声发音.\n" +
            "合成可以多次调用，SDK内部有缓存队列，会依次完成。\n\n";

    private final String TAG = "WuyaActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wuya);
        initialViews(); // 配置onclick
        initPermission(); // android 6.0以上动态权限申请

        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        mainHandler = new Handler() {
            /*
             * @param msg
             */
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                handle(msg);
            }

        };

        initialTts(); // 初始化TTS引擎
    }

    @Override
    protected void onDestroy() {
        //记住最后的文章和位置
        rememberSkip();
        synthesizer.release();
        super.onDestroy();
    }

    private void rememberSkip() {
        SharedPreferences userSettings = getSharedPreferences(PreferenceUtil.PREFERENCE_NAME, 0);
        SharedPreferences.Editor editor = userSettings.edit();
        editor.putLong(hrefEdit.getText().toString(),skipLength);
        editor.putString("lastFile",hrefEdit.getText().toString());
        editor.commit();
    }


    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.play_pause:
                    if(item.getTitle().equals(getResources().getString(R.string.title_play))) {
                        item.setTitle(R.string.title_pause);
                        item.setIcon(R.drawable.pause_48);
                        // 合成前可以修改参数：
                        Map<String, String> params = getParams();
                        synthesizer.setParams(params);
                        loadModel();
                        readResult(PLAY_NORMAL);
                    }
                    else {
                        item.setTitle(R.string.title_play);
                        item.setIcon(R.drawable.play_48);
                        stop();
                    }
                    //通知系统更新菜单
                    supportInvalidateOptionsMenu();
                    return true;
                case R.id.forward:
                    readResult(PLAY_FORWARD);
                    navigation.getMenu().getItem(0).setTitle(R.string.title_play);
                    navigation.getMenu().getItem(0).setIcon(R.drawable.play_48);
                    return true;
                case R.id.rewind:
                    readResult(PLAY_REWIND);
                    navigation.getMenu().getItem(0).setTitle(R.string.title_play);
                    navigation.getMenu().getItem(0).setIcon(R.drawable.play_48);
                    return true;
                case R.id.settings:
                    startAct(SettingsActivity.class);
                    return true;
            }
            return false;
        }

    };


    /**
     * 初始化引擎，需要的参数均在InitConfig类里
     * <p>
     * DEMO中提供了3个SpeechSynthesizerListener的实现
     * MessageListener 仅仅用log.i记录日志，在logcat中可以看见
     * UiMessageListener 在MessageListener的基础上，对handler发送消息，实现UI的文字更新
     * FileSaveListener 在UiMessageListener的基础上，使用 onSynthesizeDataArrived回调，获取音频流
     */
    protected void initialTts() {
        // 设置初始化参数
        SpeechSynthesizerListener listener = new UiMessageListener(mainHandler); // 此处可以改为 含有您业务逻辑的SpeechSynthesizerListener的实现类

        Map<String, String> params = getParams();

        String offline_speaker = PreferenceManager.getDefaultSharedPreferences(this).getString("offline_speaker",OfflineResource.VOICE_FEMALE);

        // appId appKey secretKey 网站上您申请的应用获取。注意使用离线合成功能的话，需要应用中填写您app的包名。包名在build.gradle中获取。
        InitConfig initConfig = new InitConfig(appId, appKey, secretKey, ttsMode, offline_speaker, params, listener);

        synthesizer = new NonBlockSyntherizer(this, initConfig, mainHandler); // 此处可以改为MySyntherizer 了解调用过程
    }

    /**
     * 合成的参数，可以初始化时填写，也可以在合成前设置。
     *
     * @return
     */
    protected Map<String, String> getParams() {
        Map<String, String> params = new HashMap<String, String>();
        // 以下参数均为选填
        String volume=PreferenceManager.getDefaultSharedPreferences(this).getString("volume","5");
        String speed=PreferenceManager.getDefaultSharedPreferences(this).getString("speed","5");
        String pitch=PreferenceManager.getDefaultSharedPreferences(this).getString("pitch","5");
        String online_speaker=PreferenceManager.getDefaultSharedPreferences(this).getString("online_speaker","0");
        params.put(SpeechSynthesizer.PARAM_SPEAKER, online_speaker); // 设置在线发声音人： 0 普通女声（默认） 1 普通男声 2 特别男声 3 情感男声<度逍遥> 4 情感儿童声<度丫丫>
        params.put(SpeechSynthesizer.PARAM_VOLUME, volume); // 设置合成的音量，0-9 ，默认 5
        params.put(SpeechSynthesizer.PARAM_SPEED, speed);// 设置合成的语速，0-9 ，默认 5
        params.put(SpeechSynthesizer.PARAM_PITCH, pitch);// 设置合成的语调，0-9 ，默认 5
        params.put(SpeechSynthesizer.PARAM_MIX_MODE, SpeechSynthesizer.MIX_MODE_DEFAULT);         // 该参数设置为TtsMode.MIX生效。即纯在线模式不生效。
        // MIX_MODE_DEFAULT 默认 ，wifi状态下使用在线，非wifi离线。在线状态下，请求超时6s自动转离线
        // MIX_MODE_HIGH_SPEED_SYNTHESIZE_WIFI wifi状态下使用在线，非wifi离线。在线状态下， 请求超时1.2s自动转离线
        // MIX_MODE_HIGH_SPEED_NETWORK ， 3G 4G wifi状态下使用在线，其它状态离线。在线状态下，请求超时1.2s自动转离线
        // MIX_MODE_HIGH_SPEED_SYNTHESIZE, 2G 3G 4G wifi状态下使用在线，其它状态离线。在线状态下，请求超时1.2s自动转离线
        return params;
    }

    /**
     * 切换离线发音。注意需要添加额外的判断：引擎在合成时该方法不能调用
     */
    private void loadModel() {

        String offline_speaker = PreferenceManager.getDefaultSharedPreferences(this).getString("offline_speaker",OfflineResource.VOICE_FEMALE);

        int result = synthesizer.loadModel(offline_speaker);
        checkResult(result, "loadModel");
    }

    private void loadLastFile() {
        SharedPreferences userSettings = getSharedPreferences(PreferenceUtil.PREFERENCE_NAME, 0);
        String lastFile = userSettings.getString("lastFile","");
        hrefEdit.setText(lastFile);
        if (!lastFile.isEmpty()) {
            skipLength = userSettings.getLong(lastFile,0);
            if (skipLength>0) {
                browseMode = BROWSE_FOLDER;
                readFile(skipLength,READ_LENGTH);
            }
        }
    }

    /**
     * 界面上的按钮对应方法
     */
    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.webPage:
                browseMode = BROWSE_WEB;
                String url = hrefEdit.getText().toString();
                skipLength = 0;
                rememberSkip();
                browseWebPage(url);
                break;
            case R.id.localFile:
                browseMode = BROWSE_FOLDER;
                rememberSkip();
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("text/*");//设置类型，我这里是任意类型，任意后缀的可以这样写。
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(intent,1);
            default:
                break;
        }
    }

    private void browseWebPage(String url) {
        url = "http://"+url.replace("http://","");
        String encoding = PreferenceManager.getDefaultSharedPreferences(this).getString("encoding","UTF-8");

        HttpUtil.doGetAsyn(url, encoding, new HttpUtil.CallBack() {
            @Override
            public void onRequestComplete(String result) {
                if (result.contains("<body")) {
                    result = result.substring(result.indexOf("<body")+5);
                    result = result.replaceAll("[a-zA-Z]|[._<>=\"-/:_';]","");
                }
                unfinishedContent = result;
                finishedContent = "";
                mainHandler.sendMessage(mainHandler.obtainMessage(UI_SPEECH_TEXT_FINISHED));
            }
        });
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Uri uri = data.getData();
        if (resultCode == Activity.RESULT_OK) {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {//4.4以后
                hrefEdit.setText(FileUtil.getPath(this, uri));
            }
            SharedPreferences userSettings = getSharedPreferences(PreferenceUtil.PREFERENCE_NAME, 0);
            skipLength = userSettings.getLong(hrefEdit.getText().toString(),0);
            readFile(skipLength,READ_LENGTH);
        }
    }


    private void readFile(long position,int length) {
        Map<String,String> result = FileUtil.openTextFile(hrefEdit.getText().toString(), position,length);
        skipLength = Long.parseLong(result.get("skip"));
        fileSize = Long.parseLong(result.get("fileSize"));
        unfinishedContent = result.get("content");
//        unfinishedContent = FileUtil.openTextFile(hrefEdit.getText().toString(),position,length);
        if (position == 0) {
            finishedContent = "";
        }
        mainHandler.sendMessage(mainHandler.obtainMessage(UI_SPEECH_TEXT_FINISHED));

    }

    private void readResult(int flag) {
        switch (flag) {
            case PLAY_NORMAL:
                if (!TextUtils.isEmpty(mInput.getText())) {
                    speak();
                    break;
                }

                if(unfinishedContent.contains("。")) {
                    String sentence = unfinishedContent.substring(0, unfinishedContent.indexOf("。")+1);
                    mInput.setText(sentence);
                    speak();
                    unfinishedContent = unfinishedContent.substring(unfinishedContent.indexOf("。")+1);
                    finishedContent += sentence;
                }
                else {
                    mInput.setText(unfinishedContent);
                    speak();
                    finishedContent += unfinishedContent;
                    unfinishedContent = "";
                }
                break;
            case PLAY_FORWARD:
                stop();
                if(unfinishedContent.contains("。")) {
                    String sentence = unfinishedContent.substring(0, unfinishedContent.indexOf("。")+1);
                    mInput.setText(sentence);
                    unfinishedContent = unfinishedContent.substring(unfinishedContent.indexOf("。")+1);
                    finishedContent += sentence;
                }
                break;
            case PLAY_REWIND:
                stop();
                if(finishedContent.contains("。")) {
                    String temp = finishedContent.substring(0,finishedContent.length()-2);
                    String sentence = finishedContent.substring(temp.lastIndexOf("。")+1);
                    mInput.setText(sentence);
                    finishedContent = finishedContent.substring(0,temp.lastIndexOf("。")+1);
                    unfinishedContent = sentence+unfinishedContent;
                }
                break;

        }


    }

    /**
     * speak 实际上是调用 synthesize后，获取音频流，然后播放。
     * 获取音频流的方式见SaveFileActivity及FileSaveListener
     * 需要合成的文本text的长度不能超过1024个GBK字节。
     */
    private void speak() {
        mShowText.setText("");
        String text = mInput.getText().toString();

        //从光标处开始读
        int cusorPosition = mInput.getSelectionStart();
        text = text.substring(cusorPosition);

        //需要合成的文本text的长度不能超过1024个GBK字节。
        if (TextUtils.isEmpty(mInput.getText())) {
            text = "欢迎使用乌鸦读书,百度语音为你提供支持。";
            mInput.setText(text);
        }

        int result;
        if (text.length()>512) {
            List<Pair<String, String>> texts = new ArrayList<Pair<String, String>>();
            int i=0;
            do {
                texts.add(new Pair<String, String>(text.substring(0,512), "a"+i));
                i++;
                text = text.substring(512);
            }while(text.length()>512);
            texts.add(new Pair<String, String>(text, "a"+i));
            result = synthesizer.batchSpeak(texts);
        }

        result = synthesizer.speak(text);
        checkResult(result, "speak");
    }

    private void checkResult(int result, String method) {
        if (result != 0) {
            toPrint("error code :" + result + " method:" + method);
        }
    }


    /**
     * 暂停播放。仅调用speak后生效
     */
    private void pause() {
        int result = synthesizer.pause();
        checkResult(result, "pause");
    }

    /**
     * 继续播放。仅调用speak后生效，调用pause生效
     */
    private void resume() {
        int result = synthesizer.resume();
        checkResult(result, "resume");
    }

    /*
     * 停止合成引擎。即停止播放，合成，清空内部合成队列。
     */
    private void stop() {
        int result = synthesizer.stop();
        checkResult(result, "stop");
    }

    protected void handle(Message msg) {
        switch (msg.what) {
            case INIT_SUCCESS:
                btnWebPage.setEnabled(true);
                btnLocalFile.setEnabled(true);
                msg.what = PRINT;
                if (fileSize==0) {
                    //加载最后播放的文件及位置
                    loadLastFile();
//                    processSeekBar.setProgress(Integer.parseInt(Long.toString(100*skipLength/fileSize)));
                }
                break;
        }
        switch (msg.what) {
            case PRINT:
                print(msg);
                break;
            case UI_CHANGE_INPUT_TEXT_SELECTION:
                if (msg.arg1 <= mInput.getText().length()) {
                    mInput.setSelection(0, msg.arg1);
                }
                break;
            case UI_CHANGE_SYNTHES_TEXT_SELECTION:
                SpannableString colorfulText = new SpannableString(mInput.getText().toString());
                if (msg.arg1 <= colorfulText.toString().length()) {
                    colorfulText.setSpan(new ForegroundColorSpan(Color.GRAY), 0, msg.arg1, Spannable
                            .SPAN_EXCLUSIVE_EXCLUSIVE);
                    mInput.setText(colorfulText);
                }
                break;
            case UI_SPEECH_TEXT_FINISHED:
                if(unfinishedContent.length()==0) {
                    if (browseMode == BROWSE_FOLDER) {
                        readFile(skipLength, READ_LENGTH);

                        if (unfinishedContent.length()==0) {
                            stop();
                            if (navigation.getMenu().getItem(0).getTitle().equals(getResources().getString(R.string.title_pause))) {
                                navigation.getMenu().getItem(0).setTitle(R.string.title_play);
                                navigation.getMenu().getItem(0).setIcon(R.drawable.play_48);
                            }
                        }
                    }
                    else {
                        //自动读取下一篇文章，适用于文章顺序排列的场景，可能需要根据不同的url进行定制
                        String url = hrefEdit.getText().toString();
                        String path = url.substring(0,url.lastIndexOf("/")+1);
                        String filename = url.substring(url.lastIndexOf("/")+1,url.lastIndexOf("."));
                        String newUrl = path+(Integer.parseInt(filename)+1)+url.substring(url.lastIndexOf("."));
                        hrefEdit.setText(newUrl);
                        rememberSkip();
                        browseWebPage(newUrl);
                    }
                    break;
                }
                if(unfinishedContent.contains("。")) {
                    String sentence = unfinishedContent.substring(0, unfinishedContent.indexOf("。")+1);

                    mInput.setText(sentence);
                    speak();
                    unfinishedContent = unfinishedContent.substring(unfinishedContent.indexOf("。")+1);
                    finishedContent += sentence;
                }
                else {
                    mInput.setText(unfinishedContent);
                    speak();
                    finishedContent += unfinishedContent;
                    unfinishedContent = "";
                }
                if (navigation.getMenu().getItem(0).getTitle().equals(getResources().getString(R.string.title_play))) {
                    navigation.getMenu().getItem(0).setTitle(R.string.title_pause);
                    navigation.getMenu().getItem(0).setIcon(R.drawable.pause_48);
                }
                break;
            default:
                break;
        }
    }

    private void initialViews() {
        btnWebPage = (ImageButton) findViewById(R.id.webPage);
        btnWebPage.setOnClickListener(this);
        btnWebPage.setEnabled(false);
        btnLocalFile = (ImageButton) findViewById(R.id.localFile);
        btnLocalFile.setOnClickListener(this);
        btnLocalFile.setEnabled(false);

        mInput = (EditText) this.findViewById(R.id.input);
        hrefEdit = (EditText) findViewById(R.id.href);

        mShowText = (TextView) this.findViewById(R.id.showText);
        mShowText.setMovementMethod(new ScrollingMovementMethod());

        textProgress = (EditText) this.findViewById(R.id.textProgress);

        processSeekBar = (SeekBar) findViewById(R.id.progressSeekBar);
        processSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            // 数值改变
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                textProgress.setText(Integer.toString(progress));
            }

            // 开始拖动
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            // 停止拖动
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (browseMode == BROWSE_FOLDER) {
                    if (fileSize > 0) {
                        skipLength = fileSize / 100 * seekBar.getProgress();
                    } else {
                        skipLength = seekBar.getProgress();
                    }
                    finishedContent = "";
                    unfinishedContent = "";
                    readFile(skipLength, READ_LENGTH);
                }
            }
        });

        navigation = (BottomNavigationView) findViewById(R.id.navigation);

        mShowText.setText(DESC);
    }

    protected void toPrint(String str) {
        Message msg = Message.obtain();
        msg.obj = str;
        mainHandler.sendMessage(msg);
    }

    private void print(Message msg) {
        String message = (String) msg.obj;
        if (message != null) {
            scrollLog(message);
        }
    }

    private void scrollLog(String message) {
        Spannable colorMessage = new SpannableString(message + "\n");
        colorMessage.setSpan(new ForegroundColorSpan(0xff0000ff), 0, message.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        mShowText.append(colorMessage);
        Layout layout = mShowText.getLayout();
        if (layout != null) {
            int scrollAmount = layout.getLineTop(mShowText.getLineCount()) - mShowText.getHeight();
            if (scrollAmount > 0) {
                mShowText.scrollTo(0, scrollAmount + mShowText.getCompoundPaddingBottom());
            } else {
                mShowText.scrollTo(0, 0);
            }
        }
    }

    /**
     * android 6.0 以上需要动态申请权限
     */
    private void initPermission() {
        String permissions[] = {
                Manifest.permission.INTERNET,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.MODIFY_AUDIO_SETTINGS,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_SETTINGS,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.CHANGE_WIFI_STATE
        };

        ArrayList<String> toApplyList = new ArrayList<String>();

        for (String perm : permissions) {
            if (PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(this, perm)) {
                toApplyList.add(perm);
                //进入到这里代表没有权限.
            }
        }
        String tmpList[] = new String[toApplyList.size()];
        if (!toApplyList.isEmpty()) {
            ActivityCompat.requestPermissions(this, toApplyList.toArray(tmpList), 123);
        }

    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        // 此处为android 6.0以上动态授权的回调，用户自行实现。
    }

    private void startAct(Class activityClass) {
        startActivity(new Intent(this, activityClass));
    }

}
