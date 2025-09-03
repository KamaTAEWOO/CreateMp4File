package com.meronacompany.createmp4file

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    
    private val mp4Writer = Mp4Writer()
    private val STORAGE_PERMISSION_REQUEST = 1001
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        
        checkPermissionsAndCreateSampleMp4()
    }
    
    private fun checkPermissionsAndCreateSampleMp4() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) 
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE),
                STORAGE_PERMISSION_REQUEST
            )
        } else {
            createSampleMp4()
        }
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION_REQUEST) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                createSampleMp4()
            } else {
                Toast.makeText(this, "파일 저장을 위해 권한이 필요합니다", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun createSampleMp4() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val sampleEncoderData = createSampleEncoderData()
                val outputFile = mp4Writer.createMp4File(sampleEncoderData, "sample_output.mp4")
                
                withContext(Dispatchers.Main) {
                    if (outputFile != null && outputFile.exists()) {
                        Toast.makeText(this@MainActivity, 
                            "MP4 파일이 생성되었습니다: ${outputFile.absolutePath}", 
                            Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this@MainActivity, 
                            "MP4 파일 생성에 실패했습니다", 
                            Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, 
                        "오류 발생: ${e.message}", 
                        Toast.LENGTH_LONG).show()
                }
                e.printStackTrace()
            }
        }
    }
    
    fun createMp4FromEncoderData(encoderData: Mp4Writer.EncoderData, fileName: String = "output.mp4") {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val outputFile = mp4Writer.createMp4File(encoderData, fileName)
                
                withContext(Dispatchers.Main) {
                    if (outputFile != null && outputFile.exists()) {
                        Toast.makeText(this@MainActivity, 
                            "MP4 파일이 바탕화면에 저장되었습니다: ${outputFile.name}", 
                            Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this@MainActivity, 
                            "MP4 파일 생성에 실패했습니다", 
                            Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, 
                        "오류 발생: ${e.message}", 
                        Toast.LENGTH_LONG).show()
                }
                e.printStackTrace()
            }
        }
    }
    
    private fun createSampleEncoderData(): Mp4Writer.EncoderData {
        val width = 1280
        val height = 720
        val frameSize = width * height * 3 / 2  // YUV420 format
        val sampleVideoData = ByteArray(frameSize) { 0 }
        
        return Mp4Writer.EncoderData(
            videoData = sampleVideoData,
            width = width,
            height = height,
            frameRate = 30,
            bitRate = 2000000
        )
    }
}