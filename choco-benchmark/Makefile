CC = gcc
CFLAGS = -Wall -Wextra -O2
SRC = main.c dirutils.c executils.c
OBJ = $(SRC:.c=.o)
BIN = benchmark

all: $(BIN)
	@rm -f $(OBJ)

$(BIN): $(OBJ)
	$(CC) $(CFLAGS) -o $@ $^

%.o: %.c
	$(CC) $(CFLAGS) -c $< -o $@

clean:
	rm -f $(OBJ) $(BIN)
