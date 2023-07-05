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
#include<future>
#include "./libs/openssl-android-arm64-v8a/include/openssl/ssl.h"


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
    const int16_t MAX_BUFF_SIZE = 1500;
    int16_t msgSize = htons(env->GetStringLength(msg));
    int16_t origSize = env->GetStringLength(msg);

    if (MAX_BUFF_SIZE < origSize)
    {
        return -1;
    }

    char buff[MAX_BUFF_SIZE + sizeof(msgSize)];

    buff[0] = (0xff & msgSize);
    buff[1] = (0xff & (msgSize >> 8));
    memcpy(&buff[0] + sizeof(msgSize), env->GetStringUTFChars(msg, NULL), origSize);

    return send(fd, buff, origSize + sizeof(msgSize), 0);
}