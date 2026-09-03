package com.saldoclaro.finance.core.time

import com.saldoclaro.finance.domain.usecase.currentMonth
import java.time.Clock
import java.time.YearMonth
import java.time.ZoneId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

interface CurrentMonthSource {
    val month: StateFlow<YearMonth>
    fun setForeground(active: Boolean)
    fun refresh()
}

class SystemCurrentMonthSource(
    private val clock: Clock,
    private val zone: ZoneId,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default),
) : CurrentMonthSource {
    private val _month = MutableStateFlow(currentMonth(clock, zone))
    private var watcher: Job? = null
    override val month: StateFlow<YearMonth> = _month

    override fun setForeground(active: Boolean) {
        watcher?.cancel()
        watcher = if (active) scope.launch { while (isActive) { delay(nextBoundaryDelayMillis()); refresh() } } else null
        if (active) refresh()
    }

    override fun refresh() {
        val current = currentMonth(clock, zone)
        if (_month.value != current) _month.value = current
    }

    private fun nextBoundaryDelayMillis() = (_month.value.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli() - clock.instant().toEpochMilli()).coerceAtLeast(1L)
}
