package com.example.kenkosahealthapp_prog7314.ui.addmenu

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.navigation.fragment.findNavController
import com.example.kenkosahealthapp_prog7314.R
import com.example.kenkosahealthapp_prog7314.databinding.BottomSheetAddMenuBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class AddMenuBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetAddMenuBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetAddMenuBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val navController = requireParentFragment().findNavController()

        // Log Food Card Action
        binding.cardLogFood.setOnClickListener {
            navController.navigate(R.id.action_global_mealHistory)
            dismiss()
        }

        // Water Card Action
        binding.cardWater.setOnClickListener {
            navController.navigate(R.id.action_global_water)
            dismiss()
        }

        // Weight Card Action
        binding.cardWeight.setOnClickListener {
            // TODO: Navigate to weight screen once destination action exists
            // navController.navigate(R.id.action_global_weight)
            dismiss()
        }
    }

    override fun onStart() {
        super.onStart()

        // Remove default white bottom sheet wrapper background to show the layout's rounded corners
        val dialog = dialog as? BottomSheetDialog
        val bottomSheet = dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet) as? FrameLayout

        bottomSheet?.let { sheet ->
            sheet.setBackgroundColor(Color.TRANSPARENT)
            val behavior = BottomSheetBehavior.from(sheet)
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}