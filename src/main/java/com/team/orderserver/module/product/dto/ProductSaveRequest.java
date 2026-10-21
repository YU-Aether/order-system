package com.team.orderserver.module.product.dto;

import com.team.orderserver.module.product.entity.ProductStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.math.BigDecimal;

public record ProductSaveRequest(
        @NotBlank(message = "SKU 不能为空")
        @Size(max = 64, message = "SKU 长度不能超过 64")
        String sku,

        @NotBlank(message = "商品名称不能为空")
        @Size(max = 128, message = "商品名称长度不能超过 128")
        String name,

        @NotNull(message = "价格不能为空")
        @DecimalMin(value = "0.01", message = "价格必须大于 0")
        @Digits(integer = 10, fraction = 2, message = "价格最多 10 位整数、2 位小数")
        BigDecimal price,

        @NotNull(message = "库存不能为空")
        @Min(value = 0, message = "库存不能为负数")
        Integer stock,

        ProductStatus status
) implements Serializable {
}
