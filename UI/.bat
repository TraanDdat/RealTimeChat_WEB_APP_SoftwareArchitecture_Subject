@echo off

echo ==============================
echo Creating mini_zalo project...
echo ==============================

REM Create folders
mkdir mini_zalo
mkdir mini_zalo\backend
mkdir mini_zalo\backend\src
mkdir mini_zalo\backend\src\main
mkdir mini_zalo\backend\src\main\java
mkdir mini_zalo\backend\src\main\java\com
mkdir mini_zalo\backend\src\main\java\com\example
mkdir mini_zalo\backend\src\main\java\com\example\demo
mkdir mini_zalo\frontend

REM Create Java files
type nul > mini_zalo\backend\src\main\java\com\example\demo\DemoApplication.java
type nul > mini_zalo\backend\src\main\java\com\example\demo\User.java
type nul > mini_zalo\backend\src\main\java\com\example\demo\UserController.java

REM Create HTML file
type nul > mini_zalo\frontend\index.html

echo.
echo ==============================
echo Project created successfully!
echo ==============================
echo.

tree mini_zalo /F

pause