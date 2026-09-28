package io.github.supermonster003.autojs6.plugin.three.stove.agent.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContract
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction

/** A new JSON document chosen by the user through the system picker; only content URIs are accepted. */
internal class CreateJsonDocument : ActivityResultContract<String, Uri?>() {
    override fun createIntent(context: Context, input: String) = Intent(Intent.ACTION_CREATE_DOCUMENT).setType("application/json")
        .addCategory(Intent.CATEGORY_OPENABLE).putExtra(Intent.EXTRA_TITLE, input)
    override fun parseResult(resultCode: Int, intent: Intent?): Uri? = intent?.data?.takeIf { resultCode == Activity.RESULT_OK && it.scheme == "content" }
}

/** Reads a picked document as strict UTF-8 and never past [maxBytes]; the caller decodes it under its own format limits. */
internal fun readJsonDocument(input: InputStream, maxBytes: Int): String {
    val output = ByteArrayOutputStream(); val buffer = ByteArray(4096)
    while (true) { val count = input.read(buffer); if (count < 0) break; require(output.size() + count <= maxBytes); output.write(buffer, 0, count) }
    return Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT)
        .decode(ByteBuffer.wrap(output.toByteArray())).toString()
}
