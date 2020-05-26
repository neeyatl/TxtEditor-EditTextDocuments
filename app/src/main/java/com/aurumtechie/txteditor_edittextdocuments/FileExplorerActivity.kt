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
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.ListFragment
import java.io.File

class FileExplorerActivity : AppCompatActivity(), FilesListFragment.Companion.DirectoryExplorer {

    companion object {
        private const val REQUEST_CODE = 4579
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_file_explorer)

        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        ) initializeFileExplorer() // If permissions are given, update the UI.
        else {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE
                )
            ) requestExternalStoragePermission()
            else requestPermissionAndOpenSettings()
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
            .replace(R.id.directoryContainer, FilesListFragment.getInstance(path))
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

class FilesListFragment : ListFragment() {

    private var path: String = "/storage/emulated/0"

    companion object {
        private const val PATH_EXTRA = "path_extra"

        fun getInstance(path: String): FilesListFragment =
            FilesListFragment().apply { this.path = path }

        interface DirectoryExplorer {
            fun onDirectoryClick(path: String)
        }
    }

    private lateinit var directoryExplorer: DirectoryExplorer

    private val values = mutableListOf<String>()

    private lateinit var arrayAdapter: ArrayAdapter<String>

    override fun onAttach(context: Context) {
        super.onAttach(context)
        directoryExplorer = context as DirectoryExplorer
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState != null) path = savedInstanceState.getString(PATH_EXTRA, path)

        val dir = File(path)

        val list = dir.list()
        list?.let {
            for (file in it)
                if (!file.startsWith(".") && file.endsWith(".txt") || !file.contains("."))
                    values.add(file)
        }
        values.sort()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        if (context != null) {
            arrayAdapter = ArrayAdapter(
                context!!,
                android.R.layout.simple_list_item_2,
                android.R.id.text1,
                values
            )
            listAdapter = arrayAdapter
        }
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        arrayAdapter.notifyDataSetChanged()
    }

    override fun onListItemClick(l: ListView, v: View, position: Int, id: Long) {
        super.onListItemClick(l, v, position, id)
        var filename = listAdapter?.getItem(position)

        filename = if (path.endsWith(File.separator)) path + filename
        else path + File.separator + filename

        if (File(filename).isDirectory)
            directoryExplorer.onDirectoryClick(filename)
        else startActivity(Intent(context, ReadFileActivity::class.java).apply {
            setDataAndType(Uri.fromFile(File(filename)), "text/plain")
        })
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(PATH_EXTRA, path)
        super.onSaveInstanceState(outState)
    }

}