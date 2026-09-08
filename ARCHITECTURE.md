# KhataGo architecture

KhataGo is a single-module Android app written entirely in Kotlin with Jetpack
Compose. There is no DI framework, no repository-of-repositories abstraction and
no network layer: the graph is small enough to read end to end, and every
dependency is visible at its call site.

## Layers

```
UI (Compose)  ->  ViewModels  ->  Repositories  ->  Room (source of truth)
                                        |
                                  domain/ (pure Kotlin: money + finance rules)
```

- **`core/`** — cross-cutting helpers: `Money` (minor units), `KhataGoTime`,
  `Outcome`, `AppContainer` and `TodayProvider`.
- **`domain/`** — pure Kotlin. Models (`Accounts.kt`, `Enums.kt`) and the finance
  engine. Nothing here touches Android, so it is unit-testable on the JVM.
- **`data/`** — Room database, DAOs, repositories, backup and export.
- **`ui/`** — design system (`components/`), navigation and screens.

## Money rule

Every amount in KhataGo is a `Long` in **minor units** (paisa). `100.50 BDT` is
stored as `10050`. No `Float` or `Double` ever holds money, at any layer, from the
text field to the PDF.

- `Money.parse()` turns user text (`1,250.75`, `৳ 1250.5`) into minor units and
  returns `null` for anything unusable, so the UI shows a sentence instead of a
  stack trace.
- `Money.splitEvenly(total, n)` distributes the remainder one paisa at a time, so
  the parts **always** add back up to exactly the total.
- Quantities are held in *thousandths* (`1.5 kg -> 1500`) for the same reason.
- `Money.safeAdd/Subtract/Multiply` clamp on overflow instead of wrapping.

## Accounting rules

These are the rules every screen, report and chart follows:

```
income  − expense = net cash flow
original + charges − valid payments = remaining
```

- Only `INCOME` counts as income. Only `EXPENSE` counts as expense.
- A **shop credit purchase** creates an obligation (`SHOP_CREDIT`); it is not an
  expense until the user records one.
- **Borrowing** money is not income — it is a payable. **Lending** is not an
  expense — it is a receivable.
- **Payments and repayments settle liabilities.** They are tracked as payments and
  never inflate income or expense totals.
- A **loan or EMI purchase** creates a schedule of installments; payments reduce
  installments, and the balance is always recomputed from the payments.

## The finance engine (`domain/finance`)

| Object | Responsibility |
| --- | --- |
| `ScheduleGenerator` | Turns (first due date, count, frequency, total) into real installments. Monthly schedules advance with `java.time`, so month ends, leap years and year changes never drift. |
| `AllocationEngine` | Decides which installment a payment lands on. A targeted installment is filled first, then the remainder spills into the earliest open installment. Shop payments are FIFO across purchases. |
| `PaymentValidator` | The single gate for every payment: rejects zero, negative, over-limit and already-settled payments, and carries the user-facing copy. |
| `BalanceEngine` | `remaining`, `paidPercent`, `netCashFlow`. Balances never go negative. |
| `InstallmentStatusEngine` | Derives `PAID / PARTIALLY_PAID / OVERDUE / DUE_TODAY / UPCOMING` from stored data, so a fully paid installment can never sit in "Overdue". |
| `DueEngine` | Classifies a due date as `OVERDUE / DUE_TODAY / DUE_SOON / SCHEDULED / SETTLED / UNSCHEDULED`. |
| `InsightEngine` | Turns the month's numbers into at most four short, honest sentences. |

## Persistence

Room, schema version 1, 21 entities, `exportSchema = true`, and **no destructive
migration**: migrations are written as explicit SQL and the builder never calls
`fallbackToDestructiveMigration`.

Every write that also touches the central ledger runs inside a Room transaction,
so the activity history can never drift from the accounts that produced it. Each
derived ledger row records `originType` + `originId`, which makes edits and deletes
idempotent: the old row is deleted by origin before the new one is written.

Indexed columns: foreign keys, `dateEpochDay`, `dueDateEpochDay`, `yearMonth`
(`year * 100 + month`) and search columns.

## Concurrency

Repositories expose `kotlinx.coroutines.flow.Flow`; ViewModels convert them to
`StateFlow` with `stateIn(..., WhileSubscribed(5_000), ...)`. Writes happen on
`viewModelScope` (or `Dispatchers.IO` inside repositories). The reminder scan runs
on WorkManager with a daily periodic request.

## Navigation

`ui/navigation/Destination.kt` holds every route; `KhataGoNavHost.kt` is the only
place that maps routes to screens. Five routes own the bottom bar
(`dashboard`, `accounts`, `transactions`, `reports`, `settings`); everything else
pushes on top with a back arrow.

Files leave the device only through the Android Storage Access Framework
(`ActivityResultContracts.CreateDocument` / `OpenDocument`): CSV, PDF and backup.

## Backup format

A backup is a single JSON file (`application/json`, extension `.khbackup`) holding
`backupVersion`, `schemaVersion`, `appVersion`, `exportedAt`, `currencyCode` and
every table's rows. `BackupRepository` validates the whole file — versions,
duplicate ids, referential integrity — **before** the current database is touched,
so a damaged or newer file can never leave the user with half a dataset.

## App Lock

The PIN is never stored. Only `SHA-256(salt + PIN)` with a per-install random salt
lives in DataStore, alongside a flag for biometric unlock, which is handed to the
system `BiometricPrompt`.

## Testing

`app/src/test` holds JVM unit tests for the parts that decide money:

- `MoneyTest` — parsing, formatting, exact splits, quantity maths, overflow.
- `FinancialEngineTest` — schedules (month ends, leap years, weekly), allocation
  (targeted, FIFO, overpayment), validation, balances, due and installment states.
- `TimeAndInsightTest` — period boundaries, relative dates, insights.
- `ExportAndBackupTest` — CSV escaping and amounts, file names, backup JSON round
  trip.

Anything that needs an Android runtime (Room instrumentation, Compose UI tests) is
out of scope for this build's verification — see the release notes for the exact
state of what has and has not been run.
