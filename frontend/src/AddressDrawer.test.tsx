import { fireEvent,render,screen,within,waitFor } from '@testing-library/react';
import { beforeEach,expect,test,vi } from 'vitest';
import { AddressDrawer } from './AddressDrawer';
import { api } from './api';

vi.mock('./api',async original=>({...await original<typeof import('./api')>(),api:vi.fn()}));
const address='0x0000000000000000000000000000000000000001';
beforeEach(()=>{vi.mocked(api).mockReset();});

test('unavailable sources stay missing and do not prevent opening independent detail',async()=>{
 vi.mocked(api).mockResolvedValue({data:[],status:'unavailable',reason:'来源维护中',source:'Test source',fetchedAt:null});
 render(<AddressDrawer address={address} asset="JCT" onClose={()=>{}}/>);
 const drawer=screen.getByRole('dialog',{name:'转入转出'});
 expect(await within(drawer).findByText('来源维护中')).toBeVisible();
 expect(within(drawer).getAllByText('—')).toHaveLength(3);
 expect(within(drawer).getByRole('link',{name:'新页面查看地址详情'})).toHaveAttribute('target','_blank');
 expect(within(drawer).getByRole('link',{name:'新页面查看地址详情'})).toHaveAttribute('rel','noopener noreferrer');
});

test('failed requests can retry and a successful empty batch is distinguished from failure',async()=>{
 vi.mocked(api).mockRejectedValueOnce(new Error('连接超时')).mockResolvedValueOnce({data:[],status:'ok',source:'Test source',fetchedAt:null});
 render(<AddressDrawer address={address} asset="JCT" onClose={()=>{}}/>);
 expect(await screen.findByRole('alert')).toHaveTextContent('连接超时');
 fireEvent.click(screen.getByRole('button',{name:'重新读取转账'}));
 expect(await screen.findByText('当前来源批次没有转账记录')).toBeVisible();
 expect(screen.queryByRole('alert')).not.toBeInTheDocument();
});

test('keyboard focus stays inside the modal and backdrop closes it',async()=>{
 vi.mocked(api).mockResolvedValue({data:[],status:'ok',source:'Test source',fetchedAt:null});
 const close=vi.fn();render(<AddressDrawer address={address} asset="JCT" onClose={close}/>);
 await screen.findByText('当前来源批次没有转账记录');
 const first=screen.getByRole('button',{name:'关闭地址预览'});const last=screen.getByRole('link',{name:'新页面查看地址详情'});
 expect(first).toHaveFocus();
 first.focus();fireEvent.keyDown(document,{key:'Tab',shiftKey:true});expect(last).toHaveFocus();
 fireEvent.keyDown(document,{key:'Tab'});expect(first).toHaveFocus();
 fireEvent.click(screen.getByRole('dialog',{name:'转入转出'}));expect(close).not.toHaveBeenCalled();
 fireEvent.click(screen.getByRole('dialog',{name:'转入转出'}).parentElement!);
 expect(close).toHaveBeenCalledTimes(1);
});

test('an older address request cannot replace the current preview',async()=>{
 let finishFirst!:(value:unknown)=>void;
 vi.mocked(api).mockImplementationOnce(()=>new Promise(resolve=>{finishFirst=resolve;}));
 vi.mocked(api).mockResolvedValue({data:[],status:'unsupported',reason:'当前币种未覆盖',source:'Test source',fetchedAt:null});
 const close=vi.fn();const view=render(<AddressDrawer address={address} asset="JCT" onClose={close}/>);
 view.rerender(<AddressDrawer address={address} asset="ETH" onClose={close}/>);
 expect(await screen.findByText('ETH 转账暂未覆盖')).toBeVisible();
 finishFirst({data:[],status:'unavailable',reason:'旧请求',source:'Old source',fetchedAt:null});
 await waitFor(()=>expect(screen.queryByText('旧请求')).not.toBeInTheDocument());
 expect(screen.getByText('当前币种未覆盖')).toBeVisible();
});
