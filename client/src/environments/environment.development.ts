import '../mocks/browser';

export const environment = {
  mockAuth: false,
  production: false,
  apiContextPath: '/api',
  tenantId: import.meta.env.NG_APP_TENANT_ID,
  clientId: import.meta.env.NG_APP_CLIENT_ID,
  apiClientId: import.meta.env.NG_APP_API_CLIENT_ID,
};

console.log('Environment:', environment);

export async function bootstrapEnvironment() {
  const { setupMocks } = await import('../mocks/browser');
  await setupMocks();
}
