gcc -c -fPIC hello.c
gcc -shared -o libhello.so hello.o
#sudo cp libhello.so /usr/lib

