#include <signal.h>
#include <stddef.h>
#include <stdlib.h>
#include <sys/types.h>
#include <unistd.h>
#include "main.h"
#include "dirutils.h"
#include "executils.h"
#include <stdio.h>
#include <sys/wait.h>

#define MAX_PARALLEL 40

char* types[]={"SAC1", "SAC3", "RsNSQ", "RNSQ", "NSAC", "AC", "onepSAC1", "RsNS1pQ", "RNS1pQ", "N1pSAC"};
char* env[]={"-Xmx6g","-XX:+UseSerialGC"};

static volatile sig_atomic_t stop = 0;
static pid_t pids[MAX_PARALLEL];
static void on_term(int sig){(void)sig; stop = 1;}

static void kill_children(int sig){
  for(size_t j = 0; j < MAX_PARALLEL; j++){
    if(pids[j] > 0) kill(-pids[j], sig);
  }
}

int main(int argc, char** argv){
  char* url ="http://localhost:3000/api";
  char** paths;
  char*** cmds;
  int TYPES_LEN=10;
  //size_t MAX_PARALLEL=20;

  if(argc <2){
    printf("Usage: %s instances_directory server_endpoint max_processes\n", argv[0]);
    return -1;
  }

  if(argc >= 3){
    url = argv[2];
    printf("url = %s\n", argv[2]);
  }

  printf("Chosen directory : %s\n", argv[1]);

  int n = get_instances_paths_at_path(&paths, argv[1]);
  if(n < 0){
    return -1;
  }

  int cmds_len = generate_commands(n, TYPES_LEN, &cmds, paths, types, url);

  struct sigaction sa = {0};
  sa.sa_handler = on_term;
  sa.sa_flags = SA_RESTART;
  sigemptyset(&sa.sa_mask);
  sigaction(SIGINT, &sa, NULL);
  sigaction(SIGTERM, &sa, NULL);

  int i = 0;
  int active = 0;
  //pid_t pids[MAX_PARALLEL];
  memset(pids, 0, sizeof pids);

  while(i<cmds_len || active>0){
    // if asked to stop: TERM, grace, then KILL
    if(stop){
      kill_children(SIGTERM);
      for(int t = 0; t<200; t++){
        for(size_t j = 0; j < MAX_PARALLEL; j++){
          if(pids[j] > 0){
            int st; pid_t r = waitpid(pids[j], &st, WNOHANG);
            if(r>0){pids[j] = 0; active--;}
          }
        }
        usleep(10000);
      }
      kill_children(SIGKILL);
    }
    // Look for finished/non-started experiments
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

    if(stop){
      if(active==0) break;
      usleep(10000);
      continue;
    }

    // Launch as many experiments as possible
    for(int j = 0; j< MAX_PARALLEL && i < cmds_len; j++){
      if(pids[j] == 0){
        pid_t pid = fork();
        if(pid == 0){
          setpgid(0,0);
          execvp(cmds[i][0], cmds[i]);
          perror("exec failed");
          _exit(127);
        } else if(pid >0){
          pids[j] =pid;
          active++;
          i++;
          printf("**************************************************\n\n");
          printf("\t\tLaunching experiment: %d / %d\n\n", i, cmds_len);
          printf("**************************************************\n\n");
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


