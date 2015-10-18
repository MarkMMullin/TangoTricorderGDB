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

#define GLM_FORCE_RADIANS

#include <jni.h>
#include <string>


#include "Tricorder.h"
#include "TangoDriver.h"
#include "VideoOverlay.h"
#include "PointCloudRenderer.h"
#include "TangoImage.h"

//* Used to inform the host the localization state has changed */
bool signalLocalizationStateChange(bool state);
/** reference to external queue hoist logic for pictures */
bool postPicture(double timestamp, unsigned char* rawbits, int width, int height);
/** reference to external queue hoist logic for point clouds */
void postPoints(double timestamp, int numPoints, float *points);
/** reference to external queue hoist logic for poses */
bool postPose(double timestamp, int channelId, double px, double py, double pz, double ax, double ay, double az, double aw,int statusCode);
/** reference to external queue hoist logic for solo pose status */
bool reportPoseStatus(int statusCode);



Tricorder* Tricorder::smSingleton = nullptr;
Tricorder::Tricorder()
{
	cam_parent_transform = nullptr;
	cam = nullptr;
	pointCloudRenderer = nullptr;
	video_overlay = nullptr;
	pointCloudValid = false; 
}
Tricorder* Tricorder::GetSingleton()
{
	if (smSingleton == nullptr)
	{
		smSingleton = new Tricorder();
		smSingleton->mTangoDriver = new TangoDriver();
	}
	return smSingleton;
}
/**  Setup projection matrix in first person view from color camera intrinsics. */
void Tricorder::SetupIntrinsics() {
	image_width = static_cast<float>(mTangoDriver->cc_width);
	image_height = static_cast<float>(mTangoDriver->cc_height);
	// Image plane focal length for x axis.
	float img_fl = static_cast<float>(mTangoDriver->cc_fx);
	image_plane_ratio = image_height / image_width;
	image_plane_dis_original = 2.0f * img_fl / image_width;
	image_plane_dis = image_plane_dis_original;
}
bool Tricorder::SetupGraphics() {

	cam_parent_transform = new tango_gl::Transform();

	cam = new tango_gl::Camera();

	pointCloudRenderer = new PointCloudRenderer();
	video_overlay = new VideoOverlay();

	cam->SetParent(nullptr);
	//ss_to_ow_mat = GlUtil::ss_to_ow_mat;
	//oc_to_c_mat = GlUtil::oc_to_c_mat;
	float height = static_cast<float>(mTangoDriver->cc_height);
	float focaly = static_cast<float>(mTangoDriver->cc_fy);
	float fov = 2*atan(0.5*height / focaly) * 57.2957795;
	cam->SetFieldOfView(fov);
	cam_parent_transform->SetPosition(kZeroVec3);
	cam_parent_transform->SetRotation(kZeroQuat);
	cam_parent_transform->SetScale(kOneVec3);
	video_overlay->SetScale(kOneVec3);
	video_overlay->SetPosition(kZeroVec3);
	video_overlay->SetRotation(kZeroQuat);
	video_overlay->SetParent(nullptr);
	return true;
}
bool Tricorder::TeardownGraphics() {
	if (cam_parent_transform != nullptr)
		delete cam_parent_transform;
	if (cam != nullptr)
		delete cam;
	if (pointCloudRenderer != nullptr)
		delete pointCloudRenderer;
	if (video_overlay != nullptr)
		delete video_overlay;
	cam_parent_transform = nullptr;
	cam = nullptr;
	pointCloudRenderer = nullptr;
	video_overlay = nullptr;
	pointCloudValid = false;
	last_localizationStatus = false;
	last_localizationCount = 0;
	return true;
}

void Tricorder::SetupViewport(int w, int h) {
	float screen_ratio = static_cast<float>(h) / static_cast<float>(w);
	if (image_plane_ratio < screen_ratio) {
		glViewport(0, 0, w, w * image_plane_ratio);
	}
	else {
		glViewport((w - h / image_plane_ratio) / 2, 0, h / image_plane_ratio, h);
	}
	cam->SetAspectRatio(1.0f / screen_ratio);
}

bool Tricorder::RenderFrame() {
	if (mTangoDriver->masterShutdown)
		return false;


	if (last_localizationStatus != mTangoDriver->is_localized) {
		last_localizationStatus = mTangoDriver->is_localized;
		signalLocalizationStateChange(last_localizationStatus);
		last_localizationCount = 0;
	}
	else if (last_localizationStatus && last_localizationCount != mTangoDriver->mLocalizationCount) {
		last_localizationCount = mTangoDriver->mLocalizationCount;
		signalLocalizationStateChange(last_localizationStatus);
	}


	if (mTangoDriver->isPoseDirty)
	{
		TangoPoseData pd =  mTangoDriver->cur_pose_data;
		mTangoDriver->isPoseDirty = false;

		postPose(pd.timestamp,
			mTangoDriver->mPoseChannel,
			pd.translation[0],
			pd.translation[1],
			pd.translation[2],
			pd.orientation[0],
			pd.orientation[1],
			pd.orientation[2],
			pd.orientation[3],
			(int) pd.status_code);
	}
	else if (mTangoDriver->IsPoseStatusChanged())
	{
		reportPoseStatus(mTangoDriver->GetPoseStatus());
	}

	if (mTangoDriver->is_image_dirty)
	{
		//pthread_mutex_lock(&mTangoDriver->image_mutex);
		mTangoDriver->ConvertImageForUse();		// to RGB form
		mTangoDriver->is_image_dirty = false;
		postPicture(mTangoDriver->image_timestamp, mTangoDriver->mTangoImage->ImageData(), 1280, 720);

		//postPicture(mTangoDriver->image_timestamp, TangoDriver::cameraImageBuffer, 1280, 720);
		//pthread_mutex_unlock(&mTangoDriver->image_mutex);
	}

	if (mTangoDriver->is_xyzij_dirty) {
		oc_2_ow_mat_depth = glm::mat4(1.0f);
		oc_2_ow_mat_depth = mTangoDriver->GetOC2OWMat(true);
		pthread_mutex_lock(&mTangoDriver->cloud_mutex);
		postPoints(mTangoDriver->pose_timestamp, mTangoDriver->depth_buffer_size, mTangoDriver->depth_buffer);
		pthread_mutex_unlock(&mTangoDriver->cloud_mutex);
		// Reset xyz_ij dirty flag.
		mTangoDriver->is_xyzij_dirty = false;
		pointCloudValid = true;
	}

	glEnable(GL_DEPTH_TEST);
	//glEnable(GL_CULL_FACE);
	glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
	glClear(GL_DEPTH_BUFFER_BIT | GL_COLOR_BUFFER_BIT);
	glEnable(GL_TEXTURE_2D);

	glm::mat4 oc_2_ow_mat_motion;

	glm::mat4 d_to_ss_mat =
		glm::translate(glm::mat4(1.0f), mTangoDriver->tango_position) * glm::mat4_cast(mTangoDriver->tango_rotation);

	glm::mat4 ow_to_oc_mat = ss_to_ow_mat * d_to_ss_mat * mTangoDriver->GetDvc2IMUMatrixInverse() *
		mTangoDriver->GetCam2IMUMatrix() * oc_to_c_mat;

	if (mTangoDriver != nullptr && !mTangoDriver->masterShutdown && mTangoDriver->mTangoImage != nullptr)
	{
		glDisable(GL_DEPTH_TEST);
			mTangoDriver->UpdateColorTexture();
		video_overlay->Render(glm::mat4(1.0f), glm::mat4(1.0f));
		glEnable(GL_DEPTH_TEST);
	}
	
	projection_mat = projection_mat_ar;
	view_mat = glm::inverse(ow_to_oc_mat);

	oc_2_ow_mat_motion = mTangoDriver->GetOC2OWMat(false);
	cam->SetTransformationMatrix(oc_2_ow_mat_motion);
	oc_2_ow_mat_depth = mTangoDriver->GetOC2OWMat(true);
	bool renderPoints3D = false;
	if (pointCloudValid) {
		
		pthread_mutex_lock(&mTangoDriver->cloud_mutex);
		if (renderPoints3D)
		{
		// Render point cloud based on depth buffer and depth frame transformation.
			pointCloudRenderer->Render(
				cam->GetProjectionMatrix(),
				cam->GetViewMatrix(),
				oc_2_ow_mat_depth,
				mTangoDriver->depth_buffer_size * 3,
				static_cast<float*>(mTangoDriver->depth_buffer)); 
		}
		else
		{
			mScratchTangoImage.BackProjectPixels(mTangoDriver->depth_buffer_size * 3, static_cast<float*>(mTangoDriver->depth_buffer)); 
			float *pts = mScratchTangoImage.GetConvertedPointBuffer();
			glDisable(GL_DEPTH_TEST);
			pointCloudRenderer->Render(glm::mat4(1.0f),
				glm::mat4(1.0f),
				video_overlay->GetTransformationMatrix(),
				mTangoDriver->depth_buffer_size * 3,
				pts); 
			glEnable(GL_DEPTH_TEST);
		}
		pthread_mutex_unlock(&mTangoDriver->cloud_mutex);

	}
	return true;
}
void Tricorder::ResetCameraOffset()
{
	cam_start_angle[0] = cam_cur_angle[0];
	cam_start_angle[1] = cam_cur_angle[1];
	cam_start_dist = cam != nullptr ? cam->GetPosition().z : 0;
}
void Tricorder::SetCameraOffset(float rotationX,float rotationY,float dist)
{
	cam_cur_angle[0] = cam_start_angle[0] + rotationX;
	cam_cur_angle[1] = cam_start_angle[1] + rotationY;
	dist = tango_gl::util::Clamp(cam_start_dist + dist * kZoomSpeed,
		kCamViewMinDist,
		kCamViewMaxDist);
	cam_cur_dist = dist;
}
void Tricorder::SetPointSizeAndColor(float psize, float drange)
{
	if (pointCloudRenderer != NULL) {
		pointCloudRenderer->SetPointSize(psize);
		pointCloudRenderer->SetZRange(drange);
	}
}