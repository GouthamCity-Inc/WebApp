#!/bin/bash

echo "#################### Setting up the environment for the webapp... ####################"

# Update package repositories
echo "#################### Updating package repositories...####################"
sudo apt-get update
echo "#################### Package repositories updated. ####################"

echo "#################### Upgrading package repositories... ####################"
sudo apt-get upgrade -y
echo "#################### Package repositories upgraded. ####################"

#echo "#################### Setting up env variables ####################"
#echo export DB_USER="$DB_USER">> /home/admin/.bashrc
#echo export DB_PASSWORD="$DB_PASSWORD" >> /home/admin/.bashrc
#source /home/admin/.bashrc

echo "#################### Setting up csye6225 group and permissions ####################"
sudo groupadd csye6225
sudo useradd -s /bin/false -g csye6225 -d /opt/csye6225 -m csye6225


echo "#################### Moving webapp, users.csv and service file from /tmp to /opt and /etc... ####################"
sudo mv /tmp/gatewayapplication-0.0.1-SNAPSHOT.jar /opt/csye6225/gatewayapplication-0.0.1-SNAPSHOT.jar
sudo mv /tmp/users.csv /opt/csye6225/users.csv
sudo mv /tmp/csye-application.service /etc/systemd/system/
echo "#################### Successfully moved webapp and init file... ####################"


echo "#################### Changing permissons for app binary and users.csv ####################"
sudo chown csye6225:csye6225 /opt/csye6225/gatewayapplication-0.0.1-SNAPSHOT.jar
sudo chown csye6225:csye6225 /opt/csye6225/users.csv
sudo touch /opt/csye6225/application.properties
chown admin:admin /opt/csye6225/application.properties


# Install OpenJDK 17 without user prompts
echo "#################### Installing OpenJDK 17... ####################"
sudo apt-get install -y openjdk-17-jdk
echo "#################### OpenJDK 17 installation was successful. ####################"

# Install OpenJDK 17 JRE without user prompts
echo "#################### Installing OpenJDK 17 JRE... ####################"
sudo apt-get install -y openjdk-17-jre
echo "#################### OpenJDK 17 JRE installation was successful. ####################"


echo "#################### Enabling application via systemd...  ####################"
sudo systemctl daemon-reload
sudo systemctl enable csye-application


echo "#################### setup complete! ####################"