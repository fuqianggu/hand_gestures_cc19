package com.eneaceolini.aereader;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.github.mikephil.charting.charts.RadarChart;

import org.opencv.ml.SVM;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by User on 2/28/2017.
 */

public class FeatureFragment extends Fragment {
    private static final String TAG = "Tab2Fragment";
    private RadarChart mChart;
    private Plotter plotter;
    ListView listView_Features;

    Classifier classifier = new Classifier();

    //create an ArrayList object to store selected items
    ArrayList<String> selectedItems = new ArrayList<String>();


    //private int numIMU = 0;
    private int numFeats = 6;

    private FeatureCalculator fcalc = new FeatureCalculator();

    String[] featureNames = new String[]{
            "MAV",
            "RMS",
            "SD"
    };

    private static boolean[] featSelected = new boolean[]{true, true, true, true, true, true};

    private static boolean[] imuSelected = new boolean[]{false, false, false, false, false, false, false, false, false, false};

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.fragment_feature, container, false);
        assert v != null;

        listView_Features = v.findViewById(R.id.listView);

        listView_Features.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

        final List<String> FeaturesArrayList = new ArrayList<String>(Arrays.asList(featureNames));


        ArrayAdapter<String> adapter_features = new ArrayAdapter<String>(getActivity(), R.layout.mytextview, FeaturesArrayList);

        listView_Features.setAdapter(adapter_features);

        for (int i = 0; i < 3; i++) {
            listView_Features.setItemChecked(i, true);
            selectedItems.add(i, adapter_features.getItem(i));
        }

        listView_Features.setOnItemClickListener((parent, view, position, id) -> {

            // selected item
            String Features_selectedItem = ((TextView) view).getText().toString();

            if (selectedItems.contains(Features_selectedItem)) {
                featureManager(Features_selectedItem, false);
                selectedItems.remove(Features_selectedItem); //remove deselected item from the list of selected items
                classifier.numFeatures--;
                numFeats--;
                Log.d("NUM FEAT: ", "" + classifier.numFeatures);
            } else {
                featureManager(Features_selectedItem, true);
                selectedItems.add(Features_selectedItem); //add selected item to the list of selected items
                classifier.numFeatures++;
                numFeats++;
                Log.d("NUM FEAT: ", "" + classifier.numFeatures);
            }

            fcalc.setFeatSelected(featSelected);
            fcalc.setNumFeatSelected(numFeats);
            plotter.setFeatures(featSelected);

        });


        mChart = (RadarChart) v.findViewById(R.id.chart);
        plotter = new Plotter(null, mChart, null);//must pass chart from this fragment

        return v;
    }

    private void featureManager(String inFeature, boolean selected) {
        int index = 0;
        for (int i = 0; i < 6; i++) {
            if (inFeature == featureNames[i]) {
                index = i;
            }
        }
        featSelected[index] = selected;
    }

    public String[] getFeatureNames() {
        return featureNames;
    }

    public static boolean[] getFeatSelected() {
        return featSelected;
    }

}
