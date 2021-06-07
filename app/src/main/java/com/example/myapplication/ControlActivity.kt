package com.example.myapplication

import android.R
import android.app.ProgressDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.os.AsyncTask
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.databinding.ControlLayoutBinding
//import io.github.controlwear.virtual.joystick.android.JoystickView
import java.io.IOException
import java.util.*


class ControlActivity : AppCompatActivity() {

    private lateinit var binding: ControlLayoutBinding
    private var direction = 0
    private var pinup = 1
    private var bypass = 0
    private var manual = 0
    private var semiauto = 0
    private var quadmode = 1

    lateinit var mainHandler: Handler

    private val updateTextTask = object : Runnable {
        override fun run() {
            sendNullMessage();
            mainHandler.postDelayed(this, 1000)
        }
    }
    // Looks like the uuid encodes to which serial port should the bluetooth connect to
    companion object {
        var m_myUUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        var m_bluetoothSocket: BluetoothSocket? = null
        lateinit var m_progress: ProgressDialog
        lateinit var m_bluetoothAdapter: BluetoothAdapter
        var m_isConnected: Boolean = false
        lateinit var m_address: String


    }

//    private val mTextViewAngleLeft: TextView? = null
//    private val mTextViewStrengthLeft: TextView? = null
//
//    private val mTextViewAngleRight: TextView? = null
//    private var mTextViewStrengthRight: TextView? = null
//    private var mTextViewCoordinateRight: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ControlLayoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        m_address = intent.getStringExtra(MainActivity.EXTRA_ADDRESS)!!

        ConnectToDevice(this).execute()
        binding.controlOn.setOnClickListener { sendCommand("a") }
        binding.controlOff.setOnClickListener { sendCommand("b") }
        binding.controlDisconnect.setOnClickListener { disconnect() }

        mainHandler = Handler(Looper.getMainLooper())


        binding.joystickViewRight.setOnMoveListener { angle, strength ->
            binding.textViewAngleRight.setText("$angle°")
            binding.textViewStrengthRight.setText("$strength%")
            binding.textViewCoordinateRight.setText(
                String.format(
                    "x%03d:y%03d",
                    binding.joystickViewRight.normalizedX - 50,
                    50 - binding.joystickViewRight.normalizedY
                )
            )
            if (50 - binding.joystickViewRight.normalizedY >= 0 && strength >= 10) {
                direction = 1
                sendCommand(
                    String.format(
                        "%02d:%05d:%03d:%01d:%01d:%01d:%01d:%01d\n",
                        direction,
                        angle,
                        strength,
                        pinup,
                        bypass,
                        manual,
                        semiauto,
                        quadmode
                    )
                )
            } else if (50 - binding.joystickViewRight.normalizedY < 0 && strength >= 10) {
                direction = -1
                sendCommand(
                    String.format(
                        "%02d:%05d:%03d:%01d:%01d:%01d:%01d:%01d\n",
                        direction,
                        angle,
                        strength,
                        pinup,
                        bypass,
                        manual,
                        semiauto,
                        quadmode
                    )
                )
            } else if (strength < 10) {
                sendCommand(
                    String.format(
                        "%02d:%05d:%03d:%01d:%01d:%01d:%01d:%01d\n",
                        direction,
                        angle,
                        0,
                        pinup,
                        bypass,
                        manual,
                        semiauto,
                        quadmode
                    )
                )
            }

        }
    }


    override fun onPause() {
    super.onPause()
    mainHandler.removeCallbacks(updateTextTask)
}

override fun onResume() {
    super.onResume()
    mainHandler.post(updateTextTask)
}

fun sendNullMessage() {
//    sendCommand(
//        String.format(
//            "%02d:%05d:%03d:%01d:%01d:%01d:%01d:%01d\n",
//            0,
//            0,
//            0,
//            pinup,
//            bypass,
//            manual,
//            semiauto,
//            quadmode
//        )
//    )

    if (binding.joystickViewRight.isEnabled){
//        Toast.makeText(applicationContext, "focused", Toast.LENGTH_SHORT ).show()
        Log.e("focus: ", "focused")
    }
    else{
//        Toast.makeText(applicationContext, "noooooot focused", Toast.LENGTH_SHORT ).show()
        Log.e("focus: ", "notttt focused")

    }


}

    private fun sendCommand(input: String) {
        if (m_bluetoothSocket != null) {
            try {
                m_bluetoothSocket!!.outputStream.write(input.toByteArray())
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

    }

    private fun disconnect() {
        if (m_bluetoothSocket != null) {
            try {
                m_bluetoothSocket!!.close()
                m_bluetoothSocket = null
                m_isConnected = false
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        finish()
    }

    private class ConnectToDevice(c: Context) : AsyncTask<Void, Void, String>() {
        private var connectSuccess: Boolean = true
        private val context: Context

        init {
            this.context = c
        }

        override fun onPreExecute() {
            super.onPreExecute()
            m_progress = ProgressDialog.show(context, "Connecting...", "Please wait")
        }

        override fun doInBackground(vararg params: Void?): String? {
            try {
                if (m_bluetoothSocket == null || !m_isConnected) {
                    m_bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
                    val device: BluetoothDevice = m_bluetoothAdapter.getRemoteDevice(m_address)
                    m_bluetoothSocket = device.createInsecureRfcommSocketToServiceRecord(m_myUUID)
                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery()
                    m_bluetoothSocket!!.connect()
                }
            } catch (e: IOException) {
                connectSuccess = false
                e.printStackTrace()
            }

            return null
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            if (!connectSuccess) {
                Log.w("bullshit", "Couldn t connect")
                Toast.makeText(context, "Could not Connect!", Toast.LENGTH_SHORT).show()
            } else {
                m_isConnected = true
            }
            m_progress.dismiss()
        }
    }
}