#!/bin/sh

# check args
if [ $# -ne 3 ]; then
    echo "usage: $0 jar path-to-waves out_file"
    exit
fi

# var def
jar=$1
path=$2
files=($2/*.wav)
out=$3
left=`ls $path | wc -l`
echo "$left files to process"

# clear any previous file
rm $out

begin=0
slice=100
while [ $left -gt 0 ]; do
    block=${files[@]:$begin:$slice}
    java -jar $jar $block >> $out
    left=`expr $left - $slice`
    echo "$left files left"
    begin=`expr $begin + $slice`
done

exit