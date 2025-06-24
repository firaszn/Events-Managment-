import 'zone.js/node';

import { APP_BASE_HREF } from '@angular/common';
import { ApplicationConfig } from '@angular/core';
import express from 'express';
import { fileURLToPath } from 'node:url';
import { dirname, join, resolve } from 'node:path';
import { Request, Response, NextFunction } from 'express';
import bootstrap from './main.server';
import { renderApplication } from '@angular/platform-server';

// The Express app is exported so that it can be used by serverless Functions.
export function app(): express.Express {
  const server = express();
  const serverDistFolder = dirname(fileURLToPath(import.meta.url));
  const browserDistFolder = resolve(serverDistFolder, '../browser');
  const indexHtml = join(serverDistFolder, 'index.server.html');

  server.set('view engine', 'html');
  server.set('views', browserDistFolder);

  // Example Express Rest API endpoints
  // server.get('/api/**', (req, res) => { });
  // Serve static files from /browser
  server.get('*.*', express.static(browserDistFolder, {
    maxAge: '1y'
  }));

  // All regular routes use the Angular engine
  server.get('*', (req: Request, res: Response, next: NextFunction) => {
    const { protocol, originalUrl, baseUrl, headers } = req;

    renderApplication(bootstrap, {
      document: indexHtml,
      url: `${protocol}://${headers.host}${originalUrl}`,
      platformProviders: [{ provide: APP_BASE_HREF, useValue: baseUrl }]
    })
    .then((html: string) => res.send(html))
    .catch((err: Error) => next(err));
  });

  server.use('/**', (req: Request, res: Response, next: NextFunction) => {
    res.render('index', { req, providers: [{ provide: APP_BASE_HREF, useValue: req.baseUrl }] });
  });

  return server;
}

function run(): void {
  const port = process.env['PORT'] || 4000;

  // Start up the Node server
  const server = app();
  server.listen(port, () => {
    console.log(`Node Express server listening on http://localhost:${port}`);
  });
}

// Start the server automatically when this file is run directly
run();
