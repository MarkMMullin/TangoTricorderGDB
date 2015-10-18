#ifndef TRICORDER_TANGOIMAGE_CPP_H
#define TRICORDER_TANGOIMAGE_CPP_H
class TangoImage {
public:
	TangoImage();
	void BackProjectPixels(int depth_buffer_size, float *depth_data_buffer);
	float* GetConvertedPointBuffer();
	void LoadYUVImage(unsigned char* rawbits);
	unsigned char* ImageData() const { return mImageRGBA;}
private:
	void pictureUV(float x, float y, float z, int* u, int* v);
	unsigned char* mImageRGBA;
};	
#endif 