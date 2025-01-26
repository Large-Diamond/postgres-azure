declare global {
  interface Window {
    __env: {
      apiContextPath: string;
      tenantId: string;
      clientId: string;
      apiClientId: string;
    };
  }
}

export const environment = {
  production: true,
  apiContextPath: window.__env.apiContextPath,
  tenantId: window.__env.tenantId,
  clientId: window.__env.clientId,
  apiClientId: window.__env.apiClientId,
};

export async function bootstrapEnvironment() {}
