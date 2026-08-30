package com.example.rapiffy.sftp;

import com.example.rapiffy.exceptions.ApiException;
import com.example.rapiffy.model.*;
import com.example.rapiffy.repos.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
public class ImageUploadService {

    private final SftpService sftpService;
    private final CategoryRepository categoryRepository;
    private final CategoryImageRepository categoryImageRepository;
    private final MasterProductRepository masterProductRepository;
    private final MasterProductImageRepository masterProductImageRepository;
    private final MasterProductVariantRepository masterProductVariantRepository;
    private final MasterProductVariantImageRepository masterProductVariantImageRepository;
    private final ShopProductRepository shopProductRepository;
    private final ShopProductImageRepository shopProductImageRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductVariantImageRepository productVariantImageRepository;

    public ImageUploadService(SftpService sftpService,
                              CategoryRepository categoryRepository,
                              CategoryImageRepository categoryImageRepository,
                              MasterProductRepository masterProductRepository,
                              MasterProductImageRepository masterProductImageRepository,
                              MasterProductVariantRepository masterProductVariantRepository,
                              MasterProductVariantImageRepository masterProductVariantImageRepository,
                              ShopProductRepository shopProductRepository,
                              ShopProductImageRepository shopProductImageRepository,
                              ProductVariantRepository productVariantRepository,
                              ProductVariantImageRepository productVariantImageRepository) {
        this.sftpService = sftpService;
        this.categoryRepository = categoryRepository;
        this.categoryImageRepository = categoryImageRepository;
        this.masterProductRepository = masterProductRepository;
        this.masterProductImageRepository = masterProductImageRepository;
        this.masterProductVariantRepository = masterProductVariantRepository;
        this.masterProductVariantImageRepository = masterProductVariantImageRepository;
        this.shopProductRepository = shopProductRepository;
        this.shopProductImageRepository = shopProductImageRepository;
        this.productVariantRepository = productVariantRepository;
        this.productVariantImageRepository = productVariantImageRepository;
    }

    /**
     * Upload image for a Category.
     * Only 1 image allowed — replaces existing if already present.
     */
    public String uploadCategoryImage(Long categoryId, MultipartFile file) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ApiException("Category not found", HttpStatus.NOT_FOUND));

        // Delete existing image if present (only 1 allowed)
        List<CategoryImage> existing = categoryImageRepository.findByCategoryId(categoryId);
        if (!existing.isEmpty()) {
            categoryImageRepository.deleteAll(existing);
        }

        String remotePath = "categories/" + categoryId;
        String imageUrl = sftpService.uploadImage(file, remotePath);

        CategoryImage image = new CategoryImage();
        image.setCategory(category);
        image.setImageUrl(imageUrl);
        categoryImageRepository.save(image);

        // Also set on category itself for direct access
        category.setImageUrl(imageUrl);
        categoryRepository.save(category);

        return imageUrl;
    }

    /**
     * Upload one or more images for a MasterProduct.
     * Appends to existing images — display order continues from last.
     */
    public List<String> uploadProductImages(Long productId, List<MultipartFile> files) {
        MasterProduct product = masterProductRepository.findById(productId)
                .orElseThrow(() -> new ApiException("Master product not found", HttpStatus.NOT_FOUND));

        Long categoryId = product.getCategory().getId();
        String remotePath = "products/" + categoryId + "/" + productId;

        // Get current max display order
        List<MasterProductImage> existing = masterProductImageRepository
                .findByMasterProductIdOrderByDisplayOrderAsc(productId);
        int nextOrder = existing.size();

        for (MultipartFile file : files) {
            String imageUrl = sftpService.uploadImage(file, remotePath);

            MasterProductImage image = new MasterProductImage();
            image.setMasterProduct(product);
            image.setImageUrl(imageUrl);
            image.setDisplayOrder(nextOrder++);
            masterProductImageRepository.save(image);

            // First image becomes the thumbnail
            if (nextOrder == 1) {
                product.setImageUrl(imageUrl);
                masterProductRepository.save(product);
            }
        }

        return masterProductImageRepository
                .findByMasterProductIdOrderByDisplayOrderAsc(productId)
                .stream().map(MasterProductImage::getImageUrl).toList();
    }

    /**
     * Upload one or more images for a MasterProductVariant.
     * Appends to existing images — display order continues from last.
     */
    public List<String> uploadVariantImages(Long variantId, List<MultipartFile> files) {
        MasterProductVariant variant = masterProductVariantRepository.findById(variantId)
                .orElseThrow(() -> new ApiException("Master variant not found", HttpStatus.NOT_FOUND));

        Long productId = variant.getParentMasterProduct().getId();
        Long categoryId = variant.getParentMasterProduct().getCategory().getId();
        String remotePath = "variants/" + categoryId + "/" + productId + "/" + variantId;

        // Get current max display order
        List<MasterProductVariantImage> existing = masterProductVariantImageRepository
                .findByVariantIdOrderByDisplayOrderAsc(variantId);
        int nextOrder = existing.size();

        for (MultipartFile file : files) {
            String imageUrl = sftpService.uploadImage(file, remotePath);

            MasterProductVariantImage image = new MasterProductVariantImage();
            image.setVariant(variant);
            image.setImageUrl(imageUrl);
            image.setDisplayOrder(nextOrder++);
            masterProductVariantImageRepository.save(image);

            // First image becomes the thumbnail
            if (nextOrder == 1) {
                variant.setImageUrl(imageUrl);
                masterProductVariantRepository.save(variant);
            }
        }

        return masterProductVariantImageRepository
                .findByVariantIdOrderByDisplayOrderAsc(variantId)
                .stream().map(MasterProductVariantImage::getImageUrl).toList();
    }

    /**
     * Upload one or more images for a ShopProduct (Admin's product).
     * Appends to existing images — display order continues from last.
     * First image becomes the thumbnail (imageUrl on ShopProduct).
     */
    public List<String> uploadShopProductImages(Long shopProductId, Long shopId, List<MultipartFile> files) {
        ShopProduct product = shopProductRepository.findById(shopProductId)
                .orElseThrow(() -> new ApiException("Shop product not found", HttpStatus.NOT_FOUND));

        if (!product.getShop().getId().equals(shopId))
            throw new ApiException("Access denied", HttpStatus.FORBIDDEN);

        String remotePath = "shop-products/" + shopId + "/" + shopProductId;

        List<ShopProductImage> existing = shopProductImageRepository.findByShopProductIdOrderByDisplayOrderAsc(shopProductId);
        int nextOrder = existing.size();

        for (MultipartFile file : files) {
            String imageUrl = sftpService.uploadImage(file, remotePath);

            ShopProductImage image = new ShopProductImage();
            image.setShopProduct(product);
            image.setImageUrl(imageUrl);
            image.setDisplayOrder(nextOrder);
            shopProductImageRepository.save(image);

            // First image becomes the thumbnail
            if (nextOrder == 0) {
                product.setImageUrl(imageUrl);
                shopProductRepository.save(product);
            }
            nextOrder++;
        }

        return shopProductImageRepository
                .findByShopProductIdOrderByDisplayOrderAsc(shopProductId)
                .stream().map(ShopProductImage::getImageUrl).toList();
    }

    /**
     * Upload one or more images for a ProductVariant (Admin's variant).
     * Appends to existing images — display order continues from last.
     * First image becomes the thumbnail (imageUrl on ProductVariant).
     */
    public List<String> uploadShopVariantImages(Long variantId, Long shopId, List<MultipartFile> files) {
        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new ApiException("Variant not found", HttpStatus.NOT_FOUND));

        if (!variant.getParentShopProduct().getShop().getId().equals(shopId))
            throw new ApiException("Access denied", HttpStatus.FORBIDDEN);

        Long shopProductId = variant.getParentShopProduct().getId();
        String remotePath = "shop-variants/" + shopId + "/" + shopProductId + "/" + variantId;

        List<ProductVariantImage> existing = productVariantImageRepository.findByVariantIdOrderByDisplayOrderAsc(variantId);
        int nextOrder = existing.size();

        for (MultipartFile file : files) {
            String imageUrl = sftpService.uploadImage(file, remotePath);

            ProductVariantImage image = new ProductVariantImage();
            image.setVariant(variant);
            image.setImageUrl(imageUrl);
            image.setDisplayOrder(nextOrder);
            productVariantImageRepository.save(image);

            // First image becomes the thumbnail
            if (nextOrder == 0) {
                variant.setImageUrl(imageUrl);
                productVariantRepository.save(variant);
            }
            nextOrder++;
        }

        return productVariantImageRepository
                .findByVariantIdOrderByDisplayOrderAsc(variantId)
                .stream().map(ProductVariantImage::getImageUrl).toList();
    }
}
