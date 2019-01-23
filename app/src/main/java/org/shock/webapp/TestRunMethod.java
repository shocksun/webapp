package org.shock.webapp;

public class TestRunMethod {

    @Inject
    public String testcallback(String str1,String str2,JavaScriptFunction function){
        function.callback(value->{
            System.out.println(value);
        },str1,str2);
        return "这是返回结果";
    }

}
