-- Combined schema and test data for payments database (Testcontainers)

-- ============================================
-- SCHEMA
-- ============================================

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

-- Create indexes
CREATE INDEX idx_pfl_ccd ON public.payment_fee_link(ccd_case_number);
CREATE INDEX idx_fee_ccd ON public.fee(ccd_case_number);
CREATE INDEX idx_fee_link ON public.fee(payment_link_id);
CREATE INDEX idx_payment_ccd ON public.payment(ccd_case_number);
CREATE INDEX idx_payment_link ON public.payment(payment_link_id);
CREATE INDEX idx_payment_ref ON public.payment(reference);
CREATE INDEX idx_remission_ccd ON public.remission(ccd_case_number);
CREATE INDEX idx_remission_fee ON public.remission(fee_id);
CREATE INDEX idx_apportion_ccd ON public.fee_pay_apportion(ccd_case_number);
CREATE INDEX idx_apportion_payment ON public.fee_pay_apportion(payment_id);


-- ============================================
-- TEST DATA
-- ============================================

-- Test Case 1: Simple case with one service request, one fee, one payment (fully paid)
INSERT INTO public.payment_fee_link (id, date_created, date_updated, payment_reference, org_id, enterprise_service_name, ccd_case_number, case_reference)
VALUES (1, '2024-01-15 10:00:00', '2024-01-15 10:00:00', 'PAY-TEST-001', 'ORG001', 'Civil Money Claims', '1000000000000001', 'REF-001');

INSERT INTO public.fee (id, code, version, payment_link_id, calculated_amount, volume, ccd_case_number, reference, net_amount, fee_amount, amount_due, date_created, date_updated)
VALUES (1, 'FEE0001', '1', 1, 100.00, 1, '1000000000000001', 'Application fee', 100.00, 100.00, 0.00, '2024-01-15 10:00:00', '2024-01-15 10:00:00');

INSERT INTO public.payment (id, amount, ccd_case_number, currency, date_created, date_updated, payment_channel, payment_method, payment_provider, payment_status, payment_link_id, reference)
VALUES (1, 100.00, '1000000000000001', 'GBP', '2024-01-15 10:00:00', '2024-01-15 10:00:00', 'online', 'card', 'gov pay', 'success', 1, 'RC-TEST-0001');

INSERT INTO public.fee_pay_apportion (id, payment_id, fee_id, payment_link_id, fee_amount, payment_amount, apportion_amount, ccd_case_number, apportion_type, date_created, date_updated)
VALUES (1, 1, 1, 1, 100.00, 100.00, 100.00, '1000000000000001', 'AUTO', '2024-01-15 10:00:00', '2024-01-15 10:00:00');


-- Test Case 2: Case with multiple fees and payments (partial payment)
INSERT INTO public.payment_fee_link (id, date_created, date_updated, payment_reference, org_id, enterprise_service_name, ccd_case_number, case_reference)
VALUES (2, '2024-01-16 10:00:00', '2024-01-16 10:00:00', 'PAY-TEST-002', 'ORG002', 'Family Public Law', '1000000000000002', 'REF-002');

INSERT INTO public.fee (id, code, version, payment_link_id, calculated_amount, volume, ccd_case_number, reference, net_amount, fee_amount, amount_due, date_created, date_updated)
VALUES (2, 'FEE0002', '1', 2, 150.00, 1, '1000000000000002', 'Hearing fee', 150.00, 150.00, 50.00, '2024-01-16 10:00:00', '2024-01-16 10:00:00'),
       (3, 'FEE0003', '1', 2, 75.00, 1, '1000000000000002', 'Court fee', 75.00, 75.00, 0.00, '2024-01-16 10:00:00', '2024-01-16 10:00:00');

INSERT INTO public.payment (id, amount, ccd_case_number, currency, date_created, date_updated, payment_channel, payment_method, payment_provider, payment_status, payment_link_id, reference)
VALUES (2, 100.00, '1000000000000002', 'GBP', '2024-01-16 10:00:00', '2024-01-16 10:00:00', 'online', 'card', 'gov pay', 'success', 2, 'RC-TEST-0002'),
       (3, 75.00, '1000000000000002', 'GBP', '2024-01-17 10:00:00', '2024-01-17 10:00:00', 'online', 'payment by account', 'pba', 'success', 2, 'RC-TEST-0003');

INSERT INTO public.fee_pay_apportion (id, payment_id, fee_id, payment_link_id, fee_amount, payment_amount, apportion_amount, ccd_case_number, apportion_type, date_created, date_updated)
VALUES (2, 2, 2, 2, 150.00, 100.00, 100.00, '1000000000000002', 'AUTO', '2024-01-16 10:00:00', '2024-01-16 10:00:00'),
       (3, 3, 3, 2, 75.00, 75.00, 75.00, '1000000000000002', 'AUTO', '2024-01-17 10:00:00', '2024-01-17 10:00:00');


-- Test Case 3: Case with remission (Help with Fees)
INSERT INTO public.payment_fee_link (id, date_created, date_updated, payment_reference, org_id, enterprise_service_name, ccd_case_number, case_reference)
VALUES (3, '2024-01-18 10:00:00', '2024-01-18 10:00:00', 'PAY-TEST-003', 'ORG001', 'Divorce', '1000000000000003', 'REF-003');

INSERT INTO public.fee (id, code, version, payment_link_id, calculated_amount, volume, ccd_case_number, reference, net_amount, fee_amount, amount_due, date_created, date_updated)
VALUES (4, 'FEE0004', '1', 3, 200.00, 1, '1000000000000003', 'Divorce fee', 150.00, 200.00, 0.00, '2024-01-18 10:00:00', '2024-01-18 10:00:00');

INSERT INTO public.remission (id, fee_id, hwf_reference, hwf_amount, beneficiary_name, ccd_case_number, payment_link_id, date_created, date_updated)
VALUES (1, 4, 'HWF-123-456', 50.00, 'John Smith', '1000000000000003', 3, '2024-01-18 10:00:00', '2024-01-18 10:00:00');

INSERT INTO public.payment (id, amount, ccd_case_number, currency, date_created, date_updated, payment_channel, payment_method, payment_provider, payment_status, payment_link_id, reference)
VALUES (4, 150.00, '1000000000000003', 'GBP', '2024-01-18 10:00:00', '2024-01-18 10:00:00', 'online', 'card', 'gov pay', 'success', 3, 'RC-TEST-0004');

INSERT INTO public.fee_pay_apportion (id, payment_id, fee_id, payment_link_id, fee_amount, payment_amount, apportion_amount, ccd_case_number, apportion_type, date_created, date_updated)
VALUES (4, 4, 4, 3, 200.00, 150.00, 150.00, '1000000000000003', 'AUTO', '2024-01-18 10:00:00', '2024-01-18 10:00:00');


-- Test Case 4: Case with refund
INSERT INTO public.payment_fee_link (id, date_created, date_updated, payment_reference, org_id, enterprise_service_name, ccd_case_number, case_reference)
VALUES (4, '2024-01-19 10:00:00', '2024-01-19 10:00:00', 'PAY-TEST-004', 'ORG003', 'Probate', '1000000000000004', 'REF-004');

INSERT INTO public.fee (id, code, version, payment_link_id, calculated_amount, volume, ccd_case_number, reference, net_amount, fee_amount, amount_due, date_created, date_updated)
VALUES (5, 'FEE0005', '1', 4, 300.00, 1, '1000000000000004', 'Probate fee', 300.00, 300.00, 0.00, '2024-01-19 10:00:00', '2024-01-19 10:00:00');

INSERT INTO public.payment (id, amount, ccd_case_number, currency, date_created, date_updated, payment_channel, payment_method, payment_provider, payment_status, payment_link_id, reference, payer_name)
VALUES (5, 300.00, '1000000000000004', 'GBP', '2024-01-19 10:00:00', '2024-01-19 10:00:00', 'online', 'card', 'gov pay', 'success', 4, 'RC-TEST-0005', 'Jane Doe');

INSERT INTO public.fee_pay_apportion (id, payment_id, fee_id, payment_link_id, fee_amount, payment_amount, apportion_amount, ccd_case_number, apportion_type, date_created, date_updated)
VALUES (5, 5, 5, 4, 300.00, 300.00, 300.00, '1000000000000004', 'AUTO', '2024-01-19 10:00:00', '2024-01-19 10:00:00');


-- Test Case 5: MESSY CASE with duplicate service requests (main PATCH use case)
-- This simulates a case where the same fee/payment was accidentally created under two service requests
INSERT INTO public.payment_fee_link (id, date_created, date_updated, payment_reference, org_id, enterprise_service_name, ccd_case_number, case_reference)
VALUES (5, '2024-01-20 10:00:00', '2024-01-20 10:00:00', 'PAY-TEST-005', 'ORG001', 'Civil Money Claims', '1000000000000005', 'REF-005'),
       (6, '2024-01-20 10:01:00', '2024-01-20 10:01:00', 'PAY-TEST-006', 'ORG001', 'Civil Money Claims', '1000000000000005', 'REF-005');

-- First service request has the "real" fee and payment
INSERT INTO public.fee (id, code, version, payment_link_id, calculated_amount, volume, ccd_case_number, reference, net_amount, fee_amount, amount_due, date_created, date_updated)
VALUES (6, 'FEE0006', '1', 5, 500.00, 1, '1000000000000005', 'Issue fee', 500.00, 500.00, 0.00, '2024-01-20 10:00:00', '2024-01-20 10:00:00');

INSERT INTO public.payment (id, amount, ccd_case_number, currency, date_created, date_updated, payment_channel, payment_method, payment_provider, payment_status, payment_link_id, reference)
VALUES (6, 500.00, '1000000000000005', 'GBP', '2024-01-20 10:00:00', '2024-01-20 10:00:00', 'online', 'card', 'gov pay', 'success', 5, 'RC-TEST-0006');

INSERT INTO public.fee_pay_apportion (id, payment_id, fee_id, payment_link_id, fee_amount, payment_amount, apportion_amount, ccd_case_number, apportion_type, date_created, date_updated)
VALUES (6, 6, 6, 5, 500.00, 500.00, 500.00, '1000000000000005', 'AUTO', '2024-01-20 10:00:00', '2024-01-20 10:00:00');

-- Second service request is the DUPLICATE (empty, to be deleted)
-- No fees or payments attached - this is a needless duplicate service request


-- Test Case 6: Case with TWO service requests where BOTH have data but one should be deleted
INSERT INTO public.payment_fee_link (id, date_created, date_updated, payment_reference, org_id, enterprise_service_name, ccd_case_number, case_reference)
VALUES (7, '2024-01-21 10:00:00', '2024-01-21 10:00:00', 'PAY-TEST-007', 'ORG002', 'Family Public Law', '1000000000000006', 'REF-006'),
       (8, '2024-01-21 10:01:00', '2024-01-21 10:01:00', 'PAY-TEST-008', 'ORG002', 'Family Public Law', '1000000000000006', 'REF-006');

-- First service request
INSERT INTO public.fee (id, code, version, payment_link_id, calculated_amount, volume, ccd_case_number, reference, net_amount, fee_amount, amount_due, date_created, date_updated)
VALUES (7, 'FEE0007', '1', 7, 250.00, 1, '1000000000000006', 'Court fee', 250.00, 250.00, 0.00, '2024-01-21 10:00:00', '2024-01-21 10:00:00');

INSERT INTO public.payment (id, amount, ccd_case_number, currency, date_created, date_updated, payment_channel, payment_method, payment_provider, payment_status, payment_link_id, reference)
VALUES (7, 250.00, '1000000000000006', 'GBP', '2024-01-21 10:00:00', '2024-01-21 10:00:00', 'online', 'card', 'gov pay', 'success', 7, 'RC-TEST-0007');

INSERT INTO public.fee_pay_apportion (id, payment_id, fee_id, payment_link_id, fee_amount, payment_amount, apportion_amount, ccd_case_number, apportion_type, date_created, date_updated)
VALUES (7, 7, 7, 7, 250.00, 250.00, 250.00, '1000000000000006', 'AUTO', '2024-01-21 10:00:00', '2024-01-21 10:00:00');

-- Second service request (DUPLICATE with its own fee/payment that should be deleted)
INSERT INTO public.fee (id, code, version, payment_link_id, calculated_amount, volume, ccd_case_number, reference, net_amount, fee_amount, amount_due, date_created, date_updated)
VALUES (8, 'FEE0007', '1', 8, 250.00, 1, '1000000000000006', 'Court fee', 250.00, 250.00, 0.00, '2024-01-21 10:01:00', '2024-01-21 10:01:00');

INSERT INTO public.payment (id, amount, ccd_case_number, currency, date_created, date_updated, payment_channel, payment_method, payment_provider, payment_status, payment_link_id, reference)
VALUES (8, 250.00, '1000000000000006', 'GBP', '2024-01-21 10:01:00', '2024-01-21 10:01:00', 'online', 'card', 'gov pay', 'success', 8, 'RC-TEST-0008');

INSERT INTO public.fee_pay_apportion (id, payment_id, fee_id, payment_link_id, fee_amount, payment_amount, apportion_amount, ccd_case_number, apportion_type, date_created, date_updated)
VALUES (8, 8, 8, 8, 250.00, 250.00, 250.00, '1000000000000006', 'AUTO', '2024-01-21 10:01:00', '2024-01-21 10:01:00');


-- Test Case 7: Case with multiple remissions on same fee (one to delete)
INSERT INTO public.payment_fee_link (id, date_created, date_updated, payment_reference, org_id, enterprise_service_name, ccd_case_number, case_reference)
VALUES (9, '2024-01-22 10:00:00', '2024-01-22 10:00:00', 'PAY-TEST-009', 'ORG001', 'Divorce', '1000000000000007', 'REF-007');

INSERT INTO public.fee (id, code, version, payment_link_id, calculated_amount, volume, ccd_case_number, reference, net_amount, fee_amount, amount_due, date_created, date_updated)
VALUES (9, 'FEE0009', '1', 9, 400.00, 1, '1000000000000007', 'Divorce fee', 300.00, 400.00, 0.00, '2024-01-22 10:00:00', '2024-01-22 10:00:00');

-- Two remissions on the same fee - one is a duplicate
INSERT INTO public.remission (id, fee_id, hwf_reference, hwf_amount, beneficiary_name, ccd_case_number, payment_link_id, date_created, date_updated)
VALUES (2, 9, 'HWF-AAA-001', 50.00, 'Alice Brown', '1000000000000007', 9, '2024-01-22 10:00:00', '2024-01-22 10:00:00'),
       (3, 9, 'HWF-AAA-002', 50.00, 'Alice Brown', '1000000000000007', 9, '2024-01-22 10:01:00', '2024-01-22 10:01:00');

INSERT INTO public.payment (id, amount, ccd_case_number, currency, date_created, date_updated, payment_channel, payment_method, payment_provider, payment_status, payment_link_id, reference)
VALUES (9, 300.00, '1000000000000007', 'GBP', '2024-01-22 10:00:00', '2024-01-22 10:00:00', 'online', 'card', 'gov pay', 'success', 9, 'RC-TEST-0009');

INSERT INTO public.fee_pay_apportion (id, payment_id, fee_id, payment_link_id, fee_amount, payment_amount, apportion_amount, ccd_case_number, apportion_type, date_created, date_updated)
VALUES (9, 9, 9, 9, 400.00, 300.00, 300.00, '1000000000000007', 'AUTO', '2024-01-22 10:00:00', '2024-01-22 10:00:00');