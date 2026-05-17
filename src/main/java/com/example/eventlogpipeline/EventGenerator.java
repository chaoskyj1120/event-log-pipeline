package com.example.eventlogpipeline;

import com.example.eventlogpipeline.entity.*;
import com.example.eventlogpipeline.repository.CategoryRepository;
import com.example.eventlogpipeline.repository.EventLogRepository;
import com.example.eventlogpipeline.repository.ProductRepository;
import com.example.eventlogpipeline.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventGenerator implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final EventLogRepository eventLogRepository;

    @Value("${server.port:8080}")
    private int serverPort;

    private static final int EVENT_COUNT = 1000;
    private final Random random = new Random();
    private RestTemplate restTemplate;
    private String baseUrl;

    @Override
    public void run(String... args) {
        restTemplate = new RestTemplate();
        baseUrl = "http://localhost:" + serverPort;

        if (eventLogRepository.count() > 0) {
            log.info("=== 이벤트 데이터가 이미 존재합니다. 생성을 건너뜁니다. ===");
            return;
        }

        List<User> users = initUsers();
        List<Product> products = initProducts();

        log.info("=== 이벤트 생성 시작: {}건 ===", EVENT_COUNT);

        for (int i = 0; i < EVENT_COUNT; i++) {
            User user = users.get(random.nextInt(users.size()));
            Product product = products.get(random.nextInt(products.size()));
            DeviceType deviceType = randomDeviceType();

            int roll = random.nextInt(10);

            if (roll < 4) {
                sendLogin(user, deviceType, i + 1);
            } else if (roll < 7) {
                sendPageView(user, product, deviceType, i + 1);
            } else {
                sendOrder(user, product, deviceType, i + 1);
            }
        }

        log.info("=== 이벤트 생성 완료 ===");
    }

    private Instant randomEventTime() {
        long secondsInMonth = 30L * 24 * 60 * 60;
        long randomSeconds = (long) (random.nextDouble() * secondsInMonth);
        return Instant.now().minusSeconds(randomSeconds);
    }

    private void sendLogin(User user, DeviceType deviceType, int index) {
        Map<String, Object> body = Map.of(
                "userId", user.getUserId(),
                "deviceType", deviceType.name(),
                "eventTime", randomEventTime().toString()
        );
        try {
            post("/users/login", body);
            log.info("[{}/{}] LOGIN - user={}", index, EVENT_COUNT, user.getUserId());
        } catch (Exception e) {
            log.warn("[{}/{}] LOGIN 실패: {}", index, EVENT_COUNT, e.getMessage());
        }
    }

    private void sendPageView(User user, Product product, DeviceType deviceType, int index) {
        String url = baseUrl + "/products/" + product.getProductId()
                + "?userId=" + user.getUserId()
                + "&deviceType=" + deviceType.name()
                + "&eventTime=" + randomEventTime().toString();
        try {
            restTemplate.getForObject(url, Map.class);
            log.info("[{}/{}] PAGE_VIEW - user={}, product={}", index, EVENT_COUNT, user.getUserId(), product.getProductName());
        } catch (Exception e) {
            log.warn("[{}/{}] PAGE_VIEW 실패: {}", index, EVENT_COUNT, e.getMessage());
        }
    }

    private void sendOrder(User user, Product product, DeviceType deviceType, int index) {
        Map<String, Object> body = Map.of(
                "userId", user.getUserId(),
                "productId", product.getProductId(),
                "quantity", random.nextInt(30) + 1,
                "deviceType", deviceType.name(),
                "eventTime", randomEventTime().toString()
        );
        try {
            post("/orders", body);
            log.info("[{}/{}] ORDER_CREATED - user={}, product={}", index, EVENT_COUNT, user.getUserId(), product.getProductName());
        } catch (HttpClientErrorException.Conflict e) {
            log.info("[{}/{}] ORDER_FAILED (재고 부족) - user={}, product={}", index, EVENT_COUNT, user.getUserId(), product.getProductName());
        } catch (Exception e) {
            log.warn("[{}/{}] ORDER 실패: {}", index, EVENT_COUNT, e.getMessage());
        }
    }

    private void post(String path, Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        restTemplate.postForObject(baseUrl + path, new HttpEntity<>(body, headers), Map.class);
    }

    private List<User> initUsers() {
        if (userRepository.count() > 0) {
            return userRepository.findAll();
        }
        return userRepository.saveAll(List.of(
                User.builder().userGrade(UserGrade.BRONZE).build(),
                User.builder().userGrade(UserGrade.BRONZE).build(),
                User.builder().userGrade(UserGrade.BRONZE).build(),
                User.builder().userGrade(UserGrade.BRONZE).build(),
                User.builder().userGrade(UserGrade.SILVER).build(),
                User.builder().userGrade(UserGrade.SILVER).build(),
                User.builder().userGrade(UserGrade.SILVER).build(),
                User.builder().userGrade(UserGrade.GOLD).build(),
                User.builder().userGrade(UserGrade.GOLD).build(),
                User.builder().userGrade(UserGrade.GOLD).build()
        ));
    }

    private List<Product> initProducts() {
        if (productRepository.count() > 0) {
            return productRepository.findAll();
        }
        Category electronics = categoryRepository.save(Category.builder().categoryName("전자기기").build());
        Category clothing = categoryRepository.save(Category.builder().categoryName("의류").build());

        return productRepository.saveAll(List.of(
                Product.builder().productName("노트북").category(electronics).price(1200000).discount(new BigDecimal("0.050")).stock(200).build(),
                Product.builder().productName("스마트폰").category(electronics).price(800000).discount(new BigDecimal("0.100")).stock(200).build(),
                Product.builder().productName("이어폰").category(electronics).price(150000).discount(new BigDecimal("0.200")).stock(200).build(),
                Product.builder().productName("태블릿").category(electronics).price(600000).discount(new BigDecimal("0.080")).stock(200).build(),
                Product.builder().productName("스마트워치").category(electronics).price(350000).discount(new BigDecimal("0.150")).stock(200).build(),
                Product.builder().productName("티셔츠").category(clothing).price(29000).discount(BigDecimal.ZERO).stock(200).build(),
                Product.builder().productName("청바지").category(clothing).price(59000).discount(new BigDecimal("0.050")).stock(200).build(),
                Product.builder().productName("패딩").category(clothing).price(180000).discount(new BigDecimal("0.100")).stock(200).build()
        ));
    }

    private DeviceType randomDeviceType() {
        DeviceType[] types = DeviceType.values();
        return types[random.nextInt(types.length)];
    }
}