package com.team.orderserver.module.product.service;

import com.team.orderserver.common.api.PageResult;
import com.team.orderserver.common.api.ResultCode;
import com.team.orderserver.common.exception.BusinessException;
import com.team.orderserver.module.product.dto.ProductResponse;
import com.team.orderserver.module.product.dto.ProductSaveRequest;
import com.team.orderserver.module.product.dto.StockAdjustRequest;
import com.team.orderserver.module.product.entity.Product;
import com.team.orderserver.module.product.entity.ProductStatus;
import com.team.orderserver.module.product.repository.ProductRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    public PageResult<ProductResponse> page(String keyword, ProductStatus status, int page, int size) {
        Specification<Product> specification = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(keyword)) {
                String like = "%" + keyword.trim() + "%";
                predicates.add(cb.or(cb.like(root.get("name"), like), cb.like(root.get("sku"), like)));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Page<Product> result = productRepository.findAll(specification,
                PageRequest.of(Math.max(page - 1, 0), size, Sort.by(Sort.Direction.DESC, "id")));
        return PageResult.of(result, this::toResponse);
    }

    public ProductResponse getById(Long id) {
        return toResponse(requireProduct(id));
    }

    @Transactional
    public ProductResponse create(ProductSaveRequest request) {
        if (productRepository.existsBySku(request.sku())) {
            throw new BusinessException(ResultCode.PRODUCT_SKU_EXISTS);
        }
        Product product = new Product();
        apply(product, request);
        productRepository.save(product);
        log.info("创建商品 | id={} | sku={}", product.getId(), product.getSku());
        return toResponse(product);
    }

    @Transactional
    public ProductResponse update(Long id, ProductSaveRequest request) {
        Product product = requireProduct(id);
        if (!product.getSku().equals(request.sku()) && productRepository.existsBySku(request.sku())) {
            throw new BusinessException(ResultCode.PRODUCT_SKU_EXISTS);
        }
        apply(product, request);
        productRepository.save(product);
        return toResponse(product);
    }

    @Transactional
    public ProductResponse adjustStock(Long id, StockAdjustRequest request) {
        Product product = requireProduct(id);
        int newStock = product.getStock() + request.delta();
        if (newStock < 0) {
            throw new BusinessException(ResultCode.STOCK_NOT_ENOUGH);
        }
        if (request.delta() > 0) {
            productRepository.increaseStock(id, request.delta());
        } else if (request.delta() < 0) {
            int affected = productRepository.decreaseStock(id, -request.delta(), product.getStatus());
            if (affected == 0) {
                throw new BusinessException(ResultCode.STOCK_NOT_ENOUGH);
            }
        }
        log.info("调整库存 | productId={} | delta={} | reason={}", id, request.delta(), request.reason());
        return getById(id);
    }

    private Product requireProduct(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ResultCode.PRODUCT_NOT_FOUND));
    }

    private void apply(Product product, ProductSaveRequest request) {
        product.setSku(request.sku());
        product.setName(request.name());
        product.setPrice(request.price());
        product.setStock(request.stock());
        product.setStatus(request.status() == null ? ProductStatus.ON_SALE : request.status());
    }

    private ProductResponse toResponse(Product product) {
        return new ProductResponse(product.getId(), product.getSku(), product.getName(), product.getPrice(),
                product.getStock(), product.getStatus().name(), product.getCreatedAt());
    }
}
