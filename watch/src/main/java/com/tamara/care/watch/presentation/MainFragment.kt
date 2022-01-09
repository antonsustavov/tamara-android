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
import com.google.zxing.common.BitMatrix
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

import com.google.zxing.MultiFormatWriter

import com.google.zxing.WriterException
import android.widget.ImageView


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
        }

    }

    private fun createQrCode() {
        bitmap = TextToImageEncode(sharedPreferencesManager.transmitterId)
        binding.imageView.setImageBitmap(bitmap)
    }

    @Throws(WriterException::class)
    fun TextToImageEncode(Value: String?): Bitmap {
        val bitMatrix: BitMatrix
        bitMatrix = MultiFormatWriter().encode(Value, BarcodeFormat.QR_CODE, 120, 120, null)

        val bitMatrixWidth = bitMatrix.width
        val bitMatrixHeight = bitMatrix.height
        val pixels = IntArray(bitMatrixWidth * bitMatrixHeight)
        for (y in 0 until bitMatrixHeight) {
            val offset = y * bitMatrixWidth
            for (x in 0 until bitMatrixWidth) {
                pixels[offset + x] =
                    if (bitMatrix[x, y]) Color.BLACK else Color.WHITE
            }
        }
        val bitmap: Bitmap =
            Bitmap.createBitmap(bitMatrixWidth, bitMatrixHeight, Bitmap.Config.ARGB_4444)
        bitmap.setPixels(pixels, 0, 120, 0, 0, bitMatrixWidth, bitMatrixHeight)
        return bitmap
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