#include "executils.h"

int generate_commands(size_t paths_len, size_t types_len,  char*** *cmds, char **paths, char **types, char *url){
  *cmds = malloc(sizeof(char**) * paths_len * types_len);
  int index=0;
  for(int i = 0; i<paths_len;i++){
    for(int j = 0; j<types_len; j++){
      (*cmds)[index] = malloc(sizeof(char *) * 18);
      (*cmds)[index][0] = "/usr/bin/java";
      (*cmds)[index][1] = "-jar";
      (*cmds)[index][2] = "../parsers/target/choco-parsers-5.0.0-beta.1-jar-with-dependencies.jar";
      (*cmds)[index][3] = paths[i];
      (*cmds)[index][4] = "-pa";
      (*cmds)[index][5] = "5";
      (*cmds)[index][6] = "-p";
      (*cmds)[index][7] = "1";
      (*cmds)[index][8] = "-sc";
      (*cmds)[index][9] = types[j];
      (*cmds)[index][10] = "-to";
      (*cmds)[index][11] = "3600000";
      (*cmds)[index][12] = "-monitor";
      (*cmds)[index][13] = "-lvl";
      (*cmds)[index][14] = "INFO";
      (*cmds)[index][15] = "-url";
      (*cmds)[index][16] = url;
      (*cmds)[index][17] = NULL;
      index++;
    }
  }
  return index;
}

void free_commands(char*** *cmds, size_t len){
  for(int i = 0; i<len; i++){
    free((*cmds)[i]);
  }
  free((*cmds));
}
