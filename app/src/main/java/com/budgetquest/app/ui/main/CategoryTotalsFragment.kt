package com.budgetquest.app.ui.main

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.budgetquest.app.R
import com.budgetquest.app.databinding.FragmentCategoryTotalsBinding
import com.budgetquest.app.domain.CategorySpendSummary
import com.budgetquest.app.domain.GameLogic
import com.budgetquest.app.ui.AppViewModel
import com.budgetquest.app.util.FormatUtils
import java.time.LocalDate

/**
 * Category totals for a selectable period with limit progress and overspend highlighting.
 */
class CategoryTotalsFragment : Fragment() {
    private var _binding: FragmentCategoryTotalsBinding? = null
    private val binding get() = _binding!!
    private val vm by activityViewModels<AppViewModel>()
    private var startDate = LocalDate.now().withDayOfMonth(1).toString()
    private var endDate = LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth()).toString()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCategoryTotalsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.startDateTotals.text = startDate
        binding.endDateTotals.text = endDate
        binding.startDateTotals.setOnClickListener { pickDate { d -> startDate = d; binding.startDateTotals.text = d; load() } }
        binding.endDateTotals.setOnClickListener { pickDate { d -> endDate = d; binding.endDateTotals.text = d; load() } }
        vm.categoryTotals.observe(viewLifecycleOwner) { totals ->
            renderTotals(totals)
        }
        load()
    }

    private fun load() {
        vm.loadCategoryTotals(startDate, endDate)
    }

    private fun renderTotals(totals: List<CategorySpendSummary>) {
        binding.categoryTotalsContainer.removeAllViews()
        val totalSpent = totals.sumOf { it.spent }
        val overCount = totals.count { GameLogic.isCategoryOverLimit(it.spent, it.limit) }
        binding.totalsSummary.text = "Total: ${FormatUtils.zar(totalSpent)} • ${totals.size} categories • $overCount over limit"
        if (totals.isEmpty()) {
            val empty = TextView(requireContext()).apply {
                text = "No spending recorded for this period"
                setTextColor(ContextCompat.getColor(requireContext(), R.color.bq_text_muted))
            }
            binding.categoryTotalsContainer.addView(empty)
            return
        }
        totals.forEach { item ->
            binding.categoryTotalsContainer.addView(buildCategoryRow(item))
        }
    }

    private fun buildCategoryRow(item: CategorySpendSummary): View {
        val over = GameLogic.isCategoryOverLimit(item.spent, item.limit)
        val percent = GameLogic.categoryUsagePercent(item.spent, item.limit)
        val card = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_soft_card)
            setPadding(28, 28, 28, 28)
            val lp = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            lp.bottomMargin = 20
            layoutParams = lp
        }
        val title = TextView(requireContext()).apply {
            text = if (over) {
                "${item.emoji} ${item.name}  •  $percent% ⚠️"
            } else {
                "${item.emoji} ${item.name}  •  $percent%"
            }
            setTextColor(ContextCompat.getColor(requireContext(), if (over) R.color.bq_warning else R.color.bq_text_dark))
            textSize = 16f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
        val detail = TextView(requireContext()).apply {
            text = if (item.limit > 0.0) {
                "${FormatUtils.zar(item.spent)} of ${FormatUtils.zar(item.limit)} limit"
            } else {
                "${FormatUtils.zar(item.spent)} spent (no limit set)"
            }
            setTextColor(ContextCompat.getColor(requireContext(), R.color.bq_text_muted))
            textSize = 13f
        }
        val bar = ProgressBar(requireContext(), null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            progress = percent.coerceAtMost(100)
            progressTintList = android.content.res.ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), if (over) R.color.bq_warning else R.color.bq_safe)
            )
            val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 16)
            lp.topMargin = 10
            layoutParams = lp
        }
        card.addView(title)
        card.addView(detail)
        if (item.limit > 0.0) card.addView(bar)
        return card
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
