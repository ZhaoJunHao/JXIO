#!/bin/bash

# Configuring Running Directory
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd $DIR
echo -e "\nThe JXIO Test directory is $DIR\n" 

# Configuring Parameters
TARGET=jx.jar
JX_SO=libjx.so
JAVA_FILES="java/src/com/mellanox/jxio/*.java java/src/com/mellanox/jxio/impl/*.java"

# Clean
rm -f tests/jx.jar
rm -f java/bin/libjx.so
rm -f tests/libjx.so
rm -f java/bin/libxio.so
rm -f tests/libxio.so

## Build Accelio
echo "Build Accelio....(libxio c code)"
cd $DIR
git submodule update --init
cd accelio/ && ./autogen.sh && ./configure --disable-raio-build && make && cd ..
if [[ $? != 0 ]] ; then
    exit 1
fi

## Build JX
echo "Build JXIO... (c code)"
cd $DIR
cd c/ && ./autogen.sh && ./configure && make && cd ..
if [[ $? != 0 ]] ; then
    exit 1
fi

echo "Build JXIO... (java code)"
cd $DIR
mkdir -p java/bin
javac -d java/bin/ $JAVA_FILES
cd java/bin ;jar -cvf $TARGET com ; cd ../..

cp -f java/bin/$TARGET tests/
cp -f c/src/libjx.so java/bin
cp -f c/src/libjx.so tests/
cp -f accelio/src/usr/libxio.so java/bin
cp -f accelio/src/usr/libxio.so tests/


