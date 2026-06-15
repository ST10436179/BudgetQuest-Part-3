package com.budgetquest.app.ui.main

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.budgetquest.app.R
import com.budgetquest.app.databinding.FragmentDashboardBinding
import com.budgetquest.app.domain.BudgetStatus
import com.budgetquest.app.domain.GameLogic
import com.budgetquest.app.ui.AppViewModel
import com.budgetquest.app.util.FormatUtils
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/**
 * Progress dashboard showing monthly spend against min/max goals with overspend highlights.
 */
class DashboardFragment : Fragment() {
    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private val vm by activityViewModels<AppViewModel>()
    private var categoryEmoji = emptyMap<Long, String>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        vm.refreshUser()
        vm.refreshMonthlySummary()
        vm.currentUser.observe(viewLifecycleOwner) { user ->
            if (user != null) {
                binding.welcome.text = "${user.username} 👋"
                binding.levelBadge.text = "Lv ${user.level}"
            }
        }
        vm.infoMessage.observe(viewLifecycleOwner) { msg ->
            if (!msg.isNullOrBlank()) {
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                vm.infoMessage.value = null
            }
        }
        vm.categoriesLive().observe(viewLifecycleOwner) { cats ->
            categoryEmoji = cats.associate { it.id to it.emoji }
            bindRecentExpenses()
        }
        vm.monthlySummary.observe(viewLifecycleOwner) { summary ->
            if (summary == null) return@observe
            binding.gaugeText.text = FormatUtils.zar(summary.totalSpent)
            binding.goalRangeText.text = "Goal range: ${FormatUtils.zar(summary.monthlyMin)} – ${FormatUtils.zar(summary.monthlyMax)}"
            binding.gauge.progress = GameLogic.progressTowardMax(summary.totalSpent, summary.monthlyMax)
            binding.statusCard1.text = statusLabel(summary.status)
            binding.statusCard2.text = "📁\n${summary.categoryCount}"
            binding.statusCard3.text = "🧾\n${summary.expenseCount}"
            binding.statusCard4.text = "⚠️\n${summary.overLimitCategories.size} Over"
            binding.budgetStatusText.text = when (summary.status) {
                BudgetStatus.ON_TRACK -> "On track between your min and max goals"
                BudgetStatus.UNDER_MIN -> "Below minimum spend goal"
                BudgetStatus.OVER_MAX -> "Above maximum spend goal"
                BudgetStatus.NO_GOALS -> "Set budget goals in Profile to track progress"
            }
            val tint = when (summary.status) {
                BudgetStatus.OVER_MAX -> R.color.bq_warning
                BudgetStatus.UNDER_MIN -> R.color.bq_accent
                BudgetStatus.ON_TRACK -> R.color.bq_safe
                BudgetStatus.NO_GOALS -> R.color.white
            }
            binding.budgetStatusText.setTextColor(ContextCompat.getColor(requireContext(), tint))
            binding.gauge.progressTintList = android.content.res.ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), tint)
            )
            bindOverspend(summary.overLimitCategories)
        }
        val now = LocalDate.now()
        val ym = YearMonth.from(now)
        val daysRemaining = ym.lengthOfMonth() - now.dayOfMonth
        binding.subtitle.text = "${now.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} • $daysRemaining days remaining • ${vm.rankName()}"
        bindRecentExpenses()
        binding.fab.setOnClickListener { findNavController().navigate(R.id.action_home_to_add_edit) }
    }

    private fun statusLabel(status: BudgetStatus): String = when (status) {
        BudgetStatus.ON_TRACK -> "🎯\nOn Track"
        BudgetStatus.UNDER_MIN -> "📉\nUnder Min"
        BudgetStatus.OVER_MAX -> "🚨\nOver Max"
        BudgetStatus.NO_GOALS -> "🎯\nNo Goals"
    }

    private fun bindOverspend(items: List<com.budgetquest.app.domain.CategorySpendSummary>) {
        binding.overspendContainer.removeAllViews()
        if (items.isEmpty()) {
            val ok = TextView(requireContext()).apply {
                text = "✅ All categories within limits this month"
                setTextColor(ContextCompat.getColor(requireContext(), R.color.bq_safe))
                textSize = 14f
            }
            binding.overspendContainer.addView(ok)
            return
        }
        items.forEach { item ->
            val row = TextView(requireContext()).apply {
                text = "${item.emoji} ${item.name} — ${FormatUtils.zar(item.spent)} of ${FormatUtils.zar(item.limit)} limit"
                setTextColor(ContextCompat.getColor(requireContext(), R.color.bq_warning))
                textSize = 14f
                setPadding(0, 8, 0, 8)
                setBackgroundColor(Color.parseColor("#33E53935"))
            }
            binding.overspendContainer.addView(row)
        }
    }

    private fun bindRecentExpenses() {
        val start = LocalDate.now().withDayOfMonth(1).toString()
        val end = LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth()).toString()
        vm.expensesLive(start, end).observe(viewLifecycleOwner) { list ->
            binding.recentListContainer.removeAllViews()
            list.take(3).forEach { e ->
                val emoji = categoryEmoji[e.categoryId] ?: "🧾"
                val row = LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setPadding(8, 10, 8, 10)
                }
                val left = TextView(requireContext()).apply {
                    text = "$emoji  ${e.description}\n${e.date}"
                    setTextColor(resources.getColor(R.color.bq_text_dark, null))
                    textSize = 14f
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }
                val right = TextView(requireContext()).apply {
                    text = FormatUtils.zar(e.amountZar)
                    setTextColor(resources.getColor(R.color.bq_accent, null))
                    textSize = 16f
                }
                row.addView(left)
                row.addView(right)
                row.setOnClickListener {
                    val bundle = Bundle().apply { putString("expenseId", e.id.toString()) }
                    findNavController().navigate(R.id.addEditExpenseFragment, bundle)
                }
                binding.recentListContainer.addView(row)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
