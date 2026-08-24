INSERT INTO amenities (code, name, created_at, updated_at) VALUES
                                                               ('AIR_CONDITIONING', 'Air Conditioning', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                                                               ('WIFI', 'WiFi', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                                                               ('BALCONY', 'Balcony', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                                                               ('PARKING', 'Parking', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                                                               ('SECURITY_24H', '24/7 Security', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
    ON CONFLICT (code) DO NOTHING;