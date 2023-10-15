packer {
  required_plugins {
    amazon = {
      version = ">= 1.0.0"
      source  = "github.com/hashicorp/amazon"
    }
  }
}

variable "aws_region" {
  type    = string
  default = "us-east-1"
}

variable "ami_prefix" {
  type    = string
  default = "csye-6225"
}

variable "access_key" {
  type    = string
  default = ""
}

variable "secret_key" {
  type    = string
  default = ""
}

variable ssh_username {
  type    = string
  default = "admin"
}

variable subnet_id {
  type    = string
  default = "subnet-03f886341a73639f9"
}


locals {
  timestamp       = regex_replace(timestamp(), "[- TZ:]", "")
  demo_account_id = "081235755261"
}


source "amazon-ebs" "debian_ami" {
  ami_name        = "${var.ami_prefix}-${local.timestamp}"
  ami_description = "image for running CSYE 6225 webapp built at ${local.timestamp}"
  ami_regions = [
    "us-east-1",
  ]
  ami_users = ["${local.demo_account_id}"]
  #   access_key      = "${var.access_key}}"
  #   secret_key      = "${var.secret_key}}"
  instance_type = "t2.micro"
  profile       = "dev" # remove this before pushing to repo
  region        = "${var.aws_region}"
  subnet_id     = "${var.subnet_id}"
  source_ami_filter {
    filters = {
      name                = "debian-12-amd64-*"
      root-device-type    = "ebs"
      virtualization-type = "hvm"
    }
    most_recent = true
    owners      = ["amazon"]
  }
  ssh_username = "${var.ssh_username}"

  launch_block_device_mappings {
    device_name           = "/dev/xvda"
    delete_on_termination = true
    volume_size           = 8
    volume_type           = "gp2"
  }
}

build {
  sources = [
    "source.amazon-ebs.debian_ami",
  ]

  provisioner "shell" {
    environment_vars = [
      "DEBIAN_FRONTEND=noninteractive",
      "CHECKPOINT_DISABLE=1"
    ]
    inline = [
      "echo Installing nginx",
      "sudo apt-get update",
      "sudo apt-get upgrade -y",
      "sudo apt-get install nginx -y",
      "sudo apt-get clean",
    ]
  }
}