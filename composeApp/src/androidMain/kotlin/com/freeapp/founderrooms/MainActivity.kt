package com.freeapp.founderrooms

import com.freeapp.founderrooms.db.DatabaseDriverFactory
import com.freeapp.founderrooms.di.appModule
import com.freeapp.founderrooms.di.initKoin
import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

class TemplateApp : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidContext(this@TemplateApp)
            modules(
                appModule,
                module {
                    single { DatabaseDriverFactory(get()) }
                }
            )
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            App()
        }
    }
}
