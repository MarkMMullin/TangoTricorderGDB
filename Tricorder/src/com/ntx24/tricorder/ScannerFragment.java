/*
 * Copyright (c) 2014-2015, Mark Mullin
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  * Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 *  * Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 *  * Neither the name of Tango Tricorder nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.ntx24.tricorder;


import java.text.DecimalFormat;
import java.text.NumberFormat;

import android.app.Fragment;
import android.graphics.Point;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.atap.tango.ux.TangoUx;

/** Fragment for the live data scanner view */
public class ScannerFragment extends Fragment   {
    public static final String ARG_FRAGMENT_ID = "fragment_number";
    Tricorder3DView arViewRenderer;
    private float[] touchStartPos = new float[2];
    private float[] touchCurPos = new float[2];

    private Point screenSize = new Point();
    View mRootView;
	public static ScannerFragment singleton;
    public ScannerFragment() {
    	singleton = this;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_scanner, container, false);
        mRootView = rootView;
        getActivity().setTitle("Tango Tricorder");
        Display display = Tricorder.singleton.getWindowManager().getDefaultDisplay();
        display.getSize(screenSize);
        GLSurfaceView arView = (GLSurfaceView) rootView.findViewById(R.id.surfaceview);
        arViewRenderer = new Tricorder3DView();
        arViewRenderer.isAutoRecovery = true;
        arView.setRenderer(arViewRenderer);
        Tricorder.singleton.updateLocalizationIndicator(false);
        if(Tricorder.singleton.mSelectedADF == null)
        	Tricorder.singleton.showMessage("No ADF loaded", Toast.LENGTH_LONG);
        else
            Tricorder.singleton.showMessage("ADF loaded", Toast.LENGTH_SHORT);
        bindTouchListener(rootView);
        return rootView;
    }

    private int changecount = 0;
    private int touchTarget = 0;
  private void bindTouchListener(View rootView) {
	rootView.setOnTouchListener(new View.OnTouchListener() {
        public boolean onTouch(View v, MotionEvent event) {
            int pointCount = event.getPointerCount();
            if (pointCount == 1) {
                switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN: {
                	changecount = 0;
                    touchStartPos[0] = event.getX(0);
                    touchStartPos[1] = event.getY(0);
                    if(touchStartPos[1] > 800 && touchStartPos[0] < 1700)
                    	touchTarget = 1;
                    else if(touchStartPos[0] > 1700 && touchStartPos[1] < 800)
                    	touchTarget = 2;
                    else
                    	touchTarget = 0;
                    break;
                }
                case MotionEvent.ACTION_MOVE: {
                	boolean dirty = false;
                    touchCurPos[0] = event.getX(0);
                    touchCurPos[1] = event.getY(0);   
                    float dx = touchCurPos[0] - touchStartPos[0];
                    float dy = touchCurPos[1] - touchStartPos[1];
                    if(touchTarget == 1) {
                        	Tricorder.singleton.adjustCaptureDepthRange(dx * 0.0005f);
                        	dirty = true;
                     } else if(touchTarget == 2) {
                        	Tricorder.singleton.adjustCapturePointSize(dy * 0.01f);
                       	dirty = true;
                      }
                      if(dirty) {
                    	  ++changecount;
                      	TangoJNINative.setCapturePointDisplayCoefficients(Tricorder.singleton.getCapturePointSize(), Tricorder.singleton.getCaptureDepthRange());
                       }
                      
                      touchStartPos[0] = touchCurPos[0];
                      touchStartPos[1] = touchCurPos[1];
                    break;
                }                
                case MotionEvent.ACTION_UP: {
                	if(changecount > 0) {
                        touchCurPos[0] = event.getX(0);
                        touchCurPos[1] = event.getY(0);
                        NumberFormat formatter = new DecimalFormat("#0.000");     
                        Tricorder.singleton.showMessage("Range = " + formatter.format(Tricorder.singleton.getCaptureDepthRange()) + " meters,Point = " + formatter.format(Tricorder.singleton.getCapturePointSize()), Toast.LENGTH_SHORT);
                        break;                	}

                }
                }
            }
            return true;

        }
});
}



	@Override
	public void onPause() {
		super.onPause();
		Tricorder.singleton.stopLocationService();
        if(Tricorder.singleton.mSelectedADF != null) {
            Boolean result = TangoJNINative.saveADF();
            Tricorder.singleton.showMessage(result ? "ADF saved" : "ADF Save failed",result ?  Toast.LENGTH_SHORT : Toast.LENGTH_LONG);
        }

		TangoJNINative.disconnectService();
	}

	@Override
	public void onResume() {
		super.onResume();
        Tricorder.singleton.startLocationService(Tricorder.singleton);
	}

}
	