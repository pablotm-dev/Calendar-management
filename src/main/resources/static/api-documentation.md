# API Documentation

This document provides comprehensive documentation for all endpoints related to projects, tasks, clients, and events (apontamentos) in the Calendar Management system.

## Table of Contents
- [Projects](#projects)
- [Tasks](#tasks)
- [Clients](#clients)
- [Events](#events)
- [Reports](#reports)

## Projects

### List All Projects

**Endpoint:** `GET /api/projetos`

**Description:** Retrieves a list of all projects.

**Curl Command:**
```bash
curl -X GET http://localhost:8081/api/projetos -H "Content-Type: application/json"
```

**Example Response:**
```json
[
  {
    "id": 1,
    "nomeProjeto": "Projeto A",
    "idCliente": 1
  },
  {
    "id": 2,
    "nomeProjeto": "Projeto B",
    "idCliente": 2
  }
]
```

### Get Project by ID

**Endpoint:** `GET /api/projetos/{id}`

**Description:** Retrieves a specific project by its ID.

**Curl Command:**
```bash
curl -X GET http://localhost:8081/api/projetos/1 -H "Content-Type: application/json"
```

**Example Response:**
```json
{
  "id": 1,
  "nomeProjeto": "Projeto A",
  "idCliente": 1
}
```

### List Projects by Client ID

**Endpoint:** `GET /api/projetos/cliente/{clienteId}`

**Description:** Retrieves all projects associated with a specific client.

**Curl Command:**
```bash
curl -X GET http://localhost:8081/api/projetos/cliente/1 -H "Content-Type: application/json"
```

**Example Response:**
```json
[
  {
    "id": 1,
    "nomeProjeto": "Projeto A",
    "idCliente": 1
  },
  {
    "id": 3,
    "nomeProjeto": "Projeto C",
    "idCliente": 1
  }
]
```

### Create Project

**Endpoint:** `POST /api/projetos`

**Description:** Creates a new project.

**Curl Command:**
```bash
curl -X POST http://localhost:8081/api/projetos -H "Content-Type: application/json" -d "{\"nomeProjeto\":\"Novo Projeto\",\"idCliente\":1}"
```

**Example Request Body:**
```json
{
  "nomeProjeto": "Novo Projeto",
  "idCliente": 1
}
```

**Example Response:**
```json
{
  "id": 4,
  "nomeProjeto": "Novo Projeto",
  "idCliente": 1
}
```

### Update Project

**Endpoint:** `PUT /api/projetos/{id}`

**Description:** Updates an existing project.

**Curl Command:**
```bash
curl -X PUT http://localhost:8081/api/projetos/4 -H "Content-Type: application/json" -d "{\"nomeProjeto\":\"Projeto Atualizado\",\"idCliente\":2}"
```

**Example Request Body:**
```json
{
  "nomeProjeto": "Projeto Atualizado",
  "idCliente": 2
}
```

**Example Response:**
```json
{
  "id": 4,
  "nomeProjeto": "Projeto Atualizado",
  "idCliente": 2
}
```

### Delete Project

**Endpoint:** `DELETE /api/projetos/{id}`

**Description:** Deletes a project.

**Curl Command:**
```bash
curl -X DELETE http://localhost:8081/api/projetos/4 -H "Content-Type: application/json"
```

**Example Response:**
```
204 No Content
```

## Tasks

### List All Tasks

**Endpoint:** `GET /api/tasks`

**Description:** Retrieves a list of all tasks.

**Curl Command:**
```bash
curl -X GET http://localhost:8081/api/tasks -H "Content-Type: application/json"
```

**Example Response:**
```json
[
  {
    "id": 1,
    "nomeTask": "Task A",
    "descricaoTask": "Descrição da Task A",
    "idProjeto": 1,
    "tagTask": "TASKA",
    "dataInicio": "2023-01-01T00:00:00",
    "dataFim": "2023-01-31T23:59:59",
    "isAtivo": true
  },
  {
    "id": 2,
    "nomeTask": "Task B",
    "descricaoTask": "Descrição da Task B",
    "idProjeto": 2,
    "tagTask": "TASKB",
    "dataInicio": "2023-02-01T00:00:00",
    "dataFim": "2023-02-28T23:59:59",
    "isAtivo": true
  }
]
```

### Get Task by ID

**Endpoint:** `GET /api/tasks/{id}`

**Description:** Retrieves a specific task by its ID.

**Curl Command:**
```bash
curl -X GET http://localhost:8081/api/tasks/1 -H "Content-Type: application/json"
```

**Example Response:**
```json
{
  "id": 1,
  "nomeTask": "Task A",
  "descricaoTask": "Descrição da Task A",
  "idProjeto": 1,
  "tagTask": "TASKA",
  "dataInicio": "2023-01-01T00:00:00",
  "dataFim": "2023-01-31T23:59:59",
  "isAtivo": true
}
```

### Get Task by Tag

**Endpoint:** `GET /api/tasks/tag/{tagTask}`

**Description:** Retrieves a specific task by its tag.

**Curl Command:**
```bash
curl -X GET http://localhost:8081/api/tasks/tag/TASKA -H "Content-Type: application/json"
```

**Example Response:**
```json
{
  "id": 1,
  "nomeTask": "Task A",
  "descricaoTask": "Descrição da Task A",
  "idProjeto": 1,
  "tagTask": "TASKA",
  "dataInicio": "2023-01-01T00:00:00",
  "dataFim": "2023-01-31T23:59:59",
  "isAtivo": true
}
```

### List Tasks by Project ID

**Endpoint:** `GET /api/tasks/projeto/{projetoId}`

**Description:** Retrieves all tasks associated with a specific project.

**Curl Command:**
```bash
curl -X GET http://localhost:8081/api/tasks/projeto/1 -H "Content-Type: application/json"
```

**Example Response:**
```json
[
  {
    "id": 1,
    "nomeTask": "Task A",
    "descricaoTask": "Descrição da Task A",
    "idProjeto": 1,
    "tagTask": "TASKA",
    "dataInicio": "2023-01-01T00:00:00",
    "dataFim": "2023-01-31T23:59:59",
    "isAtivo": true
  },
  {
    "id": 3,
    "nomeTask": "Task C",
    "descricaoTask": "Descrição da Task C",
    "idProjeto": 1,
    "tagTask": "TASKC",
    "dataInicio": "2023-03-01T00:00:00",
    "dataFim": "2023-03-31T23:59:59",
    "isAtivo": true
  }
]
```

### Create Task

**Endpoint:** `POST /api/tasks`

**Description:** Creates a new task.

**Curl Command:**
```bash
curl -X POST http://localhost:8081/api/tasks -H "Content-Type: application/json" -d "{\"nomeTask\":\"Nova Task\",\"descricaoTask\":\"Descrição da Nova Task\",\"idProjeto\":1,\"tagTask\":\"NEWTASK\",\"dataInicio\":\"2023-04-01T00:00:00\",\"dataFim\":\"2023-04-30T23:59:59\",\"isAtivo\":true}"
```

**Example Request Body:**
```json
{
  "nomeTask": "Nova Task",
  "descricaoTask": "Descrição da Nova Task",
  "idProjeto": 1,
  "tagTask": "NEWTASK",
  "dataInicio": "2023-04-01T00:00:00",
  "dataFim": "2023-04-30T23:59:59",
  "isAtivo": true
}
```

**Example Response:**
```json
{
  "id": 4,
  "nomeTask": "Nova Task",
  "descricaoTask": "Descrição da Nova Task",
  "idProjeto": 1,
  "tagTask": "NEWTASK",
  "dataInicio": "2023-04-01T00:00:00",
  "dataFim": "2023-04-30T23:59:59",
  "isAtivo": true
}
```

### Update Task

**Endpoint:** `PUT /api/tasks/{id}`

**Description:** Updates an existing task.

**Curl Command:**
```bash
curl -X PUT http://localhost:8081/api/tasks/4 -H "Content-Type: application/json" -d "{\"nomeTask\":\"Task Atualizada\",\"descricaoTask\":\"Descrição Atualizada\",\"idProjeto\":2,\"tagTask\":\"UPDTASK\",\"dataInicio\":\"2023-05-01T00:00:00\",\"dataFim\":\"2023-05-31T23:59:59\",\"isAtivo\":true}"
```

**Example Request Body:**
```json
{
  "nomeTask": "Task Atualizada",
  "descricaoTask": "Descrição Atualizada",
  "idProjeto": 2,
  "tagTask": "UPDTASK",
  "dataInicio": "2023-05-01T00:00:00",
  "dataFim": "2023-05-31T23:59:59",
  "isAtivo": true
}
```

**Example Response:**
```json
{
  "id": 4,
  "nomeTask": "Task Atualizada",
  "descricaoTask": "Descrição Atualizada",
  "idProjeto": 2,
  "tagTask": "UPDTASK",
  "dataInicio": "2023-05-01T00:00:00",
  "dataFim": "2023-05-31T23:59:59",
  "isAtivo": true
}
```

### Delete Task

**Endpoint:** `DELETE /api/tasks/{id}`

**Description:** Deletes a task.

**Curl Command:**
```bash
curl -X DELETE http://localhost:8081/api/tasks/4 -H "Content-Type: application/json"
```

**Example Response:**
```
204 No Content
```

## Clients

### List All Clients

**Endpoint:** `GET /api/clientes`

**Description:** Retrieves a list of all clients.

**Curl Command:**
```bash
curl -X GET http://localhost:8081/api/clientes -H "Content-Type: application/json"
```

**Example Response:**
```json
[
  {
    "id": 1,
    "nomeCliente": "Cliente A"
  },
  {
    "id": 2,
    "nomeCliente": "Cliente B"
  }
]
```

### Get Client by ID

**Endpoint:** `GET /api/clientes/{id}`

**Description:** Retrieves a specific client by its ID.

**Curl Command:**
```bash
curl -X GET http://localhost:8081/api/clientes/1 -H "Content-Type: application/json"
```

**Example Response:**
```json
{
  "id": 1,
  "nomeCliente": "Cliente A"
}
```

### Create Client

**Endpoint:** `POST /api/clientes`

**Description:** Creates a new client.

**Curl Command:**
```bash
curl -X POST http://localhost:8081/api/clientes -H "Content-Type: application/json" -d "{\"nomeCliente\":\"Novo Cliente\"}"
```

**Example Request Body:**
```json
{
  "nomeCliente": "Novo Cliente"
}
```

**Example Response:**
```json
{
  "id": 3,
  "nomeCliente": "Novo Cliente"
}
```

### Update Client

**Endpoint:** `PUT /api/clientes/{id}`

**Description:** Updates an existing client.

**Curl Command:**
```bash
curl -X PUT http://localhost:8081/api/clientes/3 -H "Content-Type: application/json" -d "{\"nomeCliente\":\"Cliente Atualizado\"}"
```

**Example Request Body:**
```json
{
  "nomeCliente": "Cliente Atualizado"
}
```

**Example Response:**
```json
{
  "id": 3,
  "nomeCliente": "Cliente Atualizado"
}
```

### Delete Client

**Endpoint:** `DELETE /api/clientes/{id}`

**Description:** Deletes a client.

**Curl Command:**
```bash
curl -X DELETE http://localhost:8081/api/clientes/3 -H "Content-Type: application/json"
```

**Example Response:**
```
204 No Content
```

## Events

Events (apontamentos) in this system are primarily managed through Google Calendar integration. The system synchronizes events from Google Calendar and associates them with tasks based on tags.

### List Calendars

**Endpoint:** `GET /calendars`

**Description:** Retrieves a list of all calendars available to the authenticated user.

**Curl Command:**
```bash
curl -X GET http://localhost:8081/calendars -H "Content-Type: application/json" -H "Authorization: Bearer {access_token}"
```

**Example Response:**
```json
[
  {
    "id": "primary",
    "summary": "user@example.com",
    "description": "Main Calendar"
  },
  {
    "id": "calendar_id_2",
    "summary": "Work Calendar",
    "description": "Work-related events"
  }
]
```

### List Events from Calendar

**Endpoint:** `GET /calendars/{calendarId}/events`

**Description:** Retrieves events from a specific calendar within an optional time range.

**Parameters:**
- `timeMin` (optional): Start time for the range (ISO-8601 format)
- `timeMax` (optional): End time for the range (ISO-8601 format)

**Curl Command:**
```bash
curl -X GET "http://localhost:8081/calendars/primary/events?timeMin=2023-01-01T00:00:00Z&timeMax=2023-12-31T23:59:59Z" -H "Content-Type: application/json" -H "Authorization: Bearer {access_token}"
```

**Example Response:**
```json
[
  {
    "id": "event_id_1",
    "summary": "[TASKA] Meeting with Client",
    "organizerEmail": "organizer@example.com",
    "htmlLink": "https://calendar.google.com/calendar/event?eid=...",
    "start": "2023-01-15T10:00:00Z",
    "end": "2023-01-15T11:00:00Z",
    "location": "Conference Room A"
  },
  {
    "id": "event_id_2",
    "summary": "[TASKB] Project Planning",
    "organizerEmail": "organizer@example.com",
    "htmlLink": "https://calendar.google.com/calendar/event?eid=...",
    "start": "2023-01-16T14:00:00Z",
    "end": "2023-01-16T16:00:00Z",
    "location": "Office"
  }
]
```

### Manual Sync for User

**Endpoint:** `POST /admin/sync/user`

**Description:** Manually triggers synchronization of calendar events for a specific user.

**Parameters:**
- `email` (required): Email of the user
- `reset` (optional): Whether to reset the sync state (default: false)

**Curl Command:**
```bash
curl -X POST "http://localhost:8081/admin/sync/user?email=user@example.com&reset=false" -H "Content-Type: application/json"
```

**Example Response:**
```json
{
  "email": "user@example.com",
  "calendarId": "primary",
  "reset": false,
  "status": "OK",
  "syncedAt": "2023-06-01T12:34:56.789Z"
}
```

### Manual Sync for All Users

**Endpoint:** `POST /admin/sync/all`

**Description:** Manually triggers synchronization of calendar events for all users defined in the application configuration.

**Parameters:**
- `reset` (optional): Whether to reset the sync state (default: false)

**Curl Command:**
```bash
curl -X POST "http://localhost:8081/admin/sync/all?reset=false" -H "Content-Type: application/json"
```

**Example Response:**
```json
[
  {
    "email": "user1@example.com",
    "calendarId": "primary",
    "reset": false,
    "status": "OK",
    "syncedAt": "2023-06-01T12:34:56.789Z"
  },
  {
    "email": "user2@example.com",
    "calendarId": "primary",
    "reset": false,
    "status": "OK",
    "syncedAt": "2023-06-01T12:34:57.123Z"
  }
]
```

## Reports

Reports provide insights into collaborator and client activities, time tracking, and project progress.

### List All Collaborator Reports

**Endpoint:** `GET /api/reports/collaborators`

**Description:** Retrieves reports for all collaborators, with optional filtering.

**Parameters:**
- `startDate` (optional): Start date for the report period (ISO-8601 format)
- `endDate` (optional): End date for the report period (ISO-8601 format)
- `periodType` (optional): Period type (WEEK, MONTH, QUARTER, YEAR)
- `collaboratorEmails` (optional): List of collaborator emails to filter by
- `projectIds` (optional): List of project IDs to filter by
- `taskIds` (optional): List of task IDs to filter by

**Curl Command:**
```bash
curl --location --request GET 'http://localhost:8081/api/reports/collaborators' \
--header 'Content-Type: application/json'
```

**Example Request Body (for POST requests):**
```json
{
  "nomeCliente": "Cliente Teste"
}
```

**Example Response:**
```json
[
  {
    "email": "user@example.com",
    "name": "User Name",
    "totalHours": 40.5,
    "projectHours": [
      {
        "projectId": 1,
        "projectName": "Project A",
        "hours": 25.0,
        "taskHours": [
          {
            "taskId": 1,
            "taskName": "Task 1",
            "hours": 15.0
          },
          {
            "taskId": 2,
            "taskName": "Task 2",
            "hours": 10.0
          }
        ]
      }
    ],
    "productivityAnalysis": {
      "averageHoursPerDay": 8.1,
      "mostProductiveDay": "MONDAY",
      "leastProductiveDay": "FRIDAY"
    }
  }
]
```

### Get Collaborator Report

**Endpoint:** `GET /api/reports/collaborators/{email}`

**Description:** Retrieves a report for a specific collaborator.

**Parameters:**
- `email` (path): Email of the collaborator
- `startDate` (optional): Start date for the report period (ISO-8601 format)
- `endDate` (optional): End date for the report period (ISO-8601 format)
- `periodType` (optional): Period type (WEEK, MONTH, QUARTER, YEAR)
- `projectIds` (optional): List of project IDs to filter by
- `taskIds` (optional): List of task IDs to filter by

**Curl Command:**
```bash
curl --location --request GET 'http://localhost:8081/api/reports/collaborators/user@example.com' \
--header 'Content-Type: application/json'
```

### Get Collaborator Excel Report

**Endpoint:** `GET /api/reports/collaborators/{email}/excel`

**Description:** Generates and downloads an Excel spreadsheet with detailed hours report for a specific collaborator. The spreadsheet includes the collaborator's email, total hours in the month, hours per project, hours per task, and all calendar events with their descriptions and hours.

**Parameters:**
- `email` (path): Email of the collaborator
- `startDate` (optional): Start date for the report period (ISO-8601 format)
- `endDate` (optional): End date for the report period (ISO-8601 format)

**Curl Command:**
```bash
curl --location --request GET 'http://localhost:8081/api/reports/collaborators/user@example.com/excel' \
--output 'relatorio_horas.xlsx'
```

**Response:**
Excel file (.xlsx) with the following sheets:
- Summary: Collaborator email and total hours
- Projects: Hours worked per project
- Tasks: Hours worked per task
- Events: All calendar events with start/end times and hours

**Example Response:**
```json
{
  "email": "user@example.com",
  "name": "User Name",
  "totalHours": 40.5,
  "projectHours": [
    {
      "projectId": 1,
      "projectName": "Project A",
      "hours": 25.0,
      "taskHours": [
        {
          "taskId": 1,
          "taskName": "Task 1",
          "hours": 15.0
        },
        {
          "taskId": 2,
          "taskName": "Task 2",
          "hours": 10.0
        }
      ]
    }
  ],
  "productivityAnalysis": {
    "averageHoursPerDay": 8.1,
    "mostProductiveDay": "MONDAY",
    "leastProductiveDay": "FRIDAY"
  }
}
```

### List All Client Reports

**Endpoint:** `GET /api/reports/clients`

**Description:** Retrieves reports for all clients, with optional filtering.

**Parameters:**
- `startDate` (optional): Start date for the report period (ISO-8601 format)
- `endDate` (optional): End date for the report period (ISO-8601 format)
- `periodType` (optional): Period type (WEEK, MONTH, QUARTER, YEAR)
- `collaboratorEmails` (optional): List of collaborator emails to filter by
- `projectIds` (optional): List of project IDs to filter by
- `taskIds` (optional): List of task IDs to filter by

**Curl Command:**
```bash
curl --location --request GET 'http://localhost:8081/api/reports/clients' \
--header 'Content-Type: application/json'
```

**Example Response:**
```json
[
  {
    "clientId": 1,
    "clientName": "Client A",
    "totalHours": 120.5,
    "projectReports": [
      {
        "projectId": 1,
        "projectName": "Project X",
        "totalHours": 80.0,
        "taskReports": [
          {
            "taskId": 1,
            "taskName": "Task Alpha",
            "totalHours": 45.0
          }
        ]
      }
    ],
    "collaboratorHours": [
      {
        "email": "dev1@example.com",
        "name": "Developer One",
        "hours": 60.0
      }
    ]
  }
]
```

### Get Client Report

**Endpoint:** `GET /api/reports/clients/{clientId}`

**Description:** Retrieves a report for a specific client.

**Parameters:**
- `clientId` (path): ID of the client
- `startDate` (optional): Start date for the report period (ISO-8601 format)
- `endDate` (optional): End date for the report period (ISO-8601 format)
- `periodType` (optional): Period type (WEEK, MONTH, QUARTER, YEAR)
- `collaboratorEmails` (optional): List of collaborator emails to filter by
- `projectIds` (optional): List of project IDs to filter by
- `taskIds` (optional): List of task IDs to filter by

**Curl Command:**
```bash
curl --location --request GET 'http://localhost:8081/api/reports/clients/1' \
--header 'Content-Type: application/json'
```

**Example Response:**
```json
{
  "clientId": 1,
  "clientName": "Client A",
  "totalHours": 120.5,
  "projectReports": [
    {
      "projectId": 1,
      "projectName": "Project X",
      "totalHours": 80.0,
      "taskReports": [
        {
          "taskId": 1,
          "taskName": "Task Alpha",
          "totalHours": 45.0
        }
      ]
    }
  ],
  "collaboratorHours": [
    {
      "email": "dev1@example.com",
      "name": "Developer One",
      "hours": 60.0
    }
  ]
}
```

### Get Client Excel Report

**Endpoint:** `GET /api/reports/clients/{clientId}/excel`

**Description:** Generates and downloads an Excel spreadsheet with detailed hours report for a specific client. The spreadsheet includes the client's name, total hours, hours per project, hours per task, and hours per collaborator.

**Parameters:**
- `clientId` (path): ID of the client
- `startDate` (optional): Start date for the report period (ISO-8601 format)
- `endDate` (optional): End date for the report period (ISO-8601 format)

**Curl Command:**
```bash
curl --location --request GET 'http://localhost:8081/api/reports/clients/1/excel' \
--output 'relatorio_cliente.xlsx'
```

**Response:**
Excel file (.xlsx) with a single sheet containing:
- Client information (name, ID)
- Report period
- Projects section with total hours per project
- Collaborator hours for each project
- Tasks section for each project with total hours per task
- Collaborator hours for each task
- Total hours for the client

## Note on Authentication

Most endpoints require authentication. The examples above assume you have already obtained an access token. For endpoints that require OAuth2 authentication (like the calendar endpoints), you need to include the access token in the Authorization header.

To obtain an access token, you need to authenticate through the OAuth2 flow:

1. Navigate to `/oauth2/authorization/google` in your browser
2. Complete the Google authentication process
3. You will be redirected back to the application with an access token
