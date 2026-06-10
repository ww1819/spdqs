@echo off
chcp 65001 >nul
title 档案工单系统 - 打包

set JAVA_HOME=E:\jdk24
cd /d "%~dp0"

echo 正在打包档案工单系统...
echo JAVA_HOME=%JAVA_HOME%
echo.

mvn clean package

if errorlevel 1 (
    echo.
    echo 打包失败，请检查 Java、Maven 及项目配置。
    pause
    exit /b 1
)

echo.
echo 打包成功: target\spdqs-system-1.0.0.jar
echo 运行 start.bat 开发启动，或 run.bat 运行 jar 包。
pause
