package com.example.rapiffy.repos;

import com.example.rapiffy.model.SubCategory;
import com.example.rapiffy.model.MasterProduct;
import com.example.rapiffy.model.Profile;
import com.example.rapiffy.model.ShopProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShopProductRepository extends JpaRepository<ShopProduct, Long> {

    List<ShopProduct> findByShop(Profile shop);

    Optional<ShopProduct> findByShopAndMasterProduct(Profile shop, MasterProduct masterProduct);

    Optional<ShopProduct> findByIdAndShop(Long id, Profile shop);

    List<ShopProduct> findByShopAndSubCategory(Profile shop, SubCategory subCategory);

    List<ShopProduct> findByShopIdAndIsActive(Long shopId, boolean isActive);

    List<ShopProduct> findByShopIdAndSubCategoryIdAndIsActive(Long shopId, Long subCategoryId, boolean isActive);
}
