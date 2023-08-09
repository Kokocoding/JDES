package com.example.jdes

import android.annotation.SuppressLint
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import kotlinx.coroutines.*
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private var timingJob: Job? = null // 用于存储定时任务的 Job 对象
    private lateinit var port: UsbSerialPort
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val textViewIds2 = arrayOf(R.id.CoolW1, R.id.CoolW2)

        for ((index, textViewId) in textViewIds2.withIndex()) {
            val textView = findViewById<TextView>(textViewId)
            val formattedText = getString(R.string.water_cooler, index + 1)
            textView.text = formattedText
        }

        openPort()

        coroutineScope.launch {
            while (isActive) { // 在协程活动期间持续监听
                val data = readData() // 调用您的 readData() 函数来读取数据
                processData(data)
                delay(1000) // 每秒监听一次，根据实际需求调整延迟时间
            }
        }
    }

    private suspend fun processData(data: ByteArray) {
        // 在这里处理接收到的数据
        // 根据实际需求解析和处理 data 数组
        val Temper = mapOf("11" to R.id.Temperature1, "12" to R.id.Temperature2, "13" to R.id.Temperature3, "14" to R.id.Temperature4,
                           "15" to R.id.Temperature5, "16" to R.id.Temperature6, "17" to R.id.Temperature7, "18" to R.id.Temperature8)
        val TemperInWater = mapOf("19" to R.id.TemperatureWI1, "1a" to R.id.TemperatureWI2)
        val TemperOutWater = mapOf("19" to R.id.TemperatureWO1, "1a" to R.id.TemperatureWO2)
        val tv = findViewById<TextView>(R.id.textView4)

        if (data.isNotEmpty() && data[0].toByte() == 0x7A.toByte()) {
            val dataType = data[1].toUByte().toString(16) // 将第二个字节转换为十六进制字符串

            when (dataType) {
                in Temper.keys -> {
                    val textViewId = Temper[dataType]
                    val textView = findViewById<TextView>(textViewId!!)
                    val temperature = data[2].toInt() // 假设温度数据在第三个字节
                    val temperatureStr = "$temperature°C"

                    withContext(Dispatchers.Main) {
                        textView.text = temperatureStr
                    }
                }
                in TemperInWater.keys ->{
                    val textViewIdIn = TemperInWater[dataType]
                    val textViewIn = findViewById<TextView>(textViewIdIn!!)
                    val textViewIdOut = TemperOutWater[dataType]
                    val textViewOut = findViewById<TextView>(textViewIdOut!!)

                    val temperatureIn = data[2].toInt() // 假设温度数据在第三个字节
                    val temperatureOut = data[3].toInt() // 假设温度数据在第三个字节

                    val temperatureStrI = "$temperatureIn°C"
                    val temperatureStrO = "$temperatureOut°C"

                    withContext(Dispatchers.Main) {
                        textViewIn.text = temperatureStrI
                        textViewOut.text = temperatureStrO
                    }
                }
            }
        }

    }

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    fun switchChange(view: View){
        val switchIds = arrayOf(R.id.switch1, R.id.switch2, R.id.switch3, R.id.switch4, R.id.switch5, R.id.switch6, R.id.switch7, R.id.switch8)
        val imagesIds = arrayOf(R.id.imageViewC1, R.id.imageViewC2, R.id.imageViewC3, R.id.imageViewC4, R.id.imageViewC5, R.id.imageViewC6, R.id.imageViewC7, R.id.imageViewC8)

        val tag = view.tag
        var cmd = byteArrayOf()

        fun setImageResourceAndCmd(imageViewId: Int, isSwitchChecked: Boolean, index: Int) {
            val switch = findViewById<Switch>(switchIds[index])
            val imageView = findViewById<ImageView>(imageViewId)

            switch.isChecked = isSwitchChecked
            imageView.setImageResource(if(isSwitchChecked) R.drawable.green else R.drawable.red)

            val indexByte = (index + 1).toByte()
            val dataByte = if (isSwitchChecked) 0x0A.toByte() else 0x0B.toByte()
            val fourthByte = (indexByte + dataByte).toByte()
            cmd = byteArrayOf(0x7A, indexByte, dataByte, fourthByte, 0xBB.toByte())
            writeData(cmd)
        }

        if (tag is String) {
            val tagInt = tag.toInt()
            val isSwitchChecked = (view as? Switch)?.isChecked ?: false

            val switchAllCall = findViewById<Switch>(R.id.switchAllCall)
            val switchG1 = findViewById<Switch>(R.id.switchGroup1)
            val switchG2 = findViewById<Switch>(R.id.switchGroup2)
            val switchG3 = findViewById<Switch>(R.id.switchGroup3)

            when(tagInt) {
                124, 125, 126, 127 -> {
                    for ((index, imageViewId) in imagesIds.withIndex()) {
                        if(tagInt == 124 && index < 4) setImageResourceAndCmd(imageViewId, isSwitchChecked, index)
                        if(tagInt == 125 && (index == 4 || index == 5)) setImageResourceAndCmd(imageViewId, isSwitchChecked, index)
                        if(tagInt == 126 && (index == 6 || index == 7)) setImageResourceAndCmd(imageViewId, isSwitchChecked, index)
                        if(tagInt == 127){
                            setImageResourceAndCmd(imageViewId, isSwitchChecked, index)
                            switchG1.isChecked = switchAllCall.isChecked
                            switchG2.isChecked = switchAllCall.isChecked
                            switchG3.isChecked = switchAllCall.isChecked
                        }else{
                            switchAllCall.isChecked = switchG1.isChecked && switchG2.isChecked && switchG3.isChecked
                        }
                    }
                }
                else -> {
                    setImageResourceAndCmd(imagesIds[tagInt-1], isSwitchChecked, tagInt-1)

                    switchG1.isChecked = findViewById<Switch>(switchIds[0]).isChecked && findViewById<Switch>(switchIds[1]).isChecked &&
                            findViewById<Switch>(switchIds[2]).isChecked && findViewById<Switch>(switchIds[3]).isChecked
                    switchG2.isChecked = findViewById<Switch>(switchIds[4]).isChecked && findViewById<Switch>(switchIds[5]).isChecked
                    switchG3.isChecked = findViewById<Switch>(switchIds[6]).isChecked && findViewById<Switch>(switchIds[7]).isChecked
                    switchAllCall.isChecked = switchG1.isChecked && switchG2.isChecked && switchG3.isChecked
                }
            }
        }
    }

    private fun openPort(){
        // open device:
        val manager = getSystemService(USB_SERVICE) as UsbManager
        val availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager)
        if (availableDrivers.isEmpty()) {
            return
        }

        val driver = availableDrivers[0]
        val connection = manager.openDevice(driver.device) ?: return

        port = driver.ports[0] // Most devices have just one port (port 0)

        port.open(connection)
        port.setParameters(9600, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE)
    }

    fun writeData(cmd: ByteArray) {
        //write
        port.write(cmd, 0)
    }

    fun readData(): ByteArray {
        //read
        val bufferSize = 1024 // 适当的缓冲区大小，根据实际需求调整
        val buffer = ByteArray(bufferSize)
        var bytesRead = 0

        try {
            bytesRead = port?.read(buffer, buffer.size.toLong().toInt()) ?: 0
        } catch (e: IOException) {
            e.printStackTrace()
        }

        // 根据实际读取的字节数截取数据
        val response = buffer.copyOf(bytesRead)
        return response
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

}