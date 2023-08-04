package com.zw.template.core

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.content.*
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.*
import android.location.Location
import android.location.LocationManager
import android.media.ExifInterface
import android.media.MediaMetadataRetriever
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.provider.Telephony
import android.text.SpannableStringBuilder
import android.text.method.ScrollingMovementMethod
import android.text.style.StyleSpan
import android.util.DisplayMetrics
import android.util.Log
import android.util.Patterns
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.Animation
import android.view.animation.Transformation
import android.view.animation.TranslateAnimation
import android.view.inputmethod.InputMethodManager
import android.webkit.MimeTypeMap
import android.widget.EditText
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.zw.template.R
import java.io.*
import java.lang.Math.atan2
import java.lang.Math.cos
import java.lang.Math.sin
import java.math.BigDecimal
import java.math.RoundingMode
import java.net.NetworkInterface
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.text.*
import java.util.*
import java.util.regex.Pattern

object CommonUtils {
    val MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 123
    val alert: AlertDialog? = null
    fun bitmapDescriptorFromVector(
        context: Context,
        @DrawableRes vectorDrawableResourceId: Int,
    ): BitmapDescriptor {
        val background =
            ContextCompat.getDrawable(context, vectorDrawableResourceId)!!
        background.setBounds(0, 0, background.intrinsicWidth, background.intrinsicHeight)
        val vectorDrawable = ContextCompat.getDrawable(context, vectorDrawableResourceId)!!
        vectorDrawable.setBounds(
            0,
            0,
            vectorDrawable.intrinsicWidth,
            vectorDrawable.intrinsicHeight
        )
        val bitmap = Bitmap.createBitmap(
            background.intrinsicWidth,
            background.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        background.draw(canvas)
        vectorDrawable.draw(canvas)
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    fun getStatusBarHeight(mActivity: Activity): Int {
        var result = 0
        val resourceId = mActivity.resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            result = mActivity.resources.getDimensionPixelSize(resourceId)
        }
        return result
    }

    // convert UTF-8 to internal Java String format
    fun convertUTF8ToString(s: String): String {
        return String(s.toByteArray(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8)
    }

    // convert internal Java String format to UTF-8
    fun convertStringToUTF8(s: String): String {
        return String(s.toByteArray(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1)
    }

//    fun getActionBarHeight(mActivity: Activity): Int {
//        // Calculate ActionBar height
//        var result = 0
//        val tv = TypedValue()
//        if (mActivity.theme.resolveAttribute(R.attr.actionBarSize, tv, true)) {
//            result =
//                TypedValue.complexToDimensionPixelSize(tv.data, mActivity.resources.displayMetrics)
//        }
//        return result
//    }


    fun resizeBitmap(mBitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        var image = mBitmap
        return if (maxHeight > 0 && maxWidth > 0) {
            val width = image.width
            val height = image.height
            if (width > maxWidth || height > maxHeight) {
                val ratioBitmap = width.toFloat() / height.toFloat()
                val ratioMax = maxWidth.toFloat() / maxHeight.toFloat()
                var finalWidth = maxWidth
                var finalHeight = maxHeight
                if (ratioMax > 1) {
                    finalWidth = (maxHeight.toFloat() * ratioBitmap).toInt()
                } else {
                    finalHeight = (maxWidth.toFloat() / ratioBitmap).toInt()
                }
                image = Bitmap.createScaledBitmap(image, finalWidth, finalHeight, true)
                image
            } else {
                image
            }
        } else {
            image
        }
    }

    fun getNewDateFormat(date: String, currentFormat: String, newFormat: String): String {
        var generatedDate = ""
        try {
            val originalFormat: DateFormat = SimpleDateFormat(currentFormat)
            val targetFormat: DateFormat = SimpleDateFormat(newFormat)
            val newDate = originalFormat.parse(date)
            if (newDate != null)
                generatedDate = targetFormat.format(newDate)
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("DateReview", e.toString())
        }
        return generatedDate
    }

    fun handleSamplingAndRotationBitmapForUpload(selectedImage: String?): Bitmap? {
        val MAX_HEIGHT = 2048
        val MAX_WIDTH = 2048

        // First decode with inJustDecodeBounds=true to check dimensions
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeFile(selectedImage, options)

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, MAX_WIDTH, MAX_HEIGHT)

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false
        var img = BitmapFactory.decodeFile(selectedImage, options)
        if (selectedImage != null) img = rotateImageIfRequired(img, selectedImage)
        return img
    }

    fun getImageAspectRatio(bitmap: Bitmap): Float {
        return bitmap.height.toFloat() / bitmap.width
    }


    fun handleSamplingAndRotationBitmap(selectedImage: String?): Bitmap? {
        val MAX_HEIGHT = 400
        val MAX_WIDTH = 400

        // First decode with inJustDecodeBounds=true to check dimensions
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeFile(selectedImage, options)

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, MAX_WIDTH, MAX_HEIGHT)

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false
        var img = BitmapFactory.decodeFile(selectedImage, options)
        if (selectedImage != null) img = rotateImageIfRequired(img, selectedImage)
        return img
    }

    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int, reqHeight: Int
    ): Int {
        // Raw height and width of image
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {

            // Calculate ratios of height and width to requested height and width
            val heightRatio = Math.round(height.toFloat() / reqHeight.toFloat())
            val widthRatio = Math.round(width.toFloat() / reqWidth.toFloat())

            // Choose the smallest ratio as inSampleSize value, this will guarantee strike_text final image
            // with both dimensions larger than or equal to the requested height and width.
            inSampleSize = if (heightRatio < widthRatio) heightRatio else widthRatio

            // This offers some additional logic in case the image has strike_text strange
            // aspect ratio. For example, strike_text panorama may have strike_text much larger
            // width than height. In these cases the total pixels might still
            // end up being too large to fit comfortably in memory, so we should
            // be more aggressive with sample down the image (=larger inSampleSize).
            val totalPixels = (width * height).toFloat()

            // Anything more than 2x the requested pixels we'll sample down further
            val totalReqPixelsCap = (reqWidth * reqHeight * 2).toFloat()
            while (totalPixels / (inSampleSize * inSampleSize) > totalReqPixelsCap) {
                inSampleSize++
            }
        }
        return inSampleSize
    }

    private fun rotateImageIfRequired(img: Bitmap?, selectedImage: String): Bitmap? {
        try {
            val ei = ExifInterface(selectedImage)
            val orientation =
                ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            return when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> rotateImage(img, 90)
                ExifInterface.ORIENTATION_ROTATE_180 -> rotateImage(img, 180)
                ExifInterface.ORIENTATION_ROTATE_270 -> rotateImage(img, 270)
                else -> img
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }

    private fun rotateImage(img: Bitmap?, degree: Int): Bitmap? {
        val matrix = Matrix()
        matrix.postRotate(degree.toFloat())
        val rotatedImg = Bitmap.createBitmap(img!!, 0, 0, img.width, img.height, matrix, true)
        img.recycle()
        return rotatedImg
    }

    fun saveImage(context: Context, bitmap: Bitmap, dirName: String): String? {
//        String filePah = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + context.getPackageName() + File.separator + dirName;
        val filePah = commonDocumentDirPath(dirName, context).absolutePath
        if (!File(filePah).exists()) {
            File(filePah).mkdirs()
        }
        val calendar = Calendar.getInstance()
        val filename =
            File.separator + "Roots_IMG_" + calendar[Calendar.YEAR] + "_" + (calendar[Calendar.MONTH] + 1) + "_" + calendar[Calendar.DAY_OF_MONTH] + "_" + calendar[Calendar.HOUR_OF_DAY] + "_" + calendar[Calendar.MINUTE] + "_" + calendar[Calendar.SECOND] + ".jpg"
        var out: FileOutputStream? = null
        try {
            out = FileOutputStream(filePah + filename)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out) // bmp is your Bitmap instance
            // PNG is strike_text lossless format, the compression factor (100) is ignored
            return filePah + filename
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                out?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        return filePah + File.separator + filename
    }

    fun moveFile(inputPath: String, inputFile: String, context: Context, dirName: String): String {
        val filePah =
            Environment.getExternalStorageDirectory().absolutePath + File.separator + context.packageName + dirName
        val calendar = Calendar.getInstance()
        val filename =
            File.separator + "photo_" + calendar[Calendar.YEAR] + "_" + (calendar[Calendar.MONTH] + 1) + "_" + calendar[Calendar.DAY_OF_MONTH] + "_" + calendar[Calendar.HOUR_OF_DAY] + "_" + calendar[Calendar.MINUTE] + "_" + calendar[Calendar.SECOND] + ".jpg"
        if (!File(filePah).exists()) {
            File(filePah).mkdirs()
        }
        var `in`: InputStream?
        var out: OutputStream?
        try {

            //create output directory if it doesn't exist
            val dir = File(filePah)
            if (!dir.exists()) {
                dir.mkdirs()
            }
            `in` = FileInputStream(inputPath + inputFile)
            out = FileOutputStream(filePah + filename)
            val buffer = ByteArray(1024)
            var read: Int
            while (`in`.read(buffer).also { read = it } != -1) {
                out.write(buffer, 0, read)
            }
            `in`.close()

            // write the output file
            out.flush()
            out.close()

            // delete the original file
            File(inputPath + inputFile).delete()
        } catch (fnfe1: FileNotFoundException) {
            printLog("tag", fnfe1.message)
        } catch (e: Exception) {
            printLog("tag", e.message)
        }
        return filePah + filename
    }

    fun deleteFile(path: String): Boolean {
        val mFile = File(path)
        return if (mFile.exists()) {
            mFile.delete()
        } else {
            false
        }
    }

    fun isTablet(context: Context): Boolean {
        return ((context.resources.configuration.screenLayout
                and Configuration.SCREENLAYOUT_SIZE_MASK)
                >= Configuration.SCREENLAYOUT_SIZE_LARGE)
    }

    fun getDeviceHeight(context: Context): Int {
        val displayMetrics = DisplayMetrics()
        (context as Activity).windowManager.defaultDisplay.getMetrics(displayMetrics)
        return displayMetrics.heightPixels
    }

    fun getDeviceWidth(context: Context): Int {
        val displayMetrics = DisplayMetrics()
        (context as Activity).windowManager.defaultDisplay.getMetrics(displayMetrics)
        return displayMetrics.widthPixels
    }

    fun getDeviceName(): String? {
        val manufacturer = Build.MANUFACTURER
        val model = Build.MODEL
        return if (model.startsWith(manufacturer)) {
            model
        } else {
            "$manufacturer $model"
        }
    }

    /*A string that uniquely identifies this build.*/
    fun getDeviceFingerPrint(): String? {
        return Build.FINGERPRINT
    }

    fun getTabletSize(context: Context): Int {
        val metrics = DisplayMetrics()
        (context as Activity).windowManager.defaultDisplay.getMetrics(metrics)
        val widthPixels = metrics.widthPixels
        val heightPixels = metrics.heightPixels

        /*float scaleFactor = metrics.density;

        float widthDp = widthPixels / scaleFactor;
        float heightDp = heightPixels / scaleFactor;

        float smallestWidth = Math.min(widthDp, heightDp);

        if (smallestWidth > 720) {
            //Device is strike_text 10" tablet
        }
        else if (smallestWidth > 600) {
            //Device is strike_text 7" tablet
        }*/
        val widthDpi = metrics.xdpi
        val heightDpi = metrics.ydpi
        val widthInches = widthPixels / widthDpi
        val heightInches = heightPixels / heightDpi
        val diagonalInches = Math.sqrt(
            (
                    widthInches * widthInches
                            + heightInches * heightInches).toDouble()
        )
        return if (diagonalInches >= 10) {
            10
        } else if (diagonalInches >= 9) {
            9
        } else if (diagonalInches >= 7) {
            7
        } else {
            7
        }
    }

    /**
     * Method to request focus of view
     *
     * @param p_view
     */
    fun requestFocus(p_view: View, p_Context: Context) {
        if (p_view.requestFocus()) {
            (p_Context as Activity).window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        }
    }

    fun isInternetAvailable(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetworkInfo = connectivityManager.activeNetworkInfo
        return activeNetworkInfo != null && activeNetworkInfo.isConnected
    }

    fun validateForEmpty(p_editText: EditText, p_context: Context, p_string: String?): Boolean {
        if (p_editText.text.toString().trim { it <= ' ' }.isEmpty()) {
            p_editText.error = p_string
            requestFocus(p_editText, p_context)
            return false
        } else {
            p_editText.error = null
        }
        return true
    }

    fun mark(
        src: Bitmap,
        watermark: String?,
        location: Point,
        color: Int,
        alpha: Int,
        size: Int,
        underline: Boolean
    ): Bitmap? {
        val w = src.width
        val h = src.height
        val result = Bitmap.createBitmap(w, h, src.config)
        val canvas = Canvas(result)
        canvas.drawBitmap(src, 0f, 0f, null)
        val paint = Paint()
        paint.color = color
        paint.alpha = alpha
        paint.textSize = size.toFloat()
        paint.isAntiAlias = true
        paint.isUnderlineText = underline
        canvas.drawText(watermark!!, location.x.toFloat(), location.y.toFloat(), paint)
        return result
    }

    fun isValidEmail(target: CharSequence?): Boolean {
        return if (target == null) {
            false
        } else {
            Patterns.EMAIL_ADDRESS.matcher(target).matches()
        }
    }

    fun isValidPhone(phone: String): Boolean {
        val p = Pattern.compile("^[+]?[0-9]{10,15}$")
        val m = p.matcher(phone)
        return m.matches()
    }

    fun showKeyBoard(view: View?) {
        if (view != null) {
            if (view is EditText) {
                val editText = view
                editText.setSelection(editText.length())
            }
            val imm =
                view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    fun showKeyBoard(editText: EditText) {
        val imm =
            editText.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(editText, InputMethodManager.SHOW_FORCED)
    }

    fun hideKeyBoard(view: View?) {
        if (view != null) {
            val imm =
                view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }


    fun getMimeType(context: Context, uri: Uri): String? {
        var mimeType: String?
        mimeType = if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
            val cr = context.contentResolver
            cr.getType(uri)
        } else {
            val fileExtension = MimeTypeMap.getFileExtensionFromUrl(
                uri
                    .toString()
            )
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                fileExtension.lowercase(Locale.getDefault())
            )
        }
        return mimeType
    }


    /**
     * This method converts dp unit to equivalent pixels, depending on device density.
     *
     * @param dp      A value in dp (density independent pixels) unit. Which we need to convert into pixels
     * @param context Context to get resources and device specific display metrics
     * @return A float value to represent px equivalent to dp depending on device density
     */
    fun convertDpToPixel(dp: Float, context: Context): Float {
        val resources = context.resources
        val metrics = resources.displayMetrics
        return dp * (metrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)
    }

    /**
     * This method converts device specific pixels to density independent pixels.
     *
     * @param px      A value in px (pixels) unit. Which we need to convert into dp
     * @param context Context to get resources and device specific display metrics
     * @return A float value to represent dp equivalent to px value
     */
    fun convertPixelsToDp(px: Float, context: Context): Float {
        val resources = context.resources
        val metrics = resources.displayMetrics
        return px / (metrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)
    }

    fun copyFile(
        inputPath: String?,
        destination: File,
        extension: String
    ): String {
        val calendar = Calendar.getInstance()
        val filename =
            "file" + calendar[Calendar.YEAR] + "_" + (calendar[Calendar.MONTH] + 1) + "_" + calendar[Calendar.DAY_OF_MONTH] + "_" + calendar[Calendar.HOUR_OF_DAY] + "_" + calendar[Calendar.MINUTE] + "_" + calendar[Calendar.SECOND] + extension
        var `in`: InputStream?
        var out: OutputStream?
        try {

            //create output directory if it doesn't exist
            if (!destination.exists()) {
                destination.mkdirs()
            }
            `in` = FileInputStream(inputPath)
            out = FileOutputStream(destination.absolutePath + File.separator + filename)
            val buffer = ByteArray(1024)
            var read: Int
            while (`in`.read(buffer).also { read = it } != -1) {
                out.write(buffer, 0, read)
            }
            `in`.close()

            // write the output file
            out.flush()
            out.close()
        } catch (fnfe1: FileNotFoundException) {
            printLog("tag", fnfe1.message)
        } catch (e: Exception) {
            printLog("tag", e.message)
        }
        return filename
    }

    private fun printLog(tag: String?, message: String?) {
        Log.d(tag, message!!)
    }

    fun getDefaultSmsAppPackageName(context: Context): String? {
        return Telephony.Sms.getDefaultSmsPackage(
            context
        )
    }

    private fun findViewSupportOrAndroid(root: View, resourceName: String): View? {
        val context = root.context
        var result: View? = null
        if (result == null) {
            val supportID = context.resources.getIdentifier(resourceName, "id", context.packageName)
            result = root.findViewById(supportID)
        }
        if (result == null) {
            val androidID = context.resources.getIdentifier(resourceName, "id", "android")
            result = root.findViewById(androidID)
        }
        return result
    }

    fun openDialer(context: Context, number: String) {
        val intent = Intent(Intent.ACTION_DIAL)
        intent.data = Uri.parse("tel:$number")
        context.startActivity(intent)
    }


    /**
     * Load UTF8withBOM or any ansi text file.
     *
     * @param filename
     * @return
     * @throws IOException
     */
    @Throws(IOException::class)
    fun loadFileAsString(filename: String?): String? {
        val BUFLEN = 1024
        val `is` = BufferedInputStream(FileInputStream(filename), BUFLEN)
        return try {
            val baos = ByteArrayOutputStream(BUFLEN)
            val bytes = ByteArray(BUFLEN)
            var isUTF8 = false
            var read: Int
            var count = 0
            while (`is`.read(bytes).also { read = it } != -1) {
                if (count == 0 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()) {
                    isUTF8 = true
                    baos.write(bytes, 3, read - 3) // drop UTF8 bom marker
                } else {
                    baos.write(bytes, 0, read)
                }
                count += read
            }
            if (isUTF8) baos.toString(StandardCharsets.UTF_8.toString()) else baos.toString()
        } finally {
            try {
                `is`.close()
            } catch (ex: Exception) {
            }
        }
    }

    /**
     * Returns MAC address of the given interface name.
     *
     * @param interfaceName eth0, wlan0 or NULL=use first interface
     * @return mac address or empty string
     */
    fun getMACAddress(interfaceName: String?): String? {
        try {
            val interfaces: List<NetworkInterface> =
                Collections.list(NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces) {
                if (interfaceName != null) {
                    if (!intf.name.equals(interfaceName, ignoreCase = true)) continue
                }
                val mac = intf.hardwareAddress ?: return ""
                val buf = StringBuilder()
                for (idx in mac.indices) buf.append(String.format("%02X:", mac[idx]))
                if (buf.length > 0) buf.deleteCharAt(buf.length - 1)
                return buf.toString()
            }
        } catch (ex: Exception) {
        } // for now eat exceptions
        return ""
        /*try {
            // this is so Linux hack
            return loadFileAsString("/sys/class/net/" +interfaceName + "/address").toUpperCase().trim();
        } catch (IOException ex) {
            return null;
        }*/
    }

    fun closeInternetAlert() {
        if (CommonUtils.alert != null) CommonUtils.alert.dismiss()
    }

    //Checking for location enabled or disabled
    fun isLocationEnabled(mContext: Context): Boolean {
        val locationManager = mContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    //returns distance in kilometers (km)
    fun distanceInKms(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val theta = lon1 - lon2
        var dist = (Math.sin(deg2rad(lat1))
                * Math.sin(deg2rad(lat2))
                + (Math.cos(deg2rad(lat1))
                * Math.cos(deg2rad(lat2))
                * Math.cos(deg2rad(theta))))
        dist = Math.acos(dist)
        dist = rad2deg(dist)
        dist *= 60 * 1.1515
        return dist
    }

    private fun deg2rad(deg: Double): Double {
        return deg * Math.PI / 180.0
    }

    private fun rad2deg(rad: Double): Double {
        return rad * 180.0 / Math.PI
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    fun checkPermission(context: Context?): Boolean {
        val currentAPIVersion = Build.VERSION.SDK_INT
        return if (currentAPIVersion >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(
                    context!!,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
                    context, Manifest.permission.CAMERA
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(
                        (context as Activity?)!!,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    ) || ActivityCompat.shouldShowRequestPermissionRationale(
                        (context as Activity?)!!, Manifest.permission.CAMERA
                    )
                ) {
                    val alertBuilder = AlertDialog.Builder(
                        context
                    )
                    alertBuilder.setCancelable(true)
                    alertBuilder.setTitle("Permission necessary")
                    alertBuilder.setMessage("External storage & camera permission is necessary")
                    alertBuilder.setPositiveButton(
                        "Yes"
                    ) { _, _ ->
                        ActivityCompat.requestPermissions(
                            (context as Activity?)!!,
                            arrayOf(
                                Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.CAMERA
                            ),
                            MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE
                        )
                    }
                    val alert = alertBuilder.create()
                    alert.show()
                } else {
                    ActivityCompat.requestPermissions(
                        (context as Activity?)!!,
                        arrayOf(
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.CAMERA
                        ),
                        MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE
                    )
                }
                false
            } else {
                true
            }
        } else {
            true
        }
    }

    fun parseDate(time: String): String? {
        val inputPattern = "dd/MM/yyyy HH:mm:ss"
        val outputPattern = "HH:mm:ss dd-MMM-yyyy"
        val inputFormat = SimpleDateFormat(inputPattern, Locale.US)
        val outputFormat = SimpleDateFormat(outputPattern, Locale.US)
        var date: Date?
        var str: String? = null
        try {
            date = inputFormat.parse(time)
            if (date != null) str = outputFormat.format(date)
        } catch (e: ParseException) {
            e.printStackTrace()
        }
        return str
    }

    fun expandView(v: View) {
        v.measure(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        val targetHeight = v.measuredHeight
        v.layoutParams.height = 1
        v.visibility = View.VISIBLE
        val a: Animation = object : Animation() {
            override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
                v.layoutParams.height =
                    if (interpolatedTime == 1f) ViewGroup.LayoutParams.WRAP_CONTENT else (targetHeight * interpolatedTime).toInt()
                v.requestLayout()
            }

            override fun willChangeBounds(): Boolean {
                return true
            }
        }

        // 1dp/ms
        a.duration = (targetHeight / v.context.resources.displayMetrics.density).toLong()
        v.startAnimation(a)
    }

    fun collapseView(v: View) {
        val initialHeight = v.measuredHeight
        val a: Animation = object : Animation() {
            override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
                if (interpolatedTime == 1f) {
                    v.visibility = View.GONE
                } else {
                    v.layoutParams.height =
                        initialHeight - (initialHeight * interpolatedTime).toInt()
                    v.requestLayout()
                }
            }

            override fun willChangeBounds(): Boolean {
                return true
            }
        }

        // 1dp/ms
//        a.setDuration((int) (initialHeight / v.getContext().getResources().getDisplayMetrics().density));
        a.duration = 300
        v.startAnimation(a)
    }

    fun getContrastColor(bgColor: Int): Int {
        return Color.rgb(
            255 - Color.red(bgColor),
            255 - Color.green(bgColor),
            255 - Color.blue(bgColor)
        )
    }

    fun getWeekDayName(dayNo: Int): String? {
        when (dayNo) {
            1 -> return "Sunday"
            2 -> return "Monday"
            3 -> return "Tuesday"
            4 -> return "Wednesday"
            5 -> return "Thursday"
            6 -> return "Friday"
            7 -> return "Saturday"
        }
        return ""
    }

    fun calculateDateDifference(startDate: Date, endDate: Date): String? {
        //milliseconds
        var different = endDate.time - startDate.time
        printLog("startDate : ", startDate.toString())
        printLog("endDate : ", endDate.toString())
        val secondsInMilli: Long = 1000
        val minutesInMilli = secondsInMilli * 60
        val hoursInMilli = minutesInMilli * 60
        val daysInMilli = hoursInMilli * 24
        val elapsedDays = different / daysInMilli
        different = different % daysInMilli
        val elapsedHours = different / hoursInMilli
        different = different % hoursInMilli
        val elapsedMinutes = different / minutesInMilli
        different = different % minutesInMilli
        val elapsedSeconds = different / secondsInMilli
        System.out.printf(
            "%d days, %d hours, %d minutes, %d seconds%n",
            elapsedDays, elapsedHours, elapsedMinutes, elapsedSeconds
        )
        printLog("elapsedDays : ", elapsedDays.toString())
        printLog("elapsedHours : ", elapsedHours.toString())
        printLog("elapsedMinutes : ", elapsedMinutes.toString())
        printLog("elapsedSeconds : ", elapsedSeconds.toString())
        return elapsedDays.toString()
    }

    /*set scrollView inside editText*/
    fun makeEditTextScrollable(editText: EditText) {
        editText.setOnTouchListener(OnTouchListener { v, event ->
            v.parent.requestDisallowInterceptTouchEvent(true)
            when (event.action and MotionEvent.ACTION_MASK) {
                MotionEvent.ACTION_SCROLL -> {
                    v.parent.requestDisallowInterceptTouchEvent(false)
                    return@OnTouchListener true
                }
            }
            false
        })
    }

    /*convert Hours:Minutes in AM/PM*/
    fun convertTimeInAM_PM(hours: Int, minutes: Int): String {
        var hour = hours
        val timeSet: String
        when {
            hour > 12 -> {
                hour -= 12
                timeSet = "PM"
            }

            hour == 0 -> {
                hour += 12
                timeSet = "AM"
            }

            hour == 12 -> {
                timeSet = "PM"
            }

            else -> {
                timeSet = "AM"
            }
        }
        val min: String = if (minutes < 10) "0$minutes" else minutes.toString()

        // Append in a StringBuilder
        return StringBuilder().append(hour).append(':')
            .append(min).append(" ").append(timeSet).toString()
    }

    fun getFormattedNumber(number: Double): String? {
        val df = DecimalFormat("0.00")
        df.roundingMode = RoundingMode.CEILING
        return df.format(number)
    }

    /* 1.4 -> 1
     * 1.6 -> 2
     * -2.1 -> -2
     * -1.3 -> -1
     * -1.5 -> -2*/
    fun getFormattedAmount(number: Double): String? {
        val formatter = DecimalFormat("#,##,###")
        formatter.roundingMode = RoundingMode.CEILING
        return formatter.format(number)
    }

    fun currencyFormatter(num: Double?): String? {
        val formatter = DecimalFormat("#,##,###")
        return formatter.format(num)
    }

    fun getFormattedAmount(amount: Int): String? {
        return NumberFormat.getNumberInstance(Locale.US).format(amount.toLong())
    }

    fun roundTwoDecimals(d: Double): Double {
        val twoDForm = DecimalFormat("#.##")
        return java.lang.Double.valueOf(twoDForm.format(d))
    }

    fun round(value: Double, places: Int): Double {
        require(places >= 0)
        var bd = BigDecimal(value)
        bd = bd.setScale(places, RoundingMode.HALF_UP)
        return bd.toDouble()
    }

    //create bitmap from view and returns it
    fun getBitmapFromView(view: View): Bitmap? {
        //Define a bitmap with the same size as the view
        val returnedBitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        //Bind a canvas to it
        val canvas = Canvas(returnedBitmap)
        canvas.drawColor(Color.DKGRAY)
        // draw the view on the canvas
        view.draw(canvas)
        //return the bitmap
        return returnedBitmap
    }

    @Throws(Throwable::class)
    fun retriveVideoFrameFromVideo(videoPath: String?): Bitmap? {
        var bitmap: Bitmap? = null
        var mediaMetadataRetriever: MediaMetadataRetriever? = null
        try {
            mediaMetadataRetriever = MediaMetadataRetriever()
            mediaMetadataRetriever.setDataSource(videoPath, HashMap())
            bitmap = mediaMetadataRetriever.frameAtTime
        } catch (e: Exception) {
            e.printStackTrace()
            throw Throwable("Exception in retriveVideoFrameFromVideo(String videoPath)" + e.message)
        } finally {
            mediaMetadataRetriever?.release()
        }
        return bitmap
    }

    fun getImageUri(inContext: Context, inImage: Bitmap): Uri? {
        fixMediaDir()
        val bytes = ByteArrayOutputStream()
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
        var path =
            MediaStore.Images.Media.insertImage(inContext.contentResolver, inImage, "Title", null)
        if (path == null) {
            path = ""
        }
        return Uri.parse(path)
    }

    fun fixMediaDir() {
        val sdcard = Environment.getExternalStorageDirectory()
        if (sdcard != null) {
            val mediaDir = File(sdcard, "DCIM/Camera")
            if (!mediaDir.exists()) {
                mediaDir.mkdirs()
            }
        }
    }

    fun isStrNotEmpty(data: String?): Boolean {
        return data != null && !data.trim { it <= ' ' }.isEmpty()
    }

    fun isEmpty(s: CharSequence?): Boolean {
        return s == null || s.length == 0
    }

    private fun getDeviceBoard(): String? {
        return Build.BOARD
    }

    fun toTitleCase(str: String?): String? {
        if (str == null) {
            return null
        }
        var space = true
        val builder = StringBuilder(str)
        val len = builder.length
        for (i in 0 until len) {
            val c = builder[i]
            if (space) {
                if (!Character.isWhitespace(c)) {
                    // Convert to title case and switch out of whitespace mode.
                    builder.setCharAt(i, Character.toTitleCase(c))
                    space = false
                }
            } else if (Character.isWhitespace(c)) {
                space = true
            } else {
                builder.setCharAt(i, Character.toLowerCase(c))
            }
        }
        return builder.toString()
    }

    fun isColorDark(color: Int): Boolean {
        val darkness =
            1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255
        // It's a dark color
        return darkness >= 0.5 // It's a light color
    }

    fun isDark(color: Int): Boolean {
        return ColorUtils.calculateLuminance(color) < 0.5
    }

    fun isWhite(color: String): Boolean {
        val list = ArrayList(
            Arrays.asList(
                "#FFFFFF",
                "#FEFEFE",
                "#FDFDFD",
                "#FCFCFC",
                "#FBFBFB",
                "#FAFAFA",
                "#F9F9F9",
                "#F8F8F8",
                "#F7F7F7",
                "#F6F6F6"
            )
        )
        return list.contains(color)
    }

    fun copyText(mActivity: Activity, content: String?) {
        val clipboard = mActivity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("label", content)
        if (clipboard == null || clip == null) return
        clipboard.setPrimaryClip(clip)
    }

    fun getLastDigits(digit: Int, data: String): String? {
        var lastFourDigits: String? = ""
        return if (data.length == digit) {
            data.also { lastFourDigits = it }
        } else {
            if (data.length > digit) {
                data.substring(data.length - digit).also { lastFourDigits = it }
            } else {
                data.also { lastFourDigits = it }
            }
        }
    }

    // slide the view from below itself to the current position
    fun slideUp(view: View) {
        view.visibility = View.VISIBLE
        val animate = TranslateAnimation(0f, 0f, view.height.toFloat(), 0f)
        animate.duration = 500
        animate.fillAfter = false
        view.startAnimation(animate)
    }

    // slide the view from its current position to below itself
    fun slideDown(view: View) {
        val animate = TranslateAnimation(0f, 0f, 0f, view.height.toFloat())
        animate.duration = 500
        animate.fillAfter = false
        view.startAnimation(animate)
        Handler(Looper.getMainLooper()).postDelayed({
            view.visibility = View.GONE
        }, 500)
    }

    fun slideFromRightToLeft(view: View) {
        val animate = TranslateAnimation(view.width.toFloat(), 0f, 0f, 0f)
        animate.duration = 500
        animate.fillAfter = false
        view.startAnimation(animate)
        view.visibility = View.VISIBLE
    }

    fun slideFromLeftToRight(view: View) {
        val animate = TranslateAnimation(0f, view.width.toFloat(), 0f, 0f)
        animate.duration = 500
        animate.fillAfter = false
        view.startAnimation(animate)
        Handler(Looper.getMainLooper()).postDelayed({
            view.visibility = View.GONE
        }, 500)
    }

    fun slideFromRightToLft(view: View) {
        val animate = TranslateAnimation(view.width.toFloat(), 0f, 0f, 0f)
        animate.duration = 500
        animate.fillAfter = false
        view.startAnimation(animate)
        view.visibility = View.VISIBLE
    }

    fun slideFromLeftToRit(view: View) {
        val animate = TranslateAnimation(0f, view.width.toFloat(), 0f, 0f)
        animate.duration = 500
        animate.fillAfter = false
        view.startAnimation(animate)
        Handler(Looper.getMainLooper()).postDelayed({
            view.visibility = View.GONE
        }, 500)
    }

    fun getCurrentDate(format: String?): String? {
        var formattedDate = ""
        try {
            val c = Calendar.getInstance().time
            println("Current time => $c")
            val df = SimpleDateFormat(format, Locale.getDefault())
            formattedDate = df.format(c)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return formattedDate
    }

    fun animateView(view: View, toVisibility: Int, toAlpha: Float, duration: Int) {
        val show = toVisibility == View.VISIBLE
        if (show) {
            view.alpha = 0f
        }
        view.visibility = View.VISIBLE
        view.animate()
            .setDuration(duration.toLong())
            .alpha(if (show) toAlpha else 0f)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    view.visibility = toVisibility
                }
            })
    }

    /**
     * It will return the random string.
     */
    fun generateRandomString(length: Int): String? {
        val AB = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
        val rnd = SecureRandom()
        val sb = StringBuilder(length)
        for (i in 0 until length) sb.append(AB[rnd.nextInt(AB.length)])
        return sb.toString()
    }

    fun stringToDate(date: String?, dateFormat: String?): Date? {
        var newDate = Date()
        val format = SimpleDateFormat(dateFormat)
        try {
            newDate = format.parse(date)
        } catch (e: ParseException) {
            e.printStackTrace()
        }
        return newDate
    }

    fun dateToString(dateToFormat: Date?, format: String?): String? {
        var dateTime = ""
        val dateFormat = SimpleDateFormat(format)
        dateTime = dateFormat.format(dateToFormat)
        return dateTime
    }

    fun parseTime(date: String?, inFormat: String?, outFormat: String?): String? {
        try {
            val date1 = SimpleDateFormat(inFormat, Locale.getDefault()).parse(date)
            val dateFormat = SimpleDateFormat(outFormat, Locale.getDefault())
            return dateFormat.format(date1)
        } catch (e: ParseException) {
            e.printStackTrace()
        }
        return null
    }

    fun removeLastChar(s: String?): String? {
        return if (s == null || s.isEmpty()) {
            s
        } else s.substring(0, s.length - 1)
    }

    fun calculateTip(total: String, percentage: Int): Double {
        if (isEmpty(total) || percentage == 0) {
            return 0.0
        }
        val finalAmount: Double = try {
            total.toDouble() * percentage / 100
        } catch (e: Exception) {
            e.printStackTrace()
            return 0.0
        }
        return finalAmount
    }

    fun getTimeDifferenceInMinute(startDate: Date, endDate: Date): Int {
        var different = endDate.time - startDate.time
        println("startDate : $startDate")
        println("endDate : $endDate")
        println("different : $different")
        val secondsInMilli: Long = 1000
        val minutesInMilli = secondsInMilli * 60
        val hoursInMilli = minutesInMilli * 60
        val daysInMilli = hoursInMilli * 24
        val elapsedDays = different / daysInMilli
        different = different % daysInMilli
        val elapsedHours = different / hoursInMilli
        different = different % hoursInMilli
        val elapsedMinutes = different / minutesInMilli
        different = different % minutesInMilli
        val elapsedSeconds = different / secondsInMilli
        System.out.printf(
            "%d days, %d hours, %d minutes, %d seconds%n",
            elapsedDays, elapsedHours, elapsedMinutes, elapsedSeconds
        )
        return (elapsedMinutes + elapsedHours.toInt() * 60 + (elapsedDays * 24).toInt() * 60).toInt()
    }

    fun getDayNumberSuffix(day: Int): String? {
        return if (day >= 11 && day <= 13) {
            "th"
        } else when (day % 10) {
            1 -> "st"
            2 -> "nd"
            3 -> "rd"
            else -> "th"
        }
    }

    fun formatInteger(str: String?): String? {
        val parsed = BigDecimal(str)
        val formatter = DecimalFormat("#,###", DecimalFormatSymbols(Locale.US))
        return formatter.format(parsed)
    }

    fun formatDecimal(str: String, MAX_DECIMAL: Int): String? {
        val parsed = BigDecimal(str)
        // example pattern VND #,###.00
        val formatter = DecimalFormat(
            "#,###." + getDecimalPattern(str, MAX_DECIMAL),
            DecimalFormatSymbols(Locale.US)
        )
        formatter.roundingMode = RoundingMode.DOWN
        return formatter.format(parsed)
    }

    /**
     * It will return suitable pattern for format decimal
     * For example: 10.2 -> return 0 | 10.23 -> return 00, | 10.235 -> return 000
     */
    private fun getDecimalPattern(str: String, MAX_DECIMAL: Int): String {
        val decimalCount = str.length - str.indexOf(".") - 1
        val decimalPattern = StringBuilder()
        var i = 0
        while (i < decimalCount && i < MAX_DECIMAL) {
            decimalPattern.append("0")
            i++
        }
        return decimalPattern.toString()
    }

    @SuppressLint("ClickableViewAccessibility")
    fun enableTextViewScroll(view: View) {
        if (view is TextView) {
            view.movementMethod = ScrollingMovementMethod()
        }
        view.setOnTouchListener { v: View, event: MotionEvent ->
            v.parent.requestDisallowInterceptTouchEvent(true)
            if (event.action and MotionEvent.ACTION_MASK == MotionEvent.ACTION_UP) {
                v.parent.requestDisallowInterceptTouchEvent(false)
            }
            false
        }
    }

    fun localToUTC(datesToConvert: String?): Date? {
        var dateToReturn = datesToConvert
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        sdf.timeZone = TimeZone.getDefault()
        var gmt: Date? = null
        val sdfOutPutToSend = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        sdfOutPutToSend.timeZone = TimeZone.getTimeZone("UTC")
        try {
            gmt = sdf.parse(datesToConvert)
            dateToReturn = sdfOutPutToSend.format(gmt)
            Log.e("Local to UTC", dateToReturn)
        } catch (e: ParseException) {
            e.printStackTrace()
        }
        return stringToDate(dateToReturn, "yyyy-MM-dd HH:mm:ss")
    }

    fun commonDocumentDirPath(FolderName: String, context: Context): File {
        var dir: File? = null
        dir = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            File(context.getExternalFilesDir(null).toString() + File.separator + FolderName)
        } else {
            File(Environment.getExternalStorageDirectory().toString() + File.separator + FolderName)
        }
        return dir
    }

    /**
     * Makes a substring of a string bold.
     *
     * @param text       Full text
     * @param textToBold Text you want to make bold
     * @return String with bold substring
     */
    fun makeSectionOfTextBold(text: String, vararg textToBold: String): SpannableStringBuilder? {
        val builder = SpannableStringBuilder(text)
        for (textItem in textToBold) {
            if (textItem.isNotEmpty() && textItem.trim { it <= ' ' } != "") {
                //for counting start/end indexes
                val testText = text.lowercase(Locale.getDefault())
                val testTextToBold = textItem.toLowerCase(Locale.getDefault())
                val startingIndex = testText.indexOf(testTextToBold)
                val endingIndex = startingIndex + testTextToBold.length
                if (startingIndex >= 0 && endingIndex >= 0) {
                    builder.setSpan(StyleSpan(Typeface.BOLD), startingIndex, endingIndex, 0)
                }
            }
        }
        return builder
    }

    fun convertGMTtoLocal(date: String?, receiveFormat: String?): String {
        var convertedDate = ""
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val sdfFinal = SimpleDateFormat(receiveFormat, Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("GMT")
        try {
            val dateObj = sdf.parse(date)
            convertedDate = sdfFinal.format(dateObj)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return convertedDate
    }

    fun convertColorToHex(color: Int): String? {
        return String.format("#%06X", 0xFFFFFF and color)
    }

    fun getLightColor(colorCode: String): String? {
        val hax = colorCode.replace("#", "")
        return "#25$hax"
    }

    fun fadeInAnimation(view: View?) {
        val fadeIn = ObjectAnimator.ofFloat(view, "alpha", .1f, 1f)
        fadeIn.duration = 1000
        fadeIn.start()
    }

    fun isTagValid(tag: String?): Boolean {
        return tag == null || !isEmpty(tag) && tag.length >= 3
    }


    fun getFileNameFromUrl(urlPath: String): String {
        return urlPath.substring(urlPath.lastIndexOf('/') + 1)
    }
    /*
        public static boolean isValidPassword(final String password) {
            Pattern pattern;
            Matcher matcher;
            final String PASSWORD_PATTERN = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{8,}$";
            pattern = Pattern.compile(PASSWORD_PATTERN);
            matcher = pattern.matcher(password);
            return matcher.matches();
        }*/

    /*
    public static boolean isValidPassword(final String password) {
        Pattern pattern;
        Matcher matcher;
        final String PASSWORD_PATTERN = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{8,}$";
        pattern = Pattern.compile(PASSWORD_PATTERN);
        matcher = pattern.matcher(password);
        return matcher.matches();
    }*/
    fun isValidPassword(password: String): Boolean {
        return password.length >= 6
    }

    fun calculateBearing(start: LatLng, end: LatLng): Double {
        val lat1 = Math.toRadians(start.latitude)
        val lon1 = Math.toRadians(start.longitude)
        val lat2 = Math.toRadians(end.latitude)
        val lon2 = Math.toRadians(end.longitude)

        val y = sin(lon2 - lon1) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(lon2 - lon1)

        var bearing = Math.toDegrees(atan2(y, x))
        if (bearing < 0) {
            bearing += 360
        }

        return bearing
    }

    fun getHomeMarker(activity: Activity, latLng: LatLng?): MarkerOptions {
        val marker = bitmapDescriptorFromVector(
            activity,
            R.drawable.ic_marker_home
        )
        return MarkerOptions().position(LatLng(latLng?.latitude!!, latLng.longitude))
            .title("Destination")
            .icon(marker)
    }

    fun getCurrentMarker(activity: Activity, latLng: LatLng?): MarkerOptions {
        val marker = bitmapDescriptorFromVector(
            activity,
            R.drawable.ic_marker_current
        )
        return MarkerOptions().position(LatLng(latLng?.latitude!!, latLng.longitude))
            .title("Current")
            .icon(marker)
    }


}