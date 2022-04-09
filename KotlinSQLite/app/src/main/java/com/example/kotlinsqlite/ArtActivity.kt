package com.example.kotlinsqlite

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.database.sqlite.SQLiteDatabase
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.kotlinsqlite.databinding.ActivityArtBinding
import com.google.android.material.snackbar.Snackbar
import java.io.ByteArrayOutputStream
import java.lang.Exception

class ArtActivity : AppCompatActivity() {
    private lateinit var binding: ActivityArtBinding
    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var permisionLauncher: ActivityResultLauncher<String>
    var selectedBitmap: Bitmap? = null
    private lateinit var database: SQLiteDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityArtBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        database = this.openOrCreateDatabase("Arts", MODE_PRIVATE, null)

        registerLauncher()

        val intent2 = intent
        val info = intent2.getStringExtra("info")

        if (info.equals("new")) {  // yeni bir veri girilecekse

            binding.artNameText.setText("")
            binding.artistNameText.setText("")
            binding.yearText.setText("")
            binding.imageView.setImageResource(R.drawable.selected)
            binding.button.visibility = View.VISIBLE

        } else {                // Girilmiş bir veriyi görmek

            binding.button.visibility = View.INVISIBLE
            val selectedId = intent.getIntExtra("id",1)
            val cursor = database.rawQuery("SELECT * FROM arts WHERE id = ?", arrayOf(selectedId.toString()))

            val artNameIx = cursor.getColumnIndex("artname")
            val artistNameIx = cursor.getColumnIndex("artistname")
            val yearIx = cursor.getColumnIndex("year")
            val imageIx = cursor.getColumnIndex("image")

            while (cursor.moveToNext()) {


                binding.artNameText.setText(cursor.getString(artNameIx))
                binding.artistNameText.setText(cursor.getString(artistNameIx))
                binding.yearText.setText(cursor.getString(yearIx))
                //Bunu alıp imageview'e koymak için Bitmap'e çevirmek gerekir
                val byteArray = cursor.getBlob(imageIx)
                val bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
                binding.imageView.setImageBitmap(bitmap)
            }
            cursor.close()

        }
    }

    fun saveButtonClicked(view: View) {

        val artName = binding.artNameText.text.toString()
        val artistName = binding.artistNameText.text.toString()
        val year = binding.yearText.text.toString()

        if (selectedBitmap != null) {
            val smallBitmap = makeSmallerBitmap(selectedBitmap!!, 300)
            //Görseli Veriye (0 ve 1) Çevirmek
            val outputStream = ByteArrayOutputStream()
            smallBitmap.compress(Bitmap.CompressFormat.JPEG, 50, outputStream)
            val byteArray = outputStream.toByteArray()

            try {

                database.execSQL("CREATE TABLE IF NOT EXISTS arts (id INTEGER PRİMARY KEY, artname VARCHAR, artistname VARCHAR,year VARCHAR,image BLOB)")

                val sqlString = "INSERT INTO arts (artname,artistname,year,image) VALUES (?,?,?,?)"
                val statement = database.compileStatement(sqlString)
                statement.bindString(1, artName)
                statement.bindString(2, artistName)
                statement.bindString(3, year)
                statement.bindBlob(4, byteArray)


                statement.execute()


            } catch (e: Exception) {
                e.printStackTrace()
            }

            val intent = Intent(this@ArtActivity, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)  //Bundan önce açık olan bütün aktiviteleri kapat
            startActivity(intent)

        }

    }

    private fun makeSmallerBitmap(image: Bitmap, maksimumSize: Int): Bitmap {
        var width = image.width
        var height = image.height

        val bitmapRatio: Double = width.toDouble() / height.toDouble()

        if (bitmapRatio > 1) {
            //landscape
            width = maksimumSize
            val scaledHeight = width / bitmapRatio
            height = scaledHeight.toInt()
        } else {
            //portrait
            height = maksimumSize
            val scaledWidth = height * bitmapRatio
            width = scaledWidth.toInt()
        }


        return Bitmap.createScaledBitmap(image, width, height, true)
    }

    fun selectImage(view: View) {

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
            ) {
                //rationale
                Snackbar.make(view, "Galeri için izin ver", Snackbar.LENGTH_INDEFINITE)
                    .setAction("İzin ver", View.OnClickListener {
                        //request permision
                        permisionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                    }).show()
            } else {
                //request permision
                permisionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }

        } else {
            //intent
            val intentToGalery =
                Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            activityResultLauncher.launch(intentToGalery)
        }

    }


    private fun registerLauncher() {

        activityResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK) {
                    val intentFromResult = result.data
                    if (intentFromResult != null) {
                        val imageData = intentFromResult.data

                        if (imageData != null) {
                            try {

                                if (Build.VERSION.SDK_INT >= 28) {
                                    val source = ImageDecoder.createSource(
                                        this@ArtActivity.contentResolver,
                                        imageData
                                    )
                                    selectedBitmap = ImageDecoder.decodeBitmap(source)
                                    binding.imageView.setImageBitmap(selectedBitmap)
                                } else {
                                    selectedBitmap = MediaStore.Images.Media.getBitmap(
                                        contentResolver,
                                        imageData
                                    )
                                    binding.imageView.setImageBitmap(selectedBitmap)
                                }

                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                }
            }

        permisionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { result ->
                if (result) {
                    //Permision Granted
                    val intentToGalery =
                        Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    activityResultLauncher.launch(intentToGalery)
                } else {
                    //Permision Denied
                    Toast.makeText(this@ArtActivity, "İzin Gerekli", Toast.LENGTH_LONG).show()
                }
            }

    }
}
