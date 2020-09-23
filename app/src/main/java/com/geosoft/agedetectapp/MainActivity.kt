package com.geosoft.agedetectapp

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.gun0912.tedpermission.PermissionListener
import com.gun0912.tedpermission.TedPermission
import kotlinx.android.synthetic.main.activity_main.*
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Response
import java.io.File


class MainActivity : AppCompatActivity() {
    lateinit var cameraUtil : CameraUtil

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        cameraUtil = CameraUtil(this)
        camera_btn.setOnClickListener {
            cameraUtil.captureCamera()
        }
        picture_btn.setOnClickListener {
            cameraUtil.getAlbum()
        }
    }

    private fun reqAge(){
        println("testtest")
        val test = File(getRealPathFromURI(cameraUtil.getImageUri()?: Uri.EMPTY))
        val requestFile = RequestBody.create(MediaType.parse("multipart/form-data"),test)
        val body = MultipartBody.Part.createFormData("image",test.name,requestFile)
        ApiClient.getRetrofit()?.create(ApiService::class.java)?.getAge(body)?.enqueue(object: retrofit2.Callback<ResponseData>{
            override fun onFailure(call: Call<ResponseData>, t: Throwable) {
                t.printStackTrace()
            }

            override fun onResponse(call: Call<ResponseData>, response: Response<ResponseData>) {
                println("test="+response.body()?.output)
            }

        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if(resultCode != RESULT_OK) return
        when(requestCode){
            cameraUtil.REQUEST_TAKE_PHOTO -> {
                if (resultCode == Activity.RESULT_OK) {
                    try {
                        cameraUtil.galleryAddPic()
                        image.setImageURI(cameraUtil.getImageUri())
                        permission {
                            reqAge()
                        }

                    } catch (e: Exception) {
                    }

                } else {
                    Toast.makeText(this, "사진찍기를 취소하였습니다.", Toast.LENGTH_SHORT).show()
                }
            }
            cameraUtil.REQUEST_TAKE_ALBUM ->{
                if (resultCode == Activity.RESULT_OK) {
                    if (data?.data != null) {
                        try {
                            cameraUtil.setImageUri(data.data)
                            image.setImageURI(cameraUtil.getImageUri())
                            permission {
                                reqAge()
                            }
                        } catch (e: Exception) {
                        }
                    }
                }
            }

        }
        super.onActivityResult(requestCode, resultCode, data)

    }

    fun getRealPathFromURI(contentUri: Uri): String {
        val proj = arrayOf(MediaStore.Images.Media.DATA)
        val cursor: Cursor? = contentResolver.query(contentUri, proj, null, null, null)
        cursor?.moveToNext()
        val path: String = cursor?.getString(cursor.getColumnIndex(MediaStore.MediaColumns.DATA))?:""
        val uri: Uri = Uri.fromFile(File(path))
        cursor?.close()
        return path
    }

    private fun permission(func : () -> Unit){
        val permissionListener = object : PermissionListener {
            override fun onPermissionDenied(deniedPermissions: MutableList<String>?) {

            }

            override fun onPermissionGranted() {
                    func()
            }
        }
        TedPermission.with(this)
            .setPermissionListener(permissionListener)
            .setRationaleMessage(resources.getString(R.string.permission_2))
            .setDeniedMessage(resources.getString(R.string.permission_1))
            .setPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA)
            .check()
    }

}