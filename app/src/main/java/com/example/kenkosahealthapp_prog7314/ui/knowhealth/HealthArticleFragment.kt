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

/**
 * One article, read-only: a coloured banner, the title, how long it takes to read, the text (sub-headings and
 * paragraphs) and a short "not medical advice" note.
 */
class HealthArticleFragment : Fragment() {

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

        setupWindowInsets()
        setupHeader()
        setupBottomNav()

        val article = HealthArticles.byId(arguments?.getString(ARG_ARTICLE_ID))
        if (article == null) {
            findNavController().navigateUp() // unknown article: nothing to show
            return
        }
        showArticle(article)
    }

    // ---------------------------------------------------------------- setup

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

    private fun setupHeader() {
        binding.buttonMenu.setOnClickListener {
            val nav = findNavController()
            runCatching {
                nav.getBackStackEntry(R.id.homeFragment).savedStateHandle.set(HomeFragment.KEY_OPEN_DRAWER, true)
            }
            nav.popBackStack(R.id.homeFragment, false)
        }
        binding.buttonBack.setOnClickListener { findNavController().navigateUp() }
        // Notification bell is intentionally non-functional for now (same as the other screens).
    }

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

    private fun showArticle(article: HealthArticle) {
        val context = requireContext()

        binding.viewBanner.background = HealthArticles.gradient(context, article)
        binding.textTitle.setText(article.titleRes)
        binding.textMeta.text = getString(R.string.kyh_min_read, HealthArticles.readingMinutes(context, article))

        binding.layoutBody.removeAllViews()
        for (line in HealthArticles.body(context, article)) {
            if (line.startsWith(HEADING_PREFIX)) {
                addHeading(line.removePrefix(HEADING_PREFIX))
            } else {
                addParagraph(line)
            }
        }
    }

    private fun addHeading(text: String) {
        binding.layoutBody.addView(
            newText(text, sizeSp = 17f, colorRes = R.color.settings_title, bold = true, topMarginDp = 20)
        )
    }

    private fun addParagraph(text: String) {
        binding.layoutBody.addView(
            newText(text, sizeSp = 15f, colorRes = R.color.settings_text, bold = false, topMarginDp = 10)
        )
    }

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

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val ARG_ARTICLE_ID = "articleId"
        private const val HEADING_PREFIX = "## "
    }
}
