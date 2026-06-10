@echo off
chcp 65001 >nul
title 档案工单系统

set JAVA_HOME=E:\jdk24
cd /d "%~dp0"

if not exist "target\spdqs-system-1.0.0.jar" (
    echo 未找到 target\spdqs-system-1.0.0.jar，请先执行打包。
    pause
    exit /b 1
)

echo 正在启动档案工单系统...
echo JAVA_HOME=%JAVA_HOME%
echo 访问地址: http://localhost:8080
echo.

"%JAVA_HOME%\bin\java.exe" -jar target\spdqs-system-1.0.0.jar

echo.
if errorlevel 1 (
    echo 启动失败。常见原因：
    echo   1. 8080 端口被占用
    echo   2. 数据库连接失败（检查 application-local.yml 或 DB_PASSWORD）
    echo   3. 查看上方日志中的具体报错
)
pause
