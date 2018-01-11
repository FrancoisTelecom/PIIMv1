package org.bytedeco.javacv_android_example;


import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;


public class MainActivity extends AppCompatActivity implements View.OnClickListener{
    private Button captureButton = null;
    private Button libraryButton = null;
    private Button analysisButton = null;
    public File image;
    private ImageView imageViewMain;
    private static final int REQUEST_PHOTO_LIB = 1;
    private static final int REQUEST_IMAGE_CAPTURE = 2;
    private static final int REQUEST_ANALYSIS = 3;
    public final static String TURI = "org.bytedeco.javacv_android_example.TURI";
    final String TAG=MainActivity.class.getName();
    private Intent analysisIntent;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // set du layout main

        captureButton = (Button) findViewById(R.id.captureButton);
        captureButton.setOnClickListener(this);

        libraryButton = (Button) findViewById(R.id.libraryButton);
        libraryButton.setOnClickListener(this);

        analysisButton = (Button) findViewById(R.id.analysisButton);
        analysisButton.setOnClickListener(this);
        analysisButton.setClickable(false);
        analysisButton.setBackgroundColor(Color.DKGRAY);

        imageViewMain = (ImageView) findViewById(R.id.mainPicture);
        analysisIntent = new Intent(MainActivity.this, AnalysisActivity.class);
        init();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // vÃ©rification Ã  quel intent on fait rÃ©fÃ©rence ici Ã  l'aide de notre identifiant
        if (resultCode == RESULT_OK && requestCode == REQUEST_PHOTO_LIB) {
            // vÃ©rification que l'opÃ©ration s'est bien dÃ©roulÃ©e
            analysisButton.setClickable(true);
            analysisButton.setBackgroundColor(Color.LTGRAY);
            getLibraryPicture(data);
        }
        else if(resultCode == RESULT_OK && requestCode == REQUEST_IMAGE_CAPTURE){
            analysisButton.setClickable(true);
            analysisButton.setBackgroundColor(Color.LTGRAY);
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            imageViewMain.setImageBitmap(imageBitmap);
            try {creatBitmapByRessources("pushpicture",0,imageBitmap);}catch(IOException e){e.printStackTrace();}
        }
        else if(resultCode == RESULT_OK && requestCode == REQUEST_ANALYSIS){
            //Toast.makeText(this,TAG+"Return ok ",Toast.LENGTH_SHORT).show();
        }
    }

    public void onClick(View v) {
        switch(v.getId()) {
            // Si l'identifiant de la vue est celui du bouton capture
            case R.id.captureButton:
                logg("OnClick Capture button");
                startCaptureActivity();
                break;
            case R.id.libraryButton:
                logg("OnClick library burtton");
                startPhotoLibraryActivity();
                break;
            case R.id.analysisButton:
                logg("OnClick analysis button");
                startAnalysisActivity();
                //change();
                break;
        }
    }

    public void change(){
        String ts= this.getCacheDir() + "/" + "coca_1";
        File tfile= new File(ts);
        String filePath = tfile.getPath();
        Bitmap bitmap = BitmapFactory.decodeFile(filePath);
        imageViewMain.setImageBitmap(bitmap);
    }

    private void startPhotoLibraryActivity() {
        Intent photoLibIntent = new Intent();
        photoLibIntent.setType("image/*");
        photoLibIntent.setAction(Intent.ACTION_GET_CONTENT);
        try {
            startActivityForResult(photoLibIntent, REQUEST_PHOTO_LIB);
        } catch (Exception e) {
            Toast.makeText(this, "L'application photo n'est pas disponible", Toast.LENGTH_LONG).show();
        }
    }

    private void startCaptureActivity(){
        String photoTest = null;
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
                Log.i(TAG,"@@Image crÃ©e");
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            if (photoFile != null) {
                photoTest = photoFile.toURI().toString();
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoTest);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    public void startAnalysisActivity(){

        startActivityForResult(analysisIntent, REQUEST_ANALYSIS);
    }

    private void getLibraryPicture(Intent data) {
        // l'adresse de l'image dans la carte SD
        Uri imageUri = data.getData();
        // Flux pour lire les donnÃ©es de la carte SD
        InputStream inputStream;
        try {
            inputStream = getContentResolver().openInputStream(imageUri);
            // obtention d'une image Bitmap
            Bitmap imageUP = BitmapFactory.decodeStream(inputStream);
            // Montre l'image Ã  l'utilisateur
            logg("height: "+ imageUP.getHeight()+" weight: "+ imageUP.getWidth() );
            imageViewMain.setImageBitmap(imageUP);
            try {creatBitmapByRessources("pushpicture",0,imageUP);}catch(IOException e){e.printStackTrace();}

        } catch (FileNotFoundException e) {
            e.printStackTrace();
            // Message Ã  l'utilisateur
            Toast.makeText(this, "Unable to open image", Toast.LENGTH_LONG).show();
        }
    }

    private File createImageFile() throws IOException {
        image = new File(this.getCacheDir() + "/"+"pushpicture");
        logg("wallImage" + image);
        return image;
    }

    private void creatBitmapByRessources(String nameFile,int drawableId,Bitmap btm) throws IOException {
        FileOutputStream output;
        Bitmap nfiles;
        String filePath;
        if(drawableId!=0){
            nfiles = BitmapFactory.decodeResource(getResources(),drawableId);
        }
        else{
            nfiles=btm;
             filePath = this.getCacheDir() + "/" + nameFile;
            analysisIntent.putExtra(TURI,nameFile);
        }
        if( nfiles.getHeight()> 3000 || nfiles.getWidth() > 2000){
            nfiles=getResizedBitmap(nfiles,nfiles.getHeight()/2,nfiles.getWidth()/2);
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        logg("@# " + nameFile + nfiles);
        nfiles.compress(Bitmap.CompressFormat.JPEG,100,bos);
         filePath = this.getCacheDir() + "/" + nameFile;
        byte[] bitmapData = bos.toByteArray();
        try {
            output = new FileOutputStream(filePath);
            output.write(bitmapData);
            output.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void logg (String txt){
        Log.i(TAG,"@@"+txt);
    }

    private void init(){
        try {
            creatBitmapByRessources("coca_1",R.drawable.coca_1,null);
            creatBitmapByRessources("coca_2",R.drawable.coca_2,null);
            creatBitmapByRessources("coca_3",R.drawable.coca_3,null);
            creatBitmapByRessources("coca_4",R.drawable.coca_4,null);
            creatBitmapByRessources("coca_5",R.drawable.coca_5,null);
            creatBitmapByRessources("coca_6",R.drawable.coca_6,null);
            creatBitmapByRessources("coca_7",R.drawable.coca_7,null);
            creatBitmapByRessources("coca_8",R.drawable.coca_8,null);
            creatBitmapByRessources("coca_9",R.drawable.coca_9,null);
            creatBitmapByRessources("coca_10",R.drawable.coca_10,null);
            creatBitmapByRessources("coca_11",R.drawable.coca_11,null);
            creatBitmapByRessources("coca_12",R.drawable.coca_12,null);

            creatBitmapByRessources("pepsi_1",R.drawable.pepsi_1,null);
            creatBitmapByRessources("pepsi_2",R.drawable.pepsi_2,null);
            creatBitmapByRessources("pepsi_3",R.drawable.pepsi_3,null);
            creatBitmapByRessources("pepsi_4",R.drawable.pepsi_4,null);
            creatBitmapByRessources("pepsi_5",R.drawable.pepsi_5,null);
            creatBitmapByRessources("pepsi_6",R.drawable.pepsi_6,null);
            creatBitmapByRessources("pepsi_7",R.drawable.pepsi_7,null);
            creatBitmapByRessources("pepsi_8",R.drawable.pepsi_8,null);
            creatBitmapByRessources("pepsi_9",R.drawable.pepsi_9,null);
            creatBitmapByRessources("pepsi_10",R.drawable.pepsi_10,null);
            creatBitmapByRessources("pepsi_11",R.drawable.pepsi_11,null);
            creatBitmapByRessources("pepsi_12",R.drawable.pepsi_12,null);

            creatBitmapByRessources("sprite_1",R.drawable.sprite_1,null);
            creatBitmapByRessources("sprite_2",R.drawable.sprite_2,null);
            creatBitmapByRessources("sprite_3",R.drawable.sprite_3,null);
            creatBitmapByRessources("sprite_4",R.drawable.sprite_4,null);
            creatBitmapByRessources("sprite_5",R.drawable.sprite_5,null);
            creatBitmapByRessources("sprite_6",R.drawable.sprite_6,null);
            creatBitmapByRessources("sprite_7",R.drawable.sprite_7,null);
            creatBitmapByRessources("sprite_8",R.drawable.sprite_8,null);
            creatBitmapByRessources("sprite_9",R.drawable.sprite_9,null);
            creatBitmapByRessources("sprite_10",R.drawable.sprite_10,null);
            creatBitmapByRessources("sprite_11",R.drawable.sprite_11,null);
            creatBitmapByRessources("sprite_12",R.drawable.sprite_12,null);
            logg("Ensembles des images creer");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Bitmap getResizedBitmap(Bitmap bm, int newWidth, int newHeight) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        // CREATE A MATRIX FOR THE MANIPULATION
        Matrix matrix = new Matrix();
        // RESIZE THE BIT MAP
        matrix.postScale(scaleWidth, scaleHeight);
        // "RECREATE" THE NEW BITMAP
        Bitmap resizedBitmap = Bitmap.createBitmap(
                bm, 0, 0, width, height, matrix, false);
        return resizedBitmap;
    }
}