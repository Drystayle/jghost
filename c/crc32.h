#ifndef CRC32_H
#define CRC32_H

// standard integer sizes for 64 bit compatibility

#ifdef WIN32
 #include "ms_stdint.h"
#else
 #include <stdint.h>
#endif

// STL

#include <fstream>
#include <iostream>
#include <sstream>
#include <iomanip>
#include <algorithm>
#include <map>
#include <queue>
#include <string>
#include <vector>

#define CRC32_POLYNOMIAL 0x04c11db7

class CCRC32
{
public:
	void Initialize( );
	uint32_t FullCRC( unsigned char *sData, uint32_t ulLength );
	void PartialCRC( uint32_t *ulInCRC, unsigned char *sData, uint32_t ulLength );

private:
	uint32_t Reflect( uint32_t ulReflect, char cChar );
	uint32_t ulTable[256];
};

#endif
