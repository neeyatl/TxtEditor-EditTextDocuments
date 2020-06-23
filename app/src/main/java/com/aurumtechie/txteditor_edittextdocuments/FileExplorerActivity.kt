package com.aurumtechie.txteditor_edittextdocuments

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.Html
import android.text.InputType
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.setPadding
import androidx.fragment.app.ListFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import java.io.File
import java.util.*

class FileExplorerActivity : AppCompatActivity(), FilesListFragment.Companion.DirectoryExplorer {

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
        // Exit app onBackPressed when only the root fragment is present in the backStack
        if (supportFragmentManager.backStackEntryCount == 1)
            startActivity(Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }).also { finish() }
        else super.onBackPressed()
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
                    MaterialAlertDialogBuilder(this)
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

    /** Creates a new file in the current folder (function is an onClick function defined in activity layout)
     * @author Neeyat Lotlikar
     * @param view FloatingActionButton View object used to show a message to the user
     * @see com.google.android.material.floatingactionbutton.FloatingActionButton
     * @see R.layout.activity_file_explorer
     */
    fun addNewFile(view: View) {

        val activeFragment = // Get the fragment which is currently active
            (supportFragmentManager.findFragmentById(R.id.directoryContainer) as FilesListFragment)

        val dir = File(activeFragment.currentPath)
        if (!dir.canWrite()) {
            Snackbar.make(
                view,
                R.string.cannot_write_to_folder,
                Snackbar.LENGTH_LONG
            ).show()
            return
        }

        val fileName = "New File ${System.currentTimeMillis()}" // Default file name

        val file = File("${dir.absolutePath}${File.separatorChar}$fileName.txt")
        if (file.createNewFile()) {

            // Update activeFragmentUI
            activeFragment.updateListViewItems()

            Snackbar.make(
                view,
                R.string.file_created_successfully,
                Snackbar.LENGTH_LONG
            ).setAction(R.string.rename) {
                activeFragment.renameFile(file)
            }.show()
        } else
            Snackbar.make(
                view,
                R.string.file_cannot_be_created,
                Snackbar.LENGTH_LONG
            ).show()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = if (item.itemId == R.id.credits) {
        val creditsString = getString(R.string.credit_string)
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.credits)
            .setMessage(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                    Html.fromHtml(creditsString, Html.FROM_HTML_MODE_COMPACT)
                else Html.fromHtml(creditsString)
            ).setPositiveButton(R.string.follow_link) { dialog, _ ->
                dialog.dismiss()
                try {
                    startActivity(Intent(Intent.ACTION_VIEW).apply {
                        data = Uri.parse("https://www.flaticon.com/authors/freepik")
                    })
                } catch (e: ActivityNotFoundException) {
                    Toast.makeText(this, R.string.browser_not_found, Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton(android.R.string.cancel) { dialog, _ -> dialog.dismiss() }
            .show()
        true
    } else super.onOptionsItemSelected(item)

}

/** ListFragment class to display all the files and folders present inside a folder
 * @author Neeyat Lotlikar */
class FilesListFragment(private var path: String = ROOT_FLAG) : ListFragment(),
    AdapterView.OnItemLongClickListener {

    val currentPath: String
        get() = path

    companion object {
        private const val PATH_EXTRA = "path_extra"

        const val ROOT_FLAG = "root_path"

        interface DirectoryExplorer {
            fun onDirectoryClick(path: String)
        }
    }

    private lateinit var directoryExplorer: DirectoryExplorer

    /** Contains filenames of all the files and folders present in the current directory.
     * It supplies the ArrayAdapter with data to populate the ListView*/
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
        updateListViewItems()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        listView.onItemLongClickListener = this
    }

    /** Fetches and updates the values list with file and folder names which are present in the current folder
     * @author Neeyat Lotlikar
     * @see com.aurumtechie.txteditor_edittextdocuments.FilesListFragment.values*/
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

    /** Updates values and then notifies the ArrayAdapter to update the UI
     * @author Neeyat Lotlikar
     * @see com.aurumtechie.txteditor_edittextdocuments.FilesListFragment.values
     * @see getListAdapter*/
    fun updateListViewItems() {
        updateValues()
        (listAdapter as ArrayAdapter<*>).notifyDataSetChanged()
    }

    override fun onListItemClick(l: ListView, v: View, position: Int, id: Long) {
        super.onListItemClick(l, v, position, id)
        var filename = listAdapter?.getItem(position).toString()

        if (filename == getString(R.string.empty_folder_indicator_item_text)) {
            Snackbar.make(l, R.string.empty_folder_indicator_item_text, Snackbar.LENGTH_SHORT)
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
            else -> Snackbar.make(l, R.string.please_select_a_txt_file, Snackbar.LENGTH_SHORT)
                .show()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(PATH_EXTRA, path)
        super.onSaveInstanceState(outState)
    }

    /** Takes user input for the file name. Renames the file if it can be renamed. Prompts the user with the result.
     * @author Neeyat Lotlikar
     * @param file File object of the file to be renamed*/
    fun renameFile(file: File) {
        val fileNameEditText = EditText(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setPadding(10)
            hint = getString(R.string.enter_file_name)
            inputType = InputType.TYPE_CLASS_TEXT
        }

        val dialog = MaterialAlertDialogBuilder(context).setCustomTitle(fileNameEditText)
            .setPositiveButton(R.string.save) { dialog, _ ->
                dialog.dismiss()

                val inputFileName = fileNameEditText.text.toString().trim()
                if (inputFileName.isEmpty() || inputFileName.isBlank()) {
                    Snackbar.make(
                        listView,
                        R.string.filename_cannot_be_empty,
                        Snackbar.LENGTH_SHORT
                    ).show()
                    return@setPositiveButton
                }

                if (inputFileName.startsWith('.')) {
                    Snackbar.make(
                        listView,
                        R.string.filename_cannot_start_with_a_dot,
                        Snackbar.LENGTH_SHORT
                    ).show()
                    return@setPositiveButton
                }

                val finalFileName = "$currentPath${File.separator}$inputFileName.txt"

                File(currentPath).listFiles()?.forEach {
                    if (it.absolutePath == finalFileName) {
                        Snackbar.make(listView, R.string.file_already_exists, Snackbar.LENGTH_SHORT)
                            .show()
                        return@setPositiveButton
                    }
                }

                if (file.renameTo(File(finalFileName)))
                    Snackbar.make(listView, R.string.file_saved_successfully, Snackbar.LENGTH_SHORT)
                        .show()
                else Snackbar.make(listView, R.string.file_cannot_be_renamed, Snackbar.LENGTH_SHORT)
                    .show()

                updateListViewItems()
            }.create()
        dialog.show()
        dialog.window?.apply { // After the window is created, get the SoftInputMode
            clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
            clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
            setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        }
    }

    /** Takes user input for file deletion and deletes the file if it can be deleted. Prompts the user with the result.
     * @author Neeyat Lotlikar
     * @param file File object of the file to be deleted*/
    private fun requestFileDeletion(file: File) {
        if (!File(currentPath).canWrite()) {
            Snackbar.make(
                listView,
                R.string.file_cannot_be_deleted,
                Snackbar.LENGTH_SHORT
            ).show()
            return
        }

        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.delete_file)
            .setMessage(if (file.isDirectory) R.string.directory_delete_warning else R.string.are_you_sure)
            .setPositiveButton(android.R.string.yes) { dialog, _ ->
                dialog.dismiss()

                if (file.delete())
                    Snackbar.make(
                        listView,
                        R.string.file_deleted_successfully,
                        Snackbar.LENGTH_SHORT
                    ).show()
                else Snackbar.make(
                    listView,
                    R.string.file_cannot_be_deleted,
                    Snackbar.LENGTH_SHORT
                ).show()

                updateListViewItems()
            }.setNegativeButton(android.R.string.no) { dialog, _ ->
                dialog.dismiss()
            }.show()
    }

    override fun onItemLongClick(
        parent: AdapterView<*>?,
        view: View?,
        position: Int,
        id: Long
    ): Boolean {
        requestFileDeletion(
            File(
                currentPath + File.separator + listAdapter?.getItem(position).toString()
            )
        )
        return true
    }

}