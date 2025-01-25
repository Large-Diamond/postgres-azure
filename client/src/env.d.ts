declare interface Env {
  NG_APP_TENANT_ID: string;
  NG_APP_CLIENT_ID: string;
  [key: string]: any;
}

declare interface ImportMeta {
  readonly env: Env;
}
