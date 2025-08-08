#include "dirutils.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>


char* prepend(const char* prefix, const char* str){
  if(!prefix || !str) return NULL;
  size_t len1 = strlen(prefix);
  size_t len2 = strlen(str);
  char* res = malloc(len1 + len2 + 1);
  if(!res) return NULL;
  memcpy(res, prefix, len1);
  memcpy(res + len1, str, len2 +1);
  return res;
}

int get_instances_paths_at_path(char** *paths, char* dir_path){
  DIR *dir = opendir(dir_path);
  if(!dir) return -1;

  /* Allocate */
  int n = get_dir_entry_length(dir);
  if(n < 0){
    return -2;
  }
  *paths = malloc(n * sizeof(char*));
  closedir(dir);

  /* Get instances path */
  dir = opendir(dir_path);
  dir_entry_foreach(dir, fill_entries, *paths);

  /* Prepend to obtain full files paths */
  size_t len = strlen(dir_path);
  int badly_terminated = dir_path[len-1] != '/';
  char* prefix = malloc(len + badly_terminated +1);

  if(!prefix) exit(1);
  strcpy(prefix, dir_path);
  if(badly_terminated) strcat(prefix, "/");

  for(int i = 0; i<n; i++){
    char* tmp = prepend(prefix, (*paths)[i]);
    (*paths)[i]= tmp;
  }
  closedir(dir);
  free(prefix); 
  return n;
}

void free_paths(char ***paths, size_t size){
  for(int i = 0; i< size; i++){
    free((*paths)[i]);
  }
  free(*paths);
}

void fill_entries(const char* name, int index, void* ctx){
  char** paths = (char**)ctx;
  paths[index] = name; 
}

void count_entries(const char* name, int index, void* ctx){
  int* counter = (int*)ctx;
  (*counter)++;
}

int get_dir_entry_length(DIR *dir){
  int i = 0;
  dir_entry_foreach(dir, count_entries, &i);
  return i;
}

void dir_entry_foreach(DIR* dir, entry_callback fn, void* ctx){
  struct dirent *entry;
  int index = 0;
  while((entry = readdir(dir)) != NULL){
    if(strcmp(entry->d_name, ".") == 0 || strcmp(entry->d_name, "..") == 0)
      continue;
    fn(entry->d_name, index , ctx);
    index++;
  }
  
}
