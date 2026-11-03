-- Pruebas del código de recuperación: sirve una vez, cambia la contraseña, no se lee desde fuera y limita intentos.
begin;
create extension if not exists pgtap with schema extensions;
select plan(10);

insert into auth.users (id, email, aud, role, encrypted_password) values
  ('00000000-0000-0000-0000-0000000000a3', 'levelup-ana3@sackito.online', 'authenticated', 'authenticated',
   extensions.crypt('vieja123', extensions.gen_salt('bf')));

create function pg_temp.as_user(u uuid) returns void language plpgsql as $$
begin
  reset role;
  perform set_config('request.jwt.claims', json_build_object('sub', u, 'role', 'authenticated')::text, true);
  set local role authenticated;
end;
$$;

select pg_temp.as_user('00000000-0000-0000-0000-0000000000a3');
create temp table code_holder as select public.create_recovery_code() as code;
grant select on code_holder to anon, authenticated;
select matches((select code from code_holder), '^[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{4}$', 'el código sale en grupos de 4');
select isnt(public.my_recovery_code_date(), null, 'se sabe cuándo se creó');
select throws_ok($$ select * from public.recovery_codes $$, '42501', null, 'la tabla de códigos no se lee');

reset role;
set local role anon;
select is(public.recover_password('levelup-ana3@sackito.online', '0000-0000-0000-0000', 'nueva1234'), 'invalid', 'código equivocado');
select is(public.recover_password('levelup-ana3@sackito.online', (select code from code_holder), 'corta'), 'weak_password',
  'la contraseña nueva debe ser fuerte');
select is(public.recover_password('LEVELUP-ANA3@sackito.online', lower((select code from code_holder)), 'nueva1234'), 'ok',
  'con el código bueno (sin importar mayúsculas) cambia');
select is(public.recover_password('levelup-ana3@sackito.online', (select code from code_holder), 'otra12345'), 'invalid',
  'el código solo sirve una vez');

reset role;
select ok((select encrypted_password = extensions.crypt('nueva1234', encrypted_password) from auth.users
  where id = '00000000-0000-0000-0000-0000000000a3'), 'la contraseña quedó cambiada');

set local role anon;
select public.recover_password('nadie@example.com', '0000-0000-0000-0000', 'nueva1234') from generate_series(1, 5);
select is(public.recover_password('nadie@example.com', '0000-0000-0000-0000', 'nueva1234'), 'rate_limited', 'máximo 5 intentos por hora');
select throws_ok($$ select public.create_recovery_code() $$, '42501', null, 'sin sesión no se crean códigos');

select * from finish();
rollback;
