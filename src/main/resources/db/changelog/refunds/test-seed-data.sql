-- Test data for refunds database (integration tests)

-- Refund for Test Case 4 (links to RC-TEST-0005)
INSERT INTO refunds (id, date_created, date_updated, amount, reason, refund_status, reference, payment_reference, created_by, updated_by, ccd_case_number, refund_instruction_type)
VALUES (1, '2024-01-20 10:00:00', '2024-01-20 10:00:00', 50.00, 'Overpayment', 'Approved', 'RF-TEST-0001', 'RC-TEST-0005', 'system', 'admin', '1000000000000004', 'AUTOMATED');

-- Two refunds for Test Case 6 payment (one to be deleted) - linked to RC-TEST-0008 which is the duplicate payment
INSERT INTO refunds (id, date_created, date_updated, amount, reason, refund_status, reference, payment_reference, created_by, updated_by, ccd_case_number, refund_instruction_type)
VALUES (2, '2024-01-21 11:00:00', '2024-01-21 11:00:00', 25.00, 'Duplicate charge', 'Approved', 'RF-TEST-0002', 'RC-TEST-0008', 'system', 'admin', '1000000000000006', 'AUTOMATED');
