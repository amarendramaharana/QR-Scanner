package com.ekyc.qrscanner;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.media.Image;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCanceledListener;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.BarcodeDetector;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    public static final int CAMERA_CODE = 110;
    private PreviewView previewView;
    private boolean isBack = true;
    private boolean isFlashOn = false;
    private CameraManager cameraManager;
    private LinearLayout pgLayout;
    private ImageAnalysis imageAnalysis;
    private ActivityResultLauncher<Intent> imageLauncher;
    private BarcodeScannerOptions options;
    private ExecutorService cameraExecutor;
    String cameraId;
    private ImageButton btnFlash;

    private Camera camera;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        viewInitialize();
        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera(CameraSelector.DEFAULT_BACK_CAMERA);
        } else {
            requestCameraPermissions();
        }
        onClickListener();
        registerImageLauncher();
        options = new BarcodeScannerOptions.Builder()
                .setBarcodeFormats(
                        Barcode.FORMAT_ALL_FORMATS)
                .build();
        // Initialize camera executor
/*        1. Camera Frame Processing is Sequential
        Camera frames are generated sequentially, and you process one frame at a time.
                Using a single-threaded executor ensures that only one frame is processed at any given moment, avoiding race conditions or out-of-order processing.
                For example, if frame 2 starts processing before frame 1 is finished, it could lead to incorrect results or crashes.*/
        cameraExecutor = Executors.newSingleThreadExecutor();

    }

    private void registerImageLauncher() {
        imageLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getData() != null) {
                Uri uri = result.getData().getData();
                InputImage image;
                try {
                    image = InputImage.fromFilePath(this, uri);
                    extractQrValueFromImage(image);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                imageAnalysis.setAnalyzer(cameraExecutor, this::analyzeImage);
            }
        });
    }

    private void extractQrValue(InputImage image, ImageProxy imageProxy) {
        BarcodeScanner scanner = BarcodeScanning.getClient(options);
        scanner.process(image).addOnSuccessListener(cameraExecutor, barcodes -> {
            if (barcodes.isEmpty()) {
                // No barcodes detected in this frame
                imageProxy.close(); // Close the imageProxy to avoid memory leaks
                return;
            }
            String value = barcodes.get(0).getRawValue();
            runOnUiThread(() -> {
                if (imageAnalysis != null) {
                    imageAnalysis.clearAnalyzer(); // Stop further analysis
                }
                pgLayout.setVisibility(View.GONE);
                Intent intent = new Intent(MainActivity.this, ResultActivity.class);
                intent.putExtra("QR_VALUE", value);
                startActivity(intent);
            });
            imageProxy.close(); // Close the imageProxy to avoid memory leaks
        }).addOnFailureListener(cameraExecutor, e -> {
            showException("Error-" + e.getMessage());
            imageProxy.close(); // Close the imageProxy to avoid memory leaks

        }).addOnCanceledListener(cameraExecutor, () -> {
            showException("Scanning Canceled");
            imageProxy.close(); // Close the imageProxy to avoid memory leaks

        });
    }

    private void extractQrValueFromImage(InputImage image) {
        pgLayout.setVisibility(View.VISIBLE);
        BarcodeScanner scanner = BarcodeScanning.getClient(options);

        scanner.process(image).addOnSuccessListener(cameraExecutor, barcodes -> {
            if (barcodes.isEmpty()) {
                showQrException("Qr code might be blur or Invalid ");
                return;
            }

            String value = barcodes.get(0).getRawValue();
            runOnUiThread(() -> {
                if (imageAnalysis != null) {
                    imageAnalysis.clearAnalyzer(); // Stop further analysis
                }
                pgLayout.setVisibility(View.GONE);
                Intent intent = new Intent(MainActivity.this, ResultActivity.class);
                intent.putExtra("QR_VALUE", value);
                startActivity(intent);
            });
        }).addOnFailureListener(cameraExecutor, e -> {
            ;
            showQrException("Error-" + e.getMessage());

        }).addOnCanceledListener(cameraExecutor, () -> {
            showQrException("Scanning Canceled");

        });
    }

    private void showException(String exception) {
        runOnUiThread(() -> {
            imageAnalysis.setAnalyzer(cameraExecutor, this::analyzeImage);//as exception occur we allow user to scan qr code ,as we stop when  user click to pick image
            Toast.makeText(MainActivity.this, exception, Toast.LENGTH_SHORT).show();

        });
    }

    private void showQrException(String exception) {
        runOnUiThread(() -> {
            pgLayout.setVisibility(View.GONE);
            imageAnalysis.setAnalyzer(cameraExecutor, this::analyzeImage);//as exception occur we allow user to scan qr code ,as we stop when  user click to pick image
            Toast.makeText(MainActivity.this, exception, Toast.LENGTH_SHORT).show();

        });
    }

    private void onClickListener() {
        //Camera Switch
        findViewById(R.id.btnCameraSwitch).setOnClickListener(view -> {
            if (isBack) {
                startCamera(CameraSelector.DEFAULT_FRONT_CAMERA);
                isBack = false;
                btnFlash.setVisibility(View.GONE);

            } else {
                startCamera(CameraSelector.DEFAULT_BACK_CAMERA);
                isBack = true;
                btnFlash.setVisibility(View.VISIBLE);
            }

        });

        //flash On or Off
        btnFlash.setOnClickListener(view -> {
            if (isFlashOn) {
                toggleFlashlight(false);

            } else {
                toggleFlashlight(true);
            }

        });

        //pick Image from gallery
        findViewById(R.id.btnPickIamge).setOnClickListener(view -> {
            Intent intent = new Intent(MediaStore.ACTION_PICK_IMAGES);
            imageLauncher.launch(intent);
            imageAnalysis.clearAnalyzer();//because as user is trying to upload image no need to scan qr code(stop the analysis iamge)
        });
    }

    private void toggleFlashlight(boolean enable) {
        if (camera != null) {
            camera.getCameraControl().enableTorch(enable);  // Enable or disable the torch
        } else {
            Toast.makeText(this, "Camera not ready yet", Toast.LENGTH_SHORT).show();
        }
        if (enable) {
            btnFlash.setBackgroundResource(R.drawable.flash_off_24px);
        } else {
            btnFlash.setBackgroundResource(R.drawable.flash_on_24px);

        }
        isFlashOn = !isFlashOn;
    }

    private void viewInitialize() {
        previewView = findViewById(R.id.viewFinder);
        pgLayout = findViewById(R.id.progressLayout);
        btnFlash = findViewById(R.id.btnFlash);
    }

    private void requestCameraPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA, Manifest.permission.CAMERA}, CAMERA_CODE);
    }


    private void startCamera(CameraSelector cameraSelector) {
    /*ProcessCameraProvider. This is used to bind the lifecycle of cameras to the lifecycle owner. This eliminates the task of opening
        and closing the camera since CameraX is lifecycle-aware.*/
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                // Unbind all use cases before rebinding
                cameraProvider.unbindAll();

                // Preview
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());
                // Create an image analysis use case
                imageAnalysis =
                        new ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build();

                imageAnalysis.setAnalyzer(cameraExecutor, this::analyzeImage);


                // Bind use cases to camera
                camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);

            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void analyzeImage(ImageProxy imageProxy) {
/*        ImageProxy is a class provided by Android's CameraX library, representing an image frame from the camera.
        It serves as a bridge between the camera and image processing libraries (such as ML Kit) and allows you to access and process image data in real-time.*/


        @SuppressWarnings("UnsafeOptInUsageError")
        InputImage image =
                InputImage.fromMediaImage(imageProxy.getImage(), imageProxy.getImageInfo().getRotationDegrees());

        extractQrValue(image, imageProxy);
    }

    private boolean allPermissionsGranted() {
        return ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera(CameraSelector.DEFAULT_BACK_CAMERA);
            } else {
                Toast.makeText(this, "Camera permission denied. Enable it in settings to use this feature.", Toast.LENGTH_LONG).show();

            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }
}