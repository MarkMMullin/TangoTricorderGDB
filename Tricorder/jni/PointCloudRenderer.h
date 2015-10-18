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

#ifndef POINT_CLOUD_JNI_EXAMPLE_POINTCLOUD_H_
#define POINT_CLOUD_JNI_EXAMPLE_POINTCLOUD_H_

#include "tango-gl/util.h"

class PointCloudRenderer {
public:
	/** Alternate styles for rendering the 3D point cloud */
	enum ERenderStyle
	{
		/** Render as a true 3D point cloud - will not align with image - primarily for off axis rendering */
		k3DShader,
		/** Render on the image plane, corrected for camera intrinsics -  primarily for on axis rendering */
		kImagePlaneShader
	};
	PointCloudRenderer(ERenderStyle renderStyle = kImagePlaneShader);

	/// Render function take in the depth buffer and the buffer size, and render
	/// the points base on current transformation.
	///
	/// depth_buffer_size: Number of vertices in of the data. Example: 60 floats
	/// in the buffer, the size should be 60/3 = 20;
	///
	/// depth_data_buffer: Pointer to float array contains float triplet of each
	/// vertices in the point cloud.
	void Render(glm::mat4 projection_mat, glm::mat4 view_mat, glm::mat4 model_mat,
		int depth_buffer_size, float* depth_data_buffer);
	/** Set the radius for each point that will be drawn */
	inline void SetPointSize(double v) { mPointSize = v;mProgramDirty = true;}
	/** Set the scaling factor for z depth for point color coding (red = near, blue = far) */
	inline void SetZRange(double v) { mZRange = v;mProgramDirty = true;}
private:
	/** Generate a pure 3D shader for rendering the points in off axis views */
	std::string generate3DVertexShader(double pointSize, double zDistance);
	/** Generate a 2D image plane shader for rendering camera corrected points on on axis views */
	std::string generateImagePlaneVertexShader(double pointSize, double zDistance);
	/** Compile the appropriate program for the render style */
	void compileProgram(double pointSize,double zDistance);
	/** Points that are to be rendered */
	GLuint mVertexBuffers;
	/** ID of compiled shader program */
	GLuint mShaderProgram;
	/** Internal shader vertex store */
	GLuint mShaderVertexStream;
	/** Point transform matrix */
	GLuint mMVPTransform;

	double mPointSize;
	double mZRange;
	bool mProgramDirty;
	/** Specifies how the 3D point cloud will be rendered */
	ERenderStyle mRenderStyle;
};

#endif  // POINT_CLOUD_JNI_EXAMPLE_POINTCLOUD_H_
