import { registerPlugin } from '@capacitor/core';

import type { CapacitorPaytmAllinOnePlugin } from './definitions';

const CapacitorPaytmAllinOne = registerPlugin<CapacitorPaytmAllinOnePlugin>(
  'CapacitorPaytmAllinOne',
  {
    web: () => import('./web').then(m => new m.CapacitorPaytmAllinOneWeb()),
  },
);

export * from './definitions';
export { CapacitorPaytmAllinOne };
