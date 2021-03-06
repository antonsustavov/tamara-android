package com.tamara.care.watch.presentation

import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.tamara.care.watch.R
import com.tamara.care.watch.data.model.ModelState
import com.tamara.care.watch.databinding.FragmentTransmitterBinding
import com.tamara.care.watch.manager.SharedPreferencesManager
import com.tamara.care.watch.utils.showToast
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import com.tamara.care.watch.utils.hideKeyBoard

@AndroidEntryPoint
class TransmitterFragment : Fragment() {


    lateinit var binding: FragmentTransmitterBinding

    private val viewModel: TransmitterViewModel by viewModels()

    @Inject
    lateinit var sharedPreferencesManager: SharedPreferencesManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_transmitter, container, false)
        binding.lifecycleOwner = this
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViews()
        setupViewModeCallbacks()
    }

    private fun setupViews() {
        binding.buttonInfo.setOnClickListener {
            viewModel.sendTransmitterInfo()
        }
        binding.inputName.doAfterTextChanged {
            viewModel.nameLiveData.postValue(it.toString())
        }

        binding.inputName.setOnKeyListener(object : View.OnKeyListener {
            override fun onKey(v: View?, keyCode: Int, event: KeyEvent?): Boolean {
                if (event?.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                    (activity as MainActivity).hideKeyBoard()
                    return true
                }
                return false
            }
        })
    }

    private fun setupViewModeCallbacks() {
        viewModel.registerTransmitterLiveData.observe(viewLifecycleOwner) {
            when (it) {
                is ModelState.Success -> {
                    sharedPreferencesManager.userName = it.data.name
                    sharedPreferencesManager.transmitterId = it.data._id
                    findNavController().popBackStack()
                }
                is ModelState.Error -> {
                    requireContext().showToast(it.error.errorMessage)
                }
                is ModelState.Loading -> {

                }
            }
        }
    }
}