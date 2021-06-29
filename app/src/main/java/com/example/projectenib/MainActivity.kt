package com.example.projectenib

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.telephony.SmsManager
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.zxing.ResultPoint
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.CaptureManager
import com.journeyapps.barcodescanner.camera.CameraSettings
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {
    private var tag : String = "MainActivity"
    lateinit var phone_number : String
    var result_qrcode : String = ""
    lateinit var captureManager: CaptureManager
    lateinit var smsManager: SmsManager
    val requestCode = 2
    val permissions = arrayOf(
        android.Manifest.permission.CAMERA,
        android.Manifest.permission.SEND_SMS,
        android.Manifest.permission.READ_CONTACTS
    )

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        requestPermission(permissions)
        checkPermissions(permissions)
        openBackCamera()

        btn_pick_number.setOnClickListener {
            pickContact()
        }
        btn_send.setOnClickListener {
            getNumber()
            sendSms(phone_number)
        }

    }

    override fun onResume() {
        super.onResume()
        //qrCodeView.resume()
        captureManager.onResume()
    }

    override fun onPause() {
        super.onPause()
        //qrCodeView.pauseAndWait()
        captureManager.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        //super.onDestroy()
        captureManager.onDestroy()
    }




    fun qr_scanned(){

        if(result_qrcode.isNotEmpty()){
            btn_pick_number.visibility = View.VISIBLE
            edit_txt_phone.visibility = View.VISIBLE
            btn_send.visibility = View.VISIBLE
        }
    }


    @RequiresApi(Build.VERSION_CODES.M)
    private fun checkPermissions(permissions: Array<out String>){

        permissions.forEach {
            if((checkSelfPermission(it)
                        != PackageManager.PERMISSION_GRANTED)){
                Toast.makeText(this, "permission non accordée", Toast.LENGTH_SHORT).show()
                (ActivityCompat.shouldShowRequestPermissionRationale(this, it))
            }
        }

    }



    private fun requestPermission(permissions: Array<out String>){

        ActivityCompat.requestPermissions(this, permissions, 1)
    }

    private fun openBackCamera(){
        captureManager = CaptureManager(this, qrCodeView)
        captureManager.initializeFromIntent(intent, Bundle())
        captureManager.decode()
        val cameraSettings: CameraSettings =
            qrCodeView.getBarcodeView().getCameraSettings()
        cameraSettings.isAutoFocusEnabled = true

        cameraSettings.focusMode = CameraSettings.FocusMode.CONTINUOUS
        cameraSettings.requestedCameraId = 0
        qrCodeView.pause()
        qrCodeView.getBarcodeView()
        //qrCodeView.scheduleLayoutAnimation()
        qrCodeView.setStatusText("")
        qrCodeView.getViewFinder().setVisibility(View.INVISIBLE)
        qrCodeView.decodeSingle((object : BarcodeCallback {
            override fun barcodeResult(result: BarcodeResult?) {
                result?.let {
                    result_qrcode = it.text
                    text_result.text =  it.text
                    qrCodeView.decodeContinuous(this)
                    qr_scanned()
                }
            }

            override fun possibleResultPoints(resultPoints: MutableList<ResultPoint>?) {
            }

        }))
        qrCodeView.resume()
    }
    //put all string in string folder to make the code cleaner
    private fun sendSms(phoneNumber: String){
        if(phoneNumber.isNotEmpty() || phoneNumber.isNotBlank()) {
            Log.d(tag, "sendSMS" + phoneNumber)
            smsManager = SmsManager.getDefault() as SmsManager
            var smsText: String =
                "Salut voici le resultat du Qr Code que j'ai scanné : // " + text_result.text.toString()
            smsManager.sendTextMessage(phoneNumber, null, smsText, null, null)
            Toast.makeText(this, "Message envoyé avec succés.", Toast.LENGTH_SHORT).show()
        }

        else{
            Toast.makeText(this, "Entrez un numero valide.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getNumber(){
        phone_number = edit_txt_phone.text.toString()
        Log.d(tag, phone_number)
    }
    //create a global variable contact_permission code later
    private fun pickContact(){
        val intent = Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI)
        startActivityForResult(intent, requestCode)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 2){
            if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                pickContact()
            }
            else{
                Toast.makeText(this, "permission denied ..", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == RESULT_OK){
            if(requestCode == requestCode){
                    val contactData: Uri = data!!.data!!
                    val c = managedQuery(contactData, null, null, null, null)
                    if (c.moveToFirst()) {
                        val id = c.getString(c.getColumnIndexOrThrow(ContactsContract.Contacts._ID))
                        val name = c.getString(c.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME))
                        val hasPhone =
                            c.getString(c.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER))
                        if (hasPhone.equals("1", ignoreCase = true)) {
                            val phones = contentResolver.query(
                                ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
                                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = " + id,
                                null, null
                            )
                            phones!!.moveToFirst()
                            var cNumber = phones!!.getString(phones!!.getColumnIndex("data1"))
                            Toast.makeText(this, name , Toast.LENGTH_SHORT).show()
                            edit_txt_phone.setText(cNumber)
                            phones.close()
                        }
                    }
                }
                else{
                    Toast.makeText(this, "canceled ..", Toast.LENGTH_SHORT).show()
                }
            }
        }

}