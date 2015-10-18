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

#include "TangoDriver.h"
#include "tango-gl/video_overlay.h"
#include "Tricorder.h"
/** reference to external hoist logic for tango events */
void reportTangoEvent(const TangoEvent* theEvent);

GLuint cached_single_texture_id;
unsigned char* TangoDriver::cameraImageBuffer;
TangoDriver* TangoDriver::singleton;

unsigned int TangoDriver::imageBufferHeight;
unsigned int TangoDriver::imageBufferStride;
unsigned int TangoDriver::imageBufferWidth;

TangoDriver::TangoDriver() :
	tango_position(glm::vec3(0.0f, 0.0f, 0.0f)),
	tango_rotation(glm::quat(1.0f, 0.0f, 0.0f, 0.0f)),
	mTangoImage(nullptr),
	mLastTangoEvent(nullptr),
	mUXMonitorActive(false),
	config_(nullptr) {
	Reinitialize();
}
void TangoDriver::Reinitialize() {
	tango_position = glm::vec3(0.0f, 0.0f, 0.0f);
	tango_rotation = glm::quat(1.0f, 0.0f, 0.0f, 0.0f);
	config_ = nullptr;
	is_xyzij_dirty = false;

	d_2_ss_mat_motion = glm::mat4(1.0f);
	d_2_ss_mat_depth = glm::mat4(1.0f);
	_dvc2IMU = glm::mat4(1.0f);
	_cam2IMU = glm::mat4(1.0f);
	cc_width = 0;
	cc_height = 0;
	cc_fx = 0;
	cc_fy = 0;
	cc_cx = 0;
	cc_cy = 0;
	depth_buffer_size = 0;
	depth_buffer = nullptr;
	image_timestamp = 0;
	is_image_dirty = false;
	is_localized = false;
	max_vertex_count = 0;
	videoOverlayTexture = 0;

	mIsDepthPerceptionOn = true;
	mIsLearningModeEnabled = true;
	mIsAutoRecoveryOn = true;
	mIsMotionTrackingOn = true;
	mLocalizationCount = 0;
	pose_timestamp = 0;
	_proposedReferenceFrame = "";
	masterShutdown = false;
	mReturnPoseFilter = EPoseFrameOfReference::kAny;
}
extern "C" {


	// Tango event callback.
	static void onTangoEvent(void*, const TangoEvent* event) {
		TangoDriver* theDriver = Tricorder::GetSingleton()->Driver();
		if (theDriver->masterShutdown)
			return;
		reportTangoEvent(event);
	}
	static void onPoseAvailable(void*, const TangoPoseData* pose) {
		TangoDriver* theDriver = Tricorder::GetSingleton()->Driver();
		if (theDriver->masterShutdown)
			return;
		pthread_mutex_lock(&theDriver->pose_mutex);

		        // Update Tango localization status.
		int poseChannel = 0;
		bool poseReported = false;
		if (pose->status_code == TANGO_POSE_VALID) {
			if (pose->frame.base == TANGO_COORDINATE_FRAME_START_OF_SERVICE
				  && pose->frame.target == TANGO_COORDINATE_FRAME_DEVICE) {
				poseChannel = 1;
				theDriver->is_localized = false;
				theDriver->mLocalizationCount = 0;
			}
			else if (pose->frame.base == TANGO_COORDINATE_FRAME_AREA_DESCRIPTION
				&& pose->frame.target == TANGO_COORDINATE_FRAME_DEVICE) {
				poseChannel = 2;
				theDriver->is_localized = true;
				theDriver->mLocalizationCount++;
			}
			if (poseChannel & theDriver->GetPoseFilter())
			{
				theDriver->LoadPose(pose, poseChannel);
				poseReported = true;
			}
		}
		if (!poseReported)
		{
			Tricorder::GetSingleton()->Driver()->is_localized = false;
			Tricorder::GetSingleton()->Driver()->mLocalizationCount = 0;
			Tricorder::GetSingleton()->Driver()->SetPoseStatus(pose->status_code);
		}
		pthread_mutex_unlock(&Tricorder::GetSingleton()->Driver()->pose_mutex);
	}
	/// Callback function when new XYZij data available, caller is responsible
	/// for allocating the memory, and the memory will be released after the
	/// callback function is over.
	/// XYZij data updates in 5Hz.
	static void onXYZijAvailable(void*, const TangoXYZij* XYZ_ij) {
		TangoDriver* driver = Tricorder::GetSingleton()->Driver();
		if (driver->masterShutdown)
			return;
		pthread_mutex_lock(&driver->cloud_mutex);
		LOGI("Start Cloud Copy");
		// Copying out the depth buffer.
		// Note: the XYZ_ij object will be out of scope after this callback is
		// excuted.
		if (XYZ_ij->xyz != nullptr) {
			memcpy(driver->depth_buffer,
				XYZ_ij->xyz,
				XYZ_ij->xyz_count * 3 * sizeof(float));
			
			driver->depth_buffer_size = XYZ_ij->xyz_count;
		}
		else
			driver->depth_buffer_size = 0;
		if (XYZ_ij->ij_cols != 0 || XYZ_ij->ij_rows != 0)
			LOGI("Mesh ready");

		driver->points_timestamp = driver->pose_timestamp = XYZ_ij->timestamp;

		// Set xyz_ij dirty flag.
		driver->is_xyzij_dirty = true;
		driver->SetDepthMatrix();

		LOGI("End Cloud Copy");
		pthread_mutex_unlock(&driver->cloud_mutex);
	}
	static void onImageAvailable(void *context, TangoCameraId id, const TangoImageBuffer *image) {
		if (Tricorder::GetSingleton()->Driver()->masterShutdown)
			return;
		pthread_mutex_lock(&Tricorder::GetSingleton()->Driver()->image_mutex);

		LOGE("Start Image Copy");
		memcpy(TangoDriver::cameraImageBuffer, image->data, image->stride * (image->height + (image->height * 0.5)));
		Tricorder::GetSingleton()->Driver()->is_image_dirty = true;
		TangoDriver::imageBufferHeight = image->height;
		//TangoDriver::imageBufferStride = image->stride;
		TangoDriver::imageBufferStride = 1560;
		TangoDriver::imageBufferWidth = image->width;
		Tricorder::GetSingleton()->Driver()->image_timestamp = image->timestamp;
		LOGE("End Image Copy");
		pthread_mutex_unlock(&Tricorder::GetSingleton()->Driver()->image_mutex);
	}
	void YUV2RGB(unsigned char& y, unsigned char& u, unsigned char& v) {
		// http://en.wikipedia.org/wiki/YUV
		const float Umax = 0.436f;
		const float Vmax = 0.615f;

		float y_scaled = y / 255.0f;
		float u_scaled = 2 * (u / 255.0f - 0.5f) * Umax;
		float v_scaled = 2 * (v / 255.0f - 0.5f) * Vmax;

		y = static_cast<unsigned char>((y_scaled + 1.13983f * v_scaled) * 255.0);
		u = static_cast<unsigned char>((y_scaled - 0.39465f * u_scaled - 0.58060f * v_scaled) * 255.0);
		v = static_cast<unsigned char>((y_scaled + 2.03211f * u_scaled) * 255.0);
	}
}

void TangoDriver::LoadPose(const TangoPoseData* pose, int poseChannel)
{
	prev_pose_data = cur_pose_data;
	cur_pose_data = *pose;
	mPoseChannel = poseChannel;
	mPoseStatus = pose->status_code;
	tango_position = glm::vec3(cur_pose_data.translation[0], cur_pose_data.translation[1], cur_pose_data.translation[2]);
	tango_rotation = glm::quat(cur_pose_data.orientation[3], cur_pose_data.orientation[0], cur_pose_data.orientation[1], cur_pose_data.orientation[2]);
	// Update device with respect to  start of service frame transformation.
	// Note: this is the pose transformation for pose frame.
	d_2_ss_mat_motion = glm::translate(glm::mat4(1.0f), tango_position) * glm::mat4_cast(tango_rotation);
	isPoseDirty = true;
}
void TangoDriver::ConvertImageForUse()
{
	if (mTangoImage == nullptr)
		mTangoImage = new TangoImage();
	pthread_mutex_lock(&Tricorder::GetSingleton()->Driver()->image_mutex);
	mTangoImage->LoadYUVImage(TangoDriver::cameraImageBuffer);
	pthread_mutex_unlock(&Tricorder::GetSingleton()->Driver()->image_mutex);
}
void TangoDriver::SetDepthMatrix()
{
	glm::vec3 translation =
		glm::vec3(cur_pose_data.translation[0], cur_pose_data.translation[1], cur_pose_data.translation[2]);
	glm::quat rotation = glm::quat(cur_pose_data.orientation[3], cur_pose_data.orientation[0], cur_pose_data.orientation[1], cur_pose_data.orientation[2]);
	// Update device with respect to  start of service frame transformation.
	// Note: this is the pose transformation for depth frame.
	d_2_ss_mat_depth = glm::translate(glm::mat4(1.0f), translation) * glm::mat4_cast(rotation);
}
// Get status string based on the pose status code.
const char* TangoDriver::getStatusStringFromStatusCode(
	TangoPoseStatusType status) {
	const char* ret_string;
	switch (status) {
	case TANGO_POSE_INITIALIZING:
		ret_string = "Initializing";
		break;
	case TANGO_POSE_VALID:
		ret_string = "Valid";
		break;
	case TANGO_POSE_INVALID:
		ret_string = "Invalid";
		break;
	case TANGO_POSE_UNKNOWN:
		ret_string = "Unknown";
		break;
	default:
		ret_string = "Status_Code_Invalid";
		break;
	}
	if (static_cast<int>(status) < 3) {
		status_count[static_cast<int>(status)] += 1;
	}
	return ret_string;
}
TangoErrorType TangoDriver::Initialize(JNIEnv* env, jobject activity) {
	// Initialize Tango Service.
	// The initialize function perform API and Tango Service version check,
	// the there is a mis-match between API and Tango Service version, the
	// function will return TANGO_INVALID.
	Reinitialize();
	TangoErrorType err = TangoService_initialize(env, activity);
	if (err != TANGO_SUCCESS)
		return err;
	GetIntrinsics();
	return TANGO_SUCCESS;
}

bool TangoDriver::SetConfigFlag(TangoConfig configuration, EConfigurationOption theOption, bool theFlag) {
	if (configuration == nullptr)
		configuration = config_;
	const char* flagName = nullptr;
	switch (theOption) {
	case EDepthPerception:
		flagName = "config_enable_depth";
		mIsDepthPerceptionOn = theFlag;
		break;
	case ELearningMode :
		flagName = "config_enable_learning_mode";
		mIsLearningModeEnabled = theFlag;
		break;
	case EAutoRecovery:
		flagName = "config_enable_auto_recovery";
		mIsAutoRecoveryOn = theFlag;
		break;
	case EMotionTracking:
		flagName = "config_enable_motion_tracking";
		mIsMotionTrackingOn = theFlag;
		break;
	case EColorCamera :
		flagName = "config_enable_color_camera";
		mIsColorCameraOn = theFlag;
		break;
	}
	bool result = true;
	if (configuration != nullptr)
		result = TangoConfig_setBool(configuration, flagName, theFlag) == TANGO_SUCCESS;
	return result;
}
bool TangoDriver::SetConfig(bool is_auto_recovery) {
	// Get the default TangoConfig.
	// We get the default config first and change the config
	// flag as needed.
	config_ = TangoService_getConfig(TANGO_CONFIG_DEFAULT);
	if (config_ == nullptr) {
		LOGE("TangoService_getConfig(): Failed");
		return false;
	}
	if (!SetConfigFlag(config_, ELearningMode, mIsLearningModeEnabled)) return false;
	if (!SetConfigFlag(config_, EAutoRecovery, mIsAutoRecoveryOn)) return false;
	if (!SetConfigFlag(config_, EDepthPerception, mIsDepthPerceptionOn)) return false;
	if (!SetConfigFlag(config_, EMotionTracking, mIsMotionTrackingOn)) return false;
	// Get library version string from service.
	TangoConfig_getString(config_,
		"tango_service_library_version",
		const_cast<char*>(lib_version_string.c_str()),
		kVersionStringLength);

	// Get max point cloud elements. The value is used for allocating
	// the depth buffer.
	int temp = 0;
	if (TangoConfig_getInt32(config_, "max_point_cloud_elements", &temp) !=
		TANGO_SUCCESS) {
		LOGE("Get max_point_cloud_elements Failed");
		return false;
	}
	max_vertex_count = static_cast<uint32_t>(temp);
	// Forward allocate the maximum size of depth buffer.
	// max_vertex_count is the vertices count, max_vertex_count*3 is
	// the actual float buffer size.
	depth_buffer = new float[3 * max_vertex_count];


	cameraImageBuffer = static_cast<unsigned char*>(malloc(1280 * 720 * 4));
	singleton = this;
	is_localized = false;
	return true;
}


/*
	Load the given ADF file and start callbacks on the pose being available from the frame
	area description.  The callback will set the localized flag
*/
bool TangoDriver::LoadADF(const std::string& adfGUID)
{
	if (TangoConfig_setString(config_,
		"config_load_area_description_UUID",
		adfGUID.c_str()) !=
		TANGO_SUCCESS) {
		LOGE("config_load_area_description_uuid Failed");
		return false;
	}
	_proposedReferenceFrame = adfGUID;
	return true;
}
bool TangoDriver::SaveADF() {
	TangoUUID wat;
	return TangoService_saveAreaDescription(&wat) == TANGO_SUCCESS;
}

bool TangoDriver::GetExtrinsics() {
	// Retrieve the Extrinsic
	TangoPoseData poseData;
	TangoCoordinateFramePair pair;
	pair.base = TANGO_COORDINATE_FRAME_IMU;
	pair.target = TANGO_COORDINATE_FRAME_DEVICE;
	if (TangoService_getPoseAtTime(0.0, pair, &poseData) != TANGO_SUCCESS) {
		LOGE("TangoService_getPoseAtTime(): Failed");
		return false;
	}
	_dvc2IMUPosition =
		glm::vec3(poseData.translation[0],
		poseData.translation[1],
		poseData.translation[2]);
	_dvc2IMURotation =
		glm::quat(poseData.orientation[3],
		poseData.orientation[0],
		poseData.orientation[1],
		poseData.orientation[2]);
	_dvc2IMU = glm::translate(glm::mat4(1.0f), _dvc2IMUPosition) * glm::mat4_cast(_dvc2IMURotation);
	pair.target = TANGO_COORDINATE_FRAME_CAMERA_COLOR;
	if (TangoService_getPoseAtTime(0.0, pair, &poseData) != TANGO_SUCCESS) {
		LOGE("TangoService_getPoseAtTime(): Failed");
		return false;
	}
	_cam2IMUPosition =
		glm::vec3(poseData.translation[0],
		poseData.translation[1],
		poseData.translation[2]);
	_cam2IMURotation =
		glm::quat(poseData.orientation[3],
		poseData.orientation[0],
		poseData.orientation[1],
		poseData.orientation[2]);
	_cam2IMU = glm::translate(glm::mat4(1.0f), _cam2IMUPosition) *	glm::mat4_cast(_cam2IMURotation);

	_dvc2IMUInv = glm::inverse(GetDvc2IMUMatrix());
	return true;
}
double TangoDriver::FieldOfView(bool horizontal)
{
	double fov = horizontal ?  2 * atan(0.5 * cc_width / cc_fx)
                : 2 * atan(0.5 * cc_height / cc_fy);    //  * 57.2957795
	return fov;
}

bool TangoDriver::GetIntrinsics() {
	// Retrieve the Intrinsic
	TangoCameraIntrinsics ccIntrinsics;
	if (TangoService_getCameraIntrinsics(TANGO_CAMERA_COLOR, &ccIntrinsics)
		!= TANGO_SUCCESS) {
		LOGE("TangoService_getCameraIntrinsics(): Failed");
		return false;
	}

	// Color camera's image plane width.
	cc_width = ccIntrinsics.width;
	// Color camera's image plane height.
	cc_height = ccIntrinsics.height;
	// Color camera's x axis focal length.
	cc_fx = ccIntrinsics.fx;
	// Color camera's y axis focal length.
	cc_fy = ccIntrinsics.fy;
	// Principal point x coordinate on the image.
	cc_cx = ccIntrinsics.cx;
	// Principal point y coordinate on the image.
	cc_cy = ccIntrinsics.cy;
	for (int i = 0; i < 5; i++) {
		cc_distortion[i] = ccIntrinsics.distortion[i];
	}
	return true;
}



void TangoDriver::UpdateColorTexture() {
	// not bothering with error return because either it worked or it didn't
	// and there have been times when updates took place whilest the call
	// yammered about failures
	TangoService_updateTexture(TANGO_CAMERA_COLOR, nullptr);
}

// Reset the Motion Tracking.
void TangoDriver::ResetMotionTracking() {
	TangoService_resetMotionTracking();
}

// Connect to Tango Service, service will start running, and
// POSE can be queried.
TangoErrorType TangoDriver::Connect() {
	TangoErrorType err;
	// Attach the onXYZijAvailable callback.
	if ((err = TangoService_connectOnXYZijAvailable(onXYZijAvailable)) != TANGO_SUCCESS) {
		LOGE("TangoService_connectOnXYZijAvailable(): Failed");
		return err;
	}

	if ((err = TangoService_connectOnFrameAvailable(TANGO_CAMERA_COLOR, this, onImageAvailable)) != TANGO_SUCCESS) {
		LOGE("TangoService_connectOnFrameAvailable(): Failed!");
		return err;
	}

	TangoCoordinateFramePair pairs[2] =
	{
	{
		TANGO_COORDINATE_FRAME_START_OF_SERVICE,
		TANGO_COORDINATE_FRAME_DEVICE
	},
		{
		TANGO_COORDINATE_FRAME_AREA_DESCRIPTION,
		TANGO_COORDINATE_FRAME_DEVICE
	}
	};

	// Attach onPoseAvailable callback.
	// The callback will be called after the service is connected.
	if ((err = TangoService_connectOnPoseAvailable(2, &pairs[0], onPoseAvailable)) != TANGO_SUCCESS) {
		LOGE("TangoService_connectOnPoseAvailable(): Failed");
		return err;
	}

	// Attach onEventAvailable callback.
	// The callback will be called after the service is connected.
	if (IsUXMonitorActive())
	{
		if ((err = TangoService_connectOnTangoEvent(onTangoEvent)) != TANGO_SUCCESS) {
			LOGE("TangoService_connectOnTangoEvent(): Failed");
			return err;
		}
	}

	LOGE("All Callbacks Bound");
	return TangoService_connect(nullptr, config_);
}

// Disconnect Tango Service.
// Disconnect will disconnect all callback from Tango Service,
// after resume, the application should re-connect all callback
// and connect to service.
// Disconnect will also reset all configuration to default.
// Before disconnecting the service, the application is reponsible to
// free the config_ handle as well.
//
// When running two Tango applications, the first application needs to
// disconnect the service, so that second application could connect the
// service with a new configration handle. The disconnect function will
// reset the configuration each time it's being called.
void TangoDriver::Disconnect() {
	masterShutdown = true;

	TangoService_disconnect();
	// tear down all the graphics stuff
}

glm::mat4 TangoDriver::GetOC2OWMat(bool is_depth_pose) {
	if (is_depth_pose) {
		glm::mat4 temp = d_2_ss_mat_depth;
		return ss_2_ow_mat * temp * glm::inverse(_dvc2IMU) * _cam2IMU *
			oc_2_c_mat;
	}
	else {
		glm::mat4 temp = d_2_ss_mat_motion;
		return ss_2_ow_mat * temp * glm::inverse(_dvc2IMU) * _cam2IMU *
			oc_2_c_mat;
	}
}

void TangoDriver::ConnectTexture(GLuint texture_id)
{
	videoOverlayTexture = texture_id;
	cached_single_texture_id = texture_id;
}
