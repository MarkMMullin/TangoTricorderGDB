#include "PointCloudRenderer.h"
#include "TangoDriver.h"
#include "Tricorder.h"

static const glm::mat4 inverse_z_mat = glm::mat4(1.0f,
	0.0f,
	0.0f,
	0.0f,
	0.0f,
	-1.0f,
	0.0f,
	0.0f,
	0.0f,
	0.0f,
	-1.0f,
	0.0f,
	0.0f,
	0.0f,
	0.0f,
	1.0f);
static const char kFragmentShader[] = "varying vec4 v_color;\n"
"void main() {\n"
"  gl_FragColor = vec4(v_color);\n"
"}\n";

std::string PointCloudRenderer::generate3DVertexShader(double pointSize, double zDistance) 
{
	char psb[32];
	char zdb[32];
	sprintf(psb, "%f", pointSize);
	sprintf(zdb, "%f", zDistance);
	std::string vs;
	vs.append("attribute vec4 vertex;\n");
	vs.append("uniform mat4 mvp;\n");
	vs.append("varying vec4 v_color;\n");
	vs.append("void main() {\n");
	vs.append("  gl_PointSize = " + std::string(psb) + ";\n");
	vs.append("  gl_Position = mvp*vertex;\n");
	vs.append("  float gradient = vertex.z/" + std::string(zdb) + ";");
	vs.append("  float invgradient = 1.0-gradient;");
	vs.append("  v_color = vec4(invgradient, 0.,gradient , 1.);\n");
	vs.append("}\n");
	return vs;
}

std::string PointCloudRenderer::generateImagePlaneVertexShader(double pointSize, double zDistance) 
{
	char psb[32];
	char zdb[32];
	sprintf(psb, "%f", pointSize);
	sprintf(zdb, "%f", zDistance);
	std::string vs;
	vs.append("attribute vec4 vertex;\n");
	vs.append("uniform mat4 mvp;\n");
	vs.append("varying vec4 v_color;\n");
	vs.append("void main() {\n");
	vs.append("  gl_PointSize = " + std::string(psb) + ";\n");
	vs.append("  gl_Position = mvp*vertex;\n");
	vs.append("  float gradient = vertex.z/" + std::string(zdb) + ";");
	vs.append("  float invgradient = 1.0-gradient;");
	vs.append("  v_color = vec4(invgradient, 0.,gradient , 1.);\n");
	vs.append("  vertex.z = 0;\n");
	vs.append("  gl_Position = mvp*vertex;\n");
	vs.append("}\n");
	return vs;
}

void PointCloudRenderer::compileProgram(double pointSize, double zDistance) {
	if (mShaderProgram != 0)
		glDeleteProgram(mShaderProgram);
	std::string vs = mRenderStyle == kImagePlaneShader ? generateImagePlaneVertexShader(mPointSize, mZRange) 
		: generate3DVertexShader(mPointSize, mZRange);
	mShaderProgram = tango_gl::util::CreateProgram(vs.c_str(), kFragmentShader);
	if (!mShaderProgram) {
		LOGE("Could not create program.");
	}
	mMVPTransform = glGetUniformLocation(mShaderProgram, "mvp");
	mShaderVertexStream = glGetAttribLocation(mShaderProgram, "vertex");
	mProgramDirty = false;
}


PointCloudRenderer::PointCloudRenderer(ERenderStyle renderStyle) {
	mRenderStyle = renderStyle;
	glEnable(GL_VERTEX_PROGRAM_POINT_SIZE);
	mPointSize = 5.0;
	mZRange = 5.0;
	mProgramDirty = true;
	mShaderProgram = 0;
	mShaderVertexStream = 0;
	mMVPTransform = 0;
	glGenBuffers(1, &mVertexBuffers);
}

void PointCloudRenderer::Render(glm::mat4 projection_mat, glm::mat4 view_mat, glm::mat4 model_mat, int depth_buffer_size, float *depth_data_buffer)
{
	// Lock xyz_ij mutex.
	if (mProgramDirty)
		compileProgram(mPointSize, mZRange);
	glUseProgram(mShaderProgram);

	glm::mat4 mvp_mat = projection_mat * view_mat * model_mat * inverse_z_mat;
	glUniformMatrix4fv(mMVPTransform, 1, GL_FALSE, glm::value_ptr(mvp_mat));

	// Bind vertex buffer.
	glBindBuffer(GL_ARRAY_BUFFER, mVertexBuffers);
	//pthread_mutex_lock(&Tricorder::GetSingleton()->Driver()->xyzij_mutex);
	glBufferData(GL_ARRAY_BUFFER,
		sizeof(GLfloat) * depth_buffer_size,
		depth_data_buffer,
		GL_STATIC_DRAW);
	glEnableVertexAttribArray(mShaderVertexStream);
	glVertexAttribPointer(mShaderVertexStream, 3, GL_FLOAT, GL_FALSE, 0, nullptr);
	glBindBuffer(GL_ARRAY_BUFFER, 0);

	glDrawArrays(GL_POINTS, 0, 3 * depth_buffer_size);
	//pthread_mutex_unlock(&Tricorder::GetSingleton()->Driver()->xyzij_mutex);

	// Unlock xyz_ij mutex.

	tango_gl::util::CheckGlError("glDrawArray()");
	glUseProgram(0);
	tango_gl::util::CheckGlError("glUseProgram()");
}
