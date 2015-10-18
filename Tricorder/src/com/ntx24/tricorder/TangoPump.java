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
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import android.content.Context;
/**
 * Realizes the abstract base for all data pumps in the system
 * such as {@code PosePump}.
 * <p>
 * {@code TangoPump} is a {@code Runnable} that is used as the base class for all of
 * the independent data pump thread drivers.
 */
public abstract class TangoPump implements Runnable {
	/**
	 * Application host context
	 */
	Gibraltar host;
	/**
	 * Name of local output file
	 */
	String localFilename;
	/**
	 * Local file output stream
	 */
	private FileOutputStream outputFile;
	/**
	 * Remote web service output stream
	 */
	private DataOutputStream dataOutputStream;


	/**
	 * Base initialization binds host and file name information
	 * @param myHost  Reference to the host application context
	 * @param localfile  Defines the name of the local file
	 */
	public TangoPump(Gibraltar myHost,String localfile) {
		host = myHost;
		localFilename = localfile;
	}

	/**
	 *
	 * @return
	 * @throws IOException
	 */
	DataOutputStream getLocalOutputStream() throws IOException {
		if(dataOutputStream != null)
			return dataOutputStream;
		outputFile = Tricorder.singleton.openFileOutput(localFilename, Context.MODE_APPEND);
		dataOutputStream = new DataOutputStream(outputFile);
		return dataOutputStream;
	}

	/**
	 *
	 * @throws IOException
	 */
	void closeLocalOutputStream() throws IOException {
		if(dataOutputStream == null)
			return;		
		dataOutputStream.close();
		dataOutputStream = null;
		outputFile = null;
	}
}
