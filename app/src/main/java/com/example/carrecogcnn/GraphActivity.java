package com.example.carrecogcnn;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.LegendEntry;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.formatter.IValueFormatter;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.github.mikephil.charting.utils.ViewPortHandler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Formatter;

public class GraphActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_graph);
        Bundle extras = getIntent().getExtras();
        //String Value = extras.getString("maxPositionsX").toString();

        BarChart barChart = findViewById(R.id.barChart);
        ArrayList<BarEntry> makerCars = new ArrayList<>();

        String[] classes = {"DS", "Dacia", "GMC", "Jeep", "MG", "MINI", "PGO", "TESLA", "smart",
                "Mitsubishi", "Toyota", "Isuzu", "Iveco", "Porsche", "Chrysler", "Lamborghini",
                "Cadillac", "Lorinser", "Rolls-Royce", "Carlsson", "Volkswagen", "Benz",
                "Audi", "Wisemann", "BMW", "Bentley", "Bugatti", "Pagani", "Jaguar", "Morgan",
                "Subaru", "Skoda", "Nissan", "Honda", "Lincoln", "Peugeot", "Opel", "Vauxhall",
                "Volvo", "Ferrari", "Maserati", "Hyundai ", "Ford", "Koenigsegg", "Infiniti",
                "FIAT", "Lancia", "Seat", "Acura", "KIA", "LAND-ROVER", "McLaren", "Maybach",
                "Dodge", "Mustang", "Suzuki", "Alfa Romeo", "Aston Martin", "Chevy", "Citroen",
                "Lexus", "Renault", "MAZDA"};

        int maxPosX = Integer.parseInt(extras.getString("maxPositionsX"));
        int maxPosNl = Integer.parseInt(extras.getString("maxPositionsNl"));
        int maxPosMob = Integer.parseInt(extras.getString("maxPositionsMob"));
        int maxPosEff = Integer.parseInt(extras.getString("maxPositionsEff"));



        makerCars.add(new BarEntry(0f,Float.parseFloat((extras.getString("confidenceX")))*100,"Xception"));
        makerCars.add(new BarEntry(1f,Float.parseFloat((extras.getString("confidenceNl")))*100, "NL-CNN"));
        makerCars.add(new BarEntry(2f,Float.parseFloat((extras.getString("confidenceMob")))*100, "MobileNet"));
        makerCars.add(new BarEntry(3f,Float.parseFloat((extras.getString("confidenceEff")))*100, "EffiecientNetB0"));

/*
        makerCars.add(new BarEntry(0f,Float.parseFloat((extras.getString("confidenceX")))*100));
        makerCars.add(new BarEntry(1f,Float.parseFloat((extras.getString("confidenceNl")))*100));
        makerCars.add(new BarEntry(2f,Float.parseFloat((extras.getString("confidenceMob")))*100));
        makerCars.add(new BarEntry(3f,Float.parseFloat((extras.getString("confidenceEff")))*100));
*/
        BarDataSet barDataSet = new BarDataSet(makerCars, "MakerCars");

        barDataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        barDataSet.setValueTextColor(Color.WHITE);
        barDataSet.setValueTextSize(16f);
        barDataSet.setValueFormatter(new PercentFormatter());





        BarData barData = new BarData(barDataSet);


        ArrayList<String> xVals = new ArrayList<>();
        xVals.add(classes[maxPosX]);
        xVals.add(classes[maxPosNl]);
        xVals.add(classes[maxPosMob]);
        xVals.add(classes[maxPosEff]);

        XAxis xAxis = barChart.getXAxis();

        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM_INSIDE);

        ValueFormatter formatter = new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return xVals.get((int) value);
            }
        };




        xAxis.setGranularity(1f); // minimum axis-step (interval) is 1
        xAxis.setValueFormatter(formatter);
        barChart.getXAxis().setTextColor(Color.WHITE);
        barChart.getXAxis().setTextSize(16f);
        //barChart.setFitBars(true);
        barChart.setData(barData);
        barChart.animateY(2000);
        barChart.setDrawValueAboveBar(false);
        barChart.getAxisLeft().setEnabled(false);
        barChart.getAxisRight().setEnabled(false);
        barChart.getDescription().setEnabled(false);
        barChart.getLegend().setEnabled(true);
        barChart.getAxisLeft().setAxisMaximum(100);
        barChart.getAxisLeft().setAxisMinimum(0);

        Legend legend = barChart.getLegend();
        legend.setFormSize(10f);
        legend.setTextColor(Color.WHITE);
        legend.setTextSize(10f);
        legend.setXEntrySpace(10f);
        LegendEntry legendEntryA = new LegendEntry();
        LegendEntry legendEntryB = new LegendEntry();
        LegendEntry legendEntryC = new LegendEntry();
        LegendEntry legendEntryD = new LegendEntry();

        legendEntryA.label = "Xception";
        legendEntryA.formColor = ColorTemplate.MATERIAL_COLORS[0];
        legendEntryB.label = "NL-CNN";
        legendEntryB.formColor = ColorTemplate.MATERIAL_COLORS[1];
        legendEntryC.label = "MobileNetV2";
        legendEntryC.formColor = ColorTemplate.MATERIAL_COLORS[2];
        legendEntryD.label = "EfficientNetB0";
        legendEntryD.formColor = ColorTemplate.MATERIAL_COLORS[3];
        legend.setCustom(Arrays.asList(legendEntryA, legendEntryB, legendEntryC, legendEntryD));

        barChart.invalidate();


    }
}