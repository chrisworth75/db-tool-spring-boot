-- Schema for payments database (Testcontainers)

CREATE SCHEMA IF NOT EXISTS public;

-- payment_fee_link table
CREATE TABLE IF NOT EXISTS public.payment_fee_link (
    id BIGSERIAL PRIMARY KEY,
    date_created TIMESTAMP NOT NULL,
    date_updated TIMESTAMP NOT NULL,
    payment_reference VARCHAR(50) NOT NULL,
    org_id VARCHAR(20),
    enterprise_service_name VARCHAR(255),
    ccd_case_number VARCHAR(25) NOT NULL,
    case_reference VARCHAR(25),
    service_request_callback_url TEXT
);

-- fee table
CREATE TABLE IF NOT EXISTS public.fee (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50),
    version VARCHAR(10),
    payment_link_id BIGINT,
    calculated_amount DECIMAL(19, 2),
    volume INTEGER,
    ccd_case_number VARCHAR(25),
    reference VARCHAR(255),
    net_amount DECIMAL(19, 2),
    fee_amount DECIMAL(19, 2),
    amount_due DECIMAL(19, 2),
    date_created TIMESTAMP,
    date_updated TIMESTAMP
);

-- payment table
CREATE TABLE IF NOT EXISTS public.payment (
    id BIGSERIAL PRIMARY KEY,
    amount DECIMAL(19, 2),
    case_reference VARCHAR(255),
    ccd_case_number VARCHAR(25),
    currency VARCHAR(10),
    date_created TIMESTAMP,
    date_updated TIMESTAMP,
    description TEXT,
    service_type VARCHAR(100),
    site_id VARCHAR(50),
    user_id VARCHAR(255),
    payment_channel VARCHAR(50),
    payment_method VARCHAR(50),
    payment_provider VARCHAR(50),
    payment_status VARCHAR(50),
    payment_link_id BIGINT,
    customer_reference VARCHAR(255),
    external_reference VARCHAR(255),
    organisation_name VARCHAR(255),
    pba_number VARCHAR(50),
    reference VARCHAR(255),
    giro_slip_no VARCHAR(50),
    s2s_service_name VARCHAR(100),
    reported_date_offline TIMESTAMP,
    service_callback_url TEXT,
    document_control_number VARCHAR(100),
    banked_date TIMESTAMP,
    payer_name VARCHAR(255),
    internal_reference VARCHAR(255)
);

-- remission table
CREATE TABLE IF NOT EXISTS public.remission (
    id BIGSERIAL PRIMARY KEY,
    fee_id BIGINT,
    hwf_reference VARCHAR(50),
    hwf_amount DECIMAL(19, 2),
    beneficiary_name VARCHAR(255),
    ccd_case_number VARCHAR(25),
    case_reference VARCHAR(255),
    payment_link_id BIGINT,
    site_id VARCHAR(50),
    date_created TIMESTAMP,
    date_updated TIMESTAMP,
    remission_reference VARCHAR(50)
);

-- fee_pay_apportion table
CREATE TABLE IF NOT EXISTS public.fee_pay_apportion (
    id BIGSERIAL PRIMARY KEY,
    payment_id BIGINT,
    fee_id BIGINT,
    payment_link_id BIGINT,
    fee_amount DECIMAL(19, 2),
    payment_amount DECIMAL(19, 2),
    apportion_amount DECIMAL(19, 2),
    ccd_case_number VARCHAR(25),
    apportion_type VARCHAR(50),
    call_surplus_amount DECIMAL(19, 2),
    created_by VARCHAR(255),
    date_created TIMESTAMP,
    date_updated TIMESTAMP
);

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_pfl_ccd ON public.payment_fee_link(ccd_case_number);
CREATE INDEX IF NOT EXISTS idx_fee_ccd ON public.fee(ccd_case_number);
CREATE INDEX IF NOT EXISTS idx_fee_link ON public.fee(payment_link_id);
CREATE INDEX IF NOT EXISTS idx_payment_ccd ON public.payment(ccd_case_number);
CREATE INDEX IF NOT EXISTS idx_payment_link ON public.payment(payment_link_id);
CREATE INDEX IF NOT EXISTS idx_payment_ref ON public.payment(reference);
CREATE INDEX IF NOT EXISTS idx_remission_ccd ON public.remission(ccd_case_number);
CREATE INDEX IF NOT EXISTS idx_remission_fee ON public.remission(fee_id);
CREATE INDEX IF NOT EXISTS idx_apportion_ccd ON public.fee_pay_apportion(ccd_case_number);
CREATE INDEX IF NOT EXISTS idx_apportion_payment ON public.fee_pay_apportion(payment_id);