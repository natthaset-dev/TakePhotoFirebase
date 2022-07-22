package com.ocps.takephotofirebase

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class MainActivity : AppCompatActivity() {

    companion object {
        private const val CAMERA_PERMISSION_CODE = 1
        private const val READ_EXTERNAL_STORAGE_PERMISSION_CODE = 2
    }

    private lateinit var filePath: Uri
    private var currentPhotoPath: String? = null

    private var resultCameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val takenPhoto: Bitmap = BitmapFactory.decodeFile(currentPhotoPath)
            val rotatePhoto = rotateBitmap(takenPhoto)
            imvPhoto.setImageBitmap(rotatePhoto)
            btnUpload.isEnabled = true
        }
    }

    private var resultGalleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            filePath = result.data?.data!!
            imvPhoto.setImageURI(filePath)
            btnUpload.isEnabled = true
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("InflateParams")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnUpload.isEnabled = false

        btnCamera.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                takePhoto()
            } else {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_CODE)
            }
        }

        btnGallery.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                selectPhoto()
            } else {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), READ_EXTERNAL_STORAGE_PERMISSION_CODE)
            }
        }

        btnUpload.setOnClickListener {
            uploadImage()
        }

        imvPhoto.setOnClickListener {
            when (imvPhoto.rotation) {
                0f -> {
                    imvPhoto.rotation = 90f
                }
                90f -> {
                    imvPhoto.rotation = 180f
                }
                180f -> {
                    imvPhoto.rotation = 270f
                }
                else -> {
                    imvPhoto.rotation = 0f
                }
            }
        }
    }

    @SuppressLint("InflateParams")
    @RequiresApi(Build.VERSION_CODES.O)
    private fun uploadImage() {
        val dialog = Dialog(this, android.R.style.Theme_Translucent_NoTitleBar)
        val progressBarView = this.layoutInflater.inflate(R.layout.full_screen_progress_bar, null)
        dialog.setContentView(progressBarView)
        dialog.setCancelable(false)
        dialog.show()

        val date = getCurrentDateTime()
        val dateString = date.toString("yyyy_MM_dd_HH_mm_ss_SSS")
        val fileName = "$dateString.jpg"
        val imageRef: StorageReference = FirebaseStorage.getInstance().reference.child("images/$fileName")
        imageRef.putFile(filePath)
            .addOnSuccessListener {
                Toast.makeText(applicationContext, "Upload successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { p0 ->
                Toast.makeText(applicationContext, "Upload failed: ${p0.message}", Toast.LENGTH_SHORT).show()
            }
            .addOnCompleteListener {
                dialog.dismiss()
                imvPhoto.setImageResource(0)
            }
    }

    private fun Date.toString(format: String, locale: Locale = Locale.getDefault()): String {
        val formatter = SimpleDateFormat(format, locale)
        return formatter.format(this)
    }

    private fun getCurrentDateTime(): Date {
        return Calendar.getInstance().time
    }

    private fun rotateBitmap(source: Bitmap): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(90f)
        return Bitmap.createBitmap(
            source, 0, 0, source.width, source.height, matrix, true
        )
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            CAMERA_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    takePhoto()
                } else {
                    showToast("Unable to open camera")
                }
            }
            READ_EXTERNAL_STORAGE_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    selectPhoto()
                } else {
                    showToast("Unable to open photo gallery")
                }
            }
        }
    }

    @SuppressLint("QueryPermissionsNeeded")
    private fun takePhoto() {
        val fileName = "photo"
        val storageDirectory = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        try {
            val imageFile = File.createTempFile(fileName, ".jpg", storageDirectory)
            currentPhotoPath = imageFile.absolutePath
            val uri = FileProvider.getUriForFile(this, "com.ocps.takephotofirebase.fileprovider", imageFile)
            filePath = uri
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            intent.putExtra(MediaStore.EXTRA_OUTPUT, uri)
            resultCameraLauncher.launch(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun selectPhoto() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        resultGalleryLauncher.launch(intent)
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}