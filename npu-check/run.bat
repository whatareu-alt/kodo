@echo off
title NPU Check — Neural Processing Unit Detection
echo.
echo   ====================================
echo       NPU Check — Scanning System...
echo   ====================================
echo.
powershell -ExecutionPolicy Bypass -NoProfile -File "%~dp0npu_check.ps1"
echo.
pause
