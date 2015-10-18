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

import java.io.IOException;
/**
 * Realizes the JPEG picture data pump for the system as a derivative of {@code TangoPump}.
 * <p>
 * This pump will either publish data from the picture queue to the relevant endpoint
 * or local file, depending on whether the system has a live connection to the cloud
 * service.
 */
public class PicturePump extends TangoPump {
	/**
	 * Initialize the picture pump using the given app reference and the local filename
	 * defined in (@code TangoPicture)
	 * @param myHost  Reference to host application
	 */
	public PicturePump(Gibraltar myHost) {
		super(myHost,TangoPicture.localFilename());
	}
	/**
	 * Dequeue elements from the processed picture queue and deliver them as they become available.
	 * <p>
	 * This will deliver information either to the endpoint of the web service or to a local
	 * file depending on whether or not the application is using the cloud.
	 */
	@Override
	public void run() {
		TangoPicture pd;
		while(host.isPumping) {
			try {
				pd = host.GetPictureQueue().take();
				if(Quaestor.Singleton().isUsingCloud())
					pd.publishToEndpoint();
				else
					pd.publishToFile(getLocalOutputStream());
			} catch (InterruptedException e) {
				e.printStackTrace();
				continue;
			} catch (IOException e) {
				e.printStackTrace();
			} 
		}
	}

}
