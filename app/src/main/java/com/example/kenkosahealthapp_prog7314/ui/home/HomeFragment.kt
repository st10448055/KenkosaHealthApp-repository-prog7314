package com.example.kenkosahealthapp_prog7314.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.kenkosahealthapp_prog7314.R
import com.example.kenkosahealthapp_prog7314.databinding.FragmentHomeBinding
import com.example.kenkosahealthapp_prog7314.databinding.ItemHomeFeatureBinding
import com.example.kenkosahealthapp_prog7314.ui.addmenu.AddMenuBottomSheet
import androidx.core.os.bundleOf
import com.google.firebase.firestore.DocumentSnapshot
import com.example.kenkosahealthapp_prog7314.data.model.UserMetrics
import com.example.kenkosahealthapp_prog7314.ui.metrics.MetricsFragment
import com.example.kenkosahealthapp_prog7314.ui.metrics.MetricsPromptState

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupDrawer()
        setupGreeting()
        setupFeatureGrid()
        setupDrawerNavigation()
        setupAddButton()
        observeOpenDrawerRequest()

        // Notification bell is intentionally non-functional for now.
    }

    /** Settings' menu button pops back here and asks for the drawer to be opened. */
    private fun observeOpenDrawerRequest() {
        val handle = findNavController().currentBackStackEntry?.savedStateHandle ?: return
        handle.getLiveData<Boolean>(KEY_OPEN_DRAWER).observe(viewLifecycleOwner) { open ->
            if (open == true) {
                binding.drawerLayout.openDrawer(GravityCompat.START)
                handle.remove<Boolean>(KEY_OPEN_DRAWER)
            }
        }
    }

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

                // First login: ask for body metrics if they haven't been given yet.
                promptForMetricsIfNeeded(uid, doc)
            }
            .addOnFailureListener {
                _binding?.textGreeting?.text = getString(R.string.hi_placeholder, "User")
            }
    }

    /**
     * A user who hasn't given their body metrics yet is sent to the metrics form.
     * (Existing users without metrics get it once too; "Skip for now" silences it until the app restarts.)
     */
    private fun promptForMetricsIfNeeded(uid: String, doc: DocumentSnapshot) {
        if (UserMetrics.fromSnapshot(doc) != null) return
        if (MetricsPromptState.skippedForUid == uid) return

        val nav = findNavController()
        if (nav.currentDestination?.id != R.id.homeFragment) return
        nav.navigate(R.id.action_global_metrics, bundleOf(MetricsFragment.ARG_ONBOARDING to true))
    }

    private fun setupAddButton() {
        binding.fabAdd.setOnClickListener {
            AddMenuBottomSheet().show(childFragmentManager, "AddMenuBottomSheet")
        }
    }


    private fun setupFeatureGrid() {
        bindFeature(binding.cardTrackingDiary, R.drawable.ic_diary, "Tracking Diary") {
            findNavController().navigate(R.id.action_home_to_trackingDiary)
        }
        bindFeature(binding.cardNutrition, R.drawable.ic_nutrition, "Nutrition") {
            findNavController().navigate(R.id.action_home_to_nutrition)
        }
        bindFeature(binding.cardSteps, R.drawable.ic_steps, "Steps") {
            findNavController().navigate(R.id.action_home_to_steps)
        }
        bindFeature(binding.cardWorkouts, R.drawable.ic_workouts, "Workouts") {
            findNavController().navigate(R.id.action_home_to_workouts)
        }
        bindFeature(binding.cardFasting, R.drawable.ic_fasting, "Fasting") {
            findNavController().navigate(R.id.action_home_to_fasting)
        }
        bindFeature(binding.cardSleep, R.drawable.ic_sleep, "Sleep") {
            findNavController().navigate(R.id.action_home_to_sleep)
        }
        bindFeature(binding.cardMedication, R.drawable.ic_medication, "Medication") {
            findNavController().navigate(R.id.action_home_to_medication)
        }
        bindFeature(binding.cardGoals, R.drawable.ic_goals, "Goals") {
            findNavController().navigate(R.id.action_home_to_goals)
        }
    }

    private fun bindFeature(
        card: ItemHomeFeatureBinding,
        iconRes: Int,
        label: String,
        onClick: () -> Unit
    ) {
        card.iconFeature.setImageResource(iconRes)
        card.textFeature.text = label
        card.root.setOnClickListener { onClick() }
    }

    private fun setupDrawerNavigation() {
        val drawer = binding.navDrawerHome

        drawer.navHome.setOnClickListener { closeDrawer() }

        drawer.navTrackingDiary.setOnClickListener {
            closeDrawer(); findNavController().navigate(R.id.action_home_to_trackingDiary)
        }
        drawer.navNutrition.setOnClickListener {
            closeDrawer(); findNavController().navigate(R.id.action_home_to_nutrition)
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

        // Fitness / Know Your Health / Statistics: no destinations yet,
        // wire these up once those fragments + nav actions exist.

        drawer.buttonDrawerLogout.setOnClickListener {
            auth.signOut()
            findNavController().navigate(
                R.id.startFragment,
                null,
                androidx.navigation.navOptions { popUpTo(R.id.nav_graph) { inclusive = true } }
            )
        }
    }

    private fun closeDrawer() {
        binding.drawerLayout.closeDrawer(GravityCompat.START)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val KEY_OPEN_DRAWER = "open_drawer"
    }
}