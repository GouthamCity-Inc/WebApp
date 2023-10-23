packer {
  required_plugins {
    amazon = {
      version = ">= 1.0.0"
      source  = "github.com/hashicorp/amazon"
    }
  }
}

variable aws_region {}
variable ami_prefix {}
variable ssh_username {}
variable subnet_id {}
variable aws_profile {}
variable db_user {}
variable db_password {}
variable instance_type {}
variable ebs_device_name {}
variable ebs_volume_size {}
variable ebs_volume_type {}

locals {
  timestamp       = regex_replace(timestamp(), "[- TZ:]", "")
  demo_account_id = "081235755261"
}


source "amazon-ebs" "debian_ami" {
  ami_name        = "${var.ami_prefix}-${local.timestamp}"
  ami_description = "image for running CSYE 6225 webapp built at ${local.timestamp}"
  ami_regions = [
    "${var.aws_region}",
  ]
  ami_users = ["${local.demo_account_id}"]

  instance_type =          "${var.instance_type}"
  profile       =           "${var.aws_profile}" # remove this before pushing to repo
  region        =          "${var.aws_region}"
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
    device_name           = "${var.ebs_device_name}"
    delete_on_termination = true
    volume_size           = "${var.ebs_volume_size}"
    volume_type           = "${var.ebs_volume_type}"
  }
}

build {
  sources = [
    "source.amazon-ebs.debian_ami",
  ]

  provisioner "file" {
    sources = [
      "./build/libs/gatewayapplication-0.0.1-SNAPSHOT.jar",
      "./users.csv"
    ]
    destination = "/tmp/"
  }

  provisioner "shell" {
    environment_vars = [
      "DEBIAN_FRONTEND=noninteractive",
      "CHECKPOINT_DISABLE=1",
      "DB_USER=${var.db_user}",
      "DB_PASSWORD=${var.db_password}",
    ]
    script = "./scripts/setup.sh"
  }
}