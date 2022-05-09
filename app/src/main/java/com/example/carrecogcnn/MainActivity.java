package com.example.carrecogcnn;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.carrecogcnn.ml.XceptionCompcars63LabelsV0;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class MainActivity extends AppCompatActivity {


    Button bOpenCamera;
    Button bSelectImage;
    TextView tvResult;
    ImageView ivAddImage;
    ImageView ivLogo;
    ActivityResultLauncher<String> mGetContent;
    ActivityResultLauncher<Intent> activityResultLauncher;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tvResult = findViewById(R.id.tv_result);
        ivAddImage = findViewById(R.id.iv_add_image);
        ivLogo = findViewById(R.id.iv_logo);

        //for getting the image from files
        bSelectImage = findViewById(R.id.b_select_image);
        bSelectImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                        && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    mGetContent.launch("image/*");
                }else {
                    requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE},1);
                }
            }
        });

        mGetContent = registerForActivityResult(new ActivityResultContracts.GetContent(), new ActivityResultCallback<Uri>() {
            @Override
            public void onActivityResult(Uri result) {
                Intent intent = new Intent(MainActivity.this, CropperActivity.class);
                intent.putExtra("DATA",result.toString());
                startActivityForResult(intent, 101);
            }
        });

        //for using the camera
        bOpenCamera = findViewById(R.id.b_open_camera);
        bOpenCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    activityResultLauncher.launch(takePictureIntent);
                }else {
                    requestPermissions(new String[]{Manifest.permission.CAMERA}, 2);
                }
            }
        });

        activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
            @Override
            public void onActivityResult(ActivityResult result) {

                Bundle extras = result.getData().getExtras();
                Uri imageUri;
                Bitmap imageBitmap = (Bitmap) extras.get("data");

                float degrees = 90; //rotation degree
                Matrix matrix = new Matrix();
                matrix.setRotate(degrees);

                /*
                WeakReference<Bitmap> resultCamera = new WeakReference<>(Bitmap.createScaledBitmap(imageBitmap,
                        imageBitmap.getWidth(), imageBitmap.getHeight(), false).copy(
                        Bitmap.Config.RGB_565, true));
                */
                WeakReference<Bitmap> resultCamera = new WeakReference<>(Bitmap.createBitmap(imageBitmap,0,0,
                        imageBitmap.getWidth(), imageBitmap.getHeight(),matrix, false).copy(
                        Bitmap.Config.RGB_565, true));

                Bitmap bm = resultCamera.get();
                imageUri = saveImage(bm, MainActivity.this);
                //iv_pick_show.setImageURI(imageUri);
                Intent intent = new Intent(MainActivity.this, CropperActivity.class);
                intent.putExtra("DATA",imageUri.toString());
                startActivityForResult(intent, 102);

            }
        });
        tvResult.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q="+tvResult.getText().toString()));
                startActivity(intent);
            }
        });

    }

    private Uri saveImage(Bitmap image, Context context){


        File imageFolder = new File(context.getCacheDir(), "images");
        Uri uri = null;
        try{
            imageFolder.mkdir();
            File file = new File(imageFolder, "captured_image.jpg");
            FileOutputStream stream = new FileOutputStream(file);
            image.compress(Bitmap.CompressFormat.JPEG, 100, stream);
            stream.flush();
            stream.close();
            uri = FileProvider.getUriForFile(context.getApplicationContext(), "com.example.carrecogcnn"+".provider", file);

        } catch (FileNotFoundException e){
            e.printStackTrace();
        }catch (IOException e){
            e.printStackTrace();
        }
        return uri;
    }


    private Bitmap uriToBitmap(Uri result) throws IOException{
        Bitmap imageBitmap = null;
        return MediaStore.Images.Media.getBitmap(this.getContentResolver(), result);
    }

    private void changeLogo(int logoIndex){
        int []logoArray={R.drawable.ds_logo,R.drawable.dacia_logo,R.drawable.gmc_logo,
                R.drawable.jeep_logo, R.drawable.mg_logo};
        ivLogo.setImageResource(logoArray[logoIndex]);
    }

    private void outputGenerator(Uri image){
        Bitmap imageBitmap = null;
        try {
            imageBitmap = uriToBitmap(image);
        }catch(IOException e){
            e.printStackTrace();
        }
        //ivAddImage.setImageBitmap(imageBitmap);

        imageBitmap = Bitmap.createScaledBitmap(imageBitmap, 128, 128, false);
        ivAddImage.setImageBitmap(imageBitmap);

        try {
            XceptionCompcars63LabelsV0 model = XceptionCompcars63LabelsV0.newInstance(getApplicationContext());

            // Creates inputs for reference.
            TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 128, 128, 3}, DataType.FLOAT32);
            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4*128*128*3);
            byteBuffer.order(ByteOrder.nativeOrder());

            int[] intValues = new int[128*128];
            imageBitmap.getPixels(intValues,0,imageBitmap.getWidth(),0,0,imageBitmap.getWidth(), imageBitmap.getHeight());
            int pixel = 0;

            for(int i = 0; i < 128; i ++){
                for(int j = 0; j < 128; j++){
                    int val = intValues[pixel++]; // RGB
                    byteBuffer.putFloat(((val >> 16) & 0xFF) * (1.f / 255));
                    byteBuffer.putFloat(((val >> 8) & 0xFF) * (1.f / 255));
                    byteBuffer.putFloat((val & 0xFF) * (1.f / 255));
                }
            }
            inputFeature0.loadBuffer(byteBuffer);

            // Runs model inference and gets result.
            XceptionCompcars63LabelsV0.Outputs outputs = model.process(inputFeature0);
            TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();


            // Releases model resources if no longer used.
            model.close();
            float[] confidences = outputFeature0.getFloatArray();
            // find the index of the class with the biggest confidence.
            int maxPos = 0;
            float maxConfidence = 0;
            for (int i = 0; i < confidences.length; i++) {
                if (confidences[i] > maxConfidence) {
                    maxConfidence = confidences[i];
                    maxPos = i;
                }
            }
            String[] classes = {"DS", "Dacia", "GMC", "Jeep", "MG", "MINI", "PGO", "TESLA", "smart",
                    "Mitsubishi", "Toyota", "Isuzu", "Iveco", "Porsche", "Chrysler", "Lamborghini",
                    "Cadillac", "Lorinser", "Rolls-Royce", "Carlsson", "Volkswagen", "Benz",
                    "Audi", "Wisemann", "BMW", "Bentley", "Bugatti", "Pagani", "Jaguar", "Morgan",
                    "Subaru", "Skoda", "Nissan", "Honda", "Lincoln", "Peugeot", "Opel", "Vauxhall",
                    "Volvo", "Ferrari", "Maserati", "Hyundai ", "Ford", "Koenigsegg", "Infiniti",
                    "FIAT", "Lancia", "Seat", "Acura", "KIA", "LAND-ROVER", "McLaren", "Maybach",
                    "Dodge", "Mustang", "Suzuki", "Alfa Romeo", "Aston Martin", "Chevy", "Citroen",
                    "Lexus", "Renault", "MAZDA"};
            tvResult.setText(classes[maxPos]);
            changeLogo(maxPos);

        } catch (IOException e) {
            // TODO Handle the exception
        }


    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    mGetContent.launch("image/*");
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    //requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE},1);
                }
            }
            case 2:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    activityResultLauncher.launch(takePictureIntent);

                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    //requestPermissions(new String[]{Manifest.permission.CAMERA}, 2);
                }
                // other 'case' lines to check for other
                // permissions this app might request.
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + requestCode);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == -1 && requestCode == 101){
            String result = data.getStringExtra("RESULT");
            Uri resultUri = null;
            if(result!=null){
                resultUri = Uri.parse(result);
            }
            outputGenerator(resultUri);
        }
        else if(resultCode == -1 && requestCode == 102){
            String result = data.getStringExtra("RESULT");
            Uri resultUri = null;
            if(result!=null){
                resultUri = Uri.parse(result);
            }
            outputGenerator(resultUri);
        }
    }
}