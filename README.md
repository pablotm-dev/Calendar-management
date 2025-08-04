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

## Monitoramento

A aplicação expõe endpoints do Spring Boot Actuator para monitoramento:

- Health check: `http://localhost:8081/actuator/health`
- Informações da aplicação: `http://localhost:8081/actuator/info`

Estes endpoints são utilizados pelos health checks do Docker para garantir que a aplicação está funcionando corretamente.
