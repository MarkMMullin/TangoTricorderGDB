#ifndef TRICORDER_VIDEOOVERLAY_CPP_H
#define TRICORDER_VIDEOOVERLAY_CPP_H

#include "tango-gl/drawable_object.h"

class VideoOverlay : public tango_gl::DrawableObject {
public:
    VideoOverlay();
    void Render(const glm::mat4& projection_mat, const glm::mat4& view_mat) const;
	GLuint GetTextureId() { return texture_id;}
    static GLenum texture_type;
private:
    void initialize();
    bool mInitialized;
    GLuint vertex_buffers_;

    GLuint attrib_vertices_;
    GLuint attrib_textureCoords;
    GLuint uniform_texture;
    GLuint vertex_buffers[3];
    GLuint shader_program_;

    GLuint uniform_mvp_mat_;
	GLuint texture_id;
};
#endif //TRICORDER_VIDEOOVERLAY_CPP_H
