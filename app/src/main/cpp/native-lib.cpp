#include <jni.h>
#include <string>

extern "C" JNIEXPORT jstring

JNICALL
Java_uzh_scenere_activities_StartupActivity_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {
    std::string hello = "Hello from C++ 1";
    return env->NewStringUTF(hello.c_str());
}
