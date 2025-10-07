# Docker image info

https://hub.docker.com/r/amazon/aws-cli

# Pull Docker image

```
docker pull amazon/aws-cli:2.31.9
```

```
docker run --rm -it amazon/aws-cli:2.31.9 --version
```

```
docker run --rm -it amazon/aws-cli:2.31.9 --help
```

```
docker run --rm -ti -v /home/ubuntu/aws:/root/.aws amazon/aws-cli:2.31.9 s3 ls
```

```
docker run --rm -ti -v /home/ubuntu/aws:/root/.aws amazon/aws-cli:2.31.9 --endpoint-url=https://<endpoint>/ s3 ls s3://<bucket-name>/protonmail/<filen-name>.zip
```

# Copy 1 file

```
docker run --rm -ti \
    -v /home/ubuntu/aws:/root/.aws -v /home/ubuntu/proton-imported.tar.gz:/workspace/proton-imported.tar.gz \
    amazon/aws-cli:2.31.9 --endpoint-url=<endpoint> \
    s3 cp /workspace/proton-imported.tar.gz s3://backup-lingmine-old/protonmail/proton-imported.tar.gz
```



## File `~/aws/config`

```
[default]
region=waw
output=text
```

## File `~/aws/credentials`

```
[default]
aws_access_key_id=<todo>
aws_secret_access_key=<todo>
```



# Verify

```
docker run --rm -ti \
    -v /home/ubuntu/aws:/root/.aws \
    amazon/aws-cli:2.31.9 --endpoint-url=<endpoint> \
    s3api head-object --bucket backup-lingmine-old --key protonmail/proton-imported.tar.gz
```

