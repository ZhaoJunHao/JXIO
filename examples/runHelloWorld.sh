#!/bin/bash

# Get Running Directory
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd $DIR

# Exporting LD library
export LD_LIBRARY_PATH=$DIR
echo -e "\nLD library is: $LD_LIBRARY_PATH\n"

# Checks to see if JAVA path is valid
if [ ! -e ${JAVA_HOME} ]; then
	echo -e "\nError: JAVA not found!\n"
	exit 1
fi

# Check arguments
if [ -z $1 ]; then
	 echo -e "$0 ERROR: Missig first parameter. Should be a 'server' or 'client'.\n"
fi
if [ -z $2 ]; then
	 echo -e "$0 ERROR: Missig second parameter. Should be a server IP.\n"
fi
if [ -z $3 ]; then
	 echo -e "$0 ERROR: Missig third parameter. Should be a server port.\n"
fi
# Get server or client side
SIDE=$1
# Get machine IP
IP=$2
# Configure Port
PORT=$3

# Compile
echo -e "\nCompiling JAVA files....\n"
javac -cp "../bin/jxio.jar:../lib/commons-logging.jar" com/mellanox/jxio/helloworld/*.java
if [[ $? != 0 ]] ; then
    exit 1
fi

# Run the tests
export LD_LIBRARY_PATH=$DIR
if ([ $SIDE == server ]); then
APPLICATION_NAME="Server"
APPLICATION="com.mellanox.jxio.helloworld.HelloServer"
elif ([ $SIDE == client ]); then
APPLICATION_NAME="Client"
APPLICATION="com.mellanox.jxio.helloworld.HelloClient"
else
echo -e "$0 ERROR: Missig first parameter. Should be a 'server' or 'client'.\n"
exit 1
fi

echo -e "\nRunning ${APPLICATION_NAME} side test...\n"
java -Dlog4j.configuration=com/mellanox/jxio/log4j.properties.jxio -cp "../bin/jxio.jar:../lib/commons-logging.jar:../lib/log4j-1.2.15.jar:." $APPLICATION $IP $PORT