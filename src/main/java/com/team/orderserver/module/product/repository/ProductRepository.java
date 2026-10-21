package com.team.orderserver.module.product.repository;

import com.team.orderserver.module.product.entity.Product;
import com.team.orderserver.module.product.entity.ProductStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    boolean existsBySku(String sku);

    Optional<Product> findBySku(String sku);

    /**
     * 条件更新扣减库存：stock >= quantity 时才更新，由数据库保证不超卖。
     *
     * @return 受影响行数，0 表示库存不足或商品不可售
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Product p
               set p.stock = p.stock - :quantity,
                   p.version = p.version + 1
             where p.id = :id
               and p.stock >= :quantity
               and p.status = :status
            """)
    int decreaseStock(@Param("id") Long id, @Param("quantity") int quantity,
                      @Param("status") ProductStatus status);

    /**
     * 归还库存（取消订单、超时关单时调用）。
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Product p
               set p.stock = p.stock + :quantity,
                   p.version = p.version + 1
             where p.id = :id
            """)
    int increaseStock(@Param("id") Long id, @Param("quantity") int quantity);
}
