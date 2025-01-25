import '../mocks/browser';

export const environment = {
  apiContextPath: '/api',
  clientId: import.meta.env.NG_APP_CLIENT_ID,
  tenantId: import.meta.env.NG_APP_TENANT_ID,
};

console.log('Environment:', environment);

export async function bootstrapEnvironment() {
  const { setupMocks } = await import('../mocks/browser');
  await setupMocks();
}
