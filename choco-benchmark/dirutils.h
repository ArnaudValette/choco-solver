#ifndef DIRUTILS_H
#define DIRUTILS_H
#include <string.h>
#include <dirent.h>
#include <stdlib.h>

char* prepend(const char* prefix, const char* str);

/* void* types */
typedef void (*entry_callback)(const char* name, int index, void*ctx);
void count_entries(const char* name, int index, void*ctx);
void fill_entries(const char* name, int index, void* ctx);

/** Modifies paths by filling it with the paths
    of the files located in dir_path.
    Returns -1 if dir_path is not a dir; otherwise
    it returns the number of elements in the newly
    allocated char** *paths.
    Call free_paths(&paths, size) to free the char***.
 */
int get_instances_paths_at_path(char** *paths, char* dir_path);
void free_paths(char ***paths, size_t size);
int get_dir_entry_length(DIR*);
void dir_entry_foreach(DIR*, entry_callback, void*);

#endif
