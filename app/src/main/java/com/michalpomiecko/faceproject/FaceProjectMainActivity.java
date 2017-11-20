package com.michalpomiecko.faceproject;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.google.android.gms.vision.face.Landmark;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class FaceProjectMainActivity extends AppCompatActivity {

    static final int REQUEST_TAKE_PHOTO = 101;

    private int CAMERA_PERMISSION = 1;
    private int READ_EXTERNAL_STORAGE_CODE = 2;
    private int WRITE_EXTERNAL_STORAGE_CODE = 3;


    private Button takeFacePhotoButton;
    private ImageView mImageView;

    @Override
    protected void onResume() {
        super.onResume();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION);
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    READ_EXTERNAL_STORAGE_CODE);
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    WRITE_EXTERNAL_STORAGE_CODE);
        }


    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread paramThread, Throwable paramThrowable) {
                Log.e("Alert", "Lets See if it Works !!!", paramThrowable);
            }
        });
        setContentView(R.layout.activity_face_project_main);
        takeFacePhotoButton = (Button) findViewById(R.id.addFaceButton);
        mImageView = (ImageView) findViewById(R.id.firstImageView);


        takeFacePhotoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                dispatchTakePictureIntent();
            }
        });
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Log.e(FaceProjectMainActivity.class.getSimpleName(), "IO Error", ex);

            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.michalpomiecko.faceproject.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }

    String mCurrentPhotoPath;

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == RESULT_OK) {
            Toast.makeText(this, "OK", Toast.LENGTH_SHORT).show();
            galleryAddPic();
            displayBitMap();
        }
    }

    private void displayBitMap() {


           Bitmap bMap = BitmapFactory.decodeFile(mCurrentPhotoPath);
            //   Bitmap bMapCopy = bMap.copy(Bitmap.Config.ARGB_8888, true);

            FaceDetector detector = new FaceDetector.Builder(this)
                    .setTrackingEnabled(false)
                    .setLandmarkType(FaceDetector.ALL_LANDMARKS)
                    .build();
            Frame frame = new Frame.Builder().setBitmap(bMap).build();
//        Canvas canvas = new Canvas(bMapCopy);
//        Paint paint = new Paint();
//        paint.setColor(Color.BLUE);
            List<DataPoint> pointsList = new ArrayList<>();
            SparseArray<Face> faces = detector.detect(frame);
            for (int i = 0; i < faces.size(); ++i) {
                Face face = faces.valueAt(i);
                for (Landmark landmark : face.getLandmarks()) {
                    int cx = (int) (landmark.getPosition().x);
                    int cy = (int) (landmark.getPosition().y);
//                canvas.drawCircle(cx,cy,10, paint);
                    pointsList.add(new DataPoint(cx,cy));

                    Log.e("Landmark from face: " + i, ", Coordinate: x: " + cx + "  y:" + cy);
                }
            }

           Bitmap bitmap = bMap.copy(Bitmap.Config.ARGB_8888, true);
            drawCircles(pointsList, bitmap);

            mImageView.setImageBitmap(bitmap);


//        paint.setAntiAlias(true);
    }

    private void drawCircles(List<DataPoint> dataPoints, Bitmap image) {
                Canvas canvas = new Canvas(image);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(Color.RED);
        for (DataPoint dataP: dataPoints) {
            canvas.drawCircle(dataP.getX(),dataP.getY(), 20, paint);
        }
    }

    class DataPoint {
        int x,y;
        public DataPoint(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public int getX() {
            return x;
        }

        public void setX(int x) {
            this.x = x;
        }

        public int getY() {
            return y;
        }

        public void setY(int y) {
            this.y = y;
        }
    }

    private void galleryAddPic() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(mCurrentPhotoPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
    }


}
