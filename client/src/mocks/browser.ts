import { delay, http, HttpResponse } from 'msw';
import { setupWorker } from 'msw/browser';
import { Database, Table } from '../types';

const databases: Database[] = [
  {
    name: 'db1',
    tablesCount: 2,
    totalRowCount: 9,
    backupsCount: 2,
    lastBackupTime: new Date(new Date().getTime() - 24 * 60 * 60 * 1000 * 2),
  },
  {
    name: 'db2',
    tablesCount: 3,
    totalRowCount: 12,
    backupsCount: 3,
    lastBackupTime: new Date(new Date().getTime() - 24 * 60 * 60 * 1000 * 0.5),
  },
  {
    name: 'db3',
    tablesCount: 1,
    totalRowCount: 3,
    backupsCount: 1,
    lastBackupTime: null,
  },
];

function getDatabase(name: string): Database {
  const database = databases.find((db) => db.name === name);

  if (!database) {
    throw new Error(`Database ${name} not found`);
  }

  return database;
}

const mocks = [
  http.get('/api/databases', async () => {
    return HttpResponse.json(databases);
  }),
  http.get('/api/database/:name/last-backup-time', async (request) => {
    const name = request.params['name']?.toString();

    if (!name) {
      return HttpResponse.text('name is required', { status: 400 });
    }

    return HttpResponse.json(getDatabase(name).lastBackupTime);
  }),
  http.get('/api/database/:name/tables', async (request) => {
    const name = request.params['name']?.toString();

    if (!name) {
      return HttpResponse.text('name is required', { status: 400 });
    }

    await delay(600);
    return HttpResponse.json({
      tables: [
        { name: 'fruites', rowCount: 4 },
        { name: 'vegetables', rowCount: 5 },
      ],
      totalRowCount: getDatabase(name).totalRowCount,
    } satisfies {
      tables: Table[];
      totalRowCount: number;
    });
  }),
  http.post('/api/cleanup', async () => {
    await delay(400);
    return HttpResponse.json(null);
  }),
  http.post('/api/backup', async () => {
    await delay(200);
    databases.forEach((db) => getDatabase(db.name).backupsCount++);
    return HttpResponse.json(null);
  }),
  http.post('/api/database/:name/restore/:backupName', async () => {
    await delay(200);
    return HttpResponse.json(null);
  }),
  http.get('/api/database/:name/backups', async () => {
    await delay(200);
    return HttpResponse.json([
      {
        name: 'backup1',
        lastModified: new Date(),
        totalRowCount: 4,
        size: 1024,
        retentionPeriod: 7,
      },
      {
        name: 'backup2',
        lastModified: new Date(new Date().getTime() - 24 * 60 * 60 * 1000 * 2),
        totalRowCount: 5,
        size: 2567,
        retentionPeriod: 14,
      },
    ]);
  }),
  http.get('/api/database/:name/backup/:backupName', async (request) => {
    const name = request.params['name']?.toString();
    const backupName = request.params['backupName']?.toString();

    if (!name) {
      return HttpResponse.text('name is required', { status: 400 });
    }

    if (!backupName) {
      return HttpResponse.text('backupName is required', { status: 400 });
    }

    await delay(200);
    return HttpResponse.json({
      url: `http://example.com/backup-${name}-${backupName}.zip`,
    });
  }),
];

export async function setupMocks() {
  const worker = setupWorker(...mocks);
  await worker.start({
    onUnhandledRequest: (request) => {
      if (request.url.startsWith('/api')) {
        console.error(`No request handler found for ${request.url}`);
      }
    },
  });
}
