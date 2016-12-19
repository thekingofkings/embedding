#!/bin/bash
for i in `seq 0 23`;
do
    dot -Tpng case-$i.dot > case$i.png
done
