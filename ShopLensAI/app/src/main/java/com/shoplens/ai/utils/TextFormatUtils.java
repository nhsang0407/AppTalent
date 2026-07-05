package com.shoplens.ai.utils;

/**
 * Text formatter to clean up basic markdown notations.
 */
public class TextFormatUtils {

    public static String stripBasicMarkdown(String text) {
        if (text == null) {
            return "";
        }
        // Remove double asterisks
        String cleaned = text.replace("**", "");
        
        // Remove bullet markers at the start of a line
        cleaned = cleaned.replaceAll("(?m)^\\s*\\*\\s*", "");
        
        return cleaned.trim();
    }
}
