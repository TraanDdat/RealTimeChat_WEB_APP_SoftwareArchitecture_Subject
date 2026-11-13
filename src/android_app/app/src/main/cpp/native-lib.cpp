#include <jni.h>
#include <string>

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_zaloclone_NativeProcessor_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {
    std::string hello = "Hello from C++ Native Code!";
    return env->NewStringUTF(hello.c_str());
}

extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_example_zaloclone_NativeProcessor_processImageSecurely(
        JNIEnv* env,
        jobject,
        jbyteArray imageData) {
    // Native processing logic (Crypto / Image processing / Compression)
    return imageData;
}
