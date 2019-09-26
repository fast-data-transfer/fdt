/*
 * LD_PRELOAD shim to force files to be opened with the O_DIRECT flag,
 * avoiding cache thrashing and allowing for 0-copy reads/writes.
 *
 * Will compare all open() file names to the prefix FDT_DIRECT_PATH,
 * and if it matches it will add O_DIRECT to the flags passes to
 * the real open64() call.
 *
 * Note that this is a simple string comparison, so things like relative
 * paths or ../s will make things not match.
 * ex:
 *     export FDT_DIRECT_PATH=/home/user/fdt-data
 *     open("fdt-data/file1") <= Will not match, no exact strcmp()
 *     open("/home/user/fdt-data/file1") <= Will set O_DIRECT
 *     open("/home/user/fdt-data/dir1/file2") <= Also matches
 *     open("/home/user/fdt-data/../otherfile") <= Will match, but
 *                  you probably didn't intend it to.
 * Because of this, it is recommended to only use fully qualified paths
 * in directories or files passed into fdt.
 *
 * Licensed under the Apache License 2.0, included with this repo.
 * Earle F. Philhower, III  <earlephilhower@yahoo.com>
 */

/*
 * Compile with:
 *    gcc -shared -fPIC open_direct.c -o open_direct.so -ldl
 * Run FDT:
 *    FDT_DIRECT_PATH=/mnt LD_PRELOAD=./open_direct.so java -jar fdt.jar ..
 */


#define _GNU_SOURCE
#include <dlfcn.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>

// Match the open64() API
typedef int (*open64_type)(const char *path, int f);

// LD_PRELOAD will cause java open64() calls to use this instead of the system
// version of open64().
int open64(const char *file, int flags, ...)
{
    static const char *directdir = NULL;
    static int directdirlen = 0;
    static int skip = 0;
    static open64_type real_open64 = NULL;

    // First pass through, get the settings and real open64 address
    if (!directdir && !skip) {
        real_open64 = (open64_type)dlsym(RTLD_NEXT,"open64");
        directdir = getenv("FDT_DIRECT_PATH");
        if (!directdir) {
            skip = 1; // No mask defined, don't do anything...
        } else {
            // Cache the length to avoid calling strlen on every open
            directdirlen = strlen(directdir);
        }
    }

    // Now handle the logic of the call.  Rely on C early expression termination
    if (!skip && !strncmp(directdir, file, directdirlen)) {
        flags |= O_DIRECT;
        fprintf(stderr, "DIRECT opening: '%s'\n", file);
    } else {
        fprintf(stderr, "Normal opening: '%s'\n", file);
    }
    return real_open64(file, flags);
}
