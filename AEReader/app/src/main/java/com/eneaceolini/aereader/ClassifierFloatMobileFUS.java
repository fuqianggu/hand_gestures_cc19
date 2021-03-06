/* Copyright 2019 The TensorFlow Authors. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
==============================================================================*/

package com.eneaceolini.aereader;

import android.app.Activity;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/** This TensorFlowLite classifier works with the float MobileNet model. */
public class ClassifierFloatMobileFUS extends ClassifierFUS {

  /**
   * An array to hold inference results, to be feed into Tensorflow Lite as outputs. This isn't part
   * of the super class, because we need a primitive array here.
   */
  private float[][] labelProbArray = null;

  /**
   * Initializes a {@code ClassifierFloatMobileNet}.
   *
   * @param activity
   */
  public ClassifierFloatMobileFUS(Activity activity, Device device, int numThreads)
      throws IOException {
    super(activity, device, numThreads);
    labelProbArray = new float[1][getNumLabels()];
  }

  @Override
  public int getFeatSize() {
    return 60 * 60 + 24;
  }

  @Override
  public int getImageSizeX() {
    return 60;
  }

  @Override
  public int getImageSizeY() {
    return 60;
  }

  @Override
  protected String getModelPath() {
    // you can download this file from
    // see build.gradle for where to obtain this file. It should be auto
    // downloaded into assets.
    return "cnn_200_emg+dvs_evs.tflite";
  }

  @Override
  protected String getLabelPath() {
    return "labels.txt";
  }

  @Override
  protected int getNumBytesPerChannel() {
    return 4; // Float.SIZE / Byte.SIZE;
  }

  @Override
  protected void addPixelValue(int pixelValue) {
    dvsData.putFloat(pixelValue  / 255.f);  // only one channel
  }

  @Override
  protected void addEMGValue(float EMGValue) {
    emgData.putFloat(EMGValue);  // only one channel
  }

  @Override
  protected float getProbability(int labelIndex) {
    return labelProbArray[0][labelIndex];
  }

  @Override
  protected void setProbability(int labelIndex, Number value) {
    labelProbArray[0][labelIndex] = value.floatValue();
  }

  @Override
  protected float getNormalizedProbability(int labelIndex) {
    return labelProbArray[0][labelIndex];
  }

  @Override
  protected void runInference() {

    Map<Integer, Object> outputMap = new HashMap<>();
    outputMap.put(0, labelProbArray);
    tflite.runForMultipleInputsOutputs(new Object[]{dvsData, emgData}, outputMap);
  }
}
