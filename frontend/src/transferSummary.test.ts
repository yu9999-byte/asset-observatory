import { expect,test } from 'vitest';
import { transferSummary } from './transferSummary';
import type { Transfer } from './types';

const row=(amount:string,direction:Transfer['direction'])=>({amount,direction} as Transfer);
test('sums token quantities exactly and excludes self transfers',()=>{
 expect(transferSummary([row('9007199254740993.000000000000000001','IN'),row('0.000000000000000002','OUT'),row('100','SELF')]))
  .toEqual({incoming:'9007199254740993.000000000000000001',outgoing:'0.000000000000000002',net:'9007199254740992.999999999999999999'});
});
test('supports mixed precision, negative net flow and no events',()=>{
 expect(transferSummary([row('10.1','IN'),row('20.123','OUT')])).toEqual({incoming:'10.1',outgoing:'20.123',net:'-10.023'});
 expect(transferSummary([])).toEqual({incoming:'0',outgoing:'0',net:'0'});
});
