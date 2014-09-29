#!/bin/sh

# Scripts for the QFJ binary distribution

scriptdir=`dirname $0`
qfjhome=$scriptdir/..

CP=$qfjhome/lib/mina-core-1.1.7.jar:$qfjhome/lib/slf4j-api-1.6.3.jar:$qfjhome/lib/slf4j-jdk14-1.6.3.jar:$qfjhome/quickfixj-all-1.5.3.jar:$qfjhome/quickfixj-core-1.5.3.jar:$qfjhome/quickfixj-msg-fix40-1.5.3.jar:$qfjhome/quickfixj-msg-fix41-1.5.3.jar:$qfjhome/quickfixj-msg-fix42-1.5.3.jar:$qfjhome/quickfixj-msg-fix43-1.5.3.jar:$qfjhome/quickfixj-msg-fix44-1.5.3.jar:$qfjhome/quickfixj-msg-fix50-1.5.3.jar:$qfjhome/quickfixj-msg-fixt11-1.5.3.jar:$qfjhome/quickfixj-examples-1.5.3.jar

java -classpath "$CP" quickfix.examples.banzai.Banzai $*

