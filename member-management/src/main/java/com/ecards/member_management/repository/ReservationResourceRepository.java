package com.ecards.member_management.repository;

import com.ecards.member_management.entity.ReservationResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

/**
 * 预约资源Repository
 */
@Repository
public interface ReservationResourceRepository extends JpaRepository<ReservationResource, Long> {

    /**
     * 分页查询资源列表（支持多条件筛选）
     */
    @Query("SELECT r FROM ReservationResource r WHERE r.storeId = :storeId " +
           "AND (:keyword IS NULL OR r.resourceName LIKE %:keyword%) " +
           "AND (:isReservable IS NULL OR r.isReservable = :isReservable) " +
           "AND (:supportCardTypes IS NULL OR r.supportCardTypes = :supportCardTypes)")
    Page<ReservationResource> findByConditions(
            @Param("storeId") byte[] storeId,
            @Param("keyword") String keyword,
            @Param("isReservable") Integer isReservable,
            @Param("supportCardTypes") Integer supportCardTypes,
            Pageable pageable
    );

    /**
     * 查询资源详情
     */
    Optional<ReservationResource> findByResourceId(Long resourceId);

    /**
     * 检查资源名称是否重复（同店铺内）
     */
    @Query("SELECT COUNT(r) > 0 FROM ReservationResource r WHERE r.storeId = :storeId " +
           "AND r.resourceName = :resourceName AND (:resourceId IS NULL OR r.resourceId != :resourceId)")
    boolean existsByStoreIdAndResourceName(
            @Param("storeId") byte[] storeId,
            @Param("resourceName") String resourceName,
            @Param("resourceId") Long resourceId
    );
}
