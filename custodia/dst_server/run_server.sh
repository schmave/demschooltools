set -x

# setup_env.sh is not in source control -- it lives only on the server
. setup_env.sh

export APPENV="production"
export MIGRATEDB="true"

# kill existing java process (if any)
kill $(ps -u custodia | grep java |  grep -o '^[0-9 ]*')

cp overseer-new.jar overseer.jar
nohup java -jar overseer.jar
