@echo off
echo Building Sit CLI...
call gradlew.bat clean bootJar

if errorlevel 1 (
    echo Build failed!
    pause
    exit /b %errorlevel%
)

echo Creating distribution...
if not exist dist mkdir dist

copy "build\libs\SIT-0.0.1-SNAPSHOT.jar" "dist\sit.jar" >nul

echo Creating portable launcher...
(
echo @echo off
echo java -jar "%%~dp0sit.jar" %%*
) > dist\sit.bat

echo.
echo ========================================================
echo  Distribution created in 'dist' folder!
echo ========================================================
echo.
echo To use 'sit' globally:
echo 1. Copy the 'dist' folder to a permanent location (e.g. C:\Program Files\SitCLI)
echo 2. Add that folder path to your System Environment Variables 'Path'
echo.
echo Or simply copy the files inside 'dist' to your other project folder.
echo.
pause
