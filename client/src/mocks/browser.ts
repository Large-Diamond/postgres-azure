import { setupWorker } from 'msw/browser';
import { delay, http, HttpResponse } from 'msw';
import { Table } from '../types';

const mocks = [
  http.get('/api/databases', async () => {
    return HttpResponse.json(['db1', 'db2']);
  }),
  http.get('/api/database/:name/last-backup-time', async () => {
    return HttpResponse.json(
      new Date(new Date().getTime() - 24 * 60 * 60 * 1000 * 2)
    );
  }),
  http.get('/api/database/:name/tables', async () => {
    await delay(600);
    return HttpResponse.json({
      tables: [
        { name: 'fruites', rowCount: 4 },
        { name: 'vegetables', rowCount: 5 },
      ],
      totalRowCount: 9,
    } satisfies {
      tables: Table[];
      totalRowCount: number;
    });
  }),
  http.post('/api/database/:name/cleanup', async () => {
    await delay(400);
    return HttpResponse.json(null);
  }),
  http.post('/api/database/:name/backup', async () => {
    await delay(200);
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
