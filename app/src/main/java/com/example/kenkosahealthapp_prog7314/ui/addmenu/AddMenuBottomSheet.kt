package com.example.kenkosahealthapp_prog7314.ui.addmenu


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.NavHostFragment
import com.example.kenkosahealthapp_prog7314.R
import com.example.kenkosahealthapp_prog7314.databinding.BottomSheetAddMenuBinding

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

        binding.optionLogFood.setOnClickListener {
            navController.navigate(R.id.action_global_mealHistory)
            dismiss()
        }

        // TODO: wire these up once their destinations + nav actions exist
        binding.optionBarcodeScan.setOnClickListener {
            dismiss() // temporarily does nothing but close the sheet
        }
        binding.optionVoiceLog.setOnClickListener {
            dismiss()
        }
        binding.optionWater.setOnClickListener {
            dismiss()
        }
        binding.optionWeight.setOnClickListener {
            dismiss()
        }
        binding.optionExercise.setOnClickListener {
            dismiss()
        }
        binding.optionLogFood.setOnClickListener {
            navController.navigate(R.id.action_global_mealHistory)
            dismiss()
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}