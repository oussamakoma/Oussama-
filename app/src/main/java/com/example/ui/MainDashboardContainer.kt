package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.DashboardScreen
import com.example.data.model.WorkshopTransaction
import com.example.ui.viewmodel.WorkshopViewModel
import com.example.ui.viewmodel.WorkshopStats
import com.example.ui.viewmodel.DateFilter
import com.example.ui.theme.LocalIsLiquidTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import kotlinx.coroutines.launch

@Composable
fun MainDashboardContainer(
    viewModel: WorkshopViewModel,
    stats: WorkshopStats,
    transactions: List<WorkshopTransaction>,
    dateFilter: DateFilter,
    onDateFilterChanged: (DateFilter) -> Unit,
    onPopulateSampleData: () -> Unit,
    onClearAll: () -> Unit,
    onOpenGoogleAssistant: () -> Unit,
    onNavigateToSections: () -> Unit,
    onNavigateToTransactions: () -> Unit,
    onTransactionClicked: (WorkshopTransaction) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    val coroutineScope = rememberCoroutineScope()
    val lang by viewModel.appLanguage.collectAsStateWithLifecycle()
    val isLiquidTheme = LocalIsLiquidTheme.current

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = pagerState.currentPage,
            containerColor = if (isLiquidTheme) Color.Transparent else MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Tab(
                selected = pagerState.currentPage == 0,
                onClick = { 
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(0)
                    }
                },
                text = { Text(if (lang == "ar") "الرئيسية" else "Home") }
            )
            Tab(
                selected = pagerState.currentPage == 1,
                onClick = { 
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(1)
                    }
                },
                text = { Text(if (lang == "ar") "الإحصائيات" else "Statistiques") }
            )
            Tab(
                selected = pagerState.currentPage == 2,
                onClick = { 
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(2)
                    }
                },
                text = { Text(if (lang == "ar") "بادج" else "Budget") }
            )
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            when (page) {
                0 -> IntroDashboardScreen(
                    viewModel = viewModel,
                    onNavigateToSections = onNavigateToSections,
                    onNavigateToTransactions = onNavigateToTransactions,
                    onTransactionClicked = onTransactionClicked,
                    isStatsPage = false
                )
                1 -> DashboardScreen(
                    viewModel = viewModel,
                    stats = stats,
                    transactionsList = transactions,
                    dateFilter = dateFilter,
                    onDateFilterChanged = onDateFilterChanged,
                    onPopulateSampleData = onPopulateSampleData,
                    onClearAll = onClearAll,
                    onCardClicked = onTransactionClicked,
                    onOpenGoogleAssistant = onOpenGoogleAssistant
                )
                2 -> BudgetCategoriesScreen(viewModel = viewModel)
            }
        }
    }
}
