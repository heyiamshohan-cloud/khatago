package com.shohan.khatago.ui.navigation

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.shohan.khatago.core.KhataGoAppInfo
import com.shohan.khatago.core.di.AppContainer
import com.shohan.khatago.core.money.Money
import com.shohan.khatago.core.result.Outcome
import com.shohan.khatago.data.backup.BackupRepository
import com.shohan.khatago.data.export.ExportFileNames
import com.shohan.khatago.data.repository.PaymentResult
import com.shohan.khatago.data.repository.ReportRangeKind
import com.shohan.khatago.domain.model.AccountKind
import com.shohan.khatago.domain.model.DueState
import com.shohan.khatago.domain.model.LedgerEntry
import com.shohan.khatago.domain.model.PersonalTab
import com.shohan.khatago.domain.model.RelatedType
import com.shohan.khatago.domain.model.UpcomingItem
import com.shohan.khatago.ui.LocalAppContainer
import com.shohan.khatago.ui.components.KhataGoBottomSheet
import com.shohan.khatago.ui.components.KhataGoQuickActionTile
import com.shohan.khatago.ui.components.iconForKind
import com.shohan.khatago.ui.screens.OnboardingScreen
import com.shohan.khatago.ui.screens.SetupScreen
import com.shohan.khatago.ui.screens.accounts.AccountsScreen
import com.shohan.khatago.ui.screens.accounts.AccountsViewModel
import com.shohan.khatago.ui.screens.dashboard.DashboardScreen
import com.shohan.khatago.ui.screens.dashboard.DashboardViewModel
import com.shohan.khatago.ui.screens.dashboard.DueListKind
import com.shohan.khatago.ui.screens.dashboard.DueListScreen
import com.shohan.khatago.ui.screens.dashboard.DueListViewModel
import com.shohan.khatago.ui.screens.emi.EmiDetailScreen
import com.shohan.khatago.ui.screens.emi.EmiDetailViewModel
import com.shohan.khatago.ui.screens.emi.EmiFormScreen
import com.shohan.khatago.ui.screens.emi.EmiFormViewModel
import com.shohan.khatago.ui.screens.emi.EmiPaymentFormScreen
import com.shohan.khatago.ui.screens.emi.EmiPaymentFormViewModel
import com.shohan.khatago.ui.screens.loan.LoanDetailScreen
import com.shohan.khatago.ui.screens.loan.LoanDetailViewModel
import com.shohan.khatago.ui.screens.loan.LoanFormScreen
import com.shohan.khatago.ui.screens.loan.LoanFormViewModel
import com.shohan.khatago.ui.screens.loan.LoanPaymentFormScreen
import com.shohan.khatago.ui.screens.loan.LoanPaymentFormViewModel
import com.shohan.khatago.ui.screens.personal.BorrowedDetailScreen
import com.shohan.khatago.ui.screens.personal.BorrowedDetailViewModel
import com.shohan.khatago.ui.screens.personal.BorrowedFormScreen
import com.shohan.khatago.ui.screens.personal.BorrowedFormViewModel
import com.shohan.khatago.ui.screens.personal.LentDetailScreen
import com.shohan.khatago.ui.screens.personal.LentDetailViewModel
import com.shohan.khatago.ui.screens.personal.LentFormScreen
import com.shohan.khatago.ui.screens.personal.LentFormViewModel
import com.shohan.khatago.ui.screens.personal.RepaymentFormScreen
import com.shohan.khatago.ui.screens.personal.RepaymentFormViewModel
import com.shohan.khatago.ui.screens.personal.ReturnFormViewModel
import com.shohan.khatago.ui.screens.reports.ReportsScreen
import com.shohan.khatago.ui.screens.reports.ReportsViewModel
import com.shohan.khatago.ui.screens.search.SearchScreen
import com.shohan.khatago.ui.screens.search.SearchViewModel
import com.shohan.khatago.ui.screens.settings.AboutScreen
import com.shohan.khatago.ui.screens.settings.AppLockScreen
import com.shohan.khatago.ui.screens.settings.BackupScreen
import com.shohan.khatago.ui.screens.settings.CategoriesScreen
import com.shohan.khatago.ui.screens.settings.CategoriesViewModel
import com.shohan.khatago.ui.screens.settings.SettingsScreen
import com.shohan.khatago.ui.screens.settings.SettingsViewModel
import com.shohan.khatago.ui.screens.shop.CreditFormScreen
import com.shohan.khatago.ui.screens.shop.CreditFormViewModel
import com.shohan.khatago.ui.screens.shop.ItemDraft
import com.shohan.khatago.ui.screens.shop.ShopDetailScreen
import com.shohan.khatago.ui.screens.shop.ShopDetailViewModel
import com.shohan.khatago.ui.screens.shop.ShopFormScreen
import com.shohan.khatago.ui.screens.shop.ShopFormViewModel
import com.shohan.khatago.ui.screens.shop.ShopPaymentFormScreen
import com.shohan.khatago.ui.screens.shop.ShopPaymentFormViewModel
import com.shohan.khatago.ui.screens.transactions.AddExpenseScreen
import com.shohan.khatago.ui.screens.transactions.AddIncomeScreen
import com.shohan.khatago.ui.screens.transactions.IncomeExpenseFormViewModel
import com.shohan.khatago.ui.screens.transactions.TransactionsScreen
import com.shohan.khatago.ui.screens.transactions.TransactionsViewModel
import com.shohan.khatago.ui.theme.CanvasWhite
import com.shohan.khatago.ui.theme.InkSecondary
import com.shohan.khatago.ui.theme.KhataGoGreen
import com.shohan.khatago.ui.theme.OnHero
import com.shohan.khatago.ui.theme.ShapeTokens
import com.shohan.khatago.ui.theme.Spacing
import com.shohan.khatago.ui.khataGoViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import java.time.LocalDate

/**
 * The whole KhataGo navigation graph.
 *
 * Every destination is reachable from here and nothing is hidden behind a
 * paywall or a login: the app opens straight into the user's own data.
 */
@Composable
fun KhataGoNavHost(
    navController: NavHostController,
    startDestination: String,
    modifier: Modifier = Modifier
) {
    val container = LocalAppContainer.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var backupStatus by remember { mutableStateOf<String?>(null) }
    var backupIsError by remember { mutableStateOf(false) }
    var backupWorking by remember { mutableStateOf(false) }
    var quickAddOpen by remember { mutableStateOf(false) }

    fun toast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    // ---------------------------------------------------------------- export

    val csvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val outcome = container.exportRepository.writeCsv(
                uri = uri,
                entries = container.ledgerRepository
                    .observeTransactionsBetween(0L, 10_000_000L, limit = 10_000)
                    .first()
            )
            toast(
                when (outcome) {
                    is Outcome.Success -> "CSV saved (${outcome.value} rows)."
                    is Outcome.Failure -> "We couldn't save that file. Please try again."
                }
            )
        }
    }

    val pdfLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val range = container.reportsRepository.rangeFor(ReportRangeKind.THIS_MONTH)
            val report = container.reportsRepository.observeReport(range).first()
            val outcome = container.exportRepository.writePdf(uri, report)
            toast(
                when (outcome) {
                    is Outcome.Success -> "PDF report saved."
                    is Outcome.Failure -> "We couldn't save that file. Please try again."
                }
            )
        }
    }

    // ---------------------------------------------------------------- backup

    fun runBackup(uri: Uri) {
        scope.launch {
            backupWorking = true
            backupStatus = null
            val payload = container.backupRepository.createBackup(KhataGoAppInfo.VERSION_NAME)
            val outcome = container.backupRepository.writeTo(uri, payload)
            backupIsError = outcome is Outcome.Failure
            backupStatus = when (outcome) {
                is Outcome.Success -> "Backup saved (${outcome.value} records)."
                is Outcome.Failure -> BackupRepository.MESSAGE_WRITE_FAILED
            }
            if (outcome is Outcome.Success) {
                container.settingsRepository.markBackupCreated()
            }
            backupWorking = false
        }
    }

    fun runRestore(uri: Uri) {
        scope.launch {
            backupWorking = true
            backupStatus = null
            when (val read = container.backupRepository.readFrom(uri)) {
                is Outcome.Failure -> {
                    backupIsError = true
                    backupStatus = BackupRepository.MESSAGE_UNREADABLE
                }
                is Outcome.Success -> when (val checked = container.backupRepository.validate(read.value)) {
                    is Outcome.Failure -> {
                        backupIsError = true
                        backupStatus = checked.message
                    }
                    is Outcome.Success -> when (val restored = container.backupRepository.restore(checked.value)) {
                        is Outcome.Success -> {
                            backupIsError = false
                            backupStatus = "Restored ${restored.value.shops} shops, ${restored.value.loans} loans, " +
                                "${restored.value.emis} EMIs and ${restored.value.people} people."
                        }
                        is Outcome.Failure -> {
                            backupIsError = true
                            backupStatus = BackupRepository.MESSAGE_RESTORE_FAILED
                        }
                    }
                }
            }
            backupWorking = false
        }
    }

    // ------------------------------------------------------------- navigation

    fun resolveUpcoming(item: UpcomingItem, onResolved: (String) -> Unit) {
        scope.launch {
            val route = when (item.kind) {
                AccountKind.SHOP_CREDIT -> {
                    val credit = container.shopRepository.getCredit(item.refId)
                    credit?.let { Destination.shopDetail(it.shopId) } ?: Destination.DASHBOARD
                }
                AccountKind.LOAN -> Destination.loanDetail(item.refId)
                AccountKind.EMI -> Destination.emiDetail(item.refId)
                AccountKind.PERSONAL_DEBT -> if (item.subtitle.equals("Borrowed", ignoreCase = true)) {
                    Destination.borrowedDetail(item.refId)
                } else {
                    Destination.lentDetail(item.refId)
                }
            }
            onResolved(route)
        }
    }

    fun openEntry(entry: LedgerEntry) {
        scope.launch {
            val route = resolveEntryRoute(container, entry) ?: return@launch
            navController.navigate(route)
        }
    }

    fun addAccount(kind: AccountKind, personalTab: PersonalTab = PersonalTab.BORROWED) {
        navController.navigate(
            when (kind) {
                AccountKind.SHOP_CREDIT -> Destination.shopForm()
                AccountKind.LOAN -> Destination.loanForm()
                AccountKind.EMI -> Destination.emiForm()
                AccountKind.PERSONAL_DEBT -> if (personalTab == PersonalTab.LENT) {
                    Destination.lentForm()
                } else {
                    Destination.borrowedForm()
                }
            }
        )
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        // ------------------------------------------------------------- splash

        composable(Destination.SPLASH) {
            SplashScreen(
                onTimeout = { target ->
                    navController.navigate(target) {
                        popUpTo(Destination.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        // --------------------------------------------------------- onboarding

        composable(Destination.ONBOARDING) {
            OnboardingScreen(
                onFinish = {
                    navController.navigate(Destination.SETUP) {
                        popUpTo(Destination.SPLASH) { inclusive = true }
                    }
                },
                onSkip = {
                    navController.navigate(Destination.SETUP) {
                        popUpTo(Destination.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        composable(Destination.SETUP) {
            SetupScreen(
                onSaveName = { name -> container.ledgerRepository.saveProfile(name) },
                onCompleted = {
                    navController.navigate(Destination.DASHBOARD) {
                        popUpTo(Destination.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        // ------------------------------------------------------------ tab: home

        composable(Destination.DASHBOARD) {
            val vm: DashboardViewModel = khataGoViewModel { DashboardViewModel(it.dashboardRepository) }
            val state by androidx.lifecycle.compose.collectAsStateWithLifecycle(vm.state)
            DashboardScreen(
                uiState = state,
                onSearch = { navController.navigate(Destination.SEARCH) },
                onProfile = { navController.navigate(Destination.SETTINGS) },
                onQuickAction = { kind -> addAccount(kind, PersonalTab.BORROWED) },
                onAddIncome = { navController.navigate(Destination.incomeForm()) },
                onAddExpense = { navController.navigate(Destination.expenseForm()) },
                onSeeAllUpcoming = { navController.navigate(Destination.UPCOMING) },
                onSeeAllTransactions = {
                    navController.navigate(Destination.TRANSACTIONS) {
                        popUpTo(Destination.DASHBOARD) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onOpenOverdue = { navController.navigate(Destination.OVERDUE) },
                onOpenTransaction = { entry -> openEntry(entry) },
                onOpenUpcoming = { item -> resolveUpcoming(item) { navController.navigate(it) } },
                onQuickAdd = { quickAddOpen = true }
            )
        }

        // -------------------------------------------------------- tab: accounts

        composable(Destination.ACCOUNTS) {
            val vm: AccountsViewModel = khataGoViewModel {
                AccountsViewModel(
                    it.shopRepository,
                    it.loanRepository,
                    it.emiRepository,
                    it.personalRepository,
                    it.todayProvider
                )
            }
            val state by androidx.lifecycle.compose.collectAsStateWithLifecycle(vm.state)
            AccountsScreen(
                uiState = state,
                onSelectTab = vm::selectTab,
                onSelectPersonalTab = vm::selectPersonalTab,
                onQueryChange = vm::setQuery,
                onAddAccount = { kind -> addAccount(kind, state.personalTab) },
                onOpenShop = { id -> navController.navigate(Destination.shopDetail(id)) },
                onOpenLoan = { id -> navController.navigate(Destination.loanDetail(id)) },
                onOpenEmi = { id -> navController.navigate(Destination.emiDetail(id)) },
                onOpenBorrowed = { id -> navController.navigate(Destination.borrowedDetail(id)) },
                onOpenLent = { id -> navController.navigate(Destination.lentDetail(id)) }
            )
        }

        // ---------------------------------------------------- tab: transactions

        composable(Destination.TRANSACTIONS) {
            val vm: TransactionsViewModel = khataGoViewModel { TransactionsViewModel(it.ledgerRepository) }
            val state by androidx.lifecycle.compose.collectAsStateWithLifecycle(vm.state)
            TransactionsScreen(
                uiState = state,
                onSelectFilter = vm::selectFilter,
                onQueryChange = vm::setQuery,
                onOpenEntry = { entry -> openEntry(entry) }
            )
        }

        // --------------------------------------------------------- tab: reports

        composable(Destination.REPORTS) {
            val vm: ReportsViewModel = khataGoViewModel { ReportsViewModel(it.reportsRepository) }
            val state by androidx.lifecycle.compose.collectAsStateWithLifecycle(vm.state)
            ReportsScreen(
                uiState = state,
                onSelectRange = vm::selectRange,
                onExportCsv = { csvLauncher.launch(ExportFileNames.csv()) },
                onExportPdf = { pdfLauncher.launch(ExportFileNames.pdf(state.range.label)) },
                onOpenEntry = { entry -> openEntry(entry) }
            )
        }

        // -------------------------------------------------------- tab: settings

        composable(Destination.SETTINGS) {
            val vm: SettingsViewModel = khataGoViewModel { SettingsViewModel(it.settingsRepository) }
            val settings by androidx.lifecycle.compose.collectAsStateWithLifecycle(vm.settings)
            SettingsScreen(
                settings = settings,
                onBackup = { navController.navigate(Destination.BACKUP) },
                onRestore = { navController.navigate(Destination.BACKUP) },
                onExportCsv = { csvLauncher.launch(ExportFileNames.csv()) },
                onExportPdf = { pdfLauncher.launch(ExportFileNames.pdf("Report")) },
                onAppLock = { navController.navigate(Destination.APP_LOCK) },
                onRemindersChanged = { enabled ->
                    scope.launch {
                        container.settingsRepository.setReminders(enabled)
                        if (enabled) container.reminderScheduler.enable() else container.reminderScheduler.disable()
                    }
                },
                onCategories = { navController.navigate(Destination.CATEGORIES) },
                onAbout = { navController.navigate(Destination.ABOUT) }
            )
        }

        // -------------------------------------------------------------- search

        composable(Destination.SEARCH) {
            val vm: SearchViewModel = khataGoViewModel { SearchViewModel(it.searchRepository) }
            val state by androidx.lifecycle.compose.collectAsStateWithLifecycle(vm.state)
            SearchScreen(
                uiState = state,
                onQueryChange = vm::setQuery,
                onBack = { navController.popBackStack() },
                onOpenAccount = { item ->
                    when (item.kind) {
                        AccountKind.SHOP_CREDIT -> navController.navigate(Destination.shopDetail(item.id))
                        AccountKind.LOAN -> navController.navigate(Destination.loanDetail(item.id))
                        AccountKind.EMI -> navController.navigate(Destination.emiDetail(item.id))
                        AccountKind.PERSONAL_DEBT -> navController.navigate(Destination.borrowedDetail(item.id))
                    }
                },
                onOpenTransaction = { item ->
                    scope.launch {
                        val entries = container.ledgerRepository
                            .observeTransactionsBetween(0L, 10_000_000L, limit = 10_000)
                            .first()
                        val match = entries.firstOrNull { it.id == item.id }
                        if (match != null) openEntry(match)
                    }
                }
            )
        }

        // --------------------------------------------------------- due lists

        composable(Destination.UPCOMING) {
            val vm: DueListViewModel = khataGoViewModel { DueListViewModel(it.dashboardRepository, DueListKind.UPCOMING) }
            val state by androidx.lifecycle.compose.collectAsStateWithLifecycle(vm.state)
            DueListScreen(
                kind = DueListKind.UPCOMING,
                items = state.items,
                total = state.total,
                onBack = { navController.popBackStack() },
                onOpen = { kind, refId ->
                    resolveUpcoming(
                        UpcomingItem(
                            kind = kind,
                            refId = refId,
                            title = "",
                            subtitle = "",
                            dueDate = LocalDate.now(),
                            remaining = 0L,
                            dueState = DueState.UPCOMING
                        )
                    ) { navController.navigate(it) }
                }
            )
        }

        composable(Destination.OVERDUE) {
            val vm: DueListViewModel = khataGoViewModel { DueListViewModel(it.dashboardRepository, DueListKind.OVERDUE) }
            val state by androidx.lifecycle.compose.collectAsStateWithLifecycle(vm.state)
            DueListScreen(
                kind = DueListKind.OVERDUE,
                items = state.items,
                total = state.total,
                onBack = { navController.popBackStack() },
                onOpen = { kind, refId ->
                    resolveUpcoming(
                        UpcomingItem(
                            kind = kind,
                            refId = refId,
                            title = "",
                            subtitle = "Borrowed",
                            dueDate = LocalDate.now(),
                            remaining = 0L,
                            dueState = DueState.OVERDUE
                        )
                    ) { navController.navigate(it) }
                }
            )
        }

        // ---------------------------------------------------------------- shop

        composable(
            route = Destination.SHOP_DETAIL,
            arguments = listOf(requiredLong("shopId"))
        ) { entry ->
            val shopId = entry.longArg("shopId")
            val vm: ShopDetailViewModel = khataGoViewModel { ShopDetailViewModel(it.shopRepository, shopId) }
            val ledger by androidx.lifecycle.compose.collectAsStateWithLifecycle(vm.ledger)
            ShopDetailScreen(
                ledger = ledger,
                onBack = { navController.popBackStack() },
                onEditShop = { navController.navigate(Destination.shopForm(shopId)) },
                onAddPurchase = { navController.navigate(Destination.creditForm(shopId)) },
                onAddPayment = { navController.navigate(Destination.shopPaymentForm(shopId)) },
                onEditPurchase = { creditId -> navController.navigate(Destination.creditForm(shopId, creditId)) },
                onDeletePurchase = { vm.deleteCredit(it) },
                onEditPayment = { paymentId -> navController.navigate(Destination.shopPaymentForm(shopId, paymentId)) },
                onDeletePayment = { vm.deletePayment(it) },
                onDeleteShop = {
                    vm.deleteShop { navController.popBackStack() }
                }
            )
        }

        composable(
            route = Destination.SHOP_FORM,
            arguments = listOf(optionalLong("shopId"))
        ) { entry ->
            val shopId = entry.optionalLongArg("shopId")
            val vm: ShopFormViewModel = khataGoViewModel { ShopFormViewModel(it.shopRepository, shopId) }
            val shop by androidx.lifecycle.compose.collectAsStateWithLifecycle(vm.shop)
            ShopFormScreen(
                shop = shop,
                isEdit = shopId != null,
                onBack = { navController.popBackStack() },
                onSave = { name, owner, phone, address, notes ->
                    vm.save(name, owner, phone, address, notes) { id ->
                        toast("Shop saved.")
                        if (shopId == null) {
                            navController.navigate(Destination.shopDetail(id)) {
                                popUpTo(Destination.SHOP_FORM) { inclusive = true }
                            }
                        } else {
                            navController.popBackStack()
                        }
                    }
                }
            )
        }

        composable(
            route = Destination.CREDIT_FORM,
            arguments = listOf(optionalLong("shopId"), optionalLong("creditId"))
        ) { entry ->
            val shopId = entry.longArg("shopId")
            val creditId = entry.optionalLongArg("creditId")
            val vm: CreditFormViewModel = khataGoViewModel {
                CreditFormViewModel(it.shopRepository, shopId, creditId)
            }
            var loaded by remember { mutableStateOf(false) }
            var date by remember { mutableStateOf(LocalDate.now()) }
            var dueDate by remember { mutableStateOf<LocalDate?>(null) }
            var notes by remember { mutableStateOf("") }
            var items by remember { mutableStateOf(listOf(ItemDraft())) }

            LaunchedEffect(creditId) {
                if (creditId != null) {
                    vm.load()?.let { (d, dd, n) ->
                        date = d
                        dueDate = dd
                        notes = n
                    }
                    val existing = vm.loadItems()
                    if (existing.isNotEmpty()) items = existing
                }
                loaded = true
            }

            CreditFormScreen(
                isEdit = creditId != null,
                initialDate = date,
                initialDueDate = dueDate,
                initialNotes = notes,
                initialItems = items,
                itemsLoaded = loaded,
                onBack = { navController.popBackStack() },
                onSave = { d, dd, list, note ->
                    vm.save(d, dd, list, note) { ok, message ->
                        if (ok) {
                            toast("Purchase saved.")
                            navController.popBackStack()
                        } else {
                            toast(message ?: "Please check the form.")
                        }
                    }
                }
            )
        }

        composable(
            route = Destination.SHOP_PAYMENT_FORM,
            arguments = listOf(optionalLong("shopId"), optionalLong("paymentId"))
        ) { entry ->
            val shopId = entry.longArg("shopId")
            val paymentId = entry.optionalLongArg("paymentId")
            val vm: ShopPaymentFormViewModel = khataGoViewModel {
                ShopPaymentFormViewModel(it.shopRepository, shopId, paymentId)
            }
            val remaining by produceState(0L, shopId) { value = vm.remaining() }
            var initialAmount by remember { mutableStateOf("") }
            var initialNotes by remember { mutableStateOf("") }
            var initialDate by remember { mutableStateOf(LocalDate.now()) }
            LaunchedEffect(paymentId) {
                vm.load()?.let { (amount, date, notes) ->
                    initialAmount = Money.toDecimal(amount).toPlainString()
                    initialNotes = notes
                    initialDate = date
                }
            }
            ShopPaymentFormScreen(
                isEdit = paymentId != null,
                remaining = remaining,
                initialAmount = initialAmount,
                initialNotes = initialNotes,
                initialDate = initialDate,
                onBack = { navController.popBackStack() },
                onSave = { amount, date, method, notes ->
                    vm.save(amount, date, method, notes) { result ->
                        when (result) {
                            is PaymentResult.Success -> {
                                toast("Payment saved.")
                                navController.popBackStack()
                            }
                            is PaymentResult.Rejected -> toast(result.message)
                        }
                    }
                }
            )
        }

        // ---------------------------------------------------------------- loan

        composable(
            route = Destination.LOAN_DETAIL,
            arguments = listOf(requiredLong("loanId"))
        ) { entry ->
            val loanId = entry.longArg("loanId")
            val vm: LoanDetailViewModel = khataGoViewModel {
                LoanDetailViewModel(it.loanRepository, it.todayProvider, loanId)
            }
            val detail by androidx.lifecycle.compose.collectAsStateWithLifecycle(vm.detail)
            LoanDetailScreen(
                detail = detail,
                onBack = { navController.popBackStack() },
                onEdit = { navController.navigate(Destination.loanForm(loanId)) },
                onAddPayment = { navController.navigate(Destination.loanPaymentForm(loanId)) },
                onEditPayment = { paymentId -> navController.navigate(Destination.loanPaymentForm(loanId, paymentId)) },
                onDeletePayment = { vm.deletePayment(it) },
                onDeleteLoan = { vm.deleteLoan { navController.popBackStack() } }
            )
        }

        composable(
            route = Destination.LOAN_FORM,
            arguments = listOf(optionalLong("loanId"))
        ) { entry ->
            val loanId = entry.optionalLongArg("loanId")
            val vm: LoanFormViewModel = khataGoViewModel { LoanFormViewModel(it.loanRepository, loanId) }
            val loan by androidx.lifecycle.compose.collectAsStateWithLifecycle(vm.loan)
            LoanFormScreen(
                loan = loan,
                isEdit = loanId != null,
                onBack = { navController.popBackStack() },
                onSave = { input ->
                    vm.save(input) { id ->
                        toast("Loan saved.")
                        if (loanId == null) {
                            navController.navigate(Destination.loanDetail(id)) {
                                popUpTo(Destination.LOAN_FORM) { inclusive = true }
                            }
                        } else {
                            navController.popBackStack()
                        }
                    }
                }
            )
        }

        composable(
            route = Destination.LOAN_PAYMENT_FORM,
            arguments = listOf(optionalLong("loanId"), optionalLong("paymentId"))
        ) { entry ->
            val loanId = entry.longArg("loanId")
            val paymentId = entry.optionalLongArg("paymentId")
            val vm: LoanPaymentFormViewModel = khataGoViewModel {
                LoanPaymentFormViewModel(it.loanRepository, loanId, paymentId)
            }
            val remaining by produceState(0L, loanId) { value = vm.remaining() }
            val nextRemaining by produceState(0L, loanId) { value = vm.nextInstallmentRemaining() }
            var initialAmount by remember { mutableStateOf("") }
            var initialNotes by remember { mutableStateOf("") }
            var initialDate by remember { mutableStateOf(LocalDate.now()) }
            LaunchedEffect(paymentId) {
                vm.load()?.let { (amount, date, notes) ->
                    initialAmount = Money.toDecimal(amount).toPlainString()
                    initialNotes = notes
                    initialDate = date
                }
            }
            LoanPaymentFormScreen(
                isEdit = paymentId != null,
                remaining = remaining,
                nextInstallmentRemaining = nextRemaining,
                initialAmount = initialAmount,
                initialNotes = initialNotes,
                initialDate = initialDate,
                onBack = { navController.popBackStack() },
                onSave = { amount, date, method, notes ->
                    vm.save(amount, date, method, notes) { result ->
                        when (result) {
                            is PaymentResult.Success -> {
                                toast("Payment saved.")
                                navController.popBackStack()
                            }
                            is PaymentResult.Rejected -> toast(result.message)
                        }
                    }
                }
            )
        }

        // ----------------------------------------------------------------- emi

        composable(
            route = Destination.EMI_DETAIL,
            arguments = listOf(requiredLong("emiId"))
        ) { entry ->
            val emiId = entry.longArg("emiId")
            val vm: EmiDetailViewModel = khataGoViewModel {
                EmiDetailViewModel(it.emiRepository, it.todayProvider, emiId)
            }
            val detail by androidx.lifecycle.compose.collectAsStateWithLifecycle(vm.detail)
            EmiDetailScreen(
                detail = detail,
                onBack = { navController.popBackStack() },
                onEdit = { navController.navigate(Destination.emiForm(emiId)) },
                onAddPayment = { navController.navigate(Destination.emiPaymentForm(emiId)) },
                onEditPayment = { paymentId -> navController.navigate(Destination.emiPaymentForm(emiId, paymentId)) },
                onDeletePayment = { vm.deletePayment(it) },
                onDeleteEmi = { vm.delete { navController.popBackStack() } }
            )
        }

        composable(
            route = Destination.EMI_FORM,
            arguments = listOf(optionalLong("emiId"))
        ) { entry ->
            val emiId = entry.optionalLongArg("emiId")
            val vm: EmiFormViewModel = khataGoViewModel { EmiFormViewModel(it.emiRepository, emiId) }
            val emi by androidx.lifecycle.compose.collectAsStateWithLifecycle(vm.emi)
            EmiFormScreen(
                emi = emi,
                isEdit = emiId != null,
                onBack = { navController.popBackStack() },
                onSave = { input ->
                    vm.save(input) { id ->
                        toast("EMI saved.")
                        if (emiId == null) {
                            navController.navigate(Destination.emiDetail(id)) {
                                popUpTo(Destination.EMI_FORM) { inclusive = true }
                            }
                        } else {
                            navController.popBackStack()
                        }
                    }
                }
            )
        }

        composable(
            route = Destination.EMI_PAYMENT_FORM,
            arguments = listOf(optionalLong("emiId"), optionalLong("paymentId"))
        ) { entry ->
            val emiId = entry.longArg("emiId")
            val paymentId = entry.optionalLongArg("paymentId")
            val vm: EmiPaymentFormViewModel = khataGoViewModel {
                EmiPaymentFormViewModel(it.emiRepository, emiId, paymentId)
            }
            val remaining by produceState(0L, emiId) { value = vm.remaining() }
            val nextRemaining by produceState(0L, emiId) { value = vm.nextInstallmentRemaining() }
            var initialAmount by remember { mutableStateOf("") }
            var initialNotes by remember { mutableStateOf("") }
            var initialDate by remember { mutableStateOf(LocalDate.now()) }
            LaunchedEffect(paymentId) {
                vm.load()?.let { (amount, date, notes) ->
                    initialAmount = Money.toDecimal(amount).toPlainString()
                    initialNotes = notes
                    initialDate = date
                }
            }
            EmiPaymentFormScreen(
                isEdit = paymentId != null,
                remaining = remaining,
                nextInstallmentRemaining = nextRemaining,
                initialAmount = initialAmount,
                initialNotes = initialNotes,
                initialDate = initialDate,
                onBack = { navController.popBackStack() },
                onSave = { amount, date, method, notes ->
                    vm.save(amount, date, method, notes) { result ->
                        when (result) {
                            is PaymentResult.Success -> {
                                toast("Payment saved.")
                                navController.popBackStack()
                            }
                            is PaymentResult.Rejected -> toast(result.message)
                        }
                    }
                }
            )
        }

        // ------------------------------------------------------------ personal

        composable(
            route = Destination.PERSONAL_BORROWED_DETAIL,
            arguments = listOf(requiredLong("debtId"))
        ) { entry ->
            val debtId = entry.longArg("debtId")
            val vm: BorrowedDetailViewModel = khataGoViewModel {
                BorrowedDetailViewModel(it.personalRepository, debtId)
            }
            val detail by androidx.lifecycle.compose.collectAsStateWithLifecycle(vm.detail)
            BorrowedDetailScreen(
                detail = detail,
                onBack = { navController.popBackStack() },
                onEdit = { navController.navigate(Destination.borrowedForm(debtId)) },
                onAddRepayment = { navController.navigate(Destination.repaymentForm(debtId)) },
                onEditRepayment = { id -> navController.navigate(Destination.repaymentForm(debtId, id)) },
                onDeleteRepayment = { vm.deleteRepayment(it) },
                onDeleteRecord = { vm.deleteDebt { navController.popBackStack() } }
            )
        }

        composable(
            route = Destination.PERSONAL_LENT_DETAIL,
            arguments = listOf(requiredLong("lendingId"))
        ) { entry ->
            val lendingId = entry.longArg("lendingId")
            val vm: LentDetailViewModel = khataGoViewModel {
                LentDetailViewModel(it.personalRepository, lendingId)
            }
            val detail by androidx.lifecycle.compose.collectAsStateWithLifecycle(vm.detail)
            LentDetailScreen(
                detail = detail,
                onBack = { navController.popBackStack() },
                onEdit = { navController.navigate(Destination.lentForm(lendingId)) },
                onAddReturn = { navController.navigate(Destination.returnForm(lendingId)) },
                onEditReturn = { id -> navController.navigate(Destination.returnForm(lendingId, id)) },
                onDeleteReturn = { vm.deleteReturn(it) },
                onDeleteRecord = { vm.deleteLending { navController.popBackStack() } }
            )
        }

        composable(
            route = Destination.BORROWED_FORM,
            arguments = listOf(optionalLong("debtId"))
        ) { entry ->
            val debtId = entry.optionalLongArg("debtId")
            val vm: BorrowedFormViewModel = khataGoViewModel {
                BorrowedFormViewModel(it.personalRepository, debtId)
            }
            var loaded by remember { mutableStateOf(false) }
            var name by remember { mutableStateOf("") }
            var amount by remember { mutableStateOf("") }
            var date by remember { mutableStateOf(LocalDate.now()) }
            var expected by remember { mutableStateOf<LocalDate?>(null) }
            var notes by remember { mutableStateOf("") }
            LaunchedEffect(debtId) {
                if (debtId != null) {
                    name = vm.personName()
                    amount = Money.toDecimal(vm.debt.value?.amount ?: 0L).toPlainString()
                    date = vm.debt.value?.borrowedDateEpochDay?.let { LocalDate.ofEpochDay(it) } ?: LocalDate.now()
                    expected = vm.debt.value?.expectedReturnDateEpochDay?.let { LocalDate.ofEpochDay(it) }
                    notes = vm.debt.value?.notes.orEmpty()
                }
                loaded = true
            }
            BorrowedFormScreen(
                initialName = name,
                initialAmount = amount,
                initialDate = date,
                initialExpected = expected,
                initialNotes = notes,
                loaded = loaded,
                isEdit = debtId != null,
                onBack = { navController.popBackStack() },
                onSave = { personName, value, taken, expectedDate, note ->
                    vm.save(personName, value, taken, expectedDate, note) { id ->
                        toast("Record saved.")
                        if (debtId == null) {
                            navController.navigate(Destination.borrowedDetail(id)) {
                                popUpTo(Destination.BORROWED_FORM) { inclusive = true }
                            }
                        } else {
                            navController.popBackStack()
                        }
                    }
                }
            )
        }

        composable(
            route = Destination.LENT_FORM,
            arguments = listOf(optionalLong("lendingId"))
        ) { entry ->
            val lendingId = entry.optionalLongArg("lendingId")
            val vm: LentFormViewModel = khataGoViewModel {
                LentFormViewModel(it.personalRepository, lendingId)
            }
            var loaded by remember { mutableStateOf(false) }
            var name by remember { mutableStateOf("") }
            var amount by remember { mutableStateOf("") }
            var date by remember { mutableStateOf(LocalDate.now()) }
            var expected by remember { mutableStateOf<LocalDate?>(null) }
            var notes by remember { mutableStateOf("") }
            LaunchedEffect(lendingId) {
                if (lendingId != null) {
                    name = vm.personName()
                    amount = Money.toDecimal(vm.lending.value?.amount ?: 0L).toPlainString()
                    date = vm.lending.value?.lentDateEpochDay?.let { LocalDate.ofEpochDay(it) } ?: LocalDate.now()
                    expected = vm.lending.value?.expectedReturnDateEpochDay?.let { LocalDate.ofEpochDay(it) }
                    notes = vm.lending.value?.notes.orEmpty()
                }
                loaded = true
            }
            LentFormScreen(
                initialName = name,
                initialAmount = amount,
                initialDate = date,
                initialExpected = expected,
                initialNotes = notes,
                loaded = loaded,
                isEdit = lendingId != null,
                onBack = { navController.popBackStack() },
                onSave = { personName, value, lent, expectedDate, note ->
                    vm.save(personName, value, lent, expectedDate, note) { id ->
                        toast("Record saved.")
                        if (lendingId == null) {
                            navController.navigate(Destination.lentDetail(id)) {
                                popUpTo(Destination.LENT_FORM) { inclusive = true }
                            }
                        } else {
                            navController.popBackStack()
                        }
                    }
                }
            )
        }

        composable(
            route = Destination.REPAYMENT_FORM,
            arguments = listOf(optionalLong("debtId"), optionalLong("repaymentId"))
        ) { entry ->
            val debtId = entry.longArg("debtId")
            val repaymentId = entry.optionalLongArg("repaymentId")
            val vm: RepaymentFormViewModel = khataGoViewModel {
                RepaymentFormViewModel(it.personalRepository, debtId, repaymentId)
            }
            val remaining by produceState(0L, debtId) { value = vm.remaining() }
            var initialAmount by remember { mutableStateOf("") }
            var initialNotes by remember { mutableStateOf("") }
            var initialDate by remember { mutableStateOf(LocalDate.now()) }
            LaunchedEffect(repaymentId) {
                vm.load()?.let { (amount, date, notes) ->
                    initialAmount = Money.toDecimal(amount).toPlainString()
                    initialNotes = notes
                    initialDate = date
                }
            }
            RepaymentFormScreen(
                isLent = false,
                isEdit = repaymentId != null,
                remaining = remaining,
                initialAmount = initialAmount,
                initialNotes = initialNotes,
                initialDate = initialDate,
                onBack = { navController.popBackStack() },
                onSave = { amount, date, method, notes ->
                    vm.save(amount, date, method, notes) { result ->
                        when (result) {
                            is PaymentResult.Success -> {
                                toast("Payment saved.")
                                navController.popBackStack()
                            }
                            is PaymentResult.Rejected -> toast(result.message)
                        }
                    }
                }
            )
        }

        composable(
            route = Destination.RETURN_FORM,
            arguments = listOf(optionalLong("lendingId"), optionalLong("returnId"))
        ) { entry ->
            val lendingId = entry.longArg("lendingId")
            val returnId = entry.optionalLongArg("returnId")
            val vm: ReturnFormViewModel = khataGoViewModel {
                ReturnFormViewModel(it.personalRepository, lendingId, returnId)
            }
            val remaining by produceState(0L, lendingId) { value = vm.remaining() }
            var initialAmount by remember { mutableStateOf("") }
            var initialNotes by remember { mutableStateOf("") }
            var initialDate by remember { mutableStateOf(LocalDate.now()) }
            LaunchedEffect(returnId) {
                vm.load()?.let { (amount, date, notes) ->
                    initialAmount = Money.toDecimal(amount).toPlainString()
                    initialNotes = notes
                    initialDate = date
                }
            }
            RepaymentFormScreen(
                isLent = true,
                isEdit = returnId != null,
                remaining = remaining,
                initialAmount = initialAmount,
                initialNotes = initialNotes,
                initialDate = initialDate,
                onBack = { navController.popBackStack() },
                onSave = { amount, date, method, notes ->
                    vm.save(amount, date, method, notes) { result ->
                        when (result) {
                            is PaymentResult.Success -> {
                                toast("Payment saved.")
                                navController.popBackStack()
                            }
                            is PaymentResult.Rejected -> toast(result.message)
                        }
                    }
                }
            )
        }

        // ---------------------------------------------------- income / expense

        composable(
            route = Destination.INCOME_FORM,
            arguments = listOf(optionalLong("incomeId"))
        ) { entry ->
            val incomeId = entry.optionalLongArg("incomeId")
            val vm: IncomeExpenseFormViewModel = khataGoViewModel {
                IncomeExpenseFormViewModel(it.ledgerRepository, "INCOME", incomeId)
            }
            val state by androidx.lifecycle.compose.collectAsStateWithLifecycle(vm.state)
            AddIncomeScreen(
                income = state.income,
                categories = state.categories,
                onBack = { navController.popBackStack() },
                onSave = { amount, date, source, category, notes ->
                    vm.saveIncome(amount, date, source, category, notes) { outcome ->
                        when (outcome) {
                            is Outcome.Success -> {
                                toast("Income saved.")
                                navController.popBackStack()
                            }
                            is Outcome.Failure -> toast(outcome.message)
                        }
                    }
                }
            )
        }

        composable(
            route = Destination.EXPENSE_FORM,
            arguments = listOf(optionalLong("expenseId"))
        ) { entry ->
            val expenseId = entry.optionalLongArg("expenseId")
            val vm: IncomeExpenseFormViewModel = khataGoViewModel {
                IncomeExpenseFormViewModel(it.ledgerRepository, "EXPENSE", expenseId)
            }
            val state by androidx.lifecycle.compose.collectAsStateWithLifecycle(vm.state)
            AddExpenseScreen(
                expense = state.expense,
                categories = state.categories,
                onBack = { navController.popBackStack() },
                onSave = { amount, date, category, place, notes ->
                    vm.saveExpense(amount, date, category, place, notes) { outcome ->
                        when (outcome) {
                            is Outcome.Success -> {
                                toast("Expense saved.")
                                navController.popBackStack()
                            }
                            is Outcome.Failure -> toast(outcome.message)
                        }
                    }
                }
            )
        }

        // ------------------------------------------------------------- settings

        composable(Destination.ABOUT) {
            AboutScreen(onBack = { navController.popBackStack() })
        }

        composable(Destination.CATEGORIES) {
            val vm: CategoriesViewModel = khataGoViewModel { CategoriesViewModel(it.ledgerRepository) }
            val categories by androidx.lifecycle.compose.collectAsStateWithLifecycle(vm.categories)
            val tab by androidx.lifecycle.compose.collectAsStateWithLifecycle(vm.currentTab)
            CategoriesScreen(
                categories = categories,
                tab = tab,
                onSelectTab = vm::select,
                onAdd = { name, callback -> vm.add(name, callback) },
                onDelete = { vm.remove(it) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Destination.APP_LOCK) {
            val settings by androidx.lifecycle.compose.collectAsStateWithLifecycle(
                khataGoViewModel { SettingsViewModel(it.settingsRepository) }.settings
            )
            AppLockScreen(
                hasPin = settings.hasPin,
                lockEnabled = settings.appLockEnabled,
                biometricEnabled = settings.biometricEnabled,
                biometricAvailable = com.shohan.khatago.ui.screens.settings.biometricAvailable(),
                onBack = { navController.popBackStack() },
                onEnable = { pin ->
                    scope.launch {
                        container.settingsRepository.setAppLock(true, pin)
                        toast("App Lock is on.")
                    }
                },
                onDisable = {
                    scope.launch {
                        container.settingsRepository.setAppLock(false)
                        toast("App Lock turned off.")
                    }
                },
                onBiometricChanged = { enabled ->
                    scope.launch { container.settingsRepository.setBiometric(enabled) }
                }
            )
        }

        composable(Destination.BACKUP) {
            val settings by androidx.lifecycle.compose.collectAsStateWithLifecycle(
                khataGoViewModel { SettingsViewModel(it.settingsRepository) }.settings
            )
            BackupScreen(
                lastBackupAt = settings.lastBackupAt,
                onBack = { navController.popBackStack() },
                onCreateBackup = { uri -> runBackup(uri) },
                onRestoreBackup = { uri -> runRestore(uri) },
                status = backupStatus,
                statusIsError = backupIsError,
                isWorking = backupWorking
            )
        }
    }

    if (quickAddOpen) {
        QuickAddSheet(
            onDismiss = { quickAddOpen = false },
            onAddAccount = { kind ->
                quickAddOpen = false
                addAccount(kind, PersonalTab.BORROWED)
            },
            onAddIncome = {
                quickAddOpen = false
                navController.navigate(Destination.incomeForm())
            },
            onAddExpense = {
                quickAddOpen = false
                navController.navigate(Destination.expenseForm())
            }
        )
    }
}

// ------------------------------------------------------------------ helpers

private fun requiredLong(name: String) = navArgument(name) { type = NavType.LongType }

private fun optionalLong(name: String) = navArgument(name) {
    type = NavType.LongType
    defaultValue = 0L
}

private fun NavBackStackEntry.longArg(name: String): Long = arguments?.getLong(name) ?: 0L

private fun NavBackStackEntry.optionalLongArg(name: String): Long? =
    arguments?.getLong(name)?.takeIf { it != 0L }

/**
 * Works out where a ledger line leads.
 *
 * Income and expense open their own editor; anything written by another module
 * opens that module's record, so the user always lands somewhere they can act.
 */
private suspend fun resolveEntryRoute(container: AppContainer, entry: LedgerEntry): String? =
    when (entry.relatedType) {
        RelatedType.INCOME -> Destination.incomeForm(entry.relatedId)
        RelatedType.EXPENSE -> Destination.expenseForm(entry.relatedId)
        RelatedType.SHOP -> Destination.shopDetail(entry.relatedId)
        RelatedType.LOAN -> Destination.loanDetail(entry.relatedId)
        RelatedType.EMI -> Destination.emiDetail(entry.relatedId)
        RelatedType.PERSON -> {
            val borrowed = container.personalRepository.observeBorrowed()
                .first()
                .firstOrNull { it.personId == entry.relatedId }
            if (borrowed != null) {
                Destination.borrowedDetail(borrowed.id)
            } else {
                container.personalRepository.observeLent()
                    .first()
                    .firstOrNull { it.personId == entry.relatedId }
                    ?.let { Destination.lentDetail(it.id) }
            }
        }
        RelatedType.NONE -> null
    }

/** The launch screen: quiet, brand-forward, and gone quickly. */
@Composable
private fun SplashScreen(onTimeout: (String) -> Unit) {
    val container = LocalAppContainer.current
    var target by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        container.ensureDefaults()
        val profile = container.ledgerRepository.getProfile()
        target = when {
            profile == null -> Destination.ONBOARDING
            profile.name.isBlank() -> Destination.SETUP
            else -> Destination.DASHBOARD
        }
        delay(700)
        target?.let { onTimeout(it) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CanvasWhite),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(KhataGoGreen, RoundedCornerShape(ShapeTokens.XLarge)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "K",
                    style = MaterialTheme.typography.headlineMedium,
                    color = OnHero
                )
            }
            Spacer(Modifier.height(Spacing.L))
            Text(
                text = KhataGoAppInfo.APP_NAME,
                style = MaterialTheme.typography.headlineSmall,
                color = com.shohan.khatago.ui.theme.InkPrimary
            )
            Spacer(Modifier.height(Spacing.XS))
            Text(
                text = KhataGoAppInfo.TAGLINE,
                style = MaterialTheme.typography.bodySmall,
                color = InkSecondary
            )
        }
    }
}

/** The dashboard FAB sheet: every way to record money in one place. */
@Composable
private fun QuickAddSheet(
    onDismiss: () -> Unit,
    onAddAccount: (AccountKind) -> Unit,
    onAddIncome: () -> Unit,
    onAddExpense: () -> Unit
) {
    KhataGoBottomSheet(title = "Record money", onDismiss = onDismiss) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.XXL),
            contentPadding = PaddingValues(vertical = Spacing.S),
            horizontalArrangement = Arrangement.spacedBy(Spacing.CardGap),
            verticalArrangement = Arrangement.spacedBy(Spacing.CardGap)
        ) {
            items(AccountKind.entries) { kind ->
                KhataGoQuickActionTile(
                    label = kind.label,
                    icon = iconForKind(kind),
                    onClick = { onAddAccount(kind) }
                )
            }
            item {
                KhataGoQuickActionTile(
                    label = "Income",
                    icon = Icons.Outlined.Add,
                    onClick = onAddIncome
                )
            }
            item {
                KhataGoQuickActionTile(
                    label = "Expense",
                    icon = Icons.Outlined.Add,
                    onClick = onAddExpense
                )
            }
        }
    }
}
