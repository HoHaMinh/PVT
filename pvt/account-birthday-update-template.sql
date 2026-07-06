-- Điền ngày sinh một lần cho các account hiện có. Định dạng bắt buộc DD/MM, không có năm.
-- Hãy thay account_name và ngày mẫu trước khi chạy từng câu UPDATE.

SELECT account_name, name, birthday
FROM account
ORDER BY name, account_name;

-- Ví dụ:
-- UPDATE account SET birthday = '08/03' WHERE account_name = 'account.can.cap.nhat';

-- Kiểm tra lại sau khi cập nhật:
SELECT account_name, name, birthday
FROM account
WHERE birthday IS NOT NULL AND TRIM(birthday) <> ''
ORDER BY birthday, name;
