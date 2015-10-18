/*
 * Copyright 2014 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#ifndef AUGMENTED_REALITY_JNI_EXAMPLE_TANGO_DATA_H_
#define AUGMENTED_REALITY_JNI_EXAMPLE_TANGO_DATA_H_

#include <pthread.h>
#include <sstream>
#include <string>
#include <GLES/gl.h>
#include "glm/glm.hpp"
#include "glm/gtc/quaternion.hpp"
#include "tango_client_api.h"
#include "TangoImage.h"

enum CameraType {FIRST_PERSON = 0,THIRD_PERSON = 1,	TOP_DOWN = 2};
enum EConfigurationOption {ELearningMode = 1,EAutoRecovery=2,EDepthPerception=4,EMotionTracking = 8,EColorCamera = 16 };

// Min/max clamp value of camera observation distance.
const float kCamViewMinDist = 1.0f;
const float kCamViewMaxDist = 100.f;
// Zoom in speed.
const float kZoomSpeed = 10.0f;

const int kMeterToMillimeter = 1000;
const int kVersionStringLength = 27;
const float kSecondToMillisecond = 1000.0f;
// Opengl camera to color camera matrix.
const glm::mat4 oc_2_c_mat =
glm::mat4(1.0f, 0.0f, 0.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, 0.0f, 0.0f,
-1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f);
// Start service to opengl world matrix.
const glm::mat4 ss_2_ow_mat =
glm::mat4(1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, 1.0f, 0.0f,
0.0f, 0.0f, 0.0f, 0.0f, 1.0f);
class TangoDriver {
public:
	//* Used to determine which poses are returned for shipping to file/cloud
	enum EPoseFrameOfReference
	{
		/** Pose relative to start of service */
		kStartOfService = 1,
		/** Pose relative to Area Definition File */
		kADF = 2,
		/** Any pose */
		kAny = 3
	};
	TangoDriver();
	void Reinitialize();
	TangoErrorType Initialize(JNIEnv* env, jobject activity);
	bool SetConfig(bool isAutoReset);
	bool SetConfigFlag(TangoConfig configuration,EConfigurationOption theOption,bool theFlag);
	TangoErrorType Connect();
	void Disconnect();
	void ResetMotionTracking();
	void ConnectTexture(GLuint texture_id);
	void UpdateColorTexture();
	bool GetIntrinsics();
	bool GetExtrinsics();
	glm::mat4 GetOC2OWMat(bool is_depth_pose);
	void ConvertImageForUse();
	void SetLearningMode(bool mode);
	bool LoadADF(const std::string& adfGUID);
	bool SaveADF();
	void LoadPose(const TangoPoseData* pose,int channelId);
	void SetDepthMatrix();
	inline const glm::mat4& GetDvc2IMUMatrix() const { return _dvc2IMU; }
	inline const glm::mat4& GetDvc2IMUMatrixInverse() const { return _dvc2IMUInv; }
	inline const glm::mat4& GetCam2IMUMatrix() const { return _cam2IMU; }
	inline const glm::vec3& GetDvcIMUPosition() const { return _dvc2IMUPosition; }
	inline const glm::quat& GetDvcIMUAttitude() const { return _dvc2IMURotation; }
	inline const glm::vec3& GetCamIMUPosition() const { return _cam2IMUPosition; }
	inline const glm::quat& GetCamIMUAttitude() const { return _cam2IMURotation; }

	double FieldOfView(bool horizontal);
	bool IsPoseStatusChanged() { return mPoseStatusDirty;}
	int GetPoseStatus() { return mPoseStatus;mPoseStatusDirty = false; }
	void SetPoseStatus(int s) { mPoseStatus = s; mPoseStatusDirty = true; }
	
	void SetTangoEvent(TangoEvent* e) { if (mLastTangoEvent == nullptr)  mLastTangoEvent = e; }
	bool NextTangoEvent(TangoEvent** e) 
	{
		if (mLastTangoEvent == nullptr) return false;
		*e = mLastTangoEvent;
		mLastTangoEvent = nullptr;
		return true;
	}
	EPoseFrameOfReference GetPoseFilter() const {return mReturnPoseFilter; }
	//* True if the host has an active UX monitor - controls whether tango events are returned to the host */
	bool IsUXMonitorActive() const { return mUXMonitorActive;}
	const char* getStatusStringFromStatusCode(TangoPoseStatusType status);
public:


	pthread_mutex_t pose_mutex = PTHREAD_MUTEX_INITIALIZER; 
	pthread_mutex_t xyzij_mutex = PTHREAD_MUTEX_INITIALIZER;
	pthread_mutex_t event_mutex = PTHREAD_MUTEX_INITIALIZER;
	pthread_mutex_t image_mutex = PTHREAD_MUTEX_INITIALIZER;
	pthread_mutex_t cloud_mutex = PTHREAD_MUTEX_INITIALIZER;

	float* depth_buffer;
	uint32_t depth_buffer_size;
	bool is_xyzij_dirty;
	bool isPoseDirty;
	int mPoseChannel;
	TangoPoseData cur_pose_data;
	TangoPoseData prev_pose_data;
	double pose_timestamp;
	double points_timestamp;
	bool is_image_dirty;
	double image_timestamp;


	uint32_t max_vertex_count;
	glm::vec3 tango_position;
	glm::quat tango_rotation;

	int status_count[3];
	std::string event_string;
	std::string lib_version_string;
	std::string pose_string;



	// matrices imported with pointcloud
	// Device to start service matrix.
	glm::mat4 d_2_ss_mat_depth;
	// Device to start service matrix.
	glm::mat4 d_2_ss_mat_motion;




	// Tango Intrinsic for color camera.
	int cc_width;
	int cc_height;
	double cc_fx;
	double cc_fy;
	double cc_cx;
	double cc_cy;
	double cc_distortion[5];

	// Localization status.
	bool is_localized;

	bool masterShutdown;
	std::string cur_uuid;
	int videoOverlayTexture;
	static unsigned char* cameraImageBuffer;
	TangoImage* mTangoImage;
	static unsigned int imageBufferHeight;
	static unsigned int imageBufferStride;
	static unsigned int imageBufferWidth;
	static TangoDriver* singleton;
	// usually the guid of the ADF when available
	std::string _proposedReferenceFrame;
	int mLocalizationCount;

private:
	bool mUXMonitorActive;
	EPoseFrameOfReference mReturnPoseFilter;
	TangoEvent* mLastTangoEvent;
	int mPoseStatus;
	bool mPoseStatusDirty;
	TangoConfig config_;
	//! Given position of device against IMU
	glm::vec3 _dvc2IMUPosition;
	//! Given rotation of device against IMU
	glm::quat _dvc2IMURotation;
	//! Device to IMU matrix computed as a function of device IMU position and attitude
	glm::mat4 _dvc2IMU;
	//! Inverted dvc2imu for rendering
	glm::mat4 _dvc2IMUInv;

	//! Given position of camera against IMU
	glm::vec3 _cam2IMUPosition;
	//! Given rotation of camera against IMU
	glm::quat _cam2IMURotation;
	//! Device to IMU matrix computed as a function of device IMU position and attitude
	glm::mat4 _cam2IMU;
	bool mIsDepthPerceptionOn;
	bool mIsLearningModeEnabled;
	bool mIsAutoRecoveryOn;
	bool mIsMotionTrackingOn;
	bool mIsColorCameraOn;
};
#endif  // AUGMENTED_REALITY_JNI_EXAMPLE_TANGO_DATA_H_
