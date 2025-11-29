-- Test data for refunds database

-- Refund for Test Case 4
INSERT INTO public.refunds (id, date_created, date_updated, amount, reason, refund_status, reference, payment_reference, created_by, updated_by, ccd_case_number, refund_instruction_type)
VALUES (1, '2024-01-20 10:00:00', '2024-01-20 10:00:00', 50.00, 'Overpayment', 'Approved', 'RF-TEST-0001', 'RC-TEST-0005', 'system', 'admin', '1000000000000004', 'AUTOMATED');

-- Reset sequence
SELECT setval('refunds_id_seq', 100);