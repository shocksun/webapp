package org.shock.webapp;

import android.content.Context;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;


public class WebAppWebView extends WebView {

    private OnScrollChange onScrollChange;
    private InjectedChromeClient injectedChromeClient;

    public WebAppWebView(Context context) {
        super(context);
        init();
    }
    private void init(){
        WebSettings settings = getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setUseWideViewPort(true);
        settings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        settings.setDomStorageEnabled(true);
        addJavascriptInterface(this,"android");
        injectedChromeClient = new InjectedChromeClient(this);
        setWebChromeClient(injectedChromeClient);
    }
    public void injectionJavascript(Object object,String name){
        injectedChromeClient.addJavascriptInterface(object,name);
    }
    @JavascriptInterface
    public String exec(String prefix,String methodname,String types,String params){
        return injectedChromeClient.exec(prefix,methodname,types,params);
    }
    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        if(onScrollChange!=null){
            onScrollChange.onScroll(l,t,l-oldl,t-oldt);
        }
    }

    public OnScrollChange getOnScrollChange() {
        return onScrollChange;
    }

    public void setOnScrollChange(OnScrollChange onScrollChange) {
        this.onScrollChange = onScrollChange;
    }

    public interface OnScrollChange{
        void onScroll(int dx,int dy,int dx_change,int dy_change);
    }

}
