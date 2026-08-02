# DigiBank Design Document

## Architecture

DigiBank uses layered Spring MVC. Controllers call transactional services, services enforce rules through repositories, and Flyway owns MySQL schema changes. Spring Security protects routes by role.

```mermaid
flowchart LR
    C[Customer] --> MVC[Spring MVC + Thymeleaf]
    S[Staff/Admin] --> MVC
    MVC --> SEC[Spring Security]
    SEC --> SV[Transactional services]
    JOB[Scheduled jobs] --> SV
    SV --> JPA[Spring Data JPA]
    JPA --> DB[(MySQL)]
    F[Flyway] --> DB
```

## Use cases

```mermaid
flowchart TB
    Customer --> Register
    Customer --> TransferFunds
    Customer --> SchedulePayments
    Customer --> PayBills
    Customer --> ApplyAndRepayLoan
    Customer --> ManageCards
    Staff --> VerifyCustomersAndBeneficiaries
    Staff --> ReviewLoansAndCards
    Admin --> Staff
    Admin --> ReviewAuditLog
```

## Core class view

```mermaid
classDiagram
    User "1" --> "0..1" Customer
    Customer "1" --> "1..*" BankAccount
    Customer "1" --> "*" Beneficiary
    Customer "1" --> "*" LoanApplication
    Customer "1" --> "*" PaymentCard
    Customer "1" --> "*" ScheduledPayment
    BankAccount "1" --> "*" AccountTransaction
    LoanApplication "1" --> "*" LoanRepaymentSchedule
```

## Transfer activity

```mermaid
flowchart TD
    A[Submit transfer] --> B{Session and PIN valid?}
    B -- No --> X[Reject safely]
    B -- Yes --> C{Accounts and recipient eligible?}
    C -- No --> X
    C -- Yes --> D[Lock account rows]
    D --> E{Balance and limit valid?}
    E -- No --> X
    E -- Yes --> F[Update balances]
    F --> G[Write receipt, ledger, audit and notifications]
    G --> H[Commit]
```

## Security and ethics

- Passwords/PINs use BCrypt. Card PAN uses AES-256-GCM; HMAC enforces uniqueness and normal views use only the last four digits.
- Secrets come from environment variables. CSRF remains enabled and account ownership is rechecked server-side.
- Stable row-lock ordering protects concurrent balance changes. Audit logs omit PINs, passwords and full sensitive identifiers.
- Safe error pages hide SQL and stack traces. Financial history is preserved rather than destructively deleted.
- Production additionally requires TLS, managed secrets, encrypted backups, retention rules, monitoring and privacy/accessibility review.

## Sprint evidence

1. Customer/account foundation.
2. Beneficiaries and staff verification.
3. Transfers, ledger, statements and notifications.
4. Loans and repayments.
5. Cards and bill payments.
6. Scheduling, security hardening, admin audit, testing and documentation.

