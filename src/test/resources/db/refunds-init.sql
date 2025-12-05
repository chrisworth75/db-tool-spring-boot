-- Combined schema and test data for refunds database (Testcontainers)

-- ============================================
-- SCHEMA
-- ============================================

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

-- Create indexes
CREATE INDEX idx_refunds_payment_ref ON public.refunds(payment_reference);
CREATE INDEX idx_refunds_ccd ON public.refunds(ccd_case_number);


-- ============================================
-- TEST DATA
-- ============================================

-- Refund for Test Case 4 (links to RC-TEST-0005)
INSERT INTO public.refunds (id, date_created, date_updated, amount, reason, refund_status, reference, payment_reference, created_by, updated_by, ccd_case_number, refund_instruction_type)
VALUES (1, '2024-01-20 10:00:00', '2024-01-20 10:00:00', 50.00, 'Overpayment', 'Approved', 'RF-TEST-0001', 'RC-TEST-0005', 'system', 'admin', '1000000000000004', 'AUTOMATED');

-- Two refunds for Test Case 6 payment (one to be deleted) - linked to RC-TEST-0008 which is the duplicate payment
INSERT INTO public.refunds (id, date_created, date_updated, amount, reason, refund_status, reference, payment_reference, created_by, updated_by, ccd_case_number, refund_instruction_type)
VALUES (2, '2024-01-21 11:00:00', '2024-01-21 11:00:00', 25.00, 'Duplicate charge', 'Approved', 'RF-TEST-0002', 'RC-TEST-0008', 'system', 'admin', '1000000000000006', 'AUTOMATED');