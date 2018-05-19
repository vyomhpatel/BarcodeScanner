package b12app.vyom.com.barcodescanner;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.PersistableBundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import org.w3c.dom.Text;

import java.io.File;
import java.io.FileNotFoundException;

public class ScannerActivity extends AppCompatActivity implements View.OnClickListener {

    public static final String IMAGE_URI = "image_uri";
    public static final String SCAN_RESULT = "scan_result";
    public static final String BARCODE_SCANNER_COULD_NOT_SET_UP_AUTOMATICALLY = "barcode scanner could not set up automatically";
    public static final int REQUEST_CODE = 007;
    public static final int PERMISSION_REQUEST_CODE = 1;
    public static final String NO_BARCODE_FOUND = "No Barcode Found!";
    public static final String PICTURE_JPG = "picture.jpg";
    public static final String PROVIDER = ".provider";
    public static final String PERMISSION_DENIED = "Permission Denied!";
    private ImageView image;
    private TextView scan_header, scan_results;
    private Button btnCapture;
    private Uri imageUri;
    private BarcodeDetector barcodeDetector;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        if (imageUri != null) {
            outPersistentState.putString(IMAGE_URI, imageUri.toString());
            outPersistentState.putString(SCAN_RESULT, scan_results.getText().toString());
        }
        super.onSaveInstanceState(outState, outPersistentState);

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanner);

        //initializing views.
        initViews();

        //retrieving data from savedInstanceState to set image and set the text.
        if(savedInstanceState!=null){

            imageUri = Uri.parse(savedInstanceState.getString(IMAGE_URI));
            scan_results.setText(savedInstanceState.getString(SCAN_RESULT));
        }

        btnCapture.setOnClickListener(this);

        //initializing barcode detector using builder pattern.
        barcodeDetector = new BarcodeDetector.Builder(getApplicationContext())
                                .setBarcodeFormats(Barcode.DATA_MATRIX | Barcode.QR_CODE)
                                .build();

        //checking if barcode detector has been properly initialized.
        if(!barcodeDetector.isOperational()){
            scan_results.setText(BARCODE_SCANNER_COULD_NOT_SET_UP_AUTOMATICALLY);
            return;
        }

    }

    private void initViews() {
        btnCapture = findViewById(R.id.btnCapture);
        image = findViewById(R.id.image);
        scan_header = findViewById(R.id.scan_header);
        scan_results = findViewById(R.id.scan_results);
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()){
            case R.id.btnCapture:
                //requesting permissions for writing to external storage.
                ActivityCompat.requestPermissions(ScannerActivity.this, new
                        String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE:
                //if permission granted then capturing image using camera.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    captureBarcode();
                } else {
                    Toast.makeText(ScannerActivity.this, PERMISSION_DENIED, Toast.LENGTH_SHORT).show();
                }
        }
    }

    private void captureBarcode() {
        //sending intent to camera app to capture the image and return the bitmap.
        Intent imageIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File picture = new File(Environment.getExternalStorageDirectory(), PICTURE_JPG);
        imageUri = FileProvider.getUriForFile(this,BuildConfig.APPLICATION_ID+ PROVIDER,picture);
        imageIntent.putExtra(MediaStore.EXTRA_OUTPUT,imageUri);
        startActivityForResult(imageIntent, REQUEST_CODE);

    }
    private void launchMediaScanIntent() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        mediaScanIntent.setData(imageUri);
        this.sendBroadcast(mediaScanIntent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==REQUEST_CODE && resultCode == RESULT_OK){
            launchMediaScanIntent();

            Bitmap bitmap = null;
            try {
                bitmap = decodeBitMapUri(this,imageUri);


            if(barcodeDetector.isOperational() && bitmap != null){
                Frame frame =  new Frame.Builder().setBitmap(bitmap).build();

                SparseArray<Barcode> barcodes = barcodeDetector.detect(frame);
                for(int index = 0; index < barcodes.size(); index++){
                    Barcode barcode = barcodes.valueAt(index);
                    scan_results.setText(barcode.displayValue);
                }

                if(barcodes.size()==0){
                    scan_results.setText(NO_BARCODE_FOUND);
                }
            } else {
                scan_results.setText(BARCODE_SCANNER_COULD_NOT_SET_UP_AUTOMATICALLY);
            }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }


    }

    private Bitmap decodeBitMapUri(Context context, Uri imageUri) throws FileNotFoundException {
        int targetWidth = 600;
        int targetHeight = 600;

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(context.getContentResolver().openInputStream(imageUri),null,options);
        int photoWidth = options.outWidth;
        int photoHeight = options.outHeight;

        int scaleFactor = Math.min(photoHeight/targetHeight,photoWidth/targetWidth);
        options.inJustDecodeBounds = false;
        options.inSampleSize = scaleFactor;

        return BitmapFactory.decodeStream(context.getContentResolver().openInputStream(imageUri),null,options);
    }


}
