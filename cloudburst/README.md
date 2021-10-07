# Running cloudburst benchmark

## Local mode

### Installation
Make sure you have conda installed. First create a conda environment from the `environment.yml` file and activate it:
```
conda env create -f environment.yml
conda activate cloudburst
```
Clone the cloudburst and anna repositories:
```
mkdir dependencies
cd dependencies
git clone https://github.com/hydro-project/cloudburst.git
git clone https://github.com/hydro-project/anna.git
cd cloudburst
git submodule update --init --recursive
```
For the next step, make sure to have `protoc` and other cloudburst/hydro dependencies installed (tested with protoc version 3.18.0). These can be installed with with the `common/scripts/install-dependencies(-osx).sh` scripts in the cloudburst repo. Building cloudburst: 
```
./scripts/build.sh
```
Install the cloudburst python package
```
pip install .
```
Build anna:
```
cd ../anna
git submodule update --init --recursive
./scripts/build.sh
```
Install anna python package:
```
cd client/python
python setup.py install
cd ../../../../
```

### Running app
Start anna/cloudburst:
```
./start.sh
```
Start flask app:
```
python -m flask run
```
Stop anna/cloudburst:
```
./stop.sh
```