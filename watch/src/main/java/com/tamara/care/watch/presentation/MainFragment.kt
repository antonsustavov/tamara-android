package com.tamara.care.watch.presentation

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.tamara.care.watch.R
import com.google.zxing.BarcodeFormat
import com.tamara.care.watch.data.eventBus.EventServiceDie
import com.tamara.care.watch.data.eventBus.MessageEventBus
import com.tamara.care.watch.databinding.FragmentMainBinding
import com.tamara.care.watch.manager.SharedPreferencesManager
import com.tamara.care.watch.manager.TrackWorker
import com.tamara.care.watch.service.TrackingService
import com.tamara.care.watch.service.TrackingService.Companion.isServiceTracking
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import android.graphics.Bitmap
import android.graphics.Color

import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.EncodeHintType
import com.tamara.care.watch.utils.BeepHelper
import java.util.*


@AndroidEntryPoint
class MainFragment : Fragment() {

    /* to connect watch with android studio - input code code into terminal
     adb connect 192.168.88.140:5555 */

    companion object {
        private const val HEART_RATE_CODE = 9379992
        private const val LOCATION_CODE = 9379991
    }

    private val compositeDisposable = CompositeDisposable()

    private lateinit var binding: FragmentMainBinding
    lateinit var bitmap: Bitmap

    @Inject
    lateinit var sharedPreferencesManager: SharedPreferencesManager
    @Inject
    lateinit var beepHelper: BeepHelper

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_main, container, false)
        binding.lifecycleOwner = this

        return binding.root
    }

    @SuppressLint("SetTextI18n")
    override fun onResume() {
        super.onResume()
        binding.transmitterId.text = "id: ${sharedPreferencesManager.transmitterId}"
        updateBackground()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (sharedPreferencesManager.userName.isNullOrEmpty()) {
            findNavController().navigate(R.id.TransmitterFragment)
        } else {
            requestPermission()
        }
        setupClicks()
        observeEventBus()
    }

    private fun observeEventBus() {
        compositeDisposable.add(
            MessageEventBus.toObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { eventModel ->
                    when (eventModel) {
                        is EventServiceDie -> {
                            updateBackground()
                        }
                    }
                }
        )
    }

    private fun setupClicks() {
        binding.restart.setOnClickListener {
            requireContext().startForegroundService(
                Intent(
                    requireActivity(),
                    TrackingService::class.java
                )
            )
            Handler(Looper.getMainLooper()).postDelayed({
                updateBackground()
            }, 1000L)
            createQrCode()
//            beepHelper.beep(2000)
        }

    }

    private fun createQrCode() {
        bitmap = textToImageEncode(sharedPreferencesManager.transmitterId)
        binding.imageView.setImageBitmap(bitmap)
    }

    @Throws(WriterException::class)
    fun textToImageEncode(Value: String?): Bitmap {
        val hints: MutableMap<EncodeHintType, Any> = EnumMap(EncodeHintType::class.java)
        hints[EncodeHintType.CHARACTER_SET] = "UTF-8"
        hints[EncodeHintType.MARGIN] = 0
        val bitMatrix = QRCodeWriter().encode(Value, BarcodeFormat.QR_CODE, 140, 140, hints)
        val bitMatrixWidth = bitMatrix.width
        val bitMatrixHeight = bitMatrix.height
        val bitMap = Bitmap.createBitmap(bitMatrixWidth, bitMatrixHeight, Bitmap.Config.RGB_565)
        for (x in 0 until bitMatrixWidth) {
            for (y in 0 until bitMatrixHeight) {
                bitMap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }

        return bitMap
    }

    private fun updateBackground() {
        if (isServiceTracking) {
            binding.background.setBackgroundColor(
                requireContext().resources.getColor(
                    R.color.green,
                    null
                )
            )
            binding.serviceStatus.text = requireContext().getString(R.string.service_is_active)
            binding.restart.visibility = View.GONE
            binding.imageView.visibility = View.VISIBLE
        } else {
            binding.background.setBackgroundColor(
                requireContext().resources.getColor(
                    R.color.red,
                    null
                )
            )
            binding.serviceStatus.text = requireContext().getString(R.string.service_is_inactive)
            binding.restart.visibility = View.VISIBLE
        }
    }

    private fun requestPermission() {
        when {
            ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.BODY_SENSORS
            ) != PackageManager.PERMISSION_GRANTED -> {
                requestPermissions(arrayOf(Manifest.permission.BODY_SENSORS), HEART_RATE_CODE)
            }
            ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED -> {
                requestPermissions(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    LOCATION_CODE
                )
            }
            ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ANSWER_PHONE_CALLS
            ) != PackageManager.PERMISSION_GRANTED -> {
                requestPermissions(arrayOf(Manifest.permission.ANSWER_PHONE_CALLS), 93679991)
            }
            ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_PHONE_STATE
            ) != PackageManager.PERMISSION_GRANTED -> {
                requestPermissions(arrayOf(Manifest.permission.READ_PHONE_STATE), 94379991)
            }
            ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_CALL_LOG
            ) != PackageManager.PERMISSION_GRANTED -> {
                requestPermissions(arrayOf(Manifest.permission.READ_CALL_LOG), 93799915)
            }
            else -> {

//                requireContext().startForegroundService(
//                    Intent(
//                        requireActivity(),
//                        TrackingService::class.java
//                    )
//                )
                try {
                    initWorker()
                } catch (e: Exception) {
                }

            }
        }
    }

    private fun initWorker() {
        val worker =
            PeriodicWorkRequest.Builder(TrackWorker::class.java, 15L, TimeUnit.MINUTES).build()
        WorkManager.getInstance(requireContext()).enqueueUniquePeriodicWork(
            TrackWorker::javaClass.name,
            ExistingPeriodicWorkPolicy.KEEP,
            worker
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        requestPermission()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (!compositeDisposable.isDisposed) {
            compositeDisposable.dispose()
        }
    }
}