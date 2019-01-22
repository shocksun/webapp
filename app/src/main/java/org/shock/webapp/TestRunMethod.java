package org.shock.webapp;

public class TestRunMethod {

    @Inject
    public void testcallback(String str1,String str2,JavaScriptFunction function){
        Object result = function.callback(str1,str2);
        System.out.println(result);
    }

}
