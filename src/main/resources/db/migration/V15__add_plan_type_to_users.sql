-- Add plan_type to users table with default value 'FREE'
ALTER TABLE users ADD COLUMN plan_type VARCHAR(20) DEFAULT 'FREE';