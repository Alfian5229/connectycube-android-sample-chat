package com.connectycube.sample.chat.utils;

import android.text.InputFilter;
import android.text.Spanned;

public class EditTextUtils {

    /**
     * This filter will restrict paste Images in Keyboard.
     */
    public static class ImageInputFilter implements InputFilter {
        private static final char OBJ = '\uFFFC';

        public CharSequence filter(CharSequence source, int start, int end,
                                   Spanned dest, int dstart, int dend) {
            for (int i = start; i < end; i++) {
                if (source.charAt(i) == OBJ) {
                    return "";
                }
            }
            return null;
        }
    }
}
