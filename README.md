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
