package `is`.xyz.mpv

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import `is`.xyz.mpv.databinding.ActivityIntentTestBinding

class IntentTestActivity : AppCompatActivity() {
    private lateinit var binding: ActivityIntentTestBinding

    private val callback = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        updateText("resultCode: ${ActivityResult.resultCodeToString(it.resultCode)}\n")
        val intent = it.data
        if (intent != null) {
            updateText("action: ${intent.action}\ndata: ${intent.data?.toString()}\n")
            val extras = intent.extras
            if (extras != null) {
                for (key in extras.keySet()) {
                    val v = extras.get(key)
                    updateText("extras[$key] = $v\n")
                }
            }
        }
    }

    private var text = ""

    private fun updateText(append: String) {
        text += append
        runOnUiThread {
            binding.info.text = this.text
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIntentTestBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.button.setOnClickListener {
            val uri = Uri.parse(binding.editText1.text.toString())
            if (uri.scheme.isNullOrEmpty())
                return@setOnClickListener

            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(uri, "video/any")
            intent.setPackage(packageName)
            /*val subtitle = Uri.parse("https://example.org/subtitle.srt")
            intent.putExtra("subs", arrayOf<Uri>(subtitle))*/
            if (binding.switch2.isChecked)
                intent.putExtra("decode_mode", 2.toByte())
            if (binding.switch3.isChecked)
                intent.putExtra("title", "example text")
            if (binding.seekBar2.progress > 0)
                intent.putExtra("position", binding.seekBar2.progress * 1000)
            callback.launch(intent)

            text = ""
            updateText("launched!\n")
        }
    }
}
