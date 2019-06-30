package com.example.angel_hack_2;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.util.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Upload extends AppCompatActivity implements View.OnClickListener {
    private final String KEY = "AKIATGLNTNC2PZ2NRWFS";
    private final String SECRET = "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";

    private AmazonS3Client s3Client;
    private BasicAWSCredentials credentials;

    //track Choosing Image Intent
    private static final int CHOOSING_IMAGE_REQUEST = 1234;

    private TextView tvFileName;
    private ImageView imageView;
    private EditText edtFileName;

    private Uri fileUri;
    private Bitmap bitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload);


        edtFileName = findViewById(R.id.edt_file_name);
        tvFileName = findViewById(R.id.tv_file_name);
        tvFileName.setText("");

        findViewById(R.id.btn_choose_file).setOnClickListener(this);
        findViewById(R.id.upload).setOnClickListener(this);

        AWSMobileClient.getInstance().initialize(this).execute();

        credentials = new BasicAWSCredentials(KEY, SECRET);
        s3Client = new AmazonS3Client(credentials);
    }
    private void start(){
        Intent intent = new Intent(this, Information.class);
        startActivity(intent);
    }
    private void uploadFile() {

        if (fileUri != null) {
            final String fileName = edtFileName.getText().toString();

            if (!validateInputFileName(fileName)) {
                return;
            }

            final File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                    "/" + fileName);

            createFile(getApplicationContext(), fileUri, file);

            TransferUtility transferUtility =
                    TransferUtility.builder()
                            .context(getApplicationContext())
                            .awsConfiguration(AWSMobileClient.getInstance().getConfiguration())
                            .s3Client(s3Client)
                            .build();

            TransferObserver uploadObserver =
                    transferUtility.upload("jsaS3/" + fileName + "." + getFileExtension(fileUri), file);


            uploadObserver.setTransferListener(new TransferListener() {

                @Override
                public void onStateChanged(int id, TransferState state) {
                    if (TransferState.COMPLETED == state) {
                        Toast.makeText(getApplicationContext(), "Upload Completed!", Toast.LENGTH_SHORT).show();

                        file.delete();
                    } else if (TransferState.FAILED == state) {
                        file.delete();
                    }
                }

                @Override
                public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                    float percentDonef = ((float) bytesCurrent / (float) bytesTotal) * 100;
                    int percentDone = (int) percentDonef;

                    tvFileName.setText("ID:" + id + "|bytesCurrent: " + bytesCurrent + "|bytesTotal: " + bytesTotal + "|" + percentDone + "%");
                    if(percentDone == 100){
                        start();
                    }
                }



                @Override
                public void onError(int id, Exception ex) {
                    ex.printStackTrace();
                }

            });
        }
    }

    @Override
    public void onClick(View view) {
        int i = view.getId();

        if (i == R.id.btn_choose_file) {
            showChoosingFile();
        } else if (i == R.id.upload) {
            uploadFile();
        }
    }

    private void showChoosingFile() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Image"), CHOOSING_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (bitmap != null) {
            bitmap.recycle();
        }

        if (requestCode == CHOOSING_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            fileUri = data.getData();
            try {
                bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), fileUri);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private String getFileExtension(Uri uri) {
        ContentResolver contentResolver = getContentResolver();
        MimeTypeMap mime = MimeTypeMap.getSingleton();

        return mime.getExtensionFromMimeType(contentResolver.getType(uri));
    }

    private boolean validateInputFileName(String fileName) {

        if (TextUtils.isEmpty(fileName)) {
            Toast.makeText(this, "Enter file name!", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void createFile(Context context, Uri srcUri, File dstFile) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(srcUri);
            if (inputStream == null) return;
            OutputStream outputStream = new FileOutputStream(dstFile);
            IOUtils.copy(inputStream, outputStream);
            inputStream.close();
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
