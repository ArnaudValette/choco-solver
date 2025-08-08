#include <stdlib.h>
#include <sys/types.h>
#include <unistd.h>
#include "main.h"
#include "dirutils.h"
#include "executils.h"
#include <stdio.h>
#include <sys/wait.h>


char* types[7]={"SAC1", "SAC3", "RsNSQ", "RNSQ", "NSAC", "AC", "NONE"};
char* env[]={"-Xmx512m","-XX:+UseSerialGC", "-Xint"};

int main(int argc, char** argv){
  char* url ="http://localhost:3000/api";
  char** paths;
  char*** cmds;
  int TYPES_LEN=7;
  size_t MAX_PARALLEL=10;

  if(argc <2){
    printf("Usage: %s instances_directory server_endpoint max_processes\n", argv[0]);
    return -1;
  }

  if(argc == 3){
    url = argv[2];
  }

  if(argc == 4){
    MAX_PARALLEL = atoi(argv[3]);
  }


  printf("Chosen directory : %s\n", argv[1]);

  int n = get_instances_paths_at_path(&paths, argv[1]);
  if(n < 0){
    return -1;
  }

  int cmds_len = generate_commands(n, 7, &cmds, paths, types, url);

  int i = 0;
  int active = 0;
  pid_t pids[MAX_PARALLEL];
  memset(pids, 0, sizeof pids);
  while(i<cmds_len || active>0){
    for(int j = 0; j < MAX_PARALLEL; j++){
      if(pids[j] > 0){
        int status;
        pid_t res = waitpid(pids[j], &status, WNOHANG);
        if(res > 0){
          pids[j] = 0;
          active--;
        }
      }
    }

    for(int j = 0; j< MAX_PARALLEL && i < cmds_len; j++){
      if(pids[j] == 0){
        pid_t pid = fork();
        if(pid == 0){
          execve(cmds[i][0], cmds[i], env);
          perror("exec failed");
          exit(1);
        } else if(pid >0){
          pids[j] =pid;
          active++;
          i++;
        } else{
          perror("fork fail !?");
          exit(1);
        }
      }
    }
    usleep(10000);
  }

  free_commands(&cmds, cmds_len);
  free_paths(&paths, n);

  return 0;
}


