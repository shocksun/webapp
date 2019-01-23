package org.shock.webapp;

import android.content.Context;
import android.widget.Toast;

import java.math.BigInteger;

public class AndroidMethod {

    private Context context;
    public AndroidMethod(Context context){
        this.context=context;
    }

    @Inject
    public void toast(String msg, BigInteger time){
        Toast toast = Toast.makeText(context,msg,time.intValue());
        toast.show();
    }

}
