# Đặc tả thiết kế cơ sở dữ liệu
## AI Learning Platform

## 0. Các chỉnh sửa so với bản thiết kế ban đầu

| Thành phần chỉnh sửa | Nội dung chỉnh sửa | Lý do chỉnh sửa |
|---|---|---|
| Định dạng Markdown | Chuyển các đoạn bảng dạng tab thành bảng Markdown, chuẩn hóa heading theo cấp `#`, `##`, `###`, `####`. | File gốc có nhiều bảng dạng văn bản thô nên khó đọc và dễ lỗi khi đưa vào báo cáo. |
| Khối mô tả schema PostgreSQL | Đóng lại khối code sau cây schema và tách bảng mapping schema/service ra khỏi code block. | Trong bản gốc, code block bị mở nhưng chưa đóng, làm toàn bộ nội dung phía sau có nguy cơ bị hiển thị sai. |
| `core.course_access_codes.expired_at` | Đổi thành `expires_at`. | Thống nhất quy ước đặt tên với `system.refresh_tokens.expires_at` và diễn đạt đúng ý nghĩa “thời điểm hết hạn”. |
| `ai_engine.document_chunks` | Bổ sung `document_version_id` để tham chiếu logic đến `core.document_versions.id`. | Khi tài liệu có nhiều phiên bản, chunk cần gắn với đúng phiên bản được index để hỗ trợ re-index, truy vết và rollback. |
| `ai_engine.document_chunks` | Bổ sung `status` với các giá trị đề xuất `ACTIVE`, `ARCHIVED`, `DELETED`. | Khi tài liệu bị cập nhật/xóa mềm, hệ thống cần biết chunk nào còn được dùng cho RAG mà không phải join trực tiếp sang schema `core`. |
| Ràng buộc khóa chính/duy nhất | Bổ sung nhóm ràng buộc đề xuất cho các bảng trung gian và bảng có thứ tự như `user_roles`, `role_permissions`, `course_members`, `course_chapters`, `course_topics`, `document_versions`. | Tránh dữ liệu trùng lặp và giữ tính toàn vẹn dữ liệu trong từng schema. |
| Ràng buộc RAG | Cập nhật ví dụ truy vấn RAG để lọc thêm `status = 'ACTIVE'`. | Đảm bảo AI Tutor chỉ sử dụng các chunk còn hiệu lực trong phạm vi môn học hiện tại. |
| Quan hệ xuyên schema | Làm rõ quan hệ xuyên schema là tham chiếu logic; foreign key vật lý chỉ nên dùng trong cùng schema. | Phù hợp nguyên tắc microservices và service ownership đã thống nhất. |

> Ghi chú: Tài liệu này được biên tập lại từ bản thiết kế cơ sở dữ liệu ban đầu do người dùng cung cấp, đồng thời chỉnh sửa một số điểm để phù hợp hơn với kiến trúc microservices, RAG theo phạm vi môn học và nguyên tắc service ownership.

## 1. Tổng quan thiết kế cơ sở dữ liệu

Hệ thống sử dụng một PostgreSQL database duy nhất nhưng chia thành nhiều schema theo từng nhóm microservice. Cách thiết kế này giúp hệ thống vẫn giữ được tính tách biệt dữ liệu theo hướng microservices, đồng thời giảm độ phức tạp triển khai trong phạm vi đồ án.

```text
PostgreSQL Database: ai_learning_platform

├── system
├── core
├── ai_app
└── ai_engine
```

| Schema | Service sở hữu | Vai trò |
| --- | --- | --- |
| system | System Service | Quản lý tài khoản, vai trò, quyền, xác thực, refresh token |
| core | Core Service | Quản lý môn học, thành viên môn học, lộ trình môn học, tài liệu |
| ai_app | AI Orchestrator Service | Quản lý chat, learning plan, quiz, tiến độ, khuyến nghị |
| ai_engine | Python AI Engine Service | Quản lý chunk, embedding, RAG, prompt, vector search |

## 2. Nguyên tắc thiết kế
### 2.1. Service ownership

Mỗi service chỉ được ghi dữ liệu vào schema mà nó sở hữu.

| Service | Schema được ghi |
| --- | --- |
| System Service | system |
| Core Service | core |
| AI Orchestrator Service | ai_app |
| Python AI Engine Service | ai_engine |

Ví dụ, Python AI Engine không cập nhật trực tiếp trạng thái trong bảng core.documents. Khi xử lý tài liệu xong, Python AI Engine phát event document.indexed hoặc document.index_failed, sau đó Core Service cập nhật trạng thái tài liệu.

### 2.2. Không dùng quan hệ entity trực tiếp xuyên service

Các bảng khác schema có thể tham chiếu nhau bằng ID, nhưng không nên tạo quan hệ entity trực tiếp trong code.

Ví dụ trong bảng ai_app.learning_plans có:

student_id
course_id

Hai trường này lần lượt tham chiếu logic đến:

system.users.id
core.courses.id

Tuy nhiên trong code không nên ánh xạ trực tiếp kiểu:

@ManyToOne
private User student;

@ManyToOne
private Course course;

Thay vào đó, chỉ lưu ID và khi cần thông tin chi tiết thì gọi API sang service sở hữu dữ liệu.

### 2.3. Quy ước đặt tên

| Thành phần | Quy ước |
| --- | --- |
| Tên bảng | snake_case, danh từ số nhiều |
| Tên cột | snake_case |
| Khóa chính | id |
| Khóa tham chiếu | {entity}_id |
| Thời gian tạo | created_at |
| Thời gian cập nhật | updated_at |
| Trạng thái | status |
| Dữ liệu linh hoạt | metadata kiểu jsonb |

## 3. Schema system
### 3.1. Ý nghĩa schema

Schema system do System Service sở hữu. Schema này lưu dữ liệu nền tảng của hệ thống như tài khoản, vai trò, quyền, refresh token và log hệ thống.

System Service chịu trách nhiệm:

Đăng ký tài khoản.
Đăng nhập.
Cấp JWT.
Refresh token.
Quản lý người dùng.
Quản lý vai trò.
Quản lý quyền.
Khóa hoặc mở khóa tài khoản.
Ghi log hoạt động hệ thống.
### 3.2. Danh sách bảng

| Bảng | Mô tả |
| --- | --- |
| system.users | Lưu thông tin tài khoản người dùng |
| system.roles | Lưu danh sách vai trò |
| system.permissions | Lưu danh sách quyền |
| system.user_roles | Bảng trung gian giữa người dùng và vai trò |
| system.role_permissions | Bảng trung gian giữa vai trò và quyền |
| system.refresh_tokens | Lưu refresh token |
| system.audit_logs | Lưu lịch sử thao tác quan trọng |
| system.login_history | Lưu lịch sử đăng nhập |

### 3.3. Bảng system.users
#### Mô tả

Bảng users lưu thông tin tài khoản của tất cả người dùng trong hệ thống, bao gồm sinh viên, giảng viên và quản trị viên.

#### Ý nghĩa

Đây là bảng trung tâm của hệ thống xác thực. Các service khác không lưu thông tin chi tiết người dùng mà chỉ lưu user_id để tham chiếu logic.

#### Các trường dữ liệu

| Trường | Kiểu dữ liệu | Bắt buộc | Mô tả |
| --- | --- | --- | --- |
| id | UUID | Có | Khóa chính của người dùng |
| email | VARCHAR(255) | Có | Email đăng nhập, duy nhất |
| username | VARCHAR(100) | Không | Tên đăng nhập tùy chọn |
| password_hash | VARCHAR(255) | Có | Mật khẩu đã mã hóa |
| full_name | VARCHAR(255) | Có | Họ tên đầy đủ |
| avatar_url | TEXT | Không | Đường dẫn ảnh đại diện |
| phone | VARCHAR(20) | Không | Số điện thoại |
| status | VARCHAR(30) | Có | Trạng thái tài khoản |
| token_version | INTEGER | Có | Phiên bản token dùng để vô hiệu hóa token cũ |
| created_at | TIMESTAMP | Có | Thời điểm tạo |
| updated_at | TIMESTAMP | Có | Thời điểm cập nhật |
| deleted_at | TIMESTAMP | Không | Thời điểm xóa mềm |

#### Giá trị đề xuất cho status

| Giá trị | Ý nghĩa |
| --- | --- |
| ACTIVE | Tài khoản đang hoạt động |
| INACTIVE | Tài khoản chưa kích hoạt |
| LOCKED | Tài khoản bị khóa |
| DELETED | Tài khoản đã bị xóa mềm |

#### Ràng buộc

| Ràng buộc | Mô tả |
| --- | --- |
| Primary key | id |
| Unique | email |
| Unique | username, nếu sử dụng |
| Index | status |

### 3.4. Bảng system.roles
#### Mô tả

Bảng roles lưu danh sách vai trò của hệ thống.

#### Ý nghĩa

Vai trò đại diện cho nhóm quyền của người dùng. Một người dùng có thể có một hoặc nhiều vai trò.

#### Các trường dữ liệu

| Trường | Kiểu dữ liệu | Bắt buộc | Mô tả |
| --- | --- | --- | --- |
| id | UUID | Có | Khóa chính |
| code | VARCHAR(100) | Có | Mã vai trò |
| name | VARCHAR(255) | Có | Tên vai trò |
| description | TEXT | Không | Mô tả vai trò |
| created_at | TIMESTAMP | Có | Thời điểm tạo |
| updated_at | TIMESTAMP | Có | Thời điểm cập nhật |

#### Dữ liệu khởi tạo đề xuất

| Code | Ý nghĩa |
| --- | --- |
| STUDENT | Sinh viên |
| LECTURER | Giảng viên |
| ADMIN | Quản trị viên |

### 3.5. Bảng system.permissions
#### Mô tả

Bảng permissions lưu danh sách quyền chi tiết trong hệ thống.

#### Ý nghĩa

Permission đại diện cho một hành động cụ thể mà backend dùng để kiểm tra quyền truy cập API.

#### Các trường dữ liệu

| Trường | Kiểu dữ liệu | Bắt buộc | Mô tả |
| --- | --- | --- | --- |
| id | UUID | Có | Khóa chính |
| code | VARCHAR(150) | Có | Mã quyền |
| name | VARCHAR(255) | Có | Tên quyền |
| description | TEXT | Không | Mô tả |
| created_at | TIMESTAMP | Có | Thời điểm tạo |

#### Ví dụ permission

| Code | Ý nghĩa |
| --- | --- |
| USER_MANAGE | Quản lý người dùng |
| ROLE_MANAGE | Quản lý vai trò và quyền |
| COURSE_MANAGE | Quản lý môn học |
| COURSE_JOIN | Tham gia môn học |
| DOCUMENT_UPLOAD | Upload tài liệu |
| DOCUMENT_MANAGE | Quản lý tài liệu |
| AI_CHAT | Chat với gia sư AI |
| QUIZ_TAKE | Làm bài luyện tập |
| PROGRESS_VIEW | Xem tiến độ học tập |

### 3.6. Bảng system.user_roles
#### Mô tả

Bảng trung gian biểu diễn quan hệ nhiều-nhiều giữa người dùng và vai trò.

#### Các trường dữ liệu

| Trường | Kiểu dữ liệu | Bắt buộc | Mô tả |
| --- | --- | --- | --- |
| user_id | UUID | Có | ID người dùng |
| role_id | UUID | Có | ID vai trò |
| assigned_at | TIMESTAMP | Có | Thời điểm gán vai trò |
| assigned_by | UUID | Không | Người gán vai trò |

#### Quan hệ
system.users 1 --- N system.user_roles
system.roles 1 --- N system.user_roles
### 3.7. Bảng system.role_permissions
#### Mô tả

Bảng trung gian biểu diễn quan hệ nhiều-nhiều giữa vai trò và quyền.

#### Các trường dữ liệu

| Trường | Kiểu dữ liệu | Bắt buộc | Mô tả |
| --- | --- | --- | --- |
| role_id | UUID | Có | ID vai trò |
| permission_id | UUID | Có | ID quyền |
| created_at | TIMESTAMP | Có | Thời điểm gán quyền |

#### Quan hệ
system.roles 1 --- N system.role_permissions
system.permissions 1 --- N system.role_permissions
### 3.8. Bảng system.refresh_tokens
#### Mô tả

Bảng refresh_tokens lưu refresh token đã cấp cho người dùng.

#### Ý nghĩa

Access token có thời gian sống ngắn. Refresh token dùng để xin access token mới khi access token hết hạn.

#### Các trường dữ liệu

| Trường | Kiểu dữ liệu | Bắt buộc | Mô tả |
| --- | --- | --- | --- |
| id | UUID | Có | Khóa chính |
| user_id | UUID | Có | Người dùng sở hữu token |
| token_hash | VARCHAR(255) | Có | Refresh token đã hash |
| expires_at | TIMESTAMP | Có | Thời điểm hết hạn |
| revoked | BOOLEAN | Có | Token đã bị thu hồi hay chưa |
| revoked_at | TIMESTAMP | Không | Thời điểm thu hồi |
| created_at | TIMESTAMP | Có | Thời điểm tạo |

### 3.9. Bảng system.audit_logs
#### Mô tả

Bảng audit_logs lưu các hành động quan trọng trong hệ thống.

#### Các trường dữ liệu

| Trường | Kiểu dữ liệu | Bắt buộc | Mô tả |
| --- | --- | --- | --- |
| id | UUID | Có | Khóa chính |
| actor_id | UUID | Không | Người thực hiện hành động |
| action | VARCHAR(150) | Có | Hành động |
| target_type | VARCHAR(100) | Không | Loại đối tượng bị tác động |
| target_id | UUID | Không | ID đối tượng bị tác động |
| ip_address | VARCHAR(50) | Không | Địa chỉ IP |
| user_agent | TEXT | Không | Thông tin client |
| metadata | JSONB | Không | Dữ liệu bổ sung |
| created_at | TIMESTAMP | Có | Thời điểm ghi log |

### 3.10. Bảng system.login_history
#### Mô tả

Bảng login_history lưu lịch sử đăng nhập.

#### Các trường dữ liệu

| Trường | Kiểu dữ liệu | Bắt buộc | Mô tả |
| --- | --- | --- | --- |
| id | UUID | Có | Khóa chính |
| user_id | UUID | Không | Người dùng đăng nhập |
| email | VARCHAR(255) | Có | Email dùng để đăng nhập |
| success | BOOLEAN | Có | Đăng nhập thành công hay thất bại |
| failure_reason | TEXT | Không | Lý do thất bại |
| ip_address | VARCHAR(50) | Không | Địa chỉ IP |
| user_agent | TEXT | Không | Thông tin client |
| created_at | TIMESTAMP | Có | Thời điểm đăng nhập |

### 3.11. Quan hệ chính trong schema system
users N --- N roles
roles N --- N permissions
users 1 --- N refresh_tokens
users 1 --- N audit_logs
users 1 --- N login_history
## 4. Schema core
### 4.1. Ý nghĩa schema

Schema core do Core Service sở hữu. Schema này lưu dữ liệu nghiệp vụ cốt lõi:

Môn học.
Thành viên môn học.
Mã tham gia môn học.
Lộ trình môn học.
Tài liệu học tập.
Trạng thái xử lý tài liệu.
### 4.2. Danh sách bảng

| Bảng | Mô tả |
| --- | --- |
| core.courses | Lưu thông tin môn học |
| core.course_members | Lưu thành viên tham gia môn học |
| core.course_access_codes | Lưu mã tham gia môn học |
| core.course_chapters | Lưu chương trong môn học |
| core.course_topics | Lưu chủ đề trong chương |
| core.documents | Lưu metadata tài liệu |
| core.document_versions | Lưu phiên bản tài liệu |
| core.document_processing_jobs | Lưu job xử lý tài liệu |

### 4.3. Bảng core.courses
#### Mô tả

Bảng courses lưu thông tin môn học hoặc không gian học tập do giảng viên tạo.

#### Ý nghĩa

Mỗi môn học là một không gian tri thức riêng. Sinh viên tham gia môn học bằng mã truy cập và AI Tutor hoạt động theo phạm vi môn học đó.

#### Các trường dữ liệu

| Trường | Kiểu dữ liệu | Bắt buộc | Mô tả |
| --- | --- | --- | --- |
| id | UUID | Có | Khóa chính |
| title | VARCHAR(255) | Có | Tên môn học |
| description | TEXT | Không | Mô tả môn học |
| level | VARCHAR(50) | Không | Cấp độ môn học |
| visibility | VARCHAR(30) | Có | Chế độ hiển thị |
| status | VARCHAR(30) | Có | Trạng thái môn học |
| created_by | UUID | Có | Người tạo, tham chiếu logic đến system.users.id |
| created_at | TIMESTAMP | Có | Thời điểm tạo |
| updated_at | TIMESTAMP | Có | Thời điểm cập nhật |
| deleted_at | TIMESTAMP | Không | Thời điểm xóa mềm |

#### Giá trị đề xuất

| Trường | Giá trị |
| --- | --- |
| visibility | PUBLIC, PRIVATE, INVITE_ONLY |
| status | DRAFT, PUBLISHED, ARCHIVED, DELETED |

### 4.4. Bảng core.course_members
#### Mô tả

Bảng course_members lưu danh sách người dùng tham gia một môn học.

#### Ý nghĩa

Bảng này thay cho mô hình ghi danh nặng kiểu LMS. Một người dùng có thể là sinh viên, giảng viên, trợ giảng hoặc chủ sở hữu trong một môn học.

#### Các trường dữ liệu

| Trường | Kiểu dữ liệu | Bắt buộc | Mô tả |
| --- | --- | --- | --- |
| id | UUID | Có | Khóa chính |
| course_id | UUID | Có | Môn học |
| user_id | UUID | Có | Người dùng, tham chiếu logic đến system.users.id |
| role_in_course | VARCHAR(50) | Có | Vai trò trong môn học |
| status | VARCHAR(30) | Có | Trạng thái thành viên |
| joined_at | TIMESTAMP | Có | Thời điểm tham gia |
| invited_by | UUID | Không | Người mời hoặc duyệt |
| removed_at | TIMESTAMP | Không | Thời điểm bị xóa khỏi môn |

#### Giá trị đề xuất

| Trường | Giá trị |
| --- | --- |
| role_in_course | OWNER, LECTURER, ASSISTANT, STUDENT |
| status | PENDING, ACTIVE, REJECTED, REMOVED |

#### Quan hệ
core.courses 1 --- N core.course_members
system.users 1 --- N core.course_members theo logic
### 4.5. Bảng core.course_access_codes
#### Mô tả

Bảng course_access_codes lưu mã tham gia môn học.

#### Ý nghĩa

Sinh viên có thể tham gia môn học bằng mã do giảng viên cung cấp. Cơ chế này giúp hệ thống không trở thành LMS quá nặng nhưng vẫn kiểm soát quyền truy cập tài liệu.

#### Các trường dữ liệu

| Trường | Kiểu dữ liệu | Bắt buộc | Mô tả |
| --- | --- | --- | --- |
| id | UUID | Có | Khóa chính |
| course_id | UUID | Có | Môn học sở hữu mã |
| code_hash | VARCHAR(255) | Có | Mã tham gia đã được hash |
| display_code | VARCHAR(50) | Không | Mã hiển thị nếu cần |
| max_uses | INTEGER | Không | Số lần dùng tối đa |
| used_count | INTEGER | Có | Số lần đã dùng |
| expires_at | TIMESTAMP | Không | Thời điểm hết hạn |
| active | BOOLEAN | Có | Mã còn hiệu lực hay không |
| created_by | UUID | Có | Người tạo mã |
| created_at | TIMESTAMP | Có | Thời điểm tạo |

#### Quan hệ
core.courses 1 --- N core.course_access_codes
### 4.6. Bảng core.course_chapters
#### Mô tả

Bảng course_chapters lưu danh sách chương của môn học.

#### Ý nghĩa

Course outline là xương sống của môn học. AI Tutor sử dụng cấu trúc chương để tạo lộ trình học cá nhân hóa, gợi ý chủ đề ôn tập và liên kết tài liệu.

#### Các trường dữ liệu

| Trường | Kiểu dữ liệu | Bắt buộc | Mô tả |
| --- | --- | --- | --- |
| id | UUID | Có | Khóa chính |
| course_id | UUID | Có | Môn học |
| title | VARCHAR(255) | Có | Tên chương |
| description | TEXT | Không | Mô tả chương |
| learning_objectives | TEXT | Không | Mục tiêu học tập |
| order_index | INTEGER | Có | Thứ tự |
| status | VARCHAR(30) | Có | Trạng thái |
| created_at | TIMESTAMP | Có | Thời điểm tạo |
| updated_at | TIMESTAMP | Có | Thời điểm cập nhật |

#### Quan hệ
core.courses 1 --- N core.course_chapters
### 4.7. Bảng core.course_topics
#### Mô tả

Bảng course_topics lưu các chủ đề nhỏ thuộc từng chương.

#### Ý nghĩa

Chủ đề giúp hệ thống theo dõi tiến độ học tập chi tiết hơn.

#### Các trường dữ liệu

| Trường | Kiểu dữ liệu | Bắt buộc | Mô tả |
| --- | --- | --- | --- |
| id | UUID | Có | Khóa chính |
| chapter_id | UUID | Có | Chương chứa chủ đề |
| title | VARCHAR(255) | Có | Tên chủ đề |
| description | TEXT | Không | Mô tả |
| order_index | INTEGER | Có | Thứ tự |
| estimated_minutes | INTEGER | Không | Thời lượng học dự kiến |
| status | VARCHAR(30) | Có | Trạng thái |
| created_at | TIMESTAMP | Có | Thời điểm tạo |
| updated_at | TIMESTAMP | Có | Thời điểm cập nhật |

#### Quan hệ
core.course_chapters 1 --- N core.course_topics
### 4.8. Bảng core.documents
#### Mô tả

Bảng documents lưu metadata của tài liệu học tập.

#### Ý nghĩa

File thật được lưu trong MinIO. Database chỉ lưu metadata, đường dẫn file và trạng thái xử lý tài liệu.

#### Các trường dữ liệu

| Trường | Kiểu dữ liệu | Bắt buộc | Mô tả |
| --- | --- | --- | --- |
| id | UUID | Có | Khóa chính |
| course_id | UUID | Có | Môn học chứa tài liệu |
| chapter_id | UUID | Không | Chương liên quan |
| topic_id | UUID | Không | Chủ đề liên quan |
| title | VARCHAR(255) | Có | Tên tài liệu |
| description | TEXT | Không | Mô tả tài liệu |
| file_name | VARCHAR(255) | Có | Tên file gốc |
| file_type | VARCHAR(50) | Có | Loại file |
| mime_type | VARCHAR(150) | Không | MIME type |
| file_size | BIGINT | Có | Kích thước file |
| storage_bucket | VARCHAR(100) | Có | Bucket trong MinIO |
| storage_path | TEXT | Có | Đường dẫn object trong MinIO |
| uploaded_by | UUID | Có | Người upload |
| status | VARCHAR(30) | Có | Trạng thái xử lý |
| indexed_at | TIMESTAMP | Không | Thời điểm vector hóa thành công |
| error_message | TEXT | Không | Lỗi xử lý nếu có |
| created_at | TIMESTAMP | Có | Thời điểm upload |
| updated_at | TIMESTAMP | Có | Thời điểm cập nhật |

#### Giá trị đề xuất cho status

| Giá trị | Ý nghĩa |
| --- | --- |
| UPLOADED | File đã được upload |
| PENDING | Đang chờ xử lý |
| PROCESSING | Đang xử lý |
| INDEXED | Đã vector hóa thành công |
| FAILED | Xử lý thất bại |
| DELETED | Đã xóa mềm |

#### Quan hệ
core.courses 1 --- N core.documents
core.course_chapters 1 --- N core.documents
core.course_topics 1 --- N core.documents
### 4.9. Bảng core.document_versions
#### Mô tả

Bảng document_versions lưu các phiên bản của tài liệu.

#### Ý nghĩa

Khi giảng viên cập nhật file tài liệu, hệ thống có thể giữ lại phiên bản cũ để truy vết hoặc rollback.

#### Các trường dữ liệu

| Trường | Kiểu dữ liệu | Bắt buộc | Mô tả |
| --- | --- | --- | --- |
| id | UUID | Có | Khóa chính |
| document_id | UUID | Có | Tài liệu gốc |
| version_number | INTEGER | Có | Số phiên bản |
| file_name | VARCHAR(255) | Có | Tên file |
| file_size | BIGINT | Có | Kích thước file |
| storage_path | TEXT | Có | Đường dẫn file |
| uploaded_by | UUID | Có | Người upload phiên bản |
| status | VARCHAR(30) | Có | Trạng thái phiên bản |
| created_at | TIMESTAMP | Có | Thời điểm tạo phiên bản |

#### Quan hệ
core.documents 1 --- N core.document_versions
### 4.10. Bảng core.document_processing_jobs
#### Mô tả

Bảng document_processing_jobs lưu thông tin các job xử lý tài liệu.

#### Ý nghĩa

Bảng này giúp Core Service theo dõi quá trình xử lý tài liệu bất đồng bộ thông qua Kafka và Python AI Engine.

#### Các trường dữ liệu

| Trường | Kiểu dữ liệu | Bắt buộc | Mô tả |
| --- | --- | --- | --- |
| id | UUID | Có | Khóa chính |
| document_id | UUID | Có | Tài liệu được xử lý |
| job_type | VARCHAR(50) | Có | Loại job |
| status | VARCHAR(30) | Có | Trạng thái job |
| retry_count | INTEGER | Có | Số lần retry |
| started_at | TIMESTAMP | Không | Thời điểm bắt đầu |
| finished_at | TIMESTAMP | Không | Thời điểm kết thúc |
| error_message | TEXT | Không | Lỗi nếu có |
| created_at | TIMESTAMP | Có | Thời điểm tạo |
| updated_at | TIMESTAMP | Có | Thời điểm cập nhật |

#### Giá trị đề xuất

| status | Ý nghĩa |
| --- | --- |
| PENDING | Đang chờ |
| PROCESSING | Đang xử lý |
| COMPLETED | Hoàn thành |
| FAILED | Thất bại |

### 4.11. Quan hệ chính trong schema core
courses 1 --- N course_members
courses 1 --- N course_access_codes
courses 1 --- N course_chapters
course_chapters 1 --- N course_topics
courses 1 --- N documents
course_chapters 1 --- N documents
course_topics 1 --- N documents
documents 1 --- N document_versions
documents 1 --- N document_processing_jobs
## 5. Schema ai_app
### 5.1. Ý nghĩa schema

Schema ai_app do AI Orchestrator Service sở hữu. Schema này lưu dữ liệu nghiệp vụ học tập với AI:

Hồ sơ học tập theo môn.
Phiên chat.
Tin nhắn.
Lộ trình học cá nhân hóa.
Bài luyện tập.
Bài làm.
Tiến độ học tập.
Khuyến nghị học tập.
Log request AI.
### 5.2. Danh sách bảng

| Bảng | Mô tả |
| --- | --- |
| ai_app.course_learning_profiles | Hồ sơ học tập theo môn |
| ai_app.chat_sessions | Phiên chat với AI Tutor |
| ai_app.chat_messages | Tin nhắn trong phiên chat |
| ai_app.learning_plans | Lộ trình học cá nhân hóa |
| ai_app.learning_plan_items | Các mục trong lộ trình học |
| ai_app.quizzes | Bài luyện tập |
| ai_app.quiz_questions | Câu hỏi trong bài luyện tập |
| ai_app.quiz_attempts | Lần làm bài |
| ai_app.quiz_attempt_answers | Câu trả lời của sinh viên |
| ai_app.learning_progress | Tiến độ học tập |
| ai_app.recommendations | Khuyến nghị học tập |
| ai_app.ai_request_logs | Lịch sử gọi AI |

### 5.3. Bảng ai_app.course_learning_profiles
#### Mô tả

Lưu hồ sơ học tập của sinh viên trong từng môn học.

#### Ý nghĩa

Hồ sơ học tập phải gắn với môn học, vì năng lực của sinh viên có thể khác nhau theo từng môn.

#### Các trường dữ liệu

| Trường | Kiểu dữ liệu | Bắt buộc | Mô tả |
| --- | --- | --- | --- |
| id | UUID | Có | Khóa chính |
| student_id | UUID | Có | Sinh viên |
| course_id | UUID | Có | Môn học |
| current_level | VARCHAR(50) | Không | Trình độ hiện tại |
| learning_goal | TEXT | Không | Mục tiêu học tập |
| available_days | INTEGER | Không | Số ngày có thể học |
| available_time_per_day_minutes | INTEGER | Không | Thời gian học mỗi ngày |
| exam_date | DATE | Không | Ngày thi hoặc deadline |
| preferred_explanation_style | VARCHAR(100) | Không | Phong cách giải thích mong muốn |
| weak_topics | JSONB | Không | Danh sách chủ đề yếu |
| metadata | JSONB | Không | Dữ liệu bổ sung |
| created_at | TIMESTAMP | Có | Thời điểm tạo |
| updated_at | TIMESTAMP | Có | Thời điểm cập nhật |

#### Ràng buộc
Unique(student_id, course_id)
### 5.4. Bảng ai_app.chat_sessions
#### Mô tả

Lưu các phiên trò chuyện giữa sinh viên và AI Tutor.

#### Các trường dữ liệu

| Trường | Kiểu dữ liệu | Bắt buộc | Mô tả |
| --- | --- | --- | --- |
| id | UUID | Có | Khóa chính |
| student_id | UUID | Có | Sinh viên |
| course_id | UUID | Có | Môn học hiện tại |
| title | VARCHAR(255) | Không | Tiêu đề phiên chat |
| status | VARCHAR(30) | Có | Trạng thái |
| created_at | TIMESTAMP | Có | Thời điểm tạo |
| updated_at | TIMESTAMP | Có | Thời điểm cập nhật |

### 5.5. Bảng ai_app.chat_messages
#### Mô tả

Lưu từng tin nhắn trong phiên chat.

#### Các trường dữ liệu

| Trường | Kiểu dữ liệu | Bắt buộc | Mô tả |
| --- | --- | --- | --- |
| id | UUID | Có | Khóa chính |
| session_id | UUID | Có | Phiên chat |
| sender_type | VARCHAR(30) | Có | Người gửi |
| content | TEXT | Có | Nội dung |
| out_of_scope | BOOLEAN | Có | Câu hỏi ngoài phạm vi môn học hay không |
| metadata | JSONB | Không | Dữ liệu bổ sung |
| created_at | TIMESTAMP | Có | Thời điểm gửi |

Giá trị đề xuất cho sender_type

| Giá trị | Ý nghĩa |
| --- | --- |
| STUDENT | Sinh viên |
| AI | AI Tutor |
| SYSTEM | Hệ thống |

#### Quan hệ
chat_sessions 1 --- N chat_messages
### 5.6. Bảng ai_app.learning_plans
#### Mô tả

Lưu lộ trình học cá nhân hóa.

#### Ý nghĩa

Learning plan được tạo dựa trên course outline, tài liệu môn học, hồ sơ học tập và mục tiêu của sinh viên.

#### Các trường dữ liệu

| Trường | Kiểu dữ liệu | Bắt buộc | Mô tả |
| --- | --- | --- | --- |
| id | UUID | Có | Khóa chính |
| student_id | UUID | Có | Sinh viên |
| course_id | UUID | Có | Môn học |
| profile_id | UUID | Không | Hồ sơ học tập được dùng |
| title | VARCHAR(255) | Có | Tên lộ trình |
| description | TEXT | Không | Mô tả |
| status | VARCHAR(30) | Có | Trạng thái |
| generated_by_ai | BOOLEAN | Có | Có do AI tạo hay không |
| created_at | TIMESTAMP | Có | Thời điểm tạo |
| updated_at | TIMESTAMP | Có | Thời điểm cập nhật |

#### Giá trị đề xuất cho status

| Giá trị | Ý nghĩa |
| --- | --- |
| DRAFT | Bản nháp |
| ACTIVE | Đang học |
| COMPLETED | Hoàn thành |
| ARCHIVED | Lưu trữ |

### 5.7. Bảng ai_app.learning_plan_items
#### Mô tả

Lưu các mục chi tiết trong một lộ trình học.

#### Các trường dữ liệu

| Trường | Kiểu dữ liệu | Bắt buộc | Mô tả |
| --- | --- | --- | --- |
| id | UUID | Có | Khóa chính |
| plan_id | UUID | Có | Lộ trình học |
| chapter_id | UUID | Không | Chương liên quan |
| topic_id | UUID | Không | Chủ đề liên quan |
| title | VARCHAR(255) | Có | Tiêu đề nhiệm vụ |
| description | TEXT | Không | Mô tả |
| day_number | INTEGER | Không | Ngày học thứ mấy |
| order_index | INTEGER | Có | Thứ tự |
| estimated_minutes | INTEGER | Không | Thời lượng dự kiến |
| status | VARCHAR(30) | Có | Trạng thái |
| due_date | DATE | Không | Ngày dự kiến hoàn thành |
| completed_at | TIMESTAMP | Không | Thời điểm hoàn thành |

#### Quan hệ
learning_plans 1 --- N learning_plan_items
### 5.8. Bảng ai_app.quizzes
#### Mô tả

Lưu bài luyện tập được tạo cho sinh viên.

#### Các trường dữ liệu

| Trường | Kiểu dữ liệu | Bắt buộc | Mô tả |
| --- | --- | --- | --- |
| id | UUID | Có | Khóa chính |
| student_id | UUID | Có | Sinh viên |
| course_id | UUID | Có | Môn học |
| title | VARCHAR(255) | Có | Tên bài luyện tập |
| quiz_type | VARCHAR(50) | Có | Loại bài |
| difficulty | VARCHAR(50) | Không | Độ khó |
| source_type | VARCHAR(50) | Có | Nguồn tạo |
| status | VARCHAR(30) | Có | Trạng thái |
| created_at | TIMESTAMP | Có | Thời điểm tạo |

#### Giá trị đề xuất

| Trường | Giá trị |
| --- | --- |
| quiz_type | PRACTICE, REVIEW, ASSESSMENT |
| difficulty | EASY, MEDIUM, HARD |
| source_type | AI, LECTURER, SYSTEM |

### 5.9. Bảng ai_app.quiz_questions
#### Mô tả

Lưu câu hỏi thuộc bài luyện tập.

#### Các trường dữ liệu

| Trường | Kiểu dữ liệu | Bắt buộc | Mô tả |
| --- | --- | --- | --- |
| id | UUID | Có | Khóa chính |
| quiz_id | UUID | Có | Bài luyện tập |
| topic_id | UUID | Không | Chủ đề liên quan |
| question_text | TEXT | Có | Nội dung câu hỏi |
| question_type | VARCHAR(50) | Có | Loại câu hỏi |
| options | JSONB | Không | Danh sách lựa chọn |
| correct_answer | TEXT | Không | Đáp án đúng hoặc tham khảo |
| explanation | TEXT | Không | Giải thích đáp án |
| points | DOUBLE PRECISION | Có | Điểm |
| order_index | INTEGER | Có | Thứ tự |

#### Quan hệ
quizzes 1 --- N quiz_questions
### 5.10. Bảng ai_app.quiz_attempts
#### Mô tả

Lưu mỗi lần sinh viên làm một bài luyện tập.

#### Các trường dữ liệu

| Trường | Kiểu dữ liệu | Bắt buộc | Mô tả |
| --- | --- | --- | --- |
| id | UUID | Có | Khóa chính |
| quiz_id | UUID | Có | Bài luyện tập |
| student_id | UUID | Có | Sinh viên |
| status | VARCHAR(30) | Có | Trạng thái |
| score | DOUBLE PRECISION | Không | Điểm |
| feedback | TEXT | Không | Nhận xét tổng quát |
| started_at | TIMESTAMP | Có | Thời điểm bắt đầu |
| submitted_at | TIMESTAMP | Không | Thời điểm nộp |
| evaluated_at | TIMESTAMP | Không | Thời điểm đánh giá |

#### Quan hệ
quizzes 1 --- N quiz_attempts
### 5.11. Bảng ai_app.quiz_attempt_answers
#### Mô tả

Lưu câu trả lời của sinh viên cho từng câu hỏi.

#### Các trường dữ liệu

| Trường | Kiểu dữ liệu | Bắt buộc | Mô tả |
| --- | --- | --- | --- |
| id | UUID | Có | Khóa chính |
| attempt_id | UUID | Có | Lần làm bài |
| question_id | UUID | Có | Câu hỏi |
| answer_text | TEXT | Không | Câu trả lời dạng văn bản |
| selected_option | VARCHAR(100) | Không | Lựa chọn nếu là trắc nghiệm |
| is_correct | BOOLEAN | Không | Đúng hay sai |
| score | DOUBLE PRECISION | Không | Điểm câu hỏi |
| feedback | TEXT | Không | Nhận xét |
| created_at | TIMESTAMP | Có | Thời điểm trả lời |

#### Quan hệ
quiz_attempts 1 --- N quiz_attempt_answers
quiz_questions 1 --- N quiz_attempt_answers
### 5.12. Bảng ai_app.learning_progress
#### Mô tả

Lưu tiến độ học tập của sinh viên theo môn, chương hoặc chủ đề.

#### Các trường dữ liệu

| Trường | Kiểu dữ liệu | Bắt buộc | Mô tả |
| --- | --- | --- | --- |
| id | UUID | Có | Khóa chính |
| student_id | UUID | Có | Sinh viên |
| course_id | UUID | Có | Môn học |
| chapter_id | UUID | Không | Chương |
| topic_id | UUID | Không | Chủ đề |
| completion_rate | DOUBLE PRECISION | Có | Tỷ lệ hoàn thành |
| mastery_score | DOUBLE PRECISION | Không | Mức độ nắm vững |
| last_activity_type | VARCHAR(100) | Không | Hoạt động gần nhất |
| last_studied_at | TIMESTAMP | Không | Lần học gần nhất |
| updated_at | TIMESTAMP | Có | Thời điểm cập nhật |

### 5.13. Bảng ai_app.recommendations
#### Mô tả

Lưu khuyến nghị học tập dành cho sinh viên.

#### Các trường dữ liệu

| Trường | Kiểu dữ liệu | Bắt buộc | Mô tả |
| --- | --- | --- | --- |
| id | UUID | Có | Khóa chính |
| student_id | UUID | Có | Sinh viên |
| course_id | UUID | Có | Môn học |
| recommendation_type | VARCHAR(100) | Có | Loại khuyến nghị |
| content | TEXT | Có | Nội dung khuyến nghị |
| reason | TEXT | Không | Lý do |
| priority | INTEGER | Không | Mức ưu tiên |
| status | VARCHAR(30) | Có | Trạng thái |
| created_at | TIMESTAMP | Có | Thời điểm tạo |

### 5.14. Bảng ai_app.ai_request_logs
#### Mô tả

Lưu lịch sử các request AI ở tầng nghiệp vụ.

#### Các trường dữ liệu

| Trường | Kiểu dữ liệu | Bắt buộc | Mô tả |
| --- | --- | --- | --- |
| id | UUID | Có | Khóa chính |
| requester_id | UUID | Có | Người gửi yêu cầu |
| course_id | UUID | Không | Môn học liên quan |
| use_case | VARCHAR(100) | Có | Loại tác vụ AI |
| request_payload | JSONB | Không | Dữ liệu request |
| response_summary | TEXT | Không | Tóm tắt response |
| status | VARCHAR(30) | Có | Trạng thái xử lý |
| model_name | VARCHAR(100) | Không | Model được dùng |
| latency_ms | INTEGER | Không | Thời gian xử lý |
| created_at | TIMESTAMP | Có | Thời điểm gọi AI |

### 5.15. Quan hệ chính trong schema ai_app
course_learning_profiles 1 --- N learning_plans
learning_plans 1 --- N learning_plan_items
chat_sessions 1 --- N chat_messages
quizzes 1 --- N quiz_questions
quizzes 1 --- N quiz_attempts
quiz_attempts 1 --- N quiz_attempt_answers
## 6. Schema ai_engine
### 6.1. Ý nghĩa schema

Schema ai_engine do Python AI Engine Service sở hữu. Schema này lưu dữ liệu kỹ thuật phục vụ RAG:

Chunk tài liệu.
Vector embedding.
Log truy vấn RAG.
Chunk được truy xuất.
Prompt template.
Cấu hình model.
Log xử lý indexing.
### 6.2. Danh sách bảng

| Bảng | Mô tả |
| --- | --- |
| ai_engine.document_chunks | Lưu chunk và embedding |
| ai_engine.rag_query_logs | Lưu log truy vấn RAG |
| ai_engine.rag_retrieved_chunks | Lưu chunk được truy xuất |
| ai_engine.prompt_templates | Lưu prompt template |
| ai_engine.model_configs | Lưu cấu hình model |
| ai_engine.document_indexing_logs | Lưu log xử lý indexing |

### 6.3. Bảng ai_engine.document_chunks
#### Mô tả

Lưu các đoạn nội dung được tách từ tài liệu và vector embedding tương ứng.

#### Ý nghĩa

Đây là bảng trung tâm của RAG. Khi sinh viên hỏi AI Tutor, hệ thống embedding câu hỏi và tìm các chunk liên quan trong bảng này theo course_id.

#### Các trường dữ liệu

| Trường | Kiểu dữ liệu | Bắt buộc | Mô tả |
| --- | --- | --- | --- |
| id | UUID | Có | Khóa chính |
| document_id | UUID | Có | Tài liệu gốc |
| document_version_id | UUID | Không | Phiên bản tài liệu liên quan, tham chiếu logic đến core.document_versions.id |
| course_id | UUID | Có | Môn học |
| chapter_id | UUID | Không | Chương liên quan |
| topic_id | UUID | Không | Chủ đề liên quan |
| chunk_index | INTEGER | Có | Thứ tự chunk |
| content | TEXT | Có | Nội dung chunk |
| token_count | INTEGER | Không | Số token ước lượng |
| embedding | VECTOR(n) | Có | Vector embedding |
| source_page | INTEGER | Không | Trang trong tài liệu |
| metadata | JSONB | Không | Metadata bổ sung |
| status | VARCHAR(30) | Có | Trạng thái chunk: ACTIVE, ARCHIVED, DELETED |
| created_at | TIMESTAMP | Có | Thời điểm tạo |

#### Ghi chú về embedding

Kích thước vector phụ thuộc vào embedding model được chọn.

Ví dụ:

embedding VECTOR(768)
embedding VECTOR(1536)
#### Index đề xuất

| Index | Ý nghĩa |
| --- | --- |
| (course_id, document_id) | Truy vấn chunk theo môn và tài liệu |
| (course_id, chapter_id) | Truy vấn chunk theo chương |
| Vector index trên embedding | Tăng tốc vector search |

### 6.4. Bảng ai_engine.rag_query_logs
#### Mô tả

Lưu thông tin mỗi lần hệ thống thực hiện truy vấn RAG.

#### Các trường dữ liệu

| Trường | Kiểu dữ liệu | Bắt buộc | Mô tả |
| --- | --- | --- | --- |
| id | UUID | Có | Khóa chính |
| request_id | UUID | Không | ID request từ AI Orchestrator |
| student_id | UUID | Không | Sinh viên đặt câu hỏi |
| course_id | UUID | Có | Môn học đang hỏi |
| question | TEXT | Có | Câu hỏi gốc |
| normalized_question | TEXT | Không | Câu hỏi sau tiền xử lý |
| top_k | INTEGER | Có | Số chunk cần lấy |
| similarity_threshold | DOUBLE PRECISION | Không | Ngưỡng tương đồng |
| model_name | VARCHAR(100) | Không | LLM được dùng |
| answer | TEXT | Không | Câu trả lời |
| status | VARCHAR(30) | Có | Trạng thái |
| latency_ms | INTEGER | Không | Thời gian xử lý |
| created_at | TIMESTAMP | Có | Thời điểm truy vấn |

#### Giá trị đề xuất cho status

| Giá trị | Ý nghĩa |
| --- | --- |
| SUCCESS | Thành công |
| FAILED | Thất bại |
| OUT_OF_SCOPE | Câu hỏi ngoài phạm vi môn học |

### 6.5. Bảng ai_engine.rag_retrieved_chunks
#### Mô tả

Lưu các chunk được truy xuất trong một lần RAG query.

#### Ý nghĩa

Bảng này giúp biết câu trả lời của AI đã dựa trên những đoạn tài liệu nào.

#### Các trường dữ liệu

| Trường | Kiểu dữ liệu | Bắt buộc | Mô tả |
| --- | --- | --- | --- |
| id | UUID | Có | Khóa chính |
| rag_query_id | UUID | Có | Truy vấn RAG |
| chunk_id | UUID | Có | Chunk được truy xuất |
| rank | INTEGER | Có | Thứ hạng |
| similarity_score | DOUBLE PRECISION | Có | Độ tương đồng |
| content_snapshot | TEXT | Không | Nội dung chunk tại thời điểm truy vấn |
| created_at | TIMESTAMP | Có | Thời điểm tạo |

#### Quan hệ
rag_query_logs 1 --- N rag_retrieved_chunks
document_chunks 1 --- N rag_retrieved_chunks
### 6.6. Bảng ai_engine.prompt_templates
#### Mô tả

Lưu prompt template được dùng bởi AI Engine.

#### Các trường dữ liệu

| Trường | Kiểu dữ liệu | Bắt buộc | Mô tả |
| --- | --- | --- | --- |
| id | UUID | Có | Khóa chính |
| code | VARCHAR(100) | Có | Mã template |
| name | VARCHAR(255) | Có | Tên template |
| task_type | VARCHAR(100) | Có | Loại tác vụ |
| template | TEXT | Có | Nội dung prompt |
| version | INTEGER | Có | Phiên bản |
| active | BOOLEAN | Có | Có đang sử dụng không |
| created_at | TIMESTAMP | Có | Thời điểm tạo |
| updated_at | TIMESTAMP | Có | Thời điểm cập nhật |

### 6.7. Bảng ai_engine.model_configs
#### Mô tả

Lưu cấu hình model AI.

#### Các trường dữ liệu

| Trường | Kiểu dữ liệu | Bắt buộc | Mô tả |
| --- | --- | --- | --- |
| id | UUID | Có | Khóa chính |
| provider | VARCHAR(100) | Có | Nhà cung cấp model |
| model_name | VARCHAR(150) | Có | Tên model LLM |
| embedding_model | VARCHAR(150) | Không | Tên embedding model |
| embedding_dimension | INTEGER | Không | Số chiều embedding |
| temperature | DOUBLE PRECISION | Không | Độ sáng tạo |
| max_tokens | INTEGER | Không | Số token tối đa |
| active | BOOLEAN | Có | Có đang dùng không |
| created_at | TIMESTAMP | Có | Thời điểm tạo |
| updated_at | TIMESTAMP | Có | Thời điểm cập nhật |

### 6.8. Bảng ai_engine.document_indexing_logs
#### Mô tả

Lưu lịch sử xử lý indexing tài liệu ở phía AI Engine.

#### Các trường dữ liệu

| Trường | Kiểu dữ liệu | Bắt buộc | Mô tả |
| --- | --- | --- | --- |
| id | UUID | Có | Khóa chính |
| document_id | UUID | Có | Tài liệu được xử lý |
| course_id | UUID | Có | Môn học |
| status | VARCHAR(30) | Có | Trạng thái xử lý |
| chunk_count | INTEGER | Không | Số chunk tạo ra |
| embedding_model | VARCHAR(150) | Không | Model embedding |
| started_at | TIMESTAMP | Không | Thời điểm bắt đầu |
| finished_at | TIMESTAMP | Không | Thời điểm kết thúc |
| error_message | TEXT | Không | Lỗi nếu có |
| metadata | JSONB | Không | Dữ liệu bổ sung |
| created_at | TIMESTAMP | Có | Thời điểm tạo log |

### 6.9. Quan hệ chính trong schema ai_engine
document_chunks N --- 1 document theo logic
rag_query_logs 1 --- N rag_retrieved_chunks
document_chunks 1 --- N rag_retrieved_chunks
document_indexing_logs N --- 1 document theo logic
## 7. Quan hệ giữa các schema
### 7.1. Tham chiếu logic quan trọng

| Từ bảng | Trường | Tham chiếu logic đến | Ý nghĩa |
| --- | --- | --- | --- |
| core.courses | created_by | system.users.id | Người tạo môn học |
| core.course_members | user_id | system.users.id | Thành viên môn học |
| core.documents | uploaded_by | system.users.id | Người upload tài liệu |
| ai_app.course_learning_profiles | student_id | system.users.id | Sinh viên |
| ai_app.course_learning_profiles | course_id | core.courses.id | Môn học |
| ai_app.chat_sessions | student_id | system.users.id | Sinh viên chat |
| ai_app.chat_sessions | course_id | core.courses.id | Môn học đang chat |
| ai_app.learning_plans | student_id | system.users.id | Sinh viên sở hữu plan |
| ai_app.learning_plans | course_id | core.courses.id | Môn học của plan |
| ai_app.quizzes | student_id | system.users.id | Sinh viên được tạo quiz |
| ai_app.quizzes | course_id | core.courses.id | Môn học của quiz |
| ai_engine.document_chunks | document_id | core.documents.id | Tài liệu gốc |
| ai_engine.document_chunks | document_version_id | core.document_versions.id | Phiên bản tài liệu được index |
| ai_engine.document_chunks | course_id | core.courses.id | Phạm vi môn học |

### 7.2. Ý nghĩa

Các quan hệ này không nhất thiết là foreign key vật lý trong PostgreSQL. Chúng là tham chiếu logic để giữ tính độc lập giữa các service. Trong phạm vi cùng một schema có thể khai báo foreign key vật lý; với quan hệ xuyên schema, nên ưu tiên kiểm tra bằng API hoặc event để phù hợp nguyên tắc service ownership.

## 8. Các mối quan hệ nghiệp vụ quan trọng
### 8.1. User, Role, Permission
User N --- N Role
Role N --- N Permission

Ý nghĩa:

User có thể có nhiều role.
Role có nhiều permission.
Backend dùng permission để kiểm tra API.
Client không được gửi role hoặc permission để backend tin.
Role và permission phải được lấy từ JWT đã ký hoặc System Service.
### 8.2. Course và CourseMember
Course 1 --- N CourseMember
User 1 --- N CourseMember theo logic

Ý nghĩa:

Sinh viên phải là thành viên môn học mới được chat AI trong môn đó.
Giảng viên phải là thành viên có vai trò OWNER hoặc LECTURER mới được upload tài liệu.
Admin có quyền quản trị hệ thống nhưng không nhất thiết là thành viên của từng môn học.
### 8.3. Course Outline
Course 1 --- N CourseChapter
CourseChapter 1 --- N CourseTopic

Ý nghĩa:

Course outline giúp AI Tutor hiểu cấu trúc môn học.
Learning plan được tạo dựa trên outline.
Tiến độ học tập có thể theo dõi theo topic.
### 8.4. Document và Knowledge Base
Document 1 --- N DocumentChunk theo logic

Ý nghĩa:

Một tài liệu sau khi xử lý sẽ được chia thành nhiều chunk.
Mỗi chunk có embedding.
RAG truy vấn chunk theo course_id.
### 8.5. Learning Plan
CourseLearningProfile 1 --- N LearningPlan
LearningPlan 1 --- N LearningPlanItem

Ý nghĩa:

Một sinh viên có thể có nhiều learning plan cho cùng một môn.
Mỗi learning plan gồm nhiều nhiệm vụ học tập.
### 8.6. Quiz
Quiz 1 --- N QuizQuestion
Quiz 1 --- N QuizAttempt
QuizAttempt 1 --- N QuizAttemptAnswer

Ý nghĩa:

Một bài luyện tập có nhiều câu hỏi.
Một sinh viên có thể làm quiz nhiều lần.
Mỗi lần làm bài có nhiều câu trả lời và feedback.
## 9. Luồng dữ liệu quan trọng
### 9.1. Đăng nhập
Frontend
→ Gateway
→ System Service
→ system.users, system.roles, system.permissions
→ JWT access token + refresh token

Dữ liệu liên quan:

| Bảng | Vai trò |
| --- | --- |
| system.users | Kiểm tra tài khoản |
| system.user_roles | Lấy role |
| system.role_permissions | Lấy permission |
| system.refresh_tokens | Lưu refresh token |
| system.login_history | Lưu lịch sử đăng nhập |

### 9.2. Tham gia môn học
Student
→ Core Service
→ core.course_access_codes
→ core.course_members

Dữ liệu liên quan:

| Bảng | Vai trò |
| --- | --- |
| core.course_access_codes | Kiểm tra mã tham gia |
| core.course_members | Thêm sinh viên vào môn |
| core.courses | Kiểm tra môn học tồn tại |

### 9.3. Upload tài liệu
Lecturer
→ Core Service
→ Check permission DOCUMENT_UPLOAD
→ Check lecturer belongs to course
→ Store file in MinIO
→ Save metadata to core.documents
→ Publish document.uploaded
→ Python AI Engine
→ Save chunks to ai_engine.document_chunks
→ Publish document.indexed / document.index_failed
→ Core Service updates document status

Dữ liệu liên quan:

| Bảng | Vai trò |
| --- | --- |
| core.course_members | Kiểm tra giảng viên thuộc môn học |
| core.documents | Lưu metadata |
| core.document_processing_jobs | Theo dõi job xử lý |
| ai_engine.document_chunks | Lưu chunk và embedding |
| ai_engine.document_indexing_logs | Log xử lý |

### 9.4. Chat với AI Tutor
Student
→ AI Orchestrator Service
→ Check student belongs to course
→ Python AI Engine
→ Vector search in ai_engine.document_chunks
→ LLM Provider
→ Save chat history

Dữ liệu liên quan:

| Bảng | Vai trò |
| --- | --- |
| core.course_members | Kiểm tra sinh viên đã tham gia môn |
| ai_app.chat_sessions | Lưu phiên chat |
| ai_app.chat_messages | Lưu tin nhắn |
| ai_engine.document_chunks | Truy xuất tri thức |
| ai_engine.rag_query_logs | Log RAG |
| ai_engine.rag_retrieved_chunks | Lưu chunk được dùng |

## 10. Ràng buộc dữ liệu quan trọng
### 10.1. Ràng buộc phân quyền
Người dùng phải đăng nhập mới dùng chức năng học tập.
Sinh viên phải tham gia môn học mới được chat AI trong môn đó.
Giảng viên phải là OWNER hoặc LECTURER của môn học mới được upload tài liệu.
Admin có quyền quản trị theo permission.
### 10.2. Ràng buộc tài liệu
File tài liệu không lưu trực tiếp trong PostgreSQL.
File được lưu trong MinIO.
core.documents.storage_path là đường dẫn tới file.
Một tài liệu phải thuộc một môn học.
Chỉ tài liệu có trạng thái INDEXED và chunk có trạng thái ACTIVE mới được dùng cho RAG.
### 10.3. Ràng buộc RAG theo môn học
Vector search phải giới hạn theo course_id.
AI Tutor không được truy vấn toàn bộ knowledge base.
Nếu không tìm thấy chunk đủ liên quan, hệ thống có thể đánh dấu câu hỏi ngoài phạm vi môn học.

Ví dụ logic truy vấn:

```sql
SELECT *
FROM ai_engine.document_chunks
WHERE course_id = :courseId
  AND status = 'ACTIVE'
ORDER BY embedding <=> :queryEmbedding
LIMIT :topK;
```
### 10.4. Ràng buộc hồ sơ học tập
Một sinh viên chỉ có một hồ sơ học tập chính cho một môn học.
Hồ sơ học tập chỉ được tạo sau khi sinh viên đã tham gia môn học.
Learning plan nên được tạo dựa trên course outline và hồ sơ học tập.

### 10.5. Ràng buộc khóa chính và duy nhất bổ sung

| Bảng | Ràng buộc đề xuất | Lý do |
| --- | --- | --- |
| system.user_roles | PRIMARY KEY(user_id, role_id) | Tránh gán trùng một vai trò cho cùng một người dùng |
| system.role_permissions | PRIMARY KEY(role_id, permission_id) | Tránh gán trùng một quyền cho cùng một vai trò |
| core.course_members | UNIQUE(course_id, user_id) | Một người dùng chỉ có một bản ghi thành viên trong một môn học |
| core.course_access_codes | UNIQUE(code_hash) | Tránh trùng mã tham gia sau khi hash |
| core.course_chapters | UNIQUE(course_id, order_index) | Đảm bảo thứ tự chương không bị trùng trong cùng một môn học |
| core.course_topics | UNIQUE(chapter_id, order_index) | Đảm bảo thứ tự chủ đề không bị trùng trong cùng một chương |
| core.document_versions | UNIQUE(document_id, version_number) | Đảm bảo số phiên bản tài liệu không bị trùng |
| ai_app.course_learning_profiles | UNIQUE(student_id, course_id) | Một sinh viên chỉ có một hồ sơ học tập chính trong một môn học |

## 11. Index đề xuất
### 11.1. Schema system

| Bảng | Index |
| --- | --- |
| users | email, username, status |
| user_roles | user_id, role_id |
| role_permissions | role_id, permission_id |
| refresh_tokens | user_id, token_hash |

### 11.2. Schema core

| Bảng | Index |
| --- | --- |
| courses | created_by, status, title |
| course_members | (course_id, user_id), user_id, (course_id, role_in_course) |
| course_access_codes | code_hash, course_id |
| course_chapters | course_id |
| course_topics | chapter_id |
| documents | (course_id, status), chapter_id, topic_id, uploaded_by |

### 11.3. Schema ai_app

| Bảng | Index |
| --- | --- |
| course_learning_profiles | (student_id, course_id) |
| chat_sessions | (student_id, course_id) |
| chat_messages | session_id, created_at |
| learning_plans | (student_id, course_id) |
| quizzes | (student_id, course_id) |
| quiz_attempts | (quiz_id, student_id) |
| learning_progress | (student_id, course_id, topic_id) |
| recommendations | (student_id, course_id, status) |

### 11.4. Schema ai_engine

| Bảng | Index |
| --- | --- |
| document_chunks | (course_id, document_id, status), (course_id, chapter_id, status), vector index trên embedding |
| rag_query_logs | (student_id, course_id), created_at, status |
| rag_retrieved_chunks | rag_query_id, chunk_id |
| prompt_templates | code, (task_type, active) |
| model_configs | (provider, active) |
