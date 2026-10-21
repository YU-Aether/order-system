package com.team.orderserver.common.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 订单号生成器：时间戳 + 进程内自增序列 + 随机数。
 * <p>单实例严格递增；多实例依赖数据库唯一索引兜底。</p>
 */
public final class OrderNoGenerator {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
    private static final AtomicInteger SEQUENCE = new AtomicInteger(0);
    private static final int MAX_SEQUENCE = 9999;

    private OrderNoGenerator() {
    }

    public static String generate() {
        int seq = SEQUENCE.updateAndGet(value -> value >= MAX_SEQUENCE ? 0 : value + 1);
        int random = ThreadLocalRandom.current().nextInt(100, 1000);
        return "OD" + LocalDateTime.now().format(FORMATTER) + String.format("%04d", seq) + random;
    }
}
