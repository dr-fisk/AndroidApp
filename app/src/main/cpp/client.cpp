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
#include <openssl/ssl.h>
#include "libs/json/json.hpp"
#include <android/log.h>
#include <poll.h>

SSL_CTX *gpCtx;
SSL *gpSsl;

// TODO: Turn selects into poll

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

int32_t openSslConnect(const int32_t cClientFd)
{
    int32_t status = 0;
    pollfd fdReady[1];
    fdReady[0].fd = cClientFd;
    fdReady[0].events = POLLIN;
    fdReady[0].revents = 0;

    while (true)
    {
        status = SSL_connect(gpSsl);
        if (0 < status)
        {
            __android_log_print(ANDROID_LOG_ERROR, "TRACKERS", "SSL Connection");
            return 0;
        }

        switch(SSL_get_error(gpSsl, status))
        {
            case SSL_ERROR_NONE:
                status = 1;
                break;
            case SSL_ERROR_WANT_CONNECT:
                status = 1;
                break;
            case SSL_ERROR_WANT_ACCEPT:
                status = 1;
                break;
            case SSL_ERROR_WANT_X509_LOOKUP:
                status = 1;
                break;
            case SSL_ERROR_WANT_READ:
                fdReady[0].events = POLLIN;
                status = poll(fdReady, 1, 1000);
                break;
            case SSL_ERROR_WANT_WRITE:
                fdReady[0].events = POLLOUT;
                status = poll(fdReady, 1, 1000);
                break;
            default:
                return -1;
        }

        if (0 >= status)
        {
            return -1;
        }
    }
}

int32_t checkConnectionStatus(sockaddr_in &rServerAddr, const int32_t cClientFd, const int32_t cStatus)
{
    int32_t status = cStatus;
    socklen_t statusSize = sizeof(status);
    socklen_t serverSize = sizeof(rServerAddr);
    pollfd fdReady[1];
    fdReady[0].fd = cClientFd;
    fdReady[0].events = POLLIN;
    fdReady[0].revents = 0;

    // Connection is not in progress so no connection can happen return failure
    if (-1 == status && errno != EINPROGRESS)
    {
        return -1;
    }

    // Connection is in progress so did not connect immediately needs more info
    // If connection status is not -1 that means connection was immediate
    if (-1 == status)
    {
        status = poll(fdReady, 1, 100);

        // Get the error code returned from select using getsockopt
        if (0 == getsockopt(cClientFd, SOL_SOCKET, SO_ERROR, &status, &statusSize))
        {
            // getsockopt error code is good one last check needed
            if (0 == status)
            {
                // Check to see if actual connection established
                status = getpeername(cClientFd, (sockaddr *) &rServerAddr, &serverSize);
            }
        }
    }

    return status;
}

int32_t attemptConnection(const std::string &crIp)
{
    sockaddr_in server;

    server.sin_family = AF_INET;
    server.sin_port = htons(2721);

    uint8_t num_tries = 0;
    int32_t status = -1;
    int clientFd = -1;
    gpSsl = SSL_new(gpCtx);

    while (gRETRIES > num_tries)
    {
        clientFd = socket(AF_INET, SOCK_STREAM, 0);

        if (clientFd < 0)
        {
            return RetType::SOCKET_FAILED;
        }

        fcntl(clientFd, F_SETFL, O_NONBLOCK);
        SSL_set_fd(gpSsl, clientFd);

        status = inet_pton(AF_INET, crIp.c_str(), &server.sin_addr);
        status = connect(clientFd, (sockaddr *) &server, sizeof(server));
        status = checkConnectionStatus(server, clientFd, status);

        __android_log_print(ANDROID_LOG_ERROR, "TRACKERS", "%s", crIp.c_str());
        // Connection successful
        if (0 == status && 0 == openSslConnect(clientFd))
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
    gpCtx = SSL_CTX_new(TLS_client_method());
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

    SSL_shutdown(gpSsl);
    SSL_free(gpSsl);
    SSL_CTX_free(gpCtx);
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

    return SSL_write(gpSsl, buff, origSize + sizeof(msgSize));
}