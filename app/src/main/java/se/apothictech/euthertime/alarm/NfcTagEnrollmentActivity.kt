package se.apothictech.euthertime.alarm

import android.app.Activity
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class NfcTagEnrollmentActivity : ComponentActivity(), NfcAdapter.ReaderCallback {
    private var status by mutableStateOf("HOLD ONE NFC TAG AGAINST THE PHONE")
    private var accepted by mutableStateOf(false)
    private val adapter by lazy { NfcAdapter.getDefaultAdapter(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Box(
                modifier = Modifier.fillMaxSize().background(Color(0xFF030806)).padding(28.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        "NFC KEY ENROLLMENT",
                        color = Color(0xFFFFB000),
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 3.sp,
                    )
                    Spacer(Modifier.height(28.dp))
                    Text(
                        if (accepted) "◆" else "((( ◆ )))",
                        color = if (accepted) Color(0xFF74FF63) else Color(0xFFFF3AA7),
                        fontSize = 64.sp,
                    )
                    Spacer(Modifier.height(24.dp))
                    Text(
                        status,
                        color = if (accepted) Color(0xFF74FF63) else Color.White,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Only a salted fingerprint is stored locally. The tag is not written or modified.",
                        color = Color(0xFFB9FFE8).copy(alpha = 0.55f),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        when {
            adapter == null -> status = "THIS PHONE HAS NO NFC READER"
            adapter?.isEnabled != true -> status = "ENABLE NFC, THEN RETURN HERE"
            else -> adapter?.enableReaderMode(this, this, READER_FLAGS, null)
        }
    }

    override fun onPause() {
        adapter?.disableReaderMode(this)
        super.onPause()
    }

    override fun onTagDiscovered(tag: Tag) {
        runCatching { NfcTagStore.enroll(this, tag.id) }
            .onSuccess { fingerprint ->
                runOnUiThread {
                    accepted = true
                    status = "TAG ACCEPTED // $fingerprint"
                    setResult(Activity.RESULT_OK)
                    window.decorView.postDelayed({ finish() }, 900L)
                }
            }
            .onFailure { error ->
                runOnUiThread { status = error.message?.uppercase() ?: "TAG COULD NOT BE ENROLLED" }
            }
    }

    companion object {
        const val READER_FLAGS = NfcAdapter.FLAG_READER_NFC_A or
            NfcAdapter.FLAG_READER_NFC_B or
            NfcAdapter.FLAG_READER_NFC_F or
            NfcAdapter.FLAG_READER_NFC_V or
            NfcAdapter.FLAG_READER_NFC_BARCODE or
            NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK
    }
}
