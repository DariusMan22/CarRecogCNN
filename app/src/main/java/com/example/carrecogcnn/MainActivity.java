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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.carrecogcnn.ml.Efficientnetb0Compcars63LabelsAugV1;
import com.example.carrecogcnn.ml.Mobilenetv2Compcars63LabelsBetter;
import com.example.carrecogcnn.ml.NlCnnCompcars63LabelsV0;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {


    Button bOpenCamera;
    Button bSelectImage;
    TextView tvResult;
    TextView tvResultProcessTime;
    TextView tvTrust;
    ImageView ivAddImage;
    ImageView ivLogo;
    ActivityResultLauncher<String> mGetContent;
    ActivityResultLauncher<Intent> activityResultLauncher;

    long difference;

    Bitmap imageBitmapUseOnItemLits = null;

    String[] items = {"Xception","NL-CNN","MobileNetV2","EfficientnetB0"};
    AutoCompleteTextView autoCompleteTxt;
    ArrayAdapter<String> adapterItems;
    String item = "Xception";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tvResult = findViewById(R.id.tv_result);
        tvResultProcessTime = findViewById(R.id.tv_result2);
        tvTrust = findViewById(R.id.tv_trust);
        ivAddImage = findViewById(R.id.iv_add_image);
        ivLogo = findViewById(R.id.iv_logo);

        autoCompleteTxt = findViewById(R.id.auto_complete_txt);
        adapterItems = new ArrayAdapter<String>(this, R.layout.list_item,items);

        autoCompleteTxt.setAdapter(adapterItems);

        autoCompleteTxt.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                item = parent.getItemAtPosition(position).toString();
                Toast.makeText(getApplicationContext(), "Model: "+item, Toast.LENGTH_SHORT).show();
                if (imageBitmapUseOnItemLits != null) {
                    changeLogo(runModel(item, imageBitmapUseOnItemLits));
                    tvResultProcessTime.setText("Timpul de clasificare este:"+"\n"+String.valueOf(difference)+"ms");
                }
                else{
                    Toast.makeText(getApplicationContext(), "Fă o poză", Toast.LENGTH_SHORT).show();
                }
            }
        });


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

        Button btn = (Button)findViewById(R.id.b_graph_activity);

        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (imageBitmapUseOnItemLits != null) {
                    List<Object> outputFeatureXception = dataFromOutputFeature(runModel(items[0],imageBitmapUseOnItemLits));
                    List<Object> outputFeatureNlCnn = dataFromOutputFeature(runModel(items[1],imageBitmapUseOnItemLits));
                    List<Object> outputFeatureMobileNetV2 = dataFromOutputFeature(runModel(items[2],imageBitmapUseOnItemLits));
                    List<Object> outputFeatureEfficientnetB0 = dataFromOutputFeature(runModel(items[3],imageBitmapUseOnItemLits));

                    Bundle extras = new Bundle();
                    extras.putString("maxPositionsX", outputFeatureXception.get(0).toString());
                    extras.putString("confidenceX", outputFeatureXception.get(1).toString());
                    extras.putString("maxPositionsNl", outputFeatureNlCnn.get(0).toString());
                    extras.putString("confidenceNl", outputFeatureNlCnn.get(1).toString());
                    extras.putString("maxPositionsMob", outputFeatureMobileNetV2.get(0).toString());
                    extras.putString("confidenceMob", outputFeatureMobileNetV2.get(1).toString());
                    extras.putString("maxPositionsEff", outputFeatureEfficientnetB0.get(0).toString());
                    extras.putString("confidenceEff", outputFeatureEfficientnetB0.get(1).toString());

                    Intent i1 = new Intent(MainActivity.this, GraphActivity.class);
                    i1.putExtras(extras);
                    startActivity(i1);
                }
                else{
                    Toast.makeText(getApplicationContext(), "Fară poză", Toast.LENGTH_SHORT).show();
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

    private Uri saveImage(@NonNull Bitmap image, @NonNull Context context){


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

    private List<Object> dataFromOutputFeature(@NonNull TensorBuffer outputFeature0){
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
        return Arrays.asList(maxPos, maxConfidence);
    }

    private void changeLogo(@NonNull TensorBuffer outputFeature0){

        List<Object> data = dataFromOutputFeature(outputFeature0);

        String[] classes = {"DS", "Dacia", "GMC", "Jeep", "MG", "MINI", "PGO", "TESLA", "smart",
                "Mitsubishi", "Toyota", "Isuzu", "Iveco", "Porsche", "Chrysler", "Lamborghini",
                "Cadillac", "Lorinser", "Rolls-Royce", "Carlsson", "Volkswagen", "Benz",
                "Audi", "Wisemann", "BMW", "Bentley", "Bugatti", "Pagani", "Jaguar", "Morgan",
                "Subaru", "Skoda", "Nissan", "Honda", "Lincoln", "Peugeot", "Opel", "Vauxhall",
                "Volvo", "Ferrari", "Maserati", "Hyundai ", "Ford", "Koenigsegg", "Infiniti",
                "FIAT", "Lancia", "Seat", "Acura", "KIA", "LAND-ROVER", "McLaren", "Maybach",
                "Dodge", "Mustang", "Suzuki", "Alfa Romeo", "Aston Martin", "Chevy", "Citroen",
                "Lexus", "Renault", "MAZDA"};

        int []logoArray={R.drawable.ds_logo,R.drawable.dacia_logo,R.drawable.gmc_logo,
                R.drawable.jeep_logo, R.drawable.mg_logo, R.drawable.mini_logo, R.drawable.pgo_logo,
                R.drawable.tesla_logo, R.drawable.smart_logo};


        tvResult.setText(classes[(int)data.get(0)]);
        tvTrust.setText("Incredere:"+ String.format("%.2f",(float)data.get(1) * 100 )+"%");
        //ivLogo.setImageResource(logoArray[maxPos]);
    }

    @NonNull
    private TensorBuffer generateTensorBuffer(@NonNull Bitmap imageBitmap, float divide, float substract){
        TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 128, 128, 3}, DataType.FLOAT32);
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4*128*128*3);
        byteBuffer.order(ByteOrder.nativeOrder());

        int[] intValues = new int[128*128];
        imageBitmap.getPixels(intValues,0,imageBitmap.getWidth(),0,0,imageBitmap.getWidth(), imageBitmap.getHeight());
        int pixel = 0;

        for(int i = 0; i < 128; i ++){
            for(int j = 0; j < 128; j++){
                int val = intValues[pixel++]; // RGB
                byteBuffer.putFloat(((val >> 16) & 0xFF) * (1.f / divide) - substract) ;
                byteBuffer.putFloat(((val >> 8) & 0xFF) * (1.f / divide) - substract);
                byteBuffer.putFloat((val & 0xFF) * (1.f / divide) - substract);
            }
        }
        inputFeature0.loadBuffer(byteBuffer);
        return inputFeature0;
    }


    private void runAllModels(@NonNull String selected_model, Bitmap imageBitmap){

    }

    private TensorBuffer runModel(@NonNull String selected_model, Bitmap imageBitmap){
        long startTime = System.currentTimeMillis();
        TensorBuffer outputFeature = null;
        if(selected_model.equals("Xception")){
            try {
                XceptionCompcars63LabelsV0 model = XceptionCompcars63LabelsV0.newInstance(getApplicationContext());

                // Creates inputs for reference.
                TensorBuffer inputFeature0 = generateTensorBuffer(imageBitmap, 255, 0);

                // Runs model inference and gets result.
                XceptionCompcars63LabelsV0.Outputs outputs = model.process(inputFeature0);
                TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();
                outputFeature = outputFeature0;

                // Releases model resources if no longer used.
                model.close();
            } catch (IOException e) {
                // TODO Handle the exception
            }
        }
        else if(selected_model.equals("NL-CNN")){
            try {
                NlCnnCompcars63LabelsV0 model = NlCnnCompcars63LabelsV0.newInstance(getApplicationContext());

                // Creates inputs for reference.
                TensorBuffer inputFeature0 = generateTensorBuffer(imageBitmap, 255, 0);


                // Runs model inference and gets result.
                NlCnnCompcars63LabelsV0.Outputs outputs = model.process(inputFeature0);
                TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();
                outputFeature = outputFeature0;

                // Releases model resources if no longer used.
                model.close();

            } catch (IOException e) {
                // TODO Handle the exception
            }
        }
        else if(selected_model.equals("MobileNetV2")){
            try {
                Mobilenetv2Compcars63LabelsBetter model = Mobilenetv2Compcars63LabelsBetter.newInstance(getApplicationContext());

                // Creates inputs for reference.
                TensorBuffer inputFeature0 = generateTensorBuffer(imageBitmap, 124.5f, 1f);


                // Runs model inference and gets result.
                Mobilenetv2Compcars63LabelsBetter.Outputs outputs = model.process(inputFeature0);
                TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();
                outputFeature = outputFeature0;

                // Releases model resources if no longer used.
                model.close();


            } catch (IOException e) {
                // TODO Handle the exception
            }
        }
        else if(selected_model.equals("EfficientnetB0")){
            try {
                Efficientnetb0Compcars63LabelsAugV1 model = Efficientnetb0Compcars63LabelsAugV1.newInstance(getApplicationContext());

                // Creates inputs for reference.
                TensorBuffer inputFeature0 = generateTensorBuffer(imageBitmap, 1.0f, 0.0f);


                // Runs model inference and gets result.
                Efficientnetb0Compcars63LabelsAugV1.Outputs outputs = model.process(inputFeature0);
                TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();
                outputFeature = outputFeature0;

                // Releases model resources if no longer used.
                model.close();

            } catch (IOException e) {
                // TODO Handle the exception
            }
        }
        else{
            try {
                XceptionCompcars63LabelsV0 model = XceptionCompcars63LabelsV0.newInstance(getApplicationContext());

                // Creates inputs for reference.
                TensorBuffer inputFeature0 = generateTensorBuffer(imageBitmap, 255, 0);

                // Runs model inference and gets result.
                XceptionCompcars63LabelsV0.Outputs outputs = model.process(inputFeature0);
                TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();
                outputFeature = outputFeature0;

                // Releases model resources if no longer used.
                model.close();

            } catch (IOException e) {
                // TODO Handle the exception
            }
        }
        //changeLogo(outputFeature);
        difference = System.currentTimeMillis() - startTime;
        return outputFeature;
        //tvResultProcessTime.setText("Timpul de clasificare este:"+"\n"+String.valueOf(difference)+"ms");
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
        imageBitmapUseOnItemLits = imageBitmap;
        ivAddImage.setImageBitmap(imageBitmap);
        changeLogo(runModel(item, imageBitmap));
        tvResultProcessTime.setText("Timpul de clasificare este:"+"\n"+String.valueOf(difference)+"ms");
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