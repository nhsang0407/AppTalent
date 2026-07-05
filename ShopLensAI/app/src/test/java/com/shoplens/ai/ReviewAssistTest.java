package com.shoplens.ai;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.shoplens.ai.model.Product;
import com.shoplens.ai.model.ReviewAssistAction;
import com.shoplens.ai.utils.ReviewAssistFallback;

import org.junit.Test;

/**
 * Unit tests for ReviewAssistFallback and basic action controls.
 */
public class ReviewAssistTest {

    @Test
    public void testPoliteRewriteFallback() {
        String text = "  hàng cũng ok giao hơi lâu nhưng dùng ổn  ";
        String result = ReviewAssistFallback.politeRewrite(text);
        assertNotNull(result);
        assertTrue(result.contains("Sản phẩm dùng khá ổn"));
        assertTrue(result.contains("Hàng cũng ok giao hơi lâu nhưng dùng ổn."));
    }

    @Test
    public void testPoliteRewriteEmpty() {
        assertEquals("", ReviewAssistFallback.politeRewrite(null));
        assertEquals("", ReviewAssistFallback.politeRewrite("   "));
    }

    @Test
    public void testShortenFallback() {
        String shortText = "Sản phẩm tốt";
        assertEquals("Sản phẩm tốt", ReviewAssistFallback.shorten(shortText));

        String longText = "Sản phẩm này rất tốt, giao hàng cực nhanh, gói hàng cẩn thận, dùng rất thích và đáng tiền.";
        String result = ReviewAssistFallback.shorten(longText);
        assertEquals(50, result.length());
        assertTrue(result.endsWith("..."));
    }

    @Test
    public void testShortenEmpty() {
        assertEquals("", ReviewAssistFallback.shorten(null));
        assertEquals("", ReviewAssistFallback.shorten(""));
    }

    @Test
    public void testProofreadFallback() {
        String raw = "  sản phẩm  rất   tốt ";
        String result = ReviewAssistFallback.proofreadBasic(raw);
        assertEquals("Sản phẩm rất tốt.", result);
    }

    @Test
    public void testProofreadEmpty() {
        assertEquals("", ReviewAssistFallback.proofreadBasic(null));
        assertEquals("", ReviewAssistFallback.proofreadBasic(""));
    }

    @Test
    public void testSuggestFromRatingPositive() {
        Product product = new Product("Gối tựa đầu", "Bedroom description", "Bedroom", 12.5, 10);
        String result = ReviewAssistFallback.suggestFromRating(5.0f, product);
        assertNotNull(result);
        assertTrue(result.contains("khá hài lòng"));
        assertTrue(result.contains("Gối tựa đầu"));
    }

    @Test
    public void testSuggestFromRatingNeutral() {
        Product product = new Product("Gối tựa đầu", "Bedroom description", "Bedroom", 12.5, 10);
        String result = ReviewAssistFallback.suggestFromRating(3.0f, product);
        assertNotNull(result);
        assertTrue(result.contains("ở mức ổn"));
        assertTrue(result.contains("đáp ứng được nhu cầu cơ bản"));
    }

    @Test
    public void testSuggestFromRatingNegative() {
        Product product = new Product("Gối tựa đầu", "Bedroom description", "Bedroom", 12.5, 10);
        String result = ReviewAssistFallback.suggestFromRating(1.0f, product);
        assertNotNull(result);
        assertTrue(result.contains("chưa thật sự hài lòng"));
    }
}
