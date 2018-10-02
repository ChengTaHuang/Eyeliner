package com.eyeliner.eyeliner

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.os.Environment
import android.support.v7.app.AppCompatActivity
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.widget.Toast
import com.bumptech.glide.Glide
import com.eyeliner.eyeliner.view.Palette
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*


class EyeEditActivity : AppCompatActivity() {

    private lateinit var uri: Uri

    companion object {
        val EXTRA_URI = "uri"

        fun launch(activity: Activity, uri: Uri) {
            val intent = Intent(activity, EyeEditActivity::class.java)
            intent.putExtra(EXTRA_URI, uri)
            activity.startActivity(intent)
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        uri = intent.extras[EXTRA_URI] as Uri
        initViews()
    }

    private fun initViews() {

        palette.viewTreeObserver.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                palette.viewTreeObserver.removeOnGlobalLayoutListener(this)
                val width = palette.measuredWidth
                val height = palette.measuredHeight
                val task = ConvertBitmapTask(baseContext, uri, palette, width, height)
                task.execute()
            }
        })

        menuSave.setOnClickListener {
            palette.changeSate(Palette.State.SAVE)
            palette.isDrawingCacheEnabled = true
            val bitmapCache = palette.drawingCache
            saveTempBitmap(bitmapCache)
            floatingActionMenu.close(true)
            floatingActionMenu.menuIconView.setImageResource(R.drawable.ic_save)
        }

        menuPreview.setOnClickListener {
            palette.changeSate(Palette.State.SAVE)
            floatingActionMenu.close(true)
            floatingActionMenu.menuIconView.setImageResource(R.drawable.ic_file)
        }

        menuInsert.setOnClickListener {
            palette.addBezier()
            palette.changeSate(Palette.State.EDIT)
            floatingActionMenu.close(true)
            floatingActionMenu.menuIconView.setImageResource(R.drawable.ic_edit)
        }

        menuEdit.setOnClickListener {
            palette.changeSate(Palette.State.EDIT)
            floatingActionMenu.close(true)
            floatingActionMenu.menuIconView.setImageResource(R.drawable.ic_edit)
        }

        menuDelete.setOnClickListener {
            palette.changeSate(Palette.State.DELETE)
            floatingActionMenu.close(true)
            floatingActionMenu.menuIconView.setImageResource(R.drawable.ic_garbage)
        }
    }

    @SuppressLint("StaticFieldLeak")
    private class ConvertBitmapTask(private val context: Context,
                                    private val uri: Uri,
                                    private val palette: Palette,
                                    private val width: Int,
                                    private val height: Int) : AsyncTask<Void, Void, Bitmap>() {

        override fun doInBackground(vararg params: Void): Bitmap? {
            val futureTarget = Glide.with(context)
                    .asBitmap()
                    .load(uri)
                    .submit(width, height)

            return futureTarget.get()
        }

        override fun onPreExecute() {
            super.onPreExecute()

        }

        override fun onPostExecute(result: Bitmap?) {
            super.onPostExecute(result)
            palette.setBackgroundBitmap(result)
        }
    }

    private fun saveTempBitmap(bitmap: Bitmap) {
        if (isExternalStorageWritable()) {
            saveImage(bitmap)
        } else {
        }
    }

    @SuppressLint("SimpleDateFormat")
    private fun saveImage(finalBitmap: Bitmap) {

        val root = Environment.getExternalStorageDirectory().toString()
        val myDir = File(root + "/Eyeliner")
        myDir.mkdirs()

        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val fname = "Shutta_$timeStamp.jpg"

        val file = File(myDir, fname)
        if (file.exists()) file.delete()
        try {
            val out = FileOutputStream(file)
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
            out.flush()
            out.close()
            Toast.makeText(applicationContext, getString(R.string.save_success), Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    private fun isExternalStorageWritable(): Boolean {
        val state = Environment.getExternalStorageState()
        return Environment.MEDIA_MOUNTED == state
    }
}