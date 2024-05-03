package com.canopus.frindl;

import android.text.TextUtils;
import android.util.Patterns;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class FirebaseUtil {
    public static boolean isValidName(String name){
        String [] keywords = {"!", "@", "#", "$", "%", "^", "&", "*", "(", ")", "{", "}",
                "[","]", ":", ";", "'", "\"", ",", "/", "|", "\\", "<", ">", "ERROR"};
        if(name.isEmpty()){return false;}
        for(String e:keywords){
            if(name.contains(e)){
                return false;
            }
        }
        return true;
    }

    public static boolean isValidEmail(CharSequence target) {
        return (!TextUtils.isEmpty(target) && Patterns.EMAIL_ADDRESS.matcher(target).matches());
    }

    public static String decode(String encodeStr){
        encodeStr = encodeStr.replace("<dot>", ".");
        encodeStr = encodeStr.replace("<hash>", "#");
        encodeStr = encodeStr.replace("<dol>", "$");
        encodeStr = encodeStr.replace("<sqb1>", "[");
        encodeStr = encodeStr.replace("<sqb2>", "]");

        return encodeStr;
    }

    public static String encode(String decodeStr){
        decodeStr = decodeStr.replace(".", "<dot>");
        decodeStr = decodeStr.replace("#", "<hash>");
        decodeStr = decodeStr.replace("$", "<dol>");
        decodeStr = decodeStr.replace("[", "<sqb1>");
        decodeStr = decodeStr.replace("]", "<sqb2>");

        return decodeStr;
    }

    public static String returnRandKey(String s){
        Calendar calendar = Calendar.getInstance();
//        Calendar.getInstance().get(Calendar.MILLISECOND);
        DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
//        System.out.println(dateFormat.format(cal.getTime()));
        return s+dateFormat.format(calendar.getTime()) + String.valueOf(System.nanoTime());
    }

}
