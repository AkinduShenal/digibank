# DigiBank

DigiBank is a university group project for building a digital banking web application module by module.

## Technology Stack

- Java 21
- Spring Boot 4.0.7
- Maven
- Spring MVC
- Spring Data JPA
- Spring Security
- Bean Validation
- Thymeleaf
- HTML, Tailwind CSS, custom CSS, and vanilla JavaScript
- MySQL

## Database

Default database name: `digibank_db`

Do not commit real database usernames, passwords, or secret override files.

## Build

```bash
./mvnw clean compile
```

## Run

```bash
sh mvnw spring-boot:run
```

On macOS, `sh mvnw` also works when Gatekeeper blocks direct execution of the downloaded Maven wrapper.

## Optional Development Staff And Admin Accounts

No passwords are stored in source control. To create development-only staff and admin users, set the following
environment variables before starting the application:

```bash
export DIGIBANK_DEV_SEED_ENABLED=true
export DIGIBANK_DEV_STAFF_PASSWORD='choose-a-strong-local-password'
export DIGIBANK_DEV_ADMIN_PASSWORD='choose-another-strong-local-password'
sh mvnw spring-boot:run
```

The default development usernames are `staff` and `admin`. Existing users are never overwritten. The seeder is
disabled outside the `dev` profile and remains disabled unless `DIGIBANK_DEV_SEED_ENABLED=true` is supplied.

New customer logins remain disabled while the customer is `PENDING_VERIFICATION`. Activating the initial account
from the staff console verifies the customer, changes both records to `ACTIVE`, and enables login.

External beneficiaries start as `PENDING` and are reviewed from `/staff/beneficiaries`. Internal DigiBank
beneficiaries are verified automatically from the bank's account records. Transfer limits must be between LKR 0.01
and LKR 1,000,000.00.

## Customer Fund Transfers

Customers can send money from `/customer/transfers/new`, view their own transfer history at
`/customer/transfers`, and open a receipt using its transfer reference. Every transfer requires an active LKR source
account, sufficient available funds, and the customer's four-digit transaction PIN. The saved-beneficiary route
also requires an active verified beneficiary and an amount within that beneficiary's limit.

Customers may also make a one-time internal transfer without saving a beneficiary. After the complete 12-digit
DigiBank account number is entered, the protected lookup confirms the eligible account holder's name. Partial,
invalid, inactive and own-account lookups do not expose customer details. The backend repeats every account check
when the transfer is submitted, and the direct recipient is not added to the beneficiary list.

Internal transfers atomically debit the source account and credit the destination DigiBank account. External
transfers currently use a simulated successful settlement because the project has no external banking network;
they debit the source account and retain a complete local transfer record. Account rows are locked in a consistent
order while transferring to protect balances from concurrent updates. Customers can access only their own transfer
records, and PINs and full account numbers are never written to audit logs.

Flyway migration `V6__create_fund_transfers.sql` creates the transfer table and
`V7__allow_direct_internal_transfers.sql` enables one-time DigiBank recipients automatically when the application
is restarted against MySQL/MariaDB. Team members only need to create an empty `digibank_db`, configure their own
local database credentials, and start the application; tables should not be created manually in phpMyAdmin.

## Customer Account Statements And Notifications

Every completed transfer now creates an immutable account-ledger entry. Internal transfers create a debit entry
for the sender and a credit entry for the recipient in the same database transaction; external transfers create the
sender's debit entry. Customers can view their account statement at `/customer/transactions`, filter it by account,
direction, date range or keyword, open a transaction detail page, and download the filtered statement as CSV or PDF.
All statement and detail queries are restricted to accounts owned by the signed-in customer.

Senders receive a debit notification for every completed transfer, and recipients of internal transfers receive an
incoming-transfer notification. Notifications can be viewed and marked as read at `/customer/notifications`, and
the customer dashboard shows recent ledger activity. Flyway migration
`V8__create_account_transactions_and_notifications.sql` creates the ledger and notification tables and backfills
ledger entries for existing completed transfers when the application restarts. Team members should restart the app
after pulling this migration; they should not create these tables manually in phpMyAdmin.

The system is being developed module by module by six members.

## Loan Management

Active customers can submit a loan application from `/customer/loans/new` for an eligible LKR account. The module
supports personal, education, home and business loans, validates amounts from LKR 50,000 to LKR 5,000,000, allows
terms from 6 to 60 months, calculates a reducing-balance monthly installment, and limits that installment to 40% of
the customer's declared monthly income. Customers can track the review status and view the repayment schedule.

Bank staff and administrators review applications at `/staff/loans`. Approval and disbursement happen atomically:
the account is locked, the approved amount is credited, a loan-disbursement ledger entry is written, the repayment
schedule is generated, and the customer receives a notification. Rejections require a reason and also notify the
customer. Both decisions are recorded in the audit log, and applications can only be decided once. Flyway migration
`V9__create_loan_management.sql` creates the loan tables and extends account-ledger transaction types automatically.

Customers with a disbursed loan can pay the next unpaid installment from the loan detail page. Repayments require a
four-digit transaction PIN, an active customer-owned LKR account and sufficient available/current balances. The
payment locks both the loan installment and debit account, creates a unique `LRP` reference, writes a
`LOAN_REPAYMENT` debit to the account statement, updates the schedule, sends a notification and writes an audit log
in one transaction. Missed due dates are marked overdue when loan records are viewed, installments must be paid in
order, and the loan closes automatically after the final installment. Flyway migration
`V10__add_loan_repayment_payments.sql` adds the payment reference, debit account and balance snapshot fields.

## Card Management

Active customers can request debit or credit cards for their own active LKR accounts from `/customer/cards`.
Duplicate open requests for the same account and card type are prevented. Bank staff and administrators review
requests at `/staff/cards`; approval creates a unique Luhn-valid card number and five-year expiry date, while
rejection requires a reason. Full card numbers are never exposed in either portal—the UI uses masked card details.

Approved cards begin as inactive. Customers activate them using their four-digit transaction PIN, can immediately
block an active card, and can reactivate cards they blocked themselves. Cards blocked by bank staff require a staff
review before reactivation. Staff can also block active or inactive issued cards with a documented reason. Every
request and status change creates an audit record, and approval, rejection, activation, blocking and reactivation
send customer notifications. Flyway migration `V11__create_card_management.sql` creates the card table when the
application restarts; team members should not create the table manually in phpMyAdmin.

## Bill Payments

Active customers can pay CEB, LECO, NWSDB, mobile and internet bills from `/customer/bill-payments/new`.
Payments support saved billers or one-time service references and require an active customer-owned LKR account,
sufficient current and available balances, an amount from LKR 10.00 to LKR 1,000,000.00, and the customer's
four-digit transaction PIN. A completed payment atomically debits the account, writes a `BILL_PAYMENT` statement
entry, creates a customer notification and records an audit event. Customers can view payment history and receipts,
manage saved billers, and can never access another customer's receipt or saved biller.

Bank staff and administrators can monitor the latest payments at `/staff/bill-payments`. Provider settlement is
simulated because this academic project has no live utility-provider API; the local debit and banking records are
fully completed. Flyway migration `V12__create_bill_payments.sql` creates the bill-payment and saved-biller tables
and extends the account-ledger transaction types automatically when the application restarts.

## Member 1 Customer And Account CRUD Mapping

Member 1 owns the customer registration, login, profile, customer dashboard and account lifecycle foundation.

- Create: customer registration creates a `User`, `Customer` and initial `BankAccount` through the backend service layer. Public account numbers and customer numbers are generated server-side, not in JavaScript.
- Read: customers can read only their own profile, dashboard and account details. Bank staff and admins can read customer records through the staff console.
- Update: customers may update permitted personal/contact/address fields only. Staff/admin users may update account type only before activation and may perform valid account status transitions through dedicated service methods.
- Delete: Member 1 uses soft deletion. Account deactivation sets account/customer status and disables login where required. Account closure preserves the database record and records a closure reason.
- Reactivation: staff/admin users may reactivate eligible deactivated accounts. Closed accounts cannot be reactivated.

## Member 1 Account Status Transitions

Allowed transitions:

- `PENDING_ACTIVATION -> ACTIVE`
- `ACTIVE -> FROZEN`
- `FROZEN -> ACTIVE`
- `ACTIVE -> DEACTIVATED`
- `FROZEN -> DEACTIVATED`
- `ACTIVE -> CLOSED`
- `FROZEN -> CLOSED`
- `DEACTIVATED -> ACTIVE` when approved

Rejected transitions:

- `CLOSED -> ACTIVE`
- `CLOSED -> FROZEN`
- `PENDING_ACTIVATION -> FROZEN`
- Any arbitrary status assignment submitted from a form

Closure requires a zero available balance, zero current balance, a documented reason, and ownership by the selected customer record. All staff/admin lifecycle actions write a safe audit log without passwords, transaction PINs or full identity values.
