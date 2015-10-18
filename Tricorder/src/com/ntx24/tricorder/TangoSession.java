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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class TangoSession extends TangoObject {
	double[] imuData;
	double[] camData;
	public TangoSession(double timestamp) {
		super(timestamp);
	}
	public TangoSession(DataInputStream s) throws IOException {
		super(s);
		int imuVals = s.readInt();
		imuData = new double[imuVals];
		for(int i = 0;i < imuVals;i++)
			imuData[i] = s.readDouble();
		int camVals = s.readInt();
		camData = new double[camVals];
		for(int i = 0;i < camVals;i++)
			camData[i] = s.readDouble();
		
	}
	@Override 
	public String toJSONString() {
		try {
			return GenerateJSON().toString();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	@Override
	public JSONObject GenerateJSON() throws JSONException {
		 JSONObject jobj = super.GenerateJSON();
	        try {
	        	// force the data arrays to load if they haven't already
	            getIMUData();getCamData();
	            JSONArray data;
	            int index = 0;
	            data = new JSONArray();
	            data.put(imuData[index++]);data.put(imuData[index++]);data.put(imuData[index++]);
	            jobj.put("devPos", data);
	            
	            data = new JSONArray();
	            data.put(imuData[index++]);data.put(imuData[index++]);data.put(imuData[index++]);data.put(imuData[index++]);
	            jobj.put("devAtt", data);
	            
	            data = new JSONArray();
	            data.put(imuData[index++]);data.put(imuData[index++]);data.put(imuData[index++]);
	            jobj.put("camPos", data);

	            
	            data = new JSONArray();
	            data.put(imuData[index++]);data.put(imuData[index++]);data.put(imuData[index++]);data.put(imuData[index++]);
	            jobj.put("camAtt", data);
	            
	            double[] camData = TangoJNINative.getCameraIntrinsics();
	            index = 0;
	            data = new JSONArray();
	            data.put(camData[index++]);data.put(camData[index++]);
	            jobj.put("imageSize", data);
	            
	            data = new JSONArray();
	            data.put(camData[index++]);data.put(camData[index++]);
	            jobj.put("focalDimensions", data);	   
	            
	            data = new JSONArray();
	            data.put(camData[index++]);data.put(camData[index++]);
	            jobj.put("principalPoint", data);    

	            data = new JSONArray();
	            data.put(camData[index++]);data.put(camData[index++]);data.put(camData[index++]);data.put(camData[index++]);data.put(camData[index++]);
	            jobj.put("distortion", data);   
	        } catch (JSONException e) {
	            e.printStackTrace();
	        }
	        return jobj;
	}

	@Override
	public String endpoint(String baseUrl) {
		return remoteEndpoint(baseUrl);
	}
	public static String localFilename() { return "Session.dat";}
	public static String remoteEndpoint(String baseUrl) { return baseUrl + "/api/tango/session";}
	@Override
	public void publishToFile(DataOutputStream  s) throws IOException {
		super.publishToFile(s);
        double[] imuData = TangoJNINative.getIMUFrame();
        s.writeInt(imuData.length);
        for(int i = 0;i < imuData.length;i++)
        	s.writeDouble(imuData[i]);
        
        double[] camData = TangoJNINative.getCameraIntrinsics();
        s.writeInt(camData.length);
        for(int i = 0;i < camData.length;i++)
        	s.writeDouble(camData[i]);
	}
	private double[] getIMUData() {
		if(imuData == null)
			imuData = TangoJNINative.getIMUFrame();
		return imuData;
	}
	private double[] getCamData() {
		if(camData == null)
			camData = TangoJNINative.getCameraIntrinsics();
		return camData;
	}
}
