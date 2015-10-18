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
 * High level logic for independent image conversion thread.
 * <p>
 * {@code ImageCompressorPump} is used to collect raw bitmaps
 * from the raw picture queue, convert them to JPEGS, and place
 * the results on the picture queue.
 * <h4>Implementation notes</h4>
 * .
 */
public class ImageCompressorPump extends TangoPump {
    /**
     * Create a new ImageCompressor pump instance.
     * <p>
     * The image compressor pump takes raw bitmaps off the raw picture queue, converts
     * them to JPEG form, and places the JPEG on the picture queue.
     */
	public ImageCompressorPump(Gibraltar myHost) {
		super(myHost,null);
		// TODO Auto-generated constructor stub
	}
    /**
     * Dequeue elements from the raw input bitmap queue, convert them to JPEGS,
     * and placing the JPEG on the picture queue.
     * <p>
     * This will deliver information either to the endpoint of the web service or to a local
     * file depending on whether or not the application is using the cloud.
     */
    @Override
	public void run() {
		RawPictureQueueItem pd;
		while(host.isPumping) {
			try {
				pd = host.GetRawPictureQueue().take();
				TangoPicture thePicture = new TangoPicture(pd.timestamp,pd.bitmap);
				host.GetPictureQueue().put(thePicture);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				continue;
			} 
		}
	}

}
