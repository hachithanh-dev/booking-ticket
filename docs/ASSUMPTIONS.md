# Assumptions, What I Have Done & What I Have Not Done

> Tài liệu mô tả các giả định, phạm vi đã triển khai, và những giới hạn của hệ thống đặt vé concert.

---

## Mục Lục

1. [Assumptions (Giả Định)](#1-assumptions-giả-định)
2. [What I Have Done (Đã Triển Khai)](#2-what-i-have-done-đã-triển-khai)
3. [What I Have Not Done (Chưa Triển Khai / Giới Hạn)](#3-what-i-have-not-done-chưa-triển-khai--giới-hạn)

---

## 1. Assumptions (Giả Định)

### 1.1 Vòng Đời Booking (5 Trạng Thái)

Tôi giả định rằng một booking sẽ trải qua **5 trạng thái**, nhiều hơn 4 trạng thái tiêu chuẩn vì cần phân biệt rõ ràng giữa "hết hạn tự động" và "thanh toán thất bại":

```
                ┌──────────────┐
                │   PENDING    │ ← Vừa đặt vé, chờ thanh toán (TTL: 15 phút)
                └──────┬───────┘
                       │
          ┌────────────┼─────────────┬──────────────────┐
          ▼            ▼             ▼                  ▼
   ┌──────────┐  ┌──────────┐  ┌──────────┐     ┌──────────┐
   │CONFIRMED │  │  FAILED  │  │ EXPIRED  │     │CANCELLED │
   │(Đã TT)   │  │(TT thất │  │(Hết hạn  │     │(Admin    │
   │          │  │ bại)     │  │ 15 phút) │     │ hủy)     │
   └──────────┘  └──────────┘  └──────────┘     └──────────┘
```

| Trạng thái | Chuyển từ | Trigger | Hành vi |
|------------|-----------|---------|---------|
| `PENDING` | — | User đặt vé | Vé bị trừ, booking hết hạn sau 15 phút |
| `CONFIRMED` | PENDING | Payment webhook `SUCCESS` | Đánh dấu đã thanh toán. Vé giữ nguyên. |
| `FAILED` | PENDING | Payment webhook `FAILED` | **Hoàn vé** (tăng `available_quantity`). Voucher **KHÔNG** hoàn. |
| `EXPIRED` | PENDING | Scheduler (mỗi 60 giây) | **Hoàn vé**. Voucher **KHÔNG** hoàn. |
| `CANCELLED` | PENDING, CONFIRMED | Admin hủy | **Hoàn vé**. Voucher **KHÔNG** hoàn. |

**Tại sao 5 thay vì 4?**
- `FAILED` vs `EXPIRED` tuy đều hoàn vé, nhưng nguyên nhân khác nhau: FAILED là do payment gateway báo thất bại, EXPIRED là do user không thanh toán trong 15 phút. Phân biệt giúp analytics và customer support xử lý chính xác hơn.
- `CANCELLED` cần tách riêng vì chỉ Admin mới có quyền hủy, và có thể hủy cả booking đã `CONFIRMED` (ví dụ: hoàn tiền, sự kiện bị hủy).

### 1.2 Xác Thực (Authentication)

Tôi giả định hệ thống **KHÔNG** tích hợp JWT/OAuth2 thực tế. Thay vào đó, sử dụng HTTP headers đơn giản để mô phỏng:

- `X-User-Id: {UUID}` — xác định user hiện tại (thay thế cho JWT token)
- `X-Role: ADMIN` — xác định quyền admin (thay thế cho role-based access control)
- `X-Idempotency-Key: {string}` — client-generated key chống request trùng

**Lý do**: Đây là bài assessment tập trung vào backend logic (concurrency, caching, database design), không phải authentication system. Header-based approach cho phép test API dễ dàng qua Apidog/Postman mà không cần setup OAuth2 server.

### 1.3 Thanh Toán (Payment)

Tôi giả định hệ thống **KHÔNG** tích hợp payment gateway thực tế (Stripe, VNPay, MoMo...). Thay vào đó:

- Cung cấp **Webhook endpoint** (`POST /api/v1/webhooks/payments`) để nhận kết quả thanh toán
- Payment gateway bên ngoài gọi webhook này khi thanh toán hoàn tất
- Webhook nhận `bookingId` + `status` (SUCCESS/FAILED) và xử lý tương ứng

**Luồng thực tế sẽ là**: User đặt vé → nhận `bookingId` → frontend redirect đến payment gateway → gateway xử lý → gateway gọi webhook → hệ thống cập nhật booking.

### 1.4 Voucher

Tôi giả định:

- **Voucher được seed sẵn trong database** (qua Flyway migration hoặc SQL thủ công). Hệ thống **KHÔNG** cung cấp API tạo/sửa/xóa voucher cho operation team.
- **Mỗi user chỉ dùng voucher 1 lần** — enforce bằng `UNIQUE(voucher_id, user_id)` trong bảng `voucher_redemptions`.
- **Voucher KHÔNG được hoàn lại** khi booking bị hủy/hết hạn/thanh toán thất bại. Lý do: ngăn vòng lặp lạm dụng voucher (đặt → hủy → đặt lại → vô hạn).
- Hỗ trợ 2 loại voucher: `FIXED_AMOUNT` (giảm cố định, VD: 50.000 VND) và `PERCENTAGE` (giảm %, có `max_discount_amount` cap).

### 1.5 User

Tôi giả định:

- **User được seed sẵn trong database**. Hệ thống **KHÔNG** cung cấp API đăng ký/đăng nhập.
- Mỗi user được định danh bằng UUID, truyền qua header `X-User-Id`.

### 1.6 Concert & Ticket

Tôi giả định:

- **Concert và Ticket Category được seed sẵn**. Hệ thống **KHÔNG** cung cấp API CRUD cho concert/ticket (tạo/sửa/xóa).
- Mỗi concert có nhiều loại vé (VIP, Standard, Economy...), mỗi loại có giá riêng và số lượng giới hạn.
- Concert chỉ hiển thị cho user khi `status = PUBLISHED`.

### 1.7 Đơn Vị Tiền Tệ

Tôi giả định hệ thống chỉ hỗ trợ **VND (Việt Nam Đồng)**:
- VND không có phần thập phân → sử dụng `BIGINT` / `Long` thay vì `BigDecimal`.
- 500.000 VNĐ được lưu là `500000`.
- Không hỗ trợ đa tiền tệ hay chuyển đổi tỷ giá.

### 1.8 Concurrency & Scale

Tôi giả định:

- Hệ thống chạy trên **1 instance duy nhất** (single node). Các chiến lược concurrency control (Redis lock, DB lock) vẫn hoạt động đúng trên multi-instance, nhưng chưa được test ở scale đó.
- **Redis là single instance** (không cluster). Redisson lock hoạt động trên single Redis node.
- Target: ~1000 concurrent bookings là đủ cho scale assessment.

---

## 2. What I Have Done (Đã Triển Khai)

### 2.1 API Endpoints

| # | Endpoint | Method | Mô tả | Trạng thái |
|---|----------|--------|-------|-----------|
| 1 | `/api/v1/concerts` | GET | Danh sách sự kiện (có phân trang) | ✅ Hoàn thành |
| 2 | `/api/v1/concerts/{id}` | GET | Chi tiết sự kiện (có Redis cache + DCL) | ✅ Hoàn thành |
| 3 | `/api/v1/tickets/{id}` | GET | Chi tiết loại vé (split cache: static + dynamic) | ✅ Hoàn thành |
| 4 | `/api/v1/bookings/reserve` | POST | Đặt vé (3-zone pattern, idempotency, atomic decrement) | ✅ Hoàn thành |
| 5 | `/api/v1/bookings/{id}` | GET | Xem chi tiết booking (chỉ owner) | ✅ Hoàn thành |
| 6 | `/api/v1/admin/bookings` | GET | Admin xem danh sách booking (filter, phân trang) | ✅ Hoàn thành |
| 7 | `/api/v1/admin/bookings/{id}/status` | PATCH | Admin hủy booking | ✅ Hoàn thành |
| 8 | `/api/v1/webhooks/payments` | POST | Webhook nhận kết quả thanh toán | ✅ Hoàn thành |

### 2.2 Concurrency & Data Safety

| Feature | Mô tả | Chi tiết |
|---------|-------|----------|
| ✅ **Chống overselling** | Atomic SQL `UPDATE SET qty -= N WHERE qty >= N` | DB CHECK constraint `available_quantity >= 0` là safety net |
| ✅ **Idempotency 3 lớp** | Redis SETNX → Business Fingerprint → Client Request ID | Chống click nhanh, content trùng, và network retry |
| ✅ **Race condition payment/expire** | `SELECT FOR UPDATE` (Pessimistic Locking) | Serialize quyền truy cập, chỉ 1 thread xử lý booking tại 1 thời điểm |
| ✅ **Voucher double-use** | Atomic increment SQL + UNIQUE constraint | 1 user / 1 voucher, enforce ở tầng DB |
| ✅ **Cache stampede** | Redisson DCL (Double-Check Locking) | Chỉ 1 thread query DB khi cache miss |

### 2.3 Caching

| Cache | Key Pattern | TTL | Invalidation |
|-------|-------------|-----|-------------|
| ✅ Concert detail | `concert:detail:{id}` | 600s | TTL-based |
| ✅ Ticket static | `ticket:static:{id}` | 600s | TTL-based |
| ✅ Ticket dynamic | `ticket:qty:{id}` | 600s | Event-driven (`AFTER_COMMIT` DEL) |
| ✅ Idempotency lock | `booking:lock:{userId}:{key}` | 30s | Auto-expire |

### 2.4 Batch Expiry Scheduler

| Feature | Mô tả |
|---------|-------|
| ✅ **Tự động hết hạn** | Scheduler chạy mỗi 60 giây, tìm booking PENDING quá 15 phút |
| ✅ **Batch processing** | Xử lý theo chunk 20 booking/transaction thay vì từng booking một |
| ✅ **Aggregate ticket restore** | Gộp số lượng vé cần hoàn theo loại → 1 UPDATE per loại vé (thay vì N UPDATE) |
| ✅ **Cache invalidation** | Sau batch expire, invalidate ticket dynamic cache qua event |

### 2.5 Database

| Feature | Mô tả |
|---------|-------|
| ✅ **Flyway migration** | Schema quản lý bằng Flyway (`V1__init_schema.sql`), `ddl-auto: validate` |
| ✅ **CHECK constraints** | DB validate: `total = subtotal - discount`, `qty >= 0`, `used_count <= max` |
| ✅ **Partial unique index** | Chống booking trùng fingerprint chỉ khi PENDING |
| ✅ **Filtered index** | Scheduler tìm booking hết hạn nhanh (chỉ index PENDING rows) |
| ✅ **N+1 query fix** | `JOIN FETCH` / `@EntityGraph` cho tất cả query có relationship |

### 2.6 Documentation & Deliverables

| Deliverable | Mô tả |
|-------------|-------|
| ✅ **README.md** | Hướng dẫn setup, architecture overview, design rationale |
| ✅ **SYSTEM_DESIGN.md** | Tài liệu thiết kế chi tiết (tiếng Việt) với ER diagram, sequence diagrams, phân tích |
| ✅ **ASSUMPTIONS.md** | Tài liệu này |
| ✅ **apidog_collection.json** | OpenAPI 3.0 spec, import vào Apidog để test API |
| ✅ **Swagger UI** | Tự động generate tại `/swagger-ui.html` |
| ✅ **Unit Tests** | 23 test cases cho `BookingTxService` (tất cả PASS) |

### 2.7 Infrastructure

| Feature | Mô tả |
|---------|-------|
| ✅ **Docker Compose** | PostgreSQL 17 + Redis 8, sẵn sàng `docker compose up` |
| ✅ **Virtual Threads** | Java 21, tận dụng virtual threads cho I/O-bound workload |
| ✅ **Graceful Shutdown** | Server chờ 30s cho request đang xử lý trước khi shutdown |
| ✅ **HikariCP tuning** | `auto-commit: false`, leak detection 30s, pool size 20 |
| ✅ **open-in-view: false** | Tắt để phát hiện N+1 sớm, không lazy load ngoài transaction |

---

## 3. What I Have Not Done (Chưa Triển Khai / Giới Hạn)

### 3.1 APIs Chưa Có

| API | Lý do không triển khai |
|-----|----------------------|
| ❌ **CRUD User** (đăng ký, đăng nhập, cập nhật profile) | Bài assessment tập trung vào booking logic, không phải user management. User được seed sẵn. |
| ❌ **CRUD Concert** (tạo, sửa, xóa sự kiện) | Tương tự — concert là dữ liệu tĩnh, seed sẵn qua SQL. Hệ thống tập trung vào read + booking. |
| ❌ **CRUD Ticket Category** (tạo, sửa loại vé) | Ticket categories gắn với concert, được tạo cùng lúc. Không cần API riêng. |
| ❌ **CRUD Voucher** (tạo, sửa, xóa voucher cho operation team) | Voucher được seed sẵn. Hệ thống đảm bảo customer có thể apply voucher khi đặt vé, nhưng không cung cấp dashboard quản lý voucher. |
| ❌ **User xem lịch sử booking** (`GET /api/v1/bookings?userId=...`) | Có thể mở rộng từ `BookingRepository` hiện tại. Chưa triển khai vì tập trung vào core booking flow. |
| ❌ **User tự hủy booking** (`DELETE /api/v1/bookings/{id}`) | Hiện chỉ Admin mới hủy được. Có thể mở rộng cho user tự hủy booking PENDING. |

### 3.2 Features Chưa Có

| Feature | Mô tả | Lý do |
|---------|-------|-------|
| ❌ **JWT / OAuth2 Authentication** | Xác thực thực tế (login, token refresh, role management) | Ngoài scope assessment. Sử dụng header-based auth đơn giản thay thế. |
| ❌ **Payment Gateway Integration** | Tích hợp VNPay/MoMo/Stripe thực tế | Ngoài scope. Cung cấp webhook endpoint thay thế. |
| ❌ **Email / SMS Notification** | Gửi email xác nhận booking, nhắc thanh toán | Ngoài scope. Có thể thêm bằng `@Async` + `@TransactionalEventListener`. |
| ❌ **Seat Selection** (chọn ghế) | User chọn ghế cụ thể thay vì chỉ chọn loại vé | Complexity cao, cần data model khác (seat map). Ngoài scope. |
| ❌ **Waiting Queue / Waitlist** | Xếp hàng khi vé hết, tự động notify khi có vé | Cần message queue (RabbitMQ/Kafka). Ngoài scope. |
| ❌ **Multi-currency** | Hỗ trợ nhiều loại tiền tệ | Hệ thống chỉ phục vụ thị trường VN, VND là đủ. |
| ❌ **Refund Logic** | Hoàn tiền khi admin hủy booking CONFIRMED | Cần tích hợp payment gateway. Hiện chỉ hủy booking + hoàn vé. |
| ❌ **Rate Limiting** | Giới hạn số request/giây cho mỗi user/IP | Nên triển khai ở API Gateway hoặc dùng Bucket4j. Chưa triển khai. |
| ❌ **Audit Log** | Ghi lại mọi thao tác (ai làm gì, lúc nào) | `created_at` / `updated_at` có sẵn, nhưng chưa có audit table riêng. |
| ❌ **Caching cho Concert List** | Cache danh sách concert (hiện chỉ cache detail) | Danh sách thay đổi ít, có thể thêm sau. Ưu tiên cache cho detail + ticket. |
| ❌ **Webhook Security** (HMAC signature verification) | Xác thực webhook đến từ payment gateway hợp lệ | Ngoài scope. Production cần verify HMAC signature. |
| ❌ **Admin Dashboard UI** | Giao diện web cho admin quản lý | Backend-only assessment. API sẵn sàng, frontend chưa triển khai. |

### 3.3 Testing Chưa Đầy Đủ

| Loại test | Trạng thái | Chi tiết |
|-----------|-----------|----------|
| ✅ Unit Test (BookingTxService) | 23 tests PASS | Cover: reservation, payment, cancel, expire, batch expire, voucher, edge cases |
| ❌ Unit Test (các service khác) | Chưa triển khai | ConcertService, ConcertCacheService, TicketCategoryCacheService, BookingService |
| ❌ Integration Test | Chưa triển khai | Test với DB + Redis thật (Testcontainers). Quan trọng cho verify SQL queries, cache behavior. |
| ❌ Controller Test (MockMvc) | Chưa triển khai | Test HTTP layer: request validation, response format, status codes |
| ❌ Concurrency Test | Chưa triển khai | Test với nhiều thread đồng thời để verify không overselling, không duplicate booking |
| ❌ Performance / Load Test | Chưa triển khai | JMeter/k6 test với 1000 concurrent users |

### 3.4 Giới Hạn Kiến Trúc

| Giới hạn | Ảnh hưởng | Hướng khắc phục |
|----------|-----------|-----------------|
| **Single instance** | Không horizontal scale. Scheduler chạy trên 1 node → nếu node chết, booking không tự expire. | Dùng ShedLock hoặc Quartz cluster cho distributed scheduling. |
| **Redis single node** | Nếu Redis chết, idempotency lock và cache mất → tăng DB load. Booking vẫn hoạt động nhờ DB constraints. | Redis Sentinel hoặc Redis Cluster cho HA. |
| **Synchronous webhook** | Webhook xử lý đồng bộ. Nếu DB chậm, payment gateway có thể timeout và retry. | Async processing với message queue. |
| **No dead letter queue** | Nếu cache invalidation fail liên tục, stale cache tồn tại đến khi TTL hết (max 600s). | Retry queue hoặc scheduled cache refresh. |
| **No monitoring** | Không có Prometheus metrics, Grafana dashboards, hay alerting. | Spring Boot Actuator + Micrometer + Prometheus. |

---

## Tổng Kết

| Hạng mục | Đã làm | Chưa làm |
|----------|--------|----------|
| **Core Booking Flow** | ✅ Đầy đủ (reserve, payment, cancel, expire) | — |
| **Concurrency Safety** | ✅ Đầy đủ (overselling, duplicate, race condition) | — |
| **Caching** | ✅ Đầy đủ (DCL, split cache, event invalidation) | ❌ Cache concert list |
| **Database Design** | ✅ Đầy đủ (3NF, constraints, indexes) | — |
| **API Endpoints** | ✅ 8 endpoints | ❌ CRUD user/concert/voucher, user history |
| **Authentication** | ✅ Header-based (đủ cho assessment) | ❌ JWT/OAuth2 |
| **Testing** | ✅ 23 unit tests | ❌ Integration, controller, concurrency tests |
| **Documentation** | ✅ README, System Design, Assumptions, Swagger, Apidog | — |
| **Infrastructure** | ✅ Docker Compose, Flyway, Virtual Threads | ❌ Monitoring, CI/CD |

Hệ thống ưu tiên **chiều sâu hơn chiều rộng**: tập trung giải quyết triệt để các vấn đề khó (concurrency, caching, data consistency) thay vì triển khai nhiều CRUD đơn giản. Các tính năng chưa triển khai đều có thể mở rộng dựa trên foundation hiện tại.
