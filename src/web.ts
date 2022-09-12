import { WebPlugin } from '@capacitor/core';

import type { CapacitorPaytmAllinOnePlugin } from './definitions';

export class CapacitorPaytmAllinOneWeb
  extends WebPlugin
  implements CapacitorPaytmAllinOnePlugin
{
  async echo(options: { value: string }): Promise<{ value: string }> {
    console.log('ECHO', options);
    return options;
  }
}
