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

import android.util.Base64;

/** Instance of a Tango 3D point cloud received from device */
public class TangoPointCloud  extends TangoObject{
	/** Data as delivered by Tango */
	float[] rawData;
    /** Compressed form of data */
	byte[] compressedData;
    /** Number of floating point values in compressed form */
	int numFloats;
    /** Instantiate a point cloud based on data received from Tango */
	public TangoPointCloud(double theTimestamp,float[] theData) {
		super(theTimestamp);
		TangoObject.latestTimestamp = timestamp = theTimestamp;
		rawData = theData;
	}
    /** Instantiate a point cloud based on a previously serialized instance */
	public TangoPointCloud(DataInputStream s) throws IOException {
		super(s);
		numFloats  = s.readInt();
		int datalen = s.readInt();
		compressedData = new byte[datalen];
		s.read(compressedData);
	}
    /** Generate JSON representation of the point cloud object */
	public String toJSONString() {
		StringBuilder sb = new StringBuilder();
		
		sb.append(super.toJSONString());
		sb.append('"');sb.append("NumFloats");sb.append('"');
		sb.append(':');
		sb.append('"');sb.append(numFloats);sb.append('"');
		sb.append(',');
		
		sb.append('"');sb.append("CompressedPoints");sb.append('"');
		sb.append(':');
		sb.append('"');sb.append(Base64.encodeToString(compressedData, Base64.DEFAULT));sb.append('"');
		sb.append('}');
		return sb.toString();

	}
    public JSONObject GenerateJSON() throws JSONException {
        JSONObject jobj = super.GenerateJSON();
        try {
            String base64 = Base64.encodeToString(compressedData, Base64.DEFAULT);
          jobj.put("NumFloats", numFloats);
          jobj.put("CompressedPoints", base64);
		  //StringBuilder sb = new StringBuilder();
          //sb.append(rawData[0]);
          //for(int i = 1;i < rawData.length ;i++) {
         // 	sb.append(',');
         // 	sb.append(rawData[i]);
         // }
          //jobj.put("PackedPoints", sb.toString());

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jobj;
    }
	public String endpoint(String baseUrl) {
		return remoteEndpoint(baseUrl);
	}
	public static String localFilename() { return "Pointcloud.dat";}
	public static String remoteEndpoint(String baseUrl) { return baseUrl + "/api/tango/points";}
	
	@Override
	public void publishToFile(DataOutputStream s) throws IOException {
		super.publishToFile(s);
		s.writeInt(numFloats);
		s.writeInt(compressedData.length);
		s.write(compressedData);	
		s.flush();
	}
}
