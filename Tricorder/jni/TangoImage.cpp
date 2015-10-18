#include "TangoImage.h"
#include "TangoDriver.h"

static int _cacheBufferSize = 0;
static float *_cacheBufferStore = nullptr;
TangoImage::TangoImage()
{
	mImageRGBA = nullptr;
}
float* TangoImage::GetConvertedPointBuffer()
{
	return _cacheBufferStore;
}
void TangoImage::BackProjectPixels(int depth_buffer_size, float *depth_data_buffer)
{
	if (_cacheBufferSize < depth_buffer_size)
	{
		if (_cacheBufferStore != nullptr)
			delete[] _cacheBufferStore;
		_cacheBufferStore = new float[depth_buffer_size];
	}
	for (int i = 0; i < depth_buffer_size; i += 3)
	{
		float x = depth_data_buffer[i];
		float y = depth_data_buffer[i + 1];
		float z = depth_data_buffer[i + 2];
		
		int u, v;
		pictureUV(x, y, z, &u, &v);
		
		x = (u / 1280.0) * 2.0 - 1.0;
		y = (v / 720.0) * 2.0 - 1.0;
		_cacheBufferStore[i] = x;
		_cacheBufferStore[i+1] = y;
		_cacheBufferStore[i+2] = z;
		// nb - since z is being returned as is, please note this point
		// has been effectively distorted
		//int addr = 1280 * 4 * v + (u * 4);
		//TangoDriver::cameraImageBufferRGBA[addr] = 255;
		//TangoDriver::cameraImageBufferRGBA[addr + 1] = 255;
		//TangoDriver::cameraImageBufferRGBA[addr + 2] = 255;
	}
}

typedef struct yuv2rgb_rgb_t {
	int r, g, b;
} yuv2rgb_rgb_t;

static void rgb_calc(yuv2rgb_rgb_t* rgb, int Y, int Cr, int Cb) {
	rgb->b = Y + Cr + (Cr >> 2) + (Cr >> 3) + (Cr >> 5);
	if (rgb->b < 0)
		rgb->b = 0;
	else if (rgb->b > 255)
		rgb->b = 255;

	rgb->g = Y - (Cb >> 2) + (Cb >> 4) + (Cb >> 5) - (Cr >> 1) + (Cr >> 3) + (Cr >> 4) + (Cr >> 5);
	if (rgb->g < 0)
		rgb->g = 0;
	else if (rgb->g > 255)
		rgb->g = 255;

	rgb->r = Y + Cb + (Cb >> 1) + (Cb >> 2) + (Cb >> 6);
	if (rgb->r < 0)
		rgb->r = 0;
	else if (rgb->r > 255)
		rgb->r = 255;
}

#define YUV2RGB_SET_RGB(p, rgb) *p++ = (unsigned char)rgb.r; *p++ = (unsigned char)rgb.g; *p++ = (unsigned char)rgb.b; *p++ = 0xff
#define IMAGEHEIGHT 720
#define IMAGEWIDTH 1280

void TangoImage::pictureUV(float x, float y, float z, int* u, int* v)
{
	// http://stackoverflow.com/questions/30640149/how-to-color-point-cloud-from-image-pixels
	float dx = x / z;
	float dy = y / z;

	float r2 = (dx * dx) + (dy * dy);

	float r4 = r2*r2;
	float r6 = r2*r4;
	float intrinsicMult = 1.0f + 0.228532999753952f*r2 + -0.663019001483917f*r4 + 0.642908990383148f*r6;
	dx = dx * intrinsicMult;
	dy = dy * intrinsicMult;

	dx = 1042.73999023438f * dx + 637.273986816406f;
	dy = 1042.96997070313f * dy + 352.928985595703;
	if (dx < 0)
		dx = 0;
	if (dx > 1279)
		dx = 1279;
	if (dy < 0)
		dy = 0;
	if (dy > 719)
		dy = 719;
	*u = static_cast<int>(dx);
	*v = static_cast<int>(dy);
}

void TangoImage::LoadYUVImage(unsigned char* rawbits)
{
	// http://en.wikipedia.org/wiki/YUV
	if(mImageRGBA == nullptr)
		mImageRGBA = static_cast<unsigned char*>(malloc(1280 * 720 * 4));
	const int width4 = IMAGEWIDTH * 4;
	unsigned char const* y0_ptr = rawbits;
	unsigned char const* y1_ptr = rawbits + IMAGEWIDTH;
	unsigned char const* cb_ptr = rawbits + (IMAGEWIDTH * IMAGEHEIGHT);
	unsigned char const* cr_ptr = cb_ptr + 1;
	unsigned char* rgba0 = mImageRGBA;
	unsigned char* rgba1 = mImageRGBA + width4;
	int Y00, Y01, Y10, Y11;
	int Cr;
	int Cb;
	int r, c;
	yuv2rgb_rgb_t rgb00, rgb01, rgb10, rgb11;
	for (r = 0; r < IMAGEHEIGHT / 2; ++r) {
		for (c = 0; c < IMAGEWIDTH / 2; ++c, cr_ptr += 2, cb_ptr += 2) {
			Cr = *cr_ptr;
			Cb = *cb_ptr;
			if (Cb < 0)
				Cb += 127;
			else
				Cb -= 128;
			if (Cr < 0)
				Cr += 127;
			else
				Cr -= 128;
			Y00 = *y0_ptr++;
			Y01 = *y0_ptr++;
			Y10 = *y1_ptr++;
			Y11 = *y1_ptr++;
			rgb_calc(&rgb00, Y00, Cr, Cb);
			rgb_calc(&rgb01, Y01, Cr, Cb);
			rgb_calc(&rgb10, Y10, Cr, Cb);
			rgb_calc(&rgb11, Y11, Cr, Cb);
			YUV2RGB_SET_RGB(rgba0, rgb00);
			YUV2RGB_SET_RGB(rgba0, rgb01);
			YUV2RGB_SET_RGB(rgba1, rgb10);
			YUV2RGB_SET_RGB(rgba1, rgb11);
		}
		y0_ptr += IMAGEWIDTH;
		y1_ptr += IMAGEWIDTH;
		rgba0 += width4;
		rgba1 += width4;
	}

}
#if 0
void LegacyLoadImage()
{
#if 0
	cameraImageBufferRGBA = cameraImageBuffer;
#else
#if 0
#if 1
	int uOffset = size / 4 + size;
	int halfstride = TangoDriver::imageBufferStride / 2;
	for (int i = 0; i < TangoDriver::imageBufferHeight; ++i)
	{
		halfi = i / 2;
		uvOffset = halfi * halfstride;
		for (int j = 0; j < TangoDriver::imageBufferWidth; ++j)
		{
			halfj = j / 2;
			uvOffsetHalfj = uvOffset + halfj;
			y_scaled = pData[i * TangoDriver::imageBufferStride + j] * invByte;
			v_scaled = 2 * (pData[uvOffsetHalfj + size] * invByte - 0.5f) * Vmax;
			u_scaled = 2 * (pData[uvOffsetHalfj + uOffset] * invByte - 0.5f) * Umax;
			*iData++ = (unsigned char)((y_scaled + 1.13983f * v_scaled) * 255.0); 
			;
			*iData++ = (unsigned char)((y_scaled - 0.39465f * u_scaled - 0.58060f * v_scaled) * 255.0);
			*iData++ = (unsigned char)((y_scaled + 2.03211f * u_scaled) * 255.0);
			*iData++ = 255;
		}
	}
#else
	for (int i = 0; i < TangoDriver::imageBufferHeight; ++i)
	{
		for (int j = 0; j < TangoDriver::imageBufferWidth; ++j)
		{
			unsigned char y = pData[i * image->stride + j];
			unsigned char v = pData[(i / 2) * (TangoDriver::imageBufferStride / 2) + (j / 2) + size];
			unsigned char u = pData[(i / 2) * (TangoDriver::imageBufferStride / 2) + (j / 2) + size + (size / 4)];
			YUV2RGB(y, u, v);
			*iData++ = y;
			*iData++ = u;
			*iData++ = v;
			*iData++ = 255;
		}
	}
#endif
#else
	yuv420sp_to_rgba(pData, TangoDriver::imageBufferWidth, TangoDriver::imageBufferHeight, iData);
#endif
#endif
}
#endif