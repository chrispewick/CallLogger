package com.pewick.calllogger.views;

import android.content.Context;
import android.graphics.Typeface;

import java.util.Hashtable;

/**
 * Used to set the custom fonts of TextViewPlus and EditTextPlus.
 */
public class TypeFaceDictionary {
    private static final Hashtable<String, Typeface> cache = new Hashtable<>();
    public static Typeface get(Context context, String fontName) {
        synchronized (cache) {
            if (!cache.containsKey(fontName)) {
//                Typeface typeFace = Typeface.createFromAsset(context.getAssets(), String.format("fonts/%s.otf", fontName));
//                cache.put(fontName, typeFace);
            }
            return cache.get(fontName);
        }
    }
}
