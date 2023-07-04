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
#include <iostream>
#include <string>
#include <sys/socket.h>
#include <sys/types.h>
#include <netinet/in.h>
#include <cstring>
#include <arpa/inet.h>
#include <unistd.h>
#include <cstdlib>

enum RetType
{
    CONNECTION_FAILED = -4,
    SOCKET_FAILED = -3,
    INVALID_ADDRESS = -2
};

static const std::string gPUBLIC_IP = "73.14.16.192";
static const std::string gSERVER_HOME_IP = "10.0.0.50";
static const uint8_t gRETRIES = 3;

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_helloworld_MainActivity_getString(JNIEnv * env, jobject obj, jstring str)
{
    std::string a = "Test";
    bool val = true;
    a += env->GetStringUTFChars(str, nullptr);
    jstring result = env->NewStringUTF(a.c_str());
    return result;
}

int32_t attemptConnection(const std::string &crIp)
{
    sockaddr_in server;

    server.sin_family = AF_INET;
    server.sin_port = htons(2721);

    uint8_t num_tries = 0;
    int8_t status = -1;
    int clientFd = -1;
    timeval tv;
    tv.tv_sec = 5;
    tv.tv_usec = 0;

    while (gRETRIES > num_tries)
    {
        fd_set socketWatch;
        clientFd = socket(AF_INET, SOCK_STREAM, 0);

        if (clientFd < 0)
        {
            return RetType::SOCKET_FAILED;
        }

        fcntl(clientFd, F_SETFL, O_NONBLOCK);

        status = inet_pton(AF_INET, crIp.c_str(), &server.sin_addr);
        status = connect(clientFd, (sockaddr *) &server, sizeof(server));

        if (0 > status)
        {
            FD_ZERO(&socketWatch);
            FD_SET(clientFd, &socketWatch);
            status = select(clientFd + 1, NULL, &socketWatch, NULL, &tv);

            if (0 < status)
            {
                break;
            }
        }
        else
        {
            break;
        }

        num_tries ++;
        close(clientFd);
        clientFd = -1;
    }

    if (-1 == status)
    {
        return status;
    }

    return clientFd;
}


extern "C" JNIEXPORT jint JNICALL
Java_com_example_helloworld_MainActivity_connectToServer(JNIEnv * env, jobject obj)
{
    int32_t fd = attemptConnection(gPUBLIC_IP);

    if (0 > fd)
    {
       fd = attemptConnection(gSERVER_HOME_IP);
    }

    return fd;
}

extern "C"
void Java_com_example_helloworld_MainActivity_close(JNIEnv * env, jobject obj, jint fd) {
    if (-1 != fd)
    {
        close(fd);
    }
}

extern "C" JNIEXPORT jint JNICALL
Java_com_example_helloworld_MainActivity_send(JNIEnv * env, jobject obj, jint fd, jstring msg)
{
    return send(fd, env->GetStringUTFChars(msg, NULL), env->GetStringLength(msg), 0);
}