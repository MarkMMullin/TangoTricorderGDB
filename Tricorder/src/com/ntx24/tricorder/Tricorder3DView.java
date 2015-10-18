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


package  com.ntx24.tricorder;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.opengl.GLSurfaceView;

import com.google.atap.tango.ux.TangoUx;
import com.google.atap.tango.ux.TangoUxLayout;

/**
 * AugmentedRealityView renders graphic content.
 */
public class Tricorder3DView implements GLSurfaceView.Renderer {
	public static final int ELearningMode = 1,EAutoRecovery=2,EDepthPerception=4,EMotionTracking = 8,EColorCameraOn = 16;
    public boolean isAutoRecovery;
    public boolean isCreated = false;

    public void onDrawFrame(GL10 gl) {
        TangoJNINative.render();
    }

    public void onSurfaceChanged(GL10 gl, int width, int height) {
    	TangoJNINative.setupViewport(width, height);
    }

    public void onSurfaceCreated(GL10 gl, EGLConfig config) {

        TangoJNINative.initialize(Tricorder.singleton, Gibraltar.Singleton());
        TangoJNINative.setupGraphic();
        TangoJNINative.setCapturePointDisplayCoefficients(Tricorder.singleton.getCapturePointSize(), Tricorder.singleton.getCaptureDepthRange());
         // ship down the configuration flags
        TangoJNINative.setConfigFlag(ELearningMode, Tricorder.singleton.mLearningModeOn);
        TangoJNINative.setConfigFlag(EAutoRecovery, Tricorder.singleton.mAutoRecoveryOn);
        TangoJNINative.setConfigFlag(EDepthPerception, Tricorder.singleton.mDepthPerceptionOn);
        TangoJNINative.setConfigFlag(EMotionTracking, Tricorder.singleton.mMotionTrackingOn);
        TangoJNINative.setConfigFlag(EColorCameraOn, true);
        TangoJNINative.setupConfig(true);
        // this connects the video overlay texture so that the camera view will be drawn
        TangoJNINative.connectTexture();
        TangoJNINative.connectService();
        if(Tricorder.singleton.mSelectedADF != null)
        	TangoJNINative.selectADF(Tricorder.singleton.mSelectedADF);
	}

}
