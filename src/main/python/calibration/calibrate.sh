#!/bin/bash
#$ -l h_rt=86400
#$ -o ./logfile_$JOB_NAME.log
#$ -j y
#$ -m a
#$ -M mueller@vsp.tu-berlin.de
#$ -cwd
#$ -pe mp 5
#$ -l mem_free=25G

date
hostname

command="python calibrate.py 20"

echo ""
echo "command is $command"

echo ""
echo "using alternative java"
module add java/11
java -version

# Script starts 5 processes in parallel
for i in $(seq 0 4); do
   $command &
   sleep 20
done

wait