package com.sumit.varnamala

import android.app.Application
import android.util.Log
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.CameraXConfig
import com.sumit.varnamala.transliteration.SignboardDictionary
import com.sumit.varnamala.utils.Transliterator

class VarnamalaApp : Application(), CameraXConfig.Provider {
    override fun onCreate() {
        super.onCreate()
        runCatching {
            assets.open("signboard_dictionary.json").bufferedReader(Charsets.UTF_8).use { it.readText() }
        }.mapCatching(SignboardDictionary::fromJson)
            .onSuccess(Transliterator::install)
            .onFailure { Log.w("VarnamalaApp", "Signboard dictionary unavailable; using rules only", it) }
    }

    override fun getCameraXConfig(): CameraXConfig {
        return Camera2Config.defaultConfig()
    }
}
