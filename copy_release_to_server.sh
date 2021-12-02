scp target/universal/demschooltools-1.1.zip evan@demschooltools.com:/home/evan/

# This usually works as well and is much faster, though once I had an issue
# where the version on the server was corrupted, seemingly by rsync
# rsync -v -h --progress target/universal/demschooltools-1.1.zip evan@demschooltools.com:/home/evan/
