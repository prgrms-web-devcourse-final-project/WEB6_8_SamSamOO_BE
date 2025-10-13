terraform {
  // aws 라이브러리 불러옴
  required_providers {
    aws = {
      source = "hashicorp/aws"
    }
  }
}

# AWS 설정 시작
provider "aws" {
  region = var.region
}
# AWS 설정 끝

# VPC 설정 시작
resource "aws_vpc" "vpc_1" {
  cidr_block = "10.0.0.0/16"

  enable_dns_support   = true
  enable_dns_hostnames = true

  tags = {
    Name = "${var.prefix}-vpc-1"
  }
}

resource "aws_subnet" "subnet_1" {
  vpc_id                  = aws_vpc.vpc_1.id
  cidr_block              = "10.0.0.0/24"
  availability_zone       = "${var.region}a"
  map_public_ip_on_launch = true

  tags = {
    Name = "${var.prefix}-subnet-1"
  }
}

resource "aws_subnet" "subnet_2" {
  vpc_id                  = aws_vpc.vpc_1.id
  cidr_block              = "10.0.1.0/24"
  availability_zone       = "${var.region}b"
  map_public_ip_on_launch = true

  tags = {
    Name = "${var.prefix}-subnet-2"
  }
}

resource "aws_subnet" "subnet_3" {
  vpc_id                  = aws_vpc.vpc_1.id
  cidr_block              = "10.0.2.0/24"
  availability_zone       = "${var.region}c"
  map_public_ip_on_launch = true

  tags = {
    Name = "${var.prefix}-subnet-3"
  }
}

resource "aws_subnet" "subnet_4" {
  vpc_id                  = aws_vpc.vpc_1.id
  cidr_block              = "10.0.3.0/24"
  availability_zone       = "${var.region}d"
  map_public_ip_on_launch = true

  tags = {
    Name = "${var.prefix}-subnet-4"
  }
}

resource "aws_internet_gateway" "igw_1" {
  vpc_id = aws_vpc.vpc_1.id

  tags = {
    Name = "${var.prefix}-igw-1"
  }
}

resource "aws_route_table" "rt_1" {
  vpc_id = aws_vpc.vpc_1.id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.igw_1.id
  }

  tags = {
    Name = "${var.prefix}-rt-1"
  }
}

resource "aws_route_table_association" "association_1" {
  subnet_id      = aws_subnet.subnet_1.id
  route_table_id = aws_route_table.rt_1.id
}

resource "aws_route_table_association" "association_2" {
  subnet_id      = aws_subnet.subnet_2.id
  route_table_id = aws_route_table.rt_1.id
}

resource "aws_route_table_association" "association_3" {
  subnet_id      = aws_subnet.subnet_3.id
  route_table_id = aws_route_table.rt_1.id
}

resource "aws_route_table_association" "association_4" {
  subnet_id      = aws_subnet.subnet_4.id
  route_table_id = aws_route_table.rt_1.id
}

resource "aws_security_group" "sg_1" {
  name = "${var.prefix}-sg-1"

  ingress {
    from_port = 0
    to_port   = 0
    protocol  = "all"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port = 0
    to_port   = 0
    protocol  = "all"
    cidr_blocks = ["0.0.0.0/0"]
  }

  vpc_id = aws_vpc.vpc_1.id

  tags = {
    Name = "${var.prefix}-sg-1"
  }
}

#S3 버킷 생성
resource "aws_s3_bucket" "s3_bucket_1" {
  bucket = "${var.prefix}-s3-bucket-1"

  force_destroy = true  # 버킷 안에 객체가 있어도 삭제 가능

  tags = {
    Name = "${var.prefix}-s3-bucket-1"
  }
}

#S3 암호화방식 설정(SSE-S3 방식)
resource "aws_s3_bucket_server_side_encryption_configuration" "s3_encryption" {
  bucket = aws_s3_bucket.s3_bucket_1.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

# SQL 파일을 S3에 업로드
resource "aws_s3_object" "init_data_sql" {
  bucket = aws_s3_bucket.s3_bucket_1.bucket
  key    = "init.sql"
  source = "${path.module}/init/sql/init.sql"
}

resource "aws_s3_object" "law_data_sql" {
  bucket = aws_s3_bucket.s3_bucket_1.bucket
  key    = "lawData.sql"
  source = "${path.module}/init/sql/prod/lawData.sql"
}

resource "aws_s3_object" "precedent_data_sql" {
  bucket = aws_s3_bucket.s3_bucket_1.bucket
  key    = "precedentData.sql"
  source = "${path.module}/init/sql/prod/precedentData.sql"
}

# EC2 설정 시작

# EC2 역할 생성
resource "aws_iam_role" "ec2_role_1" {
  name = "${var.prefix}-ec2-role-1"

  # 이 역할에 대한 신뢰 정책 설정. EC2 서비스가 이 역할을 가정할 수 있도록 설정
  assume_role_policy = <<EOF
  {
    "Version": "2012-10-17",
    "Statement": [
      {
        "Sid": "",
        "Action": "sts:AssumeRole",
        "Principal": {
            "Service": "ec2.amazonaws.com"
        },
        "Effect": "Allow"
      }
    ]
  }
  EOF

  tags = {
    Name = "${var.prefix}-ec2-role-1"
  }
}

# EC2 역할에 AmazonS3FullAccess 정책을 부착
resource "aws_iam_role_policy_attachment" "s3_full_access" {
  role       = aws_iam_role.ec2_role_1.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonS3FullAccess"
}

# EC2 역할에 AmazonEC2RoleforSSM 정책을 부착
resource "aws_iam_role_policy_attachment" "ec2_ssm" {
  role       = aws_iam_role.ec2_role_1.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore"
}

# EC2가 자기 자신에 SendCommand 할 수 있게 허용
resource "aws_iam_role_policy" "ec2_allow_ssm_send_command_self" {
  name = "${var.prefix}-allow-ssm-send-command"
  role = aws_iam_role.ec2_role_1.name

  policy = jsonencode({
    Version = "2012-10-17",
    Statement = [
      {
        Sid    = "SendCommandToThisInstance",
        Effect = "Allow",
        Action = ["ssm:SendCommand"],
        Resource = [
          aws_instance.ec2_1.arn,                                        # 타겟: 이 인스턴스
          "arn:aws:ssm:${var.region}::document/AWS-RunShellScript"       # 사용할 SSM 문서
        ]
      },
      {
        Sid    = "ReadCommandResults",
        Effect = "Allow",
        Action = [
          "ssm:GetCommandInvocation",
          "ssm:ListCommands",
          "ssm:ListCommandInvocations"
        ],
        Resource = "*"
      }
    ]
  })
}

# IAM 인스턴스 프로파일 생성
resource "aws_iam_instance_profile" "instance_profile_1" {
  name = "${var.prefix}-instance-profile-1"
  role = aws_iam_role.ec2_role_1.name

  tags = {
    Name = "${var.prefix}-instance-profile-1"
  }
}

locals {
  ec2_user_data_base = <<-END_OF_FILE
#!/bin/bash
# 가상 메모리 4GB 설정
dd if=/dev/zero of=/swapfile bs=128M count=32
chmod 600 /swapfile
mkswap /swapfile
swapon /swapfile
sh -c 'echo "/swapfile swap swap defaults 0 0" >> /etc/fstab'

# 타임존 설정
timedatectl set-timezone Asia/Seoul

# 환경변수 세팅(/etc/environment)
echo "PASSWORD_1=${var.password_1}" >> /etc/environment
echo "APP_1_DOMAIN=${var.app_1_domain}" >> /etc/environment
echo "APP_1_DB_NAME=${var.app_1_db_name}" >> /etc/environment
echo "GITHUB_ACCESS_TOKEN_1_OWNER=${var.github_access_token_1_owner}" >> /etc/environment
echo "GITHUB_ACCESS_TOKEN_1=${var.github_access_token_1}" >> /etc/environment
source /etc/environment

# 도커 설치 및 실행/활성화
yum install docker -y
systemctl enable docker
systemctl start docker

# 도커 네트워크 생성
docker network create common

# nginx 설치
docker run -d \
  --name npm \
  --restart unless-stopped \
  --network common \
  -p 80:80 \
  -p 443:443 \
  -p 81:81 \
  -e TZ=Asia/Seoul \
  -e INITIAL_ADMIN_EMAIL=admin@npm.com \
  -e INITIAL_ADMIN_PASSWORD=${var.password_1} \
  -v /dockerProjects/npm/volumes/data:/data \
  -v /dockerProjects/npm/volumes/etc/letsencrypt:/etc/letsencrypt \
  jc21/nginx-proxy-manager:latest

# redis 설치
docker run -d \
  --name=redis \
  --restart unless-stopped \
  --network common \
  -p 6379:6379 \
  -e TZ=Asia/Seoul \
  redis --requirepass ${var.password_1}


# SQL 폴더 생성
mkdir -p /home/ec2-user/app/init/sql/prod

# S3에서 SQL 파일 다운로드
aws s3 cp s3://${var.prefix}-s3-bucket-1/init.sql /home/ec2-user/app/init/sql/init.sql
aws s3 cp s3://${var.prefix}-s3-bucket-1/lawData.sql /home/ec2-user/app/init/sql/prod/lawData.sql
aws s3 cp s3://${var.prefix}-s3-bucket-1/precedentData.sql /home/ec2-user/app/init/sql/prod/precedentData.sql
aws s3 cp s3://${var.prefix}-s3-bucket-1/legal_cases.snapshot /home/ec2-user/app/init/qdrant/snapshot/legal_cases.snapshot

# MySQL 설정 폴더 생성 및 UTF8 설정
mkdir -p /dockerProjects/mysql/volumes/etc/mysql/conf.d

cat <<EOF > /dockerProjects/mysql/volumes/etc/mysql/conf.d/charset.cnf
[mysqld]
character-set-server = utf8mb4
collation-server = utf8mb4_general_ci
lower_case_table_names=1
innodb_buffer_pool_size = 1G
innodb_flush_log_at_trx_commit = 2
innodb_flush_method = O_DIRECT
tmp_table_size = 256M
max_heap_table_size = 256M
max_connections = 100

[client]
default-character-set = utf8mb4

[mysql]
default-character-set = utf8mb4
EOF

# mysql 설치
docker run -d \
  --name mysql \
  --restart unless-stopped \
  -v /dockerProjects/mysql/volumes/var/lib/mysql:/var/lib/mysql \
  -v /dockerProjects/mysql/volumes/etc/mysql/conf.d:/etc/mysql/conf.d \
  -v /home/ec2-user/app/init/sql:/docker-entrypoint-initdb.d \
  --network common \
  -p 3306:3306 \
  -e MYSQL_ROOT_PASSWORD=${var.password_1} \
  -e MYSQL_DATABASE=${var.app_1_db_name} \
  -e TZ=Asia/Seoul \
  mysql:latest

# MySQL 컨테이너가 준비될 때까지 대기
echo "MySQL이 기동될 때까지 대기 중..."
until docker exec mysql mysql -uroot -p${var.password_1} -e "SELECT 1" &> /dev/null; do
  echo "MySQL이 아직 준비되지 않음. 5초 후 재시도..."
  sleep 5
done
echo "MySQL이 준비됨. 초기화 스크립트 실행 중..."

docker exec mysql mysql -uroot -p${var.password_1} -e "
    CREATE USER 'lldjlocal'@'127.0.0.1' IDENTIFIED WITH caching_sha2_password BY '1234';
    CREATE USER 'lldjlocal'@'172.18.%.%' IDENTIFIED WITH caching_sha2_password BY '1234';
    CREATE USER 'lldj'@'%' IDENTIFIED WITH caching_sha2_password BY '${var.password_1}';

    GRANT ALL PRIVILEGES ON *.* TO 'lldjlocal'@'127.0.0.1';
    GRANT ALL PRIVILEGES ON *.* TO 'lldjlocal'@'172.18.%.%';
    GRANT ALL PRIVILEGES ON *.* TO 'lldj'@'%';

    FLUSH PRIVILEGES;
"
docker exec -i mysql mysql -uroot -p${var.password_1} ${var.app_1_db_name} < /home/ec2-user/app/init/sql/prod/lawData.sql
docker exec -i mysql mysql -uroot -p${var.password_1} ${var.app_1_db_name} < /home/ec2-user/app/init/sql/prod/precedentData.sql

# Qdrant 설치
mkdir -p /qdrant/snapshots/legal_cases
aws s3 cp s3://${var.prefix}-s3-bucket-1/legal_cases.snapshot /qdrant/snapshots/legal_cases/legal_cases.snapshot

docker run -d \
  --name qdrant \
  --restart unless-stopped \
  --network common \
  -p 6333:6333 \
  -p 6334:6334 \
  -v /qdrant/storage:/qdrant/storage \
  -v /qdrant/snapshots:/qdrant/snapshots \
  qdrant/qdrant

# Qdrant healthcheck 대기
echo "Qdrant가 준비될 때까지 대기 중..."
until curl -fs http://localhost:6333/healthz > /dev/null; do
  echo "Qdrant가 아직 준비되지 않음. 5초 후 재시도..."
  sleep 5
done
echo "Qdrant 준비 됨"

# 볼륨 먼저 만들기(한 번만)
docker volume create ollama-data

# 컨테이너 실행
docker run -d \
  --name ollama \
  --restart unless-stopped \
  --network common \
  -p 11434:11434 \
  -v ollama-data:/root/.ollama \
  --entrypoint /bin/sh \
  --health-cmd 'curl -f http://localhost:11434/api/version || exit 1' \
  --health-interval 10s \
  --health-timeout 5s \
  --health-retries 10 \
  ollama/ollama:latest \
  -c 'ollama serve & sleep 5 && ollama pull daynice/kure-v1:567m && wait'

echo "${var.github_access_token_1}" | docker login ghcr.io -u ${var.github_access_token_1_owner} --password-stdin

END_OF_FILE
}

# 최신 Amazon Linux 2023 AMI 조회 (프리 티어 호환)
data "aws_ami" "latest_amazon_linux" {
  most_recent = true
  owners = ["amazon"]

  filter {
    name = "name"
    values = ["al2023-ami-2023.*-x86_64"]
  }

  filter {
    name = "architecture"
    values = ["x86_64"]
  }

  filter {
    name = "virtualization-type"
    values = ["hvm"]
  }

  filter {
    name = "root-device-type"
    values = ["ebs"]
  }
}

# EC2 인스턴스 생성
resource "aws_instance" "ec2_1" {
  # 사용할 AMI ID
  ami = data.aws_ami.latest_amazon_linux.id
  # EC2 인스턴스 유형
  instance_type = "t3.small"
  # 사용할 서브넷 ID
  subnet_id = aws_subnet.subnet_2.id
  # 적용할 보안 그룹 ID
  vpc_security_group_ids = [aws_security_group.sg_1.id]
  # 퍼블릭 IP 연결 설정
  associate_public_ip_address = true

  # 인스턴스에 IAM 역할 연결
  iam_instance_profile = aws_iam_instance_profile.instance_profile_1.name

  # 인스턴스에 태그 설정
  tags = {
    Name = "${var.prefix}-ec2-1"
  }

  # 루트 볼륨 설정
  root_block_device {
    volume_type = "gp3"
    volume_size = 30 # 볼륨 크기를 12GB로 설정
  }

  user_data = <<-EOF
${local.ec2_user_data_base}
EOF
}

# Elastic IP 생성
resource "aws_eip" "eip_1" {
  domain = "vpc"

  tags = {
    Name = "${var.prefix}-eip-1"
  }

}

# Elastic IP와 EC2 인스턴스 연결
resource "aws_eip_association" "eip_assoc" {
  instance_id   = aws_instance.ec2_1.id
  allocation_id = aws_eip.eip_1.id
}