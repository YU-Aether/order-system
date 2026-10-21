package com.team.orderserver.module.order.service;

import com.team.orderserver.common.api.ResultCode;
import com.team.orderserver.common.exception.BusinessException;
import com.team.orderserver.common.lock.RedisDistributedLock;
import com.team.orderserver.config.OrderProperties;
import com.team.orderserver.module.order.dto.CreateOrderRequest;
import com.team.orderserver.module.order.dto.OrderItemRequest;
import com.team.orderserver.module.order.dto.OrderResponse;
import com.team.orderserver.module.order.entity.Order;
import com.team.orderserver.module.order.entity.OrderItem;
import com.team.orderserver.module.order.entity.OrderStatus;
import com.team.orderserver.module.order.repository.OrderItemRepository;
import com.team.orderserver.module.order.repository.OrderRepository;
import com.team.orderserver.module.payment.repository.PaymentRecordRepository;
import com.team.orderserver.module.product.entity.Product;
import com.team.orderserver.module.product.entity.ProductStatus;
import com.team.orderserver.module.product.repository.ProductRepository;
import com.team.orderserver.mq.OrderEventPublisher;
import com.team.orderserver.mq.event.OrderEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderItemRepository orderItemRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private PaymentRecordRepository paymentRecordRepository;
    @Mock
    private RedisDistributedLock distributedLock;
    @Mock
    private OrderCacheService orderCacheService;
    @Mock
    private OrderTimeoutScheduler timeoutScheduler;
    @Mock
    private OrderEventPublisher eventPublisher;
    @Mock
    private OrderStateMachine orderStateMachine;
    @Mock
    private TransactionTemplate transactionTemplate;

    private OrderServiceImpl orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderServiceImpl(orderRepository, orderItemRepository, productRepository,
                paymentRecordRepository, distributedLock, orderCacheService, timeoutScheduler,
                eventPublisher, orderStateMachine, new OrderProperties(15, 100), transactionTemplate);

        lenient().when(distributedLock.tryLock(anyString(), any(Duration.class), any(Duration.class)))
                .thenReturn("lock-token");
        lenient().doAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(mock(TransactionStatus.class));
        }).when(transactionTemplate).execute(any(TransactionCallback.class));
    }

    @Test
    void createShouldThrowWhenStockNotEnough() {
        when(productRepository.findAllById(anyCollection()))
                .thenReturn(List.of(product(1L, "129.00", 0)));
        when(productRepository.decreaseStock(eq(1L), eq(2), eq(ProductStatus.ON_SALE))).thenReturn(0);

        CreateOrderRequest request = new CreateOrderRequest(List.of(new OrderItemRequest(1L, 2)), null);

        BusinessException ex = assertThrows(BusinessException.class, () -> orderService.create(10L, request));

        assertEquals(ResultCode.STOCK_NOT_ENOUGH, ex.getResultCode());
        verify(orderRepository, never()).save(any(Order.class));
        verify(distributedLock).unlock(anyString(), eq("lock-token"));
    }

    @Test
    void createShouldCalculateTotalAmount() {
        when(productRepository.findAllById(anyCollection()))
                .thenReturn(List.of(product(1L, "129.00", 10)));
        when(productRepository.decreaseStock(anyLong(), anyInt(), any(ProductStatus.class))).thenReturn(1);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(100L);
            return order;
        });
        when(orderItemRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        CreateOrderRequest request = new CreateOrderRequest(List.of(new OrderItemRequest(1L, 3)), "尽快发货");

        OrderResponse response = orderService.create(10L, request);

        assertEquals(new BigDecimal("387.00"), response.totalAmount());
        assertEquals(OrderStatus.CREATED.name(), response.status());
        assertEquals(1, response.items().size());
        verify(timeoutScheduler).schedule(eq(response.orderNo()), any(LocalDateTime.class));
        verify(eventPublisher).publish(any(OrderEvent.class));
        verify(distributedLock).unlock(anyString(), eq("lock-token"));
    }

    @Test
    void createShouldFailFastWhenLockNotAcquired() {
        when(distributedLock.tryLock(anyString(), any(Duration.class), any(Duration.class))).thenReturn(null);

        CreateOrderRequest request = new CreateOrderRequest(List.of(new OrderItemRequest(1L, 1)), null);

        BusinessException ex = assertThrows(BusinessException.class, () -> orderService.create(10L, request));

        assertEquals(ResultCode.DUPLICATE_REQUEST, ex.getResultCode());
        verify(productRepository, never()).findAllById(any());
    }

    private Product product(Long id, String price, int stock) {
        Product product = new Product();
        product.setId(id);
        product.setSku("SKU-" + id);
        product.setName("测试商品" + id);
        product.setPrice(new BigDecimal(price));
        product.setStock(stock);
        product.setStatus(ProductStatus.ON_SALE);
        return product;
    }

    /**
     * 编译期约束：确保测试与实现使用同一套实体字段。
     */
    @SuppressWarnings("unused")
    private OrderItem sampleItem() {
        return new OrderItem();
    }
}
