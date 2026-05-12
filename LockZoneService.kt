package com.example.lockzone

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.hardware.camera2.CameraManager
import android.os.Build
import android.provider.MediaStore
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import kotlin.math.abs

class LockZoneService : AccessibilityService() {

    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: View
    private lateinit var gestureDetector: GestureDetector
    private var isFlashlightOn = false

    @SuppressLint("ClickableViewAccessibility")
    override fun onServiceConnected() {
        super.onServiceConnected()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        
        setupOverlayView()
        setupGestures()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupOverlayView() {
        overlayView = View(this).apply {
            // Đặt màu nền bán trong suốt để dễ debug lúc đầu (VD: Color.parseColor("#44FF0000")). 
            // Khi dùng thật, đổi thành Color.TRANSPARENT
            setBackgroundColor(Color.TRANSPARENT) 
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            120, // Độ cao của vùng nhận diện (pixel) ở đáy màn hình
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM // Ép xuống đáy
        }

        windowManager.addView(overlayView, params)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupGestures() {
        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                // Ví dụ: 1 chạm -> Mở Secure Camera (hoạt động trên Lock Screen)
                openSecureCamera()
                return true
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {
                // Ví dụ: Nhấp đúp -> Tắt màn hình
                turnOffScreen()
                return true
            }

            override fun onLongPress(e: MotionEvent) {
                // Ví dụ: Giữ lâu -> Bật/tắt đèn pin
                toggleFlashlight()
            }

            override fun onFling(
                e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float
            ): Boolean {
                if (e1 == null) return false
                val diffY = e2.y - e1.y
                val diffX = e2.x - e1.x

                if (abs(diffX) > abs(diffY)) {
                    if (abs(diffX) > 100 && abs(velocityX) > 100) {
                        if (diffX > 0) {
                            // Vuốt phải
                        } else {
                            // Vuốt trái
                        }
                        return true
                    }
                } else {
                    if (abs(diffY) > 100 && abs(velocityY) > 100) {
                        if (diffY < 0) {
                            // Vuốt lên -> Có thể gọi Global Action mở Quick Settings
                            performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)
                        } else {
                            // Vuốt xuống
                        }
                        return true
                    }
                }
                return false
            }
        })

        // Gắn GestureDetector vào vùng Overlay
        overlayView.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            true // Consume touch event
        }
    }

    // --- CÁC ACTION ---

    private fun turnOffScreen() {
        // Tắt màn hình siêu sạch bằng API gốc của Android 9+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
        }
    }

    private fun toggleFlashlight() {
        try {
            val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = cameraManager.cameraIdList[0] // Thường cam 0 là cam sau
            isFlashlightOn = !isFlashlightOn
            cameraManager.setTorchMode(cameraId, isFlashlightOn)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun openSecureCamera() {
        // Intent an toàn để mở camera đè lên lock screen mà không cần mở khóa mật khẩu
        val intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA_SECURE).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
        }
        startActivity(intent)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Không xử lý event UI, tiết kiệm CPU tuyệt đối
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        if (::overlayView.isInitialized) {
            windowManager.removeView(overlayView)
        }
    }
}
