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

import org.json.JSONException;
import org.json.JSONObject;

public class TangoPoseData extends TangoObject {
	private static boolean mPrimed = false;
	public static double mminX,mmaxX,mminY,mmaxY,mminZ,mmaxZ;
	static int[] getProcessedArea() {
		int[] result = new int[3];
		result[0] = (int) ((mmaxX - mminX) * 100);
		result[1] = (int) ((mmaxY - mminY) * 100);
		result[2] = (int) ((mmaxZ - mminZ) * 100);
		return result;
	}
	
	Double timestamp;
	byte quality;
	Double posX,posY,posZ;
	Double attX,attY,attZ,attW;
	String poseContext;
	public TangoPoseData(double[] rawPoseData,String frameOfReference) {
		super(rawPoseData[0]);
		/*
		 * Pose Raw Ordering
		 * 		Timestamp
		 * 		pose quality (0..255)
		 * 		PosX
		 * 		PoxY
		 * 		PosZ
		 * 		AttX
		 * 		AttY
		 * 		AttZ
		 * 		AttW
		 */
		quality = (byte) ((int) rawPoseData[1]);
		posX = rawPoseData[2];posY = rawPoseData[3];posZ = rawPoseData[4];
		attX = rawPoseData[5];attY = rawPoseData[6];attZ = rawPoseData[7];attW = rawPoseData[8];
		poseContext = frameOfReference;
		if(mPrimed) {
			if(posX < mminX) mminX = posX; else if (posX > mmaxX) mmaxX = posX;
			if(posY < mminY) mminY = posY; else if (posY > mmaxY) mmaxY = posY;
			if(posZ < mminZ) mminZ = posZ; else if (posZ > mmaxZ) mmaxZ = posZ;
		} else {
			mminX = mmaxX = posX;
			mminY = mmaxY = posY;
			mminZ = mmaxZ = posZ;
			mPrimed = true;
		}
	}
	public TangoPoseData(DataInputStream s) throws IOException {
		super(s);
		quality = s.readByte();
		posX = s.readDouble();
		posY = s.readDouble();
		posZ = s.readDouble();
		attX = s.readDouble();
		attY = s.readDouble();
		attZ = s.readDouble();
		attW = s.readDouble();
		poseContext = s.readUTF();
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
    public JSONObject GenerateJSON() throws JSONException {
        JSONObject jobj = super.GenerateJSON();
        try {
            jobj.put("Quality",quality);
            jobj.put("Px", posX);
            jobj.put("Py", posY);
            jobj.put("Pz", posZ);
            jobj.put("AQ0", attX);
            jobj.put("AQ1", attY);
            jobj.put("AQ2", attZ);
            jobj.put("AQ3", attW);
            jobj.put("FrameOfReference", poseContext);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jobj;
    }

	@Override
	public String endpoint(String baseUrl) {
		return remoteEndpoint(baseUrl);
	}
	public static String localFilename() { return "Pose.dat";}
	public static String remoteEndpoint(String baseUrl) { return baseUrl + "/api/tango/pose";}

	@Override
	public void publishToFile(DataOutputStream s) throws IOException {
		super.publishToFile(s);
		s.writeByte(quality);
		s.writeDouble(posX);
		s.writeDouble(posY);
		s.writeDouble(posZ);
		s.writeDouble(attX);
		s.writeDouble(attY);
		s.writeDouble(attZ);
		s.writeDouble(attW);
		s.writeUTF(poseContext);
		s.flush();
	}
}
