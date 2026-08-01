-- Flyway Migration V2: Seed Cities
INSERT INTO cities (name, state, pincode) VALUES
('Mumbai', 'Maharashtra', '400001'),
('Delhi', 'Delhi', '110001'),
('Bengaluru', 'Karnataka', '560001'),
('Hyderabad', 'Telangana', '500001'),
('Chennai', 'Tamil Nadu', '600001'),
('Kolkata', 'West Bengal', '700001'),
('Pune', 'Maharashtra', '411001'),
('Ahmedabad', 'Gujarat', '380001')
ON CONFLICT (name) DO NOTHING;
