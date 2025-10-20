# Calendar Management Application

Este é um sistema de gerenciamento de calendário que permite o controle de clientes, projetos e tarefas.

## Requisitos

- Docker
- Docker Compose

## Como executar com Docker

1. Clone o repositório:
   ```
   git clone <url-do-repositorio>
   cd Calendar-management
   ```

2. Execute a aplicação com Docker Compose:
   ```
   docker-compose up -d
   ```

   Este comando irá:
   - Construir a imagem da aplicação Spring Boot
   - Iniciar um container PostgreSQL
   - Iniciar a aplicação conectada ao banco de dados

3. A aplicação estará disponível em:
   ```
   http://localhost:8081
   ```

   A aplicação também está disponível na porta 8080 (configurada em server.http.port).

4. Para parar a aplicação:
   ```
   docker-compose down
   ```

5. Para parar a aplicação e remover os volumes (isso apagará os dados do banco):
   ```
   docker-compose down -v
   ```

## Configuração do Banco de Dados

O banco de dados PostgreSQL está configurado com:
- Nome do banco: apontamentos
- Usuário: usuario
- Senha: senha

Os dados são persistidos em um volume Docker, então eles não serão perdidos quando os containers forem reiniciados.

## Desenvolvimento

Para desenvolvimento local sem Docker, configure o arquivo `application.properties` para apontar para sua instância local do PostgreSQL.

## Configuração do Servidor

A aplicação está configurada para aceitar conexões HTTP nas portas 8081 (porta principal) e 8080 (porta adicional):

1. **Portas**: A aplicação utiliza a porta 8081 como porta principal e a porta 8080 como porta adicional.

2. **Configuração das portas**: As propriedades das portas estão definidas no arquivo `application.properties`:
   ```
   server.port=8081
   server.http.port=8080
   ```

## Monitoramento

A aplicação expõe endpoints do Spring Boot Actuator para monitoramento:

- Health check: `http://localhost:8081/actuator/health`
- Informações da aplicação: `http://localhost:8081/actuator/info`

Estes endpoints são utilizados pelos health checks do Docker para garantir que a aplicação está funcionando corretamente.

## Deploy no Render

A aplicação está configurada para ser facilmente implantada na plataforma Render. Para detalhes sobre como fazer o deploy de forma segura, especialmente em relação às credenciais do Google Service Account, consulte o guia detalhado em [docs/RENDER_DEPLOYMENT.md](docs/RENDER_DEPLOYMENT.md).

### Segurança das Credenciais

Para proteger as credenciais do Google Service Account:

1. O arquivo `service-account.json` está configurado para ser ignorado pelo Git (.gitignore)
2. A aplicação foi modificada para carregar as credenciais de uma variável de ambiente quando disponível
3. O arquivo `render.yaml` inclui a configuração para a variável de ambiente `GOOGLE_SERVICE_ACCOUNT_JSON`

Isso permite que você mantenha o arquivo localmente para desenvolvimento, mas use variáveis de ambiente seguras em produção.

## Documentação da API

A API do Calendar Management fornece endpoints para gerenciar clientes, projetos, tarefas e gerar relatórios. Abaixo está a documentação completa dos endpoints disponíveis.

### Autenticação

A API utiliza OAuth2 com Google para autenticação. Todos os endpoints (exceto os especificamente marcados como públicos) requerem autenticação.

#### Verificar Status de Autenticação
- **URL**: `/api/auth/status`
- **Método**: GET
- **Autenticação**: Não requerida
- **Resposta de Sucesso (Autenticado)**: 
  - **Código**: 200 OK
  - **Conteúdo**: 
  ```json
  {
    "authenticated": true,
    "name": "Nome do Usuário",
    "email": "usuario@exemplo.com",
    "picture": "https://exemplo.com/foto-usuario.jpg"
  }
  ```
- **Resposta de Sucesso (Não Autenticado)**: 
  - **Código**: 401 Unauthorized
  - **Conteúdo**: 
  ```json
  {
    "authenticated": false,
    "loginUrl": "/oauth2/authorization/google"
  }
  ```

#### Iniciar Fluxo de Autenticação
- **URL**: `/oauth2/authorization/google`
- **Método**: GET
- **Autenticação**: Não requerida
- **Descrição**: Redireciona o usuário para a página de login do Google.

Para mais detalhes sobre como implementar a autenticação no frontend, consulte o arquivo [frontend-instructions.md](src/main/resources/static/frontend-instructions.md).

### Clientes

> **Nota**: Todos os endpoints de clientes também estão disponíveis com o prefixo `/api`, por exemplo, `/api/clientes`.

#### Listar todos os clientes
- **URL**: `/clientes` ou `/api/clientes`
- **Método**: GET
- **Resposta de Sucesso**: 
  - **Código**: 200 OK
  - **Conteúdo**: Array de objetos Cliente
  ```json
  [
    {
      "id": 1,
      "nomeCliente": "Nome do Cliente"
    }
  ]
  ```

#### Obter cliente por ID
- **URL**: `/clientes/{id}`
- **Método**: GET
- **Parâmetros URL**: `id=[Long]` ID do cliente
- **Resposta de Sucesso**: 
  - **Código**: 200 OK
  - **Conteúdo**: Objeto Cliente
  ```json
  {
    "id": 1,
    "nomeCliente": "Nome do Cliente"
  }
  ```
- **Resposta de Erro**: 
  - **Código**: 404 Not Found

#### Criar novo cliente
- **URL**: `/clientes`
- **Método**: POST
- **Corpo da Requisição**: 
  ```json
  {
    "nomeCliente": "Nome do Cliente"
  }
  ```
- **Resposta de Sucesso**: 
  - **Código**: 201 Created
  - **Conteúdo**: Objeto Cliente criado
  ```json
  {
    "id": 1,
    "nomeCliente": "Nome do Cliente"
  }
  ```

#### Atualizar cliente
- **URL**: `/clientes/{id}`
- **Método**: PUT
- **Parâmetros URL**: `id=[Long]` ID do cliente
- **Corpo da Requisição**: 
  ```json
  {
    "nomeCliente": "Novo Nome do Cliente"
  }
  ```
- **Resposta de Sucesso**: 
  - **Código**: 200 OK
  - **Conteúdo**: Objeto Cliente atualizado
  ```json
  {
    "id": 1,
    "nomeCliente": "Novo Nome do Cliente"
  }
  ```
- **Resposta de Erro**: 
  - **Código**: 404 Not Found

#### Excluir cliente
- **URL**: `/clientes/{id}`
- **Método**: DELETE
- **Parâmetros URL**: `id=[Long]` ID do cliente
- **Resposta de Sucesso**: 
  - **Código**: 204 No Content
- **Resposta de Erro**: 
  - **Código**: 404 Not Found

### Projetos

> **Nota**: Todos os endpoints de projetos também estão disponíveis com o prefixo `/api`, por exemplo, `/api/projetos`.

#### Listar todos os projetos
- **URL**: `/projetos` ou `/api/projetos`
- **Método**: GET
- **Resposta de Sucesso**: 
  - **Código**: 200 OK
  - **Conteúdo**: Array de objetos Projeto
  ```json
  [
    {
      "id": 1,
      "nomeProjeto": "Nome do Projeto",
      "idCliente": 1
    }
  ]
  ```

#### Listar projetos por cliente
- **URL**: `/projetos/cliente/{clienteId}`
- **Método**: GET
- **Parâmetros URL**: `clienteId=[Long]` ID do cliente
- **Resposta de Sucesso**: 
  - **Código**: 200 OK
  - **Conteúdo**: Array de objetos Projeto
  ```json
  [
    {
      "id": 1,
      "nomeProjeto": "Nome do Projeto",
      "idCliente": 1
    }
  ]
  ```

#### Obter projeto por ID
- **URL**: `/projetos/{id}`
- **Método**: GET
- **Parâmetros URL**: `id=[Long]` ID do projeto
- **Resposta de Sucesso**: 
  - **Código**: 200 OK
  - **Conteúdo**: Objeto Projeto
  ```json
  {
    "id": 1,
    "nomeProjeto": "Nome do Projeto",
    "idCliente": 1
  }
  ```
- **Resposta de Erro**: 
  - **Código**: 404 Not Found

#### Criar novo projeto
- **URL**: `/projetos`
- **Método**: POST
- **Corpo da Requisição**: 
  ```json
  {
    "nomeProjeto": "Nome do Projeto",
    "idCliente": 1
  }
  ```
- **Resposta de Sucesso**: 
  - **Código**: 201 Created
  - **Conteúdo**: Objeto Projeto criado
  ```json
  {
    "id": 1,
    "nomeProjeto": "Nome do Projeto",
    "idCliente": 1
  }
  ```
- **Resposta de Erro**: 
  - **Código**: 400 Bad Request

#### Atualizar projeto
- **URL**: `/projetos/{id}`
- **Método**: PUT
- **Parâmetros URL**: `id=[Long]` ID do projeto
- **Corpo da Requisição**: 
  ```json
  {
    "nomeProjeto": "Novo Nome do Projeto",
    "idCliente": 1
  }
  ```
- **Resposta de Sucesso**: 
  - **Código**: 200 OK
  - **Conteúdo**: Objeto Projeto atualizado
  ```json
  {
    "id": 1,
    "nomeProjeto": "Novo Nome do Projeto",
    "idCliente": 1
  }
  ```
- **Resposta de Erro**: 
  - **Código**: 404 Not Found

#### Excluir projeto
- **URL**: `/projetos/{id}`
- **Método**: DELETE
- **Parâmetros URL**: `id=[Long]` ID do projeto
- **Resposta de Sucesso**: 
  - **Código**: 204 No Content
- **Resposta de Erro**: 
  - **Código**: 404 Not Found

### Tasks (Tarefas)

> **Nota**: Todos os endpoints de tarefas também estão disponíveis com o prefixo `/api`, por exemplo, `/api/tasks`.

#### Listar todas as tarefas
- **URL**: `/tasks` ou `/api/tasks`
- **Método**: GET
- **Resposta de Sucesso**: 
  - **Código**: 200 OK
  - **Conteúdo**: Array de objetos Task
  ```json
  [
    {
      "id": 1,
      "nomeTask": "Nome da Tarefa",
      "descricaoTask": "Descrição da Tarefa",
      "idProjeto": 1,
      "tagTask": "TAG-001",
      "dataInicio": "2023-01-01T09:00:00",
      "dataFim": "2023-01-01T17:00:00",
      "isAtivo": true
    }
  ]
  ```

#### Listar tarefas por projeto
- **URL**: `/tasks/projeto/{projetoId}`
- **Método**: GET
- **Parâmetros URL**: `projetoId=[Long]` ID do projeto
- **Resposta de Sucesso**: 
  - **Código**: 200 OK
  - **Conteúdo**: Array de objetos Task
  ```json
  [
    {
      "id": 1,
      "nomeTask": "Nome da Tarefa",
      "descricaoTask": "Descrição da Tarefa",
      "idProjeto": 1,
      "tagTask": "TAG-001",
      "dataInicio": "2023-01-01T09:00:00",
      "dataFim": "2023-01-01T17:00:00",
      "isAtivo": true
    }
  ]
  ```

#### Obter tarefa por ID
- **URL**: `/tasks/{id}`
- **Método**: GET
- **Parâmetros URL**: `id=[Long]` ID da tarefa
- **Resposta de Sucesso**: 
  - **Código**: 200 OK
  - **Conteúdo**: Objeto Task
  ```json
  {
    "id": 1,
    "nomeTask": "Nome da Tarefa",
    "descricaoTask": "Descrição da Tarefa",
    "idProjeto": 1,
    "tagTask": "TAG-001",
    "dataInicio": "2023-01-01T09:00:00",
    "dataFim": "2023-01-01T17:00:00",
    "isAtivo": true
  }
  ```
- **Resposta de Erro**: 
  - **Código**: 404 Not Found

#### Obter tarefa por tag
- **URL**: `/tasks/tag/{tagTask}`
- **Método**: GET
- **Parâmetros URL**: `tagTask=[String]` Tag da tarefa
- **Resposta de Sucesso**: 
  - **Código**: 200 OK
  - **Conteúdo**: Objeto Task
  ```json
  {
    "id": 1,
    "nomeTask": "Nome da Tarefa",
    "descricaoTask": "Descrição da Tarefa",
    "idProjeto": 1,
    "tagTask": "TAG-001",
    "dataInicio": "2023-01-01T09:00:00",
    "dataFim": "2023-01-01T17:00:00",
    "isAtivo": true
  }
  ```
- **Resposta de Erro**: 
  - **Código**: 404 Not Found

#### Criar nova tarefa
- **URL**: `/tasks`
- **Método**: POST
- **Corpo da Requisição**: 
  ```json
  {
    "nomeTask": "Nome da Tarefa",
    "descricaoTask": "Descrição da Tarefa",
    "idProjeto": 1,
    "tagTask": "TAG-001",
    "dataInicio": "2023-01-01T09:00:00",
    "dataFim": "2023-01-01T17:00:00",
    "isAtivo": true
  }
  ```
- **Resposta de Sucesso**: 
  - **Código**: 201 Created
  - **Conteúdo**: Objeto Task criado
  ```json
  {
    "id": 1,
    "nomeTask": "Nome da Tarefa",
    "descricaoTask": "Descrição da Tarefa",
    "idProjeto": 1,
    "tagTask": "TAG-001",
    "dataInicio": "2023-01-01T09:00:00",
    "dataFim": "2023-01-01T17:00:00",
    "isAtivo": true
  }
  ```
- **Resposta de Erro**: 
  - **Código**: 400 Bad Request
  - **Conteúdo**: Mensagem de erro

#### Atualizar tarefa
- **URL**: `/tasks/{id}`
- **Método**: PUT
- **Parâmetros URL**: `id=[Long]` ID da tarefa
- **Corpo da Requisição**: 
  ```json
  {
    "nomeTask": "Novo Nome da Tarefa",
    "descricaoTask": "Nova Descrição da Tarefa",
    "idProjeto": 1,
    "tagTask": "TAG-001",
    "dataInicio": "2023-01-01T09:00:00",
    "dataFim": "2023-01-01T17:00:00",
    "isAtivo": true
  }
  ```
- **Resposta de Sucesso**: 
  - **Código**: 200 OK
  - **Conteúdo**: Objeto Task atualizado
  ```json
  {
    "id": 1,
    "nomeTask": "Novo Nome da Tarefa",
    "descricaoTask": "Nova Descrição da Tarefa",
    "idProjeto": 1,
    "tagTask": "TAG-001",
    "dataInicio": "2023-01-01T09:00:00",
    "dataFim": "2023-01-01T17:00:00",
    "isAtivo": true
  }
  ```
- **Resposta de Erro**: 
  - **Código**: 400 Bad Request
  - **Conteúdo**: Mensagem de erro

#### Excluir tarefa
- **URL**: `/tasks/{id}`
- **Método**: DELETE
- **Parâmetros URL**: `id=[Long]` ID da tarefa
- **Resposta de Sucesso**: 
  - **Código**: 204 No Content
- **Resposta de Erro**: 
  - **Código**: 404 Not Found

### Relatórios

#### Gerar relatório para um colaborador específico
- **URL**: `/api/reports/collaborators/{email}`
- **Método**: GET
- **Parâmetros URL**: 
  - `email=[String]` Email do colaborador
  - `startDate=[Instant]` (opcional) Data de início do período do relatório
  - `endDate=[Instant]` (opcional) Data de fim do período do relatório
  - `periodType=[Enum]` (opcional) Tipo de período (WEEK, MONTH, QUARTER, YEAR)
  - `projectIds=[List<Long>]` (opcional) Lista de IDs de projetos para filtrar
  - `taskIds=[List<Long>]` (opcional) Lista de IDs de tarefas para filtrar
- **Resposta de Sucesso**: 
  - **Código**: 200 OK
  - **Conteúdo**: Objeto CollaboratorReportDTO
  ```json
  {
    "collaboratorEmail": "usuario@exemplo.com",
    "totalHoursWorked": 40.5,
    "hoursPerProject": [
      {
        "projectId": 1,
        "projectName": "Projeto A",
        "hours": 25.0,
        "clientName": "Cliente X"
      },
      {
        "projectId": 2,
        "projectName": "Projeto B",
        "hours": 15.5,
        "clientName": "Cliente Y"
      }
    ],
    "hoursPerTask": [
      {
        "taskId": 1,
        "taskName": "Tarefa 1",
        "hours": 10.0,
        "projectId": 1,
        "projectName": "Projeto A"
      },
      {
        "taskId": 2,
        "taskName": "Tarefa 2",
        "hours": 15.0,
        "projectId": 1,
        "projectName": "Projeto A"
      }
    ],
    "productivityAnalysis": {
      "hoursByDayOfWeek": {
        "1": 8.0,
        "2": 8.5,
        "3": 8.0,
        "4": 8.0,
        "5": 8.0
      },
      "hoursByMonth": {
        "1": 160.0,
        "2": 152.0
      },
      "trendData": [
        {
          "date": "2023-01-01",
          "hours": 8.0
        },
        {
          "date": "2023-01-02",
          "hours": 8.5
        }
      ],
      "peakPeriods": [
        {
          "startDate": "2023-01-15",
          "endDate": "2023-01-19",
          "hours": 45.0
        }
      ],
      "lowPeriods": [
        {
          "startDate": "2023-01-01",
          "endDate": "2023-01-05",
          "hours": 20.0
        }
      ]
    },
    "contextDetails": {
      "hoursByTimePeriod": {
        "MORNING": 20.0,
        "AFTERNOON": 15.5,
        "EVENING": 5.0
      },
      "averageSessionDurationMinutes": 120.0
    }
  }
  ```

#### Gerar relatórios para todos os colaboradores
- **URL**: `/api/reports/collaborators`
- **Método**: GET
- **Parâmetros URL**: 
  - `startDate=[Instant]` (opcional) Data de início do período do relatório
  - `endDate=[Instant]` (opcional) Data de fim do período do relatório
  - `periodType=[Enum]` (opcional) Tipo de período (WEEK, MONTH, QUARTER, YEAR)
  - `collaboratorEmails=[List<String>]` (opcional) Lista de emails de colaboradores para filtrar
  - `projectIds=[List<Long>]` (opcional) Lista de IDs de projetos para filtrar
  - `taskIds=[List<Long>]` (opcional) Lista de IDs de tarefas para filtrar
- **Resposta de Sucesso**: 
  - **Código**: 200 OK
  - **Conteúdo**: Array de objetos CollaboratorReportDTO

#### Gerar relatório para um cliente específico
- **URL**: `/api/reports/clients/{clientId}`
- **Método**: GET
- **Parâmetros URL**: 
  - `clientId=[Long]` ID do cliente
  - `startDate=[Instant]` (opcional) Data de início do período do relatório
  - `endDate=[Instant]` (opcional) Data de fim do período do relatório
  - `periodType=[Enum]` (opcional) Tipo de período (WEEK, MONTH, QUARTER, YEAR)
  - `collaboratorEmails=[List<String>]` (opcional) Lista de emails de colaboradores para filtrar
  - `projectIds=[List<Long>]` (opcional) Lista de IDs de projetos para filtrar
  - `taskIds=[List<Long>]` (opcional) Lista de IDs de tarefas para filtrar
- **Resposta de Sucesso**: 
  - **Código**: 200 OK
  - **Conteúdo**: Objeto ClientReportDTO
  ```json
  {
    "clientId": 1,
    "clientName": "Cliente X",
    "projects": [
      {
        "projectId": 1,
        "projectName": "Projeto A",
        "totalHours": 40.0,
        "collaboratorHours": [
          {
            "collaboratorEmail": "usuario1@exemplo.com",
            "hours": 25.0
          },
          {
            "collaboratorEmail": "usuario2@exemplo.com",
            "hours": 15.0
          }
        ],
        "tasks": [
          {
            "taskId": 1,
            "taskName": "Tarefa 1",
            "totalHours": 20.0,
            "collaboratorHours": [
              {
                "collaboratorEmail": "usuario1@exemplo.com",
                "hours": 12.0
              },
              {
                "collaboratorEmail": "usuario2@exemplo.com",
                "hours": 8.0
              }
            ]
          },
          {
            "taskId": 2,
            "taskName": "Tarefa 2",
            "totalHours": 20.0,
            "collaboratorHours": [
              {
                "collaboratorEmail": "usuario1@exemplo.com",
                "hours": 13.0
              },
              {
                "collaboratorEmail": "usuario2@exemplo.com",
                "hours": 7.0
              }
            ]
          }
        ]
      }
    ]
  }
  ```
- **Resposta de Erro**: 
  - **Código**: 404 Not Found

#### Gerar relatórios para todos os clientes
- **URL**: `/api/reports/clients`
- **Método**: GET
- **Parâmetros URL**: 
  - `startDate=[Instant]` (opcional) Data de início do período do relatório
  - `endDate=[Instant]` (opcional) Data de fim do período do relatório
  - `periodType=[Enum]` (opcional) Tipo de período (WEEK, MONTH, QUARTER, YEAR)
  - `collaboratorEmails=[List<String>]` (opcional) Lista de emails de colaboradores para filtrar
  - `projectIds=[List<Long>]` (opcional) Lista de IDs de projetos para filtrar
  - `taskIds=[List<Long>]` (opcional) Lista de IDs de tarefas para filtrar
- **Resposta de Sucesso**: 
  - **Código**: 200 OK
  - **Conteúdo**: Array de objetos ClientReportDTO

## Prompt para Criação de Frontend

Abaixo está um prompt que pode ser usado para solicitar a outra IA a criação de um frontend que se integre com esta API:

```
Crie um frontend em React para uma aplicação de gerenciamento de calendário e apontamentos de horas. O frontend deve se integrar com uma API REST existente e ter as seguintes características:

### Design e Estilo
- Utilize cores claras e VIVAS, com boa variação de cores
- O fundo principal deve ser branco
- Interface limpa e moderna
- Design responsivo para funcionar em dispositivos móveis e desktop

### Páginas e Funcionalidades

1. **Página de Login**
   - Formulário simples com campos para usuário e senha
   - Por enquanto, use credenciais mockadas (usuário: admin, senha: admin)
   - Após login bem-sucedido, redirecionar para a página principal

2. **Página Principal / Dashboard**
   - Visão geral dos apontamentos recentes
   - Acesso rápido às principais funcionalidades
   - Estatísticas básicas (número de clientes, projetos, tarefas)

3. **Gerenciamento de Clientes (CRUD completo)**
   - Listagem de clientes com opções para editar e excluir
   - Formulário para adicionar/editar cliente
   - Confirmação antes de excluir

4. **Gerenciamento de Projetos (CRUD completo)**
   - Listagem de projetos com filtro por cliente
   - Formulário para adicionar/editar projeto (incluindo seleção do cliente)
   - Confirmação antes de excluir

5. **Gerenciamento de Tarefas (CRUD completo)**
   - Listagem de tarefas com filtros por cliente, projeto e status
   - Formulário para adicionar/editar tarefa (incluindo seleção do projeto)
   - Campos para data/hora de início e fim
   - Confirmação antes de excluir

6. **Consulta de Apontamentos**
   - Página dedicada para visualizar apontamentos
   - Filtros avançados por cliente, projeto, tarefa, período
   - Visualização em formato de lista e calendário
   - Opção para exportar dados

### Integração com a API

A API está disponível em http://localhost:8081 e possui os seguintes endpoints:

#### Clientes
- GET /clientes - Listar todos os clientes
- GET /clientes/{id} - Obter cliente por ID
- POST /clientes - Criar novo cliente
- PUT /clientes/{id} - Atualizar cliente
- DELETE /clientes/{id} - Excluir cliente

#### Projetos
- GET /projetos - Listar todos os projetos
- GET /projetos/cliente/{clienteId} - Listar projetos por cliente
- GET /projetos/{id} - Obter projeto por ID
- POST /projetos - Criar novo projeto
- PUT /projetos/{id} - Atualizar projeto
- DELETE /projetos/{id} - Excluir projeto

#### Tarefas
- GET /tasks - Listar todas as tarefas
- GET /tasks/projeto/{projetoId} - Listar tarefas por projeto
- GET /tasks/{id} - Obter tarefa por ID
- GET /tasks/tag/{tagTask} - Obter tarefa por tag
- POST /tasks - Criar nova tarefa
- PUT /tasks/{id} - Atualizar tarefa
- DELETE /tasks/{id} - Excluir tarefa

### Estrutura dos Dados

**Cliente:**
```json
{
  "id": 1,
  "nomeCliente": "Nome do Cliente"
}
```

**Projeto:**
```json
{
  "id": 1,
  "nomeProjeto": "Nome do Projeto",
  "idCliente": 1
}
```

**Tarefa:**
```json
{
  "id": 1,
  "nomeTask": "Nome da Tarefa",
  "descricaoTask": "Descrição da Tarefa",
  "idProjeto": 1,
  "tagTask": "TAG-001",
  "dataInicio": "2023-01-01T09:00:00",
  "dataFim": "2023-01-01T17:00:00",
  "isAtivo": true
}
```

### Tecnologias Sugeridas
- React para a construção da interface
- React Router para navegação
- Axios para chamadas à API
- React Hook Form para formulários
- Material-UI, Chakra UI ou Ant Design para componentes
- React Query para gerenciamento de estado e cache
- Dayjs ou date-fns para manipulação de datas

Por favor, forneça o código-fonte completo, incluindo instruções para instalação e execução do projeto.
```
