This directory contains the SSL certificate for HTTPS support.

In a production environment, you would need to:
1. Generate a proper SSL certificate using keytool or another certificate management tool
2. Place the certificate in this directory
3. Update the application.properties file with the correct certificate details

For development, you can generate a self-signed certificate using the following command:
keytool -genkeypair -alias calendar-management -keyalg RSA -keysize 2048 -storetype PKCS12 -keystore calendar-management.p12 -validity 3650 -dname "CN=localhost, OU=Calendar Management, O=Calendar Management, L=Local, ST=Local, C=BR" -storepass calendar123 -keypass calendar123