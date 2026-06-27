package com.shoplens.ai.utils;

/**
 * App-wide constants: Firestore collection names, storage paths, roles and statuses.
 */
public final class Constants {

    private Constants() {
    }

    // Firestore collections
    public static final String COLLECTION_USERS = "users";
    public static final String COLLECTION_PRODUCTS = "products";
    public static final String COLLECTION_ORDERS = "orders";
    public static final String COLLECTION_REVIEWS = "reviews";
    public static final String COLLECTION_SEARCH_LOGS = "searchLogs";

    // Storage
    public static final String STORAGE_PRODUCTS = "product_images/";

    // Roles
    public static final String ROLE_ADMIN = "admin";
    public static final String ROLE_USER = "user";

    // Order statuses
    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_CONFIRMED = "confirmed";
    public static final String STATUS_DONE = "done";
    public static final String STATUS_CANCELLED = "cancelled";

    // Search types
    public static final String SEARCH_TYPE_BARCODE = "barcode";
    public static final String SEARCH_TYPE_VISUAL = "visual";

    // Intent extras
    public static final String EXTRA_PRODUCT_ID = "extra_product_id";
    public static final String EXTRA_SEARCH_QUERY = "extra_search_query";
    public static final String EXTRA_BARCODE = "extra_barcode";

    // Visual search result codes
    public static final int RESULT_BARCODE = 1001;
    public static final int RESULT_VISUAL = 1002;

    // Misc
    public static final int LOW_STOCK_THRESHOLD = 5;
    public static final int RECENT_ORDERS_LIMIT = 10;
}
