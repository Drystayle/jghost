/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class ch_drystayle_jghost_util_JniUtil */

#ifndef _Included_ch_drystayle_jghost_util_JniUtil
#define _Included_ch_drystayle_jghost_util_JniUtil
#ifdef __cplusplus
extern "C" {
#endif
#undef ch_drystayle_jghost_util_JniUtil_BNCSUTIL_PLATFORM_X86
#define ch_drystayle_jghost_util_JniUtil_BNCSUTIL_PLATFORM_X86 1L
#undef ch_drystayle_jghost_util_JniUtil_BNCSUTIL_PLATFORM_WINDOWS
#define ch_drystayle_jghost_util_JniUtil_BNCSUTIL_PLATFORM_WINDOWS 1L
#undef ch_drystayle_jghost_util_JniUtil_BNCSUTIL_PLATFORM_WIN
#define ch_drystayle_jghost_util_JniUtil_BNCSUTIL_PLATFORM_WIN 1L
#undef ch_drystayle_jghost_util_JniUtil_BNCSUTIL_PLATFORM_MAC
#define ch_drystayle_jghost_util_JniUtil_BNCSUTIL_PLATFORM_MAC 2L
#undef ch_drystayle_jghost_util_JniUtil_BNCSUTIL_PLATFORM_PPC
#define ch_drystayle_jghost_util_JniUtil_BNCSUTIL_PLATFORM_PPC 2L
#undef ch_drystayle_jghost_util_JniUtil_BNCSUTIL_PLATFORM_OSX
#define ch_drystayle_jghost_util_JniUtil_BNCSUTIL_PLATFORM_OSX 3L
/*
 * Class:     ch_drystayle_jghost_util_JniUtil
 * Method:    nls_init
 * Signature: (Ljava/lang/String;Ljava/lang/String;)J
 */
JNIEXPORT jlong JNICALL Java_ch_drystayle_jghost_util_JniUtil_nls_1init
  (JNIEnv *, jclass, jstring, jstring);

/*
 * Class:     ch_drystayle_jghost_util_JniUtil
 * Method:    nls_reset
 * Signature: (JLjava/lang/String;Ljava/lang/String;)J
 */
JNIEXPORT jlong JNICALL Java_ch_drystayle_jghost_util_JniUtil_nls_1reset
  (JNIEnv *, jclass, jlong, jstring, jstring);

/*
 * Class:     ch_drystayle_jghost_util_JniUtil
 * Method:    nls_delete
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_ch_drystayle_jghost_util_JniUtil_nls_1delete
  (JNIEnv *, jclass, jlong);

/*
 * Class:     ch_drystayle_jghost_util_JniUtil
 * Method:    nls_getPublicKey
 * Signature: (J)[C
 */
JNIEXPORT jcharArray JNICALL Java_ch_drystayle_jghost_util_JniUtil_nls_1getPublicKey
  (JNIEnv *, jclass, jlong);

/*
 * Class:     ch_drystayle_jghost_util_JniUtil
 * Method:    nls_getClientSessionKey
 * Signature: (JLjava/lang/String;Ljava/lang/String;)[C
 */
JNIEXPORT jcharArray JNICALL Java_ch_drystayle_jghost_util_JniUtil_nls_1getClientSessionKey
  (JNIEnv *, jclass, jlong, jstring, jstring);

/*
 * Class:     ch_drystayle_jghost_util_JniUtil
 * Method:    getExeInfo
 * Signature: (Ljava/lang/String;I)[Ljava/lang/String;
 */
JNIEXPORT jobjectArray JNICALL Java_ch_drystayle_jghost_util_JniUtil_getExeInfo
  (JNIEnv *, jclass, jstring, jint);

/*
 * Class:     ch_drystayle_jghost_util_JniUtil
 * Method:    checkRevisionFlat
 * Signature: (Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_ch_drystayle_jghost_util_JniUtil_checkRevisionFlat
  (JNIEnv *, jclass, jstring, jstring, jstring, jstring, jstring);

/*
 * Class:     ch_drystayle_jghost_util_JniUtil
 * Method:    decoder_init
 * Signature: (Ljava/lang/String;)J
 */
JNIEXPORT jlong JNICALL Java_ch_drystayle_jghost_util_JniUtil_decoder_1init
  (JNIEnv *, jclass, jstring);

/*
 * Class:     ch_drystayle_jghost_util_JniUtil
 * Method:    decoder_isKeyValid
 * Signature: (J)Z
 */
JNIEXPORT jboolean JNICALL Java_ch_drystayle_jghost_util_JniUtil_decoder_1isKeyValid
  (JNIEnv *, jclass, jlong);

/*
 * Class:     ch_drystayle_jghost_util_JniUtil
 * Method:    decoder_getProduct
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_ch_drystayle_jghost_util_JniUtil_decoder_1getProduct
  (JNIEnv *, jclass, jlong);

/*
 * Class:     ch_drystayle_jghost_util_JniUtil
 * Method:    decoder_getVal1
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_ch_drystayle_jghost_util_JniUtil_decoder_1getVal1
  (JNIEnv *, jclass, jlong);

/*
 * Class:     ch_drystayle_jghost_util_JniUtil
 * Method:    decoder_getHash
 * Signature: (JII)[C
 */
JNIEXPORT jcharArray JNICALL Java_ch_drystayle_jghost_util_JniUtil_decoder_1getHash
  (JNIEnv *, jclass, jlong, jint, jint);

/*
 * Class:     ch_drystayle_jghost_util_JniUtil
 * Method:    decoder_delete
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_ch_drystayle_jghost_util_JniUtil_decoder_1delete
  (JNIEnv *, jclass, jlong);

/*
 * Class:     ch_drystayle_jghost_util_JniUtil
 * Method:    archive_open
 * Signature: (Ljava/lang/String;)J
 */
JNIEXPORT jlong JNICALL Java_ch_drystayle_jghost_util_JniUtil_archive_1open
  (JNIEnv *, jclass, jstring);

/*
 * Class:     ch_drystayle_jghost_util_JniUtil
 * Method:    archive_readFile
 * Signature: (JLjava/lang/String;)[B
 */
JNIEXPORT jbyteArray JNICALL Java_ch_drystayle_jghost_util_JniUtil_archive_1readFile
  (JNIEnv *, jclass, jlong, jstring);

/*
 * Class:     ch_drystayle_jghost_util_JniUtil
 * Method:    archive_close
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_ch_drystayle_jghost_util_JniUtil_archive_1close
  (JNIEnv *, jclass, jlong);

/*
 * Class:     ch_drystayle_jghost_util_JniUtil
 * Method:    crc32_full
 * Signature: ([B)I
 */
JNIEXPORT jint JNICALL Java_ch_drystayle_jghost_util_JniUtil_crc32_1full
  (JNIEnv *, jclass, jbyteArray);

/*
 * Class:     ch_drystayle_jghost_util_JniUtil
 * Method:    sha1_init
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL Java_ch_drystayle_jghost_util_JniUtil_sha1_1init
  (JNIEnv *, jclass);

/*
 * Class:     ch_drystayle_jghost_util_JniUtil
 * Method:    sha1_reset
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_ch_drystayle_jghost_util_JniUtil_sha1_1reset
  (JNIEnv *, jclass, jlong);

/*
 * Class:     ch_drystayle_jghost_util_JniUtil
 * Method:    sha1_update
 * Signature: (J[B)V
 */
JNIEXPORT void JNICALL Java_ch_drystayle_jghost_util_JniUtil_sha1_1update
  (JNIEnv *, jclass, jlong, jbyteArray);

/*
 * Class:     ch_drystayle_jghost_util_JniUtil
 * Method:    sha1_final
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_ch_drystayle_jghost_util_JniUtil_sha1_1final
  (JNIEnv *, jclass, jlong);

/*
 * Class:     ch_drystayle_jghost_util_JniUtil
 * Method:    sha1_getHash
 * Signature: (J)[C
 */
JNIEXPORT jcharArray JNICALL Java_ch_drystayle_jghost_util_JniUtil_sha1_1getHash
  (JNIEnv *, jclass, jlong);

/*
 * Class:     ch_drystayle_jghost_util_JniUtil
 * Method:    sha1_delete
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_ch_drystayle_jghost_util_JniUtil_sha1_1delete
  (JNIEnv *, jclass, jlong);

/*
 * Class:     ch_drystayle_jghost_util_JniUtil
 * Method:    crc_valXORRotateLeft
 * Signature: (I[B)I
 */
JNIEXPORT jint JNICALL Java_ch_drystayle_jghost_util_JniUtil_crc_1valXORRotateLeft
  (JNIEnv *, jclass, jint, jbyteArray);

/*
 * Class:     ch_drystayle_jghost_util_JniUtil
 * Method:    crc_rotl
 * Signature: (II)I
 */
JNIEXPORT jint JNICALL Java_ch_drystayle_jghost_util_JniUtil_crc_1rotl
  (JNIEnv *, jclass, jint, jint);

#ifdef __cplusplus
}
#endif
#endif