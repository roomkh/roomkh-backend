# 45. Admin Profile & Notifications API

## 1. Overview
This module handles the top-right header section of the Admin Dashboard, specifically providing the logged-in admin's profile information and their unread notifications (alerts/bells).

## 2. Database Changes (Migration)
A new `notifications` table is introduced to store system alerts for users.

```sql
CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_notification_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_notifications_user_id ON notifications(user_id);
CREATE INDEX idx_notifications_is_read ON notifications(is_read);