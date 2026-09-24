-- ============================================================
-- KiotViet Fashion Clone — Migration V2
-- Order Items, Stock History, Users, Roles, User Roles
-- MySQL 8.0
-- ============================================================

-- ============================================================
-- TABLE: roles
-- ============================================================
CREATE TABLE IF NOT EXISTS roles
(
    id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(50)  NOT NULL,
    description VARCHAR(200),

    CONSTRAINT uq_roles_name UNIQUE (name)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- Seed data: 4 roles cố định
INSERT IGNORE INTO roles (name, description)
VALUES ('ROLE_OWNER', 'Chủ cửa hàng — toàn quyền'),
       ('ROLE_MANAGER', 'Quản lý — quản lý kho, nhân viên, báo cáo'),
       ('ROLE_CASHIER', 'Thu ngân — tạo hóa đơn, quản lý khách hàng'),
       ('ROLE_WAREHOUSE_STAFF', 'Nhân viên kho — quản lý phiếu nhập, tồn kho');

-- ============================================================
-- TABLE: users
-- ============================================================
CREATE TABLE IF NOT EXISTS users
(
    id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    username        VARCHAR(100) NOT NULL,                    -- VARCHAR(100) để chứa _deleted_ suffix
    password_hash   VARCHAR(255) NOT NULL,
    full_name       VARCHAR(100) NOT NULL,
    email           VARCHAR(200),                             -- VARCHAR(200) để chứa _deleted_ suffix
    phone           VARCHAR(50),
    status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',   -- ACTIVE | INACTIVE | LOCKED
    last_login_at   DATETIME(6),                              -- Cập nhật khi login thành công

    is_deleted      BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at      DATETIME(6)  NOT NULL,
    updated_at      DATETIME(6)  NOT NULL,

    -- 💡 UNIQUE phải giữ nguyên vì @SQLDelete sẽ rename trước khi soft-delete
    CONSTRAINT uq_users_username UNIQUE (username),
    CONSTRAINT uq_users_email UNIQUE (email),

    INDEX idx_users_username (username),
    INDEX idx_users_status (status),
    INDEX idx_users_is_deleted (is_deleted),
    INDEX idx_users_last_login_at (last_login_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- ============================================================
-- TABLE: user_roles (join table ManyToMany)
-- ============================================================
CREATE TABLE IF NOT EXISTS user_roles
(
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,

    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- ============================================================
-- TABLE: order_items
-- ============================================================
CREATE TABLE IF NOT EXISTS order_items
(
    id         BIGINT         NOT NULL AUTO_INCREMENT PRIMARY KEY,
    order_id   BIGINT         NOT NULL,
    variant_id BIGINT         NOT NULL,
    quantity   INT            NOT NULL,
    sell_price DECIMAL(15, 2) NOT NULL,                      -- snapshot giá tại thời điểm mua
    line_total DECIMAL(15, 2) NOT NULL,                      -- = quantity * sell_price

    -- 💡 Không có is_deleted: OrderItem không bao giờ bị xóa riêng lẻ
    -- Lifecycle gắn với Order aggregate

    CONSTRAINT chk_order_items_quantity CHECK (quantity > 0),
    CONSTRAINT chk_order_items_price CHECK (sell_price > 0),
    CONSTRAINT chk_order_items_line_total CHECK (line_total > 0),

    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_order_items_variant FOREIGN KEY (variant_id) REFERENCES product_variants (id),

    INDEX idx_order_items_order_id (order_id),
    INDEX idx_order_items_variant_id (variant_id),
    -- 💡 Index kết hợp (order_id, variant_id ASC) hỗ trợ query sort trong cancelOrder
    INDEX idx_order_items_order_variant (order_id, variant_id ASC)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- ============================================================
-- TABLE: stock_histories
-- ============================================================
CREATE TABLE IF NOT EXISTS stock_histories
(
    id               BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    variant_id       BIGINT       NOT NULL,
    transaction_type VARCHAR(20)  NOT NULL,                   -- IMPORT | EXPORT | ADJUSTMENT
    change_quantity  INT          NOT NULL,                   -- Dương = nhập/hoàn, Âm = xuất
    balance_before   INT          NOT NULL,
    balance_after    INT          NOT NULL,
    reference_code   VARCHAR(100),                            -- IMP-xxx, ORD-xxx, CANCEL-ORD-xxx
    note             VARCHAR(500),
    created_by       VARCHAR(100),
    created_at       DATETIME(6)  NOT NULL,

    -- 💡 Senior Note: KHÔNG có is_deleted, updated_at, is_deleted.
    -- StockHistory là immutable append-only log (Quy tắc #6).
    -- Không bao giờ UPDATE hay DELETE bản ghi này.

    -- 💡 Verify constraint: balance_before + change_quantity = balance_after
    CONSTRAINT chk_stock_history_balance
        CHECK (balance_before + change_quantity = balance_after),

    CONSTRAINT chk_stock_history_balance_after_non_negative
        CHECK (balance_after >= 0),

    CONSTRAINT fk_stock_histories_variant FOREIGN KEY (variant_id) REFERENCES product_variants (id),

    INDEX idx_stock_histories_variant_id (variant_id),
    INDEX idx_stock_histories_reference_code (reference_code),
    INDEX idx_stock_histories_created_at (created_at),
    INDEX idx_stock_histories_transaction_type (transaction_type)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- ============================================================
-- ALTER TABLE orders: thêm column created_by, đổi status
-- ============================================================

-- Thêm created_by nếu chưa có
ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS created_by VARCHAR(100) AFTER note;

-- Thêm discount_amount nếu chưa có
ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS discount_amount DECIMAL(15, 2) NOT NULL DEFAULT 0.00 AFTER final_amount;

-- Index cho created_by (USR-03 stats)
ALTER TABLE orders
    ADD INDEX IF NOT EXISTS idx_orders_created_by (created_by);
