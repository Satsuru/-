INSERT INTO analytics.sales_facts (id, hotel_id, status, amount, occurred_on) VALUES
    (1, 1, 'PAID', 308.88, DATE '2026-06-01'),
    (2, 1, 'CREATED', 312.00, DATE '2026-06-10');

INSERT INTO analytics.occupancy_facts (id, hotel_id, stay_date, sold_nights, total_nights) VALUES
    (1, 1, DATE '2026-06-01', 1, 2);
