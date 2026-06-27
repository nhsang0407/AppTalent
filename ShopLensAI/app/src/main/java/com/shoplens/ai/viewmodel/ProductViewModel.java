package com.shoplens.ai.viewmodel;

import android.app.Application;
import android.graphics.Bitmap;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.shoplens.ai.ai.GeminiService;
import com.shoplens.ai.model.Product;
import com.shoplens.ai.repository.ProductRepository;

import java.util.List;

public class ProductViewModel extends AndroidViewModel {

    private final ProductRepository repository = new ProductRepository();
    private final GeminiService geminiService;

    private final MutableLiveData<List<Product>> products = new MutableLiveData<>();
    private final MutableLiveData<Product> selectedProduct = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<String> aiAnalysisResult = new MutableLiveData<>();
    private final MutableLiveData<Bitmap> aiGeneratedImage = new MutableLiveData<>();
    private final MutableLiveData<String> aiGeneratedDescription = new MutableLiveData<>();
    private final MutableLiveData<Boolean> productSaved = new MutableLiveData<>();

    public ProductViewModel(@NonNull Application application) {
        super(application);
        this.geminiService = new GeminiService(application);
    }

    public MutableLiveData<List<Product>> getProducts() {
        return products;
    }

    public MutableLiveData<Product> getSelectedProduct() {
        return selectedProduct;
    }

    public MutableLiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public MutableLiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public MutableLiveData<String> getAiAnalysisResult() {
        return aiAnalysisResult;
    }

    public MutableLiveData<Bitmap> getAiGeneratedImage() {
        return aiGeneratedImage;
    }

    public MutableLiveData<String> getAiGeneratedDescription() {
        return aiGeneratedDescription;
    }

    public MutableLiveData<Boolean> getProductSaved() {
        return productSaved;
    }

    public void loadProducts(@Nullable String categoryFilter) {
        isLoading.setValue(true);
        repository.getAllProducts(categoryFilter, new ProductRepository.ProductListCallback() {
            @Override
            public void onSuccess(List<Product> list) {
                isLoading.setValue(false);
                products.setValue(list);
            }

            @Override
            public void onError(Exception e) {
                isLoading.setValue(false);
                errorMessage.setValue(e.getMessage());
            }
        });
    }

    public void searchProducts(String keyword) {
        isLoading.setValue(true);
        repository.searchProducts(keyword, new ProductRepository.ProductListCallback() {
            @Override
            public void onSuccess(List<Product> list) {
                isLoading.setValue(false);
                products.setValue(list);
            }

            @Override
            public void onError(Exception e) {
                isLoading.setValue(false);
                errorMessage.setValue(e.getMessage());
            }
        });
    }

    /** Visual search: identify the product with Gemini, then run a keyword search. */
    public void searchByImage(Bitmap image) {
        isLoading.setValue(true);
        geminiService.analyzeProductImage(image, new GeminiService.GeminiCallback<String>() {
            @Override
            public void onSuccess(String result) {
                aiAnalysisResult.setValue(result);
                String keyword = extractKeyword(result);
                searchProducts(keyword);
            }

            @Override
            public void onError(Exception e) {
                isLoading.setValue(false);
                errorMessage.setValue(e.getMessage());
            }
        });
    }

    /** Pulls a usable search term out of the JSON-ish Gemini response. */
    private String extractKeyword(String json) {
        if (json == null) {
            return "";
        }
        String marker = "\"productName\"";
        int idx = json.indexOf(marker);
        if (idx >= 0) {
            int colon = json.indexOf(':', idx);
            int firstQuote = json.indexOf('"', colon + 1);
            int secondQuote = json.indexOf('"', firstQuote + 1);
            if (firstQuote >= 0 && secondQuote > firstQuote) {
                return json.substring(firstQuote + 1, secondQuote);
            }
        }
        // Fallback: first line of free text.
        return json.split("\\r?\\n")[0].replaceAll("[^a-zA-Z0-9 ]", "").trim();
    }

    public void getProductById(String productId) {
        isLoading.setValue(true);
        repository.getProductById(productId, new ProductRepository.ProductCallback() {
            @Override
            public void onSuccess(Product product) {
                isLoading.setValue(false);
                selectedProduct.setValue(product);
            }

            @Override
            public void onError(Exception e) {
                isLoading.setValue(false);
                errorMessage.setValue(e.getMessage());
            }
        });
    }

    public void getProductByBarcode(String barcode) {
        isLoading.setValue(true);
        repository.getProductByBarcode(barcode, new ProductRepository.ProductCallback() {
            @Override
            public void onSuccess(Product product) {
                isLoading.setValue(false);
                selectedProduct.setValue(product);
            }

            @Override
            public void onError(Exception e) {
                isLoading.setValue(false);
                errorMessage.setValue(e.getMessage());
            }
        });
    }

    public void addProduct(Product product, @Nullable Uri imageUri) {
        isLoading.setValue(true);
        repository.addProduct(product, imageUri, new ProductRepository.ProductCallback() {
            @Override
            public void onSuccess(Product saved) {
                isLoading.setValue(false);
                productSaved.setValue(true);
            }

            @Override
            public void onError(Exception e) {
                isLoading.setValue(false);
                errorMessage.setValue(e.getMessage());
            }
        });
    }

    public void updateProduct(Product product) {
        isLoading.setValue(true);
        repository.updateProduct(product, new ProductRepository.ProductCallback() {
            @Override
            public void onSuccess(Product saved) {
                isLoading.setValue(false);
                productSaved.setValue(true);
            }

            @Override
            public void onError(Exception e) {
                isLoading.setValue(false);
                errorMessage.setValue(e.getMessage());
            }
        });
    }

    public void addProductImageThenUpdate(Product product, Uri imageUri) {
        isLoading.setValue(true);
        repository.updateProductWithImage(product, imageUri, new ProductRepository.ProductCallback() {
            @Override
            public void onSuccess(Product saved) {
                isLoading.setValue(false);
                productSaved.setValue(true);
            }

            @Override
            public void onError(Exception e) {
                isLoading.setValue(false);
                errorMessage.setValue(e.getMessage());
            }
        });
    }

    public void updateStock(String productId, int newStock) {
        isLoading.setValue(true);
        repository.updateStock(productId, newStock, new ProductRepository.VoidCallback() {
            @Override
            public void onSuccess() {
                isLoading.setValue(false);
                productSaved.setValue(true);
            }

            @Override
            public void onError(Exception e) {
                isLoading.setValue(false);
                errorMessage.setValue(e.getMessage());
            }
        });
    }

    public void deleteProduct(String productId) {
        isLoading.setValue(true);
        repository.deleteProduct(productId, new ProductRepository.VoidCallback() {
            @Override
            public void onSuccess() {
                isLoading.setValue(false);
                loadProducts(null);
            }

            @Override
            public void onError(Exception e) {
                isLoading.setValue(false);
                errorMessage.setValue(e.getMessage());
            }
        });
    }

    public void generateProductImage(String name, String description) {
        isLoading.setValue(true);
        geminiService.generateProductImage(name, description,
                new GeminiService.GeminiCallback<Bitmap>() {
                    @Override
                    public void onSuccess(Bitmap result) {
                        isLoading.setValue(false);
                        aiGeneratedImage.setValue(result);
                    }

                    @Override
                    public void onError(Exception e) {
                        isLoading.setValue(false);
                        errorMessage.setValue(e.getMessage());
                    }
                });
    }

    public void generateProductDescription(String name, String category) {
        isLoading.setValue(true);
        geminiService.generateProductDescription(name, category,
                new GeminiService.GeminiCallback<String>() {
                    @Override
                    public void onSuccess(String result) {
                        isLoading.setValue(false);
                        aiGeneratedDescription.setValue(result);
                    }

                    @Override
                    public void onError(Exception e) {
                        isLoading.setValue(false);
                        errorMessage.setValue(e.getMessage());
                    }
                });
    }
}
