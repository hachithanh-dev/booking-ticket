-- V1__init_schema.sql
-- Concert Ticket Booking Platform - Initial Schema

CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    full_name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE concerts (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    venue VARCHAR(255) NOT NULL,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_concert_status
        CHECK (status IN ('DRAFT', 'PUBLISHED', 'CANCELLED', 'ENDED')),
    CONSTRAINT chk_concert_time
        CHECK (start_time < end_time)
);

CREATE TABLE ticket_categories (
    id UUID PRIMARY KEY,
    concert_id UUID NOT NULL REFERENCES concerts(id),
    name VARCHAR(100) NOT NULL,
    price_amount BIGINT NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'VND',
    total_quantity INT NOT NULL,
    available_quantity INT NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_ticket_category_name_per_concert
        UNIQUE (concert_id, name),
    CONSTRAINT chk_ticket_category_status
        CHECK (status IN ('ACTIVE', 'INACTIVE', 'SOLD_OUT')),
    CONSTRAINT chk_ticket_category_price
        CHECK (price_amount >= 0),
    CONSTRAINT chk_ticket_category_quantity
        CHECK (
            total_quantity >= 0
            AND available_quantity >= 0
            AND available_quantity <= total_quantity
        )
);

CREATE TABLE vouchers (
    id UUID PRIMARY KEY,
    code VARCHAR(100) NOT NULL UNIQUE,
    discount_type VARCHAR(50) NOT NULL,
    discount_value BIGINT NOT NULL,
    max_discount_amount BIGINT,
    max_redemptions INT NOT NULL,
    used_count INT NOT NULL DEFAULT 0,
    starts_at TIMESTAMP NOT NULL,
    ends_at TIMESTAMP NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_voucher_status
        CHECK (status IN ('ACTIVE', 'INACTIVE', 'EXPIRED')),
    CONSTRAINT chk_voucher_discount_type
        CHECK (discount_type IN ('FIXED_AMOUNT', 'PERCENTAGE')),
    CONSTRAINT chk_voucher_discount_value
        CHECK (discount_value >= 0),
    CONSTRAINT chk_voucher_max_discount_amount
        CHECK (max_discount_amount IS NULL OR max_discount_amount >= 0),
    CONSTRAINT chk_voucher_percentage_value
        CHECK (
            discount_type <> 'PERCENTAGE'
            OR discount_value <= 100
        ),
    CONSTRAINT chk_voucher_redemptions
        CHECK (
            max_redemptions >= 0
            AND used_count >= 0
            AND used_count <= max_redemptions
        ),
    CONSTRAINT chk_voucher_time
        CHECK (starts_at < ends_at)
);

CREATE TABLE bookings (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    concert_id UUID NOT NULL REFERENCES concerts(id),
    voucher_id UUID REFERENCES vouchers(id),
    client_request_id VARCHAR(100) NOT NULL,
    business_fingerprint VARCHAR(128) NOT NULL,
    status VARCHAR(50) NOT NULL,
    subtotal_amount BIGINT NOT NULL,
    discount_amount BIGINT NOT NULL DEFAULT 0,
    total_amount BIGINT NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'VND',
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_booking_user_client_request
        UNIQUE (user_id, client_request_id),
    CONSTRAINT chk_booking_status
        CHECK (status IN ('PENDING', 'CONFIRMED', 'CANCELLED', 'FAILED', 'EXPIRED')),
    CONSTRAINT chk_booking_amounts
        CHECK (
            subtotal_amount >= 0
            AND discount_amount >= 0
            AND total_amount >= 0
            AND total_amount = subtotal_amount - discount_amount
        )
);

CREATE TABLE booking_items (
    id UUID PRIMARY KEY,
    booking_id UUID NOT NULL REFERENCES bookings(id) ON DELETE CASCADE,
    ticket_category_id UUID NOT NULL REFERENCES ticket_categories(id),
    quantity INT NOT NULL,
    unit_price_amount BIGINT NOT NULL,
    total_amount BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_booking_item_category
        UNIQUE (booking_id, ticket_category_id),
    CONSTRAINT chk_booking_item_quantity
        CHECK (quantity > 0),
    CONSTRAINT chk_booking_item_amount
        CHECK (
            unit_price_amount >= 0
            AND total_amount = quantity * unit_price_amount
        )
);

CREATE TABLE voucher_redemptions (
    id UUID PRIMARY KEY,
    voucher_id UUID NOT NULL REFERENCES vouchers(id),
    user_id UUID NOT NULL REFERENCES users(id),
    booking_id UUID NOT NULL REFERENCES bookings(id),
    redeemed_at TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_voucher_user
        UNIQUE (voucher_id, user_id),
    CONSTRAINT uq_voucher_redemption_booking
        UNIQUE (booking_id)
);

-- Partial unique index: prevent duplicate PENDING bookings with same fingerprint
CREATE UNIQUE INDEX idx_uq_booking_fingerprint_pending
ON bookings (business_fingerprint)
WHERE status = 'PENDING';

-- Performance index: worker finds expired PENDING bookings efficiently
CREATE INDEX idx_bookings_worker_expiry
ON bookings (status, expires_at)
WHERE status = 'PENDING';

-- Performance indexes for API queries
CREATE INDEX idx_concerts_status ON concerts (status);
CREATE INDEX idx_ticket_categories_concert_id ON ticket_categories (concert_id);
CREATE INDEX idx_bookings_user_id ON bookings (user_id);
CREATE INDEX idx_bookings_concert_id ON bookings (concert_id);
CREATE INDEX idx_vouchers_code ON vouchers (code);
