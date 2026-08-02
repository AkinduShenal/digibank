# DigiBank Requirements Traceability

This matrix maps the supplied bank-system specification to the implemented Spring Boot modules.

| Requirement area | Implementation | Main routes | Database objects |
|---|---|---|---|
| Customer and account CRUD | Registration, profile, staff verification and soft-delete lifecycle | `/open-account`, `/customer/profile`, `/staff/customers` | `users`, `customers`, `bank_accounts` |
| Beneficiaries | Customer CRUD, internal verification, staff external verification/rejection and limits | `/customer/beneficiaries`, `/staff/beneficiaries` | `beneficiaries` |
| Fund transfers | Own-account, direct DigiBank and beneficiary transfers; locking, receipts, ledger and audited compensating reversal | `/customer/transfers`, `/staff/transfers` | `fund_transfers`, `account_transactions` |
| Scheduled payments | One-time/monthly transfers and bills, edit/cancel, automatic processing | `/customer/schedules` | `scheduled_payments` |
| Loans | Personal, home, vehicle, education and business; secure document upload, edit/withdraw, decision and repayment | `/customer/loans`, `/staff/loans` | `loan_applications`, `loan_repayment_schedule` |
| Cards | Debit/credit, activation, block, limits, lost/stolen, cancellation and expiry | `/customer/cards`, `/staff/cards` | `payment_cards` |
| Bills/payees | Electricity, water, mobile and internet bills, saved-biller create/edit/remove and receipts | `/customer/bill-payments`, `/staff/bill-payments` | `saved_billers`, `bill_payments` |
| Governance | Roles, encrypted card PAN, password reset, safe errors and paginated audit viewer | `/login`, `/forgot-password`, `/admin/dashboard` | `audit_logs`, `password_reset_tokens` |

## Status vocabulary mapping

- Loan `PENDING_REVIEW` = pending; `DISBURSED` = active; `CLOSED` = completed.
- Banking deletes use `DEACTIVATED`, `CANCELLED`, `CLOSED` or `DELETED` states so financial history remains intact.
- Completed transactions are immutable. Corrections require a compensating transaction, not ledger deletion.
- High-volume customer, transfer, loan, card, bill and audit views provide search/filter controls and bounded pages.

## Academic simulations

- External-bank and utility-provider settlement are simulated because no live APIs were supplied.
- Local development can display a password-reset link. Production must disable this and use approved email/SMS delivery.
