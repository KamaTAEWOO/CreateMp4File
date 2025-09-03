package com.meronacompany.createmp4file

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Environment
import java.io.File
import java.nio.ByteBuffer

class Mp4Writer {
    
    data class EncoderData(
        val videoData: ByteArray,
        val width: Int,
        val height: Int,
        val frameRate: Int = 30,
        val bitRate: Int = 2000000
    )
    
    private var mediaMuxer: MediaMuxer? = null
    private var mediaCodec: MediaCodec? = null
    private var videoTrackIndex: Int = -1
    private var isStarted = false
    
    fun createMp4File(encoderData: EncoderData, fileName: String = "output.mp4"): File? {
        try {
            val outputFile = getDesktopFile(fileName)
            
            setupMediaMuxer(outputFile)
            setupMediaCodec(encoderData)
            
            writeVideoData(encoderData.videoData)
            
            return outputFile
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        } finally {
            cleanup()
        }
    }
    
    private fun getDesktopFile(fileName: String): File {
        val desktopPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        return File(desktopPath, fileName)
    }
    
    private fun setupMediaMuxer(outputFile: File) {
        mediaMuxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
    }
    
    private fun setupMediaCodec(encoderData: EncoderData) {
        val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, encoderData.width, encoderData.height)
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar)
        format.setInteger(MediaFormat.KEY_BIT_RATE, encoderData.bitRate)
        format.setInteger(MediaFormat.KEY_FRAME_RATE, encoderData.frameRate)
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        
        mediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        mediaCodec?.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        mediaCodec?.start()
    }
    
    private fun writeVideoData(videoData: ByteArray) {
        val codec = mediaCodec ?: return
        val muxer = mediaMuxer ?: return
        
        val inputBuffers = codec.inputBuffers
        val outputBuffers = codec.outputBuffers
        val bufferInfo = MediaCodec.BufferInfo()
        
        var inputBufferIndex = codec.dequeueInputBuffer(10000)
        if (inputBufferIndex >= 0) {
            val inputBuffer = inputBuffers[inputBufferIndex]
            inputBuffer.clear()
            inputBuffer.put(videoData)
            codec.queueInputBuffer(inputBufferIndex, 0, videoData.size, 0, 0)
        }
        
        codec.queueInputBuffer(inputBufferIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
        
        var outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, 10000)
        while (outputBufferIndex >= 0) {
            when (outputBufferIndex) {
                MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    val newFormat = codec.outputFormat
                    videoTrackIndex = muxer.addTrack(newFormat)
                    muxer.start()
                    isStarted = true
                }
                MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED -> {
                    // Deprecated, but handle if needed
                }
                else -> {
                    if (outputBufferIndex >= 0 && isStarted) {
                        val outputBuffer = outputBuffers[outputBufferIndex]
                        if (bufferInfo.size != 0) {
                            outputBuffer.position(bufferInfo.offset)
                            outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                            muxer.writeSampleData(videoTrackIndex, outputBuffer, bufferInfo)
                        }
                        codec.releaseOutputBuffer(outputBufferIndex, false)
                    }
                }
            }
            
            if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                break
            }
            
            outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, 10000)
        }
    }
    
    private fun cleanup() {
        try {
            mediaCodec?.stop()
            mediaCodec?.release()
            mediaCodec = null
            
            if (isStarted) {
                mediaMuxer?.stop()
            }
            mediaMuxer?.release()
            mediaMuxer = null
            
            isStarted = false
            videoTrackIndex = -1
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}