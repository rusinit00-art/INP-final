@echo off
set JAVA_HOME=C:\Program Files\Java\jdk-21
set JAVA=%JAVA_HOME%\bin\java.exe
set JFX=lib\javafx-sdk-21.0.6\lib

if not exist out\com\auction\client\AuctionClientApp.class (
  call compile.bat
)

"%JAVA%" --module-path %JFX% --add-modules javafx.controls -cp out com.auction.client.AuctionClientApp
