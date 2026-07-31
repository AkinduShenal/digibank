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
./mvnw spring-boot:run
```

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
