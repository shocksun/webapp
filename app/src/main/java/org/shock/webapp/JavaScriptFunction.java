package org.shock.webapp;

import android.webkit.ValueCallback;

public interface JavaScriptFunction {
    void callback(ValueCallback<String> callback,Object... obj);
}
