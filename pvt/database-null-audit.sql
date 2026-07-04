-- PVT: kiểm kê dữ liệu cũ bị NULL. File này chỉ đọc, không UPDATE hoặc DELETE.

SELECT month_event_id, month_event_description, month_event_deadline,
       month_event_status, response_status, last_time_response,
       registered_day, is_all, is_hidden, is_deleted
FROM month_event
WHERE month_event_description IS NULL
   OR month_event_deadline IS NULL
   OR month_event_status IS NULL
   OR response_status IS NULL
   OR last_time_response IS NULL
   OR registered_day IS NULL
   OR is_all IS NULL
   OR is_hidden IS NULL
   OR is_deleted IS NULL
ORDER BY month_event_id;

SELECT id_response, month_event_id, event_information_response,
       created_by_user, created_by_date
FROM response_event_information
WHERE month_event_id IS NULL
   OR event_information_response IS NULL
   OR created_by_user IS NULL
   OR created_by_date IS NULL
ORDER BY id_response;

SELECT account_name, name
FROM account
WHERE name IS NULL OR TRIM(name) = ''
ORDER BY account_name;

SELECT id, manager_username, account_name, display_order
FROM user_tab_order
WHERE manager_username IS NULL
   OR TRIM(manager_username) = ''
   OR account_name IS NULL
   OR TRIM(account_name) = ''
   OR display_order IS NULL
ORDER BY id;
