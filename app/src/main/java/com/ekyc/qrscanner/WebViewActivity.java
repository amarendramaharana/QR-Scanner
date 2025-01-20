package com.ekyc.qrscanner;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;

public class WebViewActivity extends AppCompatActivity {
private  WebView webView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_view);
String url=getIntent().getStringExtra("URL");
webView=findViewById(R.id.webView);
//webView.setWebChromeClient(new WebChromeClient());
setWebViewSettings(webView);
      webView.loadUrl(url);
    }
    @SuppressLint("SetJavaScriptEnabled")
    private void setWebViewSettings(WebView webView) {
        WebSettings settings = webView.getSettings();
        // Enable Javascript  //true
        settings.setJavaScriptEnabled(true);
        // Use WideViewport and Zoom out if there is no viewport defined
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        // Enable pinch to zoom without the zoom buttons
        settings.setBuiltInZoomControls(true);
        // Allow use of Local Storage
        settings.setDomStorageEnabled(true);
        // Hide the zoom controls for HONEYCOMB+
        settings.setDisplayZoomControls(false);
        // Enable remote debugging via chrome://inspect
        WebView.setWebContentsDebuggingEnabled(false);
    }
    @Override
    public void onBackPressed() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}