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

The system is being developed module by module by six members.

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
