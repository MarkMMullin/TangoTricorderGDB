#ifndef TRICORDER_TRIDCORDER_CPP_H
#define TRICORDER_TRIDCORDER_CPP_H
#include "TangoDriver.h"
#include "tango-gl/camera.h"
#include "tango-gl/util.h"
#include "tango-gl/transform.h"
#include "PointCloudRenderer.h"
#include "VideoOverlay.h"
#include "TangoDriver.h"

const float kZero = 0.0f;
const glm::vec3 kZeroVec3 = glm::vec3(0.0f, 0.0f, 0.0f);
const glm::vec3 kOneVec3 = glm::vec3(1.0f, 1.0f, 1.0f);
const glm::quat kZeroQuat = glm::quat(1.0f, 0.0f, 0.0f, 0.0f);

class Tricorder {
public:
	static Tricorder* GetSingleton();
	
	void SetupIntrinsics();
	/** Called from Java to initialize the tricorder system subsystem. */
	bool SetupGraphics();
	bool TeardownGraphics();
	/** Update viewport according to surface dimensions, always use image plane's ratio and make full use of the screen. */
	void SetupViewport(int w, int h);
	bool RenderFrame();
	TangoDriver* Driver() { return mTangoDriver;}
	VideoOverlay* Video() { return video_overlay; }
	void ResetCameraOffset();
	void SetCameraOffset(float rotationX, float rotationY, float dist);
	void SetPointSizeAndColor(float psize, float drange);
private:
	Tricorder();
	static Tricorder* smSingleton;
	TangoDriver* mTangoDriver;
	float image_width;
	float image_height;
	/**  Color Camera image plane ratio. */
	float image_plane_ratio;
	/**  Color Camera image plane distance to view point. */
	float image_plane_dis;
	float image_plane_dis_original;
	/** Render camera's parent transformation.
	This object is a pivot transformtion for render camera to rotate around.
*/
	tango_gl::Transform* cam_parent_transform;
	
/** Camera defining viewpoint */
	tango_gl::Camera *cam;
	
/** Handles the actual drawing of the point cloud */
	PointCloudRenderer* pointCloudRenderer;
/** Renders the live video background */
	VideoOverlay *video_overlay;
	/** True if the current point cloud is valid and has not been processed */
	bool pointCloudValid;
	bool last_localizationStatus = false;
	int last_localizationCount = 0;
	glm::mat4 oc_2_ow_mat_depth;
	/**  Start of Service with respect to Opengl World matrix. */
	glm::mat4 ss_to_ow_mat;
/**  Opengl Camera with respect to Color Camera matrix. */
	glm::mat4 oc_to_c_mat;
	/**  Projection matrix from render camera. */
	glm::mat4 projection_mat;
	/**  First person projection matrix from color camera intrinsics. */
	glm::mat4 projection_mat_ar;
	/**  First person view matrix from color camera extrinsics. */
	glm::mat4 view_mat;
	TangoImage mScratchTangoImage;
	// Single finger touch positional values.
// First element in the array is x-axis touching position.
// Second element in the array is y-axis touching position.
	float cam_start_angle[2];
	float cam_cur_angle[2];

	// Double finger touch distance value.
	float cam_start_dist;
	float cam_cur_dist;
};
#endif
