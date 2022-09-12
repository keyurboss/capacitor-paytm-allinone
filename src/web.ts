import { WebPlugin } from '@capacitor/core';

import type { CapacitorPaytmAllinOnePlugin } from './definitions';

export class CapacitorPaytmAllinOneWeb
  extends WebPlugin
  implements CapacitorPaytmAllinOnePlugin
{
  startTransaction(options: {
    orderId: string;
    mid: string;
    txnToken: string;
    amount: string;
    callbackUrl: string;
    isStaging: boolean;
    restrictAppInvoke: boolean;
  }): Promise<any> {
    console.log('Txn', options);
    throw new Error('Method not implemented.');
  }
}
