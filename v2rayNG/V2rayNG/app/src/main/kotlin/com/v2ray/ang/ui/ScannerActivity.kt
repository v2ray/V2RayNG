package com.v2ray.ang.ui

import android.app.Activity
import android.os.Bundle
import com.google.zxing.Result
import me.dm7.barcodescanner.zxing.ZXingScannerView
import android.content.Intent
import com.google.zxing.BarcodeFormat


class ScannerActivity : BaseActivity(), ZXingScannerView.ResultHandler {
    private var mScannerView: ZXingScannerView? = null

    public override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        mScannerView = ZXingScannerView(this)   // Programmatically initialize the scanner view

        mScannerView?.setAutoFocus(true)
        val formats = ArrayList<BarcodeFormat>()
        formats.add(BarcodeFormat.QR_CODE)
        mScannerView?.setFormats(formats)

        setContentView(mScannerView)                // Set the scanner view as the content view

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    public override fun onResume() {
        super.onResume()
        mScannerView!!.setResultHandler(this) // Register ourselves as a handler for scan results.
        mScannerView!!.startCamera()          // Start camera on resume
    }

    public override fun onPause() {
        super.onPause()
        mScannerView!!.stopCamera()           // Stop camera on pause
    }

    override fun handleResult(rawResult: Result) {
        // Do something with the result here
//        Log.v(FragmentActivity.TAG, rawResult.text) // Prints scan results
//        Log.v(FragmentActivity.TAG, rawResult.barcodeFormat.toString()) // Prints the scan format (qrcode, pdf417 etc.)

        val intent = Intent()
        intent.putExtra("SCAN_RESULT", rawResult.text)
        setResult(Activity.RESULT_OK, intent)
        finish()

        // If you would like to resume scanning, call this method below:
//        mScannerView!!.resumeCameraPreview(this)
    }
}
