package com.example.myapplication

import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.AppCompatTextView
import com.vng.materialdialogs.MaterialDialog
import com.vng.materialdialogs.customview.customView
import com.vng.materialdialogs.customview.progressView
import com.vng.materialdialogs.input.input


class MainActivity : AppCompatActivity() {
    var dialog : MaterialDialog? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)

        findViewById<TextView>(R.id.tv_test).setOnClickListener {
             dialog = MaterialDialog(this).customView(viewRes = R.layout.layout_custom)
                .positiveButton(text = "ABCD"){
                    materialDialog ->
                    materialDialog.findViewById<AppCompatTextView>(R.id.tv_custom).text = "gefgh"
                }
            
            dialog?.show()
        }

        findViewById<TextView>(R.id.tv_second).setOnClickListener {


            dialog?.show()
        }
    }
}