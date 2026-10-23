import { useEffect,useRef,useState } from 'react';
import { createPortal } from 'react-dom';
import { ArrowDownLeft,ArrowDownUp,ArrowUpRight,ExternalLink,RefreshCw,Wallet,X } from 'lucide-react';
import { api,number,short,time } from './api';
import { transferSummary } from './transferSummary';
import type { Envelope,Transfer } from './types';

export function AddressDrawer({address,asset,onClose}:{address:string;asset:string;onClose:()=>void}){
 const panel=useRef<HTMLElement>(null);const closeButton=useRef<HTMLButtonElement>(null);
 const [result,setResult]=useState<Envelope<Transfer[]>>();const [error,setError]=useState('');const [busy,setBusy]=useState(true);
 const [direction,setDirection]=useState('all');const [revision,setRevision]=useState(0);
 const detailUrl=`/addresses/${encodeURIComponent(address)}?asset=${encodeURIComponent(asset)}`;
 useEffect(()=>{
  const previousFocus=document.activeElement as HTMLElement|null;const root=document.getElementById('root');
  const wasInert=root?.inert;const overflow=document.body.style.overflow;
  if(root)root.inert=true;document.body.style.overflow='hidden';closeButton.current?.focus();
  function handleKey(event:KeyboardEvent){
   if(event.key==='Escape'){event.preventDefault();onClose();return;}
   if(event.key!=='Tab')return;
   const controls=Array.from(panel.current?.querySelectorAll<HTMLElement>('*')??[]).filter(element=>element.matches('a[href],button:not(:disabled),input:not(:disabled),select:not(:disabled),[tabindex="0"]'));
   if(!controls?.length)return;
   const first=controls[0],last=controls[controls.length-1];
   if(event.shiftKey&&(document.activeElement===first||!panel.current?.contains(document.activeElement))){event.preventDefault();last.focus();}
   else if(!event.shiftKey&&(document.activeElement===last||!panel.current?.contains(document.activeElement))){event.preventDefault();first.focus();}
  }
  document.addEventListener('keydown',handleKey);
  return()=>{document.removeEventListener('keydown',handleKey);if(root)root.inert=wasInert??false;document.body.style.overflow=overflow;previousFocus?.focus();};
 },[onClose]);
 useEffect(()=>{
  let active=true;setBusy(true);setError('');setResult(undefined);
  api<Envelope<Transfer[]>>(`/api/addresses/${encodeURIComponent(address)}/transfers?asset=${encodeURIComponent(asset)}`)
   .then(data=>{if(active)setResult(data);})
   .catch(e=>{if(active)setError((e as Error).message);})
   .finally(()=>{if(active)setBusy(false);});
  return()=>{active=false;};
 },[address,asset,revision]);
 const rows=result?.data??[];const filtered=rows.filter(row=>direction==='all'||row.direction===direction);
 const totals=transferSummary(rows);const available=result&&(result.status==='ok'||result.status==='partial'||result.status==='stale');
 const summaryValue=(value:string)=>rows.length?number(value,8):'—';
 return createPortal(<div className="address-drawer-backdrop" onClick={event=>{if(event.target===event.currentTarget)onClose();}}>
  <aside ref={panel} className="address-drawer" role="dialog" aria-modal="true" aria-labelledby="address-drawer-title" aria-describedby="address-drawer-address">
   <header className="address-drawer-header"><div><span className="drawer-eyebrow">{asset} · 地址预览</span><h2 id="address-drawer-title"><Wallet size={18}/>转入转出</h2></div><button ref={closeButton} className="icon-button" aria-label="关闭地址预览" title="关闭地址预览" onClick={onClose}><X size={20}/></button></header>
   <div className="address-drawer-body">
    <div className="drawer-address"><span id="address-drawer-address" className="mono">{address}</span><span>Ethereum 地址 · {asset}</span></div>
    <div className="drawer-flow-summary" role="group" aria-label="本批次资金流"><div><span>转入</span><strong className="change-up">{summaryValue(totals.incoming)}</strong><small>{asset}</small></div><div><span>转出</span><strong className="change-down">{summaryValue(totals.outgoing)}</strong><small>{asset}</small></div><div><span>净流量</span><strong className={totals.net.startsWith('-')?'change-down':'change-up'}>{summaryValue(totals.net)}</strong><small>{asset}</small></div></div>
    <p className="drawer-scope">仅统计当前来源批次，自转不计入流入流出。</p>
    <div className="drawer-transfer-tools"><div className="filter-tabs" role="group" aria-label="预览事件方向">{[{key:'all',label:'全部'},{key:'IN',label:'转入'},{key:'OUT',label:'转出'}].map(item=><button key={item.key} className={direction===item.key?'active':''} aria-pressed={direction===item.key} onClick={()=>setDirection(item.key)}>{item.label}</button>)}</div><button className="icon-button" title="重新读取转账" aria-label="重新读取转账" disabled={busy} onClick={()=>setRevision(value=>value+1)}><RefreshCw size={16} className={busy?'spinning':''}/></button></div>
    {busy?<div className="drawer-empty" role="status">正在读取转账记录…</div>:error?<div className="drawer-empty" role="alert"><strong>转账读取失败</strong><span>{error}</span></div>:!available?<div className="drawer-empty" role="status"><strong>{result?.status==='unsupported'?`${asset} 转账暂未覆盖`:'转账来源暂不可用'}</strong><span>{result?.reason??'暂时无法读取该地址的转账记录'}</span></div>:!filtered.length?<div className="drawer-empty" role="status"><strong>{rows.length?'没有符合方向的已加载记录':'当前来源批次没有转账记录'}</strong><span>当前结果不代表该地址完整历史。</span></div>:<>
     <div className="drawer-transfer-count">当前批次 {filtered.length} 笔{filtered.length>8?' · 预览前 8 笔':''}</div>
     <ul className="drawer-transfer-list" aria-label="地址转账预览">{filtered.slice(0,8).map(row=><li key={row.id}>
      <div className={'drawer-transfer-direction direction '+row.direction.toLowerCase()}>{row.direction==='IN'?<ArrowDownLeft size={16}/>:row.direction==='OUT'?<ArrowUpRight size={16}/>:<ArrowDownUp size={16}/>}{{IN:'转入',OUT:'转出',SELF:'自转'}[row.direction]}</div>
      <div className="drawer-transfer-party"><span className="mono" title={row.direction==='IN'?row.from:row.to}>{short(row.direction==='IN'?row.from:row.to)}</span><time dateTime={row.sourceAt??undefined}>{time(row.sourceAt)} · UTC+8</time></div>
      <div className={'drawer-transfer-quantity '+(row.direction==='IN'?'change-up':row.direction==='OUT'?'change-down':'muted')} title={row.amount+' '+asset}><strong>{row.direction==='IN'?'+':row.direction==='OUT'?'−':''}{number(row.amount,8)}</strong><small>{asset}</small></div>
     </li>)}</ul>
    </>}
    {result&&<div className="drawer-source"><span>来源：{result.source}</span><span>{result.fetchedAt?'抓取 '+time(result.fetchedAt)+' · UTC+8':'尚无成功抓取时间'}</span></div>}
   </div>
   <footer className="address-drawer-footer"><a className="primary drawer-detail-link" href={detailUrl} target="_blank" rel="noopener noreferrer">新页面查看地址详情<ExternalLink size={16}/></a></footer>
  </aside>
 </div>,document.body);
}
