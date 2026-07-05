package com.shoplens.ai.utils;

import com.shoplens.ai.model.AssistantRecommendation;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser utility to extract structured product recommendation blocks from text.
 */
public class AssistantRecommendationParser {

    public static List<AssistantRecommendation> parseRecommendations(String response) {
        List<AssistantRecommendation> list = new ArrayList<>();
        if (response == null || response.trim().isEmpty()) {
            return list;
        }

        Pattern productPattern = Pattern.compile("\\[PRODUCT\\](.*?)\\[/PRODUCT\\]", Pattern.DOTALL);
        Matcher matcher = productPattern.matcher(response);

        while (matcher.find()) {
            String blockContent = matcher.group(1);
            String id = getValueByField(blockContent, "id");
            String name = getValueByField(blockContent, "name");
            String price = getValueByField(blockContent, "price");
            String stock = getValueByField(blockContent, "stock");
            String reason = getValueByField(blockContent, "reason");

            if (id != null && !id.trim().isEmpty()) {
                list.add(new AssistantRecommendation(id.trim(), name, price, stock, reason));
            }
        }

        return list;
    }

    public static String removeProductBlocks(String response) {
        if (response == null) {
            return "";
        }
        String cleaned = response.replaceAll("(?s)\\[PRODUCT\\].*?\\[/PRODUCT\\]", "");
        return cleaned.trim();
    }

    private static String getValueByField(String block, String fieldName) {
        Pattern fieldPattern = Pattern.compile("^\\s*" + fieldName + "\\s*:\\s*(.*?)\\s*$", Pattern.MULTILINE);
        Matcher matcher = fieldPattern.matcher(block);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "";
    }
}
