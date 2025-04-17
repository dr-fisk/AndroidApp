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
#include <chrono>

SSL_CTX *gpCtx;
SSL *gpSsl;
jclass gMainActivity;
jmethodID gGetClientFd;
jmethodID gSetClientFd;
std::mutex gSendMutex;
std::mutex gRecvMutex;
std::future<jint> gPollServerFuture;
JavaVM *gpVm;

// TODO: Turn selects into poll

enum RetType
{
    CONNECTION_FAILED = -4,
    SOCKET_FAILED = -3,
    INVALID_ADDRESS = -2
};

static const std::string gPUBLIC_IP = "71.56.211.239";
static const std::string gSERVER_HOME_IP = "10.0.0.129";
static const uint8_t gRETRIES = 3;

bool GetJniEnv(JavaVM *vm, JNIEnv **env)
{
    bool did_attach_thread = false;
    *env = nullptr;
    // Check if the current thread is attached to the VM
    auto get_env_result = vm->GetEnv((void**)env, JNI_VERSION_1_6);
//    if (get_env_result == JNI_EDETACHED) {
        if (vm->AttachCurrentThread(env, NULL) == JNI_OK) {
            did_attach_thread = true;
        } else {
            // Failed to attach thread. Throw an exception if you want to.
        }
//     else if (get_env_result == JNI_EVERSION) {
//        // Unsupported JNI version. Throw an exception if you want to.
//    }
    return did_attach_thread;
}

int32_t openSslConnect(const int32_t cClientFd, const int32_t cTimeout)
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
            __android_log_print(ANDROID_LOG_ERROR, "openSslConnect", "SSL Connection");
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
                status = poll(fdReady, 1, cTimeout);
                break;
            case SSL_ERROR_WANT_WRITE:
                fdReady[0].events = POLLOUT;
                status = poll(fdReady, 1, cTimeout);
                break;
            default:
                return -1;
        }

        if (0 >= status)
        {
//            __android_log_print(ANDROID_LOG_ERROR, "openSslConnect", "Failed SSL");
            return -1;
        }
    }
}

int32_t checkConnectionStatus(sockaddr_in &rServerAddr, const int32_t cClientFd, const int32_t cStatus,
                              const int32_t cTimeout)
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
//        __android_log_print(ANDROID_LOG_ERROR, "openSslConnect", "Connection not in progress");
        return -1;
    }

    // Connection is in progress so did not connect immediately needs more info
    // If connection status is not -1 that means connection was immediate
    if (-1 == status)
    {
        status = poll(fdReady, 1, cTimeout);

        // Get the error code returned from select using getsockopt
        if (0 == getsockopt(cClientFd, SOL_SOCKET, SO_ERROR, &status, &statusSize))
        {
//            __android_log_print(ANDROID_LOG_ERROR, "openSslConnect", "getsockopt");
            // getsockopt error code is good one last check needed
            if (0 == status)
            {
//                __android_log_print(ANDROID_LOG_ERROR, "openSslConnect", "Checking peer");
                // Check to see if actual connection established
                status = getpeername(cClientFd, (sockaddr *) &rServerAddr, &serverSize);
            }
        }
    }

    return status;
}

JNIEXPORT jint JNI_OnLoad(JavaVM* vm, void* reserved) {
    JNIEnv* env;
    gpVm = vm;
    if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
        return JNI_ERR;
    }

    gMainActivity = env->FindClass("com/example/syncup/MainActivity");
    gGetClientFd = env->GetMethodID(gMainActivity, "getClientFd", "()I");
    gSetClientFd = env->GetMethodID(gMainActivity, "setClientFd", "(I)V");
    gpCtx = SSL_CTX_new(TLS_method());
//    __android_log_print(ANDROID_LOG_ERROR, "TRACKERS", "%d Fd", env->CallIntMethod(obj, gGetClientFd));

    return JNI_VERSION_1_6;
}

int32_t attemptConnection(const std::string &crIp, const int32_t cTimeout)
{
    sockaddr_in server;
    const int32_t SSL_TIMEOUT = 5000;

    server.sin_family = AF_INET;
    server.sin_port = htons(2721);

    uint8_t num_tries = 0;
    int32_t status = -1;
    int clientFd = -1;

    while (gRETRIES > num_tries)
    {
        clientFd = socket(AF_INET, SOCK_STREAM, 0);

        if (clientFd < 0)
        {
            __android_log_print(ANDROID_LOG_ERROR, "openSslConnect", "Socket failed to create");
            return RetType::SOCKET_FAILED;
        }

        gpSsl = SSL_new(gpCtx);
        fcntl(clientFd, F_SETFL, O_NONBLOCK);
        SSL_set_fd(gpSsl, clientFd);

        status = inet_pton(AF_INET, crIp.c_str(), &server.sin_addr);
        status = connect(clientFd, (sockaddr *) &server, sizeof(server));
        status = checkConnectionStatus(server, clientFd, status, cTimeout);

        __android_log_print(ANDROID_LOG_ERROR, "TRACKERS", "%s", crIp.c_str());
        // Connection successful
        if (0 == status && 0 == openSslConnect(clientFd, SSL_TIMEOUT))
        {
            break;
        }

        num_tries ++;
        close(clientFd);
        clientFd = -1;

        if (nullptr != gpSsl)
        {
            SSL_shutdown(gpSsl);
            SSL_free(gpSsl);
            gpSsl = nullptr;
        }
    }

    if (-1 == status)
    {
        return status;
    }

    return clientFd;
}

int32_t connectToServer(const int32_t cTimeout)
{
    __android_log_print(ANDROID_LOG_ERROR, "openSslConnect", "Trying to connect");
    int32_t fd = attemptConnection(gPUBLIC_IP, cTimeout);

    if (0 > fd)
    {
        fd = attemptConnection(gSERVER_HOME_IP, cTimeout);
    }

    return fd;
}

int32_t recvUtility(const int32_t cClientFd, void* pBuff, const uint32_t cBytesToRecv, int32_t cTimeoutMs)
{
    uint32_t bytesRecvd = 0;
    int32_t bytesReturnedSSL = 0;

    do
    {
        bytesReturnedSSL = SSL_read(gpSsl, ((char *) pBuff) + bytesRecvd,
                                    cBytesToRecv - bytesRecvd);
        if (bytesReturnedSSL > 0) {
            bytesRecvd += bytesReturnedSSL;
        }
        else {
            if(SSL_ERROR_WANT_READ ==  SSL_get_error(gpSsl, bytesReturnedSSL))
            {
                continue;
            }
            else {
                __android_log_print(ANDROID_LOG_ERROR, "Recv",
                                    "OpenSsl read returned an error of: %d",
                                    SSL_get_error(gpSsl, bytesReturnedSSL));
                __android_log_print(ANDROID_LOG_ERROR, "Recv",
                                    "Errno: %d",
                                    errno);
            }
            return -1;
        }
    }
    while(bytesRecvd < cBytesToRecv);

    return bytesRecvd;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_example_syncup_MainActivity_recv(JNIEnv * env, jobject obj,const jint cClientFd, jbyteArray pBuff, const jint cBytesToRecv, const jint cTimeoutMs)
{
    uint16_t payload_size = 0;
    int32_t bytesRecvd = 0;
    pollfd fds[1];
    fds[0].fd = cClientFd;
    fds[0].events = POLLIN;
    fds[0].revents = 0;
    int32_t pollret = 0;
    uint32_t timeoutTime = 0;
    const uint8_t POLL_TIME = 100;

    while(timeoutTime <= cTimeoutMs) {
        pollret = poll(fds, 1, POLL_TIME);

        if(pollret > 0) {

            if (-1 == recvUtility(cClientFd, &payload_size, sizeof(payload_size), cTimeoutMs)) {
                __android_log_print(ANDROID_LOG_ERROR, "Recv", "TCP Header recv timed out.");
                return -1;
            }

            payload_size = ntohs(payload_size);

            if (payload_size > cBytesToRecv) {
                // temp will need to recv all data left in buffer
                return -1;
            }

            jboolean notCopy = JNI_FALSE;
            char* buff = (char*)env->GetByteArrayElements(pBuff, &notCopy);
            bytesRecvd = recvUtility(cClientFd, buff, payload_size, cTimeoutMs);

            if (-1 == bytesRecvd)
            {
                __android_log_print(ANDROID_LOG_ERROR, "Recv", "Payload recv timed out.");
                return -1;
            }

            buff[bytesRecvd] = '\0';

            env->ReleaseByteArrayElements(pBuff, (jbyte*)buff, 0);
            return bytesRecvd;
        }
        else if (0 > pollret)
        {
            return -1;
        }
        else
        {
            timeoutTime += POLL_TIME;
        }
    }

    return -1;
}


int32_t sendMsg(char* pBuff, const uint32_t cBuffSize, const int32_t cFd)
{
    std::unique_lock<std::mutex> lock(gSendMutex);
    const int16_t MAX_BUFF_SIZE = 2500;
    const int16_t msgSize = htons(cBuffSize);
    int16_t origSize = cBuffSize;

    if (MAX_BUFF_SIZE < origSize)
    {
        __android_log_print(ANDROID_LOG_ERROR, "Send Msg", "Unsupported buff size of %d.", origSize);

        return -1;
    }

    const uint16_t SIZE = sizeof(msgSize) + cBuffSize;

    char buff[MAX_BUFF_SIZE + sizeof(msgSize)];

    uint32_t bytesWritten = 0;
    int32_t bytesReturnedSSL = 0;
    uint32_t timeoutTime = 0;
    pollfd fds[1];
    int32_t pollret = 1;
    fds[0].fd = cFd;
    fds[0].events = POLLOUT;
    fds[0].revents = 0;
    const uint8_t POLL_TIME = 100;
    const uint16_t cTimeOutMs = 5000; // Temp TODO: make parameter

    buff[0] = (0xff & msgSize);
    buff[1] = (0xff & (msgSize >> 8));
    memcpy(&buff[0] + sizeof(msgSize), pBuff, origSize);

    do
    {
        pollret = poll(fds, 1, POLL_TIME);

        if (0 < pollret)
        {
            bytesReturnedSSL = SSL_write(gpSsl, (char *)buff + bytesWritten, SIZE - bytesWritten);

            if (bytesReturnedSSL > 0)
            {
                bytesWritten += bytesReturnedSSL;
                timeoutTime = 0;
            }
            else
            {
                if (SSL_ERROR_WANT_WRITE == SSL_get_error(gpSsl, bytesReturnedSSL))
                {
                    continue;
                }
                __android_log_print(ANDROID_LOG_ERROR, "Send Msg", "OpenSsl Write returned an error of: %d.", bytesReturnedSSL);
                return -1;
            }
        }
        else if (0 == pollret)
        {
            timeoutTime += POLL_TIME;

            if (timeoutTime >= cTimeOutMs)
            {
                __android_log_print(ANDROID_LOG_ERROR, "Send Msg", "Timeout occurred trying to send.");
                return -1;
            }
        }
        else
        {
            __android_log_print(ANDROID_LOG_ERROR, "Send Msg", "Poll error occurred trying to send %d.", pollret);
            return -1;
        }
    }
    while(bytesWritten < SIZE);

//    __android_log_print(ANDROID_LOG_ERROR, "Send", "%d sent.", bytesWritten);

    lock.unlock();

    return bytesWritten;
}

int32_t pollServer(jobject obj)
{
    JNIEnv *env = nullptr;
    const int32_t SLEEP_TIME = 5;
    nlohmann::json pollMsg;
    nlohmann::json pollMsg2;
    pollMsg["Event"] = 10;
    pollfd fds[1];

    GetJniEnv(gpVm, &env);
    std::string msg;
    std::unique_lock<std::mutex> sendLock(gSendMutex);
    sendLock.unlock();

    int32_t pollret = 0;
    fds[0].fd = env->CallIntMethod(obj, gGetClientFd);
    fds[0].events = POLLOUT;
    fds[0].revents = 0;
    //Have shutdown var
    while(true)
    {
        std::this_thread::sleep_for(std::chrono::seconds(SLEEP_TIME));

        if (nullptr != gpSsl)
        {
            msg = to_string(pollMsg);
            if (0 > sendMsg(&msg[0], msg.size(), env->CallIntMethod(obj, gGetClientFd)))
            {
                sendLock.lock();
                if (0 < env->CallIntMethod(obj, gGetClientFd))
                {
                    close(env->CallIntMethod(obj, gGetClientFd));
                    SSL_shutdown(gpSsl);
                    SSL_free(gpSsl);
                    env->CallVoidMethod(obj, gSetClientFd, -2);
                }
                else
                {
                    int32_t fd = connectToServer(100);
                    if (0 <= fd)
                    {
                        env->CallVoidMethod(obj, gSetClientFd, fd);

                        fds[0].fd = fd;
                    }
                }

                sendLock.unlock();
            }
        }
        else
        {
            sendLock.lock();
            int32_t fd = connectToServer(100);
            if (0 <= fd)
            {
                env->CallVoidMethod(obj, gSetClientFd, fd);
            }
            sendLock.unlock();
        }
    }
    return 0;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_example_syncup_MainActivity_connectToServer(JNIEnv * env, jobject obj)
{
    const int INIT_TIMEOUT = 1000;
    int32_t fd = connectToServer(INIT_TIMEOUT);
    gPollServerFuture = std::async(std::launch::async, &pollServer, env->NewGlobalRef(obj));
    return fd;
}

extern "C"
void Java_com_example_syncup_MainActivity_close(JNIEnv * env, jobject obj, jint fd) {
    if (-1 != fd) {
        close(fd);
    }

    if (nullptr != gpSsl) {
        SSL_shutdown(gpSsl);
        SSL_free(gpSsl);
    }

    if (nullptr != gpCtx)
    {
        SSL_CTX_free(gpCtx);
    }
}

extern "C" JNIEXPORT jint JNICALL
Java_com_example_syncup_MainActivity_sendMsg(JNIEnv * env, jobject obj, jint fd, jbyteArray msg, jint buffSize)
{
    // Add Json
    if (nullptr != gpSsl)
    {
//        __android_log_print(ANDROID_LOG_ERROR, "Send", "%d client fd.", fd);
        jboolean notCopy = JNI_FALSE;
        char* buff = (char*)env->GetByteArrayElements(msg, &notCopy);

//        __android_log_print(ANDROID_LOG_ERROR, "Send", "%s.", buff);

        const int32_t ret = sendMsg(buff, buffSize, fd);
//        __android_log_print(ANDROID_LOG_ERROR, "Send", "Sent %d bytes.", ret);

        env->ReleaseByteArrayElements(msg, (jbyte*)buff, 0);
        return ret;
    }


    __android_log_print(ANDROID_LOG_ERROR, "Command", "%s", msg);

    return -1;
}