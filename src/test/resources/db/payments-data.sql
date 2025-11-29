-- Test data for payments database

-- Test Case 1: Simple case with one service request, one fee, one payment (fully paid)
INSERT INTO public.payment_fee_link (id, date_created, date_updated, payment_reference, org_id, enterprise_service_name, ccd_case_number, case_reference)
VALUES (1, '2024-01-15 10:00:00', '2024-01-15 10:00:00', 'PAY-TEST-001', 'ORG001', 'Civil Money Claims', '1000000000000001', 'REF-001');

INSERT INTO public.fee (id, code, version, payment_link_id, calculated_amount, volume, ccd_case_number, reference, net_amount, fee_amount, amount_due, date_created, date_updated)
VALUES (1, 'FEE0001', '1', 1, 100.00, 1, '1000000000000001', 'Application fee', 100.00, 100.00, 0.00, '2024-01-15 10:00:00', '2024-01-15 10:00:00');

INSERT INTO public.payment (id, amount, ccd_case_number, currency, date_created, date_updated, payment_channel, payment_method, payment_provider, payment_status, payment_link_id, reference)
VALUES (1, 100.00, '1000000000000001', 'GBP', '2024-01-15 10:00:00', '2024-01-15 10:00:00', 'online', 'card', 'gov pay', 'success', 1, 'RC-TEST-0001');

INSERT INTO public.fee_pay_apportion (id, payment_id, fee_id, payment_link_id, fee_amount, payment_amount, apportion_amount, ccd_case_number, apportion_type, date_created, date_updated)
VALUES (1, 1, 1, 1, 100.00, 100.00, 100.00, '1000000000000001', 'AUTO', '2024-01-15 10:00:00', '2024-01-15 10:00:00');


-- Test Case 2: Case with multiple fees and payments
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


-- Test Case 4: Case with refund (payment ref for refund lookup)
INSERT INTO public.payment_fee_link (id, date_created, date_updated, payment_reference, org_id, enterprise_service_name, ccd_case_number, case_reference)
VALUES (4, '2024-01-19 10:00:00', '2024-01-19 10:00:00', 'PAY-TEST-004', 'ORG003', 'Probate', '1000000000000004', 'REF-004');

INSERT INTO public.fee (id, code, version, payment_link_id, calculated_amount, volume, ccd_case_number, reference, net_amount, fee_amount, amount_due, date_created, date_updated)
VALUES (5, 'FEE0005', '1', 4, 300.00, 1, '1000000000000004', 'Probate fee', 300.00, 300.00, 0.00, '2024-01-19 10:00:00', '2024-01-19 10:00:00');

INSERT INTO public.payment (id, amount, ccd_case_number, currency, date_created, date_updated, payment_channel, payment_method, payment_provider, payment_status, payment_link_id, reference, payer_name)
VALUES (5, 300.00, '1000000000000004', 'GBP', '2024-01-19 10:00:00', '2024-01-19 10:00:00', 'online', 'card', 'gov pay', 'success', 4, 'RC-TEST-0005', 'Jane Doe');

INSERT INTO public.fee_pay_apportion (id, payment_id, fee_id, payment_link_id, fee_amount, payment_amount, apportion_amount, ccd_case_number, apportion_type, date_created, date_updated)
VALUES (5, 5, 5, 4, 300.00, 300.00, 300.00, '1000000000000004', 'AUTO', '2024-01-19 10:00:00', '2024-01-19 10:00:00');


-- Reset sequences to avoid conflicts
SELECT setval('payment_fee_link_id_seq', 100);
SELECT setval('fee_id_seq', 100);
SELECT setval('payment_id_seq', 100);
SELECT setval('remission_id_seq', 100);
SELECT setval('fee_pay_apportion_id_seq', 100);