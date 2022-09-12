export interface CapacitorPaytmAllinOnePlugin {
  startTransaction(options: {
    orderId: string;
    mid: string;
    txnToken: string;
    amount: string;
    callbackUrl: string;
    isStaging: boolean;
    restrictAppInvoke: boolean;
  }): Promise<any>;
}
