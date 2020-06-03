package com.aurumtechie.txteditor_edittextdocuments

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.ListFragment
import java.io.File
import java.util.*

class FileExplorerActivity : AppCompatActivity(),
    FilesListFragment.Companion.DirectoryExplorer {

    companion object {
        private const val REQUEST_CODE = 4579
        private const val CURRENT_FRAGMENT_KEY = "current_fragment_restore"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_file_explorer)

        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        ) { // If permissions are given, update the UI.
            if (savedInstanceState == null) initializeFileExplorer()
            /* else SavedInstanceState will be used by the fragment manager to restore the state*/
        } else {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE
                )
            ) requestExternalStoragePermission()
            else requestPermissionAndOpenSettings()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Save the state of the fragments so that it can be retrieved on activity recreation
        supportFragmentManager.apply {
            putFragment(outState, CURRENT_FRAGMENT_KEY, findFragmentById(R.id.directoryContainer)!!)
        }
    }

    // Add the root directory fragment which is the first screen the user sees.
    private fun initializeFileExplorer() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.directoryContainer, FilesListFragment())
            .addToBackStack(null).commit()
    }

    // Invoked from the fragment. Adds another fragment to the back stack representing the new directory the user clicked on.
    override fun onDirectoryClick(path: String) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.directoryContainer, FilesListFragment(path))
            .addToBackStack(null).commit()
    }

    override fun onBackPressed() {
        // pop the root directory fragment and then exit the app
        if (supportFragmentManager.backStackEntryCount == 1)
            super.onBackPressed()
        super.onBackPressed()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    initializeFileExplorer()
                else if (ActivityCompat.shouldShowRequestPermissionRationale(
                        this,
                        android.Manifest.permission.READ_EXTERNAL_STORAGE
                    )
                ) // If permission was denied once before but the user wasn't informed why the permission is necessary, do so.
                    AlertDialog.Builder(this)
                        .setMessage(R.string.external_storage_permission_rationale)
                        .setPositiveButton(android.R.string.ok) { dialog, _ ->
                            dialog.dismiss()
                            requestExternalStoragePermission()
                        }.show()
                else /* If user has chosen to not be shown permission requests any longer,
                     inform the user about it's importance and redirect her/him to device settings
                     so that permissions can be given */
                    requestPermissionAndOpenSettings()
            }
        }
    }

    private fun requestExternalStoragePermission() = ActivityCompat.requestPermissions(
        this,
        arrayOf(
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        ),
        REQUEST_CODE
    )

    private fun requestPermissionAndOpenSettings() = AlertDialog.Builder(this)
        .setMessage(R.string.permission_request)
        .setPositiveButton(R.string.show_settings) { dialog, _ ->
            dialog.dismiss()
            // Open application settings to enable the user to toggle the permission settings
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
            })
        }.show()
}

/** ListFragment class to display all the files and folders present inside a folder
 * @author Neeyat Lotlikar */
class FilesListFragment(private var path: String = ROOT_FLAG) : ListFragment() {

    companion object {
        private const val PATH_EXTRA = "path_extra"

        const val ROOT_FLAG = "root_path"

        interface DirectoryExplorer {
            fun onDirectoryClick(path: String)
        }
    }

    private lateinit var directoryExplorer: DirectoryExplorer

    private val values = mutableListOf<String>()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        directoryExplorer = context as DirectoryExplorer
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState != null) path = savedInstanceState.getString(PATH_EXTRA, path)

        val dir = File(path)

        val directories = mutableListOf<String>()
        val files = mutableListOf<String>()

        val list = dir.list()
        list?.let {
            for (file in it)
                if (!file.startsWith("."))
                    if (!file.contains("."))
                        directories.add(file)
                    else files.add(file)
        }
        // Sorting directories and files separately so that they can be displayed separately
        directories.sortBy { it.toLowerCase(Locale.ROOT) }
        files.sortBy { it.toLowerCase(Locale.ROOT) }
        values.apply {
            addAll(directories)
            addAll(files)
            if (this.isEmpty()) add(getString(R.string.empty_folder_indicator_item_text))
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        listAdapter = context?.let {
            ArrayAdapter(
                it,
                android.R.layout.simple_list_item_2,
                android.R.id.text1,
                values
            )
        }
        return super.onCreateView(inflater, container, savedInstanceState)
    }


    override fun onStart() {
        super.onStart()
        updateValues()
        (listAdapter as ArrayAdapter<*>).notifyDataSetChanged()
    }

    private fun updateValues() {
        if (values.isNotEmpty()) values.clear()

        if (path == ROOT_FLAG) {
            // Add root folders to values list
            val externalStorageFiles = ContextCompat.getExternalFilesDirs(context!!, null)
            externalStorageFiles.forEach {
                values.add(
                    it.path.toString()
                        .substringBefore("/Android/data/${context!!.packageName}/files")
                )
            }

        } else {
            val dir = File(path)
            val directories = mutableListOf<String>()
            val files = mutableListOf<String>()

            val list = dir.list()
            list?.let {
                for (file in it)
                    if (!file.startsWith("."))
                        if (!file.contains("."))
                            directories.add(file)
                        else files.add(file)
            }
            // Sorting directories and files separately so that they can be displayed separately
            directories.sortBy { it.toLowerCase(Locale.ROOT) }
            files.sortBy { it.toLowerCase(Locale.ROOT) }
            values.apply {
                addAll(directories)
                addAll(files)
                if (this.isEmpty()) add(getString(R.string.empty_folder_indicator_item_text))
            }
        }
    }

    override fun onListItemClick(l: ListView, v: View, position: Int, id: Long) {
        super.onListItemClick(l, v, position, id)
        var filename = listAdapter?.getItem(position).toString()

        if (filename == getString(R.string.empty_folder_indicator_item_text)) {
            Toast.makeText(context, R.string.empty_folder_indicator_item_text, Toast.LENGTH_SHORT)
                .show()
            return
        }

        if (path != ROOT_FLAG) // filename is already complete when ROOT_FLAG is the path
            filename = if (path.endsWith(File.separator)) path + filename
            else path + File.separator + filename

        val selectedItem = File(filename)
        when {
            selectedItem.isDirectory -> directoryExplorer.onDirectoryClick(filename)
            selectedItem.isFile &&
                    filename.subSequence(filename.lastIndexOf('.'), filename.length) == ".txt" ->
                startActivity(Intent(context, ReadFileActivity::class.java).apply {
                    setDataAndType(Uri.fromFile(File(filename)), "text/plain")
                })
            else -> Toast.makeText(context, R.string.please_select_a_txt_file, Toast.LENGTH_SHORT)
                .show()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(PATH_EXTRA, path)
        super.onSaveInstanceState(outState)
    }

}