@echo off

REM using windows powershell to unzip the jre.zip
powershell.exe -nologo -noprofile -command "& { Add-Type -A 'System.IO.Compression.FileSystem'; [IO.Compression.ZipFile]::ExtractToDirectory('jre.zip', '.'); }"
del jre.zip 