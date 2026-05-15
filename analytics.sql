-- ================================================
-- 전체 현황
-- ================================================

-- 1. 이벤트 타입별 발생 횟수
-- 전체 이벤트 현황을 한눈에 파악
SELECT event_type,
       COUNT(*) AS count
FROM event_logs
GROUP BY event_type
ORDER BY count DESC;


-- 2. 정상/실패 이벤트 비율
-- 전체 이벤트 중 성공과 실패 비율
SELECT CASE WHEN is_succeeded THEN '정상' ELSE '실패' END   AS status,
       COUNT(*)                                             AS count,
       ROUND(COUNT(*)::numeric / SUM(COUNT(*)) OVER () * 100, 2) AS percent
FROM event_logs
GROUP BY is_succeeded;


-- ================================================
-- 상품 분석
-- ================================================

-- 3. 상품별 조회수 대비 구매 비율
-- 조회 후 실제 구매로 이어진 비율 파악
SELECT p.product_name,
       COUNT(CASE WHEN e.event_type = 'PAGE_VIEW' THEN 1 END)     AS view_count,
       COUNT(CASE WHEN e.event_type = 'ORDER_CREATED' THEN 1 END) AS order_count,
       ROUND(
               COUNT(CASE WHEN e.event_type = 'ORDER_CREATED' THEN 1 END)::numeric /
               NULLIF(COUNT(CASE WHEN e.event_type = 'PAGE_VIEW' THEN 1 END), 0) * 100,
               2)                                                  AS purchase_rate_percent
FROM event_logs e
         JOIN products p ON e.product_id = p.product_id
WHERE e.event_type IN ('PAGE_VIEW', 'ORDER_CREATED')
GROUP BY p.product_id, p.product_name
ORDER BY purchase_rate_percent DESC;


-- ================================================
-- 시간 분석
-- ================================================

-- 4. 시간대별 이벤트 발생량 및 주문/조회 비율
-- 서비스 피크 타임 및 시간대별 행동 패턴 파악
SELECT EXTRACT(HOUR FROM event_time)                                        AS hour,
       COUNT(*)                                                             AS total_events,
       COUNT(CASE WHEN event_type = 'PAGE_VIEW' THEN 1 END)                AS view_count,
       COUNT(CASE WHEN event_type = 'ORDER_CREATED' THEN 1 END)            AS order_count,
       ROUND(COUNT(CASE WHEN event_type = 'PAGE_VIEW' THEN 1 END)::numeric /
             NULLIF(COUNT(*), 0) * 100, 1)                                  AS view_rate_percent,
       ROUND(COUNT(CASE WHEN event_type = 'ORDER_CREATED' THEN 1 END)::numeric /
             NULLIF(COUNT(*), 0) * 100, 1)                                  AS order_rate_percent
FROM event_logs
GROUP BY hour
ORDER BY hour;


-- 5. 요일별 주문율
-- 어떤 요일에 주문이 가장 많이 발생하는지 파악
SELECT CASE EXTRACT(DOW FROM event_time)
           WHEN 0 THEN '일요일'
           WHEN 1 THEN '월요일'
           WHEN 2 THEN '화요일'
           WHEN 3 THEN '수요일'
           WHEN 4 THEN '목요일'
           WHEN 5 THEN '금요일'
           WHEN 6 THEN '토요일'
           END                                                              AS day_of_week,
       COUNT(CASE WHEN event_type = 'ORDER_CREATED' THEN 1 END)            AS order_count,
       COUNT(*)                                                             AS total_events,
       ROUND(COUNT(CASE WHEN event_type = 'ORDER_CREATED' THEN 1 END)::numeric /
             NULLIF(COUNT(*), 0) * 100, 2)                                  AS order_rate_percent
FROM event_logs
GROUP BY EXTRACT(DOW FROM event_time), day_of_week
ORDER BY EXTRACT(DOW FROM event_time);


-- ================================================
-- 디바이스 분석
-- ================================================

-- 6. 디바이스 타입별 주문율
-- 어떤 디바이스에서 주문이 더 많이 발생하는지 파악
SELECT device_type,
       COUNT(CASE WHEN event_type = 'ORDER_CREATED' THEN 1 END)            AS order_count,
       COUNT(*)                                                             AS total_events,
       ROUND(COUNT(CASE WHEN event_type = 'ORDER_CREATED' THEN 1 END)::numeric /
             COUNT(*) * 100, 2)                                             AS order_rate_percent
FROM event_logs
WHERE device_type IS NOT NULL
GROUP BY device_type
ORDER BY order_rate_percent DESC;


-- ================================================
-- 실패 분석
-- ================================================

-- 7. 주문 실패 원인별 비율
-- 어떤 이유로 주문이 실패하는지 파악
SELECT failed_reason,
       COUNT(*)                                                             AS fail_count,
       ROUND(COUNT(*)::numeric / SUM(COUNT(*)) OVER () * 100, 2)           AS percent
FROM event_logs
WHERE event_type = 'ORDER_FAILED'
  AND failed_reason IS NOT NULL
GROUP BY failed_reason
ORDER BY fail_count DESC;


-- ================================================
-- 유저 분석
-- ================================================

-- 8. 유저별 총 이벤트 수
-- 유저별 활동량 파악
SELECT u.user_id,
       u.user_grade,
       COUNT(*)                                                              AS total_events,
       COUNT(CASE WHEN e.event_type = 'LOGIN' THEN 1 END)                   AS login_count,
       COUNT(CASE WHEN e.event_type = 'PAGE_VIEW' THEN 1 END)               AS view_count,
       COUNT(CASE WHEN e.event_type = 'ORDER_CREATED' THEN 1 END)           AS order_count
FROM event_logs e
         JOIN users u ON e.user_id = u.user_id
GROUP BY u.user_id, u.user_grade
ORDER BY total_events DESC;


-- 9. 재구매 유저 비율
-- 2번 이상 주문한 유저 비율 파악
SELECT COUNT(DISTINCT CASE WHEN order_count >= 2 THEN user_id END)          AS repurchase_users,
       COUNT(DISTINCT user_id)                                               AS total_users,
       ROUND(COUNT(DISTINCT CASE WHEN order_count >= 2 THEN user_id END)::numeric /
             NULLIF(COUNT(DISTINCT user_id), 0) * 100, 2)                   AS repurchase_rate_percent
FROM (SELECT user_id, COUNT(*) AS order_count
      FROM orders
      GROUP BY user_id) order_stats;


-- 11. 카테고리별 판매량
-- 카테고리별 총 주문 수량 및 주문 건수 비교
SELECT c.category_name,
       COUNT(o.order_id)                                                    AS order_count,
       SUM(o.quantity)                                                      AS total_quantity,
       ROUND(AVG(o.quantity), 1)                                            AS avg_quantity
FROM orders o
         JOIN products p ON o.product_id = p.product_id
         JOIN categories c ON p.category_id = c.category_id
GROUP BY c.category_id, c.category_name
ORDER BY total_quantity DESC;


-- 10. 등급별 구매력
-- 등급별 총 주문 금액, 평균 주문 금액, 1인당 주문 횟수 비교
SELECT u.user_grade,
       COUNT(o.order_id)                                                    AS total_orders,
       SUM(o.price_at_order * o.quantity)                                   AS total_amount,
       ROUND(AVG(o.price_at_order * o.quantity), 0)                         AS avg_order_amount,
       ROUND(COUNT(o.order_id)::numeric / COUNT(DISTINCT u.user_id), 2)     AS orders_per_user
FROM users u
         LEFT JOIN orders o ON u.user_id = o.user_id
GROUP BY u.user_grade
ORDER BY total_amount DESC;