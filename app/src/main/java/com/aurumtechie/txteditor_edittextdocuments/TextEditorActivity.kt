@file:Suppress("BlockingMethodInNonBlockingContext")

package com.aurumtechie.txteditor_edittextdocuments

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_read_file.progressCircular
import kotlinx.android.synthetic.main.activity_text_editor.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileWriter
import java.io.IOException

class TextEditorActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_text_editor)

        setSupportActionBar(toolbar)

        CoroutineScope(Dispatchers.Default).launch { // Perform data retrieval tasks in the background
            intent.data?.let {
                val fileContent = StringBuilder("")
                contentResolver.openInputStream(it)?.bufferedReader()
                    ?.forEachLine { line -> fileContent.append("$line\n") } // Add a line break for each new line
                // Make UI updates in the UI/Main thread
                withContext(Dispatchers.Main) {
                    fileContentEditText.setText(fileContent)
                    progressCircular.visibility = View.GONE
                }
            }
                ?: withContext(Dispatchers.Main) {
                    progressCircular.visibility = View.GONE
                    Toast.makeText(
                        this@TextEditorActivity, getString(R.string.file_not_found),
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.editor_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.save -> {
            saveFileContent()
            true
        }
        R.id.cancel_button -> {
            onBackPressed()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    @SuppressLint("SetWorldWritable")
    private fun saveFileContent() {
        val dialog = Dialog(this).apply {
            title = getString(R.string.saving_file)
        }
        dialog.show()
        val fileContent = fileContentEditText.text.toString()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                FileWriter(intent.data!!.path.toString()).apply {
                    write(fileContent)
                    close()
                }
                withContext(Dispatchers.Main) {
                    dialog.dismiss()
                    Toast.makeText(
                        this@TextEditorActivity,
                        getString(R.string.file_saved_successfully),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: IOException) {
                withContext(Dispatchers.Main) {
                    dialog.dismiss()
                    Toast.makeText(
                        this@TextEditorActivity,
                        getString(R.string.error_while_saving),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                e.printStackTrace()
            }
        }
    }

    override fun onBackPressed() {
        AlertDialog.Builder(this).setTitle(R.string.do_you_want_to_save_the_file)
            .setPositiveButton(android.R.string.yes) { dialog, _ ->
                dialog.dismiss()
                saveFileContent()
                super.onBackPressed()
            }.setNegativeButton(android.R.string.no) { dialog, _ ->
                dialog.dismiss()
                super.onBackPressed()
            }.show()
    }
}
