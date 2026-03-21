package `is`.xyz.mpv

import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import `is`.xyz.mpv.databinding.ActivityCodecInfoBinding

class CodecInfoActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCodecInfoBinding

    private fun collect(videoOnly: Boolean, decodeOnly: Boolean): String {
        // REGULAR_CODECS <=> "These are the codecs that are returned prior to API 21, using the now deprecated static methods."
        val codecs = MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos

        // Group by aliases
        val grouped = mutableMapOf<String, MediaCodecInfo>()
        val otherNames = mutableMapOf<String, MutableList<String>>()
        for (codec in codecs) {
            if (videoOnly && !codec.supportedTypes.any { it.startsWith("video/", true) })
                continue
            if (decodeOnly && codec.isEncoder)
                continue
            var key = ""
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                key = codec.canonicalName
                if (codec.name == key)
                    grouped[key] = codec
            } else {
                // least-effort approximation
                key = codec.name
                if (key.startsWith("OMX.google."))
                    key = key.replaceFirst("OMX.google.", "c2.android.")
                else if (key.startsWith("OMX."))
                    key = key.replaceFirst("OMX.", "c2.")
                grouped[key] = codec
            }
            if (otherNames.containsKey(key))
                otherNames[key]!!.add(codec.name)
            else
                otherNames[key] = mutableListOf(codec.name)
        }

        val out = mutableListOf<String>()
        for ((primaryKey, codec) in grouped) {
            var line = if (codec.isEncoder) "E: " else "D: "
            line += codec.name
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (codec.isHardwareAccelerated)
                    line += " [HW]"
                if (codec.isSoftwareOnly)
                    line += " [SW]"
                if (codec.isVendor)
                    line += " [V]"
            }
            out.add(line)

            otherNames[primaryKey]?.forEach {
                if (it != codec.name)
                    out.add("   $it")
            }

            // Merge mime types with the same profile set
            val groupedProfiles = mutableMapOf<String, MutableList<String>>()
            for (type in codec.supportedTypes) {
                val levels = codec.getCapabilitiesForType(type).profileLevels
                // note: ffmpeg only checks/uses profiles, not levels
                val s = if (levels.isEmpty())
                    ""
                else
                    levels.map { it.profile }.sorted().distinct().joinToString()
                if (groupedProfiles.containsKey(s))
                    groupedProfiles[s]!!.add(type)
                else
                    groupedProfiles[s] = mutableListOf(type)
            }

            for ((levels, mimeTypes) in groupedProfiles) {
                out.add("- ${mimeTypes.joinToString()}")
                if (levels.isNotEmpty())
                    out.add("-> $levels")
            }
        }
        return out.joinToString("\n") + "\n"
    }

    private fun refresh() {
        binding.info.text = collect(binding.switch1.isChecked, binding.switch2.isChecked)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCodecInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Utils.handleInsetsAsPadding(binding.root)

        binding.switch1.setOnCheckedChangeListener { _, _ -> refresh() }
        binding.switch2.setOnCheckedChangeListener { _, _ -> refresh() }
        refresh()
    }
}
