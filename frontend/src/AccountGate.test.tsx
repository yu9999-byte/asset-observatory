import { beforeEach,expect,test,vi } from 'vitest';
import { fireEvent,render,screen,waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { App } from './main';
import { api,setAuthCredentials } from './api';

vi.mock('./api',async original=>({...await original<typeof import('./api')>(),api:vi.fn(),setAuthCredentials:vi.fn()}));
beforeEach(()=>{vi.mocked(api).mockReset();vi.mocked(setAuthCredentials).mockReset();});

test('public registration creates the account before setting credentials',async()=>{
 vi.mocked(api).mockRejectedValue(Object.assign(new Error('Unauthorized'),{status:401}));
 render(<MemoryRouter><App/></MemoryRouter>);
 fireEvent.click(await screen.findByRole('button',{name:'创建新账户'}));
 fireEvent.change(screen.getByLabelText('用户名'),{target:{value:'new.reader'}});
 fireEvent.change(screen.getByLabelText('密码'),{target:{value:'new-reader-password-2026'}});
 vi.mocked(api).mockImplementation(async url=>{
  if(url==='/api/accounts'){
   expect(setAuthCredentials).not.toHaveBeenCalled();
   return {username:'new.reader',role:'RESEARCHER'};
  }
  throw Object.assign(new Error('Login probe reached'),{status:401});
 });
 fireEvent.click(screen.getByRole('button',{name:'注册并进入'}));
 await waitFor(()=>expect(setAuthCredentials).toHaveBeenCalledWith('new.reader','new-reader-password-2026'));
 expect(api).toHaveBeenCalledWith('/api/accounts','POST',{username:'new.reader',password:'new-reader-password-2026'});
 await waitFor(()=>expect(setAuthCredentials).toHaveBeenLastCalledWith('',null));
});
