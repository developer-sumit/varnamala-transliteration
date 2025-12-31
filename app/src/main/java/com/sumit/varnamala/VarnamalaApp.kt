package com.sumit.varnamala

import android.app.Application
import com.sumit.varnamala.transliteration.IndianSignboardDictionary
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.CameraXConfig

class VarnamalaApp : Application(), CameraXConfig.Provider {
    override fun onCreate() {
        super.onCreate()
        IndianSignboardDictionary.init(this)
    }

    override fun getCameraXConfig(): CameraXConfig {
        return Camera2Config.defaultConfig()
    }
}