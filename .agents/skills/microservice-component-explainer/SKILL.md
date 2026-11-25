---
name: microservice-component-explainer
description: "Chuyên gia phân tích kiến trúc, giải thích chi tiết cơ chế hoạt động, các thành phần tương tác và cách cấu hình của từng service/thành phần trong hệ thống Microservice (API Gateway, Load Balancing, Eureka, Config Server, Auth Service, User Service, Department Service, Prometheus, Grafana, MySQL). Kích hoạt khi người dùng yêu cầu tìm hiểu, giải thích, phân tích luồng hoặc cấu hình một thành phần cụ thể dựa trên tài liệu hướng dẫn và codebase."
---

# Microservice Component Explainer

Skill này hướng dẫn quy trình phân tích và giải thích chuyên sâu về cơ chế hoạt động, luồng tương tác và cách cấu hình của từng thành phần trong hệ thống Microservice dựa trên tài liệu [huong-dan-microservice-phan-quyen-loadbalancing-monitoring.md](../../../huong-dan-microservice-phan-quyen-loadbalancing-monitoring.md) và codebase thực tế trong thư mục `microservice-demo/`.

---

## 1. Mục đích và Phạm vi (Purpose & Scope)

- **Mục đích:** Cung cấp câu trả lời có cấu trúc chuẩn, chi tiết kỹ thuật sâu, minh họa trực quan bằng sơ đồ Mermaid, trích dẫn file code/config chính xác và phân tích rủi ro hệ thống khi thành phần gặp sự cố.
- **Phạm vi thành phần hỗ trợ:**
  1. **API Gateway:** `api-gateway-service` (Định tuyến, filter JWT, chặn 401 sớm, load balancing cổng vào).
  2. **Load Balancing:** Spring Cloud LoadBalancer (Round-robin giữa `user-service-1` và `user-service-2`, hostname tracking, stateless JWT).
  3. **Config Server:** `config-service` (Quản lý cấu hình tập trung, native config-repo, chia sẻ JWT secret, profile docker vs local).
  4. **Eureka Server:** `eureka-service` (Service discovery, heartbeat, danh bạ dịch vụ).
  5. **Auth Service:** `auth-service` & `mysql-auth` (Xác thực, phân quyền RBAC, cấp/đổi token, token revocation).
  6. **Business Services & Token Relay:** `user-service`, `department-service` (Tự kiểm tra quyền, token relay qua `RestTemplate`).
  7. **Monitoring & Metrics:** `prometheus` & `grafana` (Mô hình pull metric, Micrometer Actuator, dashboard 4701, đếm mã lỗi 401/403).

---

## 2. Khi nào nên dùng (When to Use)

- Người dùng hỏi: *"Hãy giải thích cơ chế hoạt động của [Thành phần]"*.
- Người dùng hỏi: *"Thành phần [X] tương tác với những service nào và luồng dữ liệu đi ra sao?"*.
- Người dùng hỏi: *"Cách cấu hình [Thành phần] trong file yaml/properties và ý nghĩa các tham số là gì?"*.
- Người dùng cần phân tích tình huống lỗi: *"Nếu [Thành phần X] bị sập thì các service khác bị ảnh hưởng như thế nào?"*.

## Khi nào KHÔNG dùng (When NOT to Use)

- Các câu hỏi chung chung về lý thuyết không liên quan đến hệ thống microservice này.
- Yêu cầu viết code mới từ đầu không dựa trên kiến trúc hiện tại.

---

## 3. Quy trình thực hiện 5 bước (Execution Workflow)

Khi nhận câu hỏi về bất kỳ thành phần nào, AI Agent phải thực hiện tuần tự:

### Bước 1: Xác định thành phần và nguồn tham chiếu
- Đọc tài liệu `huong-dan-microservice-phan-quyen-loadbalancing-monitoring.md` tại các mục liên quan đến thành phần đó.
- Kiểm tra các file code và file cấu hình tương ứng trong thư mục `microservice-demo/`:
  - Mã nguồn Java: Controller, Filter, Service, SecurityConfig...
  - Cấu hình: `config-repo/*.yml`, `application.yml`, `docker-compose.yml`, `monitoring/`.

### Bước 2: Lập bản đồ tương tác (Interaction Mapping)
Xác định đầy đủ tất cả các thành phần mà đối tượng đang phân tích có tương tác:
- **Thành phần gọi tới nó (Upstream callers):** Ai gửi request tới nó? (Người dùng, Gateway, Service khác, Prometheus...)
- **Thành phần mà nó gọi tới (Downstream dependencies):** Nó gọi tiếp đi đâu? (Service nội bộ, Database, Config Server, Eureka...)
- **Giao thức & Cổng giao tiếp:** HTTP/REST, TCP, JDBC, Port nội bộ vs Port public.
- **Hợp đồng dữ liệu & Bảo mật:** Header `Authorization: Bearer <token>`, JWT claims, Body JSON, SQL queries.

### Bước 3: Phân tích cơ chế hoạt động & Luồng xử lý (Internal Mechanism & Request Flow)
- Trình bày vòng đời xử lý request từ khi nhận đến khi phản hồi.
- Minh họa bằng sơ đồ Mermaid (Sequence Diagram hoặc Flowchart).
- Giải thích các thuật toán hoặc cơ chế đặc thù (ví dụ: Round-robin, Zero-Trust, Token Relay, Pull metric 5s, Cache/Stateless).

### Bước 4: Trích dẫn Code & Phân tích cấu hình (Code Traces & Configuration)
- Trích dẫn chính xác đường dẫn file, đoạn code hoặc đoạn config then chốt.
- Giải thích **nguyên nhân và mục đích** tại sao tham số đó lại được đặt giá trị như vậy (Rationale).

### Bước 5: Phân tích rủi ro & Kịch bản sự cố (Failure Modes & Impact Analysis)
- Đặt câu hỏi: Nếu thành phần này bị tắt, sập (crash), hoặc quá tải, chuyện gì xảy ra?
- Bán kính ảnh hưởng (Blast Radius): Ảnh hưởng cục bộ hay làm tê liệt toàn hệ thống?
- Mã lỗi trả về cho client (`401`, `403`, `500`, `503` hay Connection Timeout)?
- Biện pháp khắc phục hoặc cơ chế tự phục hồi (Healthcheck, Restart policy, Fallback).

---

## 4. Quy chuẩn định dạng đầu ra (Required Output Format)

> [!IMPORTANT]
> Câu trả lời **BẮT BUỘC** tuân thủ cấu trúc 5 phần dưới đây. **TUYỆT ĐỐI KHÔNG** sử dụng bảng so sánh (theo yêu cầu người dùng). Thay vào đó, tập trung làm rõ **các thành phần tương tác** và **cơ chế kỹ thuật**.

### Cấu trúc câu trả lời:

#### 1. Tổng quan & Vai trò kiến trúc (Architectural Role)
- Tóm tắt ngắn gọn thành phần này là gì trong hệ thống.
- Nhiệm vụ cốt lõi và mục tiêu thiết kế (Single Responsibility).

#### 2. Các thành phần tương tác (Component Interactions)
Liệt kê chi tiết theo danh mục:
- **Upstream (Phía trước gọi vào):** Tên thành phần, giao thức, cổng, kiểu request.
- **Downstream (Phía sau gọi đi):** Tên thành phần, giao thức, cổng, mục đích tương tác.
- **Dữ liệu / Token trao đổi:** Dữ liệu mang theo (header, query, body, JWT claims).

#### 3. Cơ chế hoạt động & Sơ đồ luồng (Mechanisms & Mermaid Diagram)
- Phân tích chi tiết từng bước xử lý logic bên trong.
- Bắt buộc có sơ đồ luồng dạng **Mermaid** (`sequenceDiagram` hoặc `flowchart`).

#### 4. Trích dẫn Code & Giải thích cấu hình (Code Snippets & Config Rationale)
- File cấu hình liên quan (kèm đường dẫn tương đối).
- Đoạn code Java / Cấu hình YAML quan trọng.
- Giải thích ý nghĩa từng tham số chính (Tại sao cổng lại là 8080/8761/8888? Tại sao interval là 5s? Tại sao order là -100?).

#### 5. Kịch bản sự cố & Khả năng chịu lỗi (Failure Scenarios & Resilience)
- Tình huống lỗi thực tế (Service sập, DB ngắt kết nối, token sai...).
- Triệu chứng và mã lỗi hiển thị (Console log, HTTP Status Code, Prometheus Metric).
- Cơ chế phòng thủ hoặc cách khắc phục dựa trên tài liệu (Mục 11 - Lỗi thường gặp).

---

## 5. Giới hạn (Limitations)

- Chỉ áp dụng phân tích trên context của bài lab Spring Boot Microservice này.
- Khi trích dẫn code, ưu tiên trích dẫn các đoạn logic thực tế có trong thư mục `microservice-demo/`.
