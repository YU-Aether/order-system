package com.team.orderserver.config;

import com.team.orderserver.module.product.entity.Product;
import com.team.orderserver.module.product.entity.ProductStatus;
import com.team.orderserver.module.product.repository.ProductRepository;
import com.team.orderserver.module.user.entity.User;
import com.team.orderserver.module.user.entity.UserRole;
import com.team.orderserver.module.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * 首次启动初始化：管理员账号与演示商品。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final PasswordEncoder passwordEncoder;
    private final BootstrapProperties bootstrapProperties;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        initAdmin();
        initProducts();
    }

    private void initAdmin() {
        String username = bootstrapProperties.adminUsername();
        if (userRepository.existsByUsername(username)) {
            return;
        }
        User admin = new User();
        admin.setUsername(username);
        admin.setPassword(passwordEncoder.encode(bootstrapProperties.adminPassword()));
        admin.setNickname("系统管理员");
        admin.setRole(UserRole.ADMIN);
        userRepository.save(admin);
        log.info("已初始化管理员账号 | username={} | 请尽快修改默认密码", username);
    }

    private void initProducts() {
        if (productRepository.count() > 0) {
            return;
        }
        createProduct("SKU-1001", "罗技 M330 静音鼠标", "129.00", 100);
        createProduct("SKU-1002", "机械键盘 K8", "399.00", 60);
        createProduct("SKU-1003", "27 寸 2K 显示器", "1299.00", 20);
        log.info("已初始化演示商品");
    }

    private void createProduct(String sku, String name, String price, int stock) {
        Product product = new Product();
        product.setSku(sku);
        product.setName(name);
        product.setPrice(new BigDecimal(price));
        product.setStock(stock);
        product.setStatus(ProductStatus.ON_SALE);
        productRepository.save(product);
    }
}
