package com.aurumtechie.txteditor_edittextdocuments

import android.app.Activity
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_read_file.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class ReadFileActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_read_file)

        setSupportActionBar(toolbar)

        fileContentTextView.movementMethod = ScrollingMovementMethod()

        if (intent?.type == "text/plain") {
            CoroutineScope(Dispatchers.Default).launch { // Perform data retrieval tasks in the background
                intent.data?.let {
                    val fileContent = StringBuilder("")
                    contentResolver.openInputStream(it)?.bufferedReader()
                        ?.forEachLine { line -> fileContent.append("\n$line") } // Add a line break for each new line
                    // Make UI updates in the UI/Main thread
                    withContext(Dispatchers.Main) {
                        fileContentTextView.text = fileContent
                        progressCircular.visibility = View.GONE
                    }
                }
                    ?: withContext(Dispatchers.Main) {
                        progressCircular.visibility = View.GONE
                        Toast.makeText(
                            this@ReadFileActivity, getString(R.string.file_not_found),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }
        } else {
            Toast.makeText(this, getString(R.string.file_cannot_be_opened), Toast.LENGTH_SHORT)
                .show()
            setResult(Activity.RESULT_CANCELED)
            finish()
        }
    }
}
