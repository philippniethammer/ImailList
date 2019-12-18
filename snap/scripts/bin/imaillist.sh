#!/bin/sh

export HOME=$SNAP_USER_DATA

java -jar -Duser.dir=$SNAP_DATA $SNAP/jar/imailList.jar # | while read line; do date=`date`; echo $date " " $line; done
