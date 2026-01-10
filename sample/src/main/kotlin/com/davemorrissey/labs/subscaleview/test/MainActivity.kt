package com.davemorrissey.labs.subscaleview.test

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import com.davemorrissey.labs.subscaleview.test.R.string
import com.davemorrissey.labs.subscaleview.test.animation.AnimationActivity
import com.davemorrissey.labs.subscaleview.test.basicfeatures.BasicFeaturesActivity
import com.davemorrissey.labs.subscaleview.test.configuration.ConfigurationActivity
import com.davemorrissey.labs.subscaleview.test.cropborders.CropBordersActivity
import com.davemorrissey.labs.subscaleview.test.databinding.MainActivityBinding
import com.davemorrissey.labs.subscaleview.test.eventhandling.EventHandlingActivity
import com.davemorrissey.labs.subscaleview.test.eventhandlingadvanced.AdvancedEventHandlingActivity
import com.davemorrissey.labs.subscaleview.test.extension.ExtensionActivity
import com.davemorrissey.labs.subscaleview.test.imagedisplay.ImageDisplayActivity
import com.davemorrissey.labs.subscaleview.test.viewpager.ViewPagerActivity

class MainActivity : AppCompatActivity() {

    private lateinit var binding: MainActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = MainActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        actionBar?.setTitle(string.main_title)

        binding.basicFeatures.setOnClickListener {
            startActivity(BasicFeaturesActivity::class.java)
        }
        binding.cropBorders.setOnClickListener {
            startActivity(CropBordersActivity::class.java)
        }
        binding.imageDisplay.setOnClickListener {
            startActivity(ImageDisplayActivity::class.java)
        }
        binding.eventHandling.setOnClickListener {
            startActivity(EventHandlingActivity::class.java)
        }
        binding.advancedEventHandling.setOnClickListener {
            startActivity(AdvancedEventHandlingActivity::class.java)
        }
        binding.viewPagerGalleries.setOnClickListener {
            startActivity(ViewPagerActivity::class.java)
        }
        binding.animation.setOnClickListener {
            startActivity(AnimationActivity::class.java)
        }
        binding.extension.setOnClickListener {
            startActivity(ExtensionActivity::class.java)
        }
        binding.configuration.setOnClickListener {
            startActivity(ConfigurationActivity::class.java)
        }
        binding.github.setOnClickListener {
            openGitHub()
        }
    }

    private fun startActivity(activity: Class<out Activity?>) {
        startActivity(Intent(this, activity))
    }

    private fun openGitHub() {
        startActivity(
            Intent(Intent.ACTION_VIEW).apply {
                data = "https://github.com/tachiyomiorg/subsampling-scale-image-view".toUri()
            }
        )
    }
}