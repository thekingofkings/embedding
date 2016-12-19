#!/bin/bash
for i in `seq 0 23`;
do
    dot -Tpdf case-$i.dot > case$i.pdf
done
