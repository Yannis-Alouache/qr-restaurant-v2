import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './app/app.config';
import { App } from './app/app';

if (!('global' in globalThis)) {
  Object.defineProperty(globalThis, 'global', {
    value: globalThis,
    configurable: true,
  });
}

bootstrapApplication(App, appConfig)
  .catch((err) => console.error(err));
