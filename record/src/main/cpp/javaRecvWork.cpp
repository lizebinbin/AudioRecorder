//
// Created by TG on 2018/12/3.
//
#include "javaRecvWork.h"
#include <string.h>
#include <string>
#include <jni.h>

using namespace std;

extern JavaVM *g_pVM;

CJavaRecvWork::CJavaRecvWork() {
    m_jWorkSockclass = 0;
    m_jobject = 0;
}

CJavaRecvWork::~CJavaRecvWork() {
    int status;
    JNIEnv *env = 0;
    status = (g_pVM)->AttachCurrentThread(&env, 0);
    if (env) {
        if (m_jobject != 0) env->DeleteGlobalRef(m_jobject);
        if (m_jWorkSockclass != 0) env->DeleteGlobalRef(m_jWorkSockclass);
    }

    if (status > 0) g_pVM->DetachCurrentThread();
}

void CJavaRecvWork::SetJCJB(JNIEnv *env, jclass jc, jobject obj) {
    if (m_jWorkSockclass == 0) {
        m_jWorkSockclass = (jclass) env->NewGlobalRef(jc);
        if (m_jWorkSockclass)
            LOGI("%s", "JNI CJavaRecvWork--SetJCJB(m_jWorkSockclass)");
    }


    if (m_jobject == 0) {
        m_jobject = env->NewGlobalRef(obj);
        if (m_jWorkSockclass)
            LOGD("%s", "JNI CJavaRecvWork--SetJCJB(m_jobject)");
    }

}

 void CJavaRecvWork::onSlowPCMData(char *pBuffer, int nLen) {
    int status = 0;
    JNIEnv *env = 0;

    status = (g_pVM)->AttachCurrentThread(&env, 0);
    if (env == 0) return;

    jmethodID mid = env->GetStaticMethodID(m_jWorkSockclass, "onSlowPCMData", "([B)V");
    if (mid == 0) return;

    jbyteArray jb = env->NewByteArray(nLen);
    char *p = (char *) env->GetByteArrayElements(jb, 0);
    memcpy(p, pBuffer, nLen);
    env->ReleaseByteArrayElements(jb, (jbyte *) p, 0);
    env->CallStaticVoidMethod(m_jWorkSockclass, mid, jb);

    env->DeleteLocalRef(jb);

    if (status > 0) g_pVM->DetachCurrentThread();
}

