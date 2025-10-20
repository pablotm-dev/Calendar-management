This directory contains the SSL certificate for HTTPS testing.

Before running the tests, you need to:
1. Run the generate-keystore.bat script in this directory to generate a self-signed certificate
2. The script will create a calendar-management.p12 file with the following properties:
   - Alias: calendar-management
   - Password: calendar123
   - Validity: 3650 days (10 years)

The application-test.properties file is configured to use this certificate for HTTPS testing.