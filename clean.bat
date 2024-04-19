@echo off
echo Cleaning...
cd src
del *.class
del *.ctxt
del *.config
rmdir /s /q apps

pause