-- Pruebas de la migración de extras: entidades nuevas, XP de regreso, ánimo, perfil e historial de grupos.
begin;
create extension if not exists pgtap with schema extensions;
select plan(14);

insert into auth.users (id, email, aud, role) values
  ('00000000-0000-0000-0000-0000000000a1', 'ana@example.com', 'authenticated', 'authenticated'),
  ('00000000-0000-0000-0000-0000000000b1', 'beto@example.com', 'authenticated', 'authenticated'),
  ('00000000-0000-0000-0000-0000000000c1', 'cris@example.com', 'authenticated', 'authenticated');
insert into public.profiles (user_id, username, display_name) values
  ('00000000-0000-0000-0000-0000000000a1', 'ana', 'Ana'),
  ('00000000-0000-0000-0000-0000000000b1', 'beto', 'Beto'),
  ('00000000-0000-0000-0000-0000000000c1', 'cris', 'Cris');
insert into public.friendships (user_a, user_b) values ('00000000-0000-0000-0000-0000000000a1', '00000000-0000-0000-0000-0000000000b1');

create function pg_temp.as_user(u uuid) returns void language plpgsql as $$
begin
  reset role;
  perform set_config('request.jwt.claims', json_build_object('sub', u, 'role', 'authenticated')::text, true);
  set local role authenticated;
end;
$$;

-- ── Sincronización ──────────────────────────────────────────────────────────────────────────────

select pg_temp.as_user('00000000-0000-0000-0000-0000000000a1');
select lives_ok($$ select public.push_records('[
  {"entity":"favorite","id":"fis.estirar","updatedAt":1,"data":{"challengeId":"fis.estirar","createdAt":1}},
  {"entity":"goal","id":"g1","updatedAt":1,"data":{"id":"g1","title":"Correr 5 km"}},
  {"entity":"xp","id":"COMEBACK:2026-10-25","updatedAt":1,"data":{"sourceType":"COMEBACK","sourceId":"2026-10-25","amount":20}},
  {"entity":"achievement","id":"first_1","updatedAt":1,"data":{"achievementId":"first_1"}}
]'::jsonb) $$, 'favoritos, metas y XP de regreso se sincronizan');
select is((select count(*) from public.user_records where entity in ('favorite', 'goal'))::int, 2, 'quedan guardados');
select throws_ok($$ select public.push_records('[{"entity":"xp","id":"COMEBACK:x","updatedAt":1,
  "data":{"sourceType":"COMEBACK","sourceId":"y","amount":20}}]'::jsonb) $$, '23514', null, 'la XP de regreso también exige id determinista');

-- ── Ánimo ───────────────────────────────────────────────────────────────────────────────────────

select lives_ok($$ select public.send_reaction('00000000-0000-0000-0000-0000000000b1', 'animo_puedes') $$, 'mensaje de ánimo a un amigo');
select throws_ok($$ select public.send_reaction('00000000-0000-0000-0000-0000000000b1', 'texto libre') $$, '23514', null,
  'solo mensajes predefinidos');

-- ── Perfil: avatar, título y trofeos ────────────────────────────────────────────────────────────

select throws_ok($$ update public.profiles set avatar = 'NO VALE' $$, '23514', null, 'el avatar es una clave conocida');
select throws_ok($$ update public.profiles set title_level = 7 $$, '23514', null, 'solo niveles de título que existen');
select throws_ok($$ update public.profiles set trophies = array['a', 'b', 'c', 'd'] $$, '23514', null, 'como mucho tres trofeos');
select lives_ok($$ update public.profiles set avatar = 'zorro', title_level = 5, trophies = array['first_1', 'inventado'] $$,
  'Ana elige avatar, título y trofeos');

select pg_temp.as_user('00000000-0000-0000-0000-0000000000b1');
select is((select f ->> 'avatar' from public.friends() f), 'zorro', 'su amigo ve el avatar');
select is((select f -> 'title' from public.friends() f), 'null'::jsonb, 'un título aún no alcanzado no se muestra');
select is((select f -> 'trophies' from public.friends() f), '["first_1"]'::jsonb, 'solo trofeos desbloqueados de verdad');

-- ── Historial de grupos ─────────────────────────────────────────────────────────────────────────

reset role;
insert into public.group_challenges (id, owner, challenge_id, mode, target, starts_on, ends_on) values
  ('00000000-0000-0000-0000-0000000000f1', '00000000-0000-0000-0000-0000000000a1', 'fis.estirar', 'coop', 5,
   current_date - 10, current_date - 4);
insert into public.group_members (group_id, user_id, status) values
  ('00000000-0000-0000-0000-0000000000f1', '00000000-0000-0000-0000-0000000000a1', 'joined'),
  ('00000000-0000-0000-0000-0000000000f1', '00000000-0000-0000-0000-0000000000b1', 'joined');
select pg_temp.as_user('00000000-0000-0000-0000-0000000000b1');
select is((select count(*) from public.my_group_history())::int, 1, 'un reto en grupo terminado aparece en el historial');
select pg_temp.as_user('00000000-0000-0000-0000-0000000000c1');
select is((select count(*) from public.my_group_history())::int, 0, 'y no en el de quien no participó');

select * from finish();
rollback;
