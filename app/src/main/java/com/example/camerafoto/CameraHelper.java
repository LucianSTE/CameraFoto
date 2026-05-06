package com.example.camerafoto;

import android.content.ContentValues;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class CameraHelper {

    public interface PhotoCallback {
        void onPhotoSaved(Uri uri);
        void onError(String message);
    }

    private final MainActivity activity;
    private final PreviewView previewView;
    private ImageCapture imageCapture;

    public CameraHelper(MainActivity activity, PreviewView previewView) {
        this.activity = activity;
        this.previewView = previewView;
    }

    public void startCamera() {
        ListenableFuture<ProcessCameraProvider> future =
                ProcessCameraProvider.getInstance(activity);

        future.addListener(() -> {
            try {
                ProcessCameraProvider provider = future.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build();

                CameraSelector selector = CameraSelector.DEFAULT_BACK_CAMERA;

                provider.unbindAll();
                provider.bindToLifecycle(
                        (LifecycleOwner) activity,
                        selector,
                        preview,
                        imageCapture
                );
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(activity));
    }

    public void takePhoto(PhotoCallback callback) {
        if (imageCapture == null) {
            callback.onError("Camera nu este pregatita.");
            return;
        }

        String photoName = "photo_" +
                new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                        .format(System.currentTimeMillis()) + ".jpg";

        ImageCapture.OutputFileOptions options;
        File photoFile = null;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, photoName);
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
            values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/AparatFoto");

            options = new ImageCapture.OutputFileOptions.Builder(
                    activity.getContentResolver(),
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    values
            ).build();
        } else {
            File picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
            File appDir = new File(picturesDir, "AparatFoto");
            if (!appDir.exists() && !appDir.mkdirs()) {
                callback.onError("Nu pot crea folderul pentru poze.");
                return;
            }

            photoFile = new File(appDir, photoName);
            options = new ImageCapture.OutputFileOptions.Builder(photoFile).build();
        }

        File savedFile = photoFile;
        imageCapture.takePicture(
                options,
                ContextCompat.getMainExecutor(activity),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(ImageCapture.OutputFileResults results) {
                        if (savedFile != null) {
                            MediaScannerConnection.scanFile(
                                    activity,
                                    new String[]{savedFile.getAbsolutePath()},
                                    new String[]{"image/jpeg"},
                                    null
                            );
                            callback.onPhotoSaved(Uri.fromFile(savedFile));
                            return;
                        }

                        Uri savedUri = results.getSavedUri();
                        if (savedUri != null) {
                            callback.onPhotoSaved(savedUri);
                        } else {
                            callback.onError("Imagine salvata, dar nu am putut obtine adresa ei.");
                        }
                    }

                    @Override
                    public void onError(ImageCaptureException exception) {
                        callback.onError("Eroare la salvarea imaginii.");
                    }
                }
        );
    }
}
