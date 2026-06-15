package com.budgetquest.app.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.budgetquest.app.R
import com.budgetquest.app.databinding.FragmentProfileBinding
import com.budgetquest.app.domain.GameLogic
import com.budgetquest.app.ui.AppViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Profile and gamification screen including badge states and navigation actions.
 */
class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val vm by activityViewModels<AppViewModel>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        vm.refreshUser()
        vm.currentUser.observe(viewLifecycleOwner) { user ->
            if (user != null) {
                binding.username.text = user.username
                val monthYear = Instant.ofEpochMilli(user.createdAt).atZone(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("MMMM yyyy"))
                binding.memberSince.text = "Member since $monthYear"
                binding.rank.text = vm.rankName()
                val nextXp = GameLogic.levelProgressMax(user.xp)
                binding.xpProgress.text = "${user.xp} XP • ${GameLogic.xpToNextLevel(user.xp)} to next level"
                binding.levelProgress.max = nextXp
                binding.levelProgress.progress = GameLogic.levelProgressValue(user.xp)
                binding.avatar.text = user.username.firstOrNull()?.uppercase() ?: "U"
            }
        }
        vm.badges.observe(viewLifecycleOwner) { badges ->
            val earned = badges.contains(GameLogic.BADGE_FIRST_ENTRY)
            binding.badgeFirst.alpha = if (earned) 1f else 0.35f
            binding.badgeFirst.text = if (earned) "🥇  First Entry — earned" else "🥇  First Entry — locked"
            val week = badges.contains(GameLogic.BADGE_WEEK_WARRIOR)
            binding.badgeWeek.alpha = if (week) 1f else 0.35f
            binding.badgeWeek.text = if (week) "⚔️  Week Warrior — earned" else "⚔️  Week Warrior — locked"
            val hero = badges.contains(GameLogic.BADGE_BUDGET_HERO)
            binding.badgeHero.alpha = if (hero) 1f else 0.35f
            binding.badgeHero.text = if (hero) "🏆  Budget Hero — earned" else "🏆  Budget Hero — locked"
        }
        binding.manageCategories.setOnClickListener { findNavController().navigate(R.id.action_profile_to_category_manager) }
        binding.setGoals.setOnClickListener { findNavController().navigate(R.id.action_profile_to_budget_goals) }
        binding.logoutBtn.setOnClickListener {
            vm.logout()
            findNavController().navigate(R.id.loginFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
