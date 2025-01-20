package com.ekyc.qrscanner;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ResultActivity extends AppCompatActivity {
    private String qrValue;
    private TextView result;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);
        result = findViewById(R.id.txtResult);
        showResult();
        generateQrAndShowInUi();
        findViewById(R.id.btnScanAgain).setOnClickListener(view -> {
       onBackPressed();
        });
        if (isValidURL(qrValue)) {
            result.setTextColor(Color.BLUE);
            result.setTypeface(Typeface.DEFAULT_BOLD);
        } else {
            result.setTextColor(Color.BLACK);
            result.setTypeface(Typeface.DEFAULT);
        }
        result.setOnClickListener(view -> {
            if (isValidURL(result.getText().toString())) {
                Intent intent = new Intent(this, WebViewActivity.class);
                intent.putExtra("URL", result.getText().toString());
                startActivity(intent);

            }
        });
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    public static boolean isValidURL(String url) {
        // Regex to check valid URL
        String regex = "((http|https)://)(www.)?"
                + "[a-zA-Z0-9@:%._\\+~#?&//=]"
                + "{2,256}\\.[a-z]"
                + "{2,6}\\b([-a-zA-Z0-9@:%"
                + "._\\+~#?&//=]*)";

        // Compile the ReGex
        Pattern p = Pattern.compile(regex);

        // If the string is empty
        // return false
        if (url == null) {
            return false;
        }

        // Find match between given string
        // and regular expression
        // using Pattern.matcher()
        Matcher m = p.matcher(url);

        // Return if the string
        // matched the ReGex
        return m.matches();
    }

    private void showResult() {
        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            if (bundle.containsKey("QR_VALUE")) {
                qrValue = bundle.getString("QR_VALUE");
                ((TextView) findViewById(R.id.txtResult)).setText(qrValue);
            }
        }
    }

    private void generateQrAndShowInUi() {
        BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
        try {
            Bitmap qrBitmap = barcodeEncoder.encodeBitmap(qrValue, BarcodeFormat.QR_CODE, 200, 200);
            ((ImageView) findViewById(R.id.imgQr)).setImageBitmap(qrBitmap);

        } catch (WriterException e) {
            Log.d("Exception", "generateQrAndShowInUi: " + e.getMessage());
        }
    }
}