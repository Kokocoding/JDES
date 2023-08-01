package com.example.jdes

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import kotlinx.coroutines.Dispatchers
import java.net.Socket
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var sck: Socket
    private var timingJob: Job? = null // 用于存储定时任务的 Job 对象



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val textViewIds = arrayOf(R.id.AirC1, R.id.AirC2, R.id.AirC3, R.id.AirC4, R.id.AirC5, R.id.AirC6, R.id.AirC7, R.id.AirC8)

        for ((index, textViewId) in textViewIds.withIndex()) {
            val textView = findViewById<TextView>(textViewId)
            val formattedText = getString(R.string.air_conditioner, index + 1)
            textView.text = formattedText
        }

        val textViewIds2 = arrayOf(R.id.CoolW1, R.id.CoolW2)

        for ((index, textViewId) in textViewIds2.withIndex()) {
            val textView = findViewById<TextView>(textViewId)
            val formattedText = getString(R.string.water_cooler, index + 1)
            textView.text = formattedText
        }

        /*
        Thread {
            try {
                sck = Socket("192.168.3.200", 6001)
            } catch (e: Exception) {
                println(e)
            }
        }.start()
        */
    }
    fun openTiming(view: View){
        val textViewRemainingTime = findViewById<TextView>(R.id.textViewRemainingTime)
        val textViewMin = findViewById<TextView>(R.id.min)
        val editTextNumber = findViewById<EditText>(R.id.editTextNumber)
        val closeButton = findViewById<Button>(R.id.timingClose)
        val timeInMillis = editTextNumber.text.toString().toLong() * 60000

        timingJob?.cancel()
        // 定時開始
        timingJob = GlobalScope.launch (Dispatchers.Main) {
            textViewRemainingTime.visibility = View.VISIBLE
            editTextNumber.visibility = View.GONE
            textViewMin.visibility = View.GONE
            view.visibility = View.GONE
            closeButton.visibility = View.VISIBLE

            var remainingTime = timeInMillis / 1000 // 将毫秒转换为秒
            while (remainingTime > 0) {
                var hour = remainingTime/60/60
                var min = remainingTime/60%60
                var sec = remainingTime%60
                textViewRemainingTime.text = "剩餘時間：$hour 時 $min 分 $sec 秒"
                delay(1000) // 每隔1秒更新一次UI
                remainingTime--
            }

            textViewRemainingTime.visibility = View.GONE
            editTextNumber.visibility = View.VISIBLE
            textViewMin.visibility = View.VISIBLE
            view.visibility = View.VISIBLE
            closeButton.visibility = View.GONE
        }
    }

    fun closeTiming(view: View){
        val textViewRemainingTime = findViewById<TextView>(R.id.textViewRemainingTime)
        val textViewMin = findViewById<TextView>(R.id.min)
        val editTextNumber = findViewById<EditText>(R.id.editTextNumber)
        val openButton = findViewById<Button>(R.id.timing)

        // 取消定時
        textViewRemainingTime.visibility = View.GONE
        editTextNumber.visibility = View.VISIBLE
        textViewMin.visibility = View.VISIBLE
        openButton.visibility = View.VISIBLE
        view.visibility = View.GONE

        timingJob?.cancel()
    }

    /*
        fun onButtonClick(view: View) {
            val tag = view.tag
            if (tag is String) {
                val buttonValue = tag.toInt() // 将标签转换为您需要的数据类型
                val cmd = byteArrayOf(0xFA.toByte(),0x01,buttonValue.toByte(),0x01,0x03,0xFD.toByte(),0x00,0x00,0x00)
                send(cmd)
            }
        }

        private fun send(data:ByteArray) {
            if(sck.isConnected){
                Thread{
                    try {
                        val outputStream = sck.getOutputStream()
                        val dataOutputStream = DataOutputStream(outputStream)

                        dataOutputStream.write(data)
                        dataOutputStream.flush()
                        outputStream.flush()
                    }
                    catch (e: java.lang.Exception){
                        println(e)
                    }
                }.start()
            }
       }
   */
}