/*
 * Copyright (c) 2022, Gluon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL GLUON BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
#include "util.h"

static jclass jGraalPrinterClass;
static jmethodID jGraalAddBTDeviceMethod;

static jclass jPrinterServiceClass;
static jobject jDalvikPrinterService;
static jmethodID jPrinterServicePrintMethod;

static void initializeGraalHandles(JNIEnv *graalEnv) {
    jGraalPrinterClass = (*graalEnv)->NewGlobalRef(graalEnv, (*graalEnv)->FindClass(graalEnv, "com/gluonhq/attachextended/printer/impl/AndroidPrinterService"));
    jGraalAddBTDeviceMethod = (*graalEnv)->GetStaticMethodID(graalEnv, jGraalPrinterClass, "addBTDevice", "(Ljava/lang/String;Ljava/lang/String;)V");
}

static void initializeDalvikHandles() {
    jPrinterServiceClass = GET_REGISTER_DALVIK_CLASS(jPrinterServiceClass, "com/gluonhq/helloandroid/DalvikPrinterService");
    ATTACH_DALVIK();
    jmethodID jPrinterServiceInitMethod = (*dalvikEnv)->GetMethodID(dalvikEnv, jPrinterServiceClass, "<init>", "(Landroid/app/Activity;)V");
    jPrinterServicePrintMethod = (*dalvikEnv)->GetMethodID(dalvikEnv, jPrinterServiceClass, "print", "(Ljava/lang/String;Ljava/lang/String;J)V");

    jobject jActivity = substrateGetActivity();
    jobject jtmpobj = (*dalvikEnv)->NewObject(dalvikEnv, jPrinterServiceClass, jPrinterServiceInitMethod, jActivity);
    jDalvikPrinterService = (*dalvikEnv)->NewGlobalRef(dalvikEnv, jtmpobj);
    DETACH_DALVIK();
}

//////////////////////////
// From Graal to native //
//////////////////////////


JNIEXPORT jint JNICALL
JNI_OnLoad_printer(JavaVM *vm, void *reserved)
{
    ATTACH_LOG_INFO("JNI_OnLoad_printer called");
#ifdef JNI_VERSION_1_8
    JNIEnv* graalEnv;
    if ((*vm)->GetEnv(vm, (void **)&graalEnv, JNI_VERSION_1_8) != JNI_OK) {
        ATTACH_LOG_WARNING("Error initializing native Printer from OnLoad");
        return JNI_FALSE;
    }
    ATTACH_LOG_FINE("[Printer Service] Initializing native Printer from OnLoad");
    initializeGraalHandles(graalEnv);
    initializeDalvikHandles();
    return JNI_VERSION_1_8;
#else
    #error Error: Java 8+ SDK is required to compile Attach
#endif
}

// from Java to Android

JNIEXPORT void JNICALL Java_com_gluonhq_attachextended_printer_impl_AndroidPrinterService_printMessage
(JNIEnv *env, jclass jClass, jstring jmessage, jstring jaddress, jlong jtimeout)
{
    const char *messageChars = (*env)->GetStringUTFChars(env, jmessage, NULL);
    const char *addressChars = (*env)->GetStringUTFChars(env, jaddress, NULL);
    ATTACH_DALVIK();
    jstring dmessage = (*dalvikEnv)->NewStringUTF(dalvikEnv, messageChars);
    jstring daddress = (*dalvikEnv)->NewStringUTF(dalvikEnv, addressChars);
    (*dalvikEnv)->CallVoidMethod(dalvikEnv, jDalvikPrinterService, jPrinterServicePrintMethod, dmessage, daddress, jtimeout);
    DETACH_DALVIK();
    (*env)->ReleaseStringUTFChars(env, jmessage, messageChars);
    (*env)->ReleaseStringUTFChars(env, jaddress, addressChars);
}

// From Dalvik to native

JNIEXPORT void JNICALL Java_com_gluonhq_helloandroid_DalvikPrinterService_detectedBTDevice
(JNIEnv *env, jobject service, jstring name, jstring address)
{
    const char *nameChars = (*env)->GetStringUTFChars(env, name, NULL);
    const char *addressChars = (*env)->GetStringUTFChars(env, address, NULL);
    if (isDebugAttach()) {
        ATTACH_LOG_FINE("Device name = %s, address = %s\n", nameChars, addressChars);
    }
    ATTACH_GRAAL();
    jstring jname = (*graalEnv)->NewStringUTF(graalEnv, nameChars);
    jstring jaddress = (*graalEnv)->NewStringUTF(graalEnv, addressChars);
    (*graalEnv)->CallStaticVoidMethod(graalEnv, jGraalPrinterClass, jGraalAddBTDeviceMethod, jname, jaddress);
    DETACH_GRAAL();
    (*env)->ReleaseStringUTFChars(env, name, nameChars);
    (*env)->ReleaseStringUTFChars(env, address, addressChars);
}
