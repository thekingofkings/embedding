echo "### 1. Generate DGE embedding"

Spatial=$1
Year=2013
if [ -f miscs/taxi-deepwalk-CA-$Spatial.vec ]; then
    echo "DGE file with setting $Spatial exists."
else
    echo "Generating embedding file ..."
	cd embedding
	mvn exec:java -Dexec.mainClass="embedding.DeepWalk" -Dexec.args="CA $Spatial $Year"	
fi


echo "### 2. Generate LINE embedding"

if [ ! -f ~/workspace/embedding/miscs/taxi-CA-h23.vec ]; then
	cd ~/workspace/LINE/linux
	./train-CA.sh
else
	echo "LINE embedding exists"
fi


echo "### 3. Generate features in python pickle file"

cd ~/workspace/embedding/python
python flowFeatureGeneration_CA.py $Spatial


echo "### 4 copy file into chicago-crime project"
cd ~/workspace/embedding/miscs
cp taxi-CA-static.matrix ~/workspace/chicago-crime/python/
cp CAflowFeatures.pickle ~/workspace/chicago-crime/python/multi-view-learning/


echo "### 5 run regression to predict crime"

cd ~/workspace/chicago-crime/python/multi-view-learning
python multi_view_prediction.py $Spatial
