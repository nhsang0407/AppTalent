package com.shoplens.ai.utils;

import android.util.Log;
import com.google.firebase.firestore.FirebaseFirestore;
import com.shoplens.ai.model.Product;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DatabaseSeeder {
    private static final String TAG = "DatabaseSeeder";

    public static void seedProductsIfNeeded() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection(Constants.COLLECTION_PRODUCTS).limit(1).get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) {
                        Log.d(TAG, "Products collection is empty, starting seed...");
                        performSeeding(db);
                    } else {
                        Log.d(TAG, "Products collection already has data. Skipping seed.");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking products collection: ", e);
                });
    }

    private static void performSeeding(FirebaseFirestore db) {
        List<Product> products = new ArrayList<>();

        // 1. Fashion (5 items)
        products.add(createProduct("Classic Denim Jacket", "A timeless indigo denim jacket with a relaxed fit, metal buttons, and dual chest pockets.", "Fashion", 49.99, 25, "123456789012", "https://images.unsplash.com/photo-1576995853123-5a10305d93c0?w=500&auto=format&fit=crop", Arrays.asList("jacket", "denim", "blue", "outerwear")));
        products.add(createProduct("Leather Chelsea Boots", "Premium leather boots with elastic side panels, pull tabs, and a durable rubber outsole.", "Fashion", 89.99, 15, "234567890123", "https://images.unsplash.com/photo-1608256246200-53e635b5b65f?w=500&auto=format&fit=crop", Arrays.asList("boots", "leather", "shoes", "chelsea")));
        products.add(createProduct("Cotton Crewneck T-Shirt", "Soft, breathable 100% organic cotton basic t-shirt. Perfect for layering or casual wear.", "Fashion", 19.99, 50, "345678901234", "https://images.unsplash.com/photo-1521572267360-ee0c2909d518?w=500&auto=format&fit=crop", Arrays.asList("shirt", "cotton", "tshirt", "casual")));
        products.add(createProduct("Pleated Midi Skirt", "Elegant pleated midi skirt featuring a high elastic waist and fluid, flowing fabric.", "Fashion", 34.99, 20, "120000000004", "https://images.unsplash.com/photo-1583496661160-fb5886a0aaaa?w=500&auto=format&fit=crop", Arrays.asList("skirt", "midi", "pleated", "women")));
        products.add(createProduct("Wool Blend Coat", "Sophisticated long wool blend coat with double-breasted button closure and side pockets.", "Fashion", 129.99, 10, "120000000005", "https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?w=500&auto=format&fit=crop", Arrays.asList("coat", "wool", "winter", "warm")));

        // 2. Electronics (6 items)
        products.add(createProduct("Wireless Noise-Canceling Headphones", "Over-ear headphones with active noise cancellation, 40-hour battery life, and crystal-clear sound.", "Electronics", 199.99, 30, "456789012345", "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=500&auto=format&fit=crop", Arrays.asList("headphones", "wireless", "audio", "anc")));
        products.add(createProduct("Smart Sports Watch", "GPS fitness tracker with heart rate monitor, sleep tracking, and customizable watch faces.", "Electronics", 149.99, 40, "567890123456", "https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=500&auto=format&fit=crop", Arrays.asList("watch", "smartwatch", "fitness", "tracker")));
        products.add(createProduct("Portable Bluetooth Speaker", "Waterproof wireless speaker with deep bass, 360-degree sound, and up to 12 hours of playtime.", "Electronics", 59.99, 45, "130000000003", "https://images.unsplash.com/photo-1608043152269-423dbba4e7e1?w=500&auto=format&fit=crop", Arrays.asList("speaker", "bluetooth", "wireless", "audio")));
        products.add(createProduct("Ergonomic Wireless Mouse", "Precision wireless mouse with adjustable DPI, thumb scroll wheel, and ergonomic hand support.", "Electronics", 45.99, 60, "130000000004", "https://images.unsplash.com/photo-1615663245857-ac93bb7c39e7?w=500&auto=format&fit=crop", Arrays.asList("mouse", "wireless", "ergonomic", "pc")));
        products.add(createProduct("Mechanical Gaming Keyboard", "RGB backlit mechanical keyboard with tactile blue switches and custom macro profiles.", "Electronics", 89.99, 25, "130000000005", "https://images.unsplash.com/photo-1587829741301-dc798b83add3?w=500&auto=format&fit=crop", Arrays.asList("keyboard", "gaming", "mechanical", "rgb")));
        products.add(createProduct("4K Ultra HD Action Camera", "Compact action camera with wide-angle lens, image stabilization, and waterproof housing up to 30m.", "Electronics", 119.99, 18, "130000000006", "https://images.unsplash.com/photo-1526170375885-4d8ecf77b99f?w=500&auto=format&fit=crop", Arrays.asList("camera", "action", "4k", "video")));

        // 3. Food (7 items)
        products.add(createProduct("Organic Espresso Beans", "Rich and bold dark roast espresso beans, 100% Arabica, sourced from sustainable farms.", "Food", 14.99, 100, "678901234567", "https://images.unsplash.com/photo-1447933601403-0c6688de566e?w=500&auto=format&fit=crop", Arrays.asList("coffee", "beans", "espresso", "organic")));
        products.add(createProduct("Artisanal Honey", "Pure, raw, unfiltered wildflower honey harvested locally from natural beehives.", "Food", 9.99, 80, "789012345678", "https://images.unsplash.com/photo-1587049352846-4a222e784d38?w=500&auto=format&fit=crop", Arrays.asList("honey", "sweet", "natural", "raw")));
        products.add(createProduct("Dark Chocolate Bar (70%)", "Premium single-origin dark chocolate bar with delicate notes of sea salt and caramel.", "Food", 4.99, 150, "140000000003", "https://images.unsplash.com/photo-1548907040-4d42b5212db3?w=500&auto=format&fit=crop", Arrays.asList("chocolate", "dark", "sweet", "snack")));
        products.add(createProduct("Assorted Herbal Tea Box", "A selection of 24 herbal tea bags including Chamomile, Peppermint, Green Tea, and Earl Grey.", "Food", 7.99, 90, "140000000004", "https://images.unsplash.com/photo-1597481499750-3e6b22637e12?w=500&auto=format&fit=crop", Arrays.asList("tea", "herbal", "chamomile", "beverage")));
        products.add(createProduct("Roasted Almonds", "Salted and dry-roasted premium almonds, the perfect healthy and energetic snack.", "Food", 8.99, 120, "140000000005", "https://images.unsplash.com/photo-1508061253366-f7da158b6d46?w=500&auto=format&fit=crop", Arrays.asList("almonds", "nuts", "snack", "healthy")));
        products.add(createProduct("Extra Virgin Olive Oil", "Cold-pressed extra virgin olive oil from Mediterranean olives, rich in antioxidants.", "Food", 16.99, 65, "140000000006", "https://images.unsplash.com/photo-1474979266404-7eaacbcd87c5?w=500&auto=format&fit=crop", Arrays.asList("oil", "olive", "cooking", "mediterranean")));
        products.add(createProduct("Gluten-Free Granola", "Crunchy baked granola clusters made with rolled oats, honey, pecans, and dried cranberries.", "Food", 6.49, 70, "140000000007", "https://images.unsplash.com/photo-1517881917430-e70dfb3610aa?w=500&auto=format&fit=crop", Arrays.asList("granola", "breakfast", "cereal", "snack")));

        // 4. Home (8 items)
        products.add(createProduct("Minimalist Ceramic Vase", "Handcrafted white matte ceramic vase, perfect for dried flowers or standalone decor.", "Home", 24.99, 30, "890123456789", "https://images.unsplash.com/photo-1612196808214-b8e1d6145a8c?w=500&auto=format&fit=crop", Arrays.asList("vase", "ceramic", "decor", "interior")));
        products.add(createProduct("Scented Soy Candle", "Natural soy wax candle with a relaxing lavender and eucalyptus scent, 40-hour burn time.", "Home", 12.99, 55, "901234567890", "https://images.unsplash.com/photo-1603006905003-be475563bc59?w=500&auto=format&fit=crop", Arrays.asList("candle", "soy", "lavender", "relax")));
        products.add(createProduct("Velvet Throw Pillow", "Super soft square velvet cushion cover with invisible zipper, adding a touch of luxury.", "Home", 15.99, 40, "150000000003", "https://images.unsplash.com/photo-1584100936595-c0654b55a2e2?w=500&auto=format&fit=crop", Arrays.asList("pillow", "cushion", "velvet", "sofa")));
        products.add(createProduct("LED Desk Lamp", "Dimmable LED table lamp with eye-caring light, USB charging port, and adjustable neck.", "Home", 29.99, 25, "150000000004", "https://images.unsplash.com/photo-1507473885765-e6ed057f782c?w=500&auto=format&fit=crop", Arrays.asList("lamp", "led", "desk", "lighting")));
        products.add(createProduct("Modern Wall Clock", "Silent non-ticking wall clock with minimalist metal frame and easy-to-read wooden face.", "Home", 34.99, 20, "150000000005", "https://images.unsplash.com/photo-1563861826100-9cb868fdcd1d?w=500&auto=format&fit=crop", Arrays.asList("clock", "wall", "time", "modern")));
        products.add(createProduct("Woven Cotton Storage Basket", "Large decorative rope basket for laundry, blankets, toys, with sturdy carrying handles.", "Home", 19.99, 35, "150000000006", "https://images.unsplash.com/photo-1531835551805-16d864c8d311?w=500&auto=format&fit=crop", Arrays.asList("basket", "storage", "cotton", "woven")));
        products.add(createProduct("Self-Watering Planter Pot", "Double-layer flower pot with water reservoir indicator, ideal for small houseplants and herbs.", "Home", 9.99, 50, "150000000007", "https://images.unsplash.com/photo-1485955900006-10f4d324d411?w=500&auto=format&fit=crop", Arrays.asList("planter", "pot", "plant", "garden")));
        products.add(createProduct("Glass Water Carafe Set", "Elegant 1-liter glass carafe with a spherical wooden oak cork stopper, includes 2 cups.", "Home", 22.99, 15, "150000000008", "https://images.unsplash.com/photo-1615485290382-441e4d049cb5?w=500&auto=format&fit=crop", Arrays.asList("carafe", "glass", "water", "cups")));

        // 5. Beauty (9 items)
        products.add(createProduct("Hydrating Face Cream", "Deeply moisturizing facial cream formulated with hyaluronic acid and green tea extract.", "Beauty", 28.00, 60, "012345678901", "https://images.unsplash.com/photo-1601049541289-9b1b7bbbfe19?w=500&auto=format&fit=crop", Arrays.asList("cream", "face", "moisturizer", "skincare")));
        products.add(createProduct("Matte Red Lipstick", "Long-lasting velvet matte lipstick with rich pigment and comfortable non-drying wear.", "Beauty", 18.00, 75, "098765432109", "https://images.unsplash.com/photo-1586495777744-4413f21062fa?w=500&auto=format&fit=crop", Arrays.asList("lipstick", "makeup", "red", "lips")));
        products.add(createProduct("Vitamin C Glow Serum", "Brightening skin booster serum containing 10% pure Vitamin C and ferulic acid.", "Beauty", 32.00, 45, "160000000003", "https://images.unsplash.com/photo-1620916566398-39f1143ab7be?w=500&auto=format&fit=crop", Arrays.asList("serum", "vitaminc", "skincare", "glow")));
        products.add(createProduct("Organic Body Lotion", "Nourishing daily body lotion enriched with organic shea butter and coconut oil.", "Beauty", 14.50, 80, "160000000004", "https://images.unsplash.com/photo-1556228720-195a672e8a03?w=500&auto=format&fit=crop", Arrays.asList("lotion", "body", "skincare", "organic")));
        products.add(createProduct("Rose Water Facial Toner", "100% pure organic rose water mist that tones, refreshes, and hydrates all skin types.", "Beauty", 12.00, 90, "160000000005", "https://images.unsplash.com/photo-1617897903246-719242758050?w=500&auto=format&fit=crop", Arrays.asList("toner", "rosewater", "mist", "skincare")));
        products.add(createProduct("Mineral Sunscreen SPF 50", "Broad-spectrum mineral sunscreen, lightweight, non-greasy, and reef-safe.", "Beauty", 24.00, 55, "160000000006", "https://images.unsplash.com/photo-1598440947619-2c35fc9aa908?w=500&auto=format&fit=crop", Arrays.asList("sunscreen", "spf50", "skincare", "sunblock")));
        products.add(createProduct("Exfoliating Coffee Scrub", "Invigorating body scrub made with organic coffee grounds, brown sugar, and sweet almond oil.", "Beauty", 15.00, 70, "160000000007", "https://images.unsplash.com/photo-1608248597279-f99d160bfcbc?w=500&auto=format&fit=crop", Arrays.asList("scrub", "body", "coffee", "exfoliate")));
        products.add(createProduct("Clarifying Clay Mask", "Deep pore cleansing bentonite clay mask infused with charcoal and tea tree oil.", "Beauty", 19.99, 50, "160000000008", "https://images.unsplash.com/photo-1567894340315-735d7c361db0?w=500&auto=format&fit=crop", Arrays.asList("mask", "clay", "charcoal", "skincare")));
        products.add(createProduct("Nourishing Hair Oil", "Lightweight argan hair oil treatment that eliminates frizz, adds shine, and strengthens ends.", "Beauty", 26.00, 40, "160000000009", "https://images.unsplash.com/photo-1608571423902-eed4a5ad8108?w=500&auto=format&fit=crop", Arrays.asList("hair", "oil", "argan", "haircare")));

        for (Product product : products) {
            db.collection(Constants.COLLECTION_PRODUCTS).document(product.getProductId())
                    .set(product)
                    .addOnSuccessListener(unused -> Log.d(TAG, "Successfully seeded product: " + product.getName()))
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to seed product: " + product.getName(), e));
        }
    }

    private static Product createProduct(String name, String description, String category, double price, int stock, String barcode, String imageUrl, List<String> tags) {
        Product p = new Product(name, description, category, price, stock);
        String productId = FirebaseFirestore.getInstance().collection(Constants.COLLECTION_PRODUCTS).document().getId();
        p.setProductId(productId);
        p.setBarcode(barcode);
        p.setImageUrl(imageUrl);
        p.setTags(tags);
        return p;
    }
}
