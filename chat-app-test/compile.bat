@echo off
set JAVA_HOME=C:\Program Files\Java\jdk-21
set JAVAC=%JAVA_HOME%\bin\javac.exe
set JFX=lib\javafx-sdk-21.0.6\lib

if not exist out mkdir out

"%JAVAC%" -d out ^
  src\main\java\com\auction\common\Protocol.java ^
  src\main\java\com\auction\server\AuctionState.java ^
  src\main\java\com\auction\server\ClientHandler.java ^
  src\main\java\com\auction\server\AuctionServer.java

if errorlevel 1 exit /b 1

"%JAVAC%" --module-path %JFX% --add-modules javafx.controls -d out -cp out ^
  src\main\java\com\auction\client\AuctionConnection.java ^
  src\main\java\com\auction\client\LoginDialog.java ^
  src\main\java\com\auction\client\AuctionClientApp.java

if errorlevel 1 exit /b 1

echo Build successful.
