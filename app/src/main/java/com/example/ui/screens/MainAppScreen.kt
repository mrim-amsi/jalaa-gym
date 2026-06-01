package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.FinanceRecord
import com.example.data.model.Member
import com.example.data.model.SubscriptionHistory
import com.example.ui.theme.*
import com.example.ui.viewmodel.GymViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(viewModel: GymViewModel) {
    // Force RTL for native Arabic experience on all devices
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        val context = LocalContext.current

        // Viewmodel states
        val members by viewModel.members.collectAsStateWithLifecycle()
        val financeRecords by viewModel.financeRecords.collectAsStateWithLifecycle()
        val activeCount by viewModel.activeMembersCount.collectAsStateWithLifecycle()
        val expiredCount by viewModel.expiredMembersCount.collectAsStateWithLifecycle()

        val dailyRev by viewModel.dailyRevenue.collectAsStateWithLifecycle()
        val dailyExp by viewModel.dailyExpense.collectAsStateWithLifecycle()
        val monthlyRev by viewModel.monthlyRevenue.collectAsStateWithLifecycle()
        val monthlyExp by viewModel.monthlyExpense.collectAsStateWithLifecycle()
        val totalNetProfit by viewModel.netProfit.collectAsStateWithLifecycle()

        val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

        // UI navigation state (0 = Dashboard, 1 = Members List, 2 = Transactions & Reports)
        var selectedTab by remember { mutableStateOf(0) }

        // Action sheets & dialog flags
        var showAddMemberDialog by remember { mutableStateOf(false) }
        var memberToEdit by remember { mutableStateOf<Member?>(null) }
        var memberToRenew by remember { mutableStateOf<Member?>(null) }
        var showAddFinanceDialog by remember { mutableStateOf(false) }
        var memberToViewHistory by remember { mutableStateOf<Member?>(null) }

        // Status notifier trigger
        LaunchedEffect(Unit) {
            viewModel.refreshStats()
        }

        Scaffold(
            topBar = {
                LargeTopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Jalaa Gym Custom Sporty Vector Icon / Badge
                            Box(
                                modifier = Modifier
                                    .size(45.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(
                                                MaterialTheme.colorScheme.primary,
                                                MaterialTheme.colorScheme.tertiary
                                            )
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.FitnessCenter,
                                    contentDescription = "شعار الجلاء",
                                    tint = Color.White,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "نادي الجلاء الرياضي",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    )
                                )
                                Text(
                                    text = "نظام الإدارة والمالية المتكامل",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.largeTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    ),
                    actions = {
                        IconButton(
                            onClick = {
                                viewModel.exportPdfReport(context) { file, err ->
                                    if (file != null) {
                                        viewModel.sharePdf(context, file)
                                    } else {
                                        Toast.makeText(context, err ?: "خطأ أثناء التصدير", Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            modifier = Modifier.testTag("pdf_export_top_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PictureAsPdf,
                                contentDescription = "تصدير تقرير PDF الكلي",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        label = { Text("الرئيسية") },
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == 0) Icons.Filled.Dashboard else Icons.Outlined.Dashboard,
                                contentDescription = "لوحة التحكم"
                            )
                        },
                        modifier = Modifier.testTag("nav_tab_dashboard")
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        label = { Text("المشتركون") },
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == 1) Icons.Filled.People else Icons.Outlined.People,
                                contentDescription = "إدارة المشتركين"
                            )
                        },
                        modifier = Modifier.testTag("nav_tab_members")
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        label = { Text("المالية") },
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == 2) Icons.Filled.AccountBalanceWallet else Icons.Outlined.AccountBalanceWallet,
                                contentDescription = "الحسابات والتقارير"
                            )
                        },
                        modifier = Modifier.testTag("nav_tab_finance")
                    )
                }
            },
            floatingActionButton = {
                when (selectedTab) {
                    1 -> {
                        ExtendedFloatingActionButton(
                            onClick = { showAddMemberDialog = true },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White,
                            icon = { Icon(Icons.Filled.PersonAdd, contentDescription = null) },
                            text = { Text("مشترك جديد") },
                            modifier = Modifier.testTag("fab_add_member")
                        )
                    }
                    2 -> {
                        ExtendedFloatingActionButton(
                            onClick = { showAddFinanceDialog = true },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White,
                            icon = { Icon(Icons.Filled.AddCard, contentDescription = null) },
                            text = { Text("تسجيل حركة") },
                            modifier = Modifier.testTag("fab_add_finance")
                        )
                    }
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = {
                        slideInHorizontally { width -> if (targetState > initialState) width else -width } + fadeIn() togetherWith
                                slideOutHorizontally { width -> if (targetState > initialState) -width else width } + fadeOut()
                    },
                    label = "TabTransition"
                ) { targetTab ->
                    when (targetTab) {
                        0 -> DashboardTab(
                            activeCount = activeCount,
                            expiredCount = expiredCount,
                            dailyRev = dailyRev,
                            dailyExp = dailyExp,
                            monthlyRev = monthlyRev,
                            monthlyExp = monthlyExp,
                            netProfit = totalNetProfit,
                            members = members,
                            onRenewMember = { memberToRenew = it },
                            onNavigateToMembers = { selectedTab = 1 }
                        )
                        1 -> MembersTab(
                            members = members,
                            searchQuery = searchQuery,
                            onSearchQueryChange = { viewModel.setSearchQuery(it) },
                            onEditMember = { memberToEdit = it },
                            onDeleteMember = { viewModel.deleteMember(it) },
                            onRenewMember = { memberToRenew = it },
                            onViewHistory = { memberToViewHistory = it }
                        )
                        2 -> FinanceTab(
                            financeRecords = financeRecords,
                            totalProfit = totalNetProfit,
                            dailyRev = dailyRev,
                            dailyExp = dailyExp,
                            monthlyRev = monthlyRev,
                            monthlyExp = monthlyExp,
                            onDeleteRecord = { viewModel.deleteFinanceRecord(it) },
                            onExportPdf = {
                                viewModel.exportPdfReport(context) { file, err ->
                                    if (file != null) {
                                        viewModel.sharePdf(context, file)
                                    } else {
                                        Toast.makeText(context, err ?: "فشل في تصدير التقرير", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }

        // Dialogs definitions
        if (showAddMemberDialog) {
            AddOrEditMemberDialog(
                onDismiss = { showAddMemberDialog = false },
                onConfirm = { name, phone, avatar ->
                    viewModel.addMember(name, phone, avatar)
                    showAddMemberDialog = false
                    Toast.makeText(context, "تم تسجيل المشترك بنجاح", Toast.LENGTH_SHORT).show()
                }
            )
        }

        memberToEdit?.let { member ->
            AddOrEditMemberDialog(
                editingMember = member,
                onDismiss = { memberToEdit = null },
                onConfirm = { name, phone, avatar ->
                    viewModel.updateMember(member.copy(name = name, phone = phone, profileImageUri = avatar))
                    memberToEdit = null
                    Toast.makeText(context, "تم تعديل بيانات المشترك", Toast.LENGTH_SHORT).show()
                }
            )
        }

        memberToRenew?.let { member ->
            RenewSubscriptionDialog(
                member = member,
                onDismiss = { memberToRenew = null },
                onConfirm = { type, price ->
                    viewModel.renewSubscription(member, type, price)
                    memberToRenew = null
                    Toast.makeText(context, "تم تجديد الاشتراك بنجاح", Toast.LENGTH_SHORT).show()
                }
            )
        }

        if (showAddFinanceDialog) {
            AddFinanceRecordDialog(
                onDismiss = { showAddFinanceDialog = false },
                onConfirm = { type, category, amount, description ->
                    viewModel.addFinanceRecord(type, category, amount, description)
                    showAddFinanceDialog = false
                    Toast.makeText(context, "تم حفظ المعاملة المالية", Toast.LENGTH_SHORT).show()
                }
            )
        }

        memberToViewHistory?.let { member ->
            val historyFlow = remember(member) { viewModel.getHistoryByMemberId(member.id) }
            val memberHistory by historyFlow.collectAsStateWithLifecycle(initialValue = emptyList())
            ViewSubscriptionHistoryDialog(
                member = member,
                history = memberHistory,
                onDismiss = { memberToViewHistory = null }
            )
        }
    }
}

// ==================== DASHBOARD TAB ====================
@Composable
fun DashboardTab(
    activeCount: Int,
    expiredCount: Int,
    dailyRev: Double,
    dailyExp: Double,
    monthlyRev: Double,
    monthlyExp: Double,
    netProfit: Double,
    members: List<Member>,
    onRenewMember: (Member) -> Unit,
    onNavigateToMembers: () -> Unit
) {
    val expiredMembers = remember(members) {
        members.filter { !it.isActive || it.activeEndDate < System.currentTimeMillis() }.take(5)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome Header Banner Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.secondary
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "أهلاً بك، في إدارة نادي الجلاء! 👋",
                            style = MaterialTheme.typography.titleLarge.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = "جميع العمليات تعمل أوفلاين 100% وبدون إنترنت بنظام تشفير محلي آمن.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        )
                    }
                }
            }
        }

        // Row 1: Active & Expired counters
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(JalaaGreenActive.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.HowToReg,
                                contentDescription = null,
                                tint = JalaaGreenActive,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "نشط ومستمر",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Text(
                            text = "$activeCount مشترك",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = JalaaGreenActive
                            )
                        )
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(JalaaRedExpired.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Cancel,
                                contentDescription = null,
                                tint = JalaaRedExpired,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "اشتراكات منتهية",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Text(
                            text = "$expiredCount مشترك",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = JalaaRedExpired
                            )
                        )
                    }
                }
            }
        }

        // Financial Daily / Monthly Summary Section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(
                                imageVector = Icons.Filled.QueryStats,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "ميزانية وأرباح الجلاء اليومية",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(JalaaBlack.copy(alpha = 0.1f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "اليوم",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = JalaaGold
                            )
                        }
                    }

                    Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "إيرادات اليوم",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Text(
                                text = "+$dailyRev شيكل",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = JalaaGreenActive
                            )
                        }

                        Column {
                            Text(
                                text = "مصروفات اليوم",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Text(
                                text = "-$dailyExp شيكل",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = JalaaRedExpired
                            )
                        }

                        Column {
                            Text(
                                text = "صافي ربح اليوم",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            val diff = dailyRev - dailyExp
                            Text(
                                text = "${if (diff >= 0) "+" else ""}$diff شيكل",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (diff >= 0) JalaaGreenActive else JalaaRedExpired
                            )
                        }
                    }
                }
            }
        }

        // Business Net Overall indicator
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AccountBalance,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "إجمالي صافي الأرباح المتراكمة",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Text(
                            text = "$netProfit شيكل",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Section: Expired Subscribers Alerts
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "تنبيهات الاشتراكات المنتهية ⚠️",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onNavigateToMembers) {
                    Text("عرض الكل")
                }
            }
        }

        if (expiredMembers.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "رائع! لا توجد اشتراكات منتهية أو بحاجة لتنبيه الآن.",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        textAlign = Alignment.Center.let { TextAlign.Center },
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        } else {
            items(expiredMembers) { member ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, JalaaRedExpired.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = member.profileImageUri ?: "🏋️‍♂️",
                                fontSize = 24.sp
                            )
                            Column {
                                Text(
                                    text = member.name,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "هاتف: ${member.phone}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }

                        Button(
                            onClick = { onRenewMember(member) },
                            colors = ButtonDefaults.buttonColors(containerColor = JalaaGold),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Filled.Autorenew, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("تجديد بضغطة", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

// ==================== MEMBERS MANAGEMENT TAB ====================
@Composable
fun MembersTab(
    members: List<Member>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onEditMember: (Member) -> Unit,
    onDeleteMember: (Member) -> Unit,
    onRenewMember: (Member) -> Unit,
    onViewHistory: (Member) -> Unit
) {
    val dateForm = SimpleDateFormat("yyyy/MM/dd", Locale("ar"))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // High polish search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("member_search_input"),
            placeholder = { Text("البحث بالاسم أو رقم الهاتف...") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(Icons.Filled.Clear, contentDescription = null)
                    }
                }
            },
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            )
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "قائمة المشتركين (${members.size} مشترك)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (members.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.PersonOff,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                    )
                    Text(
                        text = "لا يوجد مشتركين مسجلين يطابقون البحث",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(members) { member ->
                    var showDropdownMenu by remember { mutableStateOf(false) }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("member_card_${member.id}"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(14.dp),
                        elevation = CardDefaults.cardElevation(1.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            // Row 1: Profile + Main Data
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // Sporty avatar
                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = member.profileImageUri ?: "🏋️‍♂️",
                                            fontSize = 22.sp
                                        )
                                    }

                                    Column {
                                        Text(
                                            text = member.name,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Text(
                                            text = "هاتف: ${member.phone}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    }
                                }

                                // Interactive status chip
                                val isActive = member.isActive && member.activeEndDate >= System.currentTimeMillis()
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isActive) JalaaGreenActive.copy(alpha = 0.15f)
                                            else JalaaRedExpired.copy(alpha = 0.15f)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = if (isActive) "نشط" else "منتهي",
                                        color = if (isActive) JalaaGreenActive else JalaaRedExpired,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                            Spacer(modifier = Modifier.height(10.dp))

                            // Bottom actions and subscription details
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "تاريخ الانضمام: ${dateForm.format(Date(member.joinDate))}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                                    )
                                    if (member.activeEndDate > 0) {
                                        Text(
                                            text = "ينتهي في: ${dateForm.format(Date(member.activeEndDate))} (${member.currentSubscriptionType ?: ""})",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.secondary,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    // Renew button
                                    IconButton(
                                        onClick = { onRenewMember(member) },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(JalaaGold.copy(alpha = 0.1f)),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Autorenew,
                                            contentDescription = "تجديد المشترك",
                                            tint = JalaaGold,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    // History log button
                                    IconButton(
                                        onClick = { onViewHistory(member) },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(JalaaBlueInfo.copy(alpha = 0.1f))
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.History,
                                            contentDescription = "سجل الاشتراكات",
                                            tint = JalaaBlueInfo,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    // More options dropdown triggering
                                    Box {
                                        IconButton(
                                            onClick = { showDropdownMenu = true },
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.MoreVert,
                                                contentDescription = "مزيد من الخيارات"
                                            )
                                        }

                                        DropdownMenu(
                                            expanded = showDropdownMenu,
                                            onDismissRequest = { showDropdownMenu = false }
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text("تعديل البيانات") },
                                                onClick = {
                                                    showDropdownMenu = false
                                                    onEditMember(member)
                                                },
                                                leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("حذف المشترك") },
                                                onClick = {
                                                    showDropdownMenu = false
                                                    onDeleteMember(member)
                                                },
                                                leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null, tint = JalaaRedExpired) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==================== FINANCE & REPORTS TAB ====================
@Composable
fun FinanceTab(
    financeRecords: List<FinanceRecord>,
    totalProfit: Double,
    dailyRev: Double,
    dailyExp: Double,
    monthlyRev: Double,
    monthlyExp: Double,
    onDeleteRecord: (FinanceRecord) -> Unit,
    onExportPdf: () -> Unit
) {
    val dateForm = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale("ar"))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // High contrast account summary banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "الملخص المالي العام للجيم 📊",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("إيرادات الشهر", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        Text("+$monthlyRev شيكل", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = JalaaGreenActive)
                    }
                    Column {
                        Text("مصروفات الشهر", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        Text("-$monthlyExp شيكل", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = JalaaRedExpired)
                    }
                    Column {
                        Text("صافي الأرباح", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        Text("$totalProfit شيكل", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = JalaaGold)
                    }
                }
            }
        }

        // PDF Exporter Card Box
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(45.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(JalaaGold.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.PictureAsPdf, contentDescription = null, tint = JalaaGold)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("التقارير الإدارية والمالية الشاملة", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    Text("اضغط للتصدير والطباعة بتنسيق PDF ومشاركته بضغطة واحدة.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
                Button(
                    onClick = onExportPdf,
                    colors = ButtonDefaults.buttonColors(containerColor = JalaaGold),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text("تصدير", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Ledger list entries
        Text(
            text = "كشف الحساب والعمليات الأخيرة 📖",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium
        )

        if (financeRecords.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "لا توجد معاملات مالية مسجلة بعد.",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(financeRecords) { record ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (record.type == "REVENUE") JalaaGreenActive.copy(alpha = 0.12f)
                                            else JalaaRedExpired.copy(alpha = 0.12f)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (record.type == "REVENUE") Icons.Filled.TrendingUp else Icons.Filled.TrendingDown,
                                        contentDescription = null,
                                        tint = if (record.type == "REVENUE") JalaaGreenActive else JalaaRedExpired,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Column {
                                    Text(
                                        text = record.category,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    if (record.description.isNotEmpty()) {
                                        Text(
                                            text = record.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Text(
                                        text = dateForm.format(Date(record.date)),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "${if (record.type == "REVENUE") "+" else "-"}${record.amount} شيكل",
                                    fontWeight = FontWeight.Bold,
                                    color = if (record.type == "REVENUE") JalaaGreenActive else JalaaRedExpired,
                                    style = MaterialTheme.typography.bodyLarge
                                )

                                IconButton(
                                    onClick = { onDeleteRecord(record) },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Delete,
                                        contentDescription = "حذف الحركة",
                                        tint = JalaaRedExpired.copy(alpha = 0.7f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==================== DIALOGS COMPONENTS ====================

// Add/Edit Member Dialog Box
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddOrEditMemberDialog(
    editingMember: Member? = null,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf(editingMember?.name ?: "") }
    var phone by remember { mutableStateOf(editingMember?.phone ?: "") }
    
    // Available sporty avatars representation
    val avatars = listOf("🏋️‍♂️", "🏃‍♂️", "🥊", "🧘‍♀️", "🔋", "🌟", "🦾", "🏆")
    var selectedAvatar by remember { mutableStateOf(editingMember?.profileImageUri ?: "🏋️‍♂️") }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && phone.isNotBlank()) {
                        onConfirm(name, phone, selectedAvatar)
                    }
                },
                enabled = name.isNotBlank() && phone.isNotBlank()
            ) {
                Text(if (editingMember != null) "حفظ التعديلات" else "إضافة العضو")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        },
        title = {
            Text(
                if (editingMember != null) "تعديل بيانات المشترك" else "إضافة مشترك جديد للجيم",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("الاسم الكامل للمشترك") },
                    placeholder = { Text("مثال: رامي العلي") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("رقم الهاتف") },
                    placeholder = { Text("مثال: 079XXXXXXXX") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Avatar Icon Picker Selection List
                Text("اختر الصورة الرياضية الرمزية (اختياري):", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    avatars.forEach { av ->
                        val isSelected = av == selectedAvatar
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) JalaaGold
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                                )
                                .clickable { selectedAvatar = av },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = av, fontSize = 20.sp)
                        }
                    }
                }
            }
        }
    )
}

// Renew Subscription Dialog Box
@Composable
fun RenewSubscriptionDialog(
    member: Member,
    onDismiss: () -> Unit,
    onConfirm: (String, Double) -> Unit
) {
    val options = listOf(
        SubscriptionOption("شهري (30 يوم)", "شهري", 30.0),
        SubscriptionOption("3 أشهر (90 يوم)", "3 أشهر", 80.0),
        SubscriptionOption("6 أشهر (180 يوم)", "6 أشهر", 140.0),
        SubscriptionOption("سنوي (365 يوم)", "سنة", 250.0)
    )
    var selectedOption by remember { mutableStateOf(options[0]) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedOption.type, selectedOption.price) },
                colors = ButtonDefaults.buttonColors(containerColor = JalaaGold)
            ) {
                Text("تأكيد التجديد الآن")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        },
        title = {
            Text(
                "تجديد اشتراك: ${member.name}",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "سيتم تجديد اشتراك العضو ابتداءً من اليوم أو تمديد اشتراكه النشط الحالي.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                options.forEach { opt ->
                    val isSelected = opt.type == selectedOption.type
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedOption = opt },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) JalaaGold.copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isSelected) JalaaGold else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { selectedOption = opt }
                                )
                                Text(text = opt.label, fontWeight = FontWeight.Bold)
                            }
                            Text(text = "${opt.price} شيكل", color = JalaaGold, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    )
}

data class SubscriptionOption(val label: String, val type: String, val price: Double)

// Add Finance Record Dialog Box
@Composable
fun AddFinanceRecordDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, Double, String) -> Unit
) {
    var type by remember { mutableStateOf("REVENUE") } // REVENUE or EXPENSE
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    val categories = if (type == "REVENUE") {
        listOf("اشتراك جيم", "بيع مكملات", "تدريب خاص", "بيع مستلزمات", "أخرى")
    } else {
        listOf("رواتب مدربين", "إيجار النادي", "فاتورة كهرباء / ماء", "صيانة أجهزة", "أخرى")
    }
    var selectedCategory by remember { mutableStateOf(categories[0]) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    val amt = amount.toDoubleOrNull()
                    if (amt != null && amt > 0) {
                        onConfirm(type, selectedCategory, amt, description)
                    }
                },
                enabled = amount.isNotEmpty() && amount.toDoubleOrNull() != null
            ) {
                Text("تسجيل المعاملة")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        },
        title = {
            Text("تسجيل حركة مالية جديدة", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Type selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            type = "REVENUE"
                            selectedCategory = "اشتراك جيم"
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (type == "REVENUE") JalaaGreenActive else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                            contentColor = if (type == "REVENUE") Color.White else MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("إيرادات (داخل)")
                    }

                    Button(
                        onClick = {
                            type = "EXPENSE"
                            selectedCategory = "رواتب مدربين"
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (type == "EXPENSE") JalaaRedExpired else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                            contentColor = if (type == "EXPENSE") Color.White else MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("مصروفات (خارج)")
                    }
                }

                // Amount
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("المبلغ بالشيكل") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Category selector
                Text("الفئة / التصنيف:", fontWeight = FontWeight.Medium)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    categories.chunked(3).forEach { chunk ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            chunk.forEach { cat ->
                                val isSelected = cat == selectedCategory
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                                        )
                                        .clickable { selectedCategory = cat }
                                        .padding(horizontal = 6.dp, vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = cat,
                                        fontSize = 12.sp,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                        textAlign = Alignment.Center.let { TextAlign.Center }
                                    )
                                }
                            }
                        }
                    }
                }

                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("تفاصيل أو ملاحظات إضافية") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    )
}

// View Subscription History Bottom Sheet / Dialog
@Composable
fun ViewSubscriptionHistoryDialog(
    member: Member,
    history: List<SubscriptionHistory>,
    onDismiss: () -> Unit
) {
    val dateForm = SimpleDateFormat("yyyy/MM/dd", Locale("ar"))

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("إغلاق")
            }
        },
        title = {
            Text("سجل اشتراكات العضو: ${member.name}", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (history.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "لا توجد اشتراكات سابقة مسجلة للعضو.",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(history) { hist ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.02f)),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            text = "النوع: ${hist.type}",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            text = "البداية: ${dateForm.format(Date(hist.startDate))}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                        Text(
                                            text = "النهاية: ${dateForm.format(Date(hist.endDate))}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    }
                                    Text(
                                        text = "${hist.pricePaid} شيكل",
                                        fontWeight = FontWeight.Bold,
                                        color = JalaaGold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    )
}
