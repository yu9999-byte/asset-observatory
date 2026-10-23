import React,{createContext,useCallback,useContext,useEffect,useRef,useState} from 'react';
import { createRoot } from 'react-dom/client';
import { BrowserRouter,Link,NavLink,Route,Routes,useLocation,useNavigate,useParams,useSearchParams } from 'react-router-dom';
import { Activity,ArrowDownLeft,ArrowRight,ArrowUpRight,ArrowDownUp,BarChart3,Check,CheckCheck,ChevronLeft,Copy,Database,ExternalLink,Eye,FileText,Globe2,Layers3,ListFilter,Orbit,RefreshCw,Search,ShieldCheck,Star,StickyNote,TriangleAlert,TrendingDown,TrendingUp,Wallet,X } from 'lucide-react';
import { api,number,short,time,setAuthCredentials } from './api';
import { DataTable,type Column } from './DataTable';
import { NotePanel } from './NotePanel';
import { AddressDrawer } from './AddressDrawer';
import { transferSummary } from './transferSummary';
import type { Asset,Derivative,Entity,Envelope,Holder,Liquidation,Market,Markets,MarketQueryPage,Note,NoteDraft,Source,Transfer,Watch,Workspace } from './types';
import './style.css';

function useLoad<T>(url:string,interval=0){
 const [data,setData]=useState<T>();const [error,setError]=useState('');const [busy,setBusy]=useState(true);const serial=useRef(0);
 const reload=useCallback(async()=>{const sequence=++serial.current;setBusy(true);try{const result=await api<T>(url);if(sequence===serial.current){setData(result);setError('');}}catch(e){if(sequence===serial.current)setError((e as Error).message);}finally{if(sequence===serial.current)setBusy(false);}},[url]);
 useEffect(()=>{setData(undefined);void reload();const id=interval?setInterval(()=>void reload(),interval):undefined;return()=>{++serial.current;if(id)clearInterval(id);};},[reload,interval]);
 return {data,error,busy,reload};
}
type Context={markets:Markets|undefined;workspace:Workspace;refresh:()=>Promise<void>;toggleWatch:(entity:Entity)=>Promise<void>;openNote:(entity:Entity)=>void;marketError:string;marketBusy:boolean};
const AppContext=createContext<Context>(null!);
const useApp=()=>useContext(AppContext);
const STATUS={ok:'可用',partial:'部分覆盖',stale:'数据过期',unavailable:'来源不可用',unsupported:'未覆盖'};
function StatusPill({value}:{value:Envelope<unknown>['status']}){return <span className={`status ${value}`}><span/>{STATUS[value]}</span>;}
function SourceInfo({value}:{value:Envelope<unknown>|undefined}){
 if(!value)return null;
 return <details className="source-info"><summary><Database size={14}/><span>{value.source}</span><StatusPill value={value.status}/><span className="source-time">抓取 {value.fetchedAt?time(value.fetchedAt):'未成功'} · UTC+8</span></summary><div><p>{value.coverage}</p>{value.reason&&<p className="warning-text">{value.reason}</p>}<p>源更新时间：{time(value.sourceAt)} · 最近成功抓取：{time(value.fetchedAt)}</p>{value.sourceUrl&&<a href={value.sourceUrl} target="_blank" rel="noreferrer">原始来源 <ExternalLink size={12}/></a>}</div></details>;
}
function CopyButton({value}:{value:string}){const [copied,setCopied]=useState(false);return <button className="icon-button copy-button" aria-label={`复制 ${value}`} title={copied?'已复制':'复制完整值'} onClick={async()=>{try{await navigator.clipboard.writeText(value);setCopied(true);setTimeout(()=>setCopied(false),1500);}catch{setCopied(false);}}}>{copied?<Check size={14}/>:<Copy size={14}/>}</button>;}
function notePreview(note:Note|undefined){return note?[note.projectName&&'项目名称：'+note.projectName,note.projectInfo&&'项目信息：'+note.projectInfo,note.text&&'自定义备注：'+note.text].filter(Boolean).join('\n')||'研究备注':'研究备注';}
function EntityActions({entity}:{entity:Entity}){const app=useApp();const [error,setError]=useState('');const [busy,setBusy]=useState(false);const watched=app.workspace.watches.some(w=>w.kind===entity.kind&&w.key.toLowerCase()===entity.key.toLowerCase());return <div className="entity-actions"><button className={`icon-button ${watched?'watched':''}`} aria-label={`${watched?'取消关注':'关注'} ${entity.key}`} aria-pressed={watched} disabled={busy} title={watched?'取消关注':'加入关注'} onClick={async()=>{setBusy(true);try{await app.toggleWatch(entity);setError('');}catch(e){setError((e as Error).message);}finally{setBusy(false);}}}><Star size={17} fill={watched?'currentColor':'none'}/></button><button className="icon-button" title={notePreview(app.workspace.notes.find(n=>n.kind===entity.kind&&n.key.toLowerCase()===entity.key.toLowerCase()))} aria-label={`备注 ${entity.key}`} onClick={()=>app.openNote(entity)}><StickyNote size={17}/></button>{error&&<span role="alert" className="inline-error">{error}</span>}</div>;}
function AssetMark({asset}:{asset:Asset}){return <span className={`asset-mark ${asset.id.toLowerCase()}`}>{asset.symbol==='BTC'?'₿':asset.symbol==='ETH'?'Ξ':asset.symbol.slice(0,1)}</span>;}
function PageHeading({eyebrow,title,description,action}:{eyebrow:string;title:React.ReactNode;description?:string;action?:React.ReactNode}){return <div className="page-heading"><div><div className="eyebrow">{eyebrow}</div><h1>{title}</h1>{description&&<p>{description}</p>}</div>{action}</div>;}
function ErrorBox({message}:{message:string}){return <p className="error-box" role="alert"><TriangleAlert size={17}/>{message}</p>;}
function ProviderPager({next,cursor,onNext,onPrevious,busy}:{next:string|null|undefined;cursor:string[];onNext:()=>void;onPrevious:()=>void;busy:boolean}){return <div className="provider-pager"><span>索引器第 {cursor.length+1} 页 · 游标分页不保证快照一致</span><button disabled={!cursor.length||busy} onClick={onPrevious}><ChevronLeft size={15}/>前一批</button><button disabled={!next||busy} onClick={onNext}>下一批<ArrowRight size={15}/></button></div>;}
function AddressLookup({asset='LINK',onPreview}:{asset?:string;onPreview?:(address:string)=>void}){const [address,setAddress]=useState('');const [error,setError]=useState('');const navigate=useNavigate();return <form className="address-lookup" onSubmit={e=>{e.preventDefault();if(!/^0x[0-9a-fA-F]{40}$/.test(address)){setError('请输入完整的 Ethereum 地址');return;}if(onPreview)onPreview(address.toLowerCase());else navigate(`/addresses/${address.toLowerCase()}?asset=${encodeURIComponent(asset)}`);setError('');}}><label><Search size={17}/><input aria-label="查询 Ethereum 地址" placeholder="输入 Ethereum 地址，查看转账事件" value={address} onChange={e=>setAddress(e.target.value.trim())}/></label><button type="submit" title="查询地址" aria-label="查询地址"><ArrowRight size={18}/></button>{error&&<span role="alert" className="lookup-error">{error}</span>}</form>;}
function MissingValue({reason='来源未提供'}:{reason?:string}){return <span className="missing-value" title={reason}>—</span>;}
function SummaryStrip({items,testId}:{items:{label:string;value:React.ReactNode;detail?:string}[];testId?:string}){
 return <div className="summary-strip" data-testid={testId}>{items.map(item=><div key={item.label}><span>{item.label}</span><strong>{item.value}</strong>{item.detail&&<small>{item.detail}</small>}</div>)}</div>;
}
function FilterTabs({value,onChange,items,label='筛选'}:{value:string;onChange:(value:string)=>void;items:{key:string;label:string;disabled?:boolean;reason?:string}[];label?:string}){
 return <div className="filter-tabs" role="group" aria-label={label}>{items.map(item=><span key={item.key} title={item.reason}><button type="button" aria-pressed={value===item.key} disabled={item.disabled} className={value===item.key?'active':''} onClick={()=>onChange(item.key)}>{item.label}</button></span>)}</div>;
}
function WatchRail({asset='LINK'}:{asset?:string}){
 const {workspace}=useApp();const watches=workspace.watches.filter(w=>w.kind==='address');
 return <details className="watch-rail" open={watches.length>0}><summary><Star size={14}/><strong>关注地址</strong><span>{watches.length}</span><small>收起 / 展开</small></summary><div>{watches.length?watches.map(w=><Link className="watch-chip" to={'/addresses/'+w.key+'?asset='+asset} key={w.key}><span className="mono">{short(w.key)}</span><ArrowUpRight size={12}/></Link>):<span className="muted">暂无关注地址</span>}<Link className="text-link rail-manage" to="/watchlist">管理关注<ArrowRight size={13}/></Link></div></details>;
}
function RankingSection({title,icon,detail,reason}:{title:string;icon:React.ReactNode;detail:string;reason:string}){
 return <section className="ranking-section"><div className="section-heading"><h2>{icon}{title}</h2><span className="muted">{detail}</span></div><div className="ranking-head"><span>币种</span><span>{title==='爆仓排行'?'多空爆仓 · USD':'变化'}</span></div><div className="research-empty"><Database size={17}/><strong>未覆盖</strong><span>{reason}</span></div></section>;
}
function DerivativesPanel({id,liquidations}:{id:string;liquidations:ReturnType<typeof useLoad<Envelope<Liquidation[]>>>}){
 const {markets}=useApp();const [view,setView]=useState('venues');
 const row=markets?.derivatives.data.find(d=>d.assetId===id);
 const columns:Column<Liquidation>[]=[{key:'time',title:'事件时间 · UTC+8',value:r=>r.sourceAt,render:r=>time(r.sourceAt)},{key:'type',title:'事件类型',value:r=>r.eventType},{key:'side',title:'方向',value:r=>r.side},{key:'size',title:'数量 · 单位待核实',value:r=>r.sizeRaw,numeric:true,render:r=>number(r.sizeRaw,8)},{key:'price',title:'执行价格 · USD',value:r=>r.price,numeric:true,render:r=>number(r.price,4)},{key:'sequence',title:'来源序列',value:r=>r.sequenceId}];
 return <section className="section"><div className="section-heading"><h2><BarChart3 size={17}/>合约市场对照</h2><FilterTabs label="合约数据视图" value={view} onChange={setView} items={[{key:'venues',label:'交易所分布'},{key:'events',label:'爆仓事件'}]}/></div>{view==='venues'?<><div className="venue-table table-scroll"><table aria-label="交易所合约数据"><thead><tr>{['来源 / 合约','持仓 OI · 原始单位','24H 成交量','资金费率','多头爆仓 · USD','空头爆仓 · USD'].map(h=><th key={h}>{h}</th>)}</tr></thead><tbody>{row?<tr><td><strong>{markets?.derivatives.source}</strong><small className="cell-detail">{row.instrument}</small></td><td className="numeric">{number(row.openInterestRaw,2)}<small className="cell-detail">{row.openInterestUnit}</small></td>{['volume','funding','long','short'].map(k=><td className="numeric" key={k}><MissingValue/></td>)}</tr>:<tr><td colSpan={6}><div className="inline-missing"><Database size={16}/><span>{id} 合约映射或来源未覆盖</span></div></td></tr>}</tbody></table></div><SourceInfo value={markets?.derivatives}/><p className="table-note">OI 属于币种的交易所市场。单位未核实时保留原始值，不与其他交易所相加；空爆仓样本不等于爆仓为零。</p></>:<>{liquidations.error&&<ErrorBox message={liquidations.error}/>}<SourceInfo value={liquidations.data}/><DataTable preferenceId="liquidations" label="爆仓样本" rows={liquidations.data?.data??[]} columns={columns} rowKey={r=>r.id} empty={liquidations.busy?'正在读取来源状态…':liquidations.data?.reason??'当前窗口未观察到样本'}/><p className="table-note">有限事件窗口，不代表 24H 或全市场总额。</p></>}</section>;
}
function MarketsPage(){
 const {markets,marketError,marketBusy,workspace}=useApp();const [scope,setScope]=useState('all');const [queryPage,setQueryPage]=useState<MarketQueryPage>();const [queryError,setQueryError]=useState('');const [queryBusy,setQueryBusy]=useState(false);
 const loadQuery=useCallback(async(q:string,sortKey:string|null,desc:boolean,limit:number)=>{const sortMap:Record<string,string>={asset:'assetId',price:'lastPrice',change:'change24hPct',volume:'volume24hBase',turnover:'turnover24hQuote'};const params=new URLSearchParams();if(q.trim())params.set('q',q.trim());params.set('sort',sortMap[sortKey??'asset']??'assetId');params.set('direction',desc?'desc':'asc');params.set('limit',String(limit));setQueryBusy(true);try{const result=await api<MarketQueryPage>('/api/markets/query?'+params.toString());setQueryPage(result);setQueryError('');}catch(e){setQueryError((e as Error).message);}finally{setQueryBusy(false);}},[]);
 const queryMarkets=queryPage?.data??markets?.market.data??[];const queryIds=new Set(queryMarkets.map(m=>m.assetId));
 const rows=(markets?.assets??[]).filter(a=>queryIds.has(a.id)&&(scope==='all'||workspace.watches.some(w=>w.kind==='asset'&&w.key===a.id))).map(asset=>({asset,market:queryMarkets.find(m=>m.assetId===asset.id),derivative:markets?.derivatives.data.find(d=>d.assetId===asset.id)}));
 type Row=typeof rows[number];
 const columns:Column<Row>[]=[
  {key:'asset',title:'资产',pinned:true,value:r=>r.asset.symbol+' '+r.asset.name,render:r=><Link className="asset-cell" to={'/assets/'+r.asset.id}><AssetMark asset={r.asset}/><span><strong>{r.asset.symbol}</strong><small>{r.asset.name}</small></span></Link>},
  {key:'price',title:'价格 · USD',value:r=>r.market?.lastPrice,numeric:true,className:'numeric',render:r=><span className="price-value">{number(r.market?.lastPrice,4)}</span>},
  {key:'change',title:'24h 涨跌',value:r=>r.market?.change24hPct,numeric:true,className:'numeric',render:r=>r.market?.change24hPct!=null?<span className={r.market.change24hPct.startsWith('-')?'change-down':'change-up'}>{r.market.change24hPct.startsWith('-')?'':'+'}{number(r.market.change24hPct,2)}%</span>:<MissingValue reason="现货源未提供滚动24小时开盘价"/>},
  {key:'marketCap',title:'流通市值',value:()=>null,numeric:true,className:'numeric',render:()=> <MissingValue reason="流通供应量与价格来源未齐"/>},
  {key:'fdv',title:'FDV',value:()=>null,numeric:true,className:'numeric',render:()=> <MissingValue reason="总供应量与价格来源未齐"/>},
  {key:'volume',title:'24h 成交量 · 币',value:r=>r.market?.volume24hBase,numeric:true,className:'numeric',render:r=>number(r.market?.volume24hBase,2)},
  {key:'turnover',title:'24h 估算成交额 · USD',value:r=>r.market?.turnover24hQuote,numeric:true,className:'numeric',render:r=><span title="来源 VWAP × 基础币成交量，仅为估算">{number(r.market?.turnover24hQuote,0)}</span>},
  {key:'oi',title:'合约 OI · 原始值',value:r=>r.derivative?.openInterestRaw,numeric:true,className:'numeric',render:r=><span title={r.derivative?.openInterestUnit??'未覆盖'}>{number(r.derivative?.openInterestRaw,2)}</span>},
  {key:'funding',title:'资金费率',value:()=>null,numeric:true,className:'numeric',render:()=> <MissingValue reason="资金费率来源尚未接入"/>},
  {key:'coverage',title:'持仓覆盖',value:r=>r.asset.chain,render:r=>r.asset.contract?<Link className="text-link" to={'/assets/'+r.asset.id}>Ethereum<ArrowUpRight size={12}/></Link>:<span className="muted">未覆盖</span>},
  {key:'actions',title:'研究',value:r=>r.asset.id,render:r=><EntityActions entity={{kind:'asset',key:r.asset.id}}/>}
 ];
 return <><PageHeading eyebrow="MARKET OVERVIEW" title="行情总览"/>
 {marketError&&<ErrorBox message={marketError}/>} {queryError&&<ErrorBox message={queryError}/>}
 <section className="section"><div className="section-heading"><div className="section-title-group"><h2><Layers3 size={17}/>资产行情</h2><FilterTabs label="行情范围" value={scope} onChange={setScope} items={[{key:'all',label:'全部'},{key:'watched',label:'已关注'}]}/></div><span className="muted section-meta">USD 报价 · 30s 轮询目标</span></div>
 <DataTable preferenceId="markets" label="资产行情" rows={rows} columns={columns} rowKey={r=>r.asset.id} onServerQuery={loadQuery} showResultSummary={false} empty={marketBusy||queryBusy?'正在加载资产目录…':'暂无符合条件的资产'}/>
 </section>
 <div className="research-grid"><RankingSection title="涨跌幅排行" icon={<TrendingUp size={17}/>} detail="24H" reason="滚动涨跌数据未覆盖"/><RankingSection title="成交量异动" icon={<BarChart3 size={17}/>} detail="24H / 历史" reason="历史对比窗口未覆盖"/><RankingSection title="持仓变化" icon={<ArrowDownUp size={17}/>} detail="合约 OI" reason="OI 历史窗口未覆盖"/><RankingSection title="爆仓排行" icon={<TrendingDown size={17}/>} detail="多 / 空" reason="完整窗口与市场覆盖未齐"/></div>
 <section className="section"><div className="section-heading"><h2><Activity size={17}/>重点地址异动</h2><Link className="text-link" to="/watchlist">关注工作区<ArrowRight size={14}/></Link></div><AddressLookup/><div className="inline-missing"><Activity size={17}/><span>异动统计未覆盖</span><small>需连续事件窗口与来源标签</small></div></section></>;
}
type HolderRow=Omit<Holder,'balance'|'rawBalance'|'isContract'> & {balance:string|null;rawBalance:string|null;isContract:boolean|null};
function AssetPage(){
 const {id=''}=useParams();const app=useApp();const asset=app.markets?.assets.find(a=>a.id===id);const [cursors,setCursors]=useState<string[]>([]);const [watched,setWatched]=useState('all');const [previewAddress,setPreviewAddress]=useState<string|null>(null);const [lookupOpen,setLookupOpen]=useState(false);
 const closePreview=useCallback(()=>setPreviewAddress(null),[]);
 useEffect(()=>{setCursors([]);setWatched('all');setPreviewAddress(null);setLookupOpen(false);},[id]);
 const cursor=cursors.at(-1);const loaded=useLoad<Envelope<Holder[]>>('/api/assets/'+encodeURIComponent(id)+'/holders'+(cursor?'?cursor='+encodeURIComponent(cursor):''),60000);
 const liquidations=useLoad<Envelope<Liquidation[]>>('/api/assets/'+encodeURIComponent(id)+'/liquidations',60000);
 const market=app.markets?.market.data.find(m=>m.assetId===id);const derivative=app.markets?.derivatives.data.find(d=>d.assetId===id);
 const addressWatches=app.workspace.watches.filter(w=>w.kind==='address');
 const sourceRows=loaded.data?.data??[];
 const customRows:HolderRow[]=addressWatches.map(w=>sourceRows.find(r=>r.address.toLowerCase()===w.key.toLowerCase())??{address:w.key,balance:null,rawBalance:null,decimals:asset?.decimals??18,isContract:null});
 const rows=watched==='watched'?customRows:sourceRows;
 const columns:Column<HolderRow>[]=[
  {key:'address',title:'持仓地址 / 标签',pinned:true,value:r=>r.address,render:r=><div className="address-cell"><button className="mono text-link address-preview-link" aria-label={'查看地址 '+r.address} aria-haspopup="dialog" title={r.address} onClick={()=>setPreviewAddress(r.address)}>{short(r.address)}</button><CopyButton value={r.address}/>{r.balance===null&&<small>当前页未覆盖</small>}</div>},
  {key:'chain',title:'链',value:()=>asset?.chain,render:()=>asset?.chain==='ethereum'?'Ethereum':asset?.chain??'—'},
  {key:'balance',title:'数量 · '+id,value:r=>r.balance,numeric:true,className:'numeric',render:r=>number(r.balance,8)},
  {key:'proportion',title:'持仓占比',value:()=>null,numeric:true,className:'numeric',render:()=> <MissingValue reason="分母未接入；不将缺失占比画成0%"/>},
  {key:'value',title:'USD 估值',value:()=>null,numeric:true,className:'numeric',render:()=> <MissingValue reason="估值需要与持仓快照匹配的报价及时间"/>},
  ...(['1H','24H','3D','7D','1M','1Y'] as const).map(horizon=>({key:'change-'+horizon,title:horizon+' 数量变化',value:()=>null,numeric:true,className:'numeric',render:()=> <MissingValue reason="该周期持仓历史未覆盖；数量变化不同于USD估值变化"/>})),
  {key:'type',title:'身份',value:r=>r.isContract===null?null:r.isContract?'合约':'未标记',render:r=><span className="neutral-tag">{r.isContract===null?'未知':r.isContract?'合约':'未标记'}</span>},
  {key:'actions',title:'研究',value:r=>r.address,render:r=><EntityActions entity={{kind:'address',key:r.address}}/>}
 ];
 return <><Link className="back-link" to="/"><ChevronLeft size={14}/>行情首页</Link><PageHeading eyebrow="TOKEN HOLDERS" title={<span className="holder-page-title">{asset&&<AssetMark asset={asset}/>}<span>{id+' · 持仓地址'}</span></span>} description={asset?.name} action={<EntityActions entity={{kind:'asset',key:id}}/>}/>
 {asset?.contract&&<div className="holder-contract"><span className="mono">{asset.contract}</span><CopyButton value={asset.contract}/></div>}
 <SummaryStrip testId="asset-summary-metrics" items={[{label:'最新价格 · USD',value:number(market?.lastPrice,4),detail:app.markets?.market.source??'行情未加载'},{label:'流通市值 / FDV',value:<MissingValue reason="供应量来源未接入"/>,detail:'供应量未覆盖'},{label:'24H 成交量 · 币',value:number(market?.volume24hBase,2),detail:id},{label:'合约 OI · 原始值',value:number(derivative?.openInterestRaw,2),detail:derivative?.openInterestUnit??'未覆盖'}]}/>
 <section className="section"><div className="section-heading"><div className="section-title-group"><h2><Wallet size={17}/>持仓地址</h2><FilterTabs label="地址关注范围" value={watched} onChange={setWatched} items={[{key:'all',label:'全部'},{key:'watched',label:'已关注'}]}/></div><button className="text-link" aria-expanded={lookupOpen} aria-controls="holder-address-lookup" onClick={()=>setLookupOpen(!lookupOpen)}><Search size={15}/>查地址</button></div>
 {lookupOpen&&<div className="holder-address-lookup" id="holder-address-lookup"><AddressLookup asset={id} onPreview={setPreviewAddress}/></div>}
 {loaded.error&&<ErrorBox message={loaded.error}/>}
 <DataTable preferenceId="holders" label="持仓地址" rows={rows} columns={columns} rowKey={r=>r.address} showExport={false} searchKeys={['address']} searchPlaceholder="搜索地址…" empty={watched==='watched'?'暂无已关注地址':loaded.busy?'正在读取索引器…':loaded.data?.reason??'暂无持仓记录'}/>
 {watched==='all'&&<ProviderPager next={loaded.data?.nextCursor} cursor={cursors} busy={loaded.busy} onNext={()=>loaded.data?.nextCursor&&setCursors([...cursors,loaded.data.nextCursor])} onPrevious={()=>setCursors(cursors.slice(0,-1))}/>}
 <SourceInfo value={loaded.data}/></section>
 <DerivativesPanel id={id} liquidations={liquidations}/>{previewAddress&&<AddressDrawer key={id+previewAddress} address={previewAddress} asset={id} onClose={closePreview}/>}</>;
}
function AddressesPage(){
 const app=useApp();const [search,setSearch]=useSearchParams();const asset=search.get('asset')??'ETH';
 const choices=(app.markets?.assets??[]).filter(item=>item.chain==='ethereum'||item.id==='ETH');
 const watches=app.workspace.watches.filter(watch=>watch.kind==='address');
 return <><PageHeading eyebrow="ADDRESS RESEARCH" title="地址查询"/>
 <section className="section address-search-section"><div className="section-heading"><h2><Search size={17}/>查询地址详情</h2><label className="select-label">代币<select aria-label="查询代币" value={asset} onChange={event=>setSearch({asset:event.target.value})}>{!choices.some(item=>item.id===asset)&&<option value={asset}>{asset}</option>}{choices.map(item=><option key={item.id} value={item.id}>{item.symbol}</option>)}</select></label></div><AddressLookup asset={asset}/></section>
 <section className="section"><div className="section-heading"><h2><Star size={17}/>关注地址</h2></div>{watches.length?<ul className="address-entry-list">{watches.map(watch=><li key={watch.key}><Wallet size={17}/><Link to={'/addresses/'+watch.key+'?asset='+encodeURIComponent(asset)} className="mono text-link" title={watch.key}>{watch.key}<ArrowRight size={15}/></Link><CopyButton value={watch.key}/></li>)}</ul>:<div className="empty-line">暂无关注地址</div>}</section></>;
}
function AddressPage(){
 const {address=''}=useParams();const [search,setSearch]=useSearchParams();const app=useApp();const asset=search.get('asset')??'LINK';const [cursors,setCursors]=useState<string[]>([]);const [direction,setDirection]=useState('all');const [period,setPeriod]=useState('loaded');
 useEffect(()=>{setCursors([]);setDirection('all');},[asset,address]);
 const cursor=cursors.at(-1);
 const loaded=useLoad<Envelope<Transfer[]>>('/api/addresses/'+encodeURIComponent(address)+'/transfers?asset='+encodeURIComponent(asset)+(cursor?'&cursor='+encodeURIComponent(cursor):''),60000);
 const sourceRows=loaded.data?.data??[];const transfers=sourceRows.filter(row=>direction==='all'||row.direction===direction);
 const totals=transferSummary(sourceRows);const hasSample=!!sourceRows.length;const currentBatch=period==='loaded';
 const totalValue=(value:string,tone:string)=>currentBatch&&hasSample?<span className={tone}>{number(value,8)}</span>:<MissingValue reason={currentBatch?'当前批次无可用事件':'统计窗口未完整覆盖'}/>;
 const addressCell=(value:string)=><div className={'address-cell '+(value.toLowerCase()===address.toLowerCase()?'current-address':'')}><Link className="mono text-link" title={value} to={'/addresses/'+value+'?asset='+asset}>{short(value)}</Link>{value.toLowerCase()===address.toLowerCase()&&<span className="neutral-tag">当前地址</span>}<CopyButton value={value}/></div>;
 const columns:Column<Transfer>[]=[
  {key:'time',title:'区块时间 · UTC+8',pinned:true,value:r=>r.sourceAt,render:r=>time(r.sourceAt)},
  {key:'direction',title:'方向',value:r=>r.direction,render:r=><span className={'direction '+r.direction.toLowerCase()}>{r.direction==='IN'?<ArrowDownLeft size={13}/>:r.direction==='OUT'?<ArrowUpRight size={13}/>:<ArrowDownUp size={13}/>}{{IN:'转入',OUT:'转出',SELF:'自转'}[r.direction]}</span>},
  {key:'from',title:'From',value:r=>r.from,render:r=>addressCell(r.from)},
  {key:'to',title:'To',value:r=>r.to,render:r=>addressCell(r.to)},
  {key:'amount',title:'数量 · '+asset,value:r=>r.amount,numeric:true,className:'numeric',render:r=><span className={'transfer-amount '+(r.direction==='IN'?'change-up':r.direction==='OUT'?'change-down':'')} title={r.amount+' '+asset}>{r.direction==='IN'?'+':r.direction==='OUT'?'−':''}{number(r.amount,8)}<span className="transfer-token">{asset}</span></span>},
  {key:'price',title:'事件价格 · USD',value:()=>null,numeric:true,className:'numeric',render:()=> <MissingValue reason="历史事件价格未接入，不使用现价代替"/>},
  {key:'value',title:'事件金额 · USD',value:()=>null,numeric:true,className:'numeric',render:()=> <MissingValue reason="历史事件价格缺失，金额不可计算"/>},
  {key:'counterparty',title:'对手方标签',value:()=>null,render:()=> <span className="muted">未标记</span>},
  {key:'hash',title:'交易 / 日志',value:r=>r.hash+':'+r.logIndex,render:r=><div className="address-cell"><a className="mono text-link" href={'https://eth.blockscout.com/tx/'+r.hash} target="_blank" rel="noreferrer">{short(r.hash)} : {r.logIndex}<ExternalLink size={12}/></a><CopyButton value={r.hash}/></div>},
  {key:'block',title:'区块',value:r=>String(r.blockNumber),numeric:true,className:'numeric'}
 ];
 return <><Link className="back-link" to={'/assets/'+asset}><ChevronLeft size={14}/>返回 {asset} 持仓地址</Link><PageHeading eyebrow="ADDRESS TRANSFERS" title="地址转账事件" description="查看当前地址的代币流入、流出与链上转账明细" action={<EntityActions entity={{kind:'address',key:address}}/>}/>
 <div className="address-banner"><Wallet size={22}/><div><span className="mono">{address}</span><small>监控地址 · Ethereum · {asset}</small></div><CopyButton value={address}/><a className="icon-button" aria-label="在区块浏览器查看地址" title="在区块浏览器查看地址" href={'https://eth.blockscout.com/address/'+encodeURIComponent(address)} target="_blank" rel="noreferrer"><ExternalLink size={16}/></a></div>
 <section className="flow-summary"><div className="section-heading"><h2><ArrowDownUp size={17}/>资金流统计</h2><FilterTabs label="统计周期" value={period} onChange={setPeriod} items={[{key:'loaded',label:'本批次'},...['24H','3D','7D'].map(p=>({key:p,label:p}))]}/></div>
 <SummaryStrip testId="address-flow-metrics" items={[{label:(currentBatch?'本批次':period)+' 累计转入',value:totalValue(totals.incoming,'change-up'),detail:asset},{label:(currentBatch?'本批次':period)+' 累计转出',value:totalValue(totals.outgoing,'change-down'),detail:asset},{label:(currentBatch?'本批次':period)+' 净流量',value:totalValue(totals.net,totals.net.startsWith('-')?'change-down':'change-up'),detail:asset},{label:'本批次转账次数',value:hasSample?String(sourceRows.length):<MissingValue reason="当前批次无可用事件"/>,detail:'包含自转；自转不计入流入、流出'}]}/>
 <p className="stat-coverage"><TriangleAlert size={13}/><span>{currentBatch?'仅统计当前来源批次':'统计窗口未完整覆盖'}</span><small>翻批后重新统计，不代表地址完整历史或周期总量</small></p></section>
 <section className="section address-transfers"><div className="section-heading"><h2><Activity size={17}/>转账事件<span className="event-count" title="筛选后当前批次的事件数">{transfers.length}</span></h2><div className="tool-group"><label className="select-label">代币<select aria-label="事件代币" value={asset} onChange={e=>setSearch({asset:e.target.value})}>{!app.markets?.assets.some(a=>a.id===asset&&a.contract)&&<option value={asset}>{asset}</option>}{app.markets?.assets.filter(a=>a.contract).map(a=><option key={a.id}>{a.id}</option>)}</select></label><button className="icon-button" title="刷新事件" aria-label="刷新事件" onClick={()=>void loaded.reload()} disabled={loaded.busy}><RefreshCw size={16} className={loaded.busy?'spinning':''}/></button></div></div>
 <div className="filter-row transfer-filter-row"><FilterTabs label="事件方向" value={direction} onChange={setDirection} items={[{key:'all',label:'全部转账'},{key:'IN',label:'转入'},{key:'OUT',label:'转出'},{key:'swap',label:'交换',disabled:true,reason:'当前来源仅提供代币转账日志，尚未接入可验证的交换事件解析'}]}/><FilterTabs label="对手方" value="all" onChange={()=>{}} items={[{key:'all',label:'全部对手方'},{key:'exchange',label:'交易所对手方',disabled:true,reason:'交易所归属缺少可追溯标签来源'}]}/></div>
 {loaded.error&&<ErrorBox message={loaded.error}/>}<DataTable preferenceId="transfers" label="转账事件" rows={transfers} columns={columns} rowKey={r=>r.id} empty={loaded.busy?'正在读取转账事件…':loaded.data?.reason??'没有符合筛选的已加载事件'}/>
 <ProviderPager next={loaded.data?.nextCursor} cursor={cursors} busy={loaded.busy} onNext={()=>loaded.data?.nextCursor&&setCursors([...cursors,loaded.data.nextCursor])} onPrevious={()=>setCursors(cursors.slice(0,-1))}/><SourceInfo value={loaded.data}/><p className="table-note">历史事件价格缺失时保留 —；不将转账解释为买卖。日志去重使用交易哈希 + 日志索引，链最终性仍待验证。</p></section></>;
}
function WatchlistPage(){const app=useApp();const columns:Column<Watch>[]=[{key:'kind',title:'类型',value:r=>r.kind,render:r=>r.kind==='asset'?'币种':'地址'},{key:'key',title:'研究对象',value:r=>r.key,render:r=><Link className="mono text-link" to={r.kind==='asset'?`/assets/${r.key}`:`/addresses/${r.key}?asset=LINK`}>{r.key}<ArrowUpRight size={14}/></Link>},{key:'created',title:'关注时间 · UTC+8',value:r=>r.createdAt,render:r=>time(r.createdAt)},{key:'action',title:'操作',value:r=>r.key,render:r=><EntityActions entity={r}/>}];return <><PageHeading eyebrow="RESEARCH WORKSPACE" title="关注工作区" description="关注与备注保存在服务端工作区，重启后继续研究。每类最多关注20项。"/><AddressLookup/><section className="section"><div className="section-heading"><h2><Star size={19}/>关注对象</h2></div><DataTable preferenceId="watches" label="关注对象" rows={app.workspace.watches} columns={columns} rowKey={r=>r.kind+':'+r.key} empty="尚未关注研究对象"/></section><section className="section"><div className="section-heading"><h2><StickyNote size={19}/>研究备注</h2></div>{app.workspace.notes.length?<div className="notes-list">{app.workspace.notes.map(n=><button key={n.kind+n.key} title={notePreview(n)} onClick={()=>app.openNote(n)}><span><FileText size={16}/><strong>{n.projectName||short(n.key)}</strong><small>{n.visible?'显示中':'已隐藏'} · v{n.version}</small></span>{n.projectName&&<p className="mono">{n.kind==='asset'?'币种':'地址'} · {short(n.key)}</p>}{n.projectInfo&&<p>{n.projectInfo}</p>}<p>{n.text||'空备注'}</p></button>)}</div>:<div className="empty-line">尚无研究备注。点击币种或地址旁的备注图标开始记录。</div>}</section></>;}
function SourcesPage(){
 const loaded=useLoad<Source[]>('/api/sources');
 const transportLabels={reachable:'实测可达',unreachable:'本轮未连通',not_verified:'未验证'};
 return <><PageHeading eyebrow="DATA PROVENANCE" title="数据来源与覆盖" description="可访问不等于允许再分发。这里公开每个来源的启用状态、限制与生产解锁条件。"/>
 <div className="release-warning"><ShieldCheck size={20}/><div><strong>生产验收尚未通过</strong><p>链上稳定来源、爆仓覆盖及第三方展示许可仍存在缺口。当前构建用于本机研究验证。</p></div></div>
 {loaded.error&&<ErrorBox message={loaded.error}/>}
 <section className="section source-register">{loaded.data?.map(source=><article key={source.name}>
  <div><Database size={20}/><h2>{source.name}</h2><span className={`status ${source.enabled?'partial':'unavailable'}`}><span/>{source.enabled?'已启用':'未启用'}</span></div>
  <p>{source.gap}</p>
  {source.verification&&<dl className="definitions">
   <dt>连通记录</dt><dd><span>{transportLabels[source.verification.transportStatus]}</span></dd>
   <dt>最近复核</dt><dd><time dateTime={source.verification.checkedAt}>{new Date(source.verification.checkedAt).toLocaleString('zh-CN',{timeZone:'Asia/Shanghai',hour12:false})}</time> UTC+8</dd>
   <dt>实测范围</dt><dd>{source.verification.scope}</dd>
   <dt>使用条件</dt><dd>{source.verification.rightsStatus}</dd>
   <dt>验证记录</dt><dd>{source.verification.evidenceId}</dd>
   <dt>状态说明</dt><dd>以上为构建时留存的验证记录，不代表实时健康状态或生产批准。</dd>
  </dl>}
  <div><a className="text-link" href={source.documentation} target="_blank" rel="noreferrer">官方文档 <ExternalLink size={13}/></a>{source.terms?<a className="text-link" href={source.terms} target="_blank" rel="noreferrer">使用条款 <ExternalLink size={13}/></a>:<span className="muted">使用条款：未核验</span>}<span className="muted">生产批准：{source.productionApproved?'是':'否'}</span></div>
 </article>)}</section>
 <section className="section"><div className="section-heading"><h2>指标口径</h2></div><dl className="definitions"><dt>价格 / 量</dt><dd>单交易所现货 USD 报价及24小时基础币成交量；VWAP×数量仅为估算成交额。</dd><dt>24h 涨跌</dt><dd>现货源没有滚动24h开盘，保持缺失；不使用UTC日开盘代替。</dd><dt>合约 OI</dt><dd>特定永续合约的来源报告值，单位待规格核实。不等于账户持仓或全市场总量。</dd><dt>持仓 / 事件</dt><dd>固定 Ethereum 合约、分页索引。来源时间、最终性未知时明确显示未知。</dd><dt>爆仓</dt><dd>仅展示可核验的有限事件样本。不将空样本推导为24小时爆仓为零。</dd><dt>时效</dt><dd>行情目标30秒刷新，90秒标记过期；链上60秒刷新，180秒标记过期。均不是上游SLA。</dd></dl></section></>;
}
function ResearchApp(){
 const market=useLoad<Markets>('/api/markets',30000);const [workspace,setWorkspace]=useState<Workspace>({watches:[],notes:[]});const [workspaceReady,setWorkspaceReady]=useState(false);const [workspaceError,setWorkspaceError]=useState('');const [noteEntity,setNoteEntity]=useState<Entity|null>(null);const location=useLocation();
 const loadWorkspace=useCallback(async()=>{try{setWorkspace(await api<Workspace>('/api/workspace'));setWorkspaceReady(true);setWorkspaceError('');}catch(e){setWorkspaceError((e as Error).message);}},[]);
 useEffect(()=>{void loadWorkspace();},[loadWorkspace]);
 // Restore panels on navigation/initial hydration; saving must not close or retarget an editor.
 useEffect(()=>{const match=location.pathname.match(/^\/(assets|addresses)\/([^/]+)$/);if(!match)return;const kind=match[1]==='assets'?'asset':'address';const note=workspace.notes.find(n=>n.kind===kind&&n.key.toLowerCase()===decodeURIComponent(match[2]).toLowerCase()&&n.visible);setNoteEntity(note?{kind,key:note.key}:null);},[location.pathname,workspaceReady]);
 async function toggleWatch(entity:Entity){const existing=workspace.watches.some(w=>w.kind===entity.kind&&w.key.toLowerCase()===entity.key.toLowerCase());await api(`/api/watches/${entity.kind}/${encodeURIComponent(entity.key)}`,existing?'DELETE':'PUT');await loadWorkspace();}
 async function refresh(){await Promise.all([market.reload(),loadWorkspace()]);}
 async function saveNote(draft:NoteDraft,visible:boolean,version:number){if(!noteEntity)return;const note=await api<Note>(`/api/notes/${noteEntity.kind}/${encodeURIComponent(noteEntity.key)}`,'PUT',{...draft,visible,version});setWorkspace(w=>({...w,notes:[...w.notes.filter(n=>n.kind!==note.kind||n.key!==note.key),note]}));}
 const value:Context={markets:market.data,workspace,refresh,toggleWatch,openNote:setNoteEntity,marketError:market.error,marketBusy:market.busy};
return <AppContext.Provider value={value}><header className="app-header"><Link className="brand" to="/"><Orbit size={24}/><strong>Asset Observatory</strong><span>研究终端</span></Link><nav aria-label="主导航"><NavLink end to="/"><Layers3 size={16}/>市场</NavLink><NavLink to="/addresses"><Wallet size={16}/>地址</NavLink><NavLink to="/watchlist"><Star size={16}/>关注</NavLink><NavLink to="/sources"><Database size={16}/>来源</NavLink></nav><div className="header-mode"><span className="local-dot"/>LOCAL <span className="header-divider"/>UTC+8</div></header><div className="application-bar"><span><Globe2 size={13}/>公开数据研究 · 无资金操作</span><Link to="/sources"><TriangleAlert size={13}/ >查看覆盖与发布缺口<ArrowUpRight size={13}/></Link></div><main>{workspaceError&&<ErrorBox message={'工作区读取失败：'+workspaceError}/>}<Routes><Route path="/" element={<MarketsPage/>}/><Route path="/assets/:id" element={<AssetPage/>}/><Route path="/addresses" element={<AddressesPage/>}/><Route path="/addresses/:address" element={<AddressPage/>}/><Route path="/watchlist" element={<WatchlistPage/>}/><Route path="/sources" element={<SourcesPage/>}/><Route path="*" element={<ErrorBox message="页面不存在"/>}/></Routes></main>{noteEntity&&<NotePanel key={noteEntity.kind+noteEntity.key} entity={noteEntity} note={workspace.notes.find(n=>n.kind===noteEntity.kind&&n.key===noteEntity.key)} onSave={saveNote} onClose={()=>setNoteEntity(null)}/>}</AppContext.Provider>;
}
function AccountGate(){
 const [ready,setReady]=useState(false);const [authenticated,setAuthenticated]=useState(false);
 const [mode,setMode]=useState<'login'|'register'>('login');const [username,setUsername]=useState('');const [password,setPassword]=useState('');const [error,setError]=useState('');const [busy,setBusy]=useState(false);
 useEffect(()=>{let active=true;api('/api/markets').then(()=>{if(active){setAuthenticated(true);setReady(true);}}).catch(e=>{if(active){const status=(e as Error&{status?:number}).status;setReady(true);if(status!==401)setError((e as Error).message);}});return()=>{active=false;}},[]);
 async function submit(event:React.FormEvent){event.preventDefault();setBusy(true);setError('');try{
  if(mode==='register')await api('/api/accounts','POST',{username,password});
  setAuthCredentials(username,password);
  await api('/api/markets');setAuthenticated(true);
 }catch(e){setAuthCredentials('',null);setError((e as Error).message);}finally{setBusy(false);}}
 if(!ready)return <main className="auth-shell"><p>正在检查研究服务…</p></main>;
 if(authenticated)return <ResearchApp/>;
 return <main className="auth-shell"><form className="auth-form" onSubmit={submit}>
  <div className="auth-brand"><Orbit size={25}/><strong>Asset Observatory</strong></div><h1>{mode==='login'?'登录研究工作区':'创建研究账户'}</h1>
  <label>用户名<input autoComplete="username" required minLength={3} maxLength={64} pattern="[A-Za-z0-9._@-]+" value={username} onChange={e=>setUsername(e.target.value)} /></label>
  <label>密码<input type="password" autoComplete={mode==='login'?'current-password':'new-password'} required minLength={16} maxLength={128} value={password} onChange={e=>setPassword(e.target.value)} /></label>
  <button className="primary-button" disabled={busy}>{busy?'请稍候…':mode==='login'?'登录':'注册并进入'}</button>
  {error&&<p className="error-box" role="alert">{error}</p>}
  <button type="button" className="auth-switch" onClick={()=>{setMode(mode==='login'?'register':'login');setError('');}}>{mode==='login'?'创建新账户':'已有账户？返回登录'}</button>
  {mode==='register'&&<small>公开注册账户为研究者权限；密码长度 16 至 128 个字符。</small>}
 </form></main>;
}
export function App(){return <AccountGate/>;}
const root=document.getElementById('root');
if(root)createRoot(root).render(<React.StrictMode><BrowserRouter><App/></BrowserRouter></React.StrictMode>);
