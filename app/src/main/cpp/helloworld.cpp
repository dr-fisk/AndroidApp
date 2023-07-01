// Write C++ code here.
//
// Do not forget to dynamically load the C++ library into your application.
//
// For instance,
//
// In MainActivity.java:
//    static {
//       System.loadLibrary("helloworld");
//    }
//
// Or, in MainActivity.kt:
//    companion object {
//      init {
//         System.loadLibrary("helloworld")
//      }
//    }
#include <jni.h>
#include <string>

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_helloworld_MainActivity_getString(JNIEnv * env, jobject obj, jstring str)
{
    std::string a = "Test";
    bool val = true;
    a += env->GetStringUTFChars(str, nullptr);
    jstring result = env->NewStringUTF(a.c_str());
    return result;
}