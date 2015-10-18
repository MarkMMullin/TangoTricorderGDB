/*
 * Copyright 2014 Google Inc. All Rights Reserved.
 * Changes Copyright 2015 Mark Mullin. All Rights Reserved.
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
#include "VideoOverlay.h"
#include "Tricorder.h"
static const char kVertexShader[] =
        "precision highp float;\n"
                "precision highp int;\n"
                "attribute vec4 vertex;\n"
                "attribute vec2 textureCoords;\n"
                "varying vec2 f_textureCoords;\n"
                "uniform mat4 mvp;\n"
                "void main() {\n"
                "  f_textureCoords = textureCoords;\n"
                "  gl_Position = mvp * vertex;\n"
                "}\n";

static const char kOESFragmentShader[] =
        "#extension GL_OES_EGL_image_external : require\n"
                "precision highp float;\n"
                "precision highp int;\n"
                "uniform samplerExternalOES texture;\n"
                "varying vec2 f_textureCoords;\n"
                "void main() {\n"
                "  gl_FragColor = texture2D(texture, f_textureCoords);\n"
                "}\n";
static const char kFragmentShader[] =
        "varying vec2 f_textureCoords;\n"
                "uniform sampler2D texture;\n"
                "void main() {\n"
                "  gl_FragColor = texture2D(texture, f_textureCoords);\n"
                "}\n";
static const GLfloat kVertices[] =
{
	-1.0,
	1.0,
	0.0,
	-1.0,
	-1.0,
	0.0,
	1.0,
	1.0,
	0.0,
	1.0,
	-1.0,
	0.0
};

static const GLushort kIndices[] =
        { 0, 1, 2, 2, 1, 3 };

static const GLfloat kTextureCoords[] =
        { 0.0, 0.0, 0.0, 1.0, 1.0, 0.0, 1.0, 1.0 };
//GLenum VideoOverlay::texture_type = GL_TEXTURE_EXTERNAL_OES;
GLenum VideoOverlay::texture_type = GL_TEXTURE_2D;

bool primed = false;


VideoOverlay::VideoOverlay() {
	initialize();
}




void VideoOverlay::Render(const glm::mat4& projection_mat,
	const glm::mat4& view_mat) const {

	pthread_mutex_lock(&Tricorder::GetSingleton()->Driver()->image_mutex);

	glTexParameterf(VideoOverlay::texture_type, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
	glTexParameterf(VideoOverlay::texture_type, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
	glTexParameterf(VideoOverlay::texture_type, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
	glTexParameterf(VideoOverlay::texture_type, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

	glUseProgram(shader_program_);
	glm::mat4 model_mat = GetTransformationMatrix();
	glm::mat4 mvp_mat = projection_mat * view_mat * model_mat;
	glUniformMatrix4fv(uniform_mvp_mat_, 1, GL_FALSE, glm::value_ptr(mvp_mat));

	glActiveTexture(GL_TEXTURE0);
	glBindTexture(GL_TEXTURE_2D, texture_id);
	if (primed) {
		glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, 1280, 720, GL_RGBA, GL_UNSIGNED_BYTE, Tricorder::GetSingleton()->Driver()->mTangoImage->ImageData());
	}
	else {
		glTexImage2D(GL_TEXTURE_2D, 0, 3, 1280, 720, 0, GL_RGBA, GL_UNSIGNED_BYTE, Tricorder::GetSingleton()->Driver()->mTangoImage->ImageData());
		primed = true;
	}
	pthread_mutex_unlock(&Tricorder::GetSingleton()->Driver()->image_mutex);

	glUniform1i(uniform_texture, 0);

	    // Bind vertices buffer.
	glBindBuffer(GL_ARRAY_BUFFER, vertex_buffers[0]);
	glEnableVertexAttribArray(attrib_vertices_);
	glVertexAttribPointer(attrib_vertices_, 3, GL_FLOAT, GL_FALSE, 0, nullptr);
	glBindBuffer(GL_ARRAY_BUFFER, 0);

	    // Bind texture coordinates buffer.
	glBindBuffer(GL_ARRAY_BUFFER, vertex_buffers[2]);
	glEnableVertexAttribArray(attrib_textureCoords);
	glVertexAttribPointer(attrib_textureCoords,
		2,
		GL_FLOAT,
		GL_FALSE,
		0,
		nullptr);
	glBindBuffer(GL_ARRAY_BUFFER, 0);

	    // Bind element array buffer.
	glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, vertex_buffers[1]);
	glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_SHORT, nullptr);
	tango_gl::util::CheckGlError("glDrawElements");
	glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);

	glUseProgram(0);
	tango_gl::util::CheckGlError("glUseProgram()");
}

void VideoOverlay::initialize() {
	mInitialized = false;
	primed = false;
	glEnable(GL_VERTEX_PROGRAM_POINT_SIZE);
	shader_program_ = tango_gl::util::CreateProgram(kVertexShader, kFragmentShader);
	if (!shader_program_) {
		LOGE("Could not create program.");
	}

	glEnable(texture_type);
	glGenTextures(1, &texture_id);
	glBindTexture(texture_type, texture_id);
	glTexParameteri(texture_type, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
	glTexParameteri(texture_type, GL_TEXTURE_MAG_FILTER, GL_NEAREST);


	uniform_texture = glGetUniformLocation(shader_program_, "texture");
	glUniform1i(uniform_texture, 0);
	//glBindTexture(texture_type, 0);

	glGenBuffers(3, vertex_buffers);
	// Allocate vertices buffer.
	glBindBuffer(GL_ARRAY_BUFFER, vertex_buffers[0]);
	glBufferData(GL_ARRAY_BUFFER,
		sizeof(GLfloat) * 3 * 4,
		kVertices,
		GL_STATIC_DRAW);
	glBindBuffer(GL_ARRAY_BUFFER, 0);

	    // Allocate triangle indices buffer.
	glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, vertex_buffers[1]);
	glBufferData(GL_ELEMENT_ARRAY_BUFFER,
		sizeof(GLushort) * 6,
		kIndices,
		GL_STATIC_DRAW);
	glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);

	    // Allocate texture coordinates buufer.
	glBindBuffer(GL_ARRAY_BUFFER, vertex_buffers[2]);
	glBufferData(GL_ARRAY_BUFFER,
		sizeof(GLfloat) * 2 * 4,
		kTextureCoords,
		GL_STATIC_DRAW);
	glBindBuffer(GL_ARRAY_BUFFER, 0);

	    // Assign the vertices attribute data.
	attrib_vertices_ = glGetAttribLocation(shader_program_, "vertex");
	glBindBuffer(GL_ARRAY_BUFFER, vertex_buffers[0]);
	glEnableVertexAttribArray(attrib_vertices_);
	glVertexAttribPointer(attrib_vertices_, 3, GL_FLOAT, GL_FALSE, 0, nullptr);
	glBindBuffer(GL_ARRAY_BUFFER, 0);

	    // Assign the texture coordinates attribute data.
	attrib_textureCoords = glGetAttribLocation(shader_program_, "textureCoords");
	glBindBuffer(GL_ARRAY_BUFFER, vertex_buffers[2]);
	glEnableVertexAttribArray(attrib_textureCoords);
	glVertexAttribPointer(attrib_textureCoords,
		2,
		GL_FLOAT,
		GL_FALSE,
		0,
		nullptr);
	glBindBuffer(GL_ARRAY_BUFFER, 0);

	uniform_mvp_mat_ = glGetUniformLocation(shader_program_, "mvp");
}
