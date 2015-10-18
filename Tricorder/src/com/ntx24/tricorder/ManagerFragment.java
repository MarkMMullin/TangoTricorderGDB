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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.EnumMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.json.JSONException;

import android.app.Fragment;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemSelectedListener;

import com.ntx24.tricorder.Tricorder.TricorderSensorStream;

public class ManagerFragment extends Fragment {
	public static ManagerFragment singleton;
	View rootView;
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		singleton = this;
		rootView = inflater
				.inflate(R.layout.fragment_manager, container, false);


		CheckBox learnMode = (CheckBox) rootView.findViewById(R.id.isLearningModeOn);
		learnMode.setChecked(Tricorder.singleton.mLearningModeOn);
		learnMode
		.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				Tricorder.singleton.mLearningModeOn = isChecked;
			}
		});

		
		CheckBox point = (CheckBox) rootView.findViewById(R.id.hasPointData);
		Boolean hasPointData = hasPointData();
		point.setEnabled(hasPointData);
		point.setChecked(hasPointData);

		CheckBox pose = (CheckBox) rootView.findViewById(R.id.hasPoseData);
		Boolean hasPoseData = hasPoseData();
		pose.setEnabled(hasPoseData);
		pose.setChecked(hasPoseData);

		CheckBox pict = (CheckBox) rootView.findViewById(R.id.hasImageData);
		Boolean hasImageData = hasPictureData();
		pict.setEnabled(hasImageData);
		pict.setChecked(hasImageData);

		CheckBox location = (CheckBox) rootView
				.findViewById(R.id.hasLocationData);
		Boolean hasLocationData = hasLocationData();
		location.setEnabled(hasLocationData);
		location.setChecked(hasLocationData);

		CheckBox session = (CheckBox) rootView
				.findViewById(R.id.hasSessionData);
		Boolean hasSessionData = hasSessionData();
		session.setEnabled(hasSessionData);
		session.setChecked(hasSessionData);

		Button uploadButton = (Button) rootView.findViewById(R.id.uploadButton);
		Button purgeButton = (Button) rootView.findViewById(R.id.purgeButton);
		Button exportRawButton = (Button) rootView.findViewById(R.id.exportRAW);
		Button exportJSONButton = (Button) rootView
				.findViewById(R.id.exportJSON);
		Boolean anyUpload = hasPointData || hasPoseData || hasImageData
				|| hasLocationData || hasSessionData;
		purgeButton.setEnabled(anyUpload);
		exportRawButton.setEnabled(anyUpload);
		exportJSONButton.setEnabled(anyUpload);
		String ess = Environment.getExternalStorageState();
		if (!Environment.MEDIA_MOUNTED.equals(ess)) {
			Tricorder.singleton.showMessage("External SD card not mounted", Toast.LENGTH_LONG);
		}

		
        Spinner spinner = (Spinner) rootView.findViewById(R.id.adfSpinner);
        final ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(Tricorder.singleton,   
        		android.R.layout.simple_spinner_item, Tricorder.singleton.mAvailableADFs);
        spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); // The drop down view

        new Thread() {
            public void run() {
            	Tricorder.singleton.runOnUiThread(new Runnable() {     
                    @Override
                    public void run() {
                    	Spinner spinner = (Spinner) Tricorder.singleton.findViewById(R.id.adfSpinner);
                    	spinner.setAdapter(spinnerArrayAdapter);
                    }
                });
            }
        }.start();
        spinner.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
            	Spinner spinner = (Spinner) getView().findViewById(R.id.adfSpinner);
            	String text = spinner.getSelectedItem().toString();
            	if(text != "NONE")
            		Tricorder.singleton.mSelectedADF = text;
            	else
            		Tricorder.singleton.mSelectedADF = null;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // your code here
            }
        });



		uploadButton.setEnabled(anyUpload);
		uploadButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				offloadLocalData();
			}
		});
		purgeButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				removeLocalStores();
			}
		});
		exportRawButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				try {
					exportRawData();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
		exportJSONButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				exportJSONData();
			}
		});

		updateFileLengths();
		new Thread() {
			public void run() {
				updateFileItems();
			}
		}.start();
		return rootView;
	}



	@Override
	public void onSaveInstanceState(Bundle outState) {
		// TODO Auto-generated method stub
		super.onSaveInstanceState(outState);
	}



	private Boolean hasSessionData() {
		return Tricorder.singleton.getFileStreamPath(
				TangoSession.localFilename()).exists();
	}

	private Boolean hasLocationData() {
		return Tricorder.singleton.getFileStreamPath(
				TangoLocation.localFilename()).exists();
	}

	private Boolean hasPictureData() {
		return Tricorder.singleton.getFileStreamPath(
				TangoPicture.localFilename()).exists();
	}

	private Boolean hasPoseData() {
		return Tricorder.singleton.getFileStreamPath(
				TangoPoseData.localFilename()).exists();
	}

	private Boolean hasPointData() {
		return Tricorder.singleton.getFileStreamPath(
				TangoPointCloud.localFilename()).exists();
	}
	private EnumMap<TricorderSensorStream,Thread> podDrivers = new EnumMap<TricorderSensorStream,Thread>(TricorderSensorStream.class);
	private Thread genericParallelOutputDriver(
			final TricorderSensorStream channelId) {
		Thread t = new Thread(new Runnable() {
			public void run() {
				try {
					switch (channelId) {
					case Session:
						uploadSessions(false);
						break;
					case Location:
						uploadLocations(false);
						break;
					case Pose:
						uploadPoses(false);
						break;
					case Points:
						uploadPoints(false);
						break;
					case Picture:
						uploadPictures(false);
						break;
					}
				} catch (IOException e) {
					e.printStackTrace();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		});
		t.start();
		podDrivers.put(channelId,t);
		return t;
	}

	long[] uploadInfo = new long[10];
	long localOffloadStartedAt;
	String offloadType;
	private void offloadLocalData() {
		Thread t = new Thread() {
			public void run() {
				updateUploadProgress();
			}
		};
		t.start();
		localOffloadStartedAt = System.currentTimeMillis();
		offloadType = "Batch Cloud";
		if (hasPictureData())
			genericParallelOutputDriver(TricorderSensorStream.Picture);
		if (hasPoseData())
			genericParallelOutputDriver(TricorderSensorStream.Pose);
		if (hasPointData())
			genericParallelOutputDriver(TricorderSensorStream.Points);
		if (hasLocationData())
			genericParallelOutputDriver(TricorderSensorStream.Location);
		if (hasSessionData())
			genericParallelOutputDriver(TricorderSensorStream.Session);
	}

	private void exportRawData() throws IOException {
		long startAt = System.currentTimeMillis();
		exportRawData(TangoSession.localFilename());
		exportRawData(TangoLocation.localFilename());
		exportRawData(TangoPoseData.localFilename());
		exportRawData(TangoPointCloud.localFilename());
		exportRawData(TangoPicture.localFilename());
		long duration = System.currentTimeMillis() - startAt;
    }

	private void exportJSONData() {
		localOffloadStartedAt = System.currentTimeMillis();

		offloadType = "Batch JSON";
			// this is compute bound - the only reason for threading it
			// is to allow the display to remain operational
			Thread t = new Thread() {
				public void run() {
					updateUploadProgress();
				}
			};
			t.start();
			Thread t1 = new Thread() {
				public void run() {

					try {
						ManagerFragment.singleton.podDrivers.put(TricorderSensorStream.Session, this);
						ManagerFragment.singleton.podDrivers.put(TricorderSensorStream.Location, this);
						ManagerFragment.singleton.podDrivers.put(TricorderSensorStream.Pose, this);
						ManagerFragment.singleton.podDrivers.put(TricorderSensorStream.Points, this);
						ManagerFragment.singleton.podDrivers.put(TricorderSensorStream.Picture, this);
						exportJSONData(Tricorder.TricorderSensorStream.Session);
						exportJSONData(Tricorder.TricorderSensorStream.Location);
						exportJSONData(Tricorder.TricorderSensorStream.Pose);
						exportJSONData(Tricorder.TricorderSensorStream.Points);
						exportJSONData(Tricorder.TricorderSensorStream.Picture);

			    } catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				}
			};
			t1.start();
			
	}

	private void exportJSONData(Tricorder.TricorderSensorStream channel)
			throws IOException, JSONException, InterruptedException {
		String internalFile = null;
		switch (channel) {
		case Session:
			internalFile = TangoSession.localFilename();
			break;
		case Location:
			internalFile = TangoLocation.localFilename();
			break;
		case Pose:
			internalFile = TangoPoseData.localFilename();
			break;
		case Points:
			internalFile = TangoPointCloud.localFilename();
			break;
		case Picture:
			internalFile = TangoPicture.localFilename();
			break;
		}
		File fIn = Tricorder.singleton.getFileStreamPath(internalFile);
		if (!fIn.exists())
			return;
		String externalFile = internalFile.substring(0,
				internalFile.length() - 3) + "json";
		File fOut = new File(
				Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
						+ "/Tango Tricorder/", externalFile);
		FileInputStream fsIn = new FileInputStream(fIn);
		DataInputStream dIn = new DataInputStream(fsIn);
		FileOutputStream fsOut = new FileOutputStream(fOut);
		DataOutputStream dOut = new DataOutputStream(fsOut);
		switch (channel) {
		case Session:
			copySessions(dIn, dOut);
			break;
		case Location:
			copyLocations(dIn, dOut);
			break;
		case Pose:
			copyPoses(dIn, dOut);
			break;
		case Points:
			copyPoints(dIn, dOut);
			break;
		case Picture:
			copyPictures(dIn, dOut);
			break;
		}
		dOut.close();
		dIn.close();
	}

	private void exportRawData(String internalFile) throws IOException {
		File fIn = Tricorder.singleton.getFileStreamPath(internalFile);
		if (!fIn.exists())
			return;

		String externalFile = internalFile.substring(0,
				internalFile.length() - 3) + "raw";
		File fOutDir = new File(
				Environment
						.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
						+ "/Tango Tricorder/");
		if (!fOutDir.exists())
			fOutDir.mkdir();
		File fOut = new File(fOutDir.getAbsolutePath(), externalFile);
		FileInputStream fsIn = new FileInputStream(fIn);
		DataInputStream dIn = new DataInputStream(fsIn);
		FileOutputStream fsOut = new FileOutputStream(fOut);
		DataOutputStream dOut = new DataOutputStream(fsOut);
		byte[] txBuffer = new byte[1024 * 32];
		int readlen = 0;
		try {
			while ((readlen = dIn.read(txBuffer)) > 0) {
				dOut.write(txBuffer, 0, readlen);
				readlen = 0;
			}
		} catch (IOException e) {
			return;
		} finally {
			if (readlen > 0)
				dOut.write(txBuffer, 0, readlen);
			dOut.close();
			dIn.close();
		}
	}

	private void updateUploadProgress() {
		long objCount = 0;
		for (int i = 5; i <= 9; i++)
			objCount += uploadInfo[i];
		final long numObjs = objCount;
		final ProgressBar pb = (ProgressBar) Tricorder.singleton
				.findViewById(R.id.uploadProgress);
		int running = 1;
		while (running > 0) {
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			Tricorder.singleton.runOnUiThread(new Thread(new Runnable() {
				public void run() {
					pb.setMax(1000);
					pb.setProgress((int) ((objectsSent.get() / (double) numObjs) * 1000.0));
				}
			}));
			running = 0;
			if(podDrivers.get(Tricorder.TricorderSensorStream.Session).isAlive()) ++running;
			if(podDrivers.get(Tricorder.TricorderSensorStream.Location).isAlive()) ++running;
			if(podDrivers.get(Tricorder.TricorderSensorStream.Pose).isAlive()) ++running;
			if(podDrivers.get(Tricorder.TricorderSensorStream.Points).isAlive()) ++running;
			if(podDrivers.get(Tricorder.TricorderSensorStream.Picture).isAlive()) ++running;
		}
		long duration = System.currentTimeMillis() - localOffloadStartedAt;

		Tricorder.singleton.runOnUiThread(new Thread(new Runnable() {
			public void run() {
				pb.setMax(1000);
				pb.setProgress(0);
			}
		}));
	}

	private void updateFileLengths() {
		displayFileLength(TangoSession.localFilename(), R.id.sessionFilesize, 0);
		displayFileLength(TangoLocation.localFilename(), R.id.locationFilesize,
				1);
		displayFileLength(TangoPoseData.localFilename(), R.id.poseFilesize, 2);
		displayFileLength(TangoPointCloud.localFilename(), R.id.pointsFilesize,
				3);
		displayFileLength(TangoPicture.localFilename(), R.id.pictureFilesize, 4);
	}

	private void updateFileItems() {
		try {
			final int countSessions = uploadSessions(true);
			Tricorder.singleton.runOnUiThread(new Thread(new Runnable() {
				public void run() {
					displayFileCount(countSessions, R.id.sessionCount);
					uploadInfo[5] = countSessions;
				}
			}));
			final int countLocations = uploadLocations(true);
			Tricorder.singleton.runOnUiThread(new Thread(new Runnable() {
				public void run() {
					displayFileCount(countLocations, R.id.locationCount);
					uploadInfo[6] = countLocations;
				}
			}));
			final int countPoses = uploadPoses(true);
			Tricorder.singleton.runOnUiThread(new Thread(new Runnable() {
				public void run() {
					displayFileCount(countPoses, R.id.poseCount);
					uploadInfo[7] = countPoses;
				}
			}));
			final int countPoints = uploadPoints(true);
			Tricorder.singleton.runOnUiThread(new Thread(new Runnable() {
				public void run() {
					displayFileCount(countPoints, R.id.pointsCount);
					uploadInfo[8] = countPoints;
				}
			}));
			final int countPictures = uploadPictures(true);
			Tricorder.singleton.runOnUiThread(new Thread(new Runnable() {
				public void run() {
					displayFileCount(countPictures, R.id.pictureCount);
					uploadInfo[9] = countPictures;
				}
			}));
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private void displayFileLength(String filename, int resourceId,
			int channelIndex) {
		File f = Tricorder.singleton.getFileStreamPath(filename);
		long fl = f.exists() ? f.length() : 0;
		uploadInfo[channelIndex] = fl;
		String mag = "b";
		if (fl > 1024 * 1024) {
			fl = fl / (1024 * 1024);
			mag = "Mb";
		} else if (fl > 1024) {
			fl = fl / 1024;
			mag = "Kb";
		}
		TextView tv = (TextView) rootView.findViewById(resourceId);
		tv.setText(Long.toString(fl) + mag);
	}

	private void displayFileCount(int count, int resourceId) {
		TextView tv = (TextView) rootView.findViewById(resourceId);
		tv.setText(Integer.toString(count));
	}

	private AtomicInteger objectsSent;

	private int uploadSessions(Boolean countOnly) throws IOException {
		objectsSent = new AtomicInteger();
		objectsSent.set(0);
		File f = Tricorder.singleton.getFileStreamPath(TangoSession
				.localFilename());
		if (!f.exists())
			return 0;
		FileInputStream fs = new FileInputStream(f);
		DataInputStream dIn = new DataInputStream(fs);
		int count = 0;
		try {
			while (true) {
				TangoSession ts = new TangoSession(dIn);
				if (!countOnly) {
					ts.publishToEndpoint();
					objectsSent.incrementAndGet();
				}
				++count;
			}
		} catch (EOFException e) {
			return count;
		} finally {
			dIn.close();
		}
	}

	private void copySessions(DataInputStream dIn, DataOutputStream dOut)
			throws IOException, JSONException {
		try {
			objectsSent = new AtomicInteger();
			objectsSent.set(0);
			while (true) {
				TangoSession ts = new TangoSession(dIn);
				String output = ts.GenerateJSON().toString()
						+ System.getProperty("line.separator");
				dOut.writeBytes(output);
				objectsSent.incrementAndGet();
			}
		} catch (EOFException e) {
			return;
		}
	}

	private int uploadPoses(Boolean countOnly) throws IOException,
			InterruptedException {
		File f = Tricorder.singleton.getFileStreamPath(TangoPoseData
				.localFilename());
		if (!f.exists())
			return 0;
		FileInputStream fs = new FileInputStream(f);
		DataInputStream dIn = new DataInputStream(fs);
		int count = 0;

		try {
			while (true) {
				TangoPoseData pd = new TangoPoseData(dIn);
				if (!countOnly) {
					pd.publishToEndpoint();
					objectsSent.incrementAndGet();
				}
				++count;
			}
		} catch (EOFException e) {
			return count;
		} finally {
			dIn.close();
		}
	}

	private void copyPoses(DataInputStream dIn, DataOutputStream dOut)
			throws IOException, InterruptedException, JSONException {

		try {
			while (true) {
				TangoPoseData pd = new TangoPoseData(dIn);
				String output = pd.GenerateJSON().toString()
						+ System.getProperty("line.separator");
				dOut.writeBytes(output);
				objectsSent.incrementAndGet();
			}
		} catch (EOFException e) {
			return;
		}
	}

	private int uploadPictures(Boolean countOnly) throws IOException,
			InterruptedException {
		File f = Tricorder.singleton.getFileStreamPath(TangoPicture
				.localFilename());
		if (!f.exists())
			return 0;
		FileInputStream fs = new FileInputStream(f);
		DataInputStream dIn = new DataInputStream(fs);
		int count = 0;

		try {
			while (true) {
				TangoPicture tp = new TangoPicture(dIn);
				if (!countOnly) {
					tp.publishToEndpoint();
					objectsSent.incrementAndGet();
				}
				++count;
			}
		} catch (EOFException e) {
			return count;
		} finally {
			dIn.close();
		}
	}

	private void copyPictures(DataInputStream dIn, DataOutputStream dOut)
			throws IOException, InterruptedException, JSONException {

		try {
			while (true) {
				TangoPicture tp = new TangoPicture(dIn);
				String output = tp.GenerateJSON().toString()
						+ System.getProperty("line.separator");
				dOut.writeBytes(output);
				objectsSent.incrementAndGet();
			}
		} catch (EOFException e) {
			return;
		}
	}

	private int uploadPoints(Boolean countOnly) throws IOException,
			InterruptedException {
		File f = Tricorder.singleton.getFileStreamPath(TangoPointCloud
				.localFilename());
		if (!f.exists())
			return 0;
		FileInputStream fs = new FileInputStream(f);
		int count = 0;
		DataInputStream dIn = new DataInputStream(fs);
		try {
			while (true) {
				TangoPointCloud pc = new TangoPointCloud(dIn);
				if (!countOnly) {
					pc.publishToEndpoint();
					objectsSent.incrementAndGet();
				}
				++count;
			}
		} catch (EOFException e) {
			return count;
		} finally {
			dIn.close();
		}
	}

	private void copyPoints(DataInputStream dIn, DataOutputStream dOut)
			throws IOException, InterruptedException, JSONException {
		try {
			while (true) {
				TangoPointCloud pc = new TangoPointCloud(dIn);
				String output = pc.toJSONString()
						+ System.getProperty("line.separator");
				dOut.writeBytes(output);
				objectsSent.incrementAndGet();
			}
		} catch (EOFException e) {
			return;
		}
	}

	private int uploadLocations(Boolean countOnly) throws IOException,
			InterruptedException {
		File f = Tricorder.singleton.getFileStreamPath(TangoLocation
				.localFilename());
		if (!f.exists())
			return 0;
		FileInputStream fs = new FileInputStream(f);
		DataInputStream dIn = new DataInputStream(fs);
		int count = 0;

		try {
			while (true) {
				TangoLocation l = new TangoLocation(dIn);
				if (!countOnly) {
					l.publishToEndpoint();
					objectsSent.incrementAndGet();
				}
				++count;
			}
		} catch (EOFException e) {
			return count;
		} finally {
			dIn.close();
		}
	}

	private int copyLocations(DataInputStream dIn, DataOutputStream dOut)
			throws IOException, InterruptedException, JSONException {
		int count = 0;

		try {
			while (true) {
				TangoLocation l = new TangoLocation(dIn);
				String output = l.GenerateJSON().toString()
						+ System.getProperty("line.separator");
				dOut.writeBytes(output);
				++count;
			}
		} catch (EOFException e) {
			return count;
		}
	}

	private void removeLocalStores() {

		Tricorder.singleton.getFileStreamPath(TangoPointCloud.localFilename())
				.delete();
		Tricorder.singleton.getFileStreamPath(TangoPoseData.localFilename())
				.delete();
		Tricorder.singleton.getFileStreamPath(TangoPicture.localFilename())
				.delete();
		Tricorder.singleton.getFileStreamPath(TangoLocation.localFilename())
				.delete();
		Tricorder.singleton.getFileStreamPath(TangoSession.localFilename())
				.delete();
	}



    }
