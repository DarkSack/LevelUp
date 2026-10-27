-- Pruebas de la lista de dispositivos: cada quien ve y cierra solo sus sesiones, nunca la que está usando.
begin;
create extension if not exists pgtap with schema extensions;
select plan(7);

insert into auth.users (id, email, aud, role) values
  ('00000000-0000-0000-0000-0000000000a1', 'ana@example.com', 'authenticated', 'authenticated'),
  ('00000000-0000-0000-0000-0000000000b1', 'beto@example.com', 'authenticated', 'authenticated');
insert into auth.sessions (id, user_id, created_at, updated_at, user_agent) values
  ('10000000-0000-0000-0000-0000000000a1', '00000000-0000-0000-0000-0000000000a1', now() - interval '2 days', now() - interval '1 day', 'LevelUp 0.1.0 (Pixel 7; Android 14)'),
  ('10000000-0000-0000-0000-0000000000a2', '00000000-0000-0000-0000-0000000000a1', now() - interval '1 day', now(), 'LevelUp 0.1.0 (Galaxy Tab; Android 13)'),
  ('10000000-0000-0000-0000-0000000000b1', '00000000-0000-0000-0000-0000000000b1', now(), now(), 'LevelUp 0.1.0 (Moto g; Android 12)');

create function pg_temp.as_user(u uuid, session uuid) returns void language plpgsql as $$
begin
  reset role;
  perform set_config('request.jwt.claims', json_build_object('sub', u, 'role', 'authenticated', 'session_id', session)::text, true);
  set local role authenticated;
end;
$$;

select pg_temp.as_user('00000000-0000-0000-0000-0000000000a1', '10000000-0000-0000-0000-0000000000a2');
select is((select count(*) from public.my_sessions()), 2::bigint, 'Ana ve sus dos sesiones y no la de Beto');
select is((select s ->> 'userAgent' from public.my_sessions() s where (s ->> 'current')::boolean),
  'LevelUp 0.1.0 (Galaxy Tab; Android 13)', 'la sesión actual va marcada');
select throws_ok($$ select public.end_session('10000000-0000-0000-0000-0000000000a2') $$, '22023', 'current_session',
  'no cierra la sesión que está usando (para eso, «Cerrar sesión»)');
select lives_ok($$ select public.end_session('10000000-0000-0000-0000-0000000000b1') $$, 'la de otra persona no da error…');
select lives_ok($$ select public.end_session('10000000-0000-0000-0000-0000000000a1') $$, 'cierra la de su otro dispositivo');

reset role;
select is((select count(*) from auth.sessions where user_id = '00000000-0000-0000-0000-0000000000b1'), 1::bigint, '…pero no la borra');
select is((select count(*) from auth.sessions where user_id = '00000000-0000-0000-0000-0000000000a1'), 1::bigint,
  'a Ana le queda solo la sesión actual');

select * from finish();
rollback;
