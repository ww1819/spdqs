@echo off
chcp 65001 >nul
title 档案工单系统

set JAVA_HOME=D:\jdk24
cd /d "%~dp0"

echo 正在启动档案工单系统...
echo JAVA_HOME=%JAVA_HOME%
echo 访问地址: http://localhost:8080
echo.

mvn spring-boot:run

if errorlevel 1 (
    echo.
    echo 启动失败，请检查 Java、Maven 及数据库配置。
    pause
)
