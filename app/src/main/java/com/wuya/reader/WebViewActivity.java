package com.wuya.reader;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;

import com.wuya.reader.util.ADFilterTool;

public class WebViewActivity extends AppCompatActivity implements MainHandlerConstant,View.OnClickListener {

    private WebView webView;

    private ImageButton btnPlay;
    private ImageButton btnFoward;
    private ImageButton btnRewind;
    private ImageButton btnHome;

    private static String currentUrl = "";

    private static final String HOME_URL = "file:///android_asset/main.html";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_view);
        init();
    }

    private void init(){
        webView = (WebView) findViewById(R.id.webView);
        //获取url，如果没有，则使用默认值
        Bundle bundle = this.getIntent().getExtras();
        //接收name值
        String url = HOME_URL;
        if (null != bundle) {
            if (null != bundle.getString("url") && !bundle.getString("url").isEmpty()) {
                url = bundle.getString("url");
            }
        }
        webView.loadUrl(url);
        //支持javascript
        webView.getSettings().setJavaScriptEnabled(true);
        // 设置可以支持缩放
        webView.getSettings().setSupportZoom(true);
        // 设置出现缩放工具
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setDisplayZoomControls(true);
        //扩大比例的缩放
        webView.getSettings().setUseWideViewPort(true);
        //自适应屏幕
        webView.getSettings().setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN);
        webView.getSettings().setLoadWithOverviewMode(true);
        //是否显示图片
        boolean showPic = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("showPic",true);
        webView.getSettings().setBlockNetworkImage(!showPic);

        webView.canGoBack();
        webView.canGoForward();
        //覆盖WebView默认使用第三方或系统默认浏览器打开网页的行为，使网页用WebView打开
        webView.setWebViewClient(new WebViewClient(){
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                // TODO Auto-generated method stub
                //返回值是true的时候控制去WebView打开，为false调用系统浏览器或第三方浏览器
                view.loadUrl(request.getUrl().toString());
                return true;
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                if(!url.contains(currentUrl)){
                    if (!ADFilterTool.hasAd(webView.getContext(), url)) {
                        return super.shouldInterceptRequest(view, request);//正常加载
                    }else{
                        return new WebResourceResponse(null,null,null);//含有广告资源屏蔽请求
                    }
                }else{
                    return super.shouldInterceptRequest(view, request);
                }


            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                currentUrl = url;
                super.onPageStarted(view, url, favicon);
            }

        });

        btnPlay = (ImageButton) findViewById(R.id.reader);
        btnPlay.setOnClickListener(this);
        btnFoward = (ImageButton) findViewById(R.id.forward);
        btnFoward.setOnClickListener(this);
        btnRewind = (ImageButton) findViewById(R.id.rewind);
        btnRewind.setOnClickListener(this);
        btnHome = (ImageButton) findViewById(R.id.home);
        btnHome.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.reader:
                //打开当前页面
                Intent intent =new Intent();
                String url = currentUrl;
                if (!url.contains("http")) {
                    url = "";
                }
                if (url.contains("?")) {
                    url = url.substring(0,url.indexOf("?"));
                }
                intent.putExtra("url",url);
                setResult(RESULT_OK, intent);
                this.finish();
                break;
            case R.id.forward:
                //打开当前页面
                webView.goForward();
                break;
            case R.id.rewind:
                //打开当前页面
                webView.goBack();
                break;
            case R.id.home:
                //打开当前页面
                webView.loadUrl(HOME_URL);
                break;
            default:
                break;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
            //返回主页
            Intent intent =new Intent();

            setResult(RESULT_CANCELED, intent);
            this.finish();
        }
        return super.onKeyDown(keyCode, event);
    }
}
