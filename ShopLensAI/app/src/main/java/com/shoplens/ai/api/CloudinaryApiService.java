package com.shoplens.ai.api;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;

public interface CloudinaryApiService {
    @Multipart
    @POST("v1_1/{cloudName}/image/upload")
    Call<ResponseBody> uploadImage(
        @Path("cloudName") String cloudName,
        @Part("upload_preset") RequestBody uploadPreset,
        @Part MultipartBody.Part file
    );
}
