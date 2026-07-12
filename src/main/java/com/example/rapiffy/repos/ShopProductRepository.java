package com.example.rapiffy.repos;

import com.example.rapiffy.model.MasterProduct;
import com.example.rapiffy.model.Profile;
import com.example.rapiffy.model.ShopProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShopProductRepository extends JpaRepository<ShopProduct, Long> {

    // Get all products for a specific shop
    List<ShopProduct> findByShop(Profile shop);

    // Check if Admin already activated a specific MasterProduct
    Optional<ShopProduct> findByShopAndMasterProduct(Profile shop, MasterProduct masterProduct);

    // Find a shop product by id and shop (ensures Admin can only access their own products)
    Optional<ShopProduct> findByIdAndShop(Long id, Profile shop);
}
