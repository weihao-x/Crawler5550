# Crawler, for CIS 5550 project

## Prerequisite
This is a Java Maven project. To install Java and Maven
```
sudo apt-get update
sudo apt install default-jre
sudo apt install maven
```

Install chrome and chrome driver
```
wget --no-verbose -O ./chrome.deb https://dl.google.com/linux/chrome/deb/pool/main/g/google-chrome-stable/google-chrome-stable_99.0.4844.84-1_amd64.deb
sudo apt install -y ./chrome.deb
rm ./chrome.deb
```
Notice that the chromedriver binary needs to be excutable.

To build the project
```
mvn clean install -DskipTests
```

## Master
To start the master node
```
sudo mvn exec:java@master -DskipTests 2>&1 | tee debug.out
```

## Worker
To start the worker node
```
sudo mvn exec:java@worker -DskipTests 2>&1 | tee debug.out
```
