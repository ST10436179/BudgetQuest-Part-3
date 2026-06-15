package com.budgetquest.app.ui.main

import android.graphics.Color
import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.LiveData
import com.budgetquest.app.R
import com.budgetquest.app.data.db.ExpenseEntity
import com.budgetquest.app.databinding.FragmentSpendingGraphBinding
import com.budgetquest.app.ui.AppViewModel
import com.budgetquest.app.util.FormatUtils
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Category bar chart and daily trend line chart with monthly min/max goal reference lines.
 */
class SpendingGraphFragment : Fragment() {
    private var _binding: FragmentSpendingGraphBinding? = null
    private val binding get() = _binding!!
    private val vm by activityViewModels<AppViewModel>()
    private var startDate = LocalDate.now().withDayOfMonth(1).toString()
    private var endDate = LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth()).toString()
    private var categoryMap = emptyMap<Long, Pair<String, String>>()
    private var expenseLiveData: LiveData<List<ExpenseEntity>>? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSpendingGraphBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.startDateGraph.text = startDate
        binding.endDateGraph.text = endDate
        binding.startDateGraph.setOnClickListener { pickDate { d -> startDate = d; binding.startDateGraph.text = d; bindCharts() } }
        binding.endDateGraph.setOnClickListener { pickDate { d -> endDate = d; binding.endDateGraph.text = d; bindCharts() } }

        vm.categoriesLive().observe(viewLifecycleOwner) { cats ->
            categoryMap = cats.associate { it.id to (it.name to it.colorHex) }
            bindCharts()
        }
        vm.monthGoals.observe(viewLifecycleOwner) { bindCharts() }
        vm.refreshMonthlySummary()
    }

    private fun bindCharts() {
        expenseLiveData?.removeObservers(viewLifecycleOwner)
        expenseLiveData = vm.expensesLive(startDate, endDate)
        expenseLiveData?.observe(viewLifecycleOwner) { expenses ->
            val (monthlyMin, monthlyMax) = vm.monthGoals.value ?: (0.0 to 0.0)
            val total = expenses.sumOf { it.amountZar }
            binding.goalSummary.text =
                "Total spent: ${FormatUtils.zar(total)}  |  Min goal: ${FormatUtils.zar(monthlyMin)}  |  Max goal: ${FormatUtils.zar(monthlyMax)}"
            bindCategoryBarChart(expenses, monthlyMin, monthlyMax)
            bindDailyLineChart(expenses, monthlyMin, monthlyMax)
        }
    }

    private fun bindCategoryBarChart(expenses: List<ExpenseEntity>, monthlyMin: Double, monthlyMax: Double) {
        val grouped = expenses.groupBy { it.categoryId }
        if (grouped.isEmpty()) {
            binding.categoryChart.clear()
            binding.categoryChart.setNoDataText("No spending in selected range")
            return
        }
        val labels = mutableListOf<String>()
        val entries = mutableListOf<BarEntry>()
        val colors = mutableListOf<Int>()
        grouped.entries.forEachIndexed { index, (categoryId, list) ->
            val category = categoryMap[categoryId] ?: return@forEachIndexed
            labels.add(category.first)
            entries.add(BarEntry(index.toFloat(), list.sumOf { it.amountZar }.toFloat()))
            colors.add(parseColor(category.second, index))
        }
        val dataSet = BarDataSet(entries, "Category spend").apply {
            this.colors = colors
            valueTextColor = Color.WHITE
            valueTextSize = 10f
        }
        binding.categoryChart.data = BarData(dataSet)
        styleBarChart(binding.categoryChart)
        binding.categoryChart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        binding.categoryChart.xAxis.labelCount = labels.size
        binding.categoryChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        binding.categoryChart.xAxis.granularity = 1f
        binding.categoryChart.axisLeft.removeAllLimitLines()
        if (monthlyMax > 0.0) {
            binding.categoryChart.axisLeft.addLimitLine(limitLine(monthlyMax.toFloat(), "Max goal", Color.parseColor("#E53935")))
        }
        if (monthlyMin > 0.0) {
            binding.categoryChart.axisLeft.addLimitLine(limitLine(monthlyMin.toFloat(), "Min goal", Color.parseColor("#36D58D")))
        }
        binding.categoryChart.invalidate()
    }

    private fun bindDailyLineChart(expenses: List<ExpenseEntity>, monthlyMin: Double, monthlyMax: Double) {
        if (expenses.isEmpty()) {
            binding.graphLegend.text = "No daily trend data"
            binding.chart.clear()
            return
        }
        val start = LocalDate.parse(startDate)
        val end = LocalDate.parse(endDate)
        val days = ChronoUnit.DAYS.between(start, end).toInt().coerceAtLeast(0)
        val labels = (0..days).map { start.plusDays(it.toLong()).dayOfMonth.toString() }
        val dailyTotals = (0..days).map { offset ->
            val day = start.plusDays(offset.toLong()).toString()
            expenses.filter { it.date == day }.sumOf { it.amountZar }.toFloat()
        }
        val entries = dailyTotals.mapIndexed { index, value -> Entry(index.toFloat(), value) }
        val dataSet = LineDataSet(entries, "Daily total").apply {
            color = ContextCompat.getColor(requireContext(), R.color.bq_accent)
            setCircleColor(color)
            lineWidth = 2.6f
            circleRadius = 4f
            valueTextColor = Color.WHITE
            setDrawValues(false)
        }
        binding.chart.data = LineData(dataSet)
        styleLineChart(binding.chart)
        binding.chart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        binding.chart.xAxis.labelCount = labels.size.coerceAtMost(10)
        binding.chart.axisLeft.removeAllLimitLines()
        if (monthlyMax > 0.0) {
            binding.chart.axisLeft.addLimitLine(limitLine(monthlyMax.toFloat(), "Max", Color.parseColor("#E53935")))
        }
        if (monthlyMin > 0.0) {
            binding.chart.axisLeft.addLimitLine(limitLine(monthlyMin.toFloat(), "Min", Color.parseColor("#36D58D")))
        }
        binding.graphLegend.text = "Dashed lines show monthly min/max goals"
        binding.chart.invalidate()
    }

    private fun styleBarChart(chart: com.github.mikephil.charting.charts.BarChart) {
        chart.setBackgroundColor(Color.parseColor("#1F2344"))
        chart.description.isEnabled = false
        chart.axisLeft.textColor = Color.WHITE
        chart.axisRight.isEnabled = false
        chart.xAxis.textColor = Color.WHITE
        chart.legend.textColor = Color.WHITE
        chart.axisLeft.gridColor = Color.parseColor("#33405A")
        chart.xAxis.gridColor = Color.parseColor("#33405A")
    }

    private fun styleLineChart(chart: com.github.mikephil.charting.charts.LineChart) {
        chart.setBackgroundColor(Color.parseColor("#1F2344"))
        chart.description.isEnabled = false
        chart.axisLeft.textColor = Color.WHITE
        chart.axisRight.isEnabled = false
        chart.xAxis.textColor = Color.WHITE
        chart.legend.textColor = Color.WHITE
        chart.axisLeft.gridColor = Color.parseColor("#33405A")
        chart.xAxis.gridColor = Color.parseColor("#33405A")
    }

    private fun limitLine(value: Float, label: String, color: Int): LimitLine =
        LimitLine(value, label).apply {
            lineColor = color
            lineWidth = 1.5f
            textColor = color
            enableDashedLine(10f, 8f, 0f)
        }

    private fun parseColor(hex: String, seed: Int): Int = try {
        Color.parseColor(hex)
    } catch (_: Exception) {
        val palette = listOf("#FF8A65", "#4DD0E1", "#BA68C8", "#FFD54F", "#81C784", "#F06292")
        Color.parseColor(palette[kotlin.math.abs(seed) % palette.size])
    }

    private fun pickDate(onPicked: (String) -> Unit) {
        val now = LocalDate.now()
        DatePickerDialog(requireContext(), { _, y, m, d ->
            onPicked("%04d-%02d-%02d".format(y, m + 1, d))
        }, now.year, now.monthValue - 1, now.dayOfMonth).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
