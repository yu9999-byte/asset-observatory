import { useState } from 'react';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { expect, test, vi } from 'vitest';
import { NotePanel } from './NotePanel';
import type { Note } from './types';

test('save acknowledgement survives the server version update', async () => {
 function Workspace() {
  const [note, setNote] = useState<Note>();
  return <NotePanel entity={{kind:'asset',key:'LINK'}} note={note} onClose={()=>{}} onSave={async (draft,visible,version)=>{
   setNote({kind:'asset',key:'LINK',...draft,visible,version:version+1,updatedAt:new Date().toISOString()});
  }}/>;
 }
 render(<Workspace/>);
 fireEvent.change(screen.getByRole('textbox',{name:'自定义备注'}), {target:{value:'Research evidence'}});
 fireEvent.click(screen.getByRole('button', {name:'保存备注'}));
 await waitFor(()=>expect(screen.getByText(/已保存/)).toBeVisible());
 fireEvent.change(screen.getByRole('textbox',{name:'自定义备注'}), {target:{value:'Revised evidence'}});
 expect(screen.getByText(/更改需保存/)).toBeVisible();
});

test('UI09 pin and collapse preserve an unsaved draft without saving',()=>{
 const onSave=vi.fn();
 render(<NotePanel entity={{kind:'asset',key:'JCT'}} note={undefined} onSave={onSave} onClose={()=>{}}/>);
 fireEvent.change(screen.getByRole('textbox',{name:'自定义备注'}),{target:{value:'Unsaved JCT research'}});
 fireEvent.click(screen.getByRole('button',{name:'固定备注'}));
 expect(screen.getByRole('button',{name:'取消固定备注'})).toHaveAttribute('aria-pressed','true');
 fireEvent.click(screen.getByRole('button',{name:'折叠备注'}));
 expect(screen.queryByRole('textbox')).not.toBeInTheDocument();
 fireEvent.click(screen.getByRole('button',{name:'展开备注'}));
 expect(screen.getByRole('textbox',{name:'自定义备注'})).toHaveValue('Unsaved JCT research');
 expect(onSave).not.toHaveBeenCalled();
});

test('PR06 three separate project fields survive folding and save as one record',async()=>{
 const onSave=vi.fn().mockResolvedValue(undefined);
 render(<NotePanel entity={{kind:'asset',key:'JCT'}} note={undefined} onSave={onSave} onClose={()=>{}}/>);
 fireEvent.change(screen.getByRole('textbox',{name:'项目名称'}),{target:{value:'Janction research'}});
 fireEvent.change(screen.getByRole('textbox',{name:'项目信息'}),{target:{value:'Source links and project context'}});
 fireEvent.change(screen.getByRole('textbox',{name:'自定义备注'}),{target:{value:'Private hypothesis'}});
 fireEvent.click(screen.getByRole('button',{name:'折叠备注'}));
 expect(screen.queryByRole('textbox',{name:'项目名称'})).not.toBeInTheDocument();
 fireEvent.click(screen.getByRole('button',{name:'展开备注'}));
 expect(screen.getByRole('textbox',{name:'项目名称'})).toHaveValue('Janction research');
 expect(screen.getByRole('textbox',{name:'项目信息'})).toHaveValue('Source links and project context');
 fireEvent.click(screen.getByRole('button',{name:'保存备注'}));
 await waitFor(()=>expect(onSave).toHaveBeenCalledWith({projectName:'Janction research',projectInfo:'Source links and project context',text:'Private hypothesis'},true,0));
});

test('PR07 failed save and hide retains every draft and never acknowledges success',async()=>{
 const onClose=vi.fn();
 const onSave=vi.fn().mockRejectedValue(new Error('409 conflict'));
 render(<NotePanel entity={{kind:'asset',key:'JCT'}} note={undefined} onSave={onSave} onClose={onClose}/>);
 for(const [name,value] of [['项目名称','Draft name'],['项目信息','Draft context'],['自定义备注','Draft note']])
  fireEvent.change(screen.getByRole('textbox',{name}),{target:{value}});
 fireEvent.click(screen.getByRole('button',{name:'保存并隐藏备注'}));
 await waitFor(()=>expect(screen.getByRole('alert')).toHaveTextContent('409 conflict'));
 expect(onClose).not.toHaveBeenCalled();
 expect(screen.getByRole('textbox',{name:'项目名称'})).toHaveValue('Draft name');
 expect(screen.getByRole('textbox',{name:'项目信息'})).toHaveValue('Draft context');
 expect(screen.getByRole('textbox',{name:'自定义备注'})).toHaveValue('Draft note');
 expect(screen.queryByText(/已保存/)).not.toBeInTheDocument();
});
