package com.davemorrissey.labs.subscaleview.test.cropborders

import android.os.Bundle
import android.view.View
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.davemorrissey.labs.subscaleview.test.AbstractPagesActivity
import com.davemorrissey.labs.subscaleview.test.Page
import com.davemorrissey.labs.subscaleview.test.R.id
import com.davemorrissey.labs.subscaleview.test.R.layout
import com.davemorrissey.labs.subscaleview.test.R.string
import com.davemorrissey.labs.subscaleview.test.extension.views.PinView
import java.util.Random

class CropBordersActivity : AbstractPagesActivity(
    string.animation_title, layout.animation_activity, listOf(
        Page(string.animation_p1_subtitle, string.animation_p1_text)
    )
) {
    private var view: PinView? = null

    private var cropBorders = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        view = findViewById(id.imageView)
        findViewById<View>(id.play).setOnClickListener {
            cropBorders = !cropBorders
            view?.setCropBorders(cropBorders)
            view?.setImage(ImageSource.asset(this, "cropborders.jpg"))
        }

        view?.setCropBorders(cropBorders)
        view?.setImage(ImageSource.asset(this, "cropborders.jpg"))
    }

}