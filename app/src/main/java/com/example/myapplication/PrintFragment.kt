package com.example.myapplication


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.example.myapplication.databinding.FragmentPrintBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment


class PrintFragment : Fragment() {
    private lateinit var binding: FragmentPrintBinding
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_print, container, false)

        binding.lifecycleOwner = this.viewLifecycleOwner

binding.button.setOnClickListener {

    val bottomSheetDialog = PrinterListFragment()

    bottomSheetDialog.show(parentFragmentManager,"ModalBottomSheet")


}



        return binding.root
    }

}