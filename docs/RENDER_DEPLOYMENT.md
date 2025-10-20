# Render Deployment Guide

## Service Account Configuration

To securely deploy the application with Google Service Account credentials, follow these steps:

### 1. Prepare the Service Account JSON

1. Obtain the service account JSON file from Google Cloud Console
2. Make sure it has the necessary permissions for Google Calendar API

### 2. Configure the Environment Variable in Render

1. Log in to your [Render Dashboard](https://dashboard.render.com/)
2. Navigate to your `calendar-management` service
3. Go to the "Environment" tab
4. Find the `GOOGLE_SERVICE_ACCOUNT_JSON` environment variable
5. Click "Edit" and paste the entire content of your service-account.json file
6. Make sure to include all quotes, brackets, and special characters
7. Click "Save Changes"

Example format of the JSON content:
```json
{
  "type": "service_account",
  "project_id": "your-project-id",
  "private_key_id": "your-private-key-id",
  "private_key": "-----BEGIN PRIVATE KEY-----\nYour private key content with \n for line breaks\n-----END PRIVATE KEY-----\n",
  "client_email": "your-service-account@your-project.iam.gserviceaccount.com",
  "client_id": "your-client-id",
  "auth_uri": "https://accounts.google.com/o/oauth2/auth",
  "token_uri": "https://oauth2.googleapis.com/token",
  "auth_provider_x509_cert_url": "https://www.googleapis.com/oauth2/v1/certs",
  "client_x509_cert_url": "https://www.googleapis.com/robot/v1/metadata/x509/your-service-account%40your-project.iam.gserviceaccount.com",
  "universe_domain": "googleapis.com"
}
```

### 3. Local Development

For local development:
1. Keep the `service-account.json` file in `src/main/resources/`
2. This file is excluded from Git by the `.gitignore` configuration
3. The application will automatically use this file when the environment variable is not set

### 4. Security Notes

- Never commit the service-account.json file to Git
- Regularly rotate your service account keys for better security
- Use Render's environment variable encryption for added security
- Consider setting up IP restrictions for your service account in Google Cloud Console