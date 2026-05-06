package com.example.camerafoto;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private PreviewView previewView;
    private ImageView imageView;
    private Button btnCapture, btnEffect;

    private CameraHelper cameraHelper;
    private Bitmap originalBitmap;

    private ActivityResultLauncher<String> cameraPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        previewView = findViewById(R.id.previewView);
        imageView = findViewById(R.id.imageView);
        btnCapture = findViewById(R.id.btnCapture);
        btnEffect = findViewById(R.id.btnEffect);

        cameraHelper = new CameraHelper(this, previewView);

        cameraPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        cameraHelper.startCamera();
                    } else {
                        Toast.makeText(this, "Permisiunea pentru camera este necesara.", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            cameraHelper.startCamera();
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }

        btnCapture.setOnClickListener(v -> fotografiere());

        btnEffect.setOnClickListener(v -> aplicaEfectAlbNegru());
    }

    private void fotografiere() {
        cameraHelper.takePhoto(new CameraHelper.PhotoCallback() {
            @Override
            public void onPhotoSaved(Uri uri) {
                try {
                    originalBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                    imageView.setImageBitmap(originalBitmap);

                    Toast.makeText(MainActivity.this, "Imagine salvata.", Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "Imagine salvata, dar nu poate fi afisata.", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onError(String message) {
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void aplicaEfectAlbNegru() {
        Bitmap bitmapAlbNegru = transformaInAlbNegru(originalBitmap);
        imageView.setImageBitmap(bitmapAlbNegru);
        Toast.makeText(this, "Efect alb-negru aplicat.", Toast.LENGTH_LONG).show();
    }

    private Bitmap transformaInAlbNegru(Bitmap bitmapOriginal) {
        Bitmap bitmapNou = Bitmap.createBitmap(
                bitmapOriginal.getWidth(),
                bitmapOriginal.getHeight(),
                Bitmap.Config.ARGB_8888
        );

        Canvas canvas = new Canvas(bitmapNou);
        Paint paint = new Paint();

        ColorMatrix colorMatrix = new ColorMatrix();
        colorMatrix.setSaturation(0);

        paint.setColorFilter(new ColorMatrixColorFilter(colorMatrix));
        canvas.drawBitmap(bitmapOriginal, 0, 0, paint);

        return bitmapNou;
    }
}