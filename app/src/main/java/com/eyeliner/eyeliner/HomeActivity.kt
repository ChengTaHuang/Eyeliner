package com.eyeliner.eyeliner

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_home.*


class HomeActivity : AppCompatActivity() {

    private val RESULT_LOAD_IMG = 100
    private val RESULT_PERMISSION = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        initViews()
    }

    private fun initViews() {
        floatingActionMenu.setOnMenuButtonClickListener {
            if (!floatingActionMenu.isOpened) {
                if (hasPermission()) {
                    openDefaultAlbumPicker()
                } else {
                    requestPermission()
                }
                floatingActionMenu.close(true)
            }
        }
    }

    private fun hasPermission(): Boolean {
        return (ContextCompat.checkSelfPermission(this@HomeActivity, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED)
    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(this@HomeActivity,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), RESULT_PERMISSION)
    }

    private fun openDefaultAlbumPicker() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), RESULT_LOAD_IMG)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                RESULT_LOAD_IMG -> {
                    EyeEditActivity.launch(this@HomeActivity, data!!.data)
                }
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            RESULT_PERMISSION -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    openDefaultAlbumPicker()
                } else {

                }
                return
            }
            else -> {

            }
        }
    }
}
