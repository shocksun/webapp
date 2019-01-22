package org.shock.webapp;

import android.content.Context;
import android.util.AttributeSet;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;

import com.google.common.base.Strings;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.util.JSONUtils;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.math.NumberUtils;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class WebAppWebView extends WebView {

    private OnScrollChange onScrollChange;

    public WebAppWebView(Context context) {
        super(context);
    }

    public WebAppWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public WebAppWebView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void init(){
        this.evaluateJavascript("(function(w){\n" +
                "    if(!w._android)return;\n" +
                "    function execfunction(){\n" +
                "        let req=this\n" +
                "        req.types=[]\n" +
                "        req.params=[]\n" +
                "        for(let any of arguments){\n" +
                "            if (typeof any === 'number'){\n" +
                "                req.types.push('number')\n" +
                "                req.params.push(any)\n" +
                "            }else if (typeof any === 'boolean'){\n" +
                "                req.types.push('boolean')\n" +
                "                req.params.push(any)\n" +
                "            }else if (typeof any === 'object'){\n" +
                "                req.types.push('object')\n" +
                "                req.params.push(any)\n" +
                "            }else if (typeof any === 'function'){\n" +
                "                req.types.push('function')\n" +
                "                let index = w._android.init.funcs.push(any)\n" +
                "                req.params.push(index-1)\n" +
                "            }else if (typeof any === 'string'){\n" +
                "                req.types.push('string')\n" +
                "                req.params.push(any)\n" +
                "            }\n" +
                "        }\n" +
                "        req.types=JSON.stringify(req.types)\n" +
                "        req.params=JSON.stringify(req.params)\n" +
                "        w._android.exec.apply(w,Object.values(req))\n" +
                "    }\n" +
                "    _android.init={\n" +
                "        funcs:[],\n" +
                "        inject(prefix,methodname){\n" +
                "            if(!w.prefix){\n" +
                "                w[prefix]={}\n" +
                "            }\n" +
                "            w[prefix][methodname]=function(){\n" +
                "                execfunction.apply({prefix,methodname},arguments)\n" +
                "            }\n" +
                "        }\n" +
                "    }\n" +
                "})(window)",null);
        this.addJavascriptInterface(this,"_android");
    }
    private Map<String, Object> injectInstance = new HashMap<String, Object>();
    private Map<String, Method> injectMethods = new HashMap<String, Method>();
    @JavascriptInterface
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
        result.put("code",-1);
        try {
            JSONArray ts = JSONArray.fromObject(types);
            JSONArray ps = JSONArray.fromObject(params);
            Object[] paramsObj = new Object[ts.size()];
            for (int i = 0; i < ts.size(); i++) {
                Object type = ts.get(i);
                Object value = ps.get(i);
                if(JSONUtils.isNull(value)) {//如果是null就不用判断了
                    paramsObj[i]=null;
                }
                if(type.equals("number")){//数字类型
                    if(NumberUtils.isNumber(value.toString())){//如果是数字就检查数字的类型
                        if(value.toString().indexOf('.')!=-1){//浮点数
                            value=new BigDecimal(value.toString());
                        }else{//整数
                            value=new BigInteger(value.toString());
                        }
                    }else {//不是数字就返回
                        result.put("msg","类型错误");
                        return result.toString();
                    }
                    paramsObj[i]=value;
                }else if(type.equals("boolean")){
                    if(Strings.isNullOrEmpty(value.toString())){
                        value=Boolean.FALSE;
                    }else{
                        value=Boolean.TRUE;
                    }
                }else if(type.equals("object")){
                    if(JSONUtils.isArray(value)){
                        List<Object> list = new ArrayList<Object>();
                        eachJSONArray(JSONArray.fromObject(value),list);
                        value=list;
                    }else{
                        Map<Object,Object> map = new HashMap<Object, Object>();
                        eachJSONObject(JSONObject.fromObject(value),map);
                        value=map;
                    }
                }

            }
        }catch (Exception e){
            result.put("msg",e.getMessage());
            return result.toString();
        }
        result.put("msg","未知错误");
        return result.toString();
    }
    private void eachJSONObject(JSONObject jsonObject,Map<Object,Object> obj){
        Iterator keys = jsonObject.keys();
        while (keys.hasNext()){
            Object key = keys.next();
            Object value = jsonObject.get(key);
            if(value instanceof JSONArray){
                List<Object> list = new ArrayList<Object>();
                eachJSONArray((JSONArray) value,list);
                obj.put(key,list);
            }else if(value instanceof  JSONObject){
                Map<Object,Object> map = new HashMap<Object, Object>();
                eachJSONObject((JSONObject)value,map);
                obj.put(key,map);
            }else{
                obj.put(key,value);
            }
        }

    }
    private void eachJSONArray(JSONArray jsonArray,List<Object> arr){
        for (Object obj : jsonArray) {
            if(obj instanceof JSONArray){
                List<Object> list = new ArrayList<Object>();
                eachJSONArray((JSONArray) obj,list);
                arr.add(list);
            }else if(obj instanceof  JSONObject){
                Map<Object,Object> map = new HashMap<Object, Object>();
                eachJSONObject((JSONObject)obj,map);
                arr.add(map);
            }else{
                arr.add(obj);
            }
        }
    }
    public void injectionJavascript(Object obj,String name){
        if(injectInstance.containsKey(name)){
            throw new Error(name+"已存在");
        }
        injectInstance.put(name,obj);
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
            if(injectMethods.containsKey(name+"_"+methodname)){
                throw new Error(name+"_"+methodname+"已存在");
            }
            evaluateJavascript("_android.init.inject(\""+name+"\",\""+methodname+"\")",null);
            Class<?>[] parameterTypes = method.getParameterTypes();
            for (Class<?> parameterType : parameterTypes) {//配合js的类型 数字类型是Number 数组是Object[] 函数是 JavaScriptFunction 对象是Map
                if(parameterType.isAssignableFrom(Number.class)){//数字
                    methodname += "_n";
                }else if(parameterType.isAssignableFrom(List.class)) {//数组
                    methodname += "_a";
                }else if(parameterType.isAssignableFrom(JavaScriptFunction.class)){ //函数
                    methodname += "_f";
                }else if(parameterType.isAssignableFrom(Map.class)){//对象
                    methodname += "_o";
                }else if(parameterType.isAssignableFrom(String.class)){//字符串
                    methodname += "_s";
                }else if(parameterType.isAssignableFrom(Boolean.class)) {//布尔
                    methodname += "_b";
                }else{
                    throw new Error("未支持的类型");
                }
            }
            injectMethods.put(name+"_"+methodname,method);
        }
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
