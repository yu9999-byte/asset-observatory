import { useEffect,useMemo,useState,type ReactNode } from 'react';
import { ArrowDown,ArrowUp,ArrowUpDown,ChevronLeft,ChevronRight,Columns3,Download,Search,RotateCcw,RefreshCw } from 'lucide-react';
import { useTablePreferences } from './useTablePreferences';
import type { TableId } from './types';
export interface Column<T>{key:string;title:string;value:(row:T)=>string|null|undefined;render?:(row:T)=>ReactNode;numeric?:boolean;className?:string;pinned?:boolean}
export function compareDecimal(a:string,b:string){
 const normalize=(s:string)=>{const neg=s.startsWith('-');const [w,f='']=s.replace(/^[+-]/,'').split('.');return {neg,w:w.replace(/^0+/,'')||'0',f};};
 const x=normalize(a),y=normalize(b);if(x.neg!==y.neg)return x.neg?-1:1;
 const length=Math.max(x.f.length,y.f.length);let result=x.w.length===y.w.length?x.w.localeCompare(y.w):Math.sign(x.w.length-y.w.length);
 if(!result)result=x.f.padEnd(length,'0').localeCompare(y.f.padEnd(length,'0'));
 return Math.sign(result)*(x.neg?-1:1);
}
export function csvCell(value:string){const safe=/^[\s]*[=+\-@\t\r]/.test(value)?"'"+value:value;return '"'+safe.replace(/"/g,'""')+'"';}
export function DataTable<T>({label,rows,columns,rowKey,empty='暂无可用记录',preferenceId,onServerQuery,showResultSummary=true,showExport=true,searchKeys,searchPlaceholder='筛选当前已加载数据…'}:{label:string;rows:T[];columns:Column<T>[];rowKey:(row:T)=>string;empty?:string;preferenceId?:TableId;onServerQuery?:(query:string,sortKey:string|null,desc:boolean,pageSize:number)=>void;showResultSummary?:boolean;showExport?:boolean;searchKeys?:string[];searchPlaceholder?:string}){
 const [query,setQuery]=useState('');const [page,setPage]=useState(0);
 const preferences=useTablePreferences(preferenceId,columns.map(c=>c.key));
 const {hidden,order,sortKey,desc,pageSize}=preferences.settings;
 const locked=preferences.busy||!!preferences.error;
 useEffect(()=>{if(onServerQuery){setPage(0);if(!locked)onServerQuery(query,sortKey,desc,pageSize);}},[onServerQuery,locked,query,sortKey,desc,pageSize]);
 const ordered=[...order.filter(key=>columns.some(c=>c.key===key)),...columns.map(c=>c.key).filter(key=>!order.includes(key))].map(key=>columns.find(c=>c.key===key)!);
 const visible=ordered.filter(c=>!hidden.includes(c.key));
 function move(key:string,delta:number){const next=ordered.map(c=>c.key);const index=next.indexOf(key);[next[index],next[index+delta]]=[next[index+delta],next[index]];void preferences.save({...preferences.settings,order:next});}
 const filtered=useMemo(()=>{
  if(onServerQuery)return rows;
  const filterColumns=searchKeys?.length?columns.filter(c=>searchKeys.includes(c.key)):columns;
  const list=rows.filter(row=>filterColumns.some(c=>(c.value(row)||'').toLowerCase().includes(query.toLowerCase())));
  const col=columns.find(c=>c.key===sortKey);if(col){list.sort((a,b)=>{const x=col.value(a),y=col.value(b);if(x==null)return y==null?0:1;if(y==null)return -1;return (col.numeric?compareDecimal(x,y):x.localeCompare(y))*(desc?-1:1);});}return list;
 },[rows,columns,query,sortKey,desc,onServerQuery]);
 const pages=Math.max(1,Math.ceil(filtered.length/pageSize));const current=Math.min(page,pages-1);
 function exportCsv(){const csv='\uFEFF'+[visible.map(c=>csvCell(c.title)).join(','),...filtered.map(r=>visible.map(c=>csvCell(c.value(r)??'')).join(','))].join('\r\n');const url=URL.createObjectURL(new Blob([csv],{type:'text/csv;charset=utf-8'}));const link=document.createElement('a');link.href=url;link.download=`${label}.csv`;link.click();setTimeout(()=>URL.revokeObjectURL(url),1000);}
 return <section className="data-table" aria-label={label}>
  <div className="table-tools"><label className="search"><Search size={16}/><input aria-label={`筛选 ${label}`} placeholder={searchPlaceholder} value={query} onChange={e=>{setQuery(e.target.value);setPage(0);}}/></label><div className="tool-group"><details className="columns-menu"><summary title="显示或隐藏列"><Columns3 size={16}/><span>显示列</span></summary><div className="columns-options">{ordered.map((c,index)=><div className="column-option" key={c.key}><label><input type="checkbox" checked={!hidden.includes(c.key)} disabled={locked||(!hidden.includes(c.key)&&visible.length===1)} onChange={()=>void preferences.save({...preferences.settings,hidden:hidden.includes(c.key)?hidden.filter(k=>k!==c.key):[...hidden,c.key]})}/>{c.title}</label><button className="icon-button" title={`上移 ${c.title}`} aria-label={`上移 ${c.title}`} disabled={locked||index===0} onClick={()=>move(c.key,-1)}><ArrowUp size={14}/></button><button className="icon-button" title={`下移 ${c.title}`} aria-label={`下移 ${c.title}`} disabled={locked||index===ordered.length-1} onClick={()=>move(c.key,1)}><ArrowDown size={14}/></button></div>)}</div></details><button className="icon-button" title="恢复默认表格配置" aria-label={`恢复默认表格配置 ${label}`} disabled={locked} onClick={()=>{void preferences.reset();setPage(0);}}/>{showExport&&<button className="icon-button" title="导出已加载筛选结果为 CSV" aria-label={`导出 ${label}`} onClick={exportCsv}><Download size={17}/></button>}</div></div>
  {preferences.error&&<div className="table-preference-error" role="alert"><span>表格配置未确认保存：{preferences.error}</span><button className="icon-button" title="重新读取表格配置" aria-label={`重新读取表格配置 ${label}`} onClick={()=>void preferences.reload()}><RefreshCw size={16}/></button></div>}
  <div className="table-scroll" tabIndex={0} aria-label={label+'横向滚动区域'}><table><thead><tr>{visible.map((c,index)=><th key={c.key} className={[c.className,index===0&&c.pinned?'pinned-column':''].filter(Boolean).join(' ')} aria-sort={sortKey===c.key?(desc?'descending':'ascending'):'none'}><button disabled={locked} onClick={()=>{void preferences.save({...preferences.settings,sortKey:c.key,desc:sortKey===c.key?!desc:false});setPage(0);}}>{c.title}{sortKey===c.key?(desc?<ArrowDown size={13}/>:<ArrowUp size={13}/>):<ArrowUpDown size={13}/>}</button></th>)}</tr></thead><tbody>{filtered.slice(current*pageSize,current*pageSize+pageSize).map(row=><tr key={rowKey(row)}>{visible.map((c,index)=><td className={[c.className,index===0&&c.pinned?'pinned-column':''].filter(Boolean).join(' ')} key={c.key}>{c.render?c.render(row):c.value(row)??'—'}</td>)}</tr>)}</tbody></table>{!filtered.length&&<div className="empty-state"><Search size={20}/><p>{empty}</p><span>缺失不代表余额或成交量为零</span></div>}</div>
  <div className="table-footer">{showResultSummary&&<span>{onServerQuery?`目录查询 ${filtered.length} / ${rows.length} 条 · 服务端筛选与排序`:`已加载 ${rows.length} 条 · 筛选 ${filtered.length} 条 · 仅对已加载数据排序`}</span>}<div className="pagination"><select aria-label={`每页条数 ${label}`} value={pageSize} disabled={locked} onChange={e=>{void preferences.save({...preferences.settings,pageSize:Number(e.target.value)});setPage(0);}}>{[15,30,60].map(size=><option key={size} value={size}>{size} / 页</option>)}</select><button className="icon-button" aria-label="上一页" disabled={current===0} onClick={()=>setPage(current-1)}><ChevronLeft size={17}/></button><span>{current+1} / {pages}</span><button className="icon-button" aria-label="下一页" disabled={current>=pages-1} onClick={()=>setPage(current+1)}><ChevronRight size={17}/></button></div></div>
 </section>;
}
