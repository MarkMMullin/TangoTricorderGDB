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

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.zip.Deflater;
/**
 * Realizes the point cloud compressor data pump for the system as a derivative of {@code TangoPump}.
 * <p>
 * This pump will accept raw data points via the raw points queue, compress them into an output
 * structure, and place the resulting structure on the points output queue.
 */
public class PointCloudCompressorPump extends TangoPump {
	/**
	 * Initialize the point cloud compressor pump using the given app reference
	 * @param myHost  Reference to host application
	 */
	public PointCloudCompressorPump(Gibraltar myHost) {
		super(myHost,null);
		// TODO Auto-generated constructor stub
	}
	/**
	 * Dequeue elements from the raw pointcloud queue, compress and convert them,
	 * and place the created object on the points queue.
	 * <p>
	 * This will deliver information either to the endpoint of the web service or to a local
	 * file depending on whether or not the application is using the cloud.
	 */
	@Override
	public void run() {
		TangoPointCloud pd;
		while(host.isPumping) {
			try {
				pd = host.GetRawPointsQueue().take();
				// compress the raw data
				ByteBuffer byteBuffer = ByteBuffer.allocate(pd.rawData.length * 4);        
		        FloatBuffer floatBuffer = byteBuffer.asFloatBuffer();
		        floatBuffer.put(pd.rawData);
		        byte[] array = byteBuffer.array();
		        
				Deflater deflater = new Deflater();
				deflater.setInput(array);
			    deflater.finish();

			     ByteArrayOutputStream baos = new ByteArrayOutputStream();
			     byte[] buf = new byte[pd.rawData.length * 4];
			     while (!deflater.finished()) {
			         int byteCount = deflater.deflate(buf);
			         baos.write(buf, 0, byteCount);
			     }
			     deflater.end();

			     pd.compressedData = baos.toByteArray();
			     pd.numFloats = pd.rawData.length;
			     pd.rawData = null;
			     host.GetPointsQueue().put(pd);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				continue;
			} 
		}


	}

}
