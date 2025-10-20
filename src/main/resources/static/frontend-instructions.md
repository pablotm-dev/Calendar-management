# Frontend Integration Instructions

## Authentication Flow

The backend now provides an authentication status endpoint that the frontend should use to check if the user is authenticated before making API requests. This will prevent CORS errors that occur when the frontend tries to make API requests without being authenticated.

### Authentication Status Endpoint

```
GET /api/auth/status
```

#### Response (Authenticated)

```json
{
  "authenticated": true,
  "name": "User Name",
  "email": "user@example.com",
  "picture": "https://example.com/user-picture.jpg"
}
```

#### Response (Not Authenticated)

```json
{
  "authenticated": false,
  "loginUrl": "/oauth2/authorization/google"
}
```

### Frontend Implementation

Here's how to implement the authentication flow in your frontend:

1. Create an authentication service that checks the authentication status:

```typescript
// auth.ts
export interface AuthUser {
  authenticated: boolean;
  name?: string;
  email?: string;
  picture?: string;
  loginUrl?: string;
}

export async function checkAuthStatus(): Promise<AuthUser> {
  try {
    const response = await fetch('http://localhost:8081/api/auth/status', {
      credentials: 'include'
    });
    return await response.json();
  } catch (error) {
    console.error('Error checking auth status:', error);
    return { authenticated: false, loginUrl: '/oauth2/authorization/google' };
  }
}

export function redirectToLogin(loginUrl: string): void {
  // Redirect to the backend's login URL
  window.location.href = `http://localhost:8081${loginUrl}`;
}
```

2. Create an API client that checks authentication before making requests:

```typescript
// api.ts
import { checkAuthStatus, redirectToLogin } from './auth';

async function fetchWithAuth(url: string, options: RequestInit = {}): Promise<Response> {
  // Check authentication status
  const authStatus = await checkAuthStatus();
  
  // If not authenticated, redirect to login
  if (!authStatus.authenticated) {
    redirectToLogin(authStatus.loginUrl!);
    // Throw an error to prevent further execution
    throw new Error('Not authenticated');
  }
  
  // Make the API request with credentials
  return fetch(url, {
    ...options,
    credentials: 'include'
  });
}

export const api = {
  // Generic methods
  async get<T>(url: string): Promise<T> {
    const response = await fetchWithAuth(url);
    return response.json();
  },
  
  async post<T>(url: string, data: any): Promise<T> {
    const response = await fetchWithAuth(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(data)
    });
    return response.json();
  },
  
  async put<T>(url: string, data: any): Promise<T> {
    const response = await fetchWithAuth(url, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(data)
    });
    return response.json();
  },
  
  async delete(url: string): Promise<void> {
    await fetchWithAuth(url, {
      method: 'DELETE'
    });
  },
  
  // Specific API endpoints
  clientes: {
    getAll: () => api.get<any[]>('http://localhost:8081/api/clientes'),
    getById: (id: number) => api.get<any>(`http://localhost:8081/api/clientes/${id}`),
    create: (data: any) => api.post<any>('http://localhost:8081/api/clientes', data),
    update: (id: number, data: any) => api.put<any>(`http://localhost:8081/api/clientes/${id}`, data),
    delete: (id: number) => api.delete(`http://localhost:8081/api/clientes/${id}`)
  },
  
  projetos: {
    getAll: () => api.get<any[]>('http://localhost:8081/api/projetos'),
    getById: (id: number) => api.get<any>(`http://localhost:8081/api/projetos/${id}`),
    getByClienteId: (clienteId: number) => api.get<any[]>(`http://localhost:8081/api/projetos/cliente/${clienteId}`),
    create: (data: any) => api.post<any>('http://localhost:8081/api/projetos', data),
    update: (id: number, data: any) => api.put<any>(`http://localhost:8081/api/projetos/${id}`, data),
    delete: (id: number) => api.delete(`http://localhost:8081/api/projetos/${id}`)
  },
  
  tasks: {
    getAll: () => api.get<any[]>('http://localhost:8081/api/tasks'),
    getById: (id: number) => api.get<any>(`http://localhost:8081/api/tasks/${id}`),
    getByProjetoId: (projetoId: number) => api.get<any[]>(`http://localhost:8081/api/tasks/projeto/${projetoId}`),
    getByTag: (tag: string) => api.get<any>(`http://localhost:8081/api/tasks/tag/${tag}`),
    create: (data: any) => api.post<any>('http://localhost:8081/api/tasks', data),
    update: (id: number, data: any) => api.put<any>(`http://localhost:8081/api/tasks/${id}`, data),
    delete: (id: number) => api.delete(`http://localhost:8081/api/tasks/${id}`)
  }
};
```

3. Use the API client in your components:

```tsx
// Example component using the API client
import React, { useEffect, useState } from 'react';
import { api } from './api';

interface Cliente {
  id: number;
  nomeCliente: string;
}

export default function ClientesPage() {
  const [clientes, setClientes] = useState<Cliente[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    async function fetchClientes() {
      try {
        const data = await api.clientes.getAll();
        setClientes(data);
        setLoading(false);
      } catch (err) {
        console.error('Error fetching clientes:', err);
        setError('Failed to load clients');
        setLoading(false);
      }
    }

    fetchClientes();
  }, []);

  if (loading) return <div>Loading...</div>;
  if (error) return <div>{error}</div>;

  return (
    <div>
      <h1>Clientes</h1>
      <ul>
        {clientes.map(cliente => (
          <li key={cliente.id}>{cliente.nomeCliente}</li>
        ))}
      </ul>
    </div>
  );
}
```

## Important Notes

1. Always include `credentials: 'include'` in your fetch requests to send cookies with cross-origin requests.
2. The backend is configured to allow requests from:
   - http://localhost:3000
   - https://calendar-management-front-nn.vercel.app
   - https://*.vercel.app
3. If your frontend is hosted on a different domain, you'll need to update the CORS configuration in the backend.
4. The authentication flow relies on cookies, so make sure your browser allows third-party cookies.
5. If you're using a state management library like Redux, you might want to store the authentication state there.

## Troubleshooting

If you're still experiencing CORS issues:

1. Check the browser console for specific error messages
2. Verify that your frontend is making requests to the correct backend URL
3. Make sure you're including credentials in your fetch requests
4. Check that the backend CORS configuration includes your frontend domain
5. Try clearing your browser cookies and cache