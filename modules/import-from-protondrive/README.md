# Links

- https://rclone.org/protondrive/
- 

# Installation

See https://rclone.org/install/

```
curl -O https://downloads.rclone.org/v1.71.2/rclone-v1.71.2-linux-amd64.zip
unzip rclone-v1.71.2-linux-amd64.zip
```

# RClone commands

```
./rclone listremotes
```

```
./rclone ls protondrive:FOLDER
```

/home/ubuntu/protondrive/proton-local

```
./rclone copy protondrive:WIEDZA /home/ubuntu/protondrive/proton-local/WIEDZA
```

```
./rclone copy --progress protondrive:WIEDZA /home/ubuntu/protondrive/proton-local/WIEDZA
```

```
./rclone copy --progress /home/ubuntu/protondrive/proton-local/WIEDZA prot:prot/WIEDZA
```


# RClone & OVH

https://help.ovhcloud.com/csm/pl-public-cloud-storage-s3-rclone?id=kb_article_view&sysparm_article=KB0047464

