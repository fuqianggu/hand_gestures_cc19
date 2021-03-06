package com.eneaceolini.aereader;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.eneaceolini.aereader.biases.IPot;
import com.eneaceolini.aereader.biases.IPotArray;
import com.eneaceolini.aereader.biases.PotArray;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvException;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.HOGDescriptor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * Created by User on 2/28/2017.
 */

public class DVSFragment extends Fragment implements CameraBridgeViewBase.CvCameraViewListener2 {
    Button btnStart;
    Button btnStop;
    Button btnBias;
    Button btnConnect;
    TextView textInfo;
    private UsbDevice device;
    private UsbManager usbManager;
    private ReadEvents readEvents;
    private Spinner biasSpinner;
    ImageView imageView;
    Handler handler;
    Runnable runnable;
    int width_image, height_image;
    private static final String BIAS_FAST = "Fast";
    private static final String BIAS_SLOW = "Slow";

    // opencv stuff
    private CameraBridgeViewBase cameraView;
    private Mat mRgba;

    BlockingQueue<ArrayList> blockingQueue = new LinkedBlockingDeque<>();
    BlockingQueue<Integer> blockingID = new LinkedBlockingDeque<>();


    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(getContext()) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    cameraView.enableView();
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    @Override
    public void onPause() {
        super.onPause();
        if (cameraView != null)
            cameraView.disableView();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (!OpenCVLoader.initDebug()) {
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, getContext(), mLoaderCallback);
        } else {
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final View v = inflater.inflate(R.layout.dvs_activity, container, false);
        assert v != null;
        cameraView = v.findViewById(R.id.camera_view);
        cameraView.setVisibility(SurfaceView.VISIBLE);
        cameraView.setCvCameraViewListener(this);

        btnStart = v.findViewById(R.id.start);
        btnStop = v.findViewById(R.id.stop);
        btnBias = v.findViewById(R.id.loadBias);
        btnConnect = v.findViewById(R.id.connect);
        textInfo = v.findViewById(R.id.info);
        biasSpinner = v.findViewById(R.id.spinner);

        DisplayMetrics dm = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(dm);
        width_image = dm.heightPixels / 2;
        height_image = dm.heightPixels / 2;


        btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    usbManager = (UsbManager) getActivity().getSystemService(Context.USB_SERVICE);

                    HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
                    Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();

                    device = deviceList.get(deviceList.keySet().iterator().next());
                    Log.d("DEVICE", "CONNECTED:: " + device.getDeviceName());
                    PendingIntent permissionIntent = PendingIntent.getBroadcast(getContext(), 0, new Intent(ACTION_USB_PERMISSION), 0);
                    IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
                    getActivity().registerReceiver(usbReceiver, filter);
                    usbManager.requestPermission(device, permissionIntent);
                    btnBias.setEnabled(true);
                    btnStart.setEnabled(true);
                    Toast.makeText(getContext(), "Connected to DVS128!", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(getContext(), "Could not open device", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btnStop.setEnabled(true);
                btnStart.setEnabled(false);
                readEvents.start();
                handler = new Handler();
            }
        });

        btnStop.setOnClickListener(v1 -> {
            readEvents.stop_thread();
            btnStart.setEnabled(true);
//            readEvents = new ReadEvents(getContext(), device, usbManager, 0, mRgba.height(), mRgba.width(), blockingQueue, blockingID);
        });

        btnBias.setOnClickListener(v12 -> {
            UsbInterface usbInterface = device.getInterface(0); // for DVS128

            UsbDeviceConnection connection = usbManager.openDevice(device);
            connection.claimInterface(usbInterface, true);

            String selectedBias = biasSpinner.getSelectedItem().toString();

            byte[] b = formatConfigurationBytes(selectedBias);

            int start = connection.controlTransfer(0, 0xb8, 0, 0, b, b.length, 0);
            Log.d("SEND BIAS", "" + start);
            Toast.makeText(getContext(), "Bias set!", Toast.LENGTH_SHORT).show();

        });
        return v;
    }

    public byte[] formatConfigurationBytes(String config) {
        // we need to cast from PotArray to IPotArray, because we need the shift register stuff

        PotArray potArray = new IPotArray();
        Log.d("BIAS", "Sending bias config:" + config);
        switch (config) {
            case BIAS_FAST:
                potArray.addPot(new IPot("cas", 11, IPot.Type.CASCODE, IPot.Sex.N, 1992, 2, "Photoreceptor cascode"));
                potArray.addPot(new IPot("injGnd", 10, IPot.Type.CASCODE, IPot.Sex.P, 1108364, 7, "Differentiator switch level, higher to turn on more"));
                potArray.addPot(new IPot("reqPd", 9, IPot.Type.NORMAL, IPot.Sex.N, 16777215, 12, "AER request pulldown"));
                potArray.addPot(new IPot("puX", 8, IPot.Type.NORMAL, IPot.Sex.P, 8159221, 11, "2nd dimension AER static pullup"));
                potArray.addPot(new IPot("diffOff", 7, IPot.Type.NORMAL, IPot.Sex.N, 132, 6, "OFF threshold, lower to raise threshold"));
                potArray.addPot(new IPot("req", 6, IPot.Type.NORMAL, IPot.Sex.N, 309590, 8, "OFF request inverter bias"));
                potArray.addPot(new IPot("refr", 5, IPot.Type.NORMAL, IPot.Sex.P, 969, 9, "Refractory period"));
                potArray.addPot(new IPot("puY", 4, IPot.Type.NORMAL, IPot.Sex.P, 16777215, 10, "1st dimension AER static pullup"));
                potArray.addPot(new IPot("diffOn", 3, IPot.Type.NORMAL, IPot.Sex.N, 209996, 5, "ON threshold - higher to raise threshold"));
                potArray.addPot(new IPot("diff", 2, IPot.Type.NORMAL, IPot.Sex.N, 13125, 4, "Differentiator"));
                potArray.addPot(new IPot("foll", 1, IPot.Type.NORMAL, IPot.Sex.P, 271, 3, "Src follower buffer between photoreceptor and differentiator"));
                potArray.addPot(new IPot("Pr", 0, IPot.Type.NORMAL, IPot.Sex.P, 217, 1, "Photoreceptor"));
                break;
            case BIAS_SLOW:
                potArray.addPot(new IPot("cas", 11, IPot.Type.CASCODE, IPot.Sex.N, 54, 2, "Photoreceptor cascode"));
                potArray.addPot(new IPot("injGnd", 10, IPot.Type.CASCODE, IPot.Sex.P, 1108364, 7, "Differentiator switch level, higher to turn on more"));
                potArray.addPot(new IPot("reqPd", 9, IPot.Type.NORMAL, IPot.Sex.N, 16777215, 12, "AER request pulldown"));
                potArray.addPot(new IPot("puX", 8, IPot.Type.NORMAL, IPot.Sex.P, 8159221, 11, "2nd dimension AER static pullup"));
                potArray.addPot(new IPot("diffOff", 7, IPot.Type.NORMAL, IPot.Sex.N, 132, 6, "OFF threshold, lower to raise threshold"));
                potArray.addPot(new IPot("req", 6, IPot.Type.NORMAL, IPot.Sex.N, 159147, 8, "OFF request inverter bias"));
                potArray.addPot(new IPot("refr", 5, IPot.Type.NORMAL, IPot.Sex.P, 6, 9, "Refractory period"));
                potArray.addPot(new IPot("puY", 4, IPot.Type.NORMAL, IPot.Sex.P, 16777215, 10, "1st dimension AER static pullup"));
                potArray.addPot(new IPot("diffOn", 3, IPot.Type.NORMAL, IPot.Sex.N, 482443, 5, "ON threshold - higher to raise threshold"));
                potArray.addPot(new IPot("diff", 2, IPot.Type.NORMAL, IPot.Sex.N, 30153, 4, "Differentiator"));
                potArray.addPot(new IPot("foll", 1, IPot.Type.NORMAL, IPot.Sex.P, 51, 3, "Src follower buffer between photoreceptor and differentiator"));
                potArray.addPot(new IPot("Pr", 0, IPot.Type.NORMAL, IPot.Sex.P, 3, 1, "Photoreceptor"));
                break;
        }


        // we make an array of bytes to hold the values sent, then we fill the array, copy it to a
        // new array of the proper size, and pass it to the routine that actually sends a vendor request
        // with a data buffer that is the bytes

        if (potArray instanceof IPotArray) {
            IPotArray ipots = (IPotArray) potArray;
            byte[] bytes = new byte[potArray.getNumPots() * 8];
            int byteIndex = 0;


            Iterator i = ipots.getShiftRegisterIterator();
            while (i.hasNext()) {
                // for each bias starting with the first one (the one closest to the ** FAR END ** of the shift register
                // we get the binary representation in byte[] form and from MSB ro LSB stuff these values into the byte array
                IPot iPot = (IPot) i.next();
                byte[] thisBiasBytes = iPot.getBinaryRepresentation();
                System.arraycopy(thisBiasBytes, 0, bytes, byteIndex, thisBiasBytes.length);
                byteIndex += thisBiasBytes.length;
            }
            byte[] toSend = new byte[byteIndex];
            System.arraycopy(bytes, 0, toSend, 0, byteIndex);
            return toSend;
        }
        return null;
    }


    private static final String ACTION_USB_PERMISSION =
            "com.android.example.USB_PERMISSION";

    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null) {
                            //call method to set up device communication
//                            readEvents = new ReadEvents(getContext(), device, usbManager, 0, mRgba.height(), mRgba.width(), blockingQueue, blockingID);
                            Log.d("DEVICE", "CONNECTED");
                        }
                    } else {
                        Log.d("TAG", "permission denied for device " + device);
                    }
                }
            }
        }
    };

    public Bitmap mat2Bit(Mat mat, int height, int width) {

    Bitmap bmp = null;
    Mat tmp = new Mat(height, width, CvType.CV_8U, new Scalar(4));
    try {
        Imgproc.cvtColor(mat, tmp, Imgproc.COLOR_GRAY2RGBA, 4);
        bmp = Bitmap.createBitmap(tmp.cols(), tmp.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(tmp, bmp);
    } catch(CvException e) {
        Log.d("Exception", e.getMessage());
    }
    return bmp;
    }


    @Override
    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat(height, width, CvType.CV_8UC4);
//        mDetector = new ColorBlobDetector();
//        mSpectrum = new Mat();
//        mBlobColorRgba = new Scalar(255);
//        mBlobColorHsv = new Scalar(255);
//        SPECTRUM_SIZE = new Size(200, 64);
//        CONTOUR_COLOR = new Scalar(255,0,0,255);
    }

    @Override
    public void onCameraViewStopped() {
        mRgba.release();
    }

    long lastTime = System.currentTimeMillis();

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Log.d("Frame rate", "" + 1000 / (System.currentTimeMillis() - lastTime));
        lastTime = System.currentTimeMillis();
        mRgba = inputFrame.rgba();
        mRgba = new Mat(mRgba.height(), mRgba.width(), CvType.CV_8UC4);
        ArrayList<DVS128Processor.DVS128Event> toDraw = new ArrayList<>();
        if (null != readEvents) {
            synchronized (this) {
                try {
                    if (blockingQueue.size() > 0) {
                        toDraw = blockingQueue.take();
                        Log.d("QUEUE SIZE", "" + blockingQueue.size());
                        Log.d("ID TAKEN", "" + blockingID.take());
                        if (toDraw.size() > 0)
                            Log.d("--Delta", "" + (toDraw.get(toDraw.size() - 1).ts - toDraw.get(0).ts));

                        mRgba = fillMat(toDraw, mRgba.height(), mRgba.width());
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        return mRgba;
    }

    public synchronized Mat fillMat(ArrayList<DVS128Processor.DVS128Event> events, int height, int width){
        DVS128Processor.DVS128Event e;
        int r;
        int g;
        Mat mat = new Mat(height, width, CvType.CV_8UC4);

        for (int i = 0; i < events.size(); i += 10) {
            e = events.get(i);
            if (null != e) {
                if (e.polarity > 0) {
                    r = 255;
                    g = 10;
                } else {
                    r = 10;
                    g = 255;
                }
                for (int j = 0; j < 4; j++) {
                    for (int k = 0; k < 4; k++) {
                        mat.put(e.x * 4 + j, e.y * 4 + k, r, g, 10, 255);
                    }
                }
            }
        }
        return mat;
    }

    private static double[] exportImgFeatures(Mat frame) {

        Mat mat = new Mat();

        Imgproc.cvtColor(frame, mat, Imgproc.COLOR_RGB2GRAY);
        Log.d("FRAME", "" + frame.type());
        Log.d("MAT", "" + mat.type());
//        for (int i = 0; i < rows; i++) {
//            for (int j = 0; j < cols; j++) {
//                mat.put(i, j, data[i * cols + j]);
//            }
//        }

        HOGDescriptor hog = new HOGDescriptor(
                new Size(28, 28), //winSize
                new Size(14, 14), //blocksize
                new Size(7, 7), //blockStride,
                new Size(14, 14), //cellSize,
                9); //nbins

        MatOfFloat descriptors = new MatOfFloat();
        hog.compute(mat, descriptors);

        float[] descArr = descriptors.toArray();
        double retArr[] = new double[descArr.length];
        for (int i = 0; i < descArr.length; i++) {
            retArr[i] = descArr[i];
        }
        return retArr;
    }
}
