# Event Log Pipeline

웹 서비스에서 발생하는 이벤트를 생성하고, 저장하고, 분석하는 파이프라인입니다.

## 실행 방법

docker-compose up --build
```

실행 시 PostgreSQL DB와 Spring Boot 앱이 함께 시작되며, 앱 시작 직후 EventGenerator가 자동으로 1000건의 이벤트를 생성하고 저장합니다.  
이미 데이터가 존재하면 이벤트 생성을 건너뜁니다.

```bash
docker-compose down -v
docker-compose up --build
```

### 주의사항: 로컬 PostgreSQL 포트 충돌

DataGrip으로 데이터를 확인하려는데 데이터가 보이지 않는다면, 로컬에 설치된 PostgreSQL과 Docker PostgreSQL이 같은 `5432` 포트를 사용하여 충돌이 발생한 것일 수 있습니다.  
이때 DataGrip은 Docker DB가 아닌 로컬 DB에 연결되어 있어 Docker에서 생성한 이벤트 데이터가 보이지 않습니다.

**해결 방법 1**: 로컬 PostgreSQL 서비스를 중지하면 DataGrip에서 `localhost:5432`로 Docker DB에 정상적으로 접속할 수 있습니다.

```powershell
# 관리자 권한 PowerShell에서 실행
Stop-Service postgresql*
```

**해결 방법 2**: 서비스를 중지하지 않더라도 아래 명령어로 Docker PostgreSQL에 직접 접속해 쿼리를 실행할 수 있습니다.

```bash
docker exec -it event-log-pipeline-db-1 psql -U event_log_pipeline_user -d event_log_pipeline
```

---

## Step 1. 이벤트 생성기 작성

### 이벤트 타입

| 이벤트 | 설명 |
|---|---|
| `LOGIN` | 사용자 로그인 |
| `PAGE_VIEW` | 상품 상세 페이지 조회 |
| `ORDER_CREATED` | 주문 성공 |
| `ORDER_FAILED` | 주문 실패 (재고 부족) |

### 설계 이유

실제 이커머스 서비스에서 발생하는 사용자 행동 흐름을 기반으로 설계했습니다.

- **LOGIN**: 사용자 활동의 시작점으로, 시간대별 접속 패턴과 디바이스 분포를 분석합니다.
- **PAGE_VIEW**: 상품 조회 후 실제 구매로 이어지는 전환율을 파악하기 위해 포함했습니다.
- **ORDER_CREATED / ORDER_FAILED**: 주문 성공과 실패를 구분하여 재고 부족 같은 운영 이슈를 탐지합니다. 실패 이벤트는 트랜잭션이 롤백되더라도 독립적으로 기록됩니다(`Propagation.REQUIRES_NEW`).

### 이벤트 생성 방식

Java로 작성된 `EventGenerator`가 앱 시작 시 1000건의 이벤트를 REST API HTTP 요청으로 생성합니다. 직접 서비스를 호출하지 않고 REST API를 통해 이벤트를 생성함으로써 실제 서비스 흐름 전체(직렬화, 유효성 검사, 라우팅)를 검증합니다.

```
EventGenerator → POST /users/login      → LOGIN 이벤트 기록
              → GET  /products/{id}    → PAGE_VIEW 이벤트 기록
              → POST /orders           → ORDER_CREATED 또는 ORDER_FAILED 이벤트 기록
```

### 초기 데이터

- 유저 10명: BRONZE 4명, SILVER 3명, GOLD 3명
- 상품 8개: 전자기기 5개 (노트북, 스마트폰, 이어폰, 태블릿, 스마트워치), 의류 3개 (티셔츠, 청바지, 패딩)
- 디바이스 타입: MOBILE, DESKTOP
- 주문 수량: 1 ~ 30개 (랜덤)
- 이벤트 시각: 최근 30일 이내 랜덤

---

## Step 2. 로그 저장

### ERD

![ERD](images/erd.png)

### 저장소 선택: PostgreSQL

**선택 이유**

이벤트 로그는 사용자, 상품, 주문과 **관계**를 가지는 정형 데이터입니다. 관계형 DB가 가장 적합합니다.  
시간대별, 유저별, 디바이스별 집계와 같은 **복잡한 분석 쿼리**를 SQL로 직관적으로 작성할 수 있으며, 인덱스를 활용한 빠른 조회가 가능합니다.

### 스키마

**event_logs**

| 컬럼 | 타입 | 설명 | 인덱스 |
|---|---|---|---|
| event_id | UUID | PK | |
| user_id | UUID | FK → users (NOT NULL) | 유저별 이벤트 조회 및 집계 |
| event_type | VARCHAR(50) | LOGIN / PAGE_VIEW / ORDER_CREATED / ORDER_FAILED | 이벤트 타입별 필터링 및 집계 |
| event_time | TIMESTAMP | 이벤트 발생 시각 | 시간대별, 요일별 범위 조회 |
| product_id | UUID | FK → products (nullable) | 상품별 조회수 대비 구매 비율 분석 |
| device_type | VARCHAR(20) | MOBILE / DESKTOP | |
| is_succeeded | BOOLEAN | 이벤트 성공 여부 (기본값 true) | |
| failed_reason | VARCHAR(20) | 실패 원인 (nullable) | |

**users**

| 컬럼 | 타입 | 설명 |
|---|---|---|
| user_id | UUID | PK |
| user_grade | VARCHAR(10) | BRONZE / SILVER / GOLD |
| created_at | TIMESTAMP | 가입 시각 |

**products**

| 컬럼 | 타입 | 설명 | 인덱스 |
|---|---|---|---|
| product_id | UUID | PK | |
| product_name | VARCHAR(255) | 상품명 | |
| category_id | UUID | FK → categories | 카테고리별 상품 조회 |
| price | INTEGER | 정가 | 가격 범위 필터링 |
| discount | DECIMAL(4,3) | 할인율 | 할인율 기준 정렬 및 필터링 |
| stock | INTEGER | 재고 수량 (NOT NULL) | |

**orders**

| 컬럼 | 타입 | 설명 | 인덱스 |
|---|---|---|---|
| order_id | UUID | PK | |
| user_id | UUID | FK → users (NOT NULL) | 유저별 주문 내역 조회 및 재구매 유저 분석 |
| product_id | UUID | FK → products (NOT NULL) | 상품별 주문 수량 집계 |
| quantity | INTEGER | 주문 수량 (NOT NULL) | |
| price_at_order | DECIMAL(10,2) | 주문 시점 가격 | |
| discount_at_order | DECIMAL(4,3) | 주문 시점 할인율 | |
| created_at | TIMESTAMP | 주문 시각 | |

**categories**

| 컬럼 | 타입 | 설명 |
|---|---|---|
| category_id | UUID | PK |
| category_name | VARCHAR(255) | 카테고리명 |

---

## Step 3. 데이터 집계 분석

`analytics.sql` 파일에 10개의 분석 쿼리를 작성했습니다. r

| 번호 | 분석 항목 |
|---|---|
| 1 | 이벤트 타입별 발생 횟수 |
| 2 | 정상 / 실패 이벤트 비율 |
| 3 | 상품별 조회수 대비 구매 비율 |
| 4 | 시간대별 이벤트 발생량 및 주문 비율 |
| 5 | 요일별 주문율 |
| 6 | 디바이스 타입별 주문율 |
| 7 | 주문 실패 원인별 비율 |
| 8 | 유저별 총 이벤트 수 |
| 9 | 재구매 유저 비율 |
| 10 | 등급별 구매력 분석 |
| 11 | 카테고리별 판매량 |

---

## Step 5. 결과 시각화

**전체 이벤트 중 성공과 실패 비율**
전체 이벤트 중 정상 처리된 이벤트와 실패한 이벤트의 비율을 나타냅니다.

![전체 이벤트 중 성공과 실패 비율](images/전체%20이벤트%20중%20성공과%20실패%20비율.png)

**시간대별 이벤트량**
하루 중 어느 시간대에 이벤트가 집중되는지 파악합니다. 서비스 피크 타임을 확인할 수 있습니다.

![시간대별 이벤트량](images/시간대별%20이벤트량.png)

**요일별 주문량**
요일별로 주문이 얼마나 발생하는지 비교합니다. 마케팅 전략 수립에 활용할 수 있습니다.

![요일별 주문량](images/요일별%20주문량.png)

**조회수 대비 구매율**
상품별로 페이지 조회 후 실제 구매로 이어진 전환율을 보여줍니다.

![조회수 대비 구매율](images/조회수%20대비%20구매율.png)

**카테고리별 판매량**
카테고리별 총 주문 수량을 비교하여 어떤 카테고리가 가장 많이 판매되는지 파악합니다.

![카테고리별 판매량](images/카테고리별%20판매량.png)

---