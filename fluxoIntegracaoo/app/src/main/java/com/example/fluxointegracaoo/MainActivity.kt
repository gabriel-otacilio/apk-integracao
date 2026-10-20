package com.example.fluxointegracaoo
import android.os.Build
import android.os.Bundle
import android.os.Environment
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import printx990.br.apix990.internal.TransactionInfo


class MainActivity : ComponentActivity() {
    override fun onResume() {
        super.onResume()
        Pos7.info.onHostActivityResume(this)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Pos7.info.onRequestPermissionsResult(this, requestCode, grantResults)
    }
    override fun onCreate(savedInstanceState: Bundle?) {


        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            TransactionInfo.openManageAllFilesSettings(this)
        }
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                CaixaApp()
            }
        }
    }
}
