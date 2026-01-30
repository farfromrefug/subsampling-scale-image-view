package com.davemorrissey.labs.subscaleview.test.examples

import android.animation.ObjectAnimator
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Bundle
import android.widget.Button
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.davemorrissey.labs.subscaleview.test.R

/**
 * Example activity demonstrating the new float rotation and ColorFilter features.
 * 
 * This example shows:
 * 1. Setting arbitrary rotation angles
 * 2. Animating rotation smoothly
 * 3. Interactive rotation control with SeekBar
 * 4. Applying various color filters (tint, grayscale, sepia)
 */
class RotationAndFilterExampleActivity : AppCompatActivity() {

    private lateinit var imageView: SubsamplingScaleImageView
    private lateinit var rotationSeekBar: SeekBar
    private lateinit var btnAnimateRotation: Button
    private lateinit var btnTintRed: Button
    private lateinit var btnGrayscale: Button
    private lateinit var btnSepia: Button
    private lateinit var btnClearFilter: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Note: You'll need to create a layout file for this activity
        // setContentView(R.layout.activity_rotation_filter_example)
        
        initViews()
        setupImageView()
        setupRotationControls()
        setupFilterControls()
    }

    private fun initViews() {
        imageView = findViewById(R.id.imageView)
        rotationSeekBar = findViewById(R.id.rotationSeekBar)
        btnAnimateRotation = findViewById(R.id.btnAnimateRotation)
        btnTintRed = findViewById(R.id.btnTintRed)
        btnGrayscale = findViewById(R.id.btnGrayscale)
        btnSepia = findViewById(R.id.btnSepia)
        btnClearFilter = findViewById(R.id.btnClearFilter)
    }

    private fun setupImageView() {
        // Load an image from assets (you can use any image source)
        imageView.setImage(ImageSource.asset(this, "sample_image.jpg"))
    }

    private fun setupRotationControls() {
        // Setup SeekBar for interactive rotation control
        rotationSeekBar.max = 360
        rotationSeekBar.progress = 0
        rotationSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    // Set rotation in real-time as user drags the SeekBar
                    imageView.setImageRotation(progress.toFloat())
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Setup button for animated rotation
        btnAnimateRotation.setOnClickListener {
            animateRotation()
        }
    }

    private fun setupFilterControls() {
        // Red tint button
        btnTintRed.setOnClickListener {
            applyRedTint()
        }

        // Grayscale button
        btnGrayscale.setOnClickListener {
            applyGrayscale()
        }

        // Sepia button
        btnSepia.setOnClickListener {
            applySepia()
        }

        // Clear filter button
        btnClearFilter.setOnClickListener {
            imageView.setColorFilter(null)
        }
    }

    private fun animateRotation() {
        // Animate from current rotation to 360 degrees (full rotation)
        val currentRotation = imageView.imageRotation
        val targetRotation = currentRotation + 360f

        val animator = ObjectAnimator.ofFloat(
            imageView,
            "imageRotation",
            currentRotation,
            targetRotation
        )
        animator.duration = 2000 // 2 seconds
        animator.start()

        // Update SeekBar to follow animation
        animator.addUpdateListener { animation ->
            val value = animation.animatedValue as Float
            rotationSeekBar.progress = (value % 360).toInt()
        }
    }

    private fun applyRedTint() {
        // Apply a semi-transparent red tint
        val redTint = PorterDuffColorFilter(
            Color.argb(100, 255, 0, 0),
            PorterDuff.Mode.SRC_ATOP
        )
        imageView.setColorFilter(redTint)
    }

    private fun applyGrayscale() {
        // Create a grayscale effect by removing saturation
        val matrix = ColorMatrix()
        matrix.setSaturation(0f) // 0 = grayscale, 1 = original colors
        imageView.setColorFilter(ColorMatrixColorFilter(matrix))
    }

    private fun applySepia() {
        // Create a proper sepia effect using standard sepia transformation matrix
        val sepiaMatrix = ColorMatrix(floatArrayOf(
            0.393f, 0.769f, 0.189f, 0f, 0f,
            0.349f, 0.686f, 0.168f, 0f, 0f,
            0.272f, 0.534f, 0.131f, 0f, 0f,
            0f,     0f,     0f,     1f, 0f
        ))
        
        imageView.setColorFilter(ColorMatrixColorFilter(sepiaMatrix))
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Save rotation state
        outState.putFloat("rotation", imageView.imageRotation)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        // Restore rotation state
        val rotation = savedInstanceState.getFloat("rotation", 0f)
        imageView.setImageRotation(rotation)
        rotationSeekBar.progress = rotation.toInt()
    }
}
