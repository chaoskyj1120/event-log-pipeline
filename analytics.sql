  -- ================================================
  -- 이벤트 분석
  -- ================================================

  -- 이벤트 타입별 발생 횟수
  SELECT event_type,
         COUNT(*) AS count
  FROM event_logs
  GROUP BY event_type
  ORDER BY count DESC;


  -- 전체 이벤트 중 성공과 실패 비율
  SELECT CASE WHEN is_succeeded THEN '정상' ELSE '실패' END AS status,
         COUNT(*) AS count
  FROM event_logs
  GROUP BY is_succeeded;


  -- 시간대별 이벤트 발생량
  SELECT EXTRACT(HOUR FROM event_time) AS hour,
         COUNT(*) AS total_events
  FROM event_logs
  GROUP BY hour
  ORDER BY hour;


  -- ================================================
  -- 디바이스 분석
  -- ================================================

  -- 디바이스 타입별 주문율
  SELECT device_type,
         COUNT(CASE WHEN event_type = 'ORDER_CREATED' THEN 1 END) AS order_count,
         COUNT(*) AS total_events
  FROM event_logs
  WHERE device_type IS NOT NULL
  GROUP BY device_type;


  -- ================================================
  -- 상품 분석
  -- ================================================

  -- 카테고리별 판매량
  SELECT c.category_name,
         SUM(o.quantity) AS total_quantity
  FROM orders o
           JOIN products p ON o.product_id = p.product_id
           JOIN categories c ON p.category_id = c.category_id
  GROUP BY c.category_id, c.category_name
  ORDER BY total_quantity DESC;


  -- 상품별 총 매출
  SELECT p.product_name,
         SUM(o.price_at_order * o.quantity) AS total_revenue
  FROM orders o
           JOIN products p ON o.product_id = p.product_id
  GROUP BY p.product_id, p.product_name
  ORDER BY total_revenue DESC;


  -- 상품별 주문 실패율
  SELECT p.product_name,
         ROUND(COUNT(CASE WHEN e.event_type = 'ORDER_FAILED' THEN 1 END)::numeric /
               NULLIF(COUNT(CASE WHEN e.event_type IN ('ORDER_CREATED', 'ORDER_FAILED') THEN 1 END), 0) * 100, 1)
               AS fail_rate_percent
  FROM event_logs e
           JOIN products p ON e.product_id = p.product_id
  WHERE e.event_type IN ('ORDER_CREATED', 'ORDER_FAILED')
  GROUP BY p.product_id, p.product_name
  ORDER BY fail_rate_percent DESC;


  -- ================================================
  -- 할인 분석
  -- ================================================

  -- 할인율 구간별 구매량
  SELECT CASE
             WHEN discount_at_order IS NULL OR discount_at_order = 0 THEN '할인 없음'
             WHEN discount_at_order < 0.1 THEN '10% 미만'
             WHEN discount_at_order < 0.2 THEN '10~20%'
             WHEN discount_at_order < 0.3 THEN '20~30%'
             ELSE '30% 이상'
         END AS discount_range,
         SUM(quantity) AS total_quantity
  FROM orders
  GROUP BY discount_range
  ORDER BY MIN(CASE WHEN discount_at_order IS NULL OR discount_at_order = 0 THEN -1
                    ELSE discount_at_order END);


  -- 할인율별 구매량
  SELECT ROUND(COALESCE(discount_at_order, 0) * 100, 0)::int AS discount_percent,
         SUM(quantity) AS total_quantity
  FROM orders
  GROUP BY discount_percent
  ORDER BY discount_percent;


  -- 가격별 구매량
  SELECT CASE
             WHEN price_at_order < 50000   THEN '5만원 미만'
             WHEN price_at_order < 200000  THEN '5~20만원'
             WHEN price_at_order < 500000  THEN '20~50만원'
             WHEN price_at_order < 1000000 THEN '50~100만원'
             ELSE '100만원 이상'
         END AS price_range,
         SUM(quantity) AS total_quantity
  FROM orders
  GROUP BY price_range
  ORDER BY MIN(price_at_order);


  -- ================================================
  -- 세그먼트 교차 분석
  -- ================================================

  -- 유저 등급별 디바이스 선호도
  SELECT u.user_grade,
         e.device_type,
         ROUND(COUNT(*)::numeric / SUM(COUNT(*)) OVER (PARTITION BY u.user_grade) * 100, 1) AS ratio_percent
  FROM event_logs e
           JOIN users u ON e.user_id = u.user_id
  WHERE e.device_type IS NOT NULL
  GROUP BY u.user_grade, e.device_type
  ORDER BY u.user_grade, e.device_type;