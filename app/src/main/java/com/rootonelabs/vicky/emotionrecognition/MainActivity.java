package com.rootonelabs.vicky.emotionrecognition;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.microsoft.projectoxford.emotion.EmotionServiceClient;
import com.microsoft.projectoxford.emotion.EmotionServiceRestClient;
import com.microsoft.projectoxford.emotion.contract.RecognizeResult;
import com.microsoft.projectoxford.emotion.contract.Scores;
import com.microsoft.projectoxford.emotion.rest.EmotionServiceException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    ImageView imageView;
    Button btnTakePicture, btnProcess;
    int TAKE_PICTURE_CODE = 100, REQUEST_PERMISSION_CODE=101;

    EmotionServiceClient restClient = new EmotionServiceRestClient("040d7a79da944fb395461010cf2a5ae1");

    Bitmap mBitmap;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if(requestCode == REQUEST_PERMISSION_CODE)
        {
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED)
                Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show();
            else
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
        }


    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

       initViews();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if(checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED &&
            checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                requestPermissions(new String[]{
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.INTERNET},REQUEST_PERMISSION_CODE);
            }


        }

        }

    private void initViews(){

        btnProcess = (Button)findViewById(R.id.btnProcess);
        btnTakePicture = (Button)findViewById(R.id.btnTakePic);
        imageView = (ImageView)findViewById(R.id.imageView);

        btnTakePicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                takePicFromGallery();
            }
        });

        btnProcess.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                processImage();
            }
        });
    }

    private void processImage() {
        //Convert Image To Stream

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        mBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
        final ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());

        AsyncTask<InputStream, String, List<RecognizeResult>> processAsync = new AsyncTask<InputStream, String, List<RecognizeResult>>() {

            ProgressDialog mDialog = new ProgressDialog(MainActivity.this);

            @Override
            protected List<RecognizeResult> doInBackground(InputStream... inputStreams) {
                publishProgress("Please Wait...");
                List<RecognizeResult> results = null;
                try {
                    results = restClient.recognizeImage(inputStreams[0]);
                } catch (EmotionServiceException e) {
                    Toast.makeText(getApplicationContext(), "Couldn't Load The Info", Toast.LENGTH_SHORT).show();
                } catch (IOException e) {
                    Toast.makeText(getApplicationContext(), "Couldn't Load The Info", Toast.LENGTH_SHORT).show();
                }
                return results;
            }

            @Override
            protected void onPreExecute() {
                mDialog.show();
            }

            @Override
            protected void onProgressUpdate(String... values) {
                mDialog.setMessage(values[0]);
            }

            @Override
            protected void onPostExecute(List<RecognizeResult> recognizeResults) {
                mDialog.dismiss();
                for(RecognizeResult res : recognizeResults) {
                    String status = getEmotion(res);
                    try {
                        imageView.setImageBitmap(ImageHelper.drawRectOnBitmap(mBitmap, res.faceRectangle, status));
                    }catch (Exception e){
                        Toast.makeText(getApplicationContext(), "Couldn't Load The Info", Toast.LENGTH_SHORT).show();
                    }
                }
            }

        };

        processAsync.execute(inputStream);
    }

    private String getEmotion(RecognizeResult res) {

        List<Double>list = new ArrayList<>();
        Scores score = res.scores;

        list.add(score.anger);
        list.add(score.contempt);
        list.add(score.disgust);
        list.add(score.fear);
        list.add(score.happiness);
        list.add(score.neutral);
        list.add(score.sadness);
        list.add(score.surprise);

        //SortList
        Collections.sort(list);

        double maxNum = list.get(list.size()-1);

        if(maxNum == score.anger) return "Anger";
        else if (maxNum == score.contempt) return "Contempt";
        else if (maxNum == score.disgust) return "Disgust";
        else if (maxNum == score.fear) return "Fear";
        else if (maxNum == score.happiness) return "Happiness";
        else if (maxNum == score.neutral) return "Neutral";
        else if (maxNum == score.sadness) return "Sadness";
        else if (maxNum == score.surprise) return "Surprise";
        else return "Can't Detect";


    }

    private void takePicFromGallery() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, TAKE_PICTURE_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == TAKE_PICTURE_CODE)
        {
            Uri selectedImageUri = data.getData();
            InputStream in = null;
            try {
                in = getContentResolver().openInputStream(selectedImageUri);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            mBitmap = BitmapFactory.decodeStream(in);
            imageView.setImageBitmap(mBitmap);
        }
    }
}
