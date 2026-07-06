package com.shoplens.ai.api;

import retrofit2.Retrofit;

public class CloudinaryClient {
    private static Retrofit retrofit = null;

    public static CloudinaryApiService getApiService() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl("https://api.cloudinary.com/")
                    .build();
        }
        return retrofit.create(CloudinaryApiService.class);
    }
}
