@echo off
set JAVA_HOME=C:\Program Files\Java\jdk-21
set JAVA=%JAVA_HOME%\bin\java.exe

if not exist out\com\auction\server\AuctionServer.class (
  call compile.bat
)

"%JAVA%" -cp out com.auction.server.AuctionServer
