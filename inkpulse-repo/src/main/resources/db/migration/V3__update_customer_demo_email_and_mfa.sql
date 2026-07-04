-- V3: Update email and enable MFA for customer_demo
UPDATE users
SET email = 'trunghien765@gmail.com', mfa_enabled = TRUE
WHERE user_name = 'customer_demo';
