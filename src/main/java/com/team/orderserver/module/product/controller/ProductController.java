package com.team.orderserver.module.product.controller;

import com.team.orderserver.common.api.PageResult;
import com.team.orderserver.common.api.Result;
import com.team.orderserver.module.product.dto.ProductResponse;
import com.team.orderserver.module.product.dto.ProductSaveRequest;
import com.team.orderserver.module.product.dto.StockAdjustRequest;
import com.team.orderserver.module.product.entity.ProductStatus;
import com.team.orderserver.module.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "02-商品", description = "商品查询与后台管理")
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @Operation(summary = "分页查询商品（公开）")
    @GetMapping
    public Result<PageResult<ProductResponse>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) ProductStatus status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(productService.page(keyword, status, page, Math.min(size, 100)));
    }

    @Operation(summary = "商品详情（公开）")
    @GetMapping("/{id}")
    public Result<ProductResponse> detail(@PathVariable Long id) {
        return Result.success(productService.getById(id));
    }

    @Operation(summary = "新增商品（管理员）")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public Result<ProductResponse> create(@Valid @RequestBody ProductSaveRequest request) {
        return Result.success(productService.create(request));
    }

    @Operation(summary = "修改商品（管理员）")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public Result<ProductResponse> update(@PathVariable Long id, @Valid @RequestBody ProductSaveRequest request) {
        return Result.success(productService.update(id, request));
    }

    @Operation(summary = "调整库存（管理员）")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/stock")
    public Result<ProductResponse> adjustStock(@PathVariable Long id,
                                               @Valid @RequestBody StockAdjustRequest request) {
        return Result.success(productService.adjustStock(id, request));
    }
}
