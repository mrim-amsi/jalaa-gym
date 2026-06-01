package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.model.FinanceRecord
import com.example.data.model.Member
import com.example.data.model.SubscriptionHistory
import com.example.data.repository.GymRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class GymViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: GymRepository

    init {
        val database = AppDatabase.getDatabase(application)
        repository = GymRepository(
            database.memberDao(),
            database.subscriptionHistoryDao(),
            database.financeRecordDao()
        )
    }

    // Modern Arabic Formatter Helper
    private val dateFormatter = SimpleDateFormat("yyyy/MM/dd", Locale("ar"))

    // Search query State
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Real-time Members Flow
    val members: StateFlow<List<Member>> = _searchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) {
                repository.getAllMembers()
            } else {
                repository.searchMembers(query)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All subscription history
    val subscriptionHistory: StateFlow<List<SubscriptionHistory>> = repository.getAllHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All finance records
    val financeRecords: StateFlow<List<FinanceRecord>> = repository.getAllFinanceRecords()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Dashboard dynamic statistics
    val currentTime = MutableStateFlow(System.currentTimeMillis())

    // Reactive member counters
    val activeMembersCount: StateFlow<Int> = currentTime
        .flatMapLatest { time -> repository.getActiveMembersCount(time) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val expiredMembersCount: StateFlow<Int> = currentTime
        .flatMapLatest { time -> repository.getExpiredMembersCount(time) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Finance Metrics (computed reactively from records list to guarantee sync accuracy)
    val dailyRevenue: StateFlow<Double> = financeRecords.map { records ->
        val today = getStartOfDay()
        records.filter { it.type == "REVENUE" && it.date >= today }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val dailyExpense: StateFlow<Double> = financeRecords.map { records ->
        val today = getStartOfDay()
        records.filter { it.type == "EXPENSE" && it.date >= today }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val monthlyRevenue: StateFlow<Double> = financeRecords.map { records ->
        val monthStart = getStartOfThisMonth()
        records.filter { it.type == "REVENUE" && it.date >= monthStart }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val monthlyExpense: StateFlow<Double> = financeRecords.map { records ->
        val monthStart = getStartOfThisMonth()
        records.filter { it.type == "EXPENSE" && it.date >= monthStart }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Total accounts net figures
    val netProfit: StateFlow<Double> = combine(financeRecords) { Array ->
        val records = Array[0]
        val rev = records.filter { it.type == "REVENUE" }.sumOf { it.amount }
        val exp = records.filter { it.type == "EXPENSE" }.sumOf { it.amount }
        rev - exp
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Search query update
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // Refresh timestamps
    fun refreshStats() {
        currentTime.value = System.currentTimeMillis()
    }

    // Member Management Business Logic
    fun addMember(name: String, phone: String, profileImageUri: String? = null) {
        viewModelScope.launch {
            val defaultJoin = System.currentTimeMillis()
            val newMember = Member(
                name = name,
                phone = phone,
                joinDate = defaultJoin,
                profileImageUri = profileImageUri,
                activeEndDate = 0,
                currentSubscriptionType = null,
                isActive = false
            )
            repository.insertMember(newMember)
            refreshStats()
        }
    }

    fun updateMember(member: Member) {
        viewModelScope.launch {
            repository.updateMember(member)
            refreshStats()
        }
    }

    fun deleteMember(member: Member) {
        viewModelScope.launch {
            repository.deleteMember(member)
            refreshStats()
        }
    }

    fun getHistoryByMemberId(memberId: Int): Flow<List<SubscriptionHistory>> =
        repository.getHistoryByMemberId(memberId)

    // Subscription Renewal System
    // Automatically signs a history record and creates a corresponding financial REVENUE entry!
    fun renewSubscription(member: Member, subType: String, price: Double) {
        viewModelScope.launch {
            val start = System.currentTimeMillis()
            val durationDays = when (subType) {
                "شهري" -> 30
                "3 أشهر" -> 90
                "6 أشهر" -> 180
                "سنة" -> 365
                else -> 30
            }
            // If already active and current end date is in the future, extend it, otherwise start from now
            val baseStart = if (member.isActive && member.activeEndDate > start) member.activeEndDate else start
            val end = baseStart + (durationDays.toLong() * 24 * 60 * 60 * 1000)

            // 1. Update Member status
            val updatedMember = member.copy(
                activeEndDate = end,
                currentSubscriptionType = subType,
                isActive = true
            )
            repository.updateMember(updatedMember)

            // 2. Insert into history
            val history = SubscriptionHistory(
                memberId = member.id,
                memberName = member.name,
                type = subType,
                startDate = baseStart,
                endDate = end,
                pricePaid = price
            )
            repository.insertSubscriptionHistory(history)

            // 3. Register financial REVENUE automatically
            val finance = FinanceRecord(
                type = "REVENUE",
                category = "اشتراك: $subType",
                amount = price,
                date = System.currentTimeMillis(),
                description = "تجديد اشتراك ${member.name} - $subType"
            )
            repository.insertFinanceRecord(finance)

            refreshStats()
        }
    }

    // Manual financial log entries
    fun addFinanceRecord(type: String, category: String, amount: Double, description: String) {
        viewModelScope.launch {
            val record = FinanceRecord(
                type = type,
                category = category,
                amount = amount,
                date = System.currentTimeMillis(),
                description = description
            )
            repository.insertFinanceRecord(record)
            refreshStats()
        }
    }

    fun deleteFinanceRecord(record: FinanceRecord) {
        viewModelScope.launch {
            repository.deleteFinanceRecord(record)
            refreshStats()
        }
    }

    // Helper functions for dates
    private fun getStartOfDay(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun getStartOfThisMonth(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    // PDF Exporting capabilities
    fun exportPdfReport(context: Context, onResult: (File?, String?) -> Unit) {
        viewModelScope.launch {
            try {
                // Collect snapshots of states safely
                val allMem = members.value
                val allFin = financeRecords.value
                val activeCount = activeMembersCount.value
                val expiredCount = expiredMembersCount.value
                val totalProfit = netProfit.value

                // Create PdfDocument
                val pdfDocument = PdfDocument()
                val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 Standard
                val page = pdfDocument.startPage(pageInfo)
                val canvas: Canvas = page.canvas

                // Background / Graphics styles
                val bgPaint = Paint().apply { color = Color.WHITE }
                canvas.drawRect(0f, 0f, 595f, 842f, bgPaint)

                // Branding Paints
                val titlePaint = Paint().apply {
                    color = Color.parseColor("#1B3B6F") // Primary Brand Dark Blue
                    textSize = 24f
                    isFakeBoldText = true
                    typeface = Typeface.DEFAULT_BOLD
                    textAlign = Paint.Align.RIGHT
                }

                val headerPaint = Paint().apply {
                    color = Color.parseColor("#212529")
                    textSize = 14f
                    isFakeBoldText = true
                    textAlign = Paint.Align.RIGHT
                }

                val normalPaint = Paint().apply {
                    color = Color.parseColor("#495057")
                    textSize = 11f
                    textAlign = Paint.Align.RIGHT
                }

                val borderPaint = Paint().apply {
                    color = Color.parseColor("#E9ECEF")
                    strokeWidth = 1f
                    style = Paint.Style.STROKE
                }

                // Header section
                canvas.drawText("نادي الجلاء الرياضي - تقرير الإدارة والمالية", 550f, 50f, titlePaint)
                canvas.drawText("تاريخ التقرير: ${dateFormatter.format(Date())}", 550f, 80f, normalPaint)

                canvas.drawLine(40f, 100f, 550f, 100f, borderPaint)

                // Section 1: Stats summary cards
                canvas.drawText("1. إحصائيات عامة والملخص المالي", 550f, 130f, headerPaint)
                canvas.drawText("المشتركون النشطون: $activeCount  |  الاشتراكات المنتهية: $expiredCount", 550f, 155f, normalPaint)
                canvas.drawText("صافي الأرباح الكلية للجيم: $totalProfit شيكل", 550f, 175f, normalPaint)

                canvas.drawLine(40f, 200f, 550f, 200f, borderPaint)

                // Section 2: Members lists summary
                canvas.drawText("2. سجل المشتركين النشطين والمنتهين", 550f, 230f, headerPaint)
                
                var currentY = 260f
                // Headers of Table
                canvas.drawText("الاسم", 550f, currentY, headerPaint)
                canvas.drawText("رقم الهاتف", 400f, currentY, headerPaint)
                canvas.drawText("تاريخ انتهاء الاشتراك", 250f, currentY, headerPaint)
                canvas.drawText("الحالة", 100f, currentY, headerPaint)

                canvas.drawLine(40f, currentY + 5, 550f, currentY + 5, borderPaint)
                currentY += 25f

                // Draw up to top 10 members to fit single A4 page neatly
                for (m in allMem.take(12)) {
                    val formattedEndDate = if (m.activeEndDate > 0) dateFormatter.format(Date(m.activeEndDate)) else "غير مشترك"
                    val statusText = if (m.isActive && m.activeEndDate >= System.currentTimeMillis()) "نشط" else "منتهي"
                    
                    canvas.drawText(m.name, 550f, currentY, normalPaint)
                    canvas.drawText(m.phone, 400f, currentY, normalPaint)
                    canvas.drawText(formattedEndDate, 250f, currentY, normalPaint)
                    canvas.drawText(statusText, 100f, currentY, normalPaint)

                    canvas.drawLine(40f, currentY + 5, 550f, currentY + 5, borderPaint)
                    currentY += 20f
                }

                canvas.drawLine(40f, 520f, 550f, 520f, borderPaint)

                // Section 3: Financial records summary
                currentY = 550f
                canvas.drawText("3. آخر الحركات والعمليات المالية", 550f, currentY, headerPaint)
                currentY += 25f
                
                canvas.drawText("الوصف / الفئة", 550f, currentY, headerPaint)
                canvas.drawText("النوع", 350f, currentY, headerPaint)
                canvas.drawText("التاريخ", 220f, currentY, headerPaint)
                canvas.drawText("القيمة", 100f, currentY, headerPaint)

                canvas.drawLine(40f, currentY + 5, 550f, currentY + 5, borderPaint)
                currentY += 25f

                for (f in allFin.take(8)) {
                    val recordDate = dateFormatter.format(Date(f.date))
                    val typeAr = if (f.type == "REVENUE") "إيرادات" else "مصروفات"
                    
                    canvas.drawText(f.category + if(f.description.isNotEmpty()) " (${f.description})" else "", 550f, currentY, normalPaint)
                    canvas.drawText(typeAr, 350f, currentY, normalPaint)
                    canvas.drawText(recordDate, 220f, currentY, normalPaint)
                    canvas.drawText("${f.amount} شيكل", 100f, currentY, normalPaint)

                    canvas.drawLine(40f, currentY + 5, 550f, currentY + 5, borderPaint)
                    currentY += 20f
                }

                // Footer
                canvas.drawText("تطبيق إدارة الجيم المتقدم - نادي الجلاء الرياضي الحديث © يعمل بالكامل دون اتصال بالإنترنت", 550f, 810f, normalPaint)

                pdfDocument.finishPage(page)

                // Write to external cache or docs directory
                val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
                if (dir != null && !dir.exists()) {
                    dir.mkdirs()
                }
                val outputFile = File(dir, "Jalaa_Gym_Report_${System.currentTimeMillis()}.pdf")
                val fos = FileOutputStream(outputFile)
                pdfDocument.writeTo(fos)
                pdfDocument.close()
                fos.close()

                onResult(outputFile, null)
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(null, e.localizedMessage ?: "حدث خطأ غير معروف أثناء تصدير التقرير")
            }
        }
    }

    // Share PDF helper using System Share sheets
    fun sharePdf(context: Context, file: File) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "مشاركة تقرير الجيم"))
    }
}
