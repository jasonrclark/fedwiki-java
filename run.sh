# run with standard input as commands
# usage: sh run.sh [site]

java \
  -cp jackson-core-2.14.2.jar:jackson-annotations-2.14.2.jar:jackson-databind-2.14.2.jar \
  Main.java $1
