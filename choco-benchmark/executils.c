#include "executils.h"

int generate_commands(size_t paths_len, size_t types_len,  char*** *cmds, char **paths, char **types, char *url){
  *cmds = malloc(sizeof(char**) * paths_len * types_len);
  int index=0;
  for(int i = 0; i<paths_len;i++){
    for(int j = 0; j<types_len; j++){
      (*cmds)[index] = malloc(sizeof(char *) * 18);
      (*cmds)[index][0] = "/usr/bin/java";
      (*cmds)[index][1] = "-Xmx10g";
      (*cmds)[index][2] = "-XX:+UseSerialGC";
      //(*cmds)[index][3] = "-Xint";
      (*cmds)[index][3] = "-jar";
      (*cmds)[index][4] = "../parsers/target/choco-parsers-5.0.0-beta.1-jar-with-dependencies.jar";
      (*cmds)[index][5] = paths[i];
      (*cmds)[index][6] = "-pa";
      (*cmds)[index][7] = "5";
      (*cmds)[index][8] = "-p";
      (*cmds)[index][9] = "1";
      (*cmds)[index][10] = "-sc";
      (*cmds)[index][11] = types[j];
      (*cmds)[index][12] = "-to";
      (*cmds)[index][13] = "3600000";
      (*cmds)[index][14] = "-monitor";
      (*cmds)[index][15] = "-lvl";
      (*cmds)[index][16] = "SILENT";
      (*cmds)[index][17] = NULL;
      //(*cmds)[index][18] = "-url";
      //(*cmds)[index][19] = url;
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
