package com.ecards.member_management.schedule;

import com.ecards.member_management.entity.ReservationResource;
import com.ecards.member_management.entity.ReservationTemplate;
import com.ecards.member_management.repository.ReservationResourceRepository;
import com.ecards.member_management.repository.ReservationTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 定时清理过期的forbidden_days日期
 * 每天凌晨2点执行
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ForbiddenDaysCleanupTask {

    private final ReservationTemplateRepository templateRepository;
    private final ReservationResourceRepository resourceRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 每天凌晨2点清理过期日期
     * cron表达式：秒 分 时 日 月 周
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void cleanupExpiredForbiddenDays() {
        log.info("开始执行forbidden_days过期日期清理任务");
        
        LocalDate today = LocalDate.now();
        int templateCount = 0;
        int resourceCount = 0;

        try {
            // 1. 清理所有模板的过期日期
            List<ReservationTemplate> templates = templateRepository.findAll();
            for (ReservationTemplate template : templates) {
                if (template.getForbiddenDays() != null && !template.getForbiddenDays().isEmpty()) {
                    List<String> cleaned = cleanupForbiddenDaysList(template.getForbiddenDays(), today);
                    if (!cleaned.equals(template.getForbiddenDays())) {
                        template.setForbiddenDays(cleaned);
                        templateRepository.save(template);
                        templateCount++;
                    }
                }
            }

            // 2. 清理所有资源的过期日期
            List<ReservationResource> resources = resourceRepository.findAll();
            for (ReservationResource resource : resources) {
                if (resource.getForbiddenDays() != null && !resource.getForbiddenDays().isEmpty()) {
                    List<String> cleaned = cleanupForbiddenDaysList(resource.getForbiddenDays(), today);
                    if (!cleaned.equals(resource.getForbiddenDays())) {
                        resource.setForbiddenDays(cleaned);
                        resourceRepository.save(resource);
                        resourceCount++;
                    }
                }
            }

            log.info("forbidden_days过期日期清理完成，模板更新数：{}，资源更新数：{}", templateCount, resourceCount);
            
        } catch (Exception e) {
            log.error("forbidden_days过期日期清理失败", e);
        }
    }

    /**
     * 清理单个forbidden_days列表中的过期日期
     * 
     * @param forbiddenDays 原始列表
     * @param today 今天的日期
     * @return 清理后的列表
     */
    private List<String> cleanupForbiddenDaysList(List<String> forbiddenDays, LocalDate today) {
        List<String> cleaned = new ArrayList<>();
        
        for (String day : forbiddenDays) {
            // 如果是周几格式（如"周六"），保留
            if (day.startsWith("周")) {
                cleaned.add(day);
                continue;
            }
            
            // 如果是具体日期格式（如"2025-11-16"），检查是否过期
            try {
                LocalDate date = LocalDate.parse(day, DATE_FORMATTER);
                // 只保留今天及以后的日期
                if (!date.isBefore(today)) {
                    cleaned.add(day);
                }
            } catch (Exception e) {
                // 日期格式错误，记录日志但不影响其他数据
                log.warn("无效的forbidden_days日期格式：{}", day);
            }
        }
        
        return cleaned;
    }
}
