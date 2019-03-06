package com.alex.licode_android;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import cube.rtc.IStreamDescription;

import static org.junit.Assert.assertEquals;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() {
        assertEquals(4, 2 + 2);
    }

    public static IStreamDescription create(JSONObject streamObj, boolean isLocal) {
        try {
            Class<?> streamDesClass = Class.forName("cube.rtc.StreamDescription");
            Method createInstance = streamDesClass.getMethod("createInstance", JSONObject.class, boolean.class);
            IStreamDescription streamDescription = (IStreamDescription) createInstance.invoke(null, streamObj,isLocal);

            return streamDescription;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Test
    public void testCreate() {

        String str = "{\"id\":637598327536235000,\"audio\":true,\"video\":true,\"data\":true,\"label\":\"ARDAMS\"}";
        try {
            JSONObject jsonObject = new JSONObject(str);
            IStreamDescription streamDescription = create(jsonObject, true);
//            String hello = streamDescription.sayHello();
//            System.out.print(hello);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}