@echo off

start /D"C:\Program Files" putty.exe -ssh evan@45.79.171.180  -L 5433:localhost:5432 -i C:\.ssh\id_rsa.ppk
