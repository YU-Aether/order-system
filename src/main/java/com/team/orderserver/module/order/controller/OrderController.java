package com.team.orderserver.module.order.controller;

import com.team.orderserver.common.api.PageResult;
import com.team.orderserver.common.api.Result;
import com.team.orderserver.module.order.dto.CreateOrderRequest;
import com.team.orderserver.module.order.dto.OrderQuery;
import com.team.orderserver.module.order.dto.OrderResponse;
import com.team.orderserver.module.order.dto.PayOrderRequest;
import com.team.orderserver.module.order.service.OrderService;
import com.team.orderserver.security.CurrentUser;
import com.team.orderserver.security.LoginUser;
import com.team.orderserver.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "03-订单", description = "下单、支付、取消、发货、收货、退款")
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "创建订单",
            description = "请求头携带 Idempotency-Key 可防止重复下单；库存不足会返回 409")
    @PostMapping
    public Result<OrderResponse> create(@CurrentUser LoginUser loginUser,
                                        @Valid @RequestBody CreateOrderRequest request) {
        return Result.success(orderService.create(loginUser.id(), request));
    }

    @Operation(summary = "分页查询订单", description = "普通用户仅能查询自己的订单，管理员可查询全部")
    @GetMapping
    public Result<PageResult<OrderResponse>> page(
            @CurrentUser LoginUser loginUser,
            @Parameter(description = "订单状态：CREATED/PAID/SHIPPED/COMPLETED/CANCELLED/REFUNDED")
            @RequestParam(required = false) String status,
            @Parameter(description = "订单号模糊搜索") @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        boolean admin = SecurityUtils.isAdmin();
        return Result.success(orderService.page(loginUser.id(), admin,
                new OrderQuery(status, keyword, page, size)));
    }

    @Operation(summary = "订单详情")
    @GetMapping("/{orderNo}")
    public Result<OrderResponse> detail(@CurrentUser LoginUser loginUser, @PathVariable String orderNo) {
        return Result.success(orderService.detail(loginUser.id(), SecurityUtils.isAdmin(), orderNo));
    }

    @Operation(summary = "支付订单")
    @PostMapping("/{orderNo}/pay")
    public Result<OrderResponse> pay(@CurrentUser LoginUser loginUser,
                                     @PathVariable String orderNo,
                                     @RequestBody(required = false) PayOrderRequest request) {
        return Result.success(orderService.pay(loginUser.id(), orderNo,
                request == null ? null : request.channel()));
    }

    @Operation(summary = "取消订单", description = "仅待支付订单可取消，取消后自动归还库存")
    @PostMapping("/{orderNo}/cancel")
    public Result<OrderResponse> cancel(@CurrentUser LoginUser loginUser, @PathVariable String orderNo) {
        return Result.success(orderService.cancel(loginUser.id(), orderNo));
    }

    @Operation(summary = "确认收货")
    @PostMapping("/{orderNo}/complete")
    public Result<OrderResponse> complete(@CurrentUser LoginUser loginUser, @PathVariable String orderNo) {
        return Result.success(orderService.complete(loginUser.id(), orderNo));
    }

    @Operation(summary = "发货（管理员）")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{orderNo}/ship")
    public Result<OrderResponse> ship(@PathVariable String orderNo) {
        return Result.success(orderService.ship(orderNo));
    }

    @Operation(summary = "退款（管理员）")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{orderNo}/refund")
    public Result<OrderResponse> refund(@PathVariable String orderNo) {
        return Result.success(orderService.refund(orderNo));
    }
}
