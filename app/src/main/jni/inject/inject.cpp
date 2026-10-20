#include "../backend/Includes.h"
#include <unistd.h>
#include <sys/system_properties.h>
#include <dlfcn.h>
#include <KittyMemory/KittyInclude.hpp>
#include "dobby/include/dobby.h"
#include <KittyMemory/KittyInclude.hpp>
#include <KittyMemory/KittyScanner.hpp>

/*
function WRITEBYTES(address,flags,value) local tt={} tt[1]={} tt[1].address=address tt[1].flags=flags tt[1].value=value gg.setValues(tt) gg.addListItems(tt) end
local off
local Floshy=gg.getRangesList('libUE4.so')[1].start

WRITEBYTES(Floshy +0x3C6FBBC,32 ,"h 00 00 A0 E3 1E FF 2F E1")
*/
void* SuperThread(void* parms)
{
    auto lib = KittyMemory::getBaseMapOf("libUE4.so");
    while(!lib.isValid()) {
        lib = KittyMemory::getBaseMapOf("libUE4.so");
        sleep(1);
    }
    
    auto patch = MemoryPatch::createWithHex(lib, 0x3C6FBBC, "00 00 A0 E3 1E FF 2F E1");
    if (patch.Modify()) {
        LOG("Patch CurrBytes: %s", patch.get_CurrBytes().c_str());
    }
    
    dlopen("libavaritia.so", RTLD_LAZY);
    LOG("loaded libavaritia.so");
    return 0;
}

__attribute__((constructor))
void _init() {
    LOG("LIB MAIN CALLED");
    pthread_t t;
	pthread_create(&t, 0, SuperThread, 0);
}