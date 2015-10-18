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

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;







import android.graphics.Bitmap;
import android.util.Base64;

/**
 * Realizes the JPEG output picture and associated data.
 * <p>
 *     raw data is array of object where first is double timestamp and
 *     second is the Bitmap the NDK built.
 */
public class TangoPicture  extends TangoObject{
	/**
	 * compressed JPEG file
	 */
	byte[] rawJPG;
	/**
	 * width of JPEG image
	 */
	int width;
	/**
	 * height of JPEG image
	 */
	int height;

	/**
	 * Constructor
	 * @param theTimestamp  Tango supplied timestamp
	 * @param theImage Raw bitmap image that will be converted to JPEG
	 */
	public TangoPicture(double theTimestamp,Bitmap theImage) {
		super(theTimestamp);
		TangoObject.latestTimestamp = timestamp = theTimestamp;
		width = theImage.getWidth();
		height = theImage.getHeight();
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();  
		theImage.compress(Bitmap.CompressFormat.JPEG, Tricorder.mJPEGQuality, byteArrayOutputStream);
		rawJPG = byteArrayOutputStream .toByteArray();
	}

	/**
	 * Construct a TangoPicture from a binary data stream
	 * @param s  The datastream
	 * @throws IOException
	 */
	public TangoPicture(DataInputStream s) throws IOException {
		super(s);
		width = s.readInt();
		height = s.readInt();
		int nbytes = s.readInt();
		rawJPG = new byte[nbytes];
		//noinspection ResultOfMethodCallIgnored
		s.read(rawJPG);
	}

	/**
	 * Convert a tango picture to a JSON string - the image itself is converted
	 * to base64 form.
	 * <p>
	 *     This is not the most efficient way to do things, a service that accepted raw jpegs
	 *     would be a lot better.  That said, it does have a certain symmetry.
	 * @return String representation of json object
	 */
	@Override 
	public String toJSONString() {
		//noinspection StringBufferReplaceableByString
		StringBuilder sb = new StringBuilder();
		
		sb.append(super.toJSONString());
		sb.append('"');sb.append("Width");sb.append('"');
		sb.append(':');
		sb.append('"');sb.append(width);sb.append('"');
		sb.append(',');
		
		sb.append('"');sb.append("Height");sb.append('"');
		sb.append(':');
		sb.append('"');sb.append(height);sb.append('"');
		sb.append(',');
		
		sb.append('"');sb.append("Depth");sb.append('"');
		sb.append(':');
		sb.append('"');sb.append(4);sb.append('"');
		sb.append(',');
		

		sb.append('"');sb.append("Image");sb.append('"');
		sb.append(':');
		sb.append('"');sb.append(Base64.encodeToString(rawJPG, Base64.DEFAULT));sb.append('"');
		sb.append('}');
		return sb.toString();
	}

	/**
	 * Generated json is {Width: width,Height: height,Depth: 4,Image - base 64 encoded JPEG image
	 * @return The (@class JSONObject) created from this instance
	 * @throws JSONException
	 */
	//
    public JSONObject GenerateJSON() throws JSONException {
        JSONObject jobj = super.GenerateJSON();
        try {
            jobj.put("Width", width);
            jobj.put("Height", height);
            jobj.put("Depth", 4);
            String encodedImage = Base64.encodeToString(rawJPG, Base64.DEFAULT);
            jobj.put("Image",encodedImage);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jobj;
    }

	/**
	 * returns the url for the service endpoint for delivering pictures
	 * @param baseUrl  The base URL to be prepended to the relative service path.
	 * @return Fully qualified service endpoint
	 */
	@Override
	public String endpoint(String baseUrl) {
		return remoteEndpoint(baseUrl);
	}

	/**
	 * Always returns (@code picture.dat) as the name of the local picture file
	 * @return the constant base name for picture data files
	 */
	public static String localFilename() { return "Picture.dat";}

	/**
	 * Generate the remote endpoint by appending the picture service relative url
	 * to the base url
	 * @param baseUrl The base URL to be prepended to the relative service path.
	 * @return  Fully qualified service reference
	 */
	public static String remoteEndpoint(String baseUrl) { return baseUrl + "/api/tango/picture";}

	/**
	 * Write raw binary data to a local file
	 * @param s  Active file output stream
	 * @throws IOException
	 */
	@Override
	public void publishToFile(DataOutputStream s) throws IOException {
		super.publishToFile(s);
		s.writeInt(width);
		s.writeInt(height);
		s.writeInt(rawJPG.length);
		s.write(rawJPG);
		s.flush();
	}
}
