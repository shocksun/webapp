package org.shock.webapp;

import android.support.v4.widget.SwipeRefreshLayout;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class InjectedWebViewClient extends WebViewClient {
    private WebView webView;
    private SwipeRefreshLayout swipeRefreshLayout;

    public InjectedWebViewClient(WebView webView, SwipeRefreshLayout swipeRefreshLayout) {
        this.webView=webView;
        this.swipeRefreshLayout=swipeRefreshLayout;
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        super.onPageFinished(view, url);
        swipeRefreshLayout.setRefreshing(false);
    }
}
