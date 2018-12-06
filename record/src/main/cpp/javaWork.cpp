//
// Created by TG on 2018/12/3.
//
#include <jni.h>
#include "javaRecvWork.h"
#include <string>
#include <malloc.h>

JavaVM *g_pVM = 0;

extern "C"
{
jint JNI_OnLoad(JavaVM *vm, void *reserved);
}

jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    LOGE("%s", "JNI_OnLoad");
//    //设置客户端回调
//    m_WorkSock.setClientCallBack(&g_JavaRecvWork);

    g_pVM = vm;
    JNIEnv *env = NULL;
    if ((vm)->GetEnv((void **) &env, JNI_VERSION_1_4) != JNI_OK) LOGD("JNI_OnLoad 2222");
    if (env == NULL) {
        LOGE("%s", "JNI_OnLoad env==NULL");
        return JNI_ERR;
    }

//    //注册本地方法.Load 目标类
//    jclass jc=env->FindClass(JNIREG_CLASS);
//    if (jc == NULL)
//    {
//        LOGD("Native registration unable to find class '%s'", JNIREG_CLASS);
//        return JNI_ERR;
//    }
//    //注册本地native方法
//    if(env->RegisterNatives(jc, gMethods, NELEM(gMethods)) < 0)
//    {
//        LOGD("ERROR: MediaPlayer native registration failed\n");
//        return JNI_ERR;
//    }

    LOGI("%s", "JNI_OnLoad Success!");

    return JNI_VERSION_1_4;
}