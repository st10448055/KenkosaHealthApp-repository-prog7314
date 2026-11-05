package com.example.kenkosahealthapp_prog7314.ui.home

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.annotation.StringRes
import androidx.core.os.bundleOf
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.kenkosahealthapp_prog7314.R
import com.example.kenkosahealthapp_prog7314.data.model.UserMetrics
import com.example.kenkosahealthapp_prog7314.data.repository.AuthRepository
import com.example.kenkosahealthapp_prog7314.databinding.FragmentHomeBinding
import com.example.kenkosahealthapp_prog7314.databinding.ItemHomeFeatureBinding
import com.example.kenkosahealthapp_prog7314.ui.addmenu.AddMenuBottomSheet
import com.example.kenkosahealthapp_prog7314.ui.metrics.MetricsFragment
import com.example.kenkosahealthapp_prog7314.ui.metrics.MetricsPromptState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.kenkosahealthapp_prog7314.KenkosaApp
import com.example.kenkosahealthapp_prog7314.notifications.FcmTokenManager
import com.example.kenkosahealthapp_prog7314.notifications.NotificationPreferences
import com.example.kenkosahealthapp_prog7314.security.BiometricPreferences
import com.example.kenkosahealthapp_prog7314.ui.auth.AuthViewModelFactory

// Home Fragment serving as the main hub containing feature navigation, user greeting, and calorie progress ring
class HomeFragment : Fragment() {

    // View binding backing property
    private var _binding: FragmentHomeBinding? = null

    // View binding property accessor
    private val binding get() = _binding!!

    // Firebase Auth instance reference
    private val auth = FirebaseAuth.getInstance()

    // Firestore database instance reference
    private val firestore = FirebaseFirestore.getInstance()

    // ViewModel instance for managing calorie ring progress
    private val calorieViewModel: CalorieRingViewModel by viewModels()

    // Android 13+ asks before an app may show notifications. If allowed, register for push right away.
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) FcmTokenManager.syncInBackground(KenkosaApp.instance)
        }

    // Inflates the fragment view layout
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    // Initialises fragment UI logic, observers, navigation listeners, and drawer setups
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeCalories()
        calorieViewModel.loadCalories()
        setupDrawer()
        setupGreeting()
        setupFeatureGrid()
        setupDrawerNavigation()
        setupAddButton()
        setupBottomNav()

        observeOpenDrawerRequest()
        askForNotificationPermissionOnce()

        // Notification bell is intentionally non-functional for now.
    }

    // Reloads calorie data when returning to home view
    override fun onResume() {
        super.onResume()
        calorieViewModel.loadCalories()
    }

    // Observes request from other screens to open navigation drawer when navigating back
    private fun observeOpenDrawerRequest() {
        val handle = findNavController().currentBackStackEntry?.savedStateHandle ?: return
        handle.getLiveData<Boolean>(KEY_OPEN_DRAWER).observe(viewLifecycleOwner) { open ->
            if (open == true) {
                binding.drawerLayout.openDrawer(GravityCompat.START)
                handle.remove<Boolean>(KEY_OPEN_DRAWER)
            }
        }
    }

    // Listens for calorie UI state updates and updates the card UI components
    private fun observeCalories() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                calorieViewModel.uiState.collect { state ->
                    val card = binding.cardCalorieRing
                    when (state) {
                        is CalorieUiState.Loading -> {
                            card.progressCalorieRing.visibility = View.VISIBLE
                            card.textCalorieError.visibility = View.GONE
                        }
                        is CalorieUiState.Success -> {
                            card.progressCalorieRing.visibility = View.GONE
                            card.textCalorieError.visibility = View.GONE
                            card.textCaloriesRemaining.text = "${state.remaining} kcal"
                            card.textMacroCarbs.text = "Carbs: ${state.carbsG}g"
                            card.textMacroProtein.text = "Protein: ${state.proteinG}g"
                            card.textMacroFat.text = "Fats: ${state.fatG}g"
                            card.textCalorieStatus.text = state.statusLabel
                            card.ringCalories.consumedFraction = state.consumed.toFloat() / state.target.toFloat()
                        }
                        is CalorieUiState.Error -> {
                            card.progressCalorieRing.visibility = View.GONE
                            card.textCalorieError.visibility = View.VISIBLE
                            card.textCalorieError.text = state.message
                        }
                    }
                }
            }
        }
    }

    // Sets up navigation drawer toggle and back button drawer closure logic
    private fun setupDrawer() {
        binding.buttonMenu.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                        binding.drawerLayout.closeDrawer(GravityCompat.START)
                    } else {
                        isEnabled = false
                        requireActivity().onBackPressedDispatcher.onBackPressed()
                        isEnabled = true
                    }
                }
            }
        )
    }

    // Fetches user profile data from Firestore to personalize header greeting
    private fun setupGreeting() {
        val user = auth.currentUser
        binding.navDrawerHome.textDrawerEmail.text = user?.email.orEmpty()

        val uid = user?.uid
        if (uid == null) {
            binding.textGreeting.text = getString(R.string.hi_placeholder, "User")
            return
        }

        firestore.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                val b = _binding ?: return@addOnSuccessListener   // view already destroyed
                val name = doc.getString("name").orEmpty()
                val surname = doc.getString("surname").orEmpty()
                val fullName = "$name $surname".trim()
                val display = fullName.ifEmpty { user.email ?: "User" }

                b.textGreeting.text = getString(R.string.hi_placeholder, display)
                b.navDrawerHome.textDrawerName.text = display

                // First login of a new account: ask for body metrics.
                promptForMetricsIfNeeded(uid, doc)
            }
            .addOnFailureListener {
                _binding?.textGreeting?.text = getString(R.string.hi_placeholder, "User")
            }
    }

    // Directs newly registered users to the metrics setup flow if required
    private fun promptForMetricsIfNeeded(uid: String, doc: DocumentSnapshot) {
        if (!UserMetrics.isPromptPending(doc)) return
        if (MetricsPromptState.skippedForUid == uid) return

        val nav = findNavController()
        if (nav.currentDestination?.id != R.id.homeFragment) return
        nav.navigate(R.id.action_global_metrics, bundleOf(MetricsFragment.ARG_ONBOARDING to true))
    }

    // Sets click listener for the bottom FAB to display quick add options
    private fun setupAddButton() {
        binding.bottomNav.fabAdd.setOnClickListener {
            AddMenuBottomSheet().show(childFragmentManager, "AddMenuBottomSheet")
        }
    }

    // Binds navigation click handlers to bottom navigation items
    private fun setupBottomNav() {
        with(binding.bottomNav) {
            navHome.setOnClickListener { /* already home, no-op */ }
            navGoals.setOnClickListener { findNavController().navigate(R.id.action_home_to_goals) }
            navStats.setOnClickListener { findNavController().navigate(R.id.action_global_statistics) }
            navSettings.setOnClickListener { findNavController().navigate(R.id.action_home_to_settings) }
        }
    }

    // Initialises home screen feature grid cards with icons, labels, colors, and navigation destinations
    private fun setupFeatureGrid() {
        bindFeature(binding.cardTrackingDiary, R.drawable.ic_diary, R.string.home_tracking_diary, Color.parseColor("#6B9080"), Color.parseColor("#E3ECE7")) {
            findNavController().navigate(R.id.action_home_to_trackingDiary)
        }
        bindFeature(binding.cardNutrition, R.drawable.ic_nutrition, R.string.home_nutrition, Color.parseColor("#4F7A5B"), Color.parseColor("#E0EBE3")) {
            findNavController().navigate(R.id.action_global_mealHistory)
        }
        bindFeature(binding.cardSteps, R.drawable.ic_wellness, R.string.home_steps, Color.parseColor("#3F8C7E"), Color.parseColor("#DFEFEC")) {
            findNavController().navigate(R.id.action_home_to_steps)
        }
        bindFeature(binding.cardWorkouts, R.drawable.ic_fitness, R.string.home_fitness, Color.parseColor("#1B3A1B"), Color.parseColor("#E2E8E2")) {
            findNavController().navigate(R.id.action_home_to_workouts)
        }
        bindFeature(binding.cardFasting, R.drawable.ic_fasting, R.string.home_fasting, Color.parseColor("#B08968"), Color.parseColor("#F3E8DE")) {
            findNavController().navigate(R.id.action_home_to_fasting)
        }
        bindFeature(binding.cardSleep, R.drawable.ic_sleep, R.string.home_sleep, Color.parseColor("#3B5166"), Color.parseColor("#E3E8EC")) {
            findNavController().navigate(R.id.action_home_to_sleep)
        }
        bindFeature(binding.cardMedication, R.drawable.ic_medication, R.string.home_medication, Color.parseColor("#C17767"), Color.parseColor("#F5E3DF")) {
            findNavController().navigate(R.id.action_home_to_medication)
        }
        bindFeature(binding.cardGoals, R.drawable.ic_goals, R.string.home_goals, Color.parseColor("#BFA05A"), Color.parseColor("#F5EEDC")) {
            findNavController().navigate(R.id.action_home_to_goals)
        }
    }

    // Utility function to bind dynamic icons, text, colors, and click actions to home feature card views
    private fun bindFeature(
        card: ItemHomeFeatureBinding,
        iconRes: Int,
        @StringRes labelRes: Int,
        accentColor: Int,
        cardTint: Int,
        onClick: () -> Unit
    ) {
        card.iconFeature.setImageResource(iconRes)
        card.textFeature.setText(labelRes)
        card.iconCircle.background.setTint(accentColor)
        card.root.setCardBackgroundColor(cardTint)
        card.root.setOnClickListener { onClick() }
    }

    // Configures navigation click listeners and logout action for the navigation drawer items
    private fun setupDrawerNavigation() {
        val drawer = binding.navDrawerHome

        drawer.navHome.setOnClickListener { closeDrawer() }

        drawer.navTrackingDiary.setOnClickListener {
            closeDrawer(); findNavController().navigate(R.id.action_home_to_trackingDiary)
        }
        drawer.navNutrition.setOnClickListener {
            closeDrawer(); findNavController().navigate(R.id.action_global_mealHistory)
        }
        drawer.navFitness.setOnClickListener {
            closeDrawer(); findNavController().navigate(R.id.action_home_to_workouts)
        }
        drawer.navFasting.setOnClickListener {
            closeDrawer(); findNavController().navigate(R.id.action_home_to_fasting)
        }
        drawer.navSleep.setOnClickListener {
            closeDrawer(); findNavController().navigate(R.id.action_home_to_sleep)
        }
        drawer.navMedication.setOnClickListener {
            closeDrawer(); findNavController().navigate(R.id.action_home_to_medication)
        }
        drawer.navGoals.setOnClickListener {
            closeDrawer(); findNavController().navigate(R.id.action_home_to_goals)
        }
        drawer.navMeditation.setOnClickListener {
            closeDrawer(); findNavController().navigate(R.id.action_home_to_wellness)
        }
        drawer.navSettings.setOnClickListener {
            closeDrawer(); findNavController().navigate(R.id.action_home_to_settings)
        }
        drawer.navKnowYourHealth.setOnClickListener {
            closeDrawer(); findNavController().navigate(R.id.action_global_knowYourHealth)
        }

        drawer.buttonDrawerLogout.setOnClickListener {
            drawer.buttonDrawerLogout.isEnabled = false   // avoid a second tap while we clean up
            val nav = findNavController()
            val appContext = requireContext().applicationContext
            // Runs on the activity so it isn't cancelled when this fragment goes away.
            requireActivity().lifecycleScope.launch {
                // Stop push notifications for this phone first (needs the login still active).
                FcmTokenManager.detachDevice(appContext)
                auth.signOut()
                // Also clear Credential Manager's remembered Google sign-in
                AuthRepository(appContext, AuthViewModelFactory.WEB_CLIENT_ID).signOut()
                nav.navigate(
                    R.id.startFragment,
                    null,
                    androidx.navigation.navOptions { popUpTo(R.id.nav_graph) { inclusive = true } }
                )
            }
        }
    }

    // Shows the system notification permission prompt the first time Home opens. Asked once only.
    private fun askForNotificationPermissionOnce() {
        val ctx = requireContext()
        val prefs = NotificationPreferences(ctx)
        val granted = ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        if (granted || prefs.permissionAsked) return
        prefs.permissionAsked = true
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    // Closes the navigation drawer menu
    private fun closeDrawer() {
        binding.drawerLayout.closeDrawer(GravityCompat.START)
    }

    // Nullifies view binding reference to prevent memory leaks when fragment view is destroyed
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val KEY_OPEN_DRAWER = "open_drawer"
    }
}
//---------------------------------------END OF FILE-------------------------------------------------