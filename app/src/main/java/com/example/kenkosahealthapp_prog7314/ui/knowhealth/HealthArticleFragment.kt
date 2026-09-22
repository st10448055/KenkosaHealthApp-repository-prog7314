package com.example.kenkosahealthapp_prog7314.ui.knowhealth

import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import com.example.kenkosahealthapp_prog7314.ui.home.HomeFragment
import androidx.navigation.fragment.findNavController
import com.example.kenkosahealthapp_prog7314.R
import com.example.kenkosahealthapp_prog7314.databinding.FragmentHealthArticleBinding

// Displays full read-only content of a single selected article
class HealthArticleFragment : Fragment() {

    // ViewBinding instance lifecycle references
    private var _binding: FragmentHealthArticleBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHealthArticleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup screen padding, header actions, and navigation handlers
        setupWindowInsets()
        setupHeader()
        setupBottomNav()

        // Fetch target article using argument ID passed through Navigation component
        val article = HealthArticles.byId(arguments?.getString(ARG_ARTICLE_ID))
        if (article == null) {
            findNavController().navigateUp() // unknown article: nothing to show
            return
        }
        showArticle(article)
    }

    // ---------------------------------------------------------------- setup

    // Adjusts view padding for top system status bar and bottom navigation bar
    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            binding.scrollArticle.updatePadding(top = bars.top)
            binding.bottomNav.root.updatePadding(bottom = bars.bottom)
            insets
        }
    }

    // Configures header buttons for menu drawer and back navigation
    private fun setupHeader() {
        // Sets state flag on HomeFragment to open side navigation drawer
        binding.buttonMenu.setOnClickListener {
            val nav = findNavController()
            runCatching {
                nav.getBackStackEntry(R.id.homeFragment).savedStateHandle.set(HomeFragment.KEY_OPEN_DRAWER, true)
            }
            nav.popBackStack(R.id.homeFragment, false)
        }
        // Navigate back to previous screen
        binding.buttonBack.setOnClickListener { findNavController().navigateUp() }
        // Notification bell is intentionally non-functional for now (same as the other screens).
    }

    // Sets up click listeners for bottom navigation bar icons
    private fun setupBottomNav() {
        with(binding.bottomNav) {
            navHome.setOnClickListener { findNavController().popBackStack(R.id.homeFragment, false) }
            navGoals.setOnClickListener { findNavController().navigate(R.id.action_article_to_goals) }
            navStats.setOnClickListener { findNavController().navigate(R.id.action_article_to_statistics) }
            navSettings.setOnClickListener { findNavController().navigate(R.id.action_article_to_settings) }
            // Nothing to add on an info screen, so the + takes you back to Home where the trackers are.
            fabAdd.setOnClickListener { findNavController().popBackStack(R.id.homeFragment, false) }
        }
    }

    // ------------------------------------------------------------- content

    // Populates article header, metadata, and body views dynamically
    private fun showArticle(article: HealthArticle) {
        val context = requireContext()

        // Apply visual banner gradient, title, and reading time estimate
        binding.viewBanner.background = HealthArticles.gradient(context, article)
        binding.textTitle.setText(article.titleRes)
        binding.textMeta.text = getString(R.string.kyh_min_read, HealthArticles.readingMinutes(context, article))

        // Clear existing views and dynamically parse body string array
        binding.layoutBody.removeAllViews()
        for (line in HealthArticles.body(context, article)) {
            if (line.startsWith(HEADING_PREFIX)) {
                addHeading(line.removePrefix(HEADING_PREFIX))
            } else {
                addParagraph(line)
            }
        }
    }

    // Adds a styled heading TextView to the layout body
    private fun addHeading(text: String) {
        binding.layoutBody.addView(
            newText(text, sizeSp = 17f, colorRes = R.color.settings_title, bold = true, topMarginDp = 20)
        )
    }

    // Adds a styled paragraph TextView to the layout body
    private fun addParagraph(text: String) {
        binding.layoutBody.addView(
            newText(text, sizeSp = 15f, colorRes = R.color.settings_text, bold = false, topMarginDp = 10)
        )
    }

    // Factory method for creating custom styled TextViews for dynamic rendering
    private fun newText(text: String, sizeSp: Float, colorRes: Int, bold: Boolean, topMarginDp: Int): TextView =
        TextView(requireContext()).apply {
            this.text = text
            setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp)
            setTextColor(requireContext().getColor(colorRes))
            setLineSpacing(0f, 1.15f)
            if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(topMarginDp) }
        }

    // Converts DP values to pixels based on device display density
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    // Clear binding reference on view destroy to prevent memory leaks
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        // Navigation argument key for passing article ID
        const val ARG_ARTICLE_ID = "articleId"

        // Prefix used in raw string resources to designate subheadings
        private const val HEADING_PREFIX = "## "
    }
}
//---------------------------------------END OF FILE-------------------------------------------------