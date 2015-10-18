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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.EnumMap;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.content.Context;
//

/**
 * Quaestor provides global state management for the application
 * <p>
 * Quaestor is a product of the evolutionary nature of this design, i.e. it served as the bucket
 * into which much cross object state was dumped.
 * <p>
 * Quaestor is realized as a Singleton - it will be created on demand
 */
public class Quaestor {
	private static Quaestor mSingleton;
	public static Quaestor Singleton() {
		if(mSingleton == null)
			mSingleton = new Quaestor();
		return mSingleton;
	}

	/**
	 * Private constructor for initializing the quaestor singleton
	 */
	private Quaestor()
	{
		mForceQueueing = false;
		mEnableCloud = true;
		mCloudServiceAvailable = false;
		// intake pump dropped/accepted counts
		mIntakePumpCounts = new EnumMap<Tricorder.TricorderSensorStream, Integer[]>(Tricorder.TricorderSensorStream.class);
		mIntakePumpCounts.put(Tricorder.TricorderSensorStream.Location,new Integer[] {0,0});
		mIntakePumpCounts.put(Tricorder.TricorderSensorStream.Picture,new Integer[] {0,0});
		mIntakePumpCounts.put(Tricorder.TricorderSensorStream.Points,new Integer[] {0,0});
		mIntakePumpCounts.put(Tricorder.TricorderSensorStream.Pose,new Integer[] {0,0});
		mIntakePumpCounts.put(Tricorder.TricorderSensorStream.Session,new Integer[] {0,0});
		
		mDeviceId = getUniqueID(Tricorder.singleton);
		
		mCloudServiceChecked = false;
		Thread t = new Thread() {
		    public void run() {
		    	mCloudServiceAvailable = checkCloudService();
				mCloudServiceChecked = true;
		    }
		};
		t.start();
	}

	/**
	 * Get a more or less unique ID for this device that will hopefully remain consistant over its
	 * lifetime.
	 *
	 * @param context  The execution context
	 * @return the secure ANDROID_ID in the context
	 */
	 public static String getUniqueID(Context context) {

		 return android.provider.Settings.Secure.getString(context.getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
	    }
	//

	/**
	 * Returns true if cloud services are available.
	 * @return True if the proof of life cloud service can be contacted and returns success
	 */
	private Boolean checkCloudService() {
        try {
            DefaultHttpClient httpclient = new DefaultHttpClient();
			String appbase =  Tricorder.serviceBaseUrl;
            HttpGet httpGetRequest = new HttpGet(appbase + "/api/tango/service");
            httpGetRequest.setHeader("Accept", "application/json");
            HttpResponse response = httpclient.execute(httpGetRequest);
			return (response.getStatusLine().getStatusCode() == 200);
        } catch (UnsupportedEncodingException ignored) {
        } catch (ClientProtocolException ignored) {
        } catch (IOException ignored) {
        }
		return false;
	}

	/**
	 * True if the cloud services are available and the user hasn't turned off cloud access.
	 * @return True if the app has an active connection to the cloud services.
	 */
	public Boolean isUsingCloud() { return mCloudServiceChecked && mCloudServiceAvailable && mEnableCloud; }

	/**
	 * Unique device identifier
	 */
	String mDeviceId;
	/**
	 * Intake data will always be queued, even when pumps are idle.
	 * <p>
	 * used to test queuing systems - generates maximum memory strain.
	 */
	public Boolean mForceQueueing;
	/**
	 * this can be used to disable cloud uploads even when the service is available.
	 */
	public Boolean mEnableCloud;
	/**
	 * availability of the cloud upload service.
	 */
	public Boolean mCloudServiceAvailable;
	/**
	 * true if service check has completed, otherwise must assume not available.
	 */
	public Boolean mCloudServiceChecked;
	/**
	 * count of intake items dropped because of queue full.
	 */
	public EnumMap<Tricorder.TricorderSensorStream,Integer[]> mIntakePumpCounts;
}
