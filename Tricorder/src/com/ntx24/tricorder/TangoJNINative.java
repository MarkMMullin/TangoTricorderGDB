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

/**
 * Interfaces between C and Java.
 */
public class TangoJNINative {


    public static native int initialize(Tricorder activity,Gibraltar gibraltarInstance);

    public static native void setupConfig(boolean isAutoRecovery);

    public static native void connectTexture();

    public static native int connectService();

    public static native void disconnectService();

    public static native void onDestroy();

    public static native void setupGraphic();

    public static native void setupViewport(int width, int height);
    public static native void setupMeshViewport(int width, int height);

    public static native void render();
    
    public static native void renderMesh();

    public static native void resetMotionTracking();

    public static native byte updateStatus();

    public static native String getPoseString();

    public static native String getVersionNumber();
    
    public static native boolean getIsLocalized();


    public static native float startSetCameraOffset();

    public static native float setCameraOffset(float rotX, float rotY, float zDistance);
    
    public static native String[] getAvailableADFs(Tricorder activity);
    
    public static native boolean selectADF(String adfName);
    
    public static native boolean saveADF();
    
    public static native double[] getIMUFrame();
    
    public static native double[] getCameraIntrinsics();
    
    public static native void setConfigFlag(int flagId,boolean flagState);
    
    public static native void setCapturePointDisplayCoefficients(double ptSize,double depthRange);
}

