package org.shock.webapp;

import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.webkit.WebSettings;

public class MainActivity extends AppCompatActivity {



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SwipeRefreshLayout swipeRefreshLayout = new SwipeRefreshLayout(this);
        WebAppWebView webView = new WebAppWebView(this,swipeRefreshLayout);
        swipeRefreshLayout.addView(webView);
        setContentView(swipeRefreshLayout);
        webView.setRefreshLayout(swipeRefreshLayout);
        WebAppWebView.setWebContentsDebuggingEnabled(true);
        webView.injectionJavascript(new AndroidMethod(this),"androidmethod");
        webView.loadUrl("http://192.168.1.105:5500/index.html");
    }
}
