import { useEffect,useRef,useState } from 'react';
import { ChevronDown,ChevronUp,GripHorizontal,Pin,PinOff,Save,X } from 'lucide-react';
import type {Entity,Note,NoteDraft} from './types';
export function NotePanel({entity,note,onSave,onClose}:{entity:Entity;note:Note|undefined;onSave:(draft:NoteDraft,visible:boolean,version:number)=>Promise<void>;onClose:()=>void}){
 const [projectName,setProjectName]=useState(note?.projectName??'');const [projectInfo,setProjectInfo]=useState(note?.projectInfo??'');
 const [text,setText]=useState(note?.text??'');const [busy,setBusy]=useState(false);const [error,setError]=useState('');const [saved,setSaved]=useState(false);const [offset,setOffset]=useState({x:0,y:0});const panel=useRef<HTMLElement>(null);const drag=useRef<{x:number;y:number;ox:number;oy:number}|null>(null);
 const [pinned,setPinned]=useState(false);const [collapsed,setCollapsed]=useState(false);
 useEffect(()=>{setText(note?.text??'');setProjectName(note?.projectName??'');setProjectInfo(note?.projectInfo??'');setError('');},[entity.kind,entity.key,note?.version]);
 useEffect(()=>{setSaved(false);},[entity.kind,entity.key]);
 async function save(visible:boolean){setBusy(true);setError('');try{await onSave({text,projectName,projectInfo},visible,note?.version??0);setSaved(true);if(!visible)onClose();}catch(e){setError((e as Error).message);}finally{setBusy(false);}}
 return <aside ref={panel} className="note-panel" role="dialog" aria-label={`研究备注 ${entity.key}`} style={{transform:`translate(${offset.x}px,${offset.y}px)`}}>
  <div className={'note-handle'+(pinned?' pinned':'')} onPointerDown={e=>{if(pinned||(e.target as HTMLElement).closest('button'))return;drag.current={x:e.clientX,y:e.clientY,ox:offset.x,oy:offset.y};e.currentTarget.setPointerCapture(e.pointerId);}} onPointerMove={e=>{if(!drag.current||!panel.current)return;const rect=panel.current.getBoundingClientRect();const x=drag.current.ox+e.clientX-drag.current.x;const y=drag.current.oy+e.clientY-drag.current.y;const baseLeft=rect.left-offset.x;const baseTop=rect.top-offset.y;setOffset({x:Math.min(window.innerWidth-rect.width-baseLeft-8,Math.max(8-baseLeft,x)),y:Math.min(window.innerHeight-rect.height-baseTop-8,Math.max(8-baseTop,y))});}} onPointerUp={()=>{drag.current=null;}} onPointerCancel={()=>{drag.current=null;}}><span><GripHorizontal size={16}/>研究备注 · {shortKey(entity.key)}</span><div className="tool-group"><button className="icon-button" title={pinned?'取消固定备注':'固定备注'} aria-label={pinned?'取消固定备注':'固定备注'} aria-pressed={pinned} onClick={()=>setPinned(!pinned)}>{pinned?<PinOff size={14}/>:<Pin size={14}/>}</button><button className="icon-button" title={collapsed?'展开备注':'折叠备注'} aria-label={collapsed?'展开备注':'折叠备注'} aria-expanded={!collapsed} onClick={()=>{setCollapsed(!collapsed);setOffset({x:0,y:0});}}>{collapsed?<ChevronUp size={15}/>:<ChevronDown size={15}/>}</button><button className="icon-button" title="保存并隐藏备注" aria-label="保存并隐藏备注" onClick={()=>save(false)} disabled={busy}><X size={15}/></button></div></div>
  {!collapsed&&<div className="note-body"><div className="note-entity">用户记录 · {entity.kind==='asset'?'币种':'地址'} · {entity.key}</div>
   <label htmlFor="note-project-name">项目名称</label><input id="note-project-name" maxLength={120} value={projectName} disabled={busy} onChange={e=>{setProjectName(e.target.value);setSaved(false);}}/>
   <small className="note-field-count">{projectName.length} / 120</small>
   <label htmlFor="note-project-info">项目信息</label><textarea id="note-project-info" className="note-project-info" maxLength={4000} value={projectInfo} disabled={busy} onChange={e=>{setProjectInfo(e.target.value);setSaved(false);}}/>
   <small className="note-field-count">{projectInfo.length} / 4000</small>
   <label htmlFor="note-text">自定义备注</label><textarea id="note-text" maxLength={4000} value={text} disabled={busy} onChange={e=>{setText(e.target.value);setSaved(false);}} placeholder="记录判断、证据链接与下一步…" autoFocus/>
   <div className="note-count" role="status">{text.length} / 4000 · {saved?'已保存':'更改需保存'}</div>{error&&<p role="alert" className="error-text">{error}；请保留草稿，刷新工作区后重试。</p>}<button className="primary" disabled={busy} onClick={()=>save(true)}><Save size={15}/>{busy?'保存中…':'保存备注'}</button></div>}
  {collapsed&&error&&<p role="alert" className="error-text note-save-error">{error}；草稿仍保留，请展开后重试。</p>}
 </aside>;
}
function shortKey(key:string){return key.length>16?key.slice(0,6)+'…'+key.slice(-4):key;}
