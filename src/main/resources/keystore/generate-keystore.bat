@echo off
echo Generating self-signed certificate for development...
keytool -genkeypair -alias calendar-management -keyalg RSA -keysize 2048 -storetype PKCS12 -keystore calendar-management.p12 -validity 3650 -dname "CN=localhost, OU=Calendar Management, O=Calendar Management, L=Local, ST=Local, C=BR" -storepass calendar123 -keypass calendar123
echo Certificate generated successfully.