@echo off
set SOURCE_ICON=e:\Code\Script\Fuel_consumption_record\icon.png
set RES_DIR=e:\Code\Script\Fuel_consumption_record\app\src\main\res

echo Copying icon to mipmap-mdpi...
copy "%SOURCE_ICON%" "%RES_DIR%\mipmap-mdpi\ic_launcher.png" /Y
copy "%SOURCE_ICON%" "%RES_DIR%\mipmap-mdpi\ic_launcher_round.png" /Y

echo Copying icon to mipmap-hdpi...
copy "%SOURCE_ICON%" "%RES_DIR%\mipmap-hdpi\ic_launcher.png" /Y
copy "%SOURCE_ICON%" "%RES_DIR%\mipmap-hdpi\ic_launcher_round.png" /Y

echo Copying icon to mipmap-xhdpi...
copy "%SOURCE_ICON%" "%RES_DIR%\mipmap-xhdpi\ic_launcher.png" /Y
copy "%SOURCE_ICON%" "%RES_DIR%\mipmap-xhdpi\ic_launcher_round.png" /Y

echo Copying icon to mipmap-xxhdpi...
copy "%SOURCE_ICON%" "%RES_DIR%\mipmap-xxhdpi\ic_launcher.png" /Y
copy "%SOURCE_ICON%" "%RES_DIR%\mipmap-xxhdpi\ic_launcher_round.png" /Y

echo Copying icon to mipmap-xxxhdpi...
copy "%SOURCE_ICON%" "%RES_DIR%\mipmap-xxxhdpi\ic_launcher.png" /Y
copy "%SOURCE_ICON%" "%RES_DIR%\mipmap-xxxhdpi\ic_launcher_round.png" /Y

echo Copying icon to mipmap-anydpi-v26...
copy "%SOURCE_ICON%" "%RES_DIR%\mipmap-anydpi-v26\ic_launcher.png" /Y
copy "%SOURCE_ICON%" "%RES_DIR%\mipmap-anydpi-v26\ic_launcher_round.png" /Y

echo Icon replacement completed!
