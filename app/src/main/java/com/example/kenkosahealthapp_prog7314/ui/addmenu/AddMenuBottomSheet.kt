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


 //Bottom sheet dialog that acts as a quick-action menu, lets the user
 //jump to logging food, water, or weight from anywhere in the app.

class AddMenuBottomSheet : BottomSheetDialogFragment() {

    // Nullable backing field for view binding, cleared in onDestroyView to avoid leaks
    private var _binding: BottomSheetAddMenuBinding? = null

    // Non-null accessor, only safe to use between onCreateView and onDestroyView
    private val binding get() = _binding!!

    //--------------------------------------------------------------------------------------------------------------------------------
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetAddMenuBinding.inflate(inflater, container, false)
        return binding.root
    }

    //--------------------------------------------------------------------------------------------------------------------------------
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Use the parent fragment's NavController since this bottom sheet
        // is shown on top of another fragment and shares its nav graph
        val navController = requireParentFragment().findNavController()

        //-----------------------------------------------------------------------
        // Log Food Card Action
        binding.cardLogFood.setOnClickListener {
            navController.navigate(R.id.action_global_mealHistory)
            dismiss()
        }

        //-----------------------------------------------------------------------
        // Water Card Action
        binding.cardWater.setOnClickListener {
            navController.navigate(R.id.action_global_water)
            dismiss()
        }
        //-----------------------------------------------------------------------
        // Weight Card Action
        binding.cardWeight.setOnClickListener {
            // TODO: Navigate to weight screen once destination action exists
            // navController.navigate(R.id.action_global_weight)
            dismiss()
        }
    }

    //--------------------------------------------------------------------------------------------------------------------------------
    override fun onStart() {
        super.onStart()

        // Remove default white bottom sheet wrapper background to show the layout's rounded corners
        val dialog = dialog as? BottomSheetDialog
        val bottomSheet = dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet) as? FrameLayout

        bottomSheet?.let { sheet ->
            // Make the wrapper transparent so our own rounded-corner background shows through
            sheet.setBackgroundColor(Color.TRANSPARENT)

            // Force the sheet fully open immediately instead of the default peek/collapsed state
            val behavior = BottomSheetBehavior.from(sheet)
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    //--------------------------------------------------------------------------------------------------------------------------------
    // Prevent memory leaks by releasing the binding reference
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
// ------------------------------------------------------- END OF FILE -----------------------------------------------------------------