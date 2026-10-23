import { describe,expect,it } from 'vitest';
import { render,screen,fireEvent,cleanup } from '@testing-library/react';
import { DataTable, csvCell, compareDecimal } from './DataTable';
describe('R08 shared research table',()=>{
 it('filters, sorts and hides columns',()=>{
  render(<DataTable label="test" rows={[{id:'a',name:'Beta'},{id:'b',name:'Alpha'}]} columns={[{key:'name',title:'Name',value:r=>r.name}]} rowKey={r=>r.id}/>);
  fireEvent.click(screen.getByRole('button',{name:'Name'}));
  expect(screen.getAllByRole('row')[1]).toHaveTextContent('Alpha');
  fireEvent.change(screen.getByLabelText('筛选 test'),{target:{value:'Beta'}});
  expect(screen.queryByText('Alpha')).not.toBeInTheDocument();
  expect(screen.getByText('Beta')).toBeInTheDocument();cleanup();
 });
 it('prevents spreadsheet formulas in CSV and sorts exact decimals',()=>{
  expect(csvCell('=WEBSERVICE("bad")')).toContain("'");
  expect(compareDecimal('9007199254740993','9007199254740992')).toBe(1);
  expect(compareDecimal('0.000000000000000001','0.000000000000000002')).toBe(-1);
 });
 it('keeps missing sort values last in both directions',()=>{
  const rows=[{id:'missing',amount:null as string|null},{id:'one',amount:'1'},{id:'two',amount:'2'}];
  render(<DataTable label="missing sort" rows={rows} columns={[{key:'amount',title:'Amount',value:r=>r.amount,numeric:true}]} rowKey={r=>r.id}/>);
  const sort=screen.getByRole('button',{name:'Amount'});
  fireEvent.click(sort);
  expect(screen.getAllByRole('row').slice(1).map(row=>row.textContent)).toEqual(['1','2','—']);
  fireEvent.click(sort);
  expect(screen.getAllByRole('row').slice(1).map(row=>row.textContent)).toEqual(['2','1','—']);
  cleanup();
 });
 it('pins only an identity column in the first visible position',()=>{
  render(<DataTable label="pin test" rows={[{name:'Alpha',amount:'1'}]} columns={[{key:'name',title:'Name',value:r=>r.name,pinned:true},{key:'amount',title:'Amount',value:r=>r.amount}]} rowKey={r=>r.name}/>);
  expect(screen.getByRole('columnheader',{name:'Name'})).toHaveClass('pinned-column');
  fireEvent.click(screen.getByRole('button',{name:'上移 Amount'}));
  expect(screen.getByRole('columnheader',{name:'Name'})).not.toHaveClass('pinned-column');
  expect(screen.getByRole('columnheader',{name:'Amount'})).not.toHaveClass('pinned-column');
 });
});
