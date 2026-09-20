package com.example.kenkosahealthapp_prog7314.ui.knowhealth

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.kenkosahealthapp_prog7314.R
import com.example.kenkosahealthapp_prog7314.databinding.FragmentKnowYourHealthBinding
import com.example.kenkosahealthapp_prog7314.ui.home.HomeFragment
import java.util.Locale

/** Know Your Health: a searchable list of short health articles. Tap a card to read it. */
class KnowYourHealthFragment : Fragment() {

    private var _binding: FragmentKnowYourHealthBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: HealthArticleAdapter

    /** article id -> lower-case title + text, built once so typing in the search box stays quick. */
    private lateinit var searchIndex: Map<String, String>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentKnowYourHealthBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupWindowInsets()
        setupHeader()
        setupBottomNav()

        searchIndex = HealthArticles.all.associate { it.id to HealthArticles.searchText(requireContext(), it) }

        adapter = HealthArticleAdapter(onClick = { article -> openArticle(article) })
        binding.recyclerArticles.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerArticles.adapter = adapter

        binding.editSearch.doAfterTextChanged { applyFilter(it?.toString().orEmpty()) }
        binding.editSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                hideKeyboard()
                true
            } else {
                false
            }
        }

        applyFilter(binding.editSearch.text.toString())
    }

    // ---------------------------------------------------------------- setup

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            binding.scrollKnow.updatePadding(top = bars.top)
            binding.bottomNav.root.updatePadding(bottom = bars.bottom)
            insets
        }
    }

    private fun setupHeader() {
        binding.buttonMenu.setOnClickListener {
            val nav = findNavController()
            runCatching {
                nav.getBackStackEntry(R.id.homeFragment).savedStateHandle.set(HomeFragment.KEY_OPEN_DRAWER, true)
            }
            nav.popBackStack(R.id.homeFragment, false)
        }
        // Notification bell is intentionally non-functional for now (same as the other screens).
    }

    private fun setupBottomNav() {
        with(binding.bottomNav) {
            navHome.setOnClickListener { findNavController().popBackStack(R.id.homeFragment, false) }
            navGoals.setOnClickListener { findNavController().navigate(R.id.action_kyh_to_goals) }
            navStats.setOnClickListener { findNavController().navigate(R.id.action_kyh_to_statistics) }
            navSettings.setOnClickListener { findNavController().navigate(R.id.action_kyh_to_settings) }
            // Nothing to add on an info screen, so the + takes you back to Home where the trackers are.
            fabAdd.setOnClickListener { findNavController().popBackStack(R.id.homeFragment, false) }
        }
    }

    // --------------------------------------------------------------- search

    /** Shows the articles whose title or text contains what was typed (all of them when the box is empty). */
    private fun applyFilter(query: String) {
        val needle = query.trim().lowercase(Locale.getDefault())
        val matches = if (needle.isEmpty()) {
            HealthArticles.all
        } else {
            HealthArticles.all.filter { searchIndex[it.id].orEmpty().contains(needle) }
        }
        adapter.submitList(matches)
        binding.textNoResults.isVisible = matches.isEmpty()
    }

    private fun hideKeyboard() {
        val manager = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        manager.hideSoftInputFromWindow(binding.editSearch.windowToken, 0)
        binding.editSearch.clearFocus()
    }

    // -------------------------------------------------------------- actions

    private fun openArticle(article: HealthArticle) {
        hideKeyboard()
        findNavController().navigate(
            R.id.action_kyh_to_article,
            bundleOf(HealthArticleFragment.ARG_ARTICLE_ID to article.id)
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
