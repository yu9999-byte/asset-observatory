import { cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { afterEach, expect, test, vi } from 'vitest';
import { DataTable } from './DataTable';
import { api } from './api';

vi.mock('./api',()=>({api:vi.fn()}));
afterEach(()=>{cleanup();vi.resetAllMocks();});
const rows=Array.from({length:35},(_,i)=>({id:String(i),name:`Asset ${i}`,amount:String(i)}));
const columns=[{key:'name',title:'Name',value:(r:typeof rows[number])=>r.name},{key:'amount',title:'Amount',numeric:true,value:(r:typeof rows[number])=>r.amount}];
const settings={tableId:'markets',hidden:['name'],order:['amount','name'],sortKey:'amount',desc:true,pageSize:30,version:1,updatedAt:'2026-10-21T00:00:00Z'};
const table=()=> <DataTable label="test" preferenceId="markets" rows={rows} columns={columns} rowKey={r=>r.id}/>;

test('hydrates server settings without writing defaults and paginates loaded rows',async()=>{
 vi.mocked(api).mockResolvedValue(settings);
 render(table());
 await waitFor(()=>expect(screen.queryByRole('columnheader',{name:'Name'})).not.toBeInTheDocument());
 expect(screen.getAllByRole('row')).toHaveLength(31);
 expect(screen.getAllByRole('row')[1]).toHaveTextContent('34');
 expect(screen.getByRole('combobox',{name:'每页条数 test'})).toHaveValue('30');
 expect(api).toHaveBeenCalledTimes(1);
 fireEvent.click(screen.getByRole('button',{name:'下一页'}));
 expect(screen.getAllByRole('row')).toHaveLength(6);
});

test('failed writes preserve saved layout and require an explicit reload',async()=>{
 vi.mocked(api).mockResolvedValueOnce(settings).mockRejectedValueOnce(new Error('Conflict')).mockResolvedValueOnce({...settings,hidden:[],version:2});
 render(table());
 const header=await screen.findByRole('button',{name:'Amount'});
 await waitFor(()=>expect(header).not.toBeDisabled());
 fireEvent.click(header);
 expect(await screen.findByRole('alert')).toHaveTextContent('Conflict');
 expect(screen.getAllByRole('row')[1]).toHaveTextContent('34');
 expect(header).toBeDisabled();
 fireEvent.click(screen.getByRole('button',{name:'重新读取表格配置 test'}));
 await waitFor(()=>expect(screen.getByRole('columnheader',{name:'Name'})).toBeVisible());
 expect(screen.queryByRole('alert')).not.toBeInTheDocument();
 expect(api).toHaveBeenCalledTimes(3);
});

test('column movement is saved with the loaded optimistic version',async()=>{
 vi.mocked(api).mockResolvedValueOnce({...settings,hidden:[]}).mockResolvedValueOnce({...settings,hidden:[],order:['name','amount'],version:2});
 render(table());
 await waitFor(()=>expect(screen.getByRole('button',{name:'Amount'})).not.toBeDisabled());
 fireEvent.click(screen.getByText('显示列'));
 fireEvent.click(screen.getByRole('button',{name:'上移 Name'}));
 await waitFor(()=>expect(api).toHaveBeenLastCalledWith('/api/table-preferences/markets','PUT',expect.objectContaining({order:['name','amount'],version:1})));
 const headers=within(screen.getAllByRole('row')[0]).getAllByRole('columnheader');
 expect(headers[0]).toHaveTextContent('Name');
});
