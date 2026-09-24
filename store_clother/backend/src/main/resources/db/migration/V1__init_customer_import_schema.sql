-- ============================================================
-- KiotViet Fashion Clone — Database Migration
-- Customer Module + Import Receipt Module
-- MySQL 8.0
-- ============================================================

-- ============================================================
-- TABLE: customers
-- ============================================================
CREATE TABLE IF NOT EXISTS customers
(
    id             BIGINT         NOT NULL AUTO_INCREMENT PRIMARY KEY,
    version        BIGINT         NOT NULL DEFAULT 0,         -- Optimistic Lock (Quy tắc #7)
    name           VARCHAR(100)   NOT NULL,
    phone          VARCHAR(50)    NOT NULL,                   -- VARCHAR(50) để chứa cả "_deleted_TIMESTAMP" suffix
    email          VARCHAR(150),
    note           VARCHAR(500),
    loyalty_points INT            NOT NULL DEFAULT 0,
    total_spent    DECIMAL(15, 2) NOT NULL DEFAULT 0.00,      -- Quy tắc #4: DECIMAL, KHÔNG FLOAT
    is_deleted     BOOLEAN        NOT NULL DEFAULT FALSE,
    created_at     DATETIME(6)    NOT NULL,
    updated_at     DATETIME(6)    NOT NULL,

    -- 💡 Senior Note: UNIQUE chỉ hiệu lực khi is_deleted = FALSE
    -- MySQL không hỗ trợ partial unique index trực tiếp,
    -- nhưng logic append "_deleted_TIMESTAMP" đảm bảo:
    -- phone gốc (VD: "0901234567") chỉ xuất hiện 1 lần trong active records.
    -- phone đã xóa (VD: "0901234567_deleted_1718467200") không trùng với bất kỳ phone nào.
    CONSTRAINT uq_customers_phone UNIQUE (phone),

    INDEX idx_customers_phone (phone),
    INDEX idx_customers_is_deleted (is_deleted),
    INDEX idx_customers_created_at (created_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- ============================================================
-- TABLE: suppliers
-- ============================================================
CREATE TABLE IF NOT EXISTS suppliers
(
    id         BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    version    BIGINT       NOT NULL DEFAULT 0,               -- Optimistic Lock (Quy tắc #7)
    name       VARCHAR(200) NOT NULL,
    phone      VARCHAR(50),
    email      VARCHAR(150),
    address    VARCHAR(500),
    note       VARCHAR(500),
    is_active  BOOLEAN      NOT NULL DEFAULT TRUE,
    is_deleted BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at DATETIME(6)  NOT NULL,
    updated_at DATETIME(6)  NOT NULL,

    INDEX idx_suppliers_is_deleted (is_deleted),
    INDEX idx_suppliers_is_active (is_active)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- ============================================================
-- TABLE: products
-- ============================================================
CREATE TABLE IF NOT EXISTS products
(
    id          BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(200)  NOT NULL,
    description VARCHAR(2000),
    is_deleted  BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at  DATETIME(6)   NOT NULL,
    updated_at  DATETIME(6)   NOT NULL,

    INDEX idx_products_is_deleted (is_deleted)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- ============================================================
-- TABLE: product_variants
-- ============================================================
CREATE TABLE IF NOT EXISTS product_variants
(
    id         BIGINT         NOT NULL AUTO_INCREMENT PRIMARY KEY,
    version    BIGINT         NOT NULL DEFAULT 0,             -- Optimistic Lock (Quy tắc #7)
    product_id BIGINT         NOT NULL,
    sku        VARCHAR(100)   NOT NULL,                       -- VARCHAR(100) để chứa "_deleted_TIMESTAMP" suffix
    color      VARCHAR(50),
    size       VARCHAR(20),
    sell_price DECIMAL(15, 2) NOT NULL,                      -- Quy tắc #4: DECIMAL
    inventory  INT            NOT NULL DEFAULT 0,

    is_deleted BOOLEAN        NOT NULL DEFAULT FALSE,
    created_at DATETIME(6)    NOT NULL,
    updated_at DATETIME(6)    NOT NULL,

    CONSTRAINT uq_product_variants_sku UNIQUE (sku),

    -- 💡 Quy tắc #5: DB CHECK (inventory >= 0) — last line of defense
    -- Ngay cả khi code có bug cho phép inventory âm, DB sẽ reject.
    CONSTRAINT chk_product_variants_inventory_non_negative CHECK (inventory >= 0),

    CONSTRAINT fk_product_variants_product FOREIGN KEY (product_id) REFERENCES products (id),
    INDEX idx_product_variants_sku (sku),
    INDEX idx_product_variants_product_id (product_id),
    INDEX idx_product_variants_is_deleted (is_deleted)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- ============================================================
-- TABLE: orders
-- ============================================================
CREATE TABLE IF NOT EXISTS orders
(
    id           BIGINT         NOT NULL AUTO_INCREMENT PRIMARY KEY,
    order_code   VARCHAR(50)    NOT NULL,
    customer_id  BIGINT,                                      -- NULL OK: guest order
    final_amount DECIMAL(15, 2) NOT NULL,                    -- Quy tắc #4
    status       VARCHAR(20)    NOT NULL DEFAULT 'PENDING',   -- PENDING | COMPLETED | CANCELLED
    note         VARCHAR(500),
    is_deleted   BOOLEAN        NOT NULL DEFAULT FALSE,
    created_at   DATETIME(6)    NOT NULL,
    updated_at   DATETIME(6)    NOT NULL,

    CONSTRAINT uq_orders_order_code UNIQUE (order_code),

    -- 💡 Senior Note: ON DELETE SET NULL — nếu customer bị hard-deleted (không nên xảy ra)
    -- thì order vẫn tồn tại với customer_id = NULL.
    -- Với soft-delete, customer record vẫn còn → FK không bị vi phạm.
    CONSTRAINT fk_orders_customer FOREIGN KEY (customer_id) REFERENCES customers (id) ON DELETE SET NULL,
    INDEX idx_orders_customer_id (customer_id),
    INDEX idx_orders_status (status),
    INDEX idx_orders_created_at (created_at),
    INDEX idx_orders_is_deleted (is_deleted)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- ============================================================
-- TABLE: import_receipts
-- ============================================================
CREATE TABLE IF NOT EXISTS import_receipts
(
    id           BIGINT         NOT NULL AUTO_INCREMENT PRIMARY KEY,
    receipt_code VARCHAR(50)    NOT NULL,
    status       VARCHAR(20)    NOT NULL DEFAULT 'DRAFT',     -- DRAFT | COMPLETED | CANCELLED
    supplier_id  BIGINT,
    total_amount DECIMAL(15, 2) NOT NULL DEFAULT 0.00,       -- Quy tắc #4
    paid_amount  DECIMAL(15, 2) NOT NULL DEFAULT 0.00,       -- Quy tắc #4
    note         VARCHAR(500),
    created_by   VARCHAR(100),
    is_deleted   BOOLEAN        NOT NULL DEFAULT FALSE,
    created_at   DATETIME(6)    NOT NULL,
    updated_at   DATETIME(6)    NOT NULL,

    CONSTRAINT uq_import_receipts_code UNIQUE (receipt_code),
    CONSTRAINT fk_import_receipts_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers (id) ON DELETE SET NULL,

    INDEX idx_import_receipts_status (status),
    INDEX idx_import_receipts_supplier_id (supplier_id),
    INDEX idx_import_receipts_created_at (created_at),
    INDEX idx_import_receipts_is_deleted (is_deleted)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- ============================================================
-- TABLE: import_receipt_details
-- ============================================================
CREATE TABLE IF NOT EXISTS import_receipt_details
(
    id                BIGINT         NOT NULL AUTO_INCREMENT PRIMARY KEY,
    import_receipt_id BIGINT         NOT NULL,
    variant_id        BIGINT         NOT NULL,
    quantity          INT            NOT NULL,
    import_price      DECIMAL(15, 2) NOT NULL,               -- Quy tắc #4

    -- 💡 Senior Note: Không có is_deleted vì đây là value object.
    -- Lifecycle của ImportReceiptDetail gắn với ImportReceipt (orphanRemoval).
    -- Không cần audit trail riêng cho từng dòng detail.

    CONSTRAINT chk_import_details_quantity CHECK (quantity > 0),
    CONSTRAINT chk_import_details_price CHECK (import_price > 0),

    CONSTRAINT fk_import_details_receipt FOREIGN KEY (import_receipt_id) REFERENCES import_receipts (id) ON DELETE CASCADE,
    CONSTRAINT fk_import_details_variant FOREIGN KEY (variant_id) REFERENCES product_variants (id),

    INDEX idx_import_details_receipt_id (import_receipt_id),
    INDEX idx_import_details_variant_id (variant_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
