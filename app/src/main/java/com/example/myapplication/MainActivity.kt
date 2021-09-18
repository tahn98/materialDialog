package com.example.myapplication

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.consts.Theme
import com.afollestad.materialdialogs.customview.progressView


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)

        findViewById<TextView>(R.id.tv_test).setOnClickListener {
            MaterialDialog(this, theme = Theme.LIGHT).show {
                progressView(R.layout.md_dialog_progress, title ="avbcd")
            }
        }
    }
}