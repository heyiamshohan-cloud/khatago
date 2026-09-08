# KhataGo

**All your finances, in one place.**

KhataGo is an offline-first personal finance manager for Android that keeps the
money people actually track every day: shop credit (baki), loans, product EMIs,
borrowed and lent money, income and expenses — with real installment schedules,
partial payments, reports and local backup.

- **Version:** 1.0.0 (version code 1)
- **Package:** `com.shohan.khatago`
- **Developer:** Shohan Khan — helloiamshohan@gmail.com
- **Language:** English only
- **Theme:** Light mode only (the system dark theme never switches KhataGo)
- **Currency:** BDT (৳)
- **Price:** Free. Forever.

---

## Why KhataGo exists

Most finance apps are built around bank feeds. KhataGo is built around the
ledger people keep in their head or in a paper khata: the shop that gives credit,
the neighbour who borrowed 500 taka, the phone on EMI, the loan with a processing
fee. It replaces that khata with something that cannot lose a page and can add up.

## What it does

| Area | What you get |
| --- | --- |
| **Dashboard** | Your total outstanding position, what is overdue or due today, today's income and expenses, upcoming payments, a six-month money overview, six quick actions and recent activity |
| **Shop Credit** | Shops with owners, multi-item credit purchases (quantity, unit, price), partial payments, FIFO settlement of the oldest purchases first, running balance and per-shop history |
| **Loans** | Principal, processing fee, interest, total payable, real installment schedule, partial or full payments, overpayment protection, payment history, progress |
| **EMI** | Product purchases with down payment, financed amount derived (never stored), installment schedule, per-installment payments |
| **Personal Debt** | Money borrowed and money lent, tracked in separate directions, with many partial repayments or returns |
| **Income & Expense** | Categories you can extend, source/place, notes, and a central activity timeline |
| **Reports** | Period filters (today, week, month, last month, year), income vs expense, net cash flow, category breakdown, outstanding distribution, CSV and PDF export |
| **Search** | One box across shops, loans, EMIs, people, categories, descriptions and notes |
| **Reminders** | A daily on-device scan for upcoming and overdue payments |
| **Backup** | Full JSON backup and restore through the Android file picker — validated before anything is replaced |
| **App Lock** | Optional 4-digit PIN (stored only as a salted hash) plus biometric unlock |

## Principles

1. **Offline first.** Room is the single source of truth. The app does not request
   the `INTERNET` permission and has no account, no backend and no analytics.
2. **Money is never a float.** Every amount is a `Long` in minor units (paisa).
   Splits always add back to exactly the total.
3. **No fake data.** Every number on screen is read from the database you filled.
4. **Payments are guarded.** A payment can never be zero, negative, or larger than
   what is actually due.
5. **Balances are derived, never stored stale.** Paid amounts are recomputed from
   the payment list after every change, so deleting or editing a payment can never
   leave a wrong balance behind.
6. **Free forever.** No ads, no subscriptions, no in-app purchases, no paywall, no
   tracking, no paid SDKs or APIs.

## Project layout

```
app/src/main/java/com/shohan/khatago/
├── core/            money, time, result type, dependency container
├── domain/          models and the financial engine (schedules, allocation, validation)
├── data/
│   ├── local/db/    Room entities, DAOs, database
│   ├── repository/  one repository per module
│   ├── backup/      JSON backup and restore
│   └── export/      CSV and PDF export
├── work/            reminder worker and scheduler
└── ui/
    ├── components/  the design system
    ├── navigation/  destinations and the navigation graph
    └── screens/     one folder per feature
```

See [ARCHITECTURE.md](ARCHITECTURE.md) for the data model, accounting rules and
module boundaries.

## Building

```bash
./gradlew assembleDebug          # debug APK
./gradlew testDebugUnitTest      # unit tests
./gradlew lintDebug              # static analysis
./gradlew assembleRelease        # release APK (see RELEASE.md for signing)
```

Requires JDK 17 and the Android SDK (compileSdk 35, minSdk 26, targetSdk 35).

## Release notes — 1.0.0

First release. Complete local finance tracking: shop credit, loans, EMIs,
borrowed and lent money, income and expenses, dashboard, reports, search,
CSV/PDF export, backup and restore, payment reminders and an optional app lock.

---

KhataGo is developed by Shohan Khan. Questions or feedback:
helloiamshohan@gmail.com
