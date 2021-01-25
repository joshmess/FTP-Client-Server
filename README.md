# simple-ftp project
Josh Messitte, Alex Holmes, Robert

## Current State:
- Server works in linux environment.
- cd, mkdir, pwd, ls, and put have complete functionality and are ready for testing.
## Outstanding Issues:
- When the server is run and the client uses 'cd' to navigate outside of the server's residing directory, 'get' and 'delete' then have bugs.

### Compile Client:
```
$ javac myftp.java
```
### Compile Server:
```
$ javac myftpserver.java
```

### Run Client:
```
$ java myftp [MACHINE_NAME] [PORT]
```

### Run Server:
```
$ java myftpserver [PORT]
```
