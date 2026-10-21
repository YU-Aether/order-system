package com.team.orderserver.module.order.controller;

import com.team.orderserver.common.api.Result;
import com.team.orderserver.common.api.ResultCode;
import com.team.orderserver.common.exception.BusinessException;
import com.team.orderserver.module.order.dto.RevenueResponse;
import com.team.orderserver.module.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * 运营后台接口，仅管理员可访问。
 */
@Tag(name = "04-运营", description = "营收统计")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final OrderService orderService;

    @Operation(summary = "按日期统计营收", description = "date 格式 yyyy-MM-dd，缺省为当天")
    @GetMapping("/revenue")
    public Result<RevenueResponse> revenue(@RequestParam(required = false) String date) {
        LocalDate day = null;
        if (date != null && !date.isBlank()) {
            try {
                day = LocalDate.parse(date);
            } catch (Exception ex) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "日期格式应为 yyyy-MM-dd");
            }
        }
        return Result.success(orderService.revenue(day));
    }
}
