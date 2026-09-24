package com.kiotviet.fashion.repository;

import com.kiotviet.fashion.entity.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Long> {
    // Thêm query khi cần trong Supplier module
}
