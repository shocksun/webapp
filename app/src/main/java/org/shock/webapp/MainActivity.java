package org.shock.webapp;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.webkit.WebSettings;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WebAppWebView webView = new WebAppWebView(this);
        setContentView(webView);
        WebAppWebView.setWebContentsDebuggingEnabled(true);
        webView.injectionJavascript(new TestRunMethod(),"testrun");
        webView.loadUrl("http://192.168.0.18:5500/index.html");
    }
}
