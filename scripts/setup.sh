#!/bin/bash

echo "#################### Setting up the environment for the webapp... ####################"

# Update package repositories
echo "#################### Updating package repositories...####################"
sudo apt-get update
echo "#################### Package repositories updated. ####################"

echo "#################### Upgrading package repositories... ####################"
sudo apt-get upgrade -y
echo "#################### Package repositories upgraded. ####################"

echo "#################### Setting up env variables ####################"
echo $DB_USER
echo $DB_PASSWORD
export DB_USER="$DB_USER">> ~/.bashrc
export DB_PASSWORD="$DB_PASSWORD" >> ~/.bashrc
source ~/.bashrc


echo "#################### Moving webapp and users.csv from /tmp to /opt... ####################"
sudo mv /tmp/gatewayapplication-0.0.1-SNAPSHOT.jar /opt/gatewayapplication-0.0.1-SNAPSHOT.jar
sudo mv /tmp/users.csv /opt/users.csv
echo "#################### Successfully moved webapp from /tmp to /opt... ####################"


# Install MariaDB server
echo "#################### Installing MariaDB server... ####################"
if sudo DEBIAN_FRONTEND=noninteractive apt-get install -y mariadb-server; then
    echo "#################### MariaDB installation was successful. ####################"
else
    echo "#################### MariaDB installation failed. ####################"
fi

# Set the MySQL root password to 'password'
mysql_root_password="password"

# Run MySQL secure installation with the predefined password
echo "#################### Configuring MariaDB... ####################"
if sudo mysql -e "ALTER USER 'root'@'localhost' IDENTIFIED BY '$mysql_root_password';"; then
    echo "#################### MariaDB configuration was successful. ####################"
else
    echo "#################### MariaDB configuration failed. ####################"
fi

# Install OpenJDK 17 without user prompts
echo "#################### Installing OpenJDK 17... ####################"
sudo apt-get install -y openjdk-17-jdk
echo "#################### OpenJDK 17 installation was successful. ####################"

# Install OpenJDK 17 JRE without user prompts
echo "#################### Installing OpenJDK 17 JRE... ####################"
sudo apt-get install -y openjdk-17-jre
echo "#################### OpenJDK 17 JRE installation was successful. ####################"


echo "#################### setup complete! ####################"