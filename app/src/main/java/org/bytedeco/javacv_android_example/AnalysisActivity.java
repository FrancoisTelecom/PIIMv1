package org.bytedeco.javacv_android_example;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.RequiresApi;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.opencv_calib3d;
import org.bytedeco.javacpp.opencv_core.DMatch;
import org.bytedeco.javacpp.opencv_core.DMatchVector;
import org.bytedeco.javacpp.opencv_core.KeyPointVector;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_features2d.BFMatcher;
import org.bytedeco.javacpp.opencv_shape;
import org.bytedeco.javacpp.opencv_xfeatures2d.SIFT;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.Arrays;

import static org.bytedeco.javacpp.opencv_core.NORM_L2;
import static org.bytedeco.javacpp.opencv_core.log;
import static org.bytedeco.javacpp.opencv_imgcodecs.imread;

/**
 * Created by ZMNF8866 on 10/09/2017.
 */

public class AnalysisActivity extends Activity implements View.OnClickListener {
    private Button returnButton = null; // creation des variables de la gestions des boutons
    private Button webButton = null;
    private ImageView imageView;
    final String TAG = MainActivity.class.getName();
    /*Variable init SIFT*/
    private int nFeatures = 0;
    private int nOctaveLayers = 3;
    private double contrastThreshold = 0.03;
    private int edgeThreshold = 10;
    private double sigma = 1.6;
    private int groupeKNN = 0;
    String resources_ID_WEB[] = new String[]{"https://www.google.fr", "https://www.cocacola.fr", "http://www.pepsi.fr/", "http://www.coca-cola-france.fr/gamme/sprite/"};
    SIFT sift;
    Intent i;
    int loopProgressBar;
    private boolean tmp = false;
    ProgressBar bar;
    int resources_ID[] = new int[]{R.drawable.not, R.drawable.coca_1, R.drawable.pepsi_4, R.drawable.sprite_10};


    public int getloopProgressBar() {
        return loopProgressBar;
    }

    public void incloopProgressBar(){
        loopProgressBar++;
        logg("b: "+loopProgressBar);
    }
    @RequiresApi(api = Build.VERSION_CODES.CUPCAKE)
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analysis);
        i = getIntent();

        returnButton = (Button) findViewById(R.id.returnButton);
        returnButton.setOnClickListener(this);

        webButton = (Button) findViewById(R.id.webButton);
        webButton.setOnClickListener(this);
        webButton.setClickable(false);
        webButton.setBackgroundColor(Color.DKGRAY);

        imageView = (ImageView) findViewById(R.id.analysisPicture);
        imageView.setOnClickListener(this);

        bar = (ProgressBar)findViewById(R.id.progressBar);
        bar.setVisibility(View.VISIBLE);
        
        imageView.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.load));
        new ThreadAnalysis().execute("");
    }


    public void onClick(View v) {
        switch (v.getId()) {
            // Si l'identifiant de la vue est celui du bouton capture
            case R.id.returnButton:
                Toast.makeText(this, "Main", Toast.LENGTH_SHORT).show();
                Intent intentResult = new Intent(AnalysisActivity.this, MainActivity.class);
                finish();
                break;
            // Si l'identifiant de la vue est celui du bouton photo library
            case R.id.webButton:
                Toast.makeText(this, "Web", Toast.LENGTH_SHORT).show();
                Intent openURL = new Intent(android.content.Intent.ACTION_VIEW);
                openURL.setData(Uri.parse(resources_ID_WEB[groupeKNN]));
                startActivity(openURL);
                break;
        }
    }

    public void setPicture() {
        //int resources_ID[] = new int[]{R.drawable.not, R.drawable.coca_1, R.drawable.pepsi_4, R.drawable.sprite_10};

        Mat imageLC = null;
        String sPushPicture = i.getStringExtra(MainActivity.TURI);
        logg("@@fileSpush" + sPushPicture);
        try {
            imageLC = OpenImRead(sPushPicture);
            // imageLC = OpenImRead(this.getCacheDir() + "/" + "coca_1");
        } catch (Exception e) {
            logg("Image LC phushPicture pas charger ");
            e.printStackTrace();
        }

        logg("@@Imread " + imageLC);

        Mat[] images = new Mat[]{
                OpenImRead("coca_1"), OpenImRead("coca_2"), OpenImRead("coca_3"), OpenImRead("coca_4"), OpenImRead("coca_5"),
                OpenImRead("coca_6"), OpenImRead("coca_7"), OpenImRead("coca_8"), OpenImRead("coca_9"), OpenImRead("coca_10"),
                OpenImRead("coca_11"), OpenImRead("coca_12"), OpenImRead("pepsi_1"), OpenImRead("pepsi_2"), OpenImRead("pepsi_3"),
                OpenImRead("pepsi_4"), OpenImRead("pepsi_5"), OpenImRead("pepsi_6"), OpenImRead("pepsi_7"), OpenImRead("pepsi_8"),
                OpenImRead("pepsi_9"), OpenImRead("pepsi_10"), OpenImRead("pepsi_11"), OpenImRead("pepsi_12"), OpenImRead("sprite_1"),
                OpenImRead("sprite_2"), OpenImRead("sprite_3"), OpenImRead("sprite_4"), OpenImRead("sprite_5"), OpenImRead("sprite_6"),
                OpenImRead("sprite_7"), OpenImRead("sprite_8"), OpenImRead("sprite_9"), OpenImRead("sprite_10"), OpenImRead("sprite_11"), OpenImRead("sprite_12")};
        int GroupeClassification[] = {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
                3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3};//ReprÃ©sente les diffÃ©rents groupes avec 1=groupe coca 2=Pepsi et 3=Sprite

        float ImageClassifiration[][] = new float[3][images.length]; // DÃ©claration de deux tableaux Ã  doubles dimensions.
        float ImageClassifirationSort[][] = new float[3][images.length];

        /*taille de la barre de progressions*/
        logg("images.length"+ images.length);
        bar.setMax(images.length);


        Loader.load(opencv_calib3d.class);
        Loader.load(opencv_shape.class);

        sift = SIFT.create(nFeatures, nOctaveLayers, contrastThreshold, edgeThreshold, sigma);
        KeyPointVector[] keypoints = new KeyPointVector[images.length]; // CrÃ©ation des tableaux pour le traitement
        Mat[] descriptors = new Mat[images.length];

        KeyPointVector ImageTestkeypoints = new KeyPointVector(); // Traitement pour l'image de test
        Mat ImageTestdescriptors = new Mat();
        sift.detect(imageLC, ImageTestkeypoints);
        sift.compute(imageLC, ImageTestkeypoints, ImageTestdescriptors);

        BFMatcher matcher = new BFMatcher(NORM_L2, false);
        DMatchVector matches = new DMatchVector();
        float distance = 0;

        for (int i = 0; i < images.length; i++) { // Traitement pour toutes les images-modÃ¨les et comparaisons avec l'image-test

            keypoints[i] = new KeyPointVector();
            descriptors[i] = new Mat();
            sift.detect(images[i], keypoints[i]);
            sift.compute(images[i], keypoints[i], descriptors[i]);
            matcher.match(ImageTestdescriptors, descriptors[i], matches);
            incloopProgressBar();
            bar.setProgress(getloopProgressBar());

            if (matches.size() < 20) {
                ImageClassifiration[0][i] = i;
                ImageClassifiration[1][i] = GroupeClassification[i];
                ImageClassifiration[2][i] = 500;
                ImageClassifirationSort[0][i] = i;
                ImageClassifirationSort[1][i] = GroupeClassification[i];
                ImageClassifirationSort[2][i] = 500;
                logg("@@distance" + i + ": " + 500); //print la distance


            } else {
                DMatchVector bestMatch = selectBest(matches, 20);

                for (int i1 = 0; i1 < bestMatch.size(); i1++) {
                    distance += bestMatch.get(i1).distance();
                }
                distance = distance / bestMatch.size();// Calcule de la distance moyenne
                ImageClassifiration[0][i] = i;
                ImageClassifiration[1][i] = GroupeClassification[i];
                ImageClassifiration[2][i] = distance;
                ImageClassifirationSort[0][i] = i;
                ImageClassifirationSort[1][i] = GroupeClassification[i];
                ImageClassifirationSort[2][i] = distance;
                logg("@@distance" + i + ": " + distance); //print la distance
            }
        }

        Arrays.sort(ImageClassifirationSort[2]); // Classification du tableau dans l'ordre croissant.
        for (int d = 0; d < images.length; d++) {
            for (int c = 0; c < images.length; c++) {
                if (ImageClassifirationSort[2][d] == ImageClassifiration[2][c]) {
                    ImageClassifirationSort[0][d] = ImageClassifiration[0][c];
                    ImageClassifirationSort[1][d] = ImageClassifiration[1][c];
                }
            }
        }
        groupeKNN = KNN(ImageClassifirationSort, 5); // Appel de la fonction KNN.
        //setPictureKNN(groupeKNN, resources_ID);
        logg("@@groupe: " + groupeKNN);// Print du rÃ©sultat, le groupe auquel l'image appartient.
    }

    public void setPictureKNN(int knn, int[] resources) {
        imageView.setImageDrawable(ContextCompat.getDrawable(this, resources[knn]));
        webButton.setClickable(true);
        webButton.setBackgroundColor(Color.LTGRAY);
    }

    public int KNN(float[][] imageClassifiration, int nombre) {
        int Group[] = {0, 0, 0};
        int group = 0;
        float moyenne = 0;
        for (int i = 0; i < nombre; i++) { // Prendre les n premiÃƒÂ¨res images du tableau classifion
            group = (int) imageClassifiration[1][i];
            logg("Fin groupe: " + group);
            Group[group - 1]++;
            moyenne = moyenne + imageClassifiration[2][i];
        }
        //Log.i(TAG,"@@moyenne :"+moyenne/nombre);
        logg("moyenne:" + moyenne / nombre);
        if ((moyenne / nombre) > 250) {
            return 0;
        }
        int min = 0, groupe = 0;
        for (int i = 0; i < 3; i++) { // Prendre le groupe qui a le plus d'image dans les n premiÃ¨res images.
            if (Group[i] > min) {
                min = Group[i];
                groupe = i + 1;
            } else if (min == Group[i]) {
                min = Group[i];
                groupe = (int) imageClassifiration[1][0];
            }
        }
        logg("Array sort: " + Group[0] + " " + Group[1] + " " + Group[2]);
        return groupe; // On retourne le groupe.
    }

    @RequiresApi(api = Build.VERSION_CODES.GINGERBREAD)
    public DMatchVector selectBest(DMatchVector matches, int numberToSelect) {
        DMatch[] sorted = toArray(matches);
        Arrays.sort(sorted, (a, b) -> {
            return a.lessThan(b) ? -1 : 1;
        });
        DMatch[] best = Arrays.copyOf(sorted, numberToSelect);

        return new DMatchVector(best);
    }

    public DMatch[] toArray(DMatchVector matches) {
        assert matches.size() <= Integer.MAX_VALUE;
        int n = (int) matches.size();
        // Convert keyPoints to Scala sequence
        DMatch[] result = new DMatch[n];
        for (int i = 0; i < n; i++) {
            result[i] = new DMatch(matches.get(i));
        }
        return result;
    }

    private Mat OpenImRead(String nameFile) {
        Mat test = null;
        String pathFile = this.getCacheDir() + "/" + nameFile;
        File tfile = new File(pathFile);
        String filePath = tfile.getPath();
        if (tfile.exists()) {
            try {
                test = imread(filePath);
                logg("imread file ok -> " + nameFile + " " + test);
            } catch (Exception e) {
                e.printStackTrace();
                logg("imread file ko");
            }
        }
        return test;
    }

    private void logg(String txt) {
        Log.i(TAG, "@@" + txt);
    }

    private class ThreadAnalysis extends AsyncTask<String, Void, String>{
        private int ressource;
        AnalysisActivity analysisActivity = new AnalysisActivity();

        @Override
        protected String doInBackground(String... params){
                try {
                    setPicture();
                    Thread.sleep(1000);
                    logg("fin du thread");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    Thread.interrupted();
                }
            logg("fin du thread");
            return "@@result";
        }
        @Override
        protected void onPostExecute(String result) {
            logg("@@fin du Post Execute");
            setPictureKNN(groupeKNN,resources_ID);
        }

        @Override
        protected void onPreExecute() {}

        @Override
        protected void onProgressUpdate(Void... values) {}
    }
}

