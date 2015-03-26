#!/bin/sh

while true; do
    read -p "Do you really wish to push to production? [y/n]: " yn
    case $yn in
        [Yy]* ) git push $1 master; break;;
        [Nn]* ) exit;;
        * ) echo "Please answer yes or no.";;
    esac
done
