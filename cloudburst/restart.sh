cd dependencies/cloudburst
./scripts/stop-cloudburst-local.sh n
cd ../anna
./scripts/stop-anna-local.sh n
./scripts/start-anna-local.sh n
cd ../cloudburst
./scripts/start-cloudburst-local.sh n

