package com.shoplens.ai;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.shoplens.ai.model.AssistantRecommendation;
import com.shoplens.ai.utils.AssistantRecommendationParser;
import com.shoplens.ai.utils.TextFormatUtils;

import org.junit.Test;

import java.util.List;

/**
 * Unit tests for recommendation parser, markdown formatting, and fallback constraints.
 */
public class ShoppingAssistantClickableTest {

    @Test
    public void testParseRecommendations() {
        String response = "Chào bạn! Đây là sản phẩm phù hợp:\n\n" +
                "[PRODUCT]\n" +
                "id: prod_101\n" +
                "name: Kem Dưỡng Ẩm Cerave\n" +
                "price: $15.50 (khoảng 380.000đ)\n" +
                "stock: 45\n" +
                "reason: Cấp ẩm tốt cho mùa đông, phù hợp túi tiền.\n" +
                "[/PRODUCT]\n\n" +
                "Bạn có cần xem thêm sản phẩm dưỡng ẩm nào khác không?";

        List<AssistantRecommendation> list = AssistantRecommendationParser.parseRecommendations(response);

        assertNotNull(list);
        assertEquals(1, list.size());

        AssistantRecommendation rec = list.get(0);
        assertEquals("prod_101", rec.getProductId());
        assertEquals("Kem Dưỡng Ẩm Cerave", rec.getProductName());
        assertEquals("$15.50 (khoảng 380.000đ)", rec.getPriceText());
        assertEquals("45", rec.getStockText());
        assertEquals("Cấp ẩm tốt cho mùa đông, phù hợp túi tiền.", rec.getReason());
    }

    @Test
    public void testParseRecommendationsSkipMissingId() {
        String response = "[PRODUCT]\n" +
                "name: Vô danh\n" +
                "price: $10.00\n" +
                "stock: 5\n" +
                "reason: Thiếu ID nên block này sẽ bị bỏ qua.\n" +
                "[/PRODUCT]";

        List<AssistantRecommendation> list = AssistantRecommendationParser.parseRecommendations(response);
        assertNotNull(list);
        assertTrue(list.isEmpty());
    }

    @Test
    public void testRemoveProductBlocks() {
        String response = "Chào bạn! Gợi ý:\n\n" +
                "[PRODUCT]\n" +
                "id: p_1\n" +
                "name: A\n" +
                "price: $1\n" +
                "stock: 1\n" +
                "reason: R\n" +
                "[/PRODUCT]\n\n" +
                "Chúc bạn mua sắm vui vẻ!";

        String conversationalText = AssistantRecommendationParser.removeProductBlocks(response);
        assertEquals("Chào bạn! Gợi ý:\n\n\n\nChúc bạn mua sắm vui vẻ!", conversationalText);
    }

    @Test
    public void testStripBasicMarkdown() {
        String text = "**Giá:** $20.00\n" +
                "**Lý do:** Tốt\n" +
                "* Gợi ý 1\n" +
                "  * Gợi ý 2";

        String cleaned = TextFormatUtils.stripBasicMarkdown(text);
        
        // Assert that ** and bullet stars are cleaned
        assertTrue(!cleaned.contains("**"));
        assertTrue(cleaned.contains("Giá: $20.00"));
        assertTrue(cleaned.contains("Lý do: Tốt"));
        assertTrue(!cleaned.contains("* Gợi ý"));
    }

    @Test
    public void testParserNullAndEmptyHandling() {
        List<AssistantRecommendation> list1 = AssistantRecommendationParser.parseRecommendations(null);
        assertNotNull(list1);
        assertTrue(list1.isEmpty());

        List<AssistantRecommendation> list2 = AssistantRecommendationParser.parseRecommendations("  ");
        assertNotNull(list2);
        assertTrue(list2.isEmpty());

        String cleanedNull = AssistantRecommendationParser.removeProductBlocks(null);
        assertEquals("", cleanedNull);
    }
}
