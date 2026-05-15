package com.ecards.member_management.repository;

import com.ecards.member_management.entity.TransactionRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * 交易记录Repository
 */
@Repository
public interface TransactionRecordRepository extends JpaRepository<TransactionRecord, Long>, JpaSpecificationExecutor<TransactionRecord> {
    
    // 基础的CRUD操作由JpaRepository提供
    // 动态查询由JpaSpecificationExecutor提供
    // 具体查询方法在Service层使用Specification实现
}

