package com.example.sujay.extendscreen.ImageProcessing;

import android.app.Activity;
import android.content.Context;
import android.hardware.Camera;
import android.util.AttributeSet;

import org.opencv.android.JavaCameraView;

import java.util.List;

/**
 * Created by sujay on 16/8/15.
 */
public class OpenCVCameraView extends JavaCameraView
{

        public OpenCVCameraView(Context context, AttributeSet attrs)
        {
            super(context, attrs);
        }
        public List<Camera.Size> getResolutionList() {
            if(mCamera!=null)
                return mCamera.getParameters().getSupportedPreviewSizes();
            return null;
        }

        public void setResolution(Camera.Size resolution) {
            disconnectCamera();
            mMaxHeight = resolution.height;
            mMaxWidth = resolution.width;
            connectCamera(getWidth(), getHeight());
        }

        public Camera.Size getResolution() {
            if(mCamera!=null)
                return mCamera.getParameters().getPreviewSize();
            return null;
        }

}
