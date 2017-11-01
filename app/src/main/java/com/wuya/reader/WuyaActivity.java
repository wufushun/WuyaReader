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
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.Layout;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.Pair;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupWindow;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.tts.client.SpeechSynthesizer;
import com.baidu.tts.client.SpeechSynthesizerListener;
import com.baidu.tts.client.TtsMode;
import com.wuya.reader.callback.SearchCallback;
import com.wuya.reader.control.InitConfig;
import com.wuya.reader.control.MySyntherizer;
import com.wuya.reader.control.NonBlockSyntherizer;
import com.wuya.reader.listener.UiMessageListener;
import com.wuya.reader.util.FileUtil;
import com.wuya.reader.util.HtmlContentUtil;
import com.wuya.reader.util.HttpUtil;
import com.wuya.reader.util.HttpsUtil;
import com.wuya.reader.util.OfflineResource;
import com.wuya.reader.util.PreferenceUtil;
import com.wuya.reader.util.ViewUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WuyaActivity extends AppCompatActivity implements MainHandlerConstant,View.OnClickListener, View.OnTouchListener {

    private TextView contentTextView;
    private ScrollView contentScrollView;
    private Handler mainHandler;
    private EditText hrefEditText;

    private ImageButton btnWebPage;
    private ImageButton btnLocalFile;
    private ImageButton btnGo;

    private ImageButton btnSearch;
    private ImageButton btnPlayPause;
    private ImageButton btnSettings;

    /*查找条件*/
    private PopupWindow searchPopupWindow;

    private final static int BROWSE_FOLDER = 0;
    private final static int BROWSE_WEB = 1;

    private int browseMode = BROWSE_WEB;
    private long skipLength = 0;
    private long fileSize = 0;

    private static String unfinishedContent="";

    private static String finishedContent="";

    private String nextUrl = "";

    private final static int PLAY_NORMAL = 0;
    private final static int PLAY_PAUSE = 1;
    private final static int PLAY_FORWARD = 2;
    private final static int PLAY_REWIND = 3;

    private static int PLAY_STATE = PLAY_PAUSE;

    private double oldDist = 0;
    private static final int NONE = 0;// 空
    private static final int DRAG = 1;// 按下第一个点
    private static final int ZOOM = 2;// 按下第二个点
    private static int mode = 0;
    /**最小字体*/
    public static final float MIN_TEXT_SIZE = 10f;

    /**最大字体*/
    public static final float MAX_TEXT_SIZE = 100.0f;

    /** 设置字体大小 */
    float textSize;

    private static int NUMBER_PER_PAGE = 250;

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

    private final String TAG = "WuyaActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wuya);
        initialViews(); // 配置onclick
        initPermission(); // android 6.0以上动态权限申请

        mainHandler = new Handler() {
            /*
             * @param msg
             */
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                isExit = false;
                handle(msg);
            }

        };

        HttpsUtil.initHttpsUrlConnection(this);

        initialTts(); // 初始化TTS引擎
    }

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

    //启动时加载最后的记录
    private void loadLastFile() {
        SharedPreferences userSettings = getSharedPreferences(PreferenceUtil.PREFERENCE_NAME, MODE_PRIVATE);
        String lastFile = userSettings.getString("lastFile","");
        skipLength = userSettings.getLong("skipLength",0);
        hrefEditText.setText(lastFile);
        if (!lastFile.isEmpty()) {
            if (lastFile.contains("http://") || lastFile.contains("https://")) {
                browseMode = BROWSE_WEB;
                browseWebPage(lastFile);
            }
            else {
                browseMode = BROWSE_FOLDER;
                if (lastFile.indexOf("file:///")==-1) {
                    readFile(skipLength);
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        //记住最后的文章和位置
        rememberSkip();
        synthesizer.release();
        super.onDestroy();
    }

    private void rememberSkip() {
        String url = hrefEditText.getText().toString();

        if (!url.isEmpty()) {
            SharedPreferences userSettings = getSharedPreferences(PreferenceUtil.PREFERENCE_NAME, 0);
            SharedPreferences.Editor editor = userSettings.edit();
            if (browseMode == BROWSE_WEB) {
                skipLength = finishedContent.length();
            }
            editor.putLong("skipLength", skipLength);
            editor.putString("lastFile", hrefEditText.getText().toString());
            editor.commit();
        }
    }


    /**
     * 界面上的触摸事件
     * @param v
     * @param event
     * @return
     */
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                mode = NONE;
                break;
            case MotionEvent.ACTION_DOWN:
                mode = DRAG;
                //左边前翻页，右边后翻页
                int left = contentTextView.getLeft();
                int right = contentTextView.getRight();
                if (event.getX()>(left+(right-left)*3/4) && event.getX()<right) {
                    setPlayPauseButton(PLAY_PAUSE);
                    nextSentence();
                }
                else if (event.getX()<(left+(right-left)/4) && event.getX()>left) {
                    setPlayPauseButton(PLAY_PAUSE);
                    prevSentence();
                }
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                oldDist = spacing(event);
                if (oldDist > 10f)
                {
                    mode = ZOOM;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (mode == ZOOM)
                {
                    // 正在移动的点距初始点的距离
                    double newDist = spacing(event);

                    if (newDist > oldDist)
                    {
                        zoomOut();
                    }
                    else if (newDist < oldDist)
                    {
                        zoomIn();
                    }
                }
                break;
            default:
                break;
        }
        /**
         *  注意返回值
         *  true：view继续响应Touch操作；
         *  false：view不再响应Touch操作，故此处若为false，只能显示起始位置，不能显示实时位置和结束位置
         */
        return true;
    }

    /**
     * 求出2个触点间的 距离
     *
     * @param event
     * @return
     */
    private double spacing(MotionEvent event)
    {
        double x = event.getX(0) - event.getX(1);
        double y = event.getY(0) - event.getY(1);
        return Math.sqrt(x * x + y * y);
    }

    /**
     * 放大
     */
    protected void zoomOut()
    {
        textSize += 0.5f;;
        if (textSize > MAX_TEXT_SIZE)
        {
            textSize = MAX_TEXT_SIZE;
        }
        contentTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX,textSize);
        resetNumberPerPage();
        resetContent();
    }

    /**
     * 缩小
     */
    protected void zoomIn()
    {
        textSize -= 0.5f;;
        if (textSize < MIN_TEXT_SIZE)
        {
            textSize = MIN_TEXT_SIZE;
        }
        contentTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX,textSize);
        resetNumberPerPage();
        resetContent();
    }

    //计算每页能显示多少字
    private void resetNumberPerPage() {
        //获取当前页总行数
        contentTextView.getExtendedPaddingTop();
        Layout layout = contentTextView.getLayout();
        if (null == layout) {
            return;
        }
        int lines = layout.getLineForVertical(contentScrollView.getHeight()-contentTextView.getLineHeight());
        //获取当前页总字数
        int currentNumber = layout.getLineEnd(lines);

        if (currentNumber<NUMBER_PER_PAGE) {
            NUMBER_PER_PAGE = currentNumber;
        }
        else {
            NUMBER_PER_PAGE += 10;
        }
    }

    //调整内容区
    private void resetContent() {
        String sentence = contentTextView.getText().toString();
        if (sentence.length() > NUMBER_PER_PAGE) {
            contentTextView.setText(sentence.substring(0, NUMBER_PER_PAGE - 1));
            unfinishedContent = sentence.substring(NUMBER_PER_PAGE) + unfinishedContent;
        } else if (sentence.length() < NUMBER_PER_PAGE) {
            contentTextView.setText(sentence + unfinishedContent.substring(0, NUMBER_PER_PAGE - sentence.length() - 1));
            unfinishedContent = unfinishedContent.substring(NUMBER_PER_PAGE - sentence.length());
        }
    }
    /**
     * 界面上的按钮对应方法
     */
    @Override
    public void onClick(View v) {
        int id = v.getId();
        resetButton();
        Intent intent;
        String url = hrefEditText.getText().toString();
        switch (id) {
            case R.id.goButton:
                setPlayPauseButton(PLAY_PAUSE);
                skipLength = 0;

                if (!url.isEmpty()) {
                    if (url.contains("http://") || url.contains("https://")) {
                        browseMode = BROWSE_WEB;
                        browseWebPage(url);
                    }
                    else {
                        browseMode = BROWSE_FOLDER;
                        readFile(skipLength);
                    }
                }


                break;
            case R.id.webPageButton:
                setPlayPauseButton(PLAY_PAUSE);
                rememberSkip();
                browseMode = BROWSE_WEB;
                //打开当前页面
                intent = new Intent(this, WebViewActivity.class);

                if (!url.contains("http://") && !url.contains("http://")) {
                    url = "";
                }
                intent.putExtra("url",url);
                startActivityForResult(intent, BROWSE_WEB);

                break;
            case R.id.localFileButton:
                setPlayPauseButton(PLAY_PAUSE);
                rememberSkip();
                browseMode = BROWSE_FOLDER;
                intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("text/*");//设置类型，我这里是任意类型，任意后缀的可以这样写。
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(intent,BROWSE_FOLDER);
            case R.id.mainSearchButton:
                setPlayPauseButton(PLAY_PAUSE);
                stop();
                showSearchView();

                break;
            case R.id.playPauseButton:
                if(PLAY_STATE == PLAY_PAUSE) {
                    // 合成前可以修改参数：
                    Map<String, String> params = getParams();
                    synthesizer.setParams(params);
                    loadModel();

                    speak();
                    setPlayPauseButton(PLAY_NORMAL);
                }
                else {
                    setPlayPauseButton(PLAY_PAUSE);
                }

                break;
            case R.id.settingsButton:
                setPlayPauseButton(PLAY_PAUSE);
                startAct(SettingsActivity.class);

                break;
            default:
                break;
        }
    }

    private void resetButton() {
        btnGo.setBackgroundColor(Color.TRANSPARENT);
        btnWebPage.setBackgroundColor(Color.TRANSPARENT);
        btnLocalFile.setBackgroundColor(Color.TRANSPARENT);
        btnSearch.setBackgroundColor(Color.TRANSPARENT);
        btnPlayPause.setBackgroundColor(Color.TRANSPARENT);
        btnSettings.setBackgroundColor(Color.TRANSPARENT);
    }

    private void setPlayPauseButton(int flag) {
        switch (flag) {
            case PLAY_NORMAL:
                btnPlayPause.setImageDrawable(getResources().getDrawable(R.drawable.pause_48,getTheme()));
                PLAY_STATE = PLAY_NORMAL;
                break;
            default:
                stop();
                btnPlayPause.setImageDrawable(getResources().getDrawable(R.drawable.play_48,getTheme()));
                PLAY_STATE = PLAY_PAUSE;
        }
    }

    /*显示查找菜单*/
    private void showSearchView(){
        if(null == searchPopupWindow) {

            searchPopupWindow = ViewUtil.getSearchView(WuyaActivity.this, new SearchCallback() {

                @Override
                public void jumpTo(int progress) {
                    if (browseMode == BROWSE_FOLDER) {
                        finishedContent = "";
                        unfinishedContent = "";
                        readFile(progress);
                    }
                    else {
                        if (finishedContent.length()>0 || unfinishedContent.length()>0) {
                            skipLength = (finishedContent.length() + unfinishedContent.length()) * progress / 100;
                            finishedContent = (finishedContent + unfinishedContent).substring(0,(int)skipLength);
                            unfinishedContent = (finishedContent + unfinishedContent).substring((int)skipLength);
                            nextSentence();
                        }
                    }
                }

                @Override
                public void search(String searchContent) {
                    if (unfinishedContent.contains(searchContent)) {
                        String sentence = unfinishedContent.substring(0,unfinishedContent.indexOf(searchContent));
                        unfinishedContent = unfinishedContent.substring(unfinishedContent.indexOf(searchContent));
                        finishedContent += sentence;
                    }
                    else if (finishedContent.contains(searchContent)) {
                        String sentence = finishedContent.substring(0,finishedContent.indexOf(searchContent));
                        unfinishedContent = finishedContent.substring(finishedContent.indexOf(searchContent))+unfinishedContent;
                        finishedContent = sentence;
                        nextSentence();
                    }
                    else {
                        if (browseMode == BROWSE_FOLDER) {
                            searchFile(searchContent, skipLength);
                        }
                    }
                }
            });

            searchPopupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
                @Override
                public void onDismiss() {
                    resetButton();
                }
            });
        }

        if(searchPopupWindow.isShowing()) {

            searchPopupWindow.dismiss();

        }
        else {

            ViewUtil.showPopUp(WuyaActivity.this, btnSearch, searchPopupWindow);
        }
    }

    private void browseWebPage(String url) {
        unfinishedContent = "";
        finishedContent = "";
        fileSize = 1;
        if (url.contains("http://") || url.contains("https://")) {
        }
        else {
            url = "http://"+url;
        }
        String encoding = PreferenceManager.getDefaultSharedPreferences(this).getString("encoding","UTF-8");

        HttpUtil.doGetAsyn(url, encoding, new HttpUtil.CallBack() {
            @Override
            public void onRequestComplete(String result) {
                Map<String, String> resultMap = HtmlContentUtil.getHtmlContent(hrefEditText.getText().toString(),result);
                String content = resultMap.get("orientContent");
                nextUrl = resultMap.get("nextUrl");
                if (skipLength>0) {
                    unfinishedContent = content.substring((int)skipLength);
                    finishedContent = content.substring(0,(int)skipLength+1);
                }
                else {
                    unfinishedContent = content;
                    finishedContent = "";
                }
                mainHandler.sendMessage(mainHandler.obtainMessage(UI_SPEECH_TEXT_FINISHED));
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        skipLength = 0;

        switch (requestCode) {
            case BROWSE_FOLDER://浏览文件夹结果
                Uri uri = data.getData();
                if (resultCode == Activity.RESULT_OK) {
                    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {//4.4以后
                        hrefEditText.setText(FileUtil.getPath(this, uri));
                    }
                    readFile(skipLength);
                }
                break;
            case BROWSE_WEB://浏览网页结果
                String url = data.getStringExtra("url");
                if (null != url && !url.isEmpty()) {
                    hrefEditText.setText(url);
                    browseWebPage(url);
                }
                break;
        }
    }


    private void readFile(long position) {
        Map<String,String> result = FileUtil.openTextFile(hrefEditText.getText().toString(), position);
        skipLength = Long.parseLong(result.get("skip"));
        fileSize = Long.parseLong(result.get("fileSize"));
        unfinishedContent = result.get("content");
        finishedContent = "";
        mainHandler.sendMessage(mainHandler.obtainMessage(UI_SPEECH_TEXT_FINISHED));

    }

    private void searchFile(String searchContent, long position) {
        if (searchContent.isEmpty()) {
            return;
        }
        Map<String,String> result = FileUtil.searchTextFile(hrefEditText.getText().toString(), position,searchContent);
        skipLength = Long.parseLong(result.get("skip"));
        fileSize = Long.parseLong(result.get("fileSize"));
        unfinishedContent = result.get("content");
        if (!unfinishedContent.isEmpty()) {
            finishedContent = "";
            mainHandler.sendMessage(mainHandler.obtainMessage(UI_SPEECH_TEXT_FINISHED));
        }
    }

    /**
     * speak 实际上是调用 synthesize后，获取音频流，然后播放。
     * 需要合成的文本text的长度不能超过1024个GBK字节。
     */
    private void speak() {
        String text = contentTextView.getText().toString();

        //从光标处开始读
        int cusorPosition = contentTextView.getSelectionStart();
        if (cusorPosition>-1) {
            text = text.substring(cusorPosition);
        }
        //需要合成的文本text的长度不能超过1024个GBK字节。
        if (TextUtils.isEmpty(contentTextView.getText())) {
            text = "欢迎使用乌鸦读书,百度语音为你提供支持。";
            contentTextView.setText(text);
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
        else {
            result = synthesizer.speak(text);
        }

        if (PLAY_STATE == PLAY_PAUSE) {
            setPlayPauseButton(PLAY_NORMAL);
        }

        checkResult(result, "speak");
    }

    private void checkResult(int result, String method) {
        if (result != 0) {
            toPrint("error code :" + result + " method:" + method);
        }
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
                btnGo.setEnabled(true);
                btnSearch.setEnabled(true);
                btnPlayPause.setEnabled(true);
                msg.what = PRINT;
                if (fileSize==0) {
                    //加载最后播放的文件及位置
                    loadLastFile();
//                    processSeekBar.setProgress(Integer.parseInt(Long.toString(100*skipLength/fileSize)));
                }
                break;
        }
        switch (msg.what) {
//            case PRINT:
//                print(msg);
//                break;
//            case UI_CHANGE_INPUT_TEXT_SELECTION:
//                if (msg.arg1 <= contentTextView.getText().length()) {
//                    contentTextView.setSelection(0, msg.arg1);
//                }
//                break;
//            case UI_CHANGE_SYNTHES_TEXT_SELECTION:
//                SpannableString colorfulText = new SpannableString(contentTextView.getText().toString());
//                if (msg.arg1 <= colorfulText.toString().length()) {
//                    colorfulText.setSpan(new ForegroundColorSpan(Color.GRAY), 0, msg.arg1, Spannable
//                            .SPAN_EXCLUSIVE_EXCLUSIVE);
//                    contentTextView.setText(colorfulText);
//                }
//                break;
            case UI_SPEECH_TEXT_FINISHED:
                nextSentence();
                if(!contentTextView.getText().toString().isEmpty()) {
                    speak();
                }
                break;
            default:
                break;
        }
    }

    //下一句
    private void nextSentence() {
        if(unfinishedContent.length()==0) {
            contentTextView.setText("");
            if (browseMode == BROWSE_FOLDER) {
                readFile(skipLength);

                if (unfinishedContent.length() == 0) {
                    stop();
                    setPlayPauseButton(PLAY_PAUSE);
                }
            } else {
                stop();
                if (!nextUrl.isEmpty()) {
                    hrefEditText.setText(nextUrl);
                    browseWebPage(nextUrl);
                }
            }
        }
        else {
            if (unfinishedContent.length()>NUMBER_PER_PAGE) {
                String sentence = unfinishedContent.substring(0, NUMBER_PER_PAGE);

                contentTextView.setText(sentence);
                unfinishedContent = unfinishedContent.substring(NUMBER_PER_PAGE);
                finishedContent += sentence;
            } else {
                contentTextView.setText(unfinishedContent);
                finishedContent += unfinishedContent;
                unfinishedContent = "";
            }
        }
    }

    //上一句
    private void prevSentence() {
        if(finishedContent.length()>NUMBER_PER_PAGE) {
            String sentence = finishedContent.substring(finishedContent.length()-NUMBER_PER_PAGE);
            contentTextView.setText(sentence);
            finishedContent = finishedContent.substring(0,finishedContent.length()-NUMBER_PER_PAGE);
            unfinishedContent = sentence+unfinishedContent;
        }
        else {
            contentTextView.setText(finishedContent);
            unfinishedContent = finishedContent+unfinishedContent;
            finishedContent = "";
        }
    }

    private void initialViews() {
        btnWebPage = (ImageButton) findViewById(R.id.webPageButton);
        btnWebPage.setOnClickListener(this);
        btnWebPage.setEnabled(false);
        btnLocalFile = (ImageButton) findViewById(R.id.localFileButton);
        btnLocalFile.setOnClickListener(this);
        btnLocalFile.setEnabled(false);
        btnGo = (ImageButton) findViewById(R.id.goButton);
        btnGo.setOnClickListener(this);
        btnGo.setEnabled(false);

        btnSearch = (ImageButton) findViewById(R.id.mainSearchButton);
        btnSearch.setOnClickListener(this);
        btnSearch.setEnabled(false);
        btnPlayPause = (ImageButton) findViewById(R.id.playPauseButton);
        btnPlayPause.setOnClickListener(this);
        btnPlayPause.setEnabled(false);
        btnSettings = (ImageButton) findViewById(R.id.settingsButton);
        btnSettings.setOnClickListener(this);

        contentTextView = (TextView) this.findViewById(R.id.contentTextView);
        contentTextView.setMovementMethod(ScrollingMovementMethod.getInstance());
        contentTextView.setOnTouchListener(this);
        textSize = contentTextView.getTextSize();

        contentScrollView = (ScrollView)this.findViewById(R.id.contentScrollView);
//        contentScrollView.setOnTouchListener(this);

        hrefEditText = (EditText) findViewById(R.id.hrefEditText);

        contentTextView.setText(R.string.app_desc);
    }

    protected void toPrint(String str) {
        Message msg = Message.obtain();
        msg.obj = str;
        mainHandler.sendMessage(msg);
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

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            exit();
            return false;
        }
        return super.onKeyDown(keyCode, event);
    }

    // 定义一个变量，来标识是否退出
    private static boolean isExit = false;

    private void exit() {
        if (!isExit) {
            isExit = true;
            Toast.makeText(getApplicationContext(), "再按一次退出程序",
                    Toast.LENGTH_SHORT).show();
            // 利用handler延迟发送更改状态信息
            mainHandler.sendEmptyMessageDelayed(0, 2000);
        } else {
            finish();
            onDestroy();
        }
    }
}
