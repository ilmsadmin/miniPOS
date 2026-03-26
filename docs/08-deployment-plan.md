# 8. Kế hoạch triển khai

## 8.1 Technology Stack (Đề xuất)

### Option A: Flutter (Khuyến nghị ⭐)

| Thành phần | Công nghệ | Lý do |
|-----------|-----------|-------|
| **Framework** | Flutter 3.x | Cross-platform, hiệu suất cao, UI đẹp |
| **Language** | Dart | Type-safe, async support tốt |
| **Local DB** | Drift (SQLite) | Type-safe queries, migration support, reactive |
| **Encryption** | SQLCipher via sqflite_sqlcipher | Mã hóa DB |
| **State Management** | Riverpod | Dependency injection, testable |
| **P2P Discovery** | nearby_connections + flutter_nsd | mDNS discovery |
| **P2P Transport** | web_socket_channel | WebSocket over LAN |
| **Barcode** | mobile_scanner | Camera barcode scanning |
| **Printing** | esc_pos_bluetooth | Thermal printer support |
| **Biometric** | local_auth | Fingerprint / Face ID |
| **Google Auth** | google_sign_in | Google OAuth 2.0 |
| **Google Drive** | googleapis + googleapis_auth | Backup/Restore API |
| **Encryption** | pointycastle + encrypt | AES-256-GCM cho backup |
| **Background Tasks** | workmanager | Auto backup scheduling |
| **Architecture** | Clean Architecture | Maintainable, testable |
| **DI** | get_it + injectable | Service locator |
| **Navigation** | go_router | Declarative routing |

### Option B: React Native

| Thành phần | Công nghệ |
|-----------|-----------|
| **Framework** | React Native 0.73+ |
| **Local DB** | WatermelonDB hoặc Realm |
| **State** | Zustand + React Query |
| **P2P** | react-native-wifi-p2p + WebSocket |
| **Navigation** | React Navigation 6 |

## 8.2 Cấu trúc dự án (Flutter — Clean Architecture)

```
minipos/
├── android/
├── ios/
├── lib/
│   ├── main.dart
│   ├── app.dart
│   │
│   ├── core/                          # Core utilities
│   │   ├── constants/
│   │   │   ├── app_constants.dart
│   │   │   ├── db_constants.dart
│   │   │   └── sync_constants.dart
│   │   ├── errors/
│   │   │   ├── exceptions.dart
│   │   │   └── failures.dart
│   │   ├── utils/
│   │   │   ├── uuid_generator.dart
│   │   │   ├── hash_utils.dart
│   │   │   ├── date_utils.dart
│   │   │   ├── currency_formatter.dart
│   │   │   └── validators.dart
│   │   ├── theme/
│   │   │   ├── app_theme.dart
│   │   │   ├── app_colors.dart
│   │   │   └── app_typography.dart
│   │   └── di/
│   │       └── injection.dart
│   │
│   ├── data/                          # Data Layer
│   │   ├── database/
│   │   │   ├── app_database.dart      # Drift database definition
│   │   │   ├── tables/
│   │   │   │   ├── stores_table.dart
│   │   │   │   ├── users_table.dart
│   │   │   │   ├── categories_table.dart
│   │   │   │   ├── products_table.dart
│   │   │   │   ├── suppliers_table.dart
│   │   │   │   ├── inventory_table.dart
│   │   │   │   ├── orders_table.dart
│   │   │   │   ├── order_items_table.dart
│   │   │   │   ├── order_payments_table.dart
│   │   │   │   ├── stock_movements_table.dart
│   │   │   │   ├── purchase_orders_table.dart
│   │   │   │   ├── customers_table.dart
│   │   │   │   ├── devices_table.dart
│   │   │   │   ├── sync_log_table.dart
│   │   │   │   ├── backup_history_table.dart
│   │   │   │   └── backup_settings_table.dart
│   │   │   ├── daos/
│   │   │   │   ├── store_dao.dart
│   │   │   │   ├── user_dao.dart
│   │   │   │   ├── category_dao.dart
│   │   │   │   ├── product_dao.dart
│   │   │   │   ├── supplier_dao.dart
│   │   │   │   ├── customer_dao.dart
│   │   │   │   ├── inventory_dao.dart
│   │   │   │   ├── order_dao.dart
│   │   │   │   ├── sync_dao.dart
│   │   │   │   └── backup_dao.dart
│   │   │   └── migrations/
│   │   │       ├── migration_v1.dart
│   │   │       └── migration_v2.dart
│   │   ├── models/                    # Data models (DB entities)
│   │   │   ├── store_model.dart
│   │   │   ├── user_model.dart
│   │   │   ├── category_model.dart
│   │   │   ├── product_model.dart
│   │   │   ├── supplier_model.dart
│   │   │   ├── customer_model.dart
│   │   │   ├── order_model.dart
│   │   │   ├── sync_message_model.dart
│   │   │   ├── backup_info_model.dart
│   │   │   └── backup_settings_model.dart
│   │   ├── repositories/             # Repository implementations
│   │   │   ├── auth_repository_impl.dart
│   │   │   ├── store_repository_impl.dart
│   │   │   ├── category_repository_impl.dart
│   │   │   ├── product_repository_impl.dart
│   │   │   ├── supplier_repository_impl.dart
│   │   │   ├── customer_repository_impl.dart
│   │   │   ├── inventory_repository_impl.dart
│   │   │   ├── order_repository_impl.dart
│   │   │   ├── sync_repository_impl.dart
│   │   │   └── backup_repository_impl.dart
│   │   └── datasources/
│   │       ├── local/
│   │       │   └── shared_prefs_datasource.dart
│   │       ├── sync/
│   │       │   ├── p2p_discovery_datasource.dart
│   │       │   └── websocket_sync_datasource.dart
│   │       └── cloud/
│   │           ├── google_auth_datasource.dart
│   │           └── google_drive_datasource.dart
│   │
│   ├── domain/                        # Domain Layer
│   │   ├── entities/                  # Business entities
│   │   │   ├── store.dart
│   │   │   ├── user.dart
│   │   │   ├── category.dart
│   │   │   ├── product.dart
│   │   │   ├── product_variant.dart
│   │   │   ├── supplier.dart
│   │   │   ├── customer.dart
│   │   │   ├── inventory_item.dart
│   │   │   ├── order.dart
│   │   │   ├── order_item.dart
│   │   │   ├── cart.dart
│   │   │   ├── cart_item.dart
│   │   │   ├── stock_movement.dart
│   │   │   ├── sync_event.dart
│   │   │   ├── backup_info.dart
│   │   │   └── backup_settings.dart
│   │   ├── repositories/             # Repository interfaces
│   │   │   ├── auth_repository.dart
│   │   │   ├── store_repository.dart
│   │   │   ├── category_repository.dart
│   │   │   ├── product_repository.dart
│   │   │   ├── supplier_repository.dart
│   │   │   ├── customer_repository.dart
│   │   │   ├── inventory_repository.dart
│   │   │   ├── order_repository.dart
│   │   │   ├── sync_repository.dart
│   │   │   └── backup_repository.dart
│   │   ├── usecases/                 # Use cases
│   │   │   ├── auth/
│   │   │   │   ├── create_store.dart
│   │   │   │   ├── login.dart
│   │   │   │   ├── logout.dart
│   │   │   │   └── join_store.dart
│   │   │   ├── category/
│   │   │   │   ├── create_category.dart
│   │   │   │   ├── update_category.dart
│   │   │   │   ├── delete_category.dart
│   │   │   │   └── get_categories.dart
│   │   │   ├── product/
│   │   │   │   ├── create_product.dart
│   │   │   │   ├── update_product.dart
│   │   │   │   ├── delete_product.dart
│   │   │   │   ├── search_products.dart
│   │   │   │   └── scan_barcode.dart
│   │   │   ├── supplier/
│   │   │   │   ├── create_supplier.dart
│   │   │   │   └── get_suppliers.dart
│   │   │   ├── customer/
│   │   │   │   ├── create_customer.dart
│   │   │   │   ├── update_customer.dart
│   │   │   │   ├── delete_customer.dart
│   │   │   │   ├── get_customers.dart
│   │   │   │   └── search_customer.dart
│   │   │   ├── inventory/
│   │   │   │   ├── create_purchase_order.dart
│   │   │   │   ├── confirm_purchase_order.dart
│   │   │   │   ├── stock_check.dart
│   │   │   │   └── get_stock_overview.dart
│   │   │   ├── order/
│   │   │   │   ├── create_order.dart
│   │   │   │   ├── process_refund.dart
│   │   │   │   └── get_order_history.dart
│   │   │   ├── report/
│   │   │   │   ├── get_dashboard_data.dart
│   │   │   │   └── get_sales_report.dart
│   │   │   └── sync/
│   │   │       ├── start_sync.dart
│   │   │       ├── handle_sync_message.dart
│   │   │       └── resolve_conflict.dart
│   │   │   └── backup/
│   │   │       ├── connect_google_account.dart
│   │   │       ├── create_backup.dart
│   │   │       ├── restore_backup.dart
│   │   │       ├── get_backup_list.dart
│   │   │       ├── delete_backup.dart
│   │   │       └── schedule_auto_backup.dart
│   │   └── services/                 # Domain services
│   │       ├── cart_service.dart
│   │       ├── customer_service.dart
│   │       ├── permission_service.dart
│   │       ├── pricing_service.dart
│   │       └── backup_service.dart
│   │
│   └── presentation/                 # Presentation Layer
│       ├── router/
│       │   └── app_router.dart
│       ├── common/                   # Shared widgets
│       │   ├── widgets/
│       │   │   ├── app_bar_widget.dart
│       │   │   ├── loading_widget.dart
│       │   │   ├── error_widget.dart
│       │   │   ├── empty_state_widget.dart
│       │   │   ├── confirm_dialog.dart
│       │   │   ├── pin_input_widget.dart
│       │   │   ├── search_bar_widget.dart
│       │   │   └── sync_status_widget.dart
│       │   └── providers/
│       │       ├── auth_provider.dart
│       │       └── sync_status_provider.dart
│       ├── screens/
│       │   ├── onboarding/
│       │   │   ├── welcome_screen.dart
│       │   │   ├── create_store_screen.dart
│       │   │   └── join_store_screen.dart
│       │   ├── auth/
│       │   │   ├── login_screen.dart
│       │   │   └── login_viewmodel.dart
│       │   ├── home/
│       │   │   ├── home_screen.dart
│       │   │   ├── home_viewmodel.dart
│       │   │   └── widgets/
│       │   │       ├── function_card.dart
│       │   │       ├── summary_strip.dart
│       │   │       └── low_stock_banner.dart
│       │   ├── pos/
│       │   │   ├── pos_flow_screen.dart
│       │   │   ├── pos_viewmodel.dart
│       │   │   ├── step1_select_products_screen.dart
│       │   │   ├── step2_quantity_price_screen.dart
│       │   │   ├── step3_select_customer_screen.dart
│       │   │   ├── step4_invoice_payment_screen.dart
│       │   │   ├── step5_receipt_screen.dart
│       │   │   └── widgets/
│       │   │       ├── pos_stepper_bar.dart
│       │   │       ├── product_grid.dart
│       │   │       ├── product_card.dart
│       │   │       ├── cart_item_row.dart
│       │   │       ├── price_edit_field.dart
│       │   │       ├── customer_picker.dart
│       │   │       ├── customer_quick_form.dart
│       │   │       ├── invoice_summary.dart
│       │   │       ├── category_tabs.dart
│       │   │       ├── payment_method_selector.dart
│       │   │       └── cash_calculator.dart
│       │   ├── products/
│       │   │   ├── product_list_screen.dart
│       │   │   ├── product_detail_screen.dart
│       │   │   ├── product_form_screen.dart
│       │   │   └── product_viewmodel.dart
│       │   ├── categories/
│       │   │   ├── category_list_screen.dart
│       │   │   └── category_form_screen.dart
│       │   ├── suppliers/
│       │   │   ├── supplier_list_screen.dart
│       │   │   ├── supplier_detail_screen.dart
│       │   │   └── supplier_form_screen.dart
│       │   ├── customers/
│       │   │   ├── customer_list_screen.dart
│       │   │   ├── customer_detail_screen.dart
│       │   │   ├── customer_form_screen.dart
│       │   │   └── customer_viewmodel.dart
│       │   ├── inventory/
│       │   │   ├── stock_overview_screen.dart
│       │   │   ├── purchase_order_screen.dart
│       │   │   ├── stock_check_screen.dart
│       │   │   └── stock_movements_screen.dart
│       │   ├── orders/
│       │   │   ├── order_list_screen.dart
│       │   │   ├── order_detail_screen.dart
│       │   │   └── refund_screen.dart
│       │   ├── reports/
│       │   │   ├── sales_report_screen.dart
│       │   │   └── product_report_screen.dart
│       │   └── settings/
│       │       ├── settings_screen.dart
│       │       ├── store_settings_screen.dart
│       │       ├── device_management_screen.dart
│       │       ├── user_management_screen.dart
│       │       ├── printer_setup_screen.dart
│       │       ├── backup_restore_screen.dart
│       │       ├── backup_progress_screen.dart
│       │       ├── restore_select_screen.dart
│       │       └── backup_restore_viewmodel.dart
│       └── tablet/                   # Tablet-specific layouts
│           ├── pos_tablet_screen.dart
│           └── home_tablet_screen.dart
│
├── test/                             # Tests
│   ├── unit/
│   │   ├── services/
│   │   ├── usecases/
│   │   └── repositories/
│   ├── widget/
│   │   └── screens/
│   └── integration/
│
├── pubspec.yaml
├── analysis_options.yaml
└── README.md
```

## 8.3 Roadmap & Milestones

### Phase 1: MVP (Minimum Viable Product) — 9 tuần

**Mục tiêu:** App cơ bản, 1 thiết bị, bán hàng được, Home card-based, POS step-by-step

| Tuần | Task | Chi tiết |
|------|------|---------|
| 1 | Setup & Foundation | Cấu trúc dự án, theme, database schema, DI |
| 2 | Auth & Store | Tạo cửa hàng, đăng nhập PIN, session management |
| 3 | Home & Navigation | Home screen card-based, navigation setup, function cards |
| 4 | Product, Category & Supplier | CRUD danh mục, CRUD sản phẩm, barcode scan, CRUD nhà cung cấp |
| 5 | Customer & Inventory | CRUD khách hàng, quản lý tồn kho, nhập kho |
| 6 | POS Step 1-3 | Chọn SP (grid), chỉnh SL & giá bán, chọn/tạo khách hàng |
| 7 | POS Step 4-5 | Hoá đơn, thanh toán, in/chia sẻ receipt |
| 8 | Reports & Dashboard | Báo cáo doanh thu cơ bản, summary strip trên Home |
| 9 | Testing & Polish | Bug fix, UI polish, testing, animation |

**Deliverables Phase 1:**
- ✅ Home screen card-based (launcher chức năng)
- ✅ Tạo cửa hàng + đăng nhập PIN
- ✅ Quản lý danh mục, sản phẩm, nhà cung cấp
- ✅ Quản lý khách hàng (CRUD, tìm kiếm)
- ✅ Nhập kho cơ bản
- ✅ POS bán hàng step-by-step (5 bước)
- ✅ Chỉnh giá bán trực tiếp trong POS
- ✅ Chọn/tạo khách hàng trong POS flow
- ✅ In hóa đơn Bluetooth
- ✅ Báo cáo doanh thu hôm nay

---

### Phase 2: Multi-Device Sync — 5 tuần

**Mục tiêu:** Nhiều thiết bị đồng bộ dữ liệu

| Tuần | Task | Chi tiết |
|------|------|---------|
| 9 | P2P Discovery | mDNS discovery, device registration |
| 10 | Sync Protocol | WebSocket connection, message protocol |
| 11 | Full Sync | Initial sync cho thiết bị mới |
| 12 | Incremental Sync | Real-time sync, conflict resolution |
| 13 | Testing Sync | Test nhiều thiết bị, edge cases, stability |

**Deliverables Phase 2:**
- ✅ Tham gia cửa hàng từ thiết bị mới
- ✅ Đồng bộ dữ liệu real-time
- ✅ Xử lý conflict
- ✅ Device management UI
- ✅ Sync status indicator

---

### Phase 3: Advanced Features + Cloud Backup — 6 tuần

**Mục tiêu:** Tính năng nâng cao, backup Google Drive, hoàn thiện

| Tuần | Task | Chi tiết |
|------|------|---------|
| 14 | Advanced POS | Biến thể SP, giảm giá, trả hàng |
| 15 | Advanced Inventory | Kiểm kho, xuất kho, stock movements |
| 16 | Reports & Export | Báo cáo chi tiết, xuất Excel/PDF |
| 17 | Security | Mã hóa DB (SQLCipher), audit log |
| 18 | Google Drive Backup | Google Sign-In, OAuth, backup thủ công, mã hóa AES-256, upload/download |
| 19 | Auto Backup & Restore | Auto backup scheduling, restore flow, backup management UI |

**Deliverables Phase 3:**
- ✅ Biến thể sản phẩm
- ✅ Giảm giá đơn hàng/sản phẩm
- ✅ Trả hàng / hoàn tiền
- ✅ Kiểm kho
- ✅ Báo cáo chi tiết + xuất file
- ✅ Database encryption
- ✅ Kết nối Google Account
- ✅ Backup thủ công lên Google Drive (AES-256 encrypted)
- ✅ Backup tự động (daily/weekly/monthly)
- ✅ Restore từ Google Drive
- ✅ Restore trên thiết bị mới (Welcome Screen)
- ✅ Quản lý bản backup (xem, xóa, tải về)

---

### Phase 4: Polish & Release — 3 tuần

| Tuần | Task | Chi tiết |
|------|------|---------|
| 20 | UI/UX Polish | Animation, responsive tablet, dark mode |
| 21 | Performance | Optimize queries, lazy loading, caching |
| 22 | Release | Beta testing, bug fix, store submission |

---

## 8.4 Ước lượng effort

### Team size tối thiểu

| Vai trò | Số lượng | Ghi chú |
|---------|---------|---------|
| Mobile Developer (Flutter) | 2 | 1 senior + 1 mid |
| UI/UX Designer | 1 | Part-time sau Phase 1 |
| QA Tester | 1 | Part-time, focus Phase 2-4 |
| **Tổng** | **3-4** | |

### Timeline tổng

```
Phase 1 (MVP):           9 tuần  █████████
Phase 2 (Multi-device):  5 tuần          █████
Phase 3 (Advanced+BK):   6 tuần               ██████
Phase 4 (Release):       3 tuần                     ███
                         ──────────────────────────────
                         Tổng: ~23 tuần (~5.75 tháng)
```

## 8.5 Tiêu chí nghiệm thu (Acceptance Criteria)

### Performance Benchmarks

| Metric | Target |
|--------|--------|
| App startup time | < 2 giây |
| POS: thêm SP vào giỏ | < 100ms |
| POS: thanh toán hoàn tất | < 500ms |
| Tìm kiếm sản phẩm | < 200ms (10,000 SP) |
| Sync incremental | < 1 giây cho 100 records |
| Sync full (1,000 SP) | < 30 giây |
| Database size (10,000 SP, 50,000 orders) | < 100MB |
| RAM usage | < 200MB |
| Battery drain (POS active) | < 5%/giờ |
| Backup (10,000 SP, 50,000 orders) | < 60 giây (WiFi) |
| Restore (10,000 SP, 50,000 orders) | < 90 giây (WiFi) |
| Backup file size (10,000 SP) | < 30MB (sau nén + mã hóa) |

### Quality Metrics

| Metric | Target |
|--------|--------|
| Crash-free rate | > 99.5% |
| Unit test coverage | > 70% |
| Integration test coverage | > 50% |
| Sync reliability | > 99% (không mất dữ liệu) |

## 8.6 Rủi ro & Giải pháp

| Rủi ro | Xác suất | Ảnh hưởng | Giải pháp |
|--------|---------|----------|-----------|
| P2P sync phức tạp hơn dự kiến | Cao | Cao | Ưu tiên simple sync trước, improve iterative |
| Conflict resolution khó chính xác | Trung bình | Cao | Sử dụng CRDT cho inventory, LWW cho phần còn lại |
| Hiệu suất kém với dữ liệu lớn | Thấp | Trung bình | Index optimization, pagination, lazy loading |
| Bluetooth printer không tương thích | Trung bình | Thấp | Test nhiều dòng printer phổ biến, fallback share image |
| Mạng WiFi không ổn định | Cao | Trung bình | Auto reconnect, offline queue, batch sync |
| Bảo mật SQLCipher ảnh hưởng performance | Thấp | Thấp | Benchmark sớm, có option tắt encryption |
| Google API thay đổi / rate limit | Thấp | Trung bình | Sử dụng official SDK, theo dõi changelog |
| User quên mật khẩu backup | Trung bình | Cao | Cảnh báo rõ ràng, gợi ý ghi nhớ, không có recovery |
| Mất mạng khi đang backup/restore | Trung bình | Thấp | Resumable upload/download, auto retry |

## 8.7 Công cụ phát triển

| Công cụ | Mục đích |
|---------|---------|
| **VS Code / Android Studio** | IDE |
| **Git + GitHub** | Version control |
| **GitHub Actions** | CI/CD |
| **Figma** | UI/UX Design |
| **Firebase Crashlytics** | Crash reporting (optional) |
| **Fastlane** | Automate build & deploy |
| **DB Browser for SQLite** | Debug database |

## 8.8 Hướng phát triển tương lai (Post-MVP)

| Feature | Mô tả | Ưu tiên |
|---------|-------|---------|
| iCloud Backup | Backup lên iCloud cho user iOS | P2 |
| Customer Loyalty | Tích điểm, khuyến mãi cho khách hàng thân thiết | P2 |
| Multi-store | 1 Owner quản lý nhiều cửa hàng | P3 |
| Analytics | Phân tích doanh thu nâng cao, AI suggestion | P3 |
| E-commerce Integration | Đồng bộ với Shopee, Lazada | P3 |
| Cloud Sync | Đồng bộ qua internet (không cùng WiFi) | P2 |
| Expense Tracking | Quản lý chi phí, thu chi | P2 |
| Multi-language | Hỗ trợ đa ngôn ngữ | P3 |
| Dark Mode | Giao diện tối | P2 |
| Promotion / Coupon | Mã khuyến mãi, combo | P2 |

---

*Tài liệu này sẽ được cập nhật thường xuyên trong quá trình phát triển.*
