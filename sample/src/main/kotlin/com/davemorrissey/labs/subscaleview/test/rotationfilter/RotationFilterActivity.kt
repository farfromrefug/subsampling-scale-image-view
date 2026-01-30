package com.davemorrissey.labs.subscaleview.test.rotationfilter

import android.animation.ObjectAnimator
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Bundle
import android.widget.SeekBar
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.davemorrissey.labs.subscaleview.test.AbstractPagesActivity
import com.davemorrissey.labs.subscaleview.test.Page
import com.davemorrissey.labs.subscaleview.test.R.id
import com.davemorrissey.labs.subscaleview.test.R.layout
import com.davemorrissey.labs.subscaleview.test.R.string

class RotationFilterActivity : AbstractPagesActivity(
    string.rotationfilter_title, layout.pages_activity, listOf(
        Page(string.rotationfilter_p1_subtitle, string.rotationfilter_p1_text),
        Page(string.rotationfilter_p2_subtitle, string.rotationfilter_p2_text),
        Page(string.rotationfilter_p3_subtitle, string.rotationfilter_p3_text),
        Page(string.rotationfilter_p4_subtitle, string.rotationfilter_p4_text)
    )
) {

    private var view: SubsamplingScaleImageView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        view = findViewById(id.imageView)
        view?.setImage(ImageSource.asset(this, "sanmartino.jpg"))
    }

    override fun onPageChanged(page: Int) {
        view?.let { imageView ->
            // Reset rotation and filter when changing pages
            imageView.setImageRotation(0f)
            imageView.setColorFilter(null)
            
            when (page) {
                0 -> {
                    // Float rotation - set to 45 degrees
                    imageView.setImageRotation(45f)
                }
                1 -> {
                    // Animated rotation
                    val animator = ObjectAnimator.ofFloat(
                        imageView,
                        "imageRotation",
                        0f,
                        360f
                    )
                    animator.duration = 3000
                    animator.start()
                }
                2 -> {
                    // Red tint color filter
                    val redTint = PorterDuffColorFilter(
                        Color.argb(100, 255, 0, 0),
                        PorterDuff.Mode.SRC_ATOP
                    )
                    imageView.setColorFilter(redTint)
                }
                3 -> {
                    // Grayscale color filter
                    val matrix = ColorMatrix()
                    matrix.setSaturation(0f)
                    imageView.setColorFilter(ColorMatrixColorFilter(matrix))
                }
            }
        }
    }
}
