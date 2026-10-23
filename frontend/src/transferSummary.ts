import type { Transfer } from './types';

function decimal(value:bigint,scale:number){
 const sign=value<0n?'-':'';const digits=(value<0n?-value:value).toString().padStart(scale+1,'0');
 if(!scale)return sign+digits;
 const fraction=digits.slice(-scale).replace(/0+$/,'');
 return sign+digits.slice(0,-scale)+(fraction?'.'+fraction:'');
}

// Keep token quantities exact, including values larger than Number.MAX_SAFE_INTEGER.
export function transferSummary(rows:Transfer[]){
 const scale=Math.max(0,...rows.map(row=>row.amount.split('.')[1]?.length??0));
 let incoming=0n;let outgoing=0n;
 for(const row of rows){
  const [whole,fraction='']=row.amount.split('.');
  const amount=BigInt(whole+fraction.padEnd(scale,'0'));
  if(row.direction==='IN')incoming+=amount;
  if(row.direction==='OUT')outgoing+=amount;
 }
 return {incoming:decimal(incoming,scale),outgoing:decimal(outgoing,scale),net:decimal(incoming-outgoing,scale)};
}
