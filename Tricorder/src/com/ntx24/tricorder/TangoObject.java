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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Date;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;
/**
 * The abstract base class of all Tango objects handled by Tricorder,
 * <p>
 * {@code TangoObject} is an abstract base class that defines the common
 * characteristics of all Tango data objects.  The base characteristics of all
 * objects is that they have both Android and Tango timestamps, and the id of
 * the device the data originated from.
 * <p>
 * This class provides mechanisms to handle serialization of data defined at
 * this level and equivalent {@code JSONObject} representations.
 * <h4>Implementation notes</h4>
 *
 */
public abstract class TangoObject {
	/**
	 * Last time data delivered
	 */
	public static HttpResponse lastResponse;
	/**
	 * used to hold latest tango timestamp for locations and sessions,
	 * which don't have one directly
	 */
    public static double latestTimestamp;
	/**
	 * Device UTC timestamp
	 */
	Date At;
	/**
	 * Tango internal timestamp
	 */
	double timestamp;
	/**
	 * 64 bit ID in hex form (16 chars)
	 */
	String device;
	/**
	 * Create a new object with the given tango timestamp and the Date timestamp initialized to present UTC time
	 *
	 * @param stamp  The double value used by Tango to timestamp data elements
	 */
	public TangoObject(double stamp) {
		At = new Date();
		TangoObject.latestTimestamp = timestamp = stamp;
		device = Quaestor.Singleton().mDeviceId;
	}
	// Rehydrate a tango object from a file stream - meant for shipping out previously captured local data
    /**
     * Rehydrate a new object from the provided low level data stream
     *
     * @param s  System will read device timestamp, tango timestamp, and device ID in raw form
     */
    public TangoObject(DataInputStream s) throws IOException {
		At = new Date(s.readLong());
		timestamp = s.readDouble();
		device = s.readUTF();
	}
    /**
     * Generate the base JSON elements that appear in all Tango JSON object, the device, at, and timestamp parameters.
     * <p>
     * This function is easily expressed, but gruesomely inefficient
     *
     * @return {@code JSONObject} representation of the abstract android and tango timestamps
     * and the device identifier
     * @throws JSONException
     */
	public JSONObject GenerateJSON() throws JSONException {
		JSONObject jobj = new JSONObject ();
		jobj.put("device",device);
		jobj.put("at", At.getTime());
        jobj.put("timestamp", timestamp);
		return jobj;
	}
	//
	//
    /**
     * Generate the base JSON elements that appear in all Tango JSON object, the device, at, and timestamp parameters.
     * <p>
     * Generate the JSON directly - this is to help address the abysmal performance of
     * {@code JSONObject.toString()).
     * <p>
     *     nb this returns the leading part of the object, i.e. the json is technically illegal
     * @return {@code JSONObject} representation of the abstract android and tango timestamps
     * and the device identifier
     * @throws JSONException
     */
	public String toJSONString() {
		StringBuilder sb = new StringBuilder();
		sb.append('{');
		sb.append('"');sb.append("device");sb.append('"');
		sb.append(':');
		sb.append('"');sb.append(device);sb.append('"');
		sb.append(',');
		

		sb.append('"');sb.append("at");sb.append('"');
		sb.append(':');
		sb.append('"');sb.append(At.getTime());sb.append('"');
		sb.append(',');
		
		sb.append('"');sb.append("timestamp");sb.append('"');
		sb.append(':');
		sb.append('"');sb.append(timestamp);sb.append('"');
		sb.append(',');
		
		return sb.toString();
	}

    /**
     * Identifies the URL of a service that will accept the data via POST calls
     * @param baseUrl  The base URL to be prepended to the relative service path.
     * @return  Text of response from server
     */
    public abstract String endpoint(String baseUrl);

    /**
     * Write the At, timestamp, and device values directly to local filestream
     * @param s
     * @throws IOException
     */
	public void publishToFile(DataOutputStream  s) throws IOException {
		s.writeLong(At.getTime());
		s.writeDouble(timestamp);
		s.writeUTF(device);
	}

    /**
     * Write the data directly to a service endpoint.
     * The (@code toJSONString()) function is used to generate the correct JSON for the deepest
     * derivation of the current object
     */
	public void publishToEndpoint() {
        try {
            DefaultHttpClient httpclient = new DefaultHttpClient();
            String appbase =  Tricorder.serviceBaseUrl;
            HttpPost httpPostRequest = new HttpPost(endpoint(appbase));
            StringEntity se = new StringEntity(toJSONString());
            httpPostRequest.setEntity(se);
            httpPostRequest.setHeader("Accept", "application/json");
            httpPostRequest.setHeader("Content-type", "application/json");
            //httpPostRequest.setHeader("Accept-Encoding", "gzip"); // only set this parameter if you would like to use gzip compression
            HttpResponse response = (HttpResponse) httpclient.execute(httpPostRequest);
			lastResponse = response;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
	}

}
