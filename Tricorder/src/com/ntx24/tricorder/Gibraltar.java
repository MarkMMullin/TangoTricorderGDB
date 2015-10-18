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
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.google.atap.tango.ux.TangoUx;
import com.google.atap.tangoservice.TangoEvent;
//
/**
 * The axle that the native and java elements revolve around.  This class serves as the
 * connection between the managed and unmanaged sides of the house
 * <p>
 * {@code LocalDate} is an immutable date-time object that represents a date, often viewed
 * as year-month-day. This object can also access other date fields such as
 * day-of-year, day-of-week and week-of-year.
 * <p>
 * This class does not store or represent a time or time-zone.
 * For example, the value "2nd October 2007" can be stored in a {@code LocalDate}.
 * <p>
 * The ISO-8601 calendar system is the modern civil calendar system used today
 * in most of the world. It is equivalent to the proleptic Gregorian calendar
 * system, in which todays's rules for leap years are applied for all time.
 * For most applications written today, the ISO-8601 rules are entirely suitable.
 * <p>
 * However, any application that makes use of historical dates and requires them
 * to be accurate will find the ISO-8601 rules unsuitable. In this case, the
 * application code should use {@code HistoricDate} and define an explicit
 * cutover date between the Julian and Gregorian calendar systems.
 *
 * <h4>Implementation notes</h4>
 * This class is immutable and thread-safe.
 */
public class Gibraltar {
	/**
	 * True when the system is actively shipping tango data to cloud or recording it to local
	 * storage.
	 * <p>
	 * Accessible because the need for speed dwarfs safety concerns - don't use propertie
	 */
	public Boolean isPumping;
	/**
	 * Queue that carries Tango Pose data delivered from the native library.
	 * <h4>Implementation notes</h4>
	 * Based on a blocking queue to simplify multithread operations and allow for a basic
	 * block on empty input design
	 */
	BlockingQueue<TangoPoseData> poseQueue;
	/**
	 * Queue that carries raw depth cloud points received from the native library.
	 * <h4>Implementation notes</h4>
	 * Based on a blocking queue to simplify multithread operations and allow for a basic
	 * block on empty input design
	 */
	BlockingQueue<TangoPointCloud> rawPointsQueue;
	/**
	 * Queue that carries the processed points data that is ready for export.
	 * <h4>Implementation notes</h4>
	 * Based on a blocking queue to simplify multithread operations and allow for a basic
	 * block on empty input design
	 */
	BlockingQueue<TangoPointCloud> pointsQueue;
	/**
	 * Queue that carries raw Tango camera bitmap images from the native library.
	 * <h4>Implementation notes</h4>
	 * Based on a blocking queue to simplify multithread operations and allow for a basic
	 * block on empty input design
	 */
	BlockingQueue<RawPictureQueueItem> rawPictureQueue;
	/**
	 * Queue that carries JPEGS generated from raw images that are ready for export.
	 * <h4>Implementation notes</h4>
	 * Based on a blocking queue to simplify multithread operations and allow for a basic
	 * block on empty input design
	 */
	BlockingQueue<TangoPicture> pictureQueue;
	/**
	 * Queue that carries location data (GPS) received from Android.
	 * <h4>Implementation notes</h4>
	 * Based on a blocking queue to simplify multithread operations and allow for a basic
	 * block on empty input design
	 */
	BlockingQueue<TangoLocation> locationQueue;
	/**
	 * Runnable object instance that handles unloading the pose pump and publishing the data
	 * to a file or a service.
	 */
	PosePump posePump;
	/**
	 * Runnable object instance that handles unloading the raw points, converting them
	 * and placing them on the points queue.
	 */
	PointCloudCompressorPump rawPointsPump;
	/**
	 * Runnable object instance that handles unloading the points queue and publishing the data
	 * to a file or a service.
	 */
	PointsPump pointsPump;
	/**
	 * Runnable object instance that handles unloading the picture queue and publishing the data
	 * to a file or a service.
	 */
	PicturePump picturePump;
	/**
	 * Runnable object instance that handles unloading the raw bitmap images, converting them
	 * to JPEGS and placing them on the picture queue.
	 */
	ImageCompressorPump rawPicturePump;
	/**
	 * Runnable object instance that handles unloading the location queue and publishing the data
	 * to a file or a service.
	 */
	LocationPump locationPump;
	/**
	 * Thread driving the output of pictures
	 */
	Thread picturePumpThread;
	/**
	 * Thread driving the conversion of raw bitmap pictures to JPEG pictures
	 */
	Thread rawPicturePumpThread;
	/**
	 * Thread driving the output of device poses
	 */
	Thread posePumpThread;
	/**
	 * Thread driving the conversion of raw point cloud data to exportable form
	 */
	Thread rawPointsPumpThread;
	/**
	 * Thread driving the output of processed point cloud data
	 */
	Thread pointsPumpThread;
	/**
	 * Thread driving the output of location data
	 */
	Thread locationPumpThread;
	/**
	 * Identifies the ADF that the pose is relative to.
	 * A zero UUID indicates there is no ADF.
	 */
	String referenceFrameIdentifier = "00000000-0000-0000-0000-000000000000";
    public Tricorder activity;
	private static Gibraltar singleton;

	/**
	 * Used to access the singleton instance of Gibralter
	 * @return Assumed preexisting singleton instance
	 */
	public static Gibraltar Singleton() { return singleton; }

	/**
	 *
	 * @param host Application that is hosting the singleton Gibralter instance
	 */
	public Gibraltar(Tricorder host) {
		singleton = this;
		activity = host;
		isPumping = false;
		poseQueue = new ArrayBlockingQueue<TangoPoseData>(Tricorder.singleton.mPoseQueueCapacity);
		rawPointsQueue = new ArrayBlockingQueue<TangoPointCloud>(Tricorder.singleton.mPointsQueueCapacity);
		pointsQueue = new ArrayBlockingQueue<TangoPointCloud>(Tricorder.singleton.mPointCompressorQueueCapacity);
		pictureQueue = new ArrayBlockingQueue<TangoPicture>(Tricorder.singleton.mPictureQueueCapacity);
		rawPictureQueue = new ArrayBlockingQueue<RawPictureQueueItem>(Tricorder.singleton.mPictureCompressorQueueCapacity);
		locationQueue = new ArrayBlockingQueue<TangoLocation>(Tricorder.singleton.mLocationQueueCapacity);

	}

	/**
	 * Gets the queue containing pose information
	 * @return A blocking queue of tango pose data
	 */
	public BlockingQueue<TangoPoseData> GetPoseQueue() { return poseQueue; }
	/**
	 * Gets the queue containing raw point cloud information
	 * @return A blocking queue of raw point cloud information
	 */
	public BlockingQueue<TangoPointCloud> GetRawPointsQueue() { return rawPointsQueue; }
	/**
	 * Gets the queue containing processed point cloud information
	 * @return A blocking queue of processed point cloud information
	 */
	public BlockingQueue<TangoPointCloud> GetPointsQueue() { return pointsQueue; }
	/**
	 * Gets the queue containing Tango images in JPEG form
	 * @return A blocking queue of Tango images in JPEG form
	 */
	public BlockingQueue<TangoPicture> GetPictureQueue() { return pictureQueue; }
	/**
	 * Gets the queue containing raw image bitmaps from Tango
	 * @return A blocking queue of raw image bitmaps from Tango
	 */
	public BlockingQueue<RawPictureQueueItem> GetRawPictureQueue() { return rawPictureQueue; }

	/**
	 * Gets the queue containing pose information
	 * @return A blocking queue of tango pose data
	 */	public BlockingQueue<TangoLocation> GetLocationQueue() { return locationQueue; }

	/**
	 * Test to see if the system is actively moving data to a service or a file
	 * @return  True if the pump threads are running
	 */
	public Boolean IsPumping() { return isPumping; }

	/**
	 * Test to see if the system has any data in it's queues
	 * @return True if there is still data moving through the system
	 */
	public Boolean IsPumpSystemEmpty() { return poseQueue.isEmpty() && rawPointsQueue.isEmpty() 
			&& pointsQueue.isEmpty() && rawPictureQueue.isEmpty() && pictureQueue.isEmpty() && locationQueue.isEmpty(); }

	/**
	 * Start the pump threads, which will begin the data export process.
	 * <p>
	 * Incoming data packets will now either be sent to the cloud endpoints or recorded to disk,
	 * depending on the connectivity state
	 */
	public void startPumps()
	{
		if(isPumping) 
			return;
		isPumping = true;	
		posePumpThread = new Thread(posePump = new PosePump(this));
		rawPointsPumpThread = new Thread(rawPointsPump = new PointCloudCompressorPump(this));
		pointsPumpThread = new Thread(pointsPump = new PointsPump(this));
		picturePumpThread = new Thread(picturePump = new PicturePump(this));
		rawPicturePumpThread = new Thread(rawPicturePump = new ImageCompressorPump(this));
		locationPumpThread = new Thread(locationPump = new LocationPump(this));
		
		posePumpThread.start();
		rawPointsPumpThread.start();
		pointsPumpThread.start();
		picturePumpThread.start();
		rawPicturePumpThread.start();
		locationPumpThread.start();
		if (BuildConfig.DEBUG) Log.v(Tricorder.TAG, "All pumps running");
		Tricorder.singleton.updateRecordingIndicator();
		Thread t1 = new Thread(new Runnable() {
			public void run() {
				TangoSession theSession = new TangoSession(TangoObject.latestTimestamp);
				if(Quaestor.Singleton().isUsingCloud())
					theSession.publishToEndpoint();
				else {
					try {
						FileOutputStream of = Tricorder.singleton.openFileOutput(TangoSession.localFilename(), Context.MODE_APPEND);
						DataOutputStream ds = new DataOutputStream(of);
						theSession.publishToFile(ds);
						ds.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		});
		t1.start();
	}
	/**
	 * Stop the pump threads and close any local output streams
	 */
	public void stopPumps()  {
		isPumping = false;
		try {
			Thread.sleep(500);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		if(!Quaestor.Singleton().isUsingCloud()) {
			try {
				posePump.closeLocalOutputStream();
				pointsPump.closeLocalOutputStream();
				picturePump.closeLocalOutputStream();
				locationPump.closeLocalOutputStream();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if (BuildConfig.DEBUG) Log.v(Tricorder.TAG, "All pumps off");
		Tricorder.singleton.updateRecordingIndicator();
	}

    int mLastPoseStatusCode = 0;
    boolean mLastPoseStatusChanged = false;
    int mLastCloudCount = 0;
    boolean mLastCloudCountChanged = false;
/*
   ####  ##  ###  #####           #####  ##  ### ####### ####### ######  #######   ##      ####  #######
     #    #   #     #               #     #   #  #  #  #  #    #  #    #  #    #    #     #    #  #    #
     #    ##  #     #               #     ##  #     #     #       #    #  #         #    #        #
     #    ##  #     #               #     ##  #     #     #  #    #    #  #  #     # #   #        #  #
     #    # # #     #               #     # # #     #     ####    #####   ####     # #   #        ####
     #    #  ##     #               #     #  ##     #     #  #    #  #    #  #    #   #  #        #  #
 #   #    #  ##     #               #     #  ##     #     #       #  #    #       #####  #        #
 #   #    #   #     #               #     #   #     #     #    #  #   #   #       #   #   #    #  #    #
  ###    ###  #   #####           #####  ###  #    ###   ####### ###  ## ####    ### ###   ####  #######
*/
	/**
	 * Called by the native library to deliver a pose
	 * @param rawPoseInfo Raw pose information from the native library
	 */
	public void queuePose(double[] rawPoseInfo,int statusCode) {
		TangoPoseData thePose = new TangoPoseData(rawPoseInfo,referenceFrameIdentifier);
		try {
			// discard the data if pumps aren't on and we're not forcing queuing
			if(!isPumping && !Quaestor.Singleton().mForceQueueing)
				return;
			Integer[] pumpInfo = Quaestor.Singleton().mIntakePumpCounts.get(Tricorder.TricorderSensorStream.Pose);
			if(poseQueue.remainingCapacity() == 0) {
				poseQueue.take();
				pumpInfo[0]++;
			}
			poseQueue.put(thePose);
			pumpInfo[1]++;
			Quaestor.Singleton().mIntakePumpCounts.put(Tricorder.TricorderSensorStream.Pose, pumpInfo);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		// report status
        mLastPoseStatusCode = statusCode;
        mLastPoseStatusChanged = true;
	}
    public void reportPoseStatus(int statusCode) {
        // report status
        mLastPoseStatusCode = statusCode;
        mLastPoseStatusChanged = true;
    }
	/**
	 * Called by the native library to deliver a raw point cloud
	 * @param timeStamp Time stamp for the point cloud
	 * @param rawPointInfo Raw point cloud information from the native library
	 */
	public void queuePoints(double timeStamp,float[] rawPointInfo) {
		// discard the data if pumps aren't on and we're not forcing queuing
		if(!isPumping && !Quaestor.Singleton().mForceQueueing)
			return;
		Integer[] pumpInfo = Quaestor.Singleton().mIntakePumpCounts.get(Tricorder.TricorderSensorStream.Points);
		TangoPointCloud thePoints = new TangoPointCloud(timeStamp,rawPointInfo);
		try {
			if(rawPointsQueue.remainingCapacity() == 0) {
				rawPointsQueue.take();
				pumpInfo[0]++;
			}
			rawPointsQueue.put(thePoints);
			pumpInfo[1]++;
			Quaestor.Singleton().mIntakePumpCounts.put(Tricorder.TricorderSensorStream.Points, pumpInfo);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
        mLastCloudCount = rawPointInfo.length;
        mLastCloudCountChanged = true;
	}
	/**
	 * Called by the native library to deliver a raw bitmap
	 * @param timeStamp Time stamp for the point cloud
	 * @param sourceImage Raw bitmap of camera image
	 */
	public void queuePicture(final double timeStamp,final Bitmap sourceImage) 
	{
		// discard the data if pumps aren't on and we're not forcing queuing
		if(!isPumping && !Quaestor.Singleton().mForceQueueing)
			return;
		Integer[] pumpInfo = Quaestor.Singleton().mIntakePumpCounts.get(Tricorder.TricorderSensorStream.Picture);
		try {
			if(rawPictureQueue.remainingCapacity() == 0) {
				rawPictureQueue.take();
				pumpInfo[0]++;
			}
			RawPictureQueueItem newItem = new RawPictureQueueItem(timeStamp,sourceImage);
			rawPictureQueue.put(newItem);
			pumpInfo[1]++;
			Quaestor.Singleton().mIntakePumpCounts.put(Tricorder.TricorderSensorStream.Picture, pumpInfo);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	/**
	 * java call to deliver GPS location
	 * @param l Preconstructed TangoLocation object, given the call originates in Java
	 */
	//
	public void queueLocation(TangoLocation l) 
	{
		// discard the data if pumps aren't on and we're not forcing queuing
		if(!isPumping && !Quaestor.Singleton().mForceQueueing)
			return;
		Integer[] pumpInfo =
				Quaestor.Singleton().mIntakePumpCounts.get(Tricorder.TricorderSensorStream.Location);
		try {
			if(locationQueue.remainingCapacity() == 0) {
				locationQueue.take();
				pumpInfo[0]++;
			}
			locationQueue.put(l);
			pumpInfo[1]++;
			Quaestor.Singleton().mIntakePumpCounts.put(Tricorder.TricorderSensorStream.Location, pumpInfo);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	/**
	 * Update UI to reflect change in localization status
	 * @param s1 Guid of active ADF - if null or empty, data is not localized
	 */
	public void signalLocalizationStateChange(String s1) {
    	final String s = s1;
        new Thread() {
            public void run() {
            	activity.runOnUiThread(new Runnable() {     
                    @Override
                    public void run() {
                    	if(s == null || s == "00000000-0000-0000-0000-000000000000")
                		{
                			Tricorder.singleton.updateLocalizationIndicator(false);
                			referenceFrameIdentifier = "00000000-0000-0000-0000-000000000000";
                		}
                		referenceFrameIdentifier = s;
                		Tricorder.singleton.updateLocalizationIndicator(true);
                    }
                });
            }
        }.start();
	}
    /** Called by native core to report events */
    public void reportEvent(String key,String val,double timeStamp, int typeCode) {
        TangoEvent event;
		event = new TangoEvent();
		event.eventKey = key;
		event.eventValue = val;
		event.timestamp = timeStamp;
		event.eventType = typeCode;
        Tricorder.singleton.SetTangoEvent(event);
    }
}
