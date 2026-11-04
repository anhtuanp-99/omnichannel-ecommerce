INSERT INTO roles (id, code, name) VALUES
                                       ('00000000-0000-0000-0000-000000000001', 'CUSTOMER',        'Customer'),
                                       ('00000000-0000-0000-0000-000000000002', 'ADMIN',           'Administrator'),
                                       ('00000000-0000-0000-0000-000000000003', 'CATALOG_STAFF',   'Catalog Staff'),
                                       ('00000000-0000-0000-0000-000000000004', 'INVENTORY_STAFF', 'Inventory Staff'),
                                       ('00000000-0000-0000-0000-000000000005', 'ORDER_STAFF',     'Order Staff'),
                                       ('00000000-0000-0000-0000-000000000006', 'FINANCE_STAFF',   'Finance Staff')
    ON CONFLICT (code) DO NOTHING;