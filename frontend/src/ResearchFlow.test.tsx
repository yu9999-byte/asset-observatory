import { beforeEach, expect, test, vi } from 'vitest';
import { act, fireEvent, render, screen, within, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { App } from './main';
import { api } from './api';

vi.mock('./api', async original => ({...await original<typeof import('./api')>(),api:vi.fn()}));
const a='0x0000000000000000000000000000000000000001';
const b='0x0000000000000000000000000000000000000002';
const c='0x0000000000000000000000000000000000000003';
const envelope=(data:unknown[])=>({data,status:'partial',source:'Synthetic test fixture',sourceUrl:'',fetchedAt:null,sourceAt:null,coverage:'TEST ONLY',reason:'测试覆盖',nextCursor:null});
const markets={assets:[{id:'JCT',symbol:'JCT',name:'Janction',chain:'ethereum',contract:a,decimals:18,spotInstrument:null,swapInstrument:null}],market:envelope([{assetId:'JCT',lastPrice:'12.345',volume24hBase:'1200',turnover24hQuote:'14000',change24hPct:null}]),derivatives:envelope([{assetId:'JCT',instrument:'TEST-JCT',openInterestRaw:'321',openInterestUnit:'source-native'}])};
beforeEach(()=>{
 vi.mocked(api).mockReset();
 vi.mocked(api).mockImplementation(async url=>{
  if(url==='/api/markets')return markets;
  if(url==='/api/workspace')return {watches:[{kind:'address',key:c,createdAt:''}],notes:[]};
  if(url.includes('/table-preferences'))return {hidden:[],order:[],sortKey:null,desc:false,pageSize:15,version:0};
  if(url.includes('/holders'))return envelope([{address:a,balance:'100',rawBalance:'100',decimals:0,isContract:true}]);
  if(url.includes('/transfers'))return envelope([
   {id:'in',from:b,to:a,direction:'IN',amount:'10',hash:'0x1',logIndex:1,blockNumber:1,sourceAt:null},
   {id:'out',from:a,to:b,direction:'OUT',amount:'20',hash:'0x2',logIndex:2,blockNumber:2,sourceAt:null},
   {id:'self',from:a,to:a,direction:'SELF',amount:'30',hash:'0x3',logIndex:3,blockNumber:3,sourceAt:null}]);
  if(url.includes('/liquidations'))return envelope([]);
  throw new Error('Unexpected URL '+url);
 });
});
function mount(path:string){render(<MemoryRouter initialEntries={[path]}><App/></MemoryRouter>);}

test('UI02 market scope and all planned research metrics remain visible',async()=>{
 mount('/');
 const table=await screen.findByRole('region',{name:'资产行情'});
 expect(await within(table).findByText('12.345')).toBeVisible();
 for(const title of ['流通市值','FDV','资金费率'])expect(within(table).getByRole('columnheader',{name:title})).toBeInTheDocument();
 for(const title of ['涨跌幅排行','成交量异动','持仓变化','爆仓排行'])expect(screen.getByRole('heading',{name:title})).toBeVisible();
 expect(screen.queryByText('全市场行情')).not.toBeInTheDocument();
});

test('UI03-06 asset summary reuses known data and custom watches retain out-of-page addresses',async()=>{
 mount('/assets/JCT');
 await screen.findByText('Janction');
 expect(screen.getByTestId('asset-summary-metrics')).toHaveTextContent('12.345');
 expect(screen.queryByRole('button',{name:'Top30'})).not.toBeInTheDocument();
 expect(screen.queryByRole('button',{name:'多签'})).not.toBeInTheDocument();
 expect(screen.queryByRole('button',{name:'自定义'})).not.toBeInTheDocument();
 expect(screen.queryByRole('button',{name:/刷新/})).not.toBeInTheDocument();
 expect(screen.queryByRole('button',{name:/导出/})).not.toBeInTheDocument();
 expect(screen.getByRole('textbox',{name:'筛选 持仓地址'})).toHaveAttribute('placeholder','搜索地址…');
 expect(screen.queryByRole('progressbar')).not.toBeInTheDocument();
 fireEvent.click(screen.getByRole('button',{name:'已关注'}));
 const table=screen.getByRole('region',{name:'持仓地址'});
 expect(await within(table).findByRole('button',{name:'查看地址 '+c})).toBeVisible();
 expect(within(table).queryByText('100')).not.toBeInTheDocument();
 expect(within(table).getByText('当前页未覆盖')).toBeVisible();
});

test('UI07-08 direction filtering excludes self and history stays explicitly missing',async()=>{
 mount(`/addresses/${a}?asset=JCT`);
 const table=await screen.findByRole('region',{name:'转账事件'});
 await within(table).findByText('+10');
 for(const title of ['From','To','事件价格 · USD','事件金额 · USD'])expect(within(table).getByRole('columnheader',{name:title})).toBeInTheDocument();
 fireEvent.click(screen.getByRole('button',{name:'转出'}));
 expect(within(table).getByText('−20')).toBeVisible();
 expect(within(table).queryByText('+10')).not.toBeInTheDocument();
 expect(within(table).queryByText('30')).not.toBeInTheDocument();
 expect(screen.getByRole('button',{name:'交易所对手方'})).toBeDisabled();
 fireEvent.click(screen.getByRole('button',{name:'7D'}));
 expect(screen.getByText('7D 累计转入')).toBeVisible();
 expect(screen.getByText('统计窗口未完整覆盖')).toBeVisible();
 await waitFor(()=>expect(within(table).getByText('−20')).toBeVisible());
});

test('holder address opens a preview and links to its token detail in a new tab',async()=>{
 mount('/assets/JCT');
 const holders=await screen.findByRole('region',{name:'持仓地址'});
 const trigger=await within(holders).findByRole('button',{name:'查看地址 '+a});
 trigger.focus();fireEvent.click(trigger);
 const drawer=await screen.findByRole('dialog',{name:'转入转出'});
 await within(drawer).findByText('+10');
 const metrics=within(drawer).getByRole('group',{name:'本批次资金流'});
 expect(metrics).toHaveTextContent('转入10JCT');
 expect(metrics).toHaveTextContent('转出20JCT');
 expect(metrics).toHaveTextContent('净流量-10JCT');
 const detail=within(drawer).getByRole('link',{name:'新页面查看地址详情'});
 expect(detail).toHaveAttribute('href',`/addresses/${a}?asset=JCT`);
 expect(detail).toHaveAttribute('target','_blank');
 fireEvent.click(within(drawer).getByRole('button',{name:'转入'}));
 expect(within(drawer).getByText('+10')).toBeVisible();
 expect(within(drawer).queryByText('−20')).not.toBeInTheDocument();
 fireEvent.keyDown(document,{key:'Escape'});
 expect(screen.queryByRole('dialog',{name:'转入转出'})).not.toBeInTheDocument();
 expect(trigger).toHaveFocus();
 expect(screen.getByRole('heading',{name:/JCT · 持仓地址$/})).toBeVisible();
});

test('address search page is reachable without a holder row and preserves the chosen asset',async()=>{
 mount('/addresses?asset=JCT');
 await screen.findByRole('heading',{name:'地址查询'});
 expect(screen.getByRole('combobox',{name:'查询代币'})).toHaveValue('JCT');
 fireEvent.change(screen.getByRole('textbox',{name:'查询 Ethereum 地址'}),{target:{value:a}});
 fireEvent.click(screen.getByRole('button',{name:'查询地址'}));
 await screen.findByRole('heading',{name:'地址转账事件'});
 expect(screen.getByRole('combobox',{name:'事件代币'})).toHaveValue('JCT');
});

test('empty ETH holder page can preview an address without silently querying LINK',async()=>{
 const previous=vi.mocked(api).getMockImplementation()!;
 vi.mocked(api).mockImplementation(async(url,...args)=>{
  if(url.startsWith('/api/assets/')&&url.includes('/holders'))return {...envelope([]),status:'unsupported',reason:'ETH 持仓暂未接入'};
  if(url.startsWith('/api/addresses/')&&url.includes('/transfers'))return {...envelope([]),status:'unsupported',reason:'ETH 原生币转账暂未接入'};
  return previous(url,...args);
 });
 mount('/assets/ETH');
 expect((await screen.findAllByText('ETH 持仓暂未接入')).length).toBeGreaterThan(0);
 fireEvent.click(screen.getByRole('button',{name:'查地址'}));
 fireEvent.change(screen.getByRole('textbox',{name:'查询 Ethereum 地址'}),{target:{value:a}});
 fireEvent.click(screen.getByRole('button',{name:'查询地址'}));
 const drawer=await screen.findByRole('dialog',{name:'转入转出'});
 expect(await within(drawer).findByText('ETH 转账暂未覆盖')).toBeVisible();
 expect(vi.mocked(api)).toHaveBeenCalledWith(`/api/addresses/${a}/transfers?asset=ETH`);
 expect(within(drawer).getByRole('link',{name:'新页面查看地址详情'})).toHaveAttribute('href',`/addresses/${a}?asset=ETH`);
 expect(within(drawer).queryByText('0')).not.toBeInTheDocument();
});

test('DA09 source review separates successful verification from enabled and licensed use',async()=>{
 const previous=vi.mocked(api).getMockImplementation()!;
 vi.mocked(api).mockImplementation(async (url,...args)=>{
  if(url==='/api/sources')return [{name:'CoinLore',documentation:'https://www.coinlore.com/cryptocurrency-data-api',terms:'https://www.coinlore.com/terms-of-use',enabled:false,productionApproved:false,gap:'对外展示范围需澄清',verification:{checkedAt:'2026-10-21T16:22:06Z',transportStatus:'reachable',scope:'7 个目录币种的行情字段；JCT 供应量缺失',rightsStatus:'未取得对外展示授权',evidenceId:'acquisition-java-direct-20261022'}}];
  return previous(url,...args);
 });
 mount('/sources');
 const heading=await screen.findByRole('heading',{name:'CoinLore'});
 const source=heading.closest('article')!;
 expect(within(source).getByText('实测可达')).toBeVisible();
 expect(within(source).getByText('未启用')).toBeVisible();
 expect(source).toHaveTextContent('生产批准：否');
 expect(source).toHaveTextContent('JCT 供应量缺失');
 expect(source).toHaveTextContent('未取得对外展示授权');
 expect(source).toHaveTextContent('acquisition-java-direct-20261022');
});

test('DA10 candidate with unverified terms has an explicit unknown state and no terms link',async()=>{
 const previous=vi.mocked(api).getMockImplementation()!;
 vi.mocked(api).mockImplementation(async(url,...args)=>url==='/api/sources'
  ?[{name:'MEXC candidate',documentation:'https://mexcdevelop.github.io/apidocs/contract_v1_en/',terms:null,enabled:false,productionApproved:false,gap:'连接与使用条件未齐'}]
  :previous(url,...args));
 mount('/sources');
 const source=(await screen.findByRole('heading',{name:'MEXC candidate'})).closest('article')!;
 expect(source).toHaveTextContent('使用条款：未核验');
 expect(within(source).queryByRole('link',{name:'使用条款'})).not.toBeInTheDocument();
 expect(within(source).getByRole('link',{name:'官方文档'})).toHaveAttribute('href','https://mexcdevelop.github.io/apidocs/contract_v1_en/');
});

test('PR07 saving a project record from the watch workspace keeps the editor open',async()=>{
 const previous=vi.mocked(api).getMockImplementation()!;
 const original={kind:'asset',key:'JCT',projectName:'Project draft',projectInfo:'Initial context',text:'Private note',visible:false,version:1,updatedAt:'2026-10-21T00:00:00Z'};
 vi.mocked(api).mockImplementation(async(url,method,...args)=>{
  if(url==='/api/workspace')return {watches:[],notes:[original]};
  if(url==='/api/notes/asset/JCT'&&method==='PUT')return {...original,...args[0] as object,version:2};
  return previous(url,method,...args);
 });
 mount('/watchlist');
 fireEvent.click(await screen.findByRole('button',{name:/Project draft/}));
 fireEvent.change(screen.getByRole('textbox',{name:'项目信息'}),{target:{value:'Updated context'}});
 await act(async()=>{fireEvent.click(screen.getByRole('button',{name:'保存备注'}));});
 await waitFor(()=>expect(screen.getByRole('dialog',{name:'研究备注 JCT'})).toBeVisible());
 await waitFor(()=>expect(screen.getByRole('status')).toHaveTextContent('已保存'));
 expect(screen.getByRole('textbox',{name:'项目信息'})).toHaveValue('Updated context');
 expect(vi.mocked(api)).toHaveBeenCalledWith('/api/notes/asset/JCT','PUT',{projectName:'Project draft',projectInfo:'Updated context',text:'Private note',visible:true,version:1});
});
