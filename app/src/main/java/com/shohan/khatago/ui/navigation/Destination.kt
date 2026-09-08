package com.shohan.khatago.ui.navigation

/** Every destination in KhataGo. Arguments are appended to the route string. */
object Destination {
    const val SPLASH = "splash"
    const val ONBOARDING = "onboarding"
    const val SETUP = "setup"

    const val DASHBOARD = "dashboard"
    const val ACCOUNTS = "accounts"
    const val TRANSACTIONS = "transactions"
    const val REPORTS = "reports"
    const val SETTINGS = "settings"

    const val SEARCH = "search"

    const val SHOP_DETAIL = "shop/{shopId}"
    const val SHOP_FORM = "shop_form?shopId={shopId}"
    const val CREDIT_FORM = "credit_form?shopId={shopId}&creditId={creditId}"
    const val SHOP_PAYMENT_FORM = "shop_payment_form?shopId={shopId}&paymentId={paymentId}"

    const val LOAN_DETAIL = "loan/{loanId}"
    const val LOAN_FORM = "loan_form?loanId={loanId}"
    const val LOAN_PAYMENT_FORM = "loan_payment_form?loanId={loanId}&paymentId={paymentId}"

    const val EMI_DETAIL = "emi/{emiId}"
    const val EMI_FORM = "emi_form?emiId={emiId}"
    const val EMI_PAYMENT_FORM = "emi_payment_form?emiId={emiId}&paymentId={paymentId}"

    const val PERSONAL_BORROWED_DETAIL = "borrowed/{debtId}"
    const val PERSONAL_LENT_DETAIL = "lent/{lendingId}"
    const val BORROWED_FORM = "borrowed_form?debtId={debtId}"
    const val LENT_FORM = "lent_form?lendingId={lendingId}"
    const val REPAYMENT_FORM = "repayment_form?debtId={debtId}&repaymentId={repaymentId}"
    const val RETURN_FORM = "return_form?lendingId={lendingId}&returnId={returnId}"

    const val INCOME_FORM = "income_form?incomeId={incomeId}"
    const val EXPENSE_FORM = "expense_form?expenseId={expenseId}"

    const val TRANSACTION_DETAIL = "transaction/{entryId}"

    const val ABOUT = "about"
    const val CATEGORIES = "categories"
    const val APP_LOCK = "app_lock"
    const val BACKUP = "backup"
    const val UPCOMING = "upcoming"
    const val OVERDUE = "overdue"

    fun shopDetail(id: Long) = "shop/$id"
    fun shopForm(id: Long? = null) = if (id == null) "shop_form?" else "shop_form?shopId=$id"
    fun creditForm(shopId: Long, creditId: Long? = null) =
        "credit_form?shopId=$shopId" + (creditId?.let { "&creditId=$it" } ?: "")
    fun shopPaymentForm(shopId: Long, paymentId: Long? = null) =
        "shop_payment_form?shopId=$shopId" + (paymentId?.let { "&paymentId=$it" } ?: "")

    fun loanDetail(id: Long) = "loan/$id"
    fun loanForm(id: Long? = null) = if (id == null) "loan_form?" else "loan_form?loanId=$id"
    fun loanPaymentForm(loanId: Long, paymentId: Long? = null) =
        "loan_payment_form?loanId=$loanId" + (paymentId?.let { "&paymentId=$it" } ?: "")

    fun emiDetail(id: Long) = "emi/$id"
    fun emiForm(id: Long? = null) = if (id == null) "emi_form?" else "emi_form?emiId=$id"
    fun emiPaymentForm(emiId: Long, paymentId: Long? = null) =
        "emi_payment_form?emiId=$emiId" + (paymentId?.let { "&paymentId=$it" } ?: "")

    fun borrowedDetail(id: Long) = "borrowed/$id"
    fun lentDetail(id: Long) = "lent/$id"
    fun borrowedForm(id: Long? = null) = if (id == null) "borrowed_form?" else "borrowed_form?debtId=$id"
    fun lentForm(id: Long? = null) = if (id == null) "lent_form?" else "lent_form?lendingId=$id"
    fun repaymentForm(debtId: Long, repaymentId: Long? = null) =
        "repayment_form?debtId=$debtId" + (repaymentId?.let { "&repaymentId=$it" } ?: "")
    fun returnForm(lendingId: Long, returnId: Long? = null) =
        "return_form?lendingId=$lendingId" + (returnId?.let { "&returnId=$it" } ?: "")

    fun transactionDetail(entryId: Long) = "transaction/$entryId"

    fun incomeForm(id: Long? = null) = if (id == null) "income_form?" else "income_form?incomeId=$id"
    fun expenseForm(id: Long? = null) = if (id == null) "expense_form?" else "expense_form?expenseId=$id"
}

/** Routes that belong to the five main tabs. */
val TAB_ROUTES = setOf(
    Destination.DASHBOARD,
    Destination.ACCOUNTS,
    Destination.TRANSACTIONS,
    Destination.REPORTS,
    Destination.SETTINGS
)
