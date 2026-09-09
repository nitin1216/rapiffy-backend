package com.example.rapiffy.repos;

import com.example.rapiffy.model.ReturnImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReturnImageRepository extends JpaRepository<ReturnImage, Long> {

    List<ReturnImage> findByReturnRequestIdOrderByDisplayOrderAsc(Long returnRequestId);
}
