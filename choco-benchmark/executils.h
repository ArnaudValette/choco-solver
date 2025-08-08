#ifndef EXECUTILS_H
#define EXECUTILS_H
#include <stddef.h>
#include <stdlib.h>
int generate_commands(size_t paths_len, size_t types_len, char*** *cmds, char **paths, char **types, char *url);
void free_commands(char*** *cmds, size_t len);
#endif
