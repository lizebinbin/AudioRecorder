//
// Created by TG on 2018/11/28.
//

//
// Created by Xionghu on 2017/11/24.
//

#include "inc/fmod.hpp"
#include "inc/fmod.h"
#include "EffectUtils.h"

#include <stdlib.h>
#include <unistd.h>
#include <android/log.h>

#define LOGI(FORMAT, ...) __android_log_print(ANDROID_LOG_INFO,"kpioneer",FORMAT,##__VA_ARGS__);
#define LOGE(FORMAT, ...) __android_log_print(ANDROID_LOG_ERROR,"kpioneer",FORMAT,##__VA_ARGS__);

#define MODE_NORMAL  0
#define MODE_LUOLI 1
#define MODE_DASHU 2
#define MODE_JINGSONG 3
#define MODE_GAOGUAI 4
#define MODE_KONGLING 5
#define MODE_HECHANG 6

using namespace FMOD;

System *mSystem;
Sound *sound;
Channel *channel;
DSP *dsp;
float frequency = 0;

void stopPlaying();

JNIEXPORT void JNICALL Java_com_lzb_record_effect_EffectUtils_fix
        (JNIEnv *env, jclass jcls, jstring path_jstr, jint type, jint save) {
    bool playing = true;

    const char *path_cstr = env->GetStringUTFChars(path_jstr, NULL);
    try {
        //初始化
        System_Create(&mSystem);
//        mSystem->setSoftwareFormat(8000,FMOD_SPEAKERMODE_MONO,1);
        if (save == 1) {
            char cDest[200] = "sdcard/test.wav";
            mSystem->setOutput(FMOD_OUTPUTTYPE_WAVWRITER);
            mSystem->init(16, FMOD_INIT_NORMAL | FMOD_INIT_PROFILE_ENABLE, cDest);
        } else {
            //手机录音一般是16位  如果是32位的音频要填32 否则无法播放声音
            mSystem->init(16, FMOD_INIT_NORMAL, NULL);
        }

        //创建声音
        mSystem->createSound(path_cstr, FMOD_DEFAULT, NULL, &sound);
        switch (type) {
            case MODE_NORMAL:
                //原生播放
                LOGI("%s", path_cstr);
                mSystem->playSound(sound, 0, false, &channel);
                LOGI("%s", "fix normal");
                break;
            case MODE_LUOLI:
                //萝莉
                //DSP digital signal process
                //dsp -> 音效
                //FMOD_DSP_TYPE_PITCH  dsp ，提升或者降低音调用的一种音效
                // FMOD_DSP_TYPE_PITCHSHIFT 在fmod_dsp_effects.h中
                mSystem->createDSPByType(FMOD_DSP_TYPE_PITCHSHIFT, &dsp);
                //设置音调的参数
                dsp->setParameterFloat(FMOD_DSP_PITCHSHIFT_PITCH, 2.5);
                mSystem->playSound(sound, 0, false, &channel);
                //添加到channel
                channel->addDSP(0, dsp);
                LOGI("%s", "fix luoli");
                break;

            case MODE_DASHU:
                //大叔
                mSystem->createDSPByType(FMOD_DSP_TYPE_PITCHSHIFT, &dsp);
                dsp->setParameterFloat(FMOD_DSP_PITCHSHIFT_PITCH, 0.8);

                mSystem->playSound(sound, 0, false, &channel);
                //添加到channel
                channel->addDSP(0, dsp);
                LOGI("%s", "fix dashu");
                break;
            case MODE_JINGSONG:
                //惊悚
                mSystem->createDSPByType(FMOD_DSP_TYPE_TREMOLO, &dsp);
                dsp->setParameterFloat(FMOD_DSP_TREMOLO_SKEW, 0.5);
                mSystem->playSound(sound, 0, false, &channel);
                channel->addDSP(0, dsp);
                break;
            case MODE_GAOGUAI:
                //搞怪
                //提高说话的速度
                mSystem->playSound(sound, 0, false, &channel);
                channel->getFrequency(&frequency);
                frequency = frequency * 1.6f;
                channel->setFrequency(frequency);
                LOGI("%s", "fix gaoguai");
                break;
            case MODE_KONGLING:
                //空灵
                mSystem->createDSPByType(FMOD_DSP_TYPE_ECHO, &dsp);
                dsp->setParameterFloat(FMOD_DSP_ECHO_DELAY, 300);
                dsp->setParameterFloat(FMOD_DSP_ECHO_FEEDBACK, 20);
                mSystem->playSound(sound, 0, false, &channel);
                channel->addDSP(0, dsp);
                LOGI("%s", "fix kongling");

                break;
            case MODE_HECHANG:
                mSystem->createDSPByType(FMOD_DSP_TYPE_CHORUS, &dsp);
                dsp->setParameterFloat(FMOD_DSP_CHORUS_MIX, 50);
                dsp->setParameterFloat(FMOD_DSP_CHORUS_RATE, 1.1);
                mSystem->playSound(sound, 0, false, &channel);
                channel->addDSP(0, dsp);
                break;

            default:
                break;
        }
    } catch (...) {
        LOGE("%s", "发生异常");
        goto END;
    }
    mSystem->update();
    //进程休眠 单位微秒 us
    //每秒钟判断是否在播放
    while (playing) {
        channel->isPlaying(&playing);
        usleep(1000 * 1000);
    }
    goto END;
    //释放资源
    END:
    env->ReleaseStringUTFChars(path_jstr, path_cstr);
    sound->release();
    mSystem->close();
    mSystem->release();


}

JNIEXPORT void JNICALL Java_com_lzb_record_effect_EffectUtils_stop(JNIEnv *env, jclass jcls) {
    stopPlaying();
}

//将PCM16LE双声道音频采样数据中左声道的音量降一半
JNIEXPORT void JNICALL
Java_com_lzb_record_effect_EffectUtils_downVolume(JNIEnv *env, jclass jcls, jstring path_str, jstring dst_path) {
    const char *fileUrl = env->GetStringUTFChars(path_str, NULL);
    const char *dstFileUrl = env->GetStringUTFChars(dst_path, NULL);
    FILE *fp = fopen(fileUrl, "rb+");
    FILE *fp1 = fopen(dstFileUrl, "wb+");

    int cnt = 0;

    unsigned char *sample = (unsigned char *) malloc(4);

    while (!feof(fp)) {
        short *samplenum = NULL;
        fread(sample, 1, 4, fp);

        samplenum = (short *) sample;
        *samplenum = *samplenum / 2;
        //L
        fwrite(sample, 1, 2, fp1);
        //R
        fwrite(sample + 2, 1, 2, fp1);

        cnt++;
    }
    LOGI("Sample Cnt:%d\n", cnt);

    free(sample);
    fclose(fp);
    fclose(fp1);
}

//将PCM16LE双声道音频采样数据的声音速度提高一倍
JNIEXPORT void JNICALL
Java_com_lzb_record_effect_EffectUtils_fasterPCM(JNIEnv *env, jclass jcls, jstring path_str, jstring dst_path) {
    const char *fileUrl = env->GetStringUTFChars(path_str, NULL);
    const char *dstFileUrl = env->GetStringUTFChars(dst_path, NULL);
    FILE *fp = fopen(fileUrl, "rb+");
    FILE *fp1 = fopen(dstFileUrl, "wb+");

    int cnt = 0;

    unsigned char *sample = (unsigned char *) malloc(4);

    while (!feof(fp)) {
        fread(sample, 1, 4, fp);
        if (cnt % 2 != 0) {
            //L
            fwrite(sample, 1, 2, fp1);
            //R
            fwrite(sample + 2, 1, 2, fp1);
        }
        cnt++;
    }
    LOGI("Sample Cnt:%d\n", cnt);
    free(sample);
    fclose(fp);
    fclose(fp1);
}

//将PCM16LE双声道音频采样数据的声音速度放慢一倍
JNIEXPORT void JNICALL
Java_com_lzb_record_effect_EffectUtils_slowerPCM(JNIEnv *env, jclass jcls, jstring path_str, jstring dst_path) {
    const char *fileUrl = env->GetStringUTFChars(path_str, NULL);
    const char *dstFileUrl = env->GetStringUTFChars(dst_path, NULL);
    FILE *fp = fopen(fileUrl, "rb+");
    FILE *fp1 = fopen(dstFileUrl, "wb+");

    int cnt = 0;

    unsigned char *sample = (unsigned char *) malloc(4);
    unsigned char *sample1 = (unsigned char *) malloc(2);

    while (!feof(fp)) {
        fread(sample, 1, 4, fp);
        //L
        fwrite(sample, 1, 2, fp1);
        //R
        fwrite(sample + 2, 1, 2, fp1);
        //写入空字节
        fwrite(sample1, 1, 1, fp1);
        fwrite(sample1 + 1, 1, 1, fp1);

    }
    LOGI("Sample Cnt:%d\n", cnt);
    free(sample);
    fclose(fp);
    fclose(fp1);
}

void stopPlaying() {
    channel->stop();
}