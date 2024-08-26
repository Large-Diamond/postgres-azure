export const environment: {
  apiContextPath: string;
} = {
  apiContextPath: document
    .querySelector('base')
    ?.getAttribute('href')
    ?.replace(/\/$/, '')!,
};

export async function bootstrapEnvironment() {}
