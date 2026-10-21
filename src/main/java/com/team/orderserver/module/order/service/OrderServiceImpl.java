package com.team.orderserver.module.order.service;

import com.team.orderserver.common.api.PageResult;
import com.team.orderserver.common.api.ResultCode;
import com.team.orderserver.common.constant.RedisKeys;
import com.team.orderserver.common.exception.BusinessException;
import com.team.orderserver.common.idempotent.Idempotent;
import com.team.orderserver.common.lock.RedisDistributedLock;
import com.team.orderserver.common.util.OrderNoGenerator;
import com.team.orderserver.config.OrderProperties;
import com.team.orderserver.module.order.dto.CreateOrderRequest;
import com.team.orderserver.module.order.dto.OrderItemRequest;
import com.team.orderserver.module.order.dto.OrderItemResponse;
import com.team.orderserver.module.order.dto.OrderQuery;
import com.team.orderserver.module.order.dto.OrderResponse;
import com.team.orderserver.module.order.entity.Order;
import com.team.orderserver.module.order.entity.OrderItem;
import com.team.orderserver.module.order.entity.OrderStatus;
import com.team.orderserver.module.order.repository.OrderItemRepository;
import com.team.orderserver.module.order.repository.OrderRepository;
import com.team.orderserver.module.payment.entity.PaymentChannel;
import com.team.orderserver.module.payment.entity.PaymentRecord;
import com.team.orderserver.module.payment.entity.PaymentStatus;
import com.team.orderserver.module.payment.repository.PaymentRecordRepository;
import com.team.orderserver.module.product.entity.Product;
import com.team.orderserver.module.product.entity.ProductStatus;
import com.team.orderserver.module.product.repository.ProductRepository;
import com.team.orderserver.mq.OrderEventPublisher;
import com.team.orderserver.mq.event.OrderEvent;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 订单核心服务。
 * <p>关键设计：</p>
 * <ul>
 *   <li>下单：幂等键 + 用户级分布式锁 + 事务模板 + 条件更新扣库存，杜绝超卖与重复下单</li>
 *   <li>状态变更：悲观锁 + 状态机校验，保证并发下状态唯一演进</li>
 *   <li>超时关单：Redis ZSet 延迟队列 + 分布式锁，多实例安全</li>
 *   <li>事件：事务提交后发布 RabbitMQ 事件，避免消息先于数据落库</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final PaymentRecordRepository paymentRecordRepository;
    private final RedisDistributedLock distributedLock;
    private final OrderCacheService orderCacheService;
    private final OrderTimeoutScheduler timeoutScheduler;
    private final OrderEventPublisher eventPublisher;
    private final OrderStateMachine stateMachine;
    private final OrderProperties orderProperties;
    private final TransactionTemplate transactionTemplate;

    @Override
    @Idempotent(keyPrefix = "order:create", timeoutSeconds = 600)
    public OrderResponse create(Long userId, CreateOrderRequest request) {
        String lockKey = RedisKeys.ORDER_CREATE_LOCK + userId;
        String token = distributedLock.tryLock(lockKey, Duration.ofSeconds(2), Duration.ofSeconds(10));
        if (token == null) {
            throw new BusinessException(ResultCode.DUPLICATE_REQUEST, "已有下单请求正在处理，请稍后再试");
        }
        try {
            OrderResponse response = transactionTemplate.execute(status -> doCreate(userId, request));
            if (response == null) {
                throw new BusinessException(ResultCode.INTERNAL_ERROR);
            }
            // 事务已提交：注册延迟关单任务并发布领域事件
            timeoutScheduler.schedule(response.orderNo(), response.expireAt());
            eventPublisher.publish(OrderEvent.created(response.orderNo(), userId, response.totalAmount()));
            return response;
        } finally {
            distributedLock.unlock(lockKey, token);
        }
    }

    @Override
    public PageResult<OrderResponse> page(Long userId, boolean admin, OrderQuery query) {
        Specification<Order> specification = (root, criteriaQuery, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (!admin) {
                predicates.add(cb.equal(root.get("userId"), userId));
            }
            if (query.statusEnum() != null) {
                predicates.add(cb.equal(root.get("status"), query.statusEnum()));
            }
            if (StringUtils.hasText(query.keyword())) {
                predicates.add(cb.like(root.get("orderNo"), "%" + query.keyword().trim() + "%"));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Order> page = orderRepository.findAll(specification,
                PageRequest.of(query.pageNo() - 1, query.pageSize(), Sort.by(Sort.Direction.DESC, "id")));

        List<Long> orderIds = page.getContent().stream().map(Order::getId).toList();
        Map<Long, List<OrderItem>> itemMap = orderIds.isEmpty()
                ? Map.of()
                : orderItemRepository.findByOrderIdIn(orderIds).stream()
                        .collect(Collectors.groupingBy(OrderItem::getOrderId));

        List<OrderResponse> records = page.getContent().stream()
                .map(order -> toResponse(order, itemMap.getOrDefault(order.getId(), List.of())))
                .toList();

        return new PageResult<>(page.getTotalElements(), page.getNumber() + 1, page.getSize(),
                page.getTotalPages(), records);
    }

    @Override
    public OrderResponse detail(Long userId, boolean admin, String orderNo) {
        OrderResponse cached = orderCacheService.get(orderNo);
        if (cached != null) {
            checkOwnership(cached.userId(), userId, admin);
            return cached;
        }
        Order order = requireOrder(orderNo);
        checkOwnership(order.getUserId(), userId, admin);
        OrderResponse response = toResponse(order,
                orderItemRepository.findByOrderIdOrderByIdAsc(order.getId()));
        orderCacheService.put(response);
        return response;
    }

    @Override
    public OrderResponse pay(Long userId, String orderNo, PaymentChannel channel) {
        OrderResponse response = transactionTemplate.execute(status -> {
            Order order = orderRepository.findByOrderNoForUpdate(orderNo)
                    .orElseThrow(() -> new BusinessException(ResultCode.ORDER_NOT_FOUND));
            checkOwnership(order.getUserId(), userId, false);
            if (order.getStatus() == OrderStatus.PAID) {
                throw new BusinessException(ResultCode.PAYMENT_DUPLICATED);
            }
            if (order.getStatus() != OrderStatus.CREATED) {
                throw new BusinessException(ResultCode.ORDER_STATUS_INVALID,
                        "订单当前状态[" + order.getStatus().getDescription() + "]不可支付");
            }
            if (order.getExpireAt().isBefore(LocalDateTime.now())) {
                throw new BusinessException(ResultCode.ORDER_EXPIRED);
            }

            stateMachine.transit(order, OrderStatus.PAID);
            order.setPaidAt(LocalDateTime.now());
            orderRepository.save(order);

            PaymentRecord record = new PaymentRecord();
            record.setPayNo("PAY" + OrderNoGenerator.generate().substring(2));
            record.setOrderNo(orderNo);
            record.setAmount(order.getTotalAmount());
            record.setChannel(channel == null ? PaymentChannel.MOCK : channel);
            record.setStatus(PaymentStatus.SUCCESS);
            record.setPaidAt(LocalDateTime.now());
            paymentRecordRepository.save(record);

            log.info("订单支付成功 | orderNo={} | amount={} | channel={}", orderNo, order.getTotalAmount(),
                    record.getChannel());
            return toResponse(order, orderItemRepository.findByOrderIdOrderByIdAsc(order.getId()));
        });

        OrderResponse result = requireResult(response, ResultCode.PAYMENT_FAILED);
        orderCacheService.evict(orderNo);
        timeoutScheduler.remove(orderNo);
        eventPublisher.publish(OrderEvent.paid(orderNo, userId, result.totalAmount()));
        return result;
    }

    @Override
    public OrderResponse cancel(Long userId, String orderNo) {
        return closeOrder(orderNo, userId, false, "用户主动取消");
    }

    @Override
    public void closeTimeoutOrder(String orderNo) {
        closeOrder(orderNo, null, true, "超时未支付，系统自动关闭");
    }

    @Override
    public OrderResponse ship(String orderNo) {
        OrderResponse response = transactionTemplate.execute(status -> {
            Order order = orderRepository.findByOrderNoForUpdate(orderNo)
                    .orElseThrow(() -> new BusinessException(ResultCode.ORDER_NOT_FOUND));
            stateMachine.transit(order, OrderStatus.SHIPPED);
            orderRepository.save(order);
            log.info("订单已发货 | orderNo={}", orderNo);
            return toResponse(order, orderItemRepository.findByOrderIdOrderByIdAsc(order.getId()));
        });
        OrderResponse result = requireResult(response, ResultCode.ORDER_STATUS_INVALID);
        orderCacheService.evict(orderNo);
        return result;
    }

    @Override
    public OrderResponse complete(Long userId, String orderNo) {
        OrderResponse response = transactionTemplate.execute(status -> {
            Order order = orderRepository.findByOrderNoForUpdate(orderNo)
                    .orElseThrow(() -> new BusinessException(ResultCode.ORDER_NOT_FOUND));
            checkOwnership(order.getUserId(), userId, false);
            stateMachine.transit(order, OrderStatus.COMPLETED);
            orderRepository.save(order);
            return toResponse(order, orderItemRepository.findByOrderIdOrderByIdAsc(order.getId()));
        });
        OrderResponse result = requireResult(response, ResultCode.ORDER_STATUS_INVALID);
        orderCacheService.evict(orderNo);
        return result;
    }

    @Override
    public OrderResponse refund(String orderNo) {
        OrderResponse response = transactionTemplate.execute(status -> {
            Order order = orderRepository.findByOrderNoForUpdate(orderNo)
                    .orElseThrow(() -> new BusinessException(ResultCode.ORDER_NOT_FOUND));
            stateMachine.transit(order, OrderStatus.REFUNDED);
            orderRepository.save(order);

            paymentRecordRepository.findByOrderNo(orderNo).ifPresent(record -> {
                record.setStatus(PaymentStatus.REFUNDED);
                paymentRecordRepository.save(record);
            });
            log.info("订单已退款 | orderNo={} | amount={}", orderNo, order.getTotalAmount());
            return toResponse(order, orderItemRepository.findByOrderIdOrderByIdAsc(order.getId()));
        });
        OrderResponse result = requireResult(response, ResultCode.ORDER_STATUS_INVALID);
        orderCacheService.evict(orderNo);
        return result;
    }

    // ---------------------------------------------------------------- 内部实现

    private OrderResponse doCreate(Long userId, CreateOrderRequest request) {
        Map<Long, Integer> quantities = new LinkedHashMap<>();
        for (OrderItemRequest item : request.items()) {
            quantities.merge(item.productId(), item.quantity(), Integer::sum);
        }

        List<Product> products = productRepository.findAllById(quantities.keySet());
        if (products.size() != quantities.size()) {
            throw new BusinessException(ResultCode.PRODUCT_NOT_FOUND);
        }

        List<OrderItem> items = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (Product product : products) {
            int quantity = quantities.get(product.getId());
            if (product.getStatus() != ProductStatus.ON_SALE) {
                throw new BusinessException(ResultCode.PRODUCT_OFF_SALE, "商品已下架: " + product.getName());
            }
            // 条件更新：stock >= quantity 才会成功，由数据库行锁保证并发安全
            int affected = productRepository.decreaseStock(product.getId(), quantity, ProductStatus.ON_SALE);
            if (affected == 0) {
                throw new BusinessException(ResultCode.STOCK_NOT_ENOUGH, "库存不足: " + product.getName());
            }

            BigDecimal subtotal = product.getPrice().multiply(BigDecimal.valueOf(quantity));
            totalAmount = totalAmount.add(subtotal);

            OrderItem item = new OrderItem();
            item.setProductId(product.getId());
            item.setProductName(product.getName());
            item.setProductPrice(product.getPrice());
            item.setQuantity(quantity);
            item.setSubtotal(subtotal);
            items.add(item);
        }

        Order order = new Order();
        order.setOrderNo(OrderNoGenerator.generate());
        order.setUserId(userId);
        order.setTotalAmount(totalAmount);
        order.setStatus(OrderStatus.CREATED);
        order.setRemark(request.remark());
        order.setExpireAt(LocalDateTime.now().plusMinutes(orderProperties.timeoutMinutes()));
        Order saved = orderRepository.save(order);

        items.forEach(item -> item.setOrderId(saved.getId()));
        orderItemRepository.saveAll(items);

        log.info("订单创建成功 | orderNo={} | userId={} | amount={} | items={}",
                saved.getOrderNo(), userId, totalAmount, items.size());
        return toResponse(saved, items);
    }

    /**
     * 统一的关单流程：分布式锁 → 事务 → 归还库存 → 状态流转 → 清理延迟任务与缓存 → 发事件。
     *
     * @param operatorId 操作人，system=true 时忽略
     * @param system     是否系统自动关单
     */
    private OrderResponse closeOrder(String orderNo, Long operatorId, boolean system, String reason) {
        String lockKey = RedisKeys.ORDER_CLOSE_LOCK + orderNo;
        String token = distributedLock.tryLock(lockKey, Duration.ofSeconds(2), Duration.ofSeconds(10));
        if (token == null) {
            throw new BusinessException(ResultCode.DUPLICATE_REQUEST, "订单正在处理中，请稍后再试");
        }
        try {
            OrderResponse response = transactionTemplate.execute(status -> {
                Order order = orderRepository.findByOrderNoForUpdate(orderNo).orElse(null);
                if (order == null) {
                    return null;
                }
                if (!system) {
                    checkOwnership(order.getUserId(), operatorId, false);
                }
                if (order.getStatus() != OrderStatus.CREATED) {
                    // 已被支付或已关闭：系统任务直接跳过，用户操作则报错
                    if (system) {
                        return null;
                    }
                    throw new BusinessException(ResultCode.ORDER_STATUS_INVALID, "只有待支付订单可以取消");
                }

                restoreStock(order);
                stateMachine.transit(order, OrderStatus.CANCELLED);
                order.setCancelledAt(LocalDateTime.now());
                orderRepository.save(order);

                log.info("订单已关闭 | orderNo={} | system={} | reason={}", orderNo, system, reason);
                return toResponse(order, orderItemRepository.findByOrderIdOrderByIdAsc(order.getId()));
            });

            if (response == null) {
                timeoutScheduler.remove(orderNo);
                return null;
            }
            orderCacheService.evict(orderNo);
            timeoutScheduler.remove(orderNo);
            eventPublisher.publish(OrderEvent.cancelled(orderNo, response.userId(), reason));
            return response;
        } finally {
            distributedLock.unlock(lockKey, token);
        }
    }

    private void restoreStock(Order order) {
        List<OrderItem> items = orderItemRepository.findByOrderIdOrderByIdAsc(order.getId());
        for (OrderItem item : items) {
            productRepository.increaseStock(item.getProductId(), item.getQuantity());
        }
    }

    private Order requireOrder(String orderNo) {
        return orderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new BusinessException(ResultCode.ORDER_NOT_FOUND));
    }

    private void checkOwnership(Long ownerId, Long userId, boolean admin) {
        if (!admin && !ownerId.equals(userId)) {
            throw new BusinessException(ResultCode.ORDER_NOT_OWNED);
        }
    }

    private OrderResponse requireResult(OrderResponse response, ResultCode fallback) {
        if (response == null) {
            throw new BusinessException(fallback);
        }
        return response;
    }

    private OrderResponse toResponse(Order order, List<OrderItem> items) {
        List<OrderItemResponse> itemResponses = items.stream()
                .map(item -> new OrderItemResponse(item.getProductId(), item.getProductName(),
                        item.getProductPrice(), item.getQuantity(), item.getSubtotal()))
                .toList();
        return new OrderResponse(order.getOrderNo(), order.getUserId(), order.getTotalAmount(),
                order.getStatus().name(), order.getStatus().getDescription(), order.getRemark(),
                order.getExpireAt(), order.getPaidAt(), order.getCancelledAt(), order.getCreatedAt(), itemResponses);
    }
}
