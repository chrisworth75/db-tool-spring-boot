-- Schema for refunds database (Testcontainers)

CREATE SCHEMA IF NOT EXISTS public;

-- refunds table
CREATE TABLE IF NOT EXISTS public.refunds (
    id BIGSERIAL PRIMARY KEY,
    date_created TIMESTAMP,
    date_updated TIMESTAMP,
    amount DECIMAL(19, 2),
    reason TEXT,
    refund_status VARCHAR(50),
    reference VARCHAR(50),
    payment_reference VARCHAR(50),
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    ccd_case_number VARCHAR(25),
    fee_ids TEXT,
    notification_sent_flag VARCHAR(10),
    contact_details TEXT,
    service_type VARCHAR(100),
    refund_instruction_type VARCHAR(50)
);

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_refunds_payment_ref ON public.refunds(payment_reference);
CREATE INDEX IF NOT EXISTS idx_refunds_ccd ON public.refunds(ccd_case_number);