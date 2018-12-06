//
// Created by TG on 2018/12/3.
//

#ifndef AUDIOOPERATION_JAVARECVWORK_H
#define AUDIOOPERATION_JAVARECVWORK_H
#define JNIREG_CLASS "com/lzb/record/effect/EffectUtils"  //指定要注册的类

#define LOGI(FORMAT, ...) __android_log_print(ANDROID_LOG_INFO,"kpioneer",FORMAT,##__VA_ARGS__);
#define LOGD(FORMAT, ...) __android_log_print(ANDROID_LOG_INFO,"kpioneer",FORMAT,##__VA_ARGS__);
#define LOGE(FORMAT, ...) __android_log_print(ANDROID_LOG_ERROR,"kpioneer",FORMAT,##__VA_ARGS__);

#include <jni.h>
#include <string>
#include <android/log.h>

using namespace std;

class CJavaRecvWork {

public:
    CJavaRecvWork();

    ~CJavaRecvWork();

    void SetJCJB(JNIEnv *env, jclass jc, jobject obj);

    void onSlowPCMData(char *pBuffer, int len);

private:
    jclass m_jWorkSockclass;
    jobject m_jobject;
};

#endif //AUDIOOPERATION_JAVARECVWORK_H
