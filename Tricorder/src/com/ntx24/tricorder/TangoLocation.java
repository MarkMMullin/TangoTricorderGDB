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

/** GPS Location report object for Tango */
public class TangoLocation   extends TangoObject {
	public double latitude;
	public double longitude;
	public double elevation;
	public String provider;
	/** Construct a TangoLocation from base data */
	public TangoLocation(double tangoTime,double lat,double lon,double elev,String prov) {
		super(tangoTime);
		latitude = lat;
		longitude = lon;
		elevation = elev;
		provider = prov;
	}
    /** Construct a TangoLocation from previously serialized instance on stream */
	public TangoLocation(DataInputStream s) throws IOException {
		super(s);
		latitude = s.readDouble();
		longitude = s.readDouble();
		elevation = s.readDouble();
		provider = s.readUTF();
	}
    /** Convert instance to equivalent JSON representation*/
	@Override 
	public String toJSONString() {
		try {
			return GenerateJSON().toString();
		} catch (JSONException e) {
		}
		return null;
	}
    /** Convert instance to equivalent JSONObject instance */
	@Override
	public JSONObject GenerateJSON() throws JSONException {
        JSONObject jobj = super.GenerateJSON();
        try {
            jobj.put("latitude", latitude);
            jobj.put("longitude", longitude);
            jobj.put("elevation", elevation);
            jobj.put("provider", provider);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jobj;
	}
    /** Defines the endpoint that location data is delivered to */
	@Override
	public String endpoint(String baseUrl) {
		return remoteEndpoint(baseUrl);	
	}
    /** Defines the local file that location data is recorded to */
	public static String localFilename() { return "Location.dat";}
    /** Defines the endpoint that location data is delivered to */
	public static String remoteEndpoint(String baseUrl) { return baseUrl + "/api/tango/location";}
    /** Write data in binary form to the local output file */
	@Override
	public void publishToFile(DataOutputStream s) throws IOException {
		super.publishToFile(s);
		s.writeDouble(latitude);
		s.writeDouble(longitude);
		s.writeDouble(elevation);
		s.writeUTF(provider);
		s.flush();
	}

	
}
