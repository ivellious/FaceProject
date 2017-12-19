package com.michalpomiecko.faceproject;

import android.Manifest;
import android.arch.persistence.room.Room;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.google.android.gms.vision.face.Landmark;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FaceProjectMainActivity extends AppCompatActivity implements View.OnClickListener {

    static final int REQUEST_TAKE_PHOTO = 101;

    private int CAMERA_PERMISSION = 1;
    private int READ_EXTERNAL_STORAGE_CODE = 2;
    private int WRITE_EXTERNAL_STORAGE_CODE = 3;
    private final static int ADD_FACE = 1;
    private final static int VERIFY_FACE = 2;

    private Button addFaceButton;
    private TextView addFaceTxt;
    private Button verifyFaceButton;
    private TextView verifyFaceTxt;
    private ImageView mImageView;
    private TextView fnameView;
    private TextView lnameView;
    private EditText fnameEdit;
    private EditText lnameEdit;
    private Button addUserBtn;
    private AppDatabase db;
    private List<DataPoint> pointsList;
    private int photoMode = 0;

    @Override
    protected void onResume() {
        super.onResume();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION);
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
        addFaceButton = (Button) findViewById(R.id.addFaceButton);
        addFaceTxt = (TextView) findViewById(R.id.addFaceTxt);
        verifyFaceButton = (Button) findViewById(R.id.verifyFaceButton);
        verifyFaceTxt = (TextView) findViewById(R.id.verifyFaceTxt);
        mImageView = (ImageView) findViewById(R.id.firstImageView);
        fnameView = (TextView) findViewById(R.id.fnameView);
        lnameView = (TextView) findViewById(R.id.lnameView);
        fnameEdit = (EditText) findViewById(R.id.fnameEdit);
        lnameEdit = (EditText) findViewById(R.id.lnameEdit);
        addUserBtn = (Button) findViewById(R.id.addUserBtn);

        addFaceButton.setOnClickListener(this);
        addUserBtn.setOnClickListener(this);
        verifyFaceButton.setOnClickListener(this);

        this.db = Room.databaseBuilder(getApplicationContext(),
                AppDatabase.class, "userDB").allowMainThreadQueries().build();

    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.addFaceButton:
                addFace();
                break;
            case R.id.addUserBtn:
                addUser();
                break;
            case R.id.verifyFaceButton:
                verifyFace();
                break;
            default:
                break;
        }
    }

    private void addFace() {
        photoMode = ADD_FACE;
        dispatchTakePictureIntent();
    }

    private void verifyFace() {
        photoMode = VERIFY_FACE;
        dispatchTakePictureIntent();
    }

    private String ratiosToString(List<Double> ratiosList) {
        Double[] ratios = ratiosList.toArray(new Double[ratiosList.size()]);
        return Arrays.toString(ratios);
    }

    private void addUser() {
        String fname = fnameEdit.getText().toString().trim();
        String lname = lnameEdit.getText().toString().trim();
        if (fname.isEmpty() || lname.isEmpty()) {
            Toast.makeText(this, "Podaj imię i nazwisko!", Toast.LENGTH_SHORT).show();
            return;
        }
        List<Double> ratios = calculateRatios(pointsList);
        String featureVector = ratiosToString(ratios);
        User user = new User();
        user.setFirstName(fname);
        user.setLastName(lname);
        user.setFeatureVector(featureVector);
        this.db.userDao().insertAll(user);
        user = this.db.userDao().findByName(user.getFirstName(), user.getLastName());
        if (user.getFirstName().equals(fname) && user.getLastName().equals(lname) && user.getUid() != 0) {
            String text = "Dodano użytkownika " + user.getUid() + " " + user.getFirstName() + " "
                    + user.getLastName();
            Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Wystąpił błąd :(", Toast.LENGTH_SHORT).show();
        }
        fnameEdit.setText("");
        lnameEdit.setText("");
        hideAddMenu();
        showMainMenu();
    }

    private void showMainMenu() {
        addFaceButton.setVisibility(View.VISIBLE);
        addFaceTxt.setVisibility(View.VISIBLE);
        verifyFaceButton.setVisibility(View.VISIBLE);
        verifyFaceTxt.setVisibility(View.VISIBLE);
    }

    private void hideMainMenu() {
        addFaceButton.setVisibility(View.GONE);
        addFaceTxt.setVisibility(View.GONE);
        verifyFaceButton.setVisibility(View.GONE);
        verifyFaceTxt.setVisibility(View.GONE);
    }

    private void showAddMenu() {
        mImageView.setVisibility(View.VISIBLE);
        fnameView.setVisibility(View.VISIBLE);
        lnameView.setVisibility(View.VISIBLE);
        fnameEdit.setVisibility(View.VISIBLE);
        lnameView.setVisibility(View.VISIBLE);
        lnameEdit.setVisibility(View.VISIBLE);
        addUserBtn.setVisibility(View.VISIBLE);
    }

    private void hideAddMenu() {
        mImageView.setVisibility(View.GONE);
        fnameView.setVisibility(View.GONE);
        lnameView.setVisibility(View.GONE);
        fnameEdit.setVisibility(View.GONE);
        lnameView.setVisibility(View.GONE);
        lnameEdit.setVisibility(View.GONE);
        addUserBtn.setVisibility(View.GONE);
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");

            if (!verifyLandmarksPresence(imageBitmap)) {
                return;
            }

            Toast.makeText(this, "Zdjęcie wykonano poprawnie", Toast.LENGTH_SHORT).show();

            switch (photoMode) {
                case ADD_FACE:
                    displayBitMap(imageBitmap);
                    hideMainMenu();
                    showAddMenu();
                    break;
                case VERIFY_FACE:
                    verifyUser();
                    break;
                default:
                    break;
            }
            photoMode = 0;
        }
    }

    private void displayBitMap(Bitmap imageBitmap) {

        Bitmap bitmap = imageBitmap.copy(Bitmap.Config.ARGB_8888, true);
        drawCircles(bitmap);

        mImageView.setImageBitmap(bitmap);
    }

    private boolean verifyLandmarksPresence(Bitmap bMap) {
        FaceDetector detector = new FaceDetector.Builder(this)
                .setTrackingEnabled(false)
                .setLandmarkType(FaceDetector.ALL_LANDMARKS)
                .build();
        Frame frame = new Frame.Builder().setBitmap(bMap).build();

        pointsList = new ArrayList<>();
        SparseArray<Face> faces = detector.detect(frame);
        if (faces.size() == 0) {
            Toast.makeText(this, "Nie wykryto twarzy! Spróbuj ponownie", Toast.LENGTH_SHORT).show();
            Log.e("Error:", "No face detected!");
            return false;
        }
        if (faces.size() > 1) {
            Toast.makeText(this, "Więcej niż jedna osoba na zdjęciu! Spróbuj ponownie.", Toast.LENGTH_SHORT).show();
            Log.e("Error:", "More than one face detected!");
            return false;
        }
        Face face = faces.valueAt(0);
        List<Landmark> landmarks = face.getLandmarks();
        if (landmarks.size() < 8) {
            Toast.makeText(this, "Za mało punktów charakterystycznych! Spróbuj ponownie.", Toast.LENGTH_SHORT).show();
            Log.e("Error:", "Not enough points detected!");
            return false;
        }

        for (Landmark landmark : landmarks) {
            int x = (int) landmark.getPosition().x;
            int y = (int) landmark.getPosition().y;
            pointsList.add(new DataPoint(x, y));
        }

        detector.release();
        return true;
    }

    private double squareError(List<Double> ratios, User user) {
        try {
            JSONArray uRatios = new JSONArray(user.getFeatureVector());
            Double se = 0.0;
            Double sum = 0.0;
            for (int i = 0; i < ratios.size(); i++) {
                sum += ratios.get(i);
                se += Math.pow(ratios.get(i) - uRatios.getDouble(i), 2);
            }
            Log.e("User: ", user.getFirstName() + " " + user.getLastName());
            Log.e("Ratio sum: ", sum.toString());
            Log.e("SquareError", se.toString());
            return se;
        } catch (Exception e) {
            Log.e("Error:", e.getMessage());
        }
        return 1e6;
    }

    private void verifyUser() {
        List<User> users = this.db.userDao().getAll();
        List<Double> ratios = calculateRatios(pointsList);
        User bestFitUser = users.get(0);
        Double bestFit = squareError(ratios, bestFitUser);
        for (User user : users.subList(1, users.size())) {
            Double se = squareError(ratios, user);
            if (se < bestFit) {
                bestFit = se;
                bestFitUser = user;
            }
        }
        String text = "Rozpoznano: " + bestFitUser.getFirstName() + " " + bestFitUser.getLastName();
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    private Double pointsDistance(DataPoint a, DataPoint b) {
        int dx = a.getX() - b.getX();
        int dy = a.getY() - b.getY();
        double dx2 = Math.pow(dx, 2);
        double dy2 = Math.pow(dy, 2);
        Double distance = Math.sqrt(dx2 + dy2);
        return distance;
    }

    private List<Double> calculateRatios(List<DataPoint> dataPoints) {
        List<Double> distances = new ArrayList<Double>();
        List<Double> ratios = new ArrayList<Double>();
        // Distances
        for (int i = 0; i < dataPoints.size(); i++) {
            for (int j = i + 1; j < dataPoints.size(); j++) {
                if (i != j) {
                    Double distance = pointsDistance(dataPoints.get(i), dataPoints.get(j));
                    distances.add(distance);
                }
            }
        }
        // Ratios
        for (int i = 0; i < distances.size(); i++) {
            for (int j = i + 1; j < distances.size(); j++) {
                if (i != j) {
                    Double ratio = distances.get(i) / distances.get(j);
                    ratios.add(ratio);
                }
            }
        }
        return ratios;
    }

    private void drawCircles(Bitmap image) {
        Canvas canvas = new Canvas(image);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(Color.RED);
        for (DataPoint dataP : pointsList) {
            canvas.drawCircle(dataP.getX(), dataP.getY(), 2, paint);
        }
    }

    class DataPoint {
        int x, y;

        private DataPoint(int x, int y) {
            this.x = x;
            this.y = y;
        }

        private int getX() {
            return x;
        }

        private int getY() {
            return y;
        }
    }
}
