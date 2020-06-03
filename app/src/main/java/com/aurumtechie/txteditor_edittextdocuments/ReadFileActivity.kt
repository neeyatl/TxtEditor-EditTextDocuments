@file:Suppress("BlockingMethodInNonBlockingContext")

package com.aurumtechie.txteditor_edittextdocuments

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.Menu
import android.view.MenuItem
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
    }

    private fun loadFileContent() {
        if (intent?.type == "text/plain") {
            CoroutineScope(Dispatchers.Default).launch { // Perform data retrieval tasks in the background
                intent.data?.let {
                    val fileContent = StringBuilder("")
                    contentResolver.openInputStream(it)?.bufferedReader()
                        ?.forEachLine { line -> fileContent.append("$line\n") } // Add a line break for each new line
                    // Make UI updates in the UI/Main thread
                    withContext(Dispatchers.Main) {
                        if (fileContent.isBlank())
                            Toast.makeText(
                                this@ReadFileActivity,
                                R.string.file_is_empty,
                                Toast.LENGTH_SHORT
                            ).show()
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

    override fun onStart() {
        super.onStart()
        loadFileContent()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.read_file_activity_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
        if (item.itemId == R.id.edit) {
            startActivity(Intent(this, TextEditorActivity::class.java).also {
                it.data = this.intent.data
            })
            true
        } else super.onOptionsItemSelected(item)
}
