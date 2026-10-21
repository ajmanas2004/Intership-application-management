# INTERNSHIP APPLICATION MANAGEMENT

Ứng dụng desktop giúp sinh viên quản lý công ty, lưu các vị trí thực tập và theo dõi tiến độ ứng tuyển. Dữ liệu được lưu trong MySQL; người dùng thao tác qua giao diện Java Swing.

## 1. Công nghệ sử dụng

| Thành phần | Công nghệ trong dự án | Vai trò |
| --- | --- | --- |
| Ngôn ngữ | Java, JDK 23 | Xử lý nghiệp vụ và khởi chạy ứng dụng |
| Giao diện | Java Swing, Nimbus Look and Feel | Cửa sổ, bảng dữ liệu, form và lịch chọn ngày |
| Cơ sở dữ liệu | MySQL 8.4+ | Lưu công ty, vị trí thực tập và đơn ứng tuyển |
| Kết nối dữ liệu | JDBC, MySQL Connector/J 26.7.0 | Thực thi truy vấn và ánh xạ dữ liệu |
| Quản lý dự án | Maven, Maven Wrapper 3.9.9 | Tải dependency, biên dịch và đóng gói |
| Đóng gói | Maven Shade Plugin | Tạo JAR thực thi kèm JDBC driver |
| File I/O | Java NIO, CSV UTF-8 | Xuất danh sách đơn ứng tuyển |
| Môi trường database tùy chọn | Docker Compose, image `mysql:8.4.11` | Chạy MySQL và khởi tạo schema lần đầu |

## 2. Kiến trúc và luồng chạy

### 2.1. Phân chia các thành phần

Ứng dụng tổ chức theo kiến trúc **View → Service → DAO → Database**, trong package gốc `com.internshipmanagement`.

| Thành phần | Trách nhiệm | Các lớp chính |
| --- | --- | --- |
| View | Hiển thị dữ liệu, nhận thao tác và gọi Service | `MainFrame`, các Panel và Dialog |
| Service | Kiểm tra dữ liệu, xử lý quy tắc nghiệp vụ | `CompanyService`, `InternshipService`, `ApplicationService`, `DashboardService`, `ExportService` |
| DAO | Thực hiện SQL qua JDBC và chuyển kết quả thành Entity | `CompanyDAO`, `InternshipDAO`, `ApplicationDAO`, `Jdbc` |
| Entity | Biểu diễn dữ liệu và các giá trị lựa chọn | `Company`, `Internship`, `Application`, `ApplicationStatus`, `DeadlineFilter` |
| Config | Đọc cấu hình và cung cấp kết nối MySQL | `DatabaseConnection`, `ConnectionProvider` |

View không viết SQL trực tiếp. `Jdbc` dùng `PreparedStatement` để truyền tham số và tự đóng connection, statement, result set sau mỗi thao tác.

### 2.2. Luồng khởi động

1. Chạy `Main.main()`.
2. `SwingUtilities.invokeLater()` đưa việc tạo giao diện lên luồng giao diện Swing (EDT) và thiết lập Nimbus.
3. `DatabaseConnection` đọc cấu hình kết nối từ biến môi trường hoặc `database.properties`.
4. `Main.createFrame()` tạo các DAO, Service và truyền chúng vào các màn hình tương ứng.
5. `MainFrame` đăng ký bốn màn hình bằng `CardLayout`, hiển thị cửa sổ và mở **Tổng quan**.
6. Dashboard tải số liệu từ database ở luồng nền rồi cập nhật giao diện.

Việc tạo đối tượng `DatabaseConnection` chỉ đọc cấu hình. Kết nối JDBC được mở khi thực hiện truy vấn; ứng dụng Java không tự tạo database hoặc bảng lúc khởi động.

### 2.3. Luồng xử lý một thao tác

Ví dụ khi thêm công ty:

```text
Người dùng bấm Thêm tại CompanyPanel
    → CompanyDialog nhận tên, ngành nghề, địa điểm, website
    → CompanyService kiểm tra dữ liệu bắt buộc và độ dài
    → CompanyDAO gọi Jdbc để INSERT vào MySQL
    → MySQL trả về ID được tạo
    → Dialog đóng sau khi lưu thành công
    → CompanyPanel tải lại danh sách
```

Các thao tác JDBC và ghi CSV chạy ở luồng nền qua `SwingWorker`; kết quả và thông báo lỗi được cập nhật trên EDT. Dữ liệu nhập sai hoặc thao tác vi phạm ràng buộc được hiển thị bằng hộp thoại.

## 3. View – Giao diện và luồng sử dụng

`MainFrame` là cửa sổ chính, chứa thanh chuyển màn hình, nút **Quay lại / Tiến tới** và nội dung trang hiện tại. Các màn hình cùng tồn tại trong `CardLayout`.

### 3.1. Tổng quan – `DashboardPanel`

- Hiển thị tổng số **đơn** đang theo dõi và tổng số đơn ở trạng thái khác `SAVED`.
- Thống kê số đơn theo từng trạng thái.
- Hiển thị tối đa 10 đơn cập nhật gần đây, gồm công ty, vị trí, trạng thái, ngày ứng tuyển và ngày cập nhật.
- Bấm thẻ tổng đơn hoặc thẻ trạng thái để mở danh sách đơn tương ứng.
- Chọn **Mở đơn** trong cột Hành động hoặc bấm đúp dòng để mở đơn.
- Có nút làm mới số liệu.

Thành phần chính: các thẻ `JPanel`/`JLabel`, bảng `JTable`, nút `JButton`.

### 3.2. Công ty – `CompanyPanel`, `CompanyDialog`

- Xem, thêm, sửa, xóa công ty.
- Tìm theo tên; lọc kết hợp ngành nghề và địa điểm.
- Bấm tên công ty hoặc **Xem vị trí** để mở danh sách vị trí của công ty đó.
- Form gồm tên công ty bắt buộc, ngành nghề, địa điểm và website.
- Chặn xóa công ty đang có vị trí thực tập liên kết.

Thành phần chính: bảng `JTable`, ô tìm `JTextField`, bộ lọc `TextFilter`, form `JDialog`.

### 3.3. Vị trí thực tập – `InternshipPanel`, `InternshipDialog`

- Xem, thêm, sửa, xóa vị trí; tìm theo tên vị trí.
- Lọc theo công ty và hạn ứng tuyển: tất cả, còn hạn, sắp hết hạn trong 7 ngày, đã hết hạn hoặc chưa có hạn.
- Khi mở từ công ty, danh sách được lọc và form thêm chọn sẵn công ty đó.
- Bấm tên vị trí hoặc **Xem đơn** để mở danh sách đơn theo vị trí.
- Bấm **Ứng tuyển** để tạo đơn với vị trí chọn sẵn; nếu đã có đơn thì mở đơn hiện có.
- Form gồm công ty, tên vị trí, địa điểm, hạn ứng tuyển và mô tả. Hạn nộp dùng `DatePicker`, có thể để trống.
- Chặn xóa vị trí đã có đơn ứng tuyển.

Thành phần chính: bảng `JTable`, bộ chọn công ty `CompanySelector`, bộ lọc hạn `JComboBox`, lịch `DatePicker`, form `JDialog`.

### 3.4. Đơn ứng tuyển – `ApplicationPanel`, `ApplicationDialog`

- Xem, tạo, sửa, xóa đơn và cập nhật nhanh trạng thái.
- Tìm theo công ty, vị trí hoặc ghi chú; kết hợp bộ lọc công ty, trạng thái và ngữ cảnh vị trí được mở từ màn trước.
- Form gồm vị trí, trạng thái, ngày ứng tuyển và ghi chú. Ngày ứng tuyển dùng `DatePicker`.
- Hiển thị đủ 5 trạng thái khi thêm, sửa hoặc cập nhật nhanh.
- Có thao tác **Xem vị trí** để quay sang vị trí liên quan.
- Xuất CSV theo đúng danh sách đang hiển thị, bao gồm bộ lọc và thứ tự sắp xếp hiện tại.

Thành phần chính: bảng `JTable`, ô tìm `JTextField`, bộ chọn công ty, bộ chọn trạng thái `JComboBox`, lịch `DatePicker`, form `JDialog`, hộp chọn file `JFileChooser`.

### 3.5. Luồng sử dụng điển hình

1. Vào **Công ty**, thêm công ty cần theo dõi.
2. Bấm tên công ty để mở **Vị trí thực tập** của công ty đó.
3. Thêm vị trí và chọn hạn ứng tuyển bằng lịch.
4. Bấm **Ứng tuyển** ở dòng vị trí để mở form đơn đã chọn sẵn vị trí và trạng thái `APPLIED`.
5. Chọn ngày, ghi chú và lưu; sau đó dùng cột **Hành động** để sửa hoặc đổi trạng thái khi có kết quả.
6. Vào **Tổng quan** để theo dõi số liệu, hoặc **Đơn ứng tuyển** để lọc và xuất CSV.

Thao tác toàn màn hình như tìm, lọc, thêm, làm mới và xuất CSV nằm trên thanh trên. Thao tác cho một bản ghi nằm trong cột **Hành động** của dòng đó.

**Quay lại / Tiến tới** hỗ trợ `Alt + ←` và `Alt + →`, giữ từ khóa, bộ lọc, dòng chọn, cách sắp xếp và vị trí cuộn trong phiên chạy. Ngày trên giao diện hiển thị theo `yyyy-MM-dd`.

## 4. Service – Chức năng và quy tắc nghiệp vụ

| Service | Các method chính | Trách nhiệm |
| --- | --- | --- |
| `CompanyService` | `getAllCompanies`, `getCompanyById`, `searchCompanies`, `createCompany`, `updateCompany`, `deleteCompany` | CRUD, tìm/lọc, kiểm tra tên công ty và liên kết vị trí |
| `InternshipService` | `getAllInternships`, `getInternshipById`, `getInternshipsByCompany`, `searchInternships`, `createInternship`, `updateInternship`, `deleteInternship` | CRUD, lọc công ty/hạn nộp, kiểm tra công ty và đơn liên kết |
| `ApplicationService` | `getAllApplications`, `getApplicationById`, `searchApplications`, `findApplicationByInternship`, `getApplicationsByStatus`, `createApplication`, `updateApplication`, `updateStatus`, `deleteApplication` | CRUD, lọc kết hợp, trạng thái, ngày ứng tuyển và ngăn đơn trùng |
| `DashboardService` | `countTotalApplications`, `countSubmittedApplications`, `countApplicationsByStatus`, `getRecentApplications`, `snapshot` | Tổng hợp số liệu và danh sách gần đây |
| `ExportService` | `exportApplicationsToCsv` | Ghi danh sách đơn ra CSV UTF-8 có BOM để hỗ trợ tiếng Việt trong Excel |

### 4.1. Trạng thái ứng tuyển

| Giá trị | Hiển thị | Ý nghĩa |
| --- | --- | --- |
| `SAVED` | Đã lưu | Lưu vị trí để theo dõi, chưa ứng tuyển |
| `APPLIED` | Đã ứng tuyển | Đã nộp đơn |
| `INTERVIEW` | Phỏng vấn | Đang ở giai đoạn phỏng vấn |
| `OFFERED` | Được nhận | Nhận được kết quả tuyển dụng tích cực |
| `REJECTED` | Bị từ chối | Đơn không được chấp nhận |

Có thể chọn trực tiếp bất kỳ trạng thái nào khi tạo hoặc sửa đơn, không bắt buộc đi tuần tự; cũng có thể sửa lại trạng thái trước đó.

### 4.2. Các ràng buộc chính

- Tên công ty và tên vị trí không được để trống. Vị trí phải thuộc công ty tồn tại; đơn phải liên kết vị trí tồn tại.
- Mỗi vị trí có tối đa một đơn ứng tuyển, được kiểm tra ở Service và bằng khóa `UNIQUE` trong database.
- Không đổi vị trí của một đơn đã tạo; muốn gắn với vị trí khác cần tạo đơn mới.
- `SAVED` không có ngày ứng tuyển; chuyển về trạng thái này sẽ xóa ngày ứng tuyển.
- Khi tạo đơn ở trạng thái khác `SAVED`, hoặc chuyển khỏi `SAVED`, ngày ứng tuyển mặc định là hôm nay nếu chưa chọn.
- Các trạng thái khác `SAVED` phải có ngày ứng tuyển, từ năm 1000 và không ở tương lai.
- Hạn ứng tuyển có thể để trống hoặc là ngày trong quá khứ; nếu có thì năm phải từ 1000 đến 9999.
- Không xóa công ty khi còn vị trí; không xóa vị trí khi còn đơn. Muốn xóa toàn bộ một chuỗi dữ liệu phải xóa đơn trước, rồi vị trí, cuối cùng là công ty.

CSV gồm ID, công ty, vị trí, ngày ứng tuyển, trạng thái, ghi chú, ngày tạo và thời điểm cập nhật. Hai cột thời điểm cuối giữ cả phần giờ từ dữ liệu; quy tắc chỉ hiển thị ngày `yyyy-MM-dd` áp dụng cho giao diện.

## 5. Entity – Cấu trúc database

Database sử dụng: `internship_management`. Cấu trúc bảng được khai báo trong `schema.sql`.

### 5.1. Công ty – `companies` / `Company`

| Cột | Kiểu dữ liệu | Ràng buộc |
| --- | --- | --- |
| `id` | `BIGINT` | Khóa chính, tự tăng |
| `name` | `VARCHAR(255)` | Không NULL |
| `industry` | `VARCHAR(100)` | Có thể NULL |
| `location` | `VARCHAR(255)` | Có thể NULL |
| `website` | `VARCHAR(255)` | Có thể NULL |

### 5.2. Vị trí – `internships` / `Internship`

| Cột | Kiểu dữ liệu | Ràng buộc |
| --- | --- | --- |
| `id` | `BIGINT` | Khóa chính, tự tăng |
| `company_id` | `BIGINT` | Khóa ngoại đến `companies.id`, không NULL |
| `position` | `VARCHAR(255)` | Không NULL |
| `location` | `VARCHAR(255)` | Có thể NULL |
| `deadline` | `DATE` | Có thể NULL |
| `description` | `TEXT` | Có thể NULL |

### 5.3. Đơn – `applications` / `Application`

| Cột | Kiểu dữ liệu | Ràng buộc |
| --- | --- | --- |
| `id` | `BIGINT` | Khóa chính, tự tăng |
| `internship_id` | `BIGINT` | Khóa ngoại đến `internships.id`, không NULL, duy nhất |
| `applied_date` | `DATE` | Có thể NULL khi `SAVED` |
| `status` | `ENUM` | Không NULL; một trong 5 trạng thái nêu trên |
| `notes` | `TEXT` | Có thể NULL |
| `created_at` | `DATETIME` | Không NULL, do Service ghi nhận |
| `updated_at` | `DATETIME` | Không NULL, do Service cập nhật |

Quan hệ: một công ty có nhiều vị trí; một vị trí có thể chưa có đơn hoặc có một đơn. Cả hai khóa ngoại dùng `ON DELETE RESTRICT`.

Các trường như `companyName`, `position`, `companyId` trên kết quả truy vấn đơn được lấy từ các bảng liên kết, không phải cột bổ sung trong `applications`.

## 6. Cấu trúc mã nguồn

```text
application-management/
├── pom.xml                         # Dependency và plugin Maven
├── mvnw, mvnw.cmd                  # Maven Wrapper cho Unix/Windows
├── .mvn/wrapper/                   # Phiên bản Maven được sử dụng
├── schema.sql                      # Cấu trúc ba bảng MySQL
├── database.properties.example     # Mẫu cấu hình kết nối Java
├── compose.yaml                    # MySQL chạy bằng Docker, tùy chọn
├── .env.example                    # Mẫu biến môi trường cho Compose
└── src/main/java/com/internshipmanagement/
    ├── Main.java                   # Điểm khởi động và kết nối các module
    ├── config/                     # DatabaseConnection, ConnectionProvider
    ├── entity/                     # Company, Internship, Application và enum
    ├── dao/                        # CompanyDAO, InternshipDAO, ApplicationDAO, Jdbc
    ├── service/                    # Các Service và validation
    └── view/
        ├── MainFrame.java          # CardLayout và lịch sử điều hướng
        ├── DashboardPanel.java
        ├── CompanyPanel.java
        ├── InternshipPanel.java
        ├── ApplicationPanel.java
        ├── DataPanel.java          # Khung bảng và trạng thái trang dùng chung
        ├── DatePicker.java         # Lịch chọn ngày
        ├── TableRowActions.java    # Nút hành động theo dòng
        ├── CompanySelector.java
        ├── TextFilter.java
        ├── NavigationTarget.java
        ├── NavigableScreen.java
        ├── Refreshable.java
        ├── Ui.java                 # Tác vụ nền và thông báo
        └── dialog/                 # FormDialog, CompanyDialog,
                                    # InternshipDialog, ApplicationDialog
```

## 7. Cấu hình và chạy ứng dụng

### 7.1. Chuẩn bị

- Cài JDK 23 và đặt `JAVA_HOME` trỏ đến thư mục JDK; `java -version` cần dùng đúng JDK.
- Chuẩn bị MySQL theo một trong hai cách bên dưới: cài trực tiếp hoặc chạy Docker Compose.
- Mở terminal ở thư mục gốc dự án. Các lệnh ví dụ dùng PowerShell trên Windows.
- Không bắt buộc cài Maven riêng; lần đầu Maven Wrapper cần Internet để tải Maven và dependency.

### 7.2. Khởi tạo MySQL trực tiếp

Đăng nhập bằng tài khoản quản trị:

```powershell
mysql -u root -p
```

Trong MySQL client, chạy:

```sql
CREATE DATABASE IF NOT EXISTS internship_management
    CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE internship_management;
SOURCE schema.sql;

CREATE USER IF NOT EXISTS 'internship_app'@'localhost' IDENTIFIED BY 'change-me';
GRANT SELECT, INSERT, UPDATE, DELETE ON internship_management.*
    TO 'internship_app'@'localhost';
```

`SOURCE schema.sql` dùng đường dẫn tương đối từ thư mục đã mở MySQL client. Thay `change-me` bằng mật khẩu bạn chọn; nếu tài khoản đã tồn tại, dùng mật khẩu hiện có hoặc cập nhật nó trong MySQL.

### 7.3. Hoặc khởi tạo MySQL bằng Docker Compose

```powershell
Copy-Item .env.example .env
```

Sửa `DB_PASSWORD`, `MYSQL_ROOT_PASSWORD` trong `.env`, rồi chạy:

```powershell
docker compose up -d
docker compose ps
```

Compose tạo database `internship_management`, tài khoản `internship_app` và chạy `schema.sql` khi volume dữ liệu được khởi tạo lần đầu. Với volume đã có dữ liệu, script không tự chạy lại. Nếu cổng 3306 đã được sử dụng, đổi `MYSQL_PORT` trong `.env` và dùng cùng cổng đó trong JDBC URL.

### 7.4. Cấu hình kết nối Java

```powershell
Copy-Item database.properties.example database.properties
```

Sửa `database.properties` theo MySQL đã chuẩn bị:

```properties
db.url=jdbc:mysql://localhost:3306/internship_management?connectTimeout=5000&socketTimeout=15000&connectionTimeZone=Asia/Ho_Chi_Minh
db.user=internship_app
db.password=change-me
```

Nếu dùng Compose, `db.password` phải trùng `DB_PASSWORD` trong `.env`. Ứng dụng Java **không tự đọc `.env`**; file này dành cho Compose.

Có thể dùng các biến môi trường `DB_URL`, `DB_USER`, `DB_PASSWORD` thay cho file cấu hình. Thứ tự ưu tiên: **biến môi trường → `database.properties` → giá trị mặc định trong code**.

`database.properties` được đọc từ thư mục làm việc hiện tại, không phải tự động từ vị trí JAR. File này và `.env` đã được Git bỏ qua.

### 7.5. Build và chạy

```powershell
.\mvnw.cmd clean package
java -jar target/application-management-1.0-SNAPSHOT.jar
```

JAR được đóng gói kèm MySQL Connector/J. Chạy lệnh từ thư mục gốc để ứng dụng đọc được `database.properties` tại đó.

Hoặc chạy trực tiếp bằng Maven:

```powershell
.\mvnw.cmd compile exec:java
```

Trên Linux/macOS, thay `.\mvnw.cmd` bằng `./mvnw`. Trong IntelliJ IDEA, mở dự án Maven, chọn Project SDK 23, đặt working directory là thư mục gốc và chạy lớp `com.internshipmanagement.Main`.

### 7.6. Khi không chạy được

| Hiện tượng | Kiểm tra |
| --- | --- |
| `release version 23 not supported` hoặc lỗi phiên bản class | JDK dùng bởi Maven và Java chạy JAR phải hỗ trợ Java 23; kiểm tra `JAVA_HOME`, `java -version`, `.\mvnw.cmd -v` |
| Không kết nối được MySQL | MySQL đã chạy, đúng host/cổng trong `db.url`, container đã sẵn sàng nếu dùng Compose |
| `Access denied` | Tên tài khoản, mật khẩu, quyền truy cập và các biến `DB_*` có đang ghi đè file cấu hình hay không |
| Không tìm thấy database hoặc bảng | Tạo database và chạy `schema.sql` trước; khởi động Java không tự tạo chúng |
