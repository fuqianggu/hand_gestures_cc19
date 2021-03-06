package com.eneaceolini.aereader;

import android.app.Activity;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;

import smile.classification.*;
import smile.math.kernel.LinearKernel;


/**
 * Created by Alex on 7/3/2017.
 */

public class Classifier {
    private String TAG = "Classifier";

    static int numFeatures = 6;
    static double[][] trainVectorP;
    static LDA lda;
    static SVM svm;
    static LogisticRegression logit;
    static DecisionTree tree;
    static NeuralNetwork net;
    static KNN knn;
    static AdaBoost forest;

    double[] features;
    static int[] classes;
    static Activity activity;

    int samples = 100;

    private static boolean trained = false;

    static int choice = 0; //default lda
    static int choice2;

    private int prediction;
    static private int classSize;

    //classifier trained booleans (just 1 for now to test)
    static boolean trainedLDA;
    static boolean trainedSVM;
    static boolean trainedLOGIT;
    static boolean trainedTREE;
    static boolean trainedNET;
    static boolean trainedKNN;
    static boolean trainedFOREST;

    static boolean trained2 = false;

    static int nIMUSensors = 0;

    double[] mins;
    double[] maxs;

    static FeatureCalculator fcalc2 = new FeatureCalculator();

    public Classifier(Activity activity) {
        this.activity = activity;
    }

    public Classifier() {

    }

    public static void reset(){//reset button from ClassificationFragment
        classes = null;
        trainVectorP = null;
        trained = false;

        trainedLDA = false;
        trainedSVM = false;
        trainedLOGIT = false;
        trainedTREE = false;
        trainedNET = false;
        trainedKNN = false;
        trainedFOREST = false;
    }

    public void setnIMUSensors(int imus){
        nIMUSensors = imus;
    }

    public void Train(ArrayList<DataVector> trainVector, ArrayList<Integer> Classes) {

        classSize = Classes.size();
        classes = new int[classSize];
        trainVectorP = new double[trainVector.size()][numFeatures * 8 + nIMUSensors];//make this dynamic yo
        for (int i = 0; i < trainVector.size(); i++) {
            for (int j = 0; j < numFeatures * 8 + nIMUSensors; j++) {
                trainVectorP[i][j] = trainVector.get(i).getValue(j).doubleValue();//invalid index 8 size is 8
            }
        }

        for (int j = 0; j < Classes.size(); j++) {
            classes[j] = Classes.get(j);
        }

        trained = true;
        trained2 = true;
        switch (choice) {
            case 0:
                trainLDA();
                break;
            case 1:
                trainSVM();
                break;
            case 2:
                trainLogit();
                break;
            case 3:
                trainTree();
                break;
            case 4:
                trainNet();
                break;
            case 5:
                trainKNN();
                break;
            case 6:
                trainAdaBoost();
                break;
        }
    }

    public void setChoice(int newChoice) {
        trained2 = false;

        if (trained) {//must re train if the a new classifier is chosen.. NEED feature that checks if one has already been trained so it doesnt train the same one twice!!!

            switch (newChoice) {
                case 0:
                    trainLDA();
                    Log.d(TAG, "Cross Validation LDA choice: ");
//                    ArrayList<Float> tempLDA = crossAccuracy(fcalc2.getSamplesClassifier(), fcalc2.getGesturesSize(), 5);
                    break;
                case 1:
                    trainSVM();
                    Log.d(TAG, "Cross Validation SVM choice: ");
//                    ArrayList<Float> tempSVM = crossAccuracy(fcalc2.getSamplesClassifier(), fcalc2.getGesturesSize(), 5);
                    break;
                case 2:
                    trainLogit();
                    Log.d(TAG, "Cross Validation Logit choice: ");
//                    ArrayList<Float> tempLogit = crossAccuracy(fcalc2.getSamplesClassifier(), fcalc2.getGesturesSize(), 5);
                    break;
                case 3:
                    trainTree();
                    Log.d(TAG, "Cross Validation Tree choice: ");
//                    ArrayList<Float> tempTree = crossAccuracy(fcalc2.getSamplesClassifier(), fcalc2.getGesturesSize(), 5);
                    break;
                case 4:
                    trainNet();
                    Log.d(TAG, "Cross Validation Net choice: ");
//                    ArrayList<Float> tempNet = crossAccuracy(fcalc2.getSamplesClassifier(), fcalc2.getGesturesSize(), 5);
                    break;
                case 5:
                    trainKNN();
                    Log.d(TAG, "Cross Validation KNN choice: ");
//                    ArrayList<Float> tempKNN = crossAccuracy(fcalc2.getSamplesClassifier(), fcalc2.getGesturesSize(), 5);
                    break;
                case 6:
                    trainAdaBoost();
                    Log.d(TAG, "Cross Validation Forest choice: ");
//                    ArrayList<Float> tempForrest = crossAccuracy(fcalc2.getSamplesClassifier(), fcalc2.getGesturesSize(), 5);
                    break;
            }
        }
        choice = newChoice;
        trained2 = true;
    }

    public void featVector(DataVector Features) {
        features = new double[Features.getLength()];
        for (int i = 0; i < Features.getLength(); i++) {
            features[i] = Features.getValue(i).doubleValue();
        }
    }

    //if flag is turned on (found in newChoice), predict or else return 1000
    public int predict(DataVector Features) {

        //System.out.println(classes.length);

        featVector(Features);
        //Log.d("2", "222 : " + String.valueOf(trainVectorP.length) + " : " + String.valueOf(classes.length) + " : " + String.valueOf(features.length));
        if (trained2) {
            switch (choice) {
                case 0:
//                    Log.d(TAG, "LDA");
                    prediction = lda.predict(features);
                    break;
                case 1:
                    int lastPred = prediction;
                    prediction = svm.predict(features);
                    if(prediction>((classSize/100)-1)){
                        prediction = lastPred;
                    }
//                    Log.d(TAG, "SVM");
                    break;
                case 2:
//                    Log.d(TAG, "LOGIT");
                    prediction = logit.predict(features);
                    //Log.d(TAG, "Logistic Regression");
                    break;
                case 3:
//                    Log.d(TAG, "TREE");
                    prediction = tree.predict(features);
                    //Log.d(TAG, "Tree");
                    break;
                case 4:
//                    Log.d(TAG, "NET");
                    for(int i = 0;i<features.length;i++){
                        features[i] = (features[i]-mins[i])/(maxs[i]-mins[i]);
                    }
                    prediction = net.predict(features);
                    //Log.d(TAG, "Net");
                    break;
                case 5:
//                    Log.d(TAG, "KNN");
                    prediction = knn.predict(features);
                    //Log.d(TAG, "KNN");
                    break;
                case 6:
//                    Log.d(TAG, "FOREST");
                    prediction = forest.predict(features);
                    //Log.d(TAG, "AdaBoost");
                    break;
            }
//            Log.d("TIME", String.valueOf(System.currentTimeMillis() - MyoGattCallback.superTimeInitial));
            return prediction;
        }
        return -1;
    }

    public void trainLDA() {
        //if selected gestures is not zero
        if (!trainedLDA) {
            Log.d("tvp length: ", String.valueOf(trainVectorP.length));
            lda = new LDA(trainVectorP, classes, 0);
            trainedLDA = true;
        }
        choice = 0;
    }

    public void trainSVM() {
        if (!trainedSVM) {
            Toast.makeText(activity, "Training SVM", Toast.LENGTH_SHORT).show();
            svm = new SVM<>(new LinearKernel(), 10.0, classSize, SVM.Multiclass.ONE_VS_ALL);//classSize + 1
            svm.learn(trainVectorP, classes);
            svm.finish();
            trainedSVM = true;
        }
        choice = 1;
    }

    public void trainLogit() {
        if (!trainedLOGIT) {
            Toast.makeText(activity, "Training Logit", Toast.LENGTH_SHORT).show();
            //Log.d("2", "222" + String.valueOf(trainVectorP.length) + " : " + String.valueOf(classes.length));
            logit = new LogisticRegression(trainVectorP, classes,  0.0, 1E-5, 5000);
            trainedLOGIT = true;
        }
        Log.d("3", "333");
        choice = 2;
    }

    public void trainTree() {
        if (!trainedTREE) {
            tree = new DecisionTree(trainVectorP, classes, 350);//in theory, greater the integer: more accurate but slower | lower the integer: less accurate but faster however, i didn't notice a difference
            trainedTREE = true;
        }
    }

    public void trainNet() {
        if (!trainedNET) {
            double[][] normalized = Normalize(trainVectorP);
            net = new NeuralNetwork(NeuralNetwork.ErrorFunction.CROSS_ENTROPY, NeuralNetwork.ActivationFunction.SOFTMAX, trainVectorP[0].length, 150, classSize + 1);
            net.learn(normalized, classes);
            net.learn(normalized, classes);
            net.learn(normalized, classes);
            trainedNET = true;
        }
    }

    public void trainKNN() {
        Log.d(TAG, "Made it to KNN");
        if (!trainedKNN) {
            knn = KNN.learn(trainVectorP, classes, (int) Math.sqrt((double) classSize));
            trainedKNN = true;
        }
    }

    public void trainAdaBoost() {
        if (!trainedFOREST) {
            forest = new AdaBoost(trainVectorP, classes, 100, 64);
            trainedFOREST = true;
        }
    }

    private double[][] Normalize(double[][] feats){//to normalize data between interval [0,1]
        int rows = feats.length;//800
        int columns = feats[0].length;//48
        maxs = new double[columns];//feats[0];
        mins = feats[0];
        double[][] normalized = new double[rows][columns];

        for(int i=1;i<rows;i++){
            for(int j=0;j<columns;j++){
                if(feats[i][j]<mins[j]){
                    mins[j] = feats[i][j];
                }
                if(feats[i][j]>maxs[j]){
                    maxs[j] = feats[i][j];
                }
            }
//            System.out.print(mins[0]);
//            System.out.println(" : " + String.valueOf(i));
        }

        for(int i=0;i<rows;i++){
            for(int j=0;j<columns;j++){
                normalized[i][j] = (feats[i][j]-mins[j])/(maxs[j]-mins[j]);
            }
        }

        return normalized;
    }

}