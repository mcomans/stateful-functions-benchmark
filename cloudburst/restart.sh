cd ../../cloudburst
./scripts/stop-cloudburst-local.sh
cd ../cloudburst-deps/anna
./scripts/stop-anna-local.sh
./scripts/start-anna-local.sh
cd ../../cloudburst
./scripts/start-cloudburst-local.sh

