import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { api } from './api';
import type { TableId, TablePreference, TableSettings } from './types';

export function useTablePreferences(tableId:TableId|undefined,keys:string[]) {
 const signature=JSON.stringify(keys);
 const defaults=useMemo<TableSettings>(()=>({hidden:[],order:JSON.parse(signature),sortKey:null,desc:false,pageSize:15}),[signature]);
 const [state,setState]=useState<{id:TableId|undefined;value:TableSettings;version:number;busy:boolean;error:string}>(
  {id:tableId,value:defaults,version:0,busy:!!tableId,error:''});
 const sequence=useRef(0);
 const pending=useRef(false);

 const reload=useCallback(async()=>{
  const request=++sequence.current;
  pending.current=true;
  setState(s=>({...s,id:tableId,busy:true,error:''}));
  try {
   const result=tableId?await api<TablePreference>(`/api/table-preferences/${tableId}`):null;
   if(request===sequence.current) setState({id:tableId,value:result??defaults,version:result?.version??0,busy:false,error:''});
  } catch(e) {
   if(request===sequence.current) setState(s=>({...s,busy:false,error:(e as Error).message}));
  } finally { if(request===sequence.current) pending.current=false; }
 },[tableId,defaults]);

 useEffect(()=>{
  setState({id:tableId,value:defaults,version:0,busy:!!tableId,error:''});
  void reload();
  return ()=>{++sequence.current;};
 },[reload,defaults,tableId]);

 async function save(value:TableSettings) {
  if(pending.current||state.error||state.id!==tableId) return;
  if(!tableId) { setState(s=>({...s,value}));return; }
  const request=++sequence.current;
  pending.current=true;
  setState(s=>({...s,busy:true,error:''}));
  try {
   const result=await api<TablePreference>(`/api/table-preferences/${tableId}`,'PUT',{
    hidden:value.hidden,order:value.order,sortKey:value.sortKey,desc:value.desc,pageSize:value.pageSize,version:state.version});
   if(request===sequence.current) setState({id:tableId,value:result,version:result.version,busy:false,error:''});
  } catch(e) {
   if(request===sequence.current) setState(s=>({...s,busy:false,error:(e as Error).message}));
  } finally { if(request===sequence.current) pending.current=false; }
 }
 return {settings:state.id===tableId?state.value:defaults,busy:state.busy||state.id!==tableId,
  error:state.error,save,reload,reset:()=>save(defaults)};
}
