# Generate SSH keys

```
ssh-keygen -t rsa -b 4096 -C "proton-mail-export-to-gmail"
```

Keys location:

```
/home/vscode/.ssh/id_rsa
```

Example connection

```
ssh ubuntu@yourhost.com -i id_rsa 
```


