#define GLM_FORCE_RADIANS

#include <jni.h>
#include <string>
#include <map>
#include <tango-gl/camera.h>
#include <tango-gl/transform.h>
#include <tango-gl/util.h>

#include "TangoDriver.h"
#include "tango-gl/video_overlay.h"
#include "PointCloudRenderer.h"
#include <tango_client_api.h>
#include "TangoDriver.h"
#include "Tricorder.h"

void loadAvailableADFs(std::map<std::string, std::string>& adfCatalog);
#ifdef __cplusplus
extern "C" {
#endif
	static jobject ManagedGibraltarInstance;
	static jclass ManagedGibraltarClass;
	static JavaVM *g_VM;
	
	JNIEXPORT jint JNICALL
		Java_com_ntx24_tricorder_TangoJNINative_initialize(
		JNIEnv* env, jobject, jobject activity,jobject gibralterInstance) 
	{
		LOGI("Gibralter instance");
		ManagedGibraltarInstance = (jclass)env->NewGlobalRef(gibralterInstance);
		(env)->GetJavaVM(&g_VM);

		// stash a reference to the gibralter class
		LOGI("Gibralter class");
		ManagedGibraltarClass = env->FindClass("com/ntx24/tricorder/Gibraltar");
		ManagedGibraltarClass = (jclass)env->NewGlobalRef(ManagedGibraltarClass);
			
			TangoErrorType err = Tricorder::GetSingleton()->Driver()->Initialize(env, activity);
		if (err != TANGO_SUCCESS) {
			if (err == TANGO_INVALID) {
				LOGE("Tango Service version mis-match");
			}
			else {
				LOGE("Tango Service initialize internal error");
			}
		}
		if (err == TANGO_SUCCESS) {
			LOGI("Tango service initialize success");
		}
		return static_cast<int>(err);
	}

	JNIEXPORT void JNICALL
		Java_com_ntx24_tricorder_TangoJNINative_setupConfig(
		JNIEnv* env, jobject, bool is_auto_recovery) {
			if (!Tricorder::GetSingleton()->Driver()->SetConfig(is_auto_recovery)) {
			LOGE("Tango set config failed");
		}
		else {
			LOGI("Tango set config success");
		}
	}

	JNIEXPORT void JNICALL
		Java_com_ntx24_tricorder_TangoJNINative_connectTexture(
		JNIEnv* env, jobject) {
			Tricorder::GetSingleton()->Driver()->ConnectTexture(Tricorder::GetSingleton()->Video()->GetTextureId());
	}

	JNIEXPORT jint JNICALL
		Java_com_ntx24_tricorder_TangoJNINative_connectService(
		JNIEnv* env, jobject) {

			TangoErrorType err =  Tricorder::GetSingleton()->Driver()->Connect();
		if (err == TANGO_SUCCESS) {
			 Tricorder::GetSingleton()->Driver()->GetExtrinsics();
			 Tricorder::GetSingleton()->Driver()->GetIntrinsics();
			Tricorder::GetSingleton()->SetupIntrinsics();
			LOGI("Tango Service connect success");
		}
		else {
			LOGE("Tango Service connect failed");
		}
		return static_cast<int>(err);
	}

	JNIEXPORT void JNICALL
		Java_com_ntx24_tricorder_TangoJNINative_setupViewport(
		JNIEnv* env, jobject, jint width, jint height) {
			Tricorder::GetSingleton()->SetupViewport(width, height);
	}

	JNIEXPORT void JNICALL
		Java_com_ntx24_tricorder_TangoJNINative_disconnectService(
		JNIEnv* env, jobject) {
		 Tricorder::GetSingleton()->Driver()->Disconnect();
			Tricorder::GetSingleton()->TeardownGraphics();
	}

	JNIEXPORT void JNICALL
		Java_com_ntx24_tricorder_TangoJNINative_onDestroy(
		JNIEnv* env, jobject) {
	}

	JNIEXPORT void JNICALL
		Java_com_ntx24_tricorder_TangoJNINative_setupGraphic(
		JNIEnv* env, jobject) {
			Tricorder::GetSingleton()->SetupGraphics();
	}

	JNIEXPORT void JNICALL
		Java_com_ntx24_tricorder_TangoJNINative_render(
		JNIEnv* env, jobject) {
			Tricorder::GetSingleton()->RenderFrame();
	}

	JNIEXPORT jboolean JNICALL
		Java_com_ntx24_tricorder_TangoJNINative_getIsLocalized(
		JNIEnv* env, jobject) {

		return false;
	}

	JNIEXPORT void JNICALL
		Java_com_ntx24_tricorder_TangoJNINative_resetMotionTracking(
		JNIEnv* env, jobject) {
		 Tricorder::GetSingleton()->Driver()->ResetMotionTracking();
	}



	JNIEXPORT jstring JNICALL
		Java_com_ntx24_tricorder_TangoJNINative_getPoseString(
		JNIEnv* env, jobject) {
		return (env)->NewStringUTF("OBSOLETE");
	}

	JNIEXPORT jstring JNICALL
		Java_com_ntx24_tricorder_TangoJNINative_getVersionNumber(
		JNIEnv* env, jobject) {
		return (env)
			->NewStringUTF( Tricorder::GetSingleton()->Driver()->lib_version_string.c_str());
	}

	// Touching GL interface.
	JNIEXPORT void JNICALL
		Java_com_ntx24_tricorder_TangoJNINative_startSetCameraOffset(
		JNIEnv* env, jobject) {
		Tricorder::GetSingleton()->ResetCameraOffset();
	}

	JNIEXPORT void JNICALL
		Java_com_ntx24_tricorder_TangoJNINative_setCameraOffset(
		JNIEnv* env, jobject, float rotation_x, float rotation_y, float dist) {
			Tricorder::GetSingleton()->SetCameraOffset(rotation_x, rotation_y,dist);
	}

	JNIEXPORT jobjectArray JNICALL
		Java_com_ntx24_tricorder_TangoJNINative_getAvailableADFs(
		JNIEnv* env, jobject,jobject activity) {


		std::map<std::string, std::string> adfCatalog;
		TangoErrorType t = TangoService_initialize(env, activity);
		if(t == TANGO_SUCCESS )
			loadAvailableADFs(adfCatalog);
		TangoService_disconnect();
		int catlen = adfCatalog.size();
		jobjectArray ret = (jobjectArray)env->NewObjectArray(catlen, env->FindClass("java/lang/String"), env->NewStringUTF(""));
		std::map<std::string, std::string>::iterator iter;
		int arrayIndex = 0;
		for (iter = adfCatalog.begin(); iter != adfCatalog.end(); ++iter) {
			std::string sc = iter->first;
			char namebuf[128];
			int i;
			for (i = 0; i < sc.size(); i++)
				namebuf[i] = sc[i];
			namebuf[i] = '\0';
			jstring adfname = env->NewStringUTF(namebuf);
			env->SetObjectArrayElement(ret, arrayIndex++, adfname);
		}
		return ret;
	}

	JNIEXPORT jboolean JNICALL
		Java_com_ntx24_tricorder_TangoJNINative_selectADF(
		JNIEnv* env, jobject, jstring adfNameStr) {
		std::map<std::string, std::string> adfCatalog;
				loadAvailableADFs(adfCatalog);
		const char *adfName = env->GetStringUTFChars(adfNameStr, 0);
		std::string adfName1 = std::string(adfName);
		std::string adfGuid = adfCatalog[std::string(adfName1)];
		bool result =  Tricorder::GetSingleton()->Driver()->LoadADF(adfGuid);
		env->ReleaseStringUTFChars(adfNameStr, adfName);
		return result;
	}
	JNIEXPORT jboolean JNICALL
	Java_com_ntx24_tricorder_TangoJNINative_saveADF(JNIEnv* env, jobject) {
		// only save if it's really localized
		if(! Tricorder::GetSingleton()->Driver()->is_localized)
			return false;
		return  Tricorder::GetSingleton()->Driver()->SaveADF();
	}
	JNIEXPORT jdoubleArray JNICALL
		Java_com_ntx24_tricorder_TangoJNINative_getIMUFrame(JNIEnv* env, jobject) {
			jdoubleArray result = env->NewDoubleArray(14);
			jdouble buffer[14];
			int i = 0;
			glm::vec3 p =  Tricorder::GetSingleton()->Driver()->GetDvcIMUPosition();
			glm::quat q =  Tricorder::GetSingleton()->Driver()->GetDvcIMUAttitude();
			buffer[i++] = p.x; buffer[i++] = p.y; buffer[i++] = p.z;
			buffer[i++] = q.x; buffer[i++] = q.y; buffer[i++] = q.z; buffer[i++] = q.w;
			p =  Tricorder::GetSingleton()->Driver()->GetCamIMUPosition();
			q =  Tricorder::GetSingleton()->Driver()->GetCamIMUAttitude();
			buffer[i++] = p.x; buffer[i++] = p.y; buffer[i++] = p.z;
			buffer[i++] = q.x; buffer[i++] = q.y; buffer[i++] = q.z; buffer[i++] = q.w;
			env->SetDoubleArrayRegion(result, 0, 14, buffer);
			return result;
	} 
	JNIEXPORT jdoubleArray JNICALL
		Java_com_ntx24_tricorder_TangoJNINative_getCameraIntrinsics(JNIEnv* env, jobject) {
			jdoubleArray result = env->NewDoubleArray(11);
			jdouble b[11];
			int i = 0;
			b[i++] =  Tricorder::GetSingleton()->Driver()->cc_width;
			b[i++] =  Tricorder::GetSingleton()->Driver()->cc_height;
			b[i++] =  Tricorder::GetSingleton()->Driver()->cc_fx;
			b[i++] =  Tricorder::GetSingleton()->Driver()->cc_fy;
			b[i++] =  Tricorder::GetSingleton()->Driver()->cc_cx;
			b[i++] =  Tricorder::GetSingleton()->Driver()->cc_cy;
			for (int j = 0; j < 5;j++)
				b[i++] =  Tricorder::GetSingleton()->Driver()->cc_distortion[j];
			env->SetDoubleArrayRegion(result, 0, 11, b);
			return result;
	}
	JNIEXPORT void Java_com_ntx24_tricorder_TangoJNINative_setConfigFlag(JNIEnv* env, jobject,int flagId,jboolean state) {
		 Tricorder::GetSingleton()->Driver()->SetConfigFlag(NULL,(EConfigurationOption) flagId,state);
	}
	JNIEXPORT void Java_com_ntx24_tricorder_TangoJNINative_setCapturePointDisplayCoefficients(JNIEnv* env, jobject,double psize,double drange) {
		Tricorder::GetSingleton()->SetPointSizeAndColor(psize, drange);
	}

#ifdef __cplusplus
}
#endif


//////////////////////////////////////
/// CALLBACKS TO JAVA
//////////////////////////////////////
JNIEnv* getjnienv(bool& threadAttached)
{
	JNIEnv * env;
	threadAttached = false;
	int getEnvStat = g_VM->GetEnv((void **)&env, JNI_VERSION_1_6);
	if (getEnvStat == JNI_EDETACHED) {

		if (g_VM->AttachCurrentThread(&env, NULL) != 0) {
			return NULL;
		}
		threadAttached = true;
	}
	else if (getEnvStat == JNI_EVERSION) {
		return NULL;
	}
	return env;
}
bool signalLocalizationStateChange(bool state)
{
	std::string str = state ?  Tricorder::GetSingleton()->Driver()->_proposedReferenceFrame : "00000000-0000-0000-0000-000000000000";
	bool threadAttached = false;
	JNIEnv * env = getjnienv(threadAttached);
	jstring frameIdJ = env->NewStringUTF(str.c_str());
	jmethodID mid = env->GetMethodID(ManagedGibraltarClass, "signalLocalizationStateChange", "(Ljava/lang/String;)V");
	env->CallVoidMethod(ManagedGibraltarInstance, mid, frameIdJ);
	return true;
}
jbyteArray stringToJByteArray(JNIEnv *env,std::string& theString)
{   
	int byteCount = theString.length();
	const jbyte* pNativeMessage = reinterpret_cast<const jbyte*>(theString.c_str());
	jbyteArray bytes = env->NewByteArray(byteCount);
	env->SetByteArrayRegion(bytes, 0, byteCount, pNativeMessage);
	return bytes;
}
static jmethodID sCachedReportEventMethodId = 0;
void reportTangoEvent(const TangoEvent* theEvent)
{
	bool threadAttached = false;
	JNIEnv * env = getjnienv(threadAttached);

	// call the return function
	if (sCachedReportEventMethodId == 0)
		sCachedReportEventMethodId = env->GetMethodID(ManagedGibraltarClass, "reportEvent", "(Ljava/lang/String;Ljava/lang/String;DI)V");
	jstring eKeyStr = env->NewStringUTF(theEvent->event_key);
	jstring eValStr = env->NewStringUTF(theEvent->event_value);


	env->CallVoidMethod(ManagedGibraltarInstance, sCachedReportEventMethodId, eKeyStr, eValStr, theEvent->timestamp, theEvent->type);
	env->DeleteLocalRef(eKeyStr);
	env->DeleteLocalRef(eValStr);
	if (env->ExceptionCheck()) {
		env->ExceptionDescribe();
	}
	if (threadAttached)
		g_VM->DetachCurrentThread();
}
bool postPose(double timestamp, int channelId, double px, double py, double pz, double ax, double ay, double az, double aw, int statusCode) {
	bool threadAttached = false;
	JNIEnv * env = getjnienv(threadAttached);

	// build the return array
	jdoubleArray result = env->NewDoubleArray(9);
	jdouble fill[9];
	fill[0] = timestamp;
	fill[1] = channelId;
	fill[2] = px; fill[3] = py; fill[4] = pz;
	fill[5] = ax; fill[6] = ay; fill[7] = az; fill[8] = aw;
	env->SetDoubleArrayRegion(result, 0, 9, fill);
	// call the return function
	jmethodID mid = env->GetMethodID(ManagedGibraltarClass, "queuePose", "([DI)V");
	env->CallVoidMethod(ManagedGibraltarInstance, mid, result);
	if (env->ExceptionCheck()) {
		env->ExceptionDescribe();
	}
	if (threadAttached)
		g_VM->DetachCurrentThread();
	env->DeleteLocalRef(result);
	return true;
}
void reportPoseStatus(int statusCode) {
	bool threadAttached = false;
	JNIEnv * env = getjnienv(threadAttached);

	// call the return function
	jmethodID mid = env->GetMethodID(ManagedGibraltarClass, "reportPoseStatus", "(I)V");
	env->CallVoidMethod(ManagedGibraltarInstance, mid, statusCode);
	if (env->ExceptionCheck()) {
		env->ExceptionDescribe();
	}
	if (threadAttached)
		g_VM->DetachCurrentThread();
}
bool postPoints(double timestamp, int numPoints, float *points) {
	JNIEnv * env;
	// double check it's all ok
	bool threadAttached = false;
	int getEnvStat = g_VM->GetEnv((void **)&env, JNI_VERSION_1_6);
	if (getEnvStat == JNI_EDETACHED) {

		if (g_VM->AttachCurrentThread(&env, NULL) != 0) {
			return false;
		}
		threadAttached = true;
	}
	else if (getEnvStat == JNI_OK) {
		//
	}
	else if (getEnvStat == JNI_EVERSION) {
		return false;
	}

	// build the return array
	jfloatArray result = env->NewFloatArray(numPoints);
	//jfloat* fill = (jfloat*)malloc(numPoints * sizeof(jfloat));
	//for (int i = 0; i < numPoints; i++) fill[i] = (float)points[i];
	env->SetFloatArrayRegion(result, 0, numPoints, points);
	// call the return function
	jmethodID mid = env->GetMethodID(ManagedGibraltarClass, "queuePoints", "(D[F)V");
	env->CallVoidMethod(ManagedGibraltarInstance, mid, timestamp,result);
	if (env->ExceptionCheck()) {
		env->ExceptionDescribe();
	}
	if (threadAttached)
		g_VM->DetachCurrentThread();
	//free(fill);
	env->DeleteLocalRef(result);
	return true;
}
static jstring cached_config_name = 0;
static jclass bcfg_class;
static jobject java_bitmap_config;
static jclass java_bitmap_class;
static jmethodID createBitmapMID;
static jmethodID setPixelsMid;
static jintArray pixels;
static unsigned int *swaparray;
bool postPicture(double timestamp, unsigned char* rawbits, int width, int height) {
	JNIEnv * env;
	// double check it's all ok
	bool threadAttached = false;
	int getEnvStat = g_VM->GetEnv((void **)&env, JNI_VERSION_1_6);
	if (getEnvStat == JNI_EDETACHED) {

		if (g_VM->AttachCurrentThread(&env, NULL) != 0) {
			return false;
		}
		threadAttached = true;
	}
	else if (getEnvStat == JNI_OK) {
		//
	}
	else if (getEnvStat == JNI_EVERSION) {
		return false;
	}
	
	int i;

	// generate the config name for the postPicture callback
	jstring j_config_name;
	if (cached_config_name == 0) {
		jstring j_config_name;
		const wchar_t configName[] = L"ARGB_8888";
		int len = wcslen(configName);
		if (sizeof(wchar_t) != sizeof(jchar)) {
			//wchar_t is defined as different length than jchar(2 bytes)
			jchar* str = (jchar*)malloc((len + 1)*sizeof(jchar));
			for (int j = 0; j < len; ++j) {
				str[j] = (jchar)configName[j];
			}
			str[len] = 0;
			j_config_name = env->NewString((const jchar*)str, len);
			free(str);
		}
		else {
			//wchar_t is defined same length as jchar(2 bytes)
			j_config_name = env->NewString((const jchar*)configName, len);
		}
		cached_config_name = (jstring)env->NewGlobalRef(j_config_name);
		bcfg_class = env->FindClass("android/graphics/Bitmap$Config");
		bcfg_class = (jclass)env->NewGlobalRef(bcfg_class);
		jmethodID smid = env->GetStaticMethodID(bcfg_class, "valueOf", "(Ljava/lang/String;)Landroid/graphics/Bitmap$Config;");
		java_bitmap_config = env->CallStaticObjectMethod(bcfg_class, smid, j_config_name);
		java_bitmap_config = (jobject)env->NewGlobalRef(java_bitmap_config);
		java_bitmap_class = (jclass)env->FindClass("android/graphics/Bitmap");
		java_bitmap_class = (jclass)env->NewGlobalRef(java_bitmap_class); 
		setPixelsMid = env->GetMethodID(java_bitmap_class, "setPixels", "([IIIIIII)V");
		createBitmapMID = env->GetStaticMethodID(java_bitmap_class, "createBitmap", "(IILandroid/graphics/Bitmap$Config;)Landroid/graphics/Bitmap;");
		pixels = env->NewIntArray(width * height);
		pixels = (jintArray) env->NewGlobalRef(pixels);

		swaparray = new unsigned int[width * height];
	}
	j_config_name = cached_config_name;

	jobject java_bitmap = env->CallStaticObjectMethod(java_bitmap_class, createBitmapMID, width, height, java_bitmap_config);
	if (java_bitmap == 0)
		return false;

	//pthread_mutex_lock(& Tricorder::GetSingleton()->Driver()->image_mutex);
	unsigned int *rotateBits = (unsigned int*) rawbits;
	unsigned int *writetarget = &swaparray[0];
	for(int i = 0;i < width * height;i++) {

		unsigned int toRotate = *rotateBits++;
		unsigned char* wtb = (unsigned char*) writetarget;
		unsigned char* rtb = (unsigned char*) &toRotate;
		wtb[0] = (unsigned char) rtb[2];			// blue ?
		wtb[1] = (unsigned char) rtb[1];		// green
		wtb[2] = (unsigned char) rtb[0];
		wtb[3] = (unsigned char) 0xFF;
		writetarget++;
	}

	env->SetIntArrayRegion(pixels, 0, width * height, (jint*)swaparray);
	//pthread_mutex_unlock(& Tricorder::GetSingleton()->Driver()->image_mutex);
	 Tricorder::GetSingleton()->Driver()->is_image_dirty = false;
	env->CallVoidMethod(java_bitmap, setPixelsMid, (jintArray)pixels, 0, width, 0, 0, width, height);

	
	// call the return function
	jmethodID mid = env->GetMethodID(ManagedGibraltarClass, "queuePicture", "(DLandroid/graphics/Bitmap;)V");
	env->CallVoidMethod(ManagedGibraltarInstance, mid, (jdouble) timestamp, java_bitmap);
	if (env->ExceptionCheck()) {
		env->ExceptionDescribe();
	}
	if (threadAttached)
		g_VM->DetachCurrentThread();
	return true;
}


/// support functions
void loadAvailableADFs(std::map<std::string, std::string>& adfCatalog)
{
	// Load the most recent ADF.
	char* uuid_list;

	// uuid_list will contain a comma separated list of UUIDs.
	if (TangoService_getAreaDescriptionUUIDList(&uuid_list) != TANGO_SUCCESS) {
		return ;
	}

	// Parse the uuid_list to get the individual uuids.
	if (uuid_list != NULL && uuid_list[0] != '\0') {
		std::vector<std::string> adf_list;
		std::vector<std::string> name_list;

		char* parsing_char;
		char* saved_ptr;
		char *name;
		parsing_char = strtok_r(uuid_list, ",", &saved_ptr);
		while (parsing_char != NULL) {
			std::string s = std::string(parsing_char);
			TangoAreaDescriptionMetadata metadata;
			const char* key = "name";
			size_t size = 0;
			if (TangoService_getAreaDescriptionMetadata(s.c_str(), &metadata) == TANGO_SUCCESS)
				TangoAreaDescriptionMetadata_get(metadata, key, &size, &name);
			std::string fullName = std::string(name);
			TangoAreaDescriptionMetadata_free(metadata);
			adf_list.push_back(s);
			adfCatalog[fullName] = s;
			parsing_char = strtok_r(NULL, ",", &saved_ptr);
		}
	}
}
