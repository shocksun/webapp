package org.shock.webapp;

import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.util.JSONUtils;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InjectedWebChromeClient extends WebChromeClient {
    private WebView webView;
    private boolean isInject=false;
    private Map<String, Object> injectInstance = new HashMap<String, Object>();
    private Map<String, Method> injectMethods = new HashMap<String, Method>();
    public InjectedWebChromeClient(WebView webView){
        this.webView=webView;
    }

    @Override
    public void onProgressChanged(WebView view, int newProgress) {
        super.onProgressChanged(view, newProgress);
        if(newProgress<=25) {
            isInject = false;
        }else if(!isInject){
            initJavaScript();
            isInject=true;
        }
    }

    public void initJavaScript(){
        StringBuffer sb = new StringBuffer();
        sb.append("(function(w){if(!w.android)return;function execfunction(){let req=this;req.types=[];req.params=[];for(let any of arguments){if(typeof any==='number'){req.types.push('number');req.params.push(any)}else if(typeof any==='boolean'){req.types.push('boolean');req.params.push(any)}else if(typeof any==='object'){req.types.push('object');req.params.push(JSON.stringify(any))}else if(typeof any==='function'){req.types.push('function');let index=inject.funcs.push(any);req.params.push(index-1)}else if(typeof any==='string'){req.types.push('string');req.params.push(any)}else{req.types.push('undefined');req.params.push(null)}}req.types=JSON.stringify(req.types);req.params=JSON.stringify(req.params);return w.android.exec(req.prefix,req.methodname,req.types,req.params)}w._android={execfunc:function(index,args){let fun=inject.funcs[index],types=args.types,params=args.params;return fun.apply(w,params)}};let inject={funcs:[],inject(prefix,methodname){console.log(`inject ${prefix}.${methodname}`);if(!w[prefix]){w[prefix]={}}w[prefix][methodname]=function(){return execfunction.apply({prefix,methodname},arguments)}}};let event=new Event(\"onAndroidMethodInit\");event.initEvent(\"androidinit\",false,true);");
        injectionJavascript(sb);
        sb.append("document.dispatchEvent(event)})(window)");
        webView.evaluateJavascript(sb.toString(),null);
    }
    public void addJavascriptInterface(Object object,String name){
        injectInstance.put(name,object);
    }
    public String exec(String prefix,String methodname,String types,String params){
        JSONObject result = new JSONObject();
        Object instance = injectInstance.get(prefix);
        result.put("code",0);
        result.put("msg","没找到"+prefix);
        if(instance==null)return result.toString();
        Method method = injectMethods.get(prefix + "_" + methodname);
        result.put("msg","没找到"+prefix + "_" + methodname);
        if(method==null)return result.toString();
        return execMethod(instance,method,types,params);
    }
    private String execMethod(Object instance,Method method,String types,String params){
        JSONObject result = new JSONObject();
        result.put("code",500);
        try {
            JSONArray ts = JSONArray.fromObject(types);
            JSONArray ps = JSONArray.fromObject(params);
            Object[] paramsObj = new Object[ts.size()];
            for (int i = 0; i < ts.size(); i++) {
                Object type = ts.get(i);
                final Object value = ps.get(i);
                if(JSONUtils.isNull(value)) {//如果是null就不用判断了
                    paramsObj[i]=null;
                }
                if(type.equals("number")){//数字类型
                    try {
                        if(value.toString().indexOf('.')!=-1){//浮点数
                            paramsObj[i]=new BigDecimal(value.toString());
                        }else{//整数
                            paramsObj[i]=new BigInteger(value.toString());
                        }
                    }catch(Exception e) {
                        result.put("msg","数字类型转换错误");
                        return result.toString();
                    }
                }else if(type.equals("boolean")){
                    if(value.equals(Boolean.FALSE)){//
                        paramsObj[i]=Boolean.FALSE;
                    }else{
                        paramsObj[i]=Boolean.TRUE;
                    }
                }else if(type.equals("object")){
                    if(JSONUtils.isArray(value)){
                        List<Object> list = new ArrayList<Object>();
                        paramsObj[i]=JSONArray.fromObject(value);
                    }else{
                        Map<Object,Object> map = new HashMap<Object, Object>();
                        paramsObj[i]=JSONObject.fromObject(value);
                    }
                }else if(type.equals("function")){
                    paramsObj[i] = (JavaScriptFunction) (callback,objs) -> {
                        JSONArray paramArr = new JSONArray();
                        paramArr.add(value);
                        JSONObject datas = new JSONObject();
                        datas.put("types","");
                        datas.put("params",objs);
                        paramArr.add(datas);
                        webView.post(()->{
                            webView.evaluateJavascript("_android.execfunc.apply(this," + paramArr.toString() + ")", callback);
                        });
                    };
                }else if(type.equals("string")){
                    paramsObj[i]=String.valueOf(value);
                }else{
                    paramsObj[i]=null;
                }
            }
            return invokeInjectMethod(instance,method,paramsObj);
        }catch (Exception e){
            result.put("msg","系统异常");
            result.put("exception",e.toString());
            return result.toString();
        }
    }
    public String invokeInjectMethod(Object instance,Method method,Object[] params){
        Class[] parameterTypes = method.getParameterTypes();
        Object[] newParams = verificatParameters(parameterTypes,params);
        JSONObject result = new JSONObject();
        result.put("code",500);
        try {
            Object res = method.invoke(instance,newParams);
            result.put("code",1);
            result.put("result",res);
        }catch (Exception e){
            result.put("msg","系统异常");
            result.put("exception",e.toString());
        }
        return result.toString();
    }
    private Object[] verificatParameters(Class[] parameterTypes,Object[] obj){
        Object[] newParams = new Object[parameterTypes.length];
        for (int i = 0; i < newParams.length; i++) {
            Class type = parameterTypes[i];
            Object param = obj[i];
            if(type.isAssignableFrom(Object.class)){//Object类型
                newParams[i]=param;
            }else if(type.isAssignableFrom(Boolean.class)||type.isAssignableFrom(boolean.class)){//Boolean类型
                if(param instanceof Boolean){
                    newParams[i]=param;
                }else{
                    newParams[i]=!(param==null||param.toString().isEmpty());//null和空串是false,其他是true
                }
            }else if(type.isAssignableFrom(Number.class)){//直接放数字
                if(param instanceof BigDecimal || param instanceof  BigInteger){
                    newParams[i]=param;
                }else{
                    newParams[i]=null;
                }
            }else if(type.isAssignableFrom(BigDecimal.class)){
                if(param instanceof BigDecimal){
                    newParams[i]=param;
                }else if(param instanceof BigInteger){
                    newParams[i]=new BigDecimal(param.toString());
                }else{
                    newParams[i]=null;
                }
            }else if(type.isAssignableFrom(BigInteger.class)){
                if(param instanceof BigInteger){
                    newParams[i]=param;
                }else if(param instanceof  BigDecimal){
                    newParams[i]=((BigDecimal) param).toBigInteger();
                }else{
                    newParams[i]=null;
                }
                //Number BigDecimal BigInteger 数字类型
            }else if(type.isAssignableFrom(JSON.class)){
                if(param instanceof JSONObject || param instanceof  JSONArray){
                    newParams[i]=param;
                }else {
                    newParams[i]=null;
                }
            }else if(type.isAssignableFrom(JSONArray.class)){
                if(param instanceof JSONArray){
                    newParams[i]=param;
                }else{
                    newParams[i]=null;
                }
            }else if(type.isAssignableFrom(JSONObject.class)){
                if(param instanceof  JSONObject){
                    newParams[i]=param;
                }else{
                    newParams[i]=null;
                }
                //JSON JSONObject JSONArray 对象类型
            }else if(type.isAssignableFrom(String.class)){//字符串
                newParams[i]=(param==null?null:param.toString());
            }else if(type.isAssignableFrom(JavaScriptFunction.class)){
                if(param instanceof JavaScriptFunction){
                    newParams[i]=param;
                }else{
                    newParams[i]=null;
                }
            }else{
                newParams[i]=null;
            }
        }
        return newParams;
    }

    public void injectionJavascript(StringBuffer sb){
        for (Map.Entry<String,Object> entry: injectInstance.entrySet()) {
            String name = entry.getKey();
            Object obj = entry.getValue();
            Class<?> clazz = obj.getClass();
            Method[] methods = clazz.getDeclaredMethods();
            for (Method method : methods) {
                Inject inject = method.getAnnotation(Inject.class);
                String methodname;
                if(inject!=null){
                    if(inject.value().isEmpty()){
                        methodname=method.getName();
                    }else{
                        methodname=inject.value();
                    }
                }else{
                    continue;
                }
                sb.append("inject.inject(\""+name+"\",\""+methodname+"\");");
                if(injectMethods.containsKey(name+"_"+methodname)){
                    continue;
                }
                injectMethods.put(name+"_"+methodname,method);
            }
        }
    }
}
