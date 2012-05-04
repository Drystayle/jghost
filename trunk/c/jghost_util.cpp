#include "jghost_util.h"
#include <bncsutil/nls.h>
#include <bncsutil/checkrevision.h>
#include <bncsutil/cdkeydecoder.h>
#include <string.h>
#include <sstream>
#include <string>
#include "crc32.h"
#include "sha1.h"

#define __STORMLIB_SELF__
#include <stormlib/StormLib.h>

#define ROTL(x,n) ((x)<<(n))|((x)>>(32-(n)))	// this won't work with signed types
#define ROTR(x,n) ((x)>>(n))|((x)<<(32-(n)))	// this won't work with signed types

uint32_t XORRotateLeft( unsigned char *data, uint32_t length );

//TODO: Release java -> c strings

/*
 * Class:     ch_drystayle_jghost_bnet_util_jni_BNCSutil
 * Method:    nls_init
 * Signature: (Ljava/lang/String;Ljava/lang/String;)J
 */
JNIEXPORT jlong JNICALL Java_ch_drystayle_jghost_util_JniUtil_nls_1init
	(JNIEnv *env, jclass clazz, jstring username, jstring password) {
	NLS* nls = new NLS( 
		env->GetStringUTFChars(username, NULL),
		env->GetStringUTFChars(password, NULL)
	);

	return (jlong) nls;
}

/*
 * Class:     ch_drystayle_jghost_bnet_util_jni_BNCSutil
 * Method:    nls_reset
 * Signature: (JLjava/lang/String;Ljava/lang/String;)J
 */
JNIEXPORT jlong JNICALL Java_ch_drystayle_jghost_util_JniUtil_nls_1reset
	(JNIEnv *env, jclass clazz, jlong nlsPointer, jstring username, jstring password) {
	delete (NLS *)nlsPointer;
	NLS* nls = new NLS( 
		env->GetStringUTFChars(username, NULL),
		env->GetStringUTFChars(password, NULL)
	);
	
	return (jlong) nls;
}

/*
 * Class:     ch_drystayle_jghost_bnet_util_jni_BNCSutil
 * Method:    nls_delete
 * Signature: (J)J
 */
JNIEXPORT void JNICALL Java_ch_drystayle_jghost_util_JniUtil_nls_1delete
	(JNIEnv *env, jclass clazz, jlong nlsPointer) {
	delete (NLS *)nlsPointer;
}

/*
 * Class:     ch_drystayle_jghost_bnet_util_jni_BNCSutil
 * Method:    nls_getPublicKey
 * Signature: (J)Ljava/lang/String;
 */
JNIEXPORT jcharArray JNICALL Java_ch_drystayle_jghost_util_JniUtil_nls_1getPublicKey
	(JNIEnv *env, jclass clazz, jlong nlsPointer) {

	const char* buf = ((NLS *)nlsPointer)->getPublicKey();
	
	jcharArray ret = env->NewCharArray(32);
	
	for (unsigned int i = 0; i < 32; i++) {
		jchar tmp = (jchar) (unsigned char) buf[i];
		env->SetCharArrayRegion(ret, i, 1, &tmp); 
	}

	return ret;
}

/*
 * Class:     ch_drystayle_jghost_bnet_util_jni_BNCSutil
 * Method:    nls_getClientSessionKey
 * Signature: (JLjava/lang/String;Ljava/lang/String;)Ljava/lang/String;
 */
JNIEXPORT jcharArray JNICALL Java_ch_drystayle_jghost_util_JniUtil_nls_1getClientSessionKey
	(JNIEnv *env, jclass clazz, jlong nlsPointer, jstring salt, jstring serverKey) {
	
	//convert string -> char*
	const jchar* saltJChar = env->GetStringChars(salt, NULL);
	jsize saltLength = env->GetStringLength(salt);
	char* saltChar = new char[saltLength];
	for (int i = 0; i < saltLength; i++) {
		saltChar[i] = (char) saltJChar[i];
	}

	const jchar* serverKeyJChar = env->GetStringChars(serverKey, NULL);
	jsize serverKeyLength = env->GetStringLength(serverKey);
	char* serverKeyChar = new char[serverKeyLength];
	for (int i = 0; i < serverKeyLength; i++) {
		serverKeyChar[i] = (char) serverKeyJChar[i];
	}

	/*printf("salt: ");
	for (int i = 0; i < saltLength; i++) {
		printf("%i ", ((unsigned char) saltChar[i]));
	}*/

	
	const char* buf =((NLS *)nlsPointer)->getClientSessionKey(
		saltChar, serverKeyChar
	);

	delete saltChar;
	delete serverKeyChar;

	jcharArray ret = env->NewCharArray(20);
	for (unsigned int i = 0; i < 20; i++) {
		jchar tmp = (jchar) (unsigned char) buf[i];
		env->SetCharArrayRegion(ret, i, 1, &tmp); 
	}

	return ret;
}

/*
 * Class:     ch_drystayle_jghost_bnet_util_jni_BNCSutil
 * Method:    getExeInfo
 * Signature: (Ljava/lang/String;II)Ljava/lang/String;
 */
JNIEXPORT jobjectArray JNICALL Java_ch_drystayle_jghost_util_JniUtil_getExeInfo
	(JNIEnv *env, jclass clazz, jstring fileWar3exe, jint platform) {
	char buf[2048];
	uint32_t EXEVersion;
	jobjectArray ret;
	std::string tmp;
	std::stringstream stringstr;

	getExeInfo(
		env->GetStringUTFChars(fileWar3exe, NULL),
		buf,
		2048,
		(uint32_t *) &EXEVersion,
		(int) platform
	);
	
	ret= (jobjectArray)env->NewObjectArray(
		5,
		env->FindClass("java/lang/String"),
		env->NewStringUTF("")
	);
	
	//Version to array[0]
	stringstr << EXEVersion;
	stringstr >> tmp;
	env->SetObjectArrayElement(ret, 0, env->NewStringUTF(tmp.c_str()));
	
	//Info to array[1]
	env->SetObjectArrayElement(ret, 1, env->NewStringUTF(buf));

	return ret;
}

/*
 * Class:     ch_drystayle_jghost_bnet_util_jni_BNCSutil
 * Method:    checkRevisionFlat
 * Signature: (Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_ch_drystayle_jghost_util_JniUtil_checkRevisionFlat
(JNIEnv *env, jclass clazz , jstring valueStringFormula, jstring war3exeFileName, jstring stormDllFileName, jstring gameDllFileName, jstring mpqFileName) {
	unsigned long EXEVersionHash;

	checkRevisionFlat( 
		env->GetStringUTFChars(valueStringFormula, NULL), 
		env->GetStringUTFChars(war3exeFileName, NULL), 
		env->GetStringUTFChars(stormDllFileName, NULL),
		env->GetStringUTFChars(gameDllFileName, NULL),
		extractMPQNumber(env->GetStringUTFChars(mpqFileName, NULL)), 
		&EXEVersionHash 
	);

	return (jint) EXEVersionHash;
}

/*
 * Class:     ch_drystayle_jghost_bnet_util_jni_BNCSutil
 * Method:    decoder_init
 * Signature: (Ljava/lang/String;)J
 */
JNIEXPORT jlong JNICALL Java_ch_drystayle_jghost_util_JniUtil_decoder_1init
(JNIEnv *env, jclass clazz, jstring key) {
	return (jlong) new CDKeyDecoder(env->GetStringUTFChars(key, NULL), env->GetStringUTFLength(key));
}

/*
 * Class:     ch_drystayle_jghost_bnet_util_jni_BNCSutil
 * Method:    decoder_isKeyValid
 * Signature: (J)I
 */
JNIEXPORT jboolean JNICALL Java_ch_drystayle_jghost_util_JniUtil_decoder_1isKeyValid
(JNIEnv *env, jclass clazz, jlong decoderPointer) {
	return (jboolean) ((CDKeyDecoder *)decoderPointer)->isKeyValid();
}

/*
 * Class:     ch_drystayle_jghost_bnet_util_jni_BNCSutil
 * Method:    decoder_getProduct
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_ch_drystayle_jghost_util_JniUtil_decoder_1getProduct
(JNIEnv *env, jclass clazz, jlong decoderPointer) {
	return (jint) ((CDKeyDecoder *)decoderPointer)->getProduct();
}

/*
 * Class:     ch_drystayle_jghost_bnet_util_jni_BNCSutil
 * Method:    decoder_getVal1
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_ch_drystayle_jghost_util_JniUtil_decoder_1getVal1
(JNIEnv *env, jclass clazz, jlong decoderPointer) {
	return (jint) ((CDKeyDecoder *)decoderPointer)->getVal1();
}

/*
 * Class:     ch_drystayle_jghost_bnet_util_jni_BNCSutil
 * Method:    decoder_getHash
 * Signature: (JII)Ljava/lang/String;
 */
JNIEXPORT jcharArray JNICALL Java_ch_drystayle_jghost_util_JniUtil_decoder_1getHash
(JNIEnv *env, jclass clazz, jlong decoderPointer, jint clientToken, jint serverToken) {
	size_t Length = ((CDKeyDecoder *)decoderPointer)->calculateHash((unsigned int) clientToken, (unsigned int) serverToken);
	char* buf = new char[Length];
	Length = ((CDKeyDecoder *)decoderPointer)->getHash(buf);
	jcharArray ret = env->NewCharArray(Length);
	
	for (unsigned int i = 0; i < Length; i++) {
		jchar tmp = (jchar) (unsigned char) buf[i];
		env->SetCharArrayRegion(ret, i, 1, &tmp); 
	}
	return ret;
}

/*
 * Class:     ch_drystayle_jghost_bnet_util_jni_BNCSutil
 * Method:    decoder_delete
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_ch_drystayle_jghost_util_JniUtil_decoder_1delete
(JNIEnv *env, jclass clazz, jlong decoderPointer) {
	delete (CDKeyDecoder *)decoderPointer;
}

/*
 * Class:     ch_drystayle_jghost_bnet_util_jni_BNCSutil
 * Method:    archive_open
 * Signature: (Ljava/lang/String;)J
 */
JNIEXPORT jlong JNICALL Java_ch_drystayle_jghost_util_JniUtil_archive_1open
(JNIEnv *env, jclass clazz, jstring archivePath) {
	HANDLE PatchMPQ;
	const char* archiveName = env->GetStringUTFChars(archivePath, NULL);
	if (SFileOpenArchive(env->GetStringUTFChars(archivePath, NULL), 0, 0, &PatchMPQ)) {
		return (jlong) PatchMPQ;
	} else {
		//throw IOException
		jclass exc = env->FindClass("java/io/IOException");
		if (exc != NULL) {
			env->ThrowNew(exc, "Unable to open archive file");
		} else {
			return (jlong) -1;
		}
	}

	return (jlong) -1;
}

/*
 * Class:     ch_drystayle_jghost_bnet_util_jni_BNCSutil
 * Method:    archive_readFile
 * Signature: (JLjava/lang/String;)[C
 */
JNIEXPORT jbyteArray JNICALL Java_ch_drystayle_jghost_util_JniUtil_archive_1readFile
(JNIEnv *env, jclass clazz, jlong archivePointer, jstring filePath) {
	HANDLE SubFile;
	const char* filePathC = env->GetStringUTFChars(filePath, NULL);

	if( SFileOpenFileEx((HANDLE) archivePointer, filePathC, 0, &SubFile ) ) {
		unsigned int FileLength = SFileGetFileSize( SubFile, NULL );

		if( FileLength > 0 && FileLength != 0xFFFFFFFF ) {
			char *SubFileData = new char[FileLength];
			DWORD BytesRead = 0;

			if(SFileReadFile(SubFile, SubFileData, FileLength, &BytesRead)) {
				jbyteArray jbArray = env->NewByteArray(FileLength);
				env->SetByteArrayRegion(jbArray, 0, FileLength, (jbyte *) SubFileData);
				return jbArray;
			} else {
				//throw IOException
				jclass exc = env->FindClass("java/io/IOException");
				if (exc != NULL) {
					env->ThrowNew(exc, "Unable to extract data from MPQ file");
				} else {
					return (jbyteArray) NULL;
				}
			}

			delete [] SubFileData;
		}

		SFileCloseFile(SubFile);
	} else {
		//throw FileNotFoundException
		jclass exc = env->FindClass("java/io/IOException");
		if (exc != NULL) {
			env->ThrowNew(exc, "Couldn't find the specified path in the MPQ file");
		} else {
			return (jbyteArray) NULL;
		}
	}

	return (jbyteArray) NULL;
}

/*
 * Class:     ch_drystayle_jghost_bnet_util_jni_BNCSutil
 * Method:    archive_close
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_ch_drystayle_jghost_util_JniUtil_archive_1close
(JNIEnv *env, jclass clazz, jlong archivePointer) {
	SFileCloseArchive((HANDLE) archivePointer);
}

/*
 * Class:     ch_drystayle_jghost_util_JniUtil
 * Method:    crc32_full
 * Signature: ([B)I
 */
JNIEXPORT jint JNICALL Java_ch_drystayle_jghost_util_JniUtil_crc32_1full
(JNIEnv *env, jclass clazz, jbyteArray bData) {
	CCRC32 crc32;
	crc32.Initialize();
	
	uint32_t i = crc32.FullCRC(
		(unsigned char*) env->GetByteArrayElements(bData, false), 
		env->GetArrayLength(bData)
	);

	return (jint) i;
}

/*
 * Class:     ch_drystayle_jghost_util_JniUtil
 * Method:    sha1_init
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL Java_ch_drystayle_jghost_util_JniUtil_sha1_1init
(JNIEnv *env, jclass clazz) {
	return (jlong) new CSHA1( );
}

/*
 * Class:     ch_drystayle_jghost_util_JniUtil
 * Method:    sha1_reset
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_ch_drystayle_jghost_util_JniUtil_sha1_1reset
(JNIEnv *env, jclass clazz, jlong sha1Pointer) {
	CSHA1* sha = (CSHA1*) sha1Pointer;
	sha->Reset();
}

/*
 * Class:     ch_drystayle_jghost_util_JniUtil
 * Method:    sha1_update
 * Signature: (J[B)V
 */
JNIEXPORT void JNICALL Java_ch_drystayle_jghost_util_JniUtil_sha1_1update
(JNIEnv *env, jclass clazz, jlong sha1Pointer, jbyteArray data) {
	CSHA1* sha = (CSHA1*) sha1Pointer;
	sha->Update(
		(unsigned char*) env->GetByteArrayElements(data, false),
		env->GetArrayLength(data)
	);
}

/*
 * Class:     ch_drystayle_jghost_util_JniUtil
 * Method:    sha1_final
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_ch_drystayle_jghost_util_JniUtil_sha1_1final
(JNIEnv *env, jclass clazz, jlong sha1Pointer) {
	CSHA1* sha = (CSHA1*) sha1Pointer;
	sha->Final();
}

/*
 * Class:     ch_drystayle_jghost_util_JniUtil
 * Method:    sha1_getHash
 * Signature: (J)[C
 */
JNIEXPORT jcharArray JNICALL Java_ch_drystayle_jghost_util_JniUtil_sha1_1getHash
(JNIEnv *env, jclass clazz, jlong sha1Pointer) {
	CSHA1* sha = (CSHA1*) sha1Pointer;
	unsigned char SHA1[20];
	memset( SHA1, 0, sizeof( unsigned char ) * 20 );
	sha->GetHash(SHA1);
	jcharArray reArray = env->NewCharArray(20);
	
	for (int i = 0; i < 20; i++) {
		jchar tmp = (jchar) SHA1[i];
		env->SetCharArrayRegion(reArray, i, 1, &tmp); 
	}

	return reArray;
}

/*
 * Class:     ch_drystayle_jghost_util_JniUtil
 * Method:    sha1_delete
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_ch_drystayle_jghost_util_JniUtil_sha1_1delete
(JNIEnv *env, jclass clazz, jlong sha1Pointer) {
	CSHA1* sha = (CSHA1*) sha1Pointer;
	delete sha;
}

/*
 * Class:     ch_drystayle_jghost_util_JniUtil
 * Method:    crc_valXORRotateLeft
 * Signature: (I[B)I
 */
JNIEXPORT jint JNICALL Java_ch_drystayle_jghost_util_JniUtil_crc_1valXORRotateLeft
(JNIEnv *env, jclass clazz, jint val, jbyteArray data) {
	uint32_t val_i = (uint32_t) val;
	
	val_i = val_i ^ XORRotateLeft(
		(unsigned char*) env->GetByteArrayElements(data, false),
		env->GetArrayLength(data)
	);
	
	return (jint) val_i;
}

/*
 * Class:     ch_drystayle_jghost_util_JniUtil
 * Method:    crc_rotl
 * Signature: (II)I
 */
JNIEXPORT jint JNICALL Java_ch_drystayle_jghost_util_JniUtil_crc_1rotl
(JNIEnv *env, jclass clazz, jint val, jint n) {
	uint32_t val_i = (uint32_t) val;
	return (jint) ROTL(val_i, n);
}

uint32_t XORRotateLeft (unsigned char *data, uint32_t length )
{
	// a big thank you to Strilanc for figuring this out

	uint32_t i = 0;
	uint32_t Val = 0;

	if( length > 3 )
	{
		while( i < length - 3 )
		{
			Val = ROTL( Val ^ ( (uint32_t)data[i] + (uint32_t)( data[i + 1] << 8 ) + (uint32_t)( data[i + 2] << 16 ) + (uint32_t)( data[i + 3] << 24 ) ), 3 );
			i += 4;
		}
	}

	while( i < length )
	{
		Val = ROTL( Val ^ data[i], 3 );
		i++;
	}

	return Val;
}