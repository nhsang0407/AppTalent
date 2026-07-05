package com.shoplens.ai;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.shoplens.ai.ai.GeminiService;
import com.shoplens.ai.model.Product;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Unit tests for AI Shopping Assistant product pre-filtering and candidate selection.
 */
public class ShoppingAssistantTest {

    private List<Product> sampleProducts;

    @Before
    public void setUp() {
        sampleProducts = new ArrayList<>();
        // Product 1: Over budget, in stock, good rating
        Product p1 = new Product("Denim Jacket", "Classic blue outerwear", "Fashion", 49.99, 10);
        p1.setProductId("p1");
        p1.setAverageRating(4.5);
        sampleProducts.add(p1);

        // Product 2: Exactly under budget, in stock, medium rating
        Product p2 = new Product("Cotton T-shirt", "Comfortable t-shirt", "Fashion", 19.99, 15);
        p2.setProductId("p2");
        p2.setAverageRating(4.0);
        sampleProducts.add(p2);

        // Product 3: Well under budget, in stock, high rating
        Product p3 = new Product("Scented Soy Candle", "Relaxing lavender scent", "Home", 12.99, 5);
        p3.setProductId("p3");
        p3.setAverageRating(4.8);
        sampleProducts.add(p3);

        // Product 4: Over budget, out of stock, high rating
        Product p4 = new Product("Hydrating Cream", "Face moisturizer skincare", "Beauty", 28.00, 0);
        p4.setProductId("p4");
        p4.setAverageRating(4.9);
        sampleProducts.add(p4);

        // Product 5: Under budget, in stock, lower rating
        Product p5 = new Product("Lipstick", "Matte red lipstick", "Beauty", 18.00, 3);
        p5.setProductId("p5");
        p5.setAverageRating(4.2);
        sampleProducts.add(p5);
    }

    @Test
    public void testBudgetFilteringVnd() {
        // "dưới 500k" -> equivalent to under 20 USD.
        // Products fitting: p2 ($19.99), p3 ($12.99), p5 ($18.00).
        List<Product> relevant = GeminiService.selectRelevantProductsForAssistant(
                "Tôi cần quà tặng dưới 500k", sampleProducts);

        assertNotNull(relevant);
        assertTrue(relevant.size() > 0);

        // Top elements must be under budget
        double firstPrice = relevant.get(0).getPrice();
        double secondPrice = relevant.get(1).getPrice();
        double thirdPrice = relevant.get(2).getPrice();

        assertTrue(firstPrice <= 20.0);
        assertTrue(secondPrice <= 20.0);
        assertTrue(thirdPrice <= 20.0);
    }

    @Test
    public void testOutOfStockPenalized() {
        // Lipstick (p5) and Hydrating Cream (p4) both relate to "skincare" or "Beauty".
        // p4 has rating 4.9 but is out of stock (stock=0).
        // p5 has rating 4.2 and is in stock.
        // With query "skincare", p5 (in stock) should be prioritized over p4 (out of stock).
        List<Product> relevant = GeminiService.selectRelevantProductsForAssistant(
                "tư vấn skincare", sampleProducts);

        assertNotNull(relevant);
        
        // Find positions of p4 and p5
        int p4Index = -1;
        int p5Index = -1;
        for (int i = 0; i < relevant.size(); i++) {
            if ("p4".equals(relevant.get(i).getProductId())) {
                p4Index = i;
            } else if ("p5".equals(relevant.get(i).getProductId())) {
                p5Index = i;
            }
        }

        // Both found
        assertTrue(p4Index != -1);
        assertTrue(p5Index != -1);

        // p5 (in stock) should be ranked higher than p4 (out of stock)
        assertTrue(p5Index < p4Index);
    }

    @Test
    public void testRatingPrioritization() {
        // Query: "Fashion" matches p1 and p2. Both are in stock.
        // p1 price: 49.99, rating 4.5
        // p2 price: 19.99, rating 4.0
        // Query has no budget, so they are matched purely on keywords and rating.
        // p1 should have higher rating score.
        List<Product> relevant = GeminiService.selectRelevantProductsForAssistant(
                "Tìm đồ Fashion", sampleProducts);

        assertNotNull(relevant);
        
        int p1Index = -1;
        int p2Index = -1;
        for (int i = 0; i < relevant.size(); i++) {
            if ("p1".equals(relevant.get(i).getProductId())) {
                p1Index = i;
            } else if ("p2".equals(relevant.get(i).getProductId())) {
                p2Index = i;
            }
        }

        assertTrue(p1Index != -1);
        assertTrue(p2Index != -1);
        // p1 (rating 4.5) should be ranked higher than p2 (rating 4.0)
        assertTrue(p1Index < p2Index);
    }

    @Test
    public void testEmptyProductListFallback() {
        List<Product> relevant = GeminiService.selectRelevantProductsForAssistant(
                "Tôi muốn mua đầm", new ArrayList<>());
        assertNotNull(relevant);
        assertTrue(relevant.isEmpty());
    }

    @Test
    public void testNullProductListFallback() {
        List<Product> relevant = GeminiService.selectRelevantProductsForAssistant(
                "Tôi muốn mua đầm", null);
        assertNotNull(relevant);
        assertTrue(relevant.isEmpty());
    }
}
