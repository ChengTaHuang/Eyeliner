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
import com.eyeliner.eyeliner.palette.Palette
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import android.app.AlertDialog
import android.view.View
import android.widget.SeekBar
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener
import com.eyeliner.eyeliner.color.picker.CustomFlag
import com.skydoves.colorpickerview.ColorPickerDialog




class EyeEditActivity : AppCompatActivity() {

    private lateinit var uri: Uri
    private lateinit var colorPickerDialog : AlertDialog

    private enum class Menu {
        EDIT, PREVIEW
    }

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
        generateColorPicker()

        palette.viewTreeObserver.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                palette.viewTreeObserver.removeOnGlobalLayoutListener(this)
                val width = palette.measuredWidth
                val height = palette.measuredHeight
                val task = ConvertBitmapTask(baseContext, uri, palette, width, height)
                task.execute()
            }
        })
        /*
            編輯畫板區塊
         */
        menuEdit.setOnMenuButtonClickListener {
            if (!menuEdit.isOpened) {
                palette.changeSate(Palette.State.EDIT)
                changeMenu(Menu.EDIT)
            }
            menuEdit.toggle(true)
        }

        btnInsert.setOnClickListener {
            palette.addBezier()
            palette.changeSate(Palette.State.EDIT)
            menuEdit.close(true)
            hideAddendaUI()
        }

        btnColorPicker.setOnClickListener {
            palette.changeSate(Palette.State.EDIT)
            menuEdit.close(true)
            showColorPicker()
            hideAddendaUI()
        }

        btnWidthLine.setOnClickListener {
            palette.changeSate(Palette.State.EDIT)
            menuEdit.close(true)
            showSeekBar()
        }

        btnEdit.setOnClickListener {
            palette.changeSate(Palette.State.EDIT)
            menuEdit.close(true)
            hideAddendaUI()
        }

        btnDelete.setOnClickListener {
            palette.changeSate(Palette.State.DELETE)
            menuEdit.close(true)
            hideAddendaUI()
        }

        /*
            儲存畫板區塊
         */
        menuPreview.setOnMenuButtonClickListener {
            if (!menuPreview.isOpened) {
                palette.changeSate(Palette.State.SAVE)
                changeMenu(Menu.PREVIEW)
                hideAddendaUI()
            }
            menuPreview.toggle(true)
        }

        btnSave.setOnClickListener {
            palette.changeSate(Palette.State.SAVE)
            palette.isDrawingCacheEnabled = true
            val bitmapCache = palette.drawingCache
            saveTempBitmap(bitmapCache)
            menuPreview.close(true)
            hideAddendaUI()
        }

        seekBarWidth.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                palette.lineStrokeWidth = progress.toFloat()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }

        })

        seekBarSpacing.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                palette.dashPath = (progress.toFloat() + 1) * 5
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }

        })
    }

    private fun hideAddendaUI(){
        hideSeekBar()
    }

    private fun showSeekBar(){
        seekBarWidth.visibility = View.VISIBLE
        seekBarSpacing.visibility = View.VISIBLE
    }

    private fun hideSeekBar(){
        seekBarWidth.visibility = View.GONE
        seekBarSpacing.visibility = View.GONE
    }

    private fun generateColorPicker(){
        val colorPickerBuilder = ColorPickerDialog.Builder(this, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT)
        with(colorPickerBuilder){
            setFlagView(CustomFlag(this@EyeEditActivity, R.layout.layout_flag))
            setPositiveButton("ok" , ColorEnvelopeListener{ envelope, _ ->
                palette.pointColor = envelope.color
            })
            setNegativeButton("cancel"){ dialog , _->
                dialog.dismiss()
            }
        }

        colorPickerDialog = colorPickerBuilder.create()
    }

    private fun showColorPicker(){
        colorPickerDialog.show()
    }

    private fun changeMenu(type: Menu) {
        when (type) {
            Menu.EDIT -> {
                menuEdit.setMenuButtonColorNormalResId(R.color.colorDelete)
                menuPreview.setMenuButtonColorNormalResId(R.color.colorPoint)
                menuPreview.close(true)
            }

            Menu.PREVIEW -> {
                menuPreview.setMenuButtonColorNormalResId(R.color.colorDelete)
                menuEdit.setMenuButtonColorNormalResId(R.color.colorPoint)
                menuEdit.close(true)
            }

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
        val fname = "Eyeliner_$timeStamp.jpg"

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
