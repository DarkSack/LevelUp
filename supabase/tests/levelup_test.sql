-- Pruebas del esquema: RLS, sincronización y reglas sociales. Se ejecutan con `supabase test db`.
begin;
create extension if not exists pgtap with schema extensions;
select plan(40);

-- Tres personas de prueba.
insert into auth.users (id, email, aud, role) values
  ('00000000-0000-0000-0000-00000000000a', 'ana@example.com', 'authenticated', 'authenticated'),
  ('00000000-0000-0000-0000-00000000000b', 'beto@example.com', 'authenticated', 'authenticated'),
  ('00000000-0000-0000-0000-00000000000c', 'cris@example.com', 'authenticated', 'authenticated');

-- Actuar como una persona: el rol y el JWT que PostgREST pondría con su sesión.
create function pg_temp.as_user(u uuid) returns void language plpgsql as $$
begin
  reset role;
  perform set_config('request.jwt.claims', json_build_object('sub', u, 'role', 'authenticated')::text, true);
  set local role authenticated;
end;
$$;

-- ── Sincronización ──────────────────────────────────────────────────────────────────────────────

select pg_temp.as_user('00000000-0000-0000-0000-00000000000a');
select lives_ok($$ select public.push_records('[
  {"entity":"instance","id":"2026-10-19|fis.caminar-10","updatedAt":100,
   "data":{"challengeId":"fis.caminar-10","status":"COMPLETED","date":"2026-10-19"}},
  {"entity":"xp","id":"CHALLENGE:2026-10-19|fis.caminar-10","updatedAt":100,
   "data":{"sourceType":"CHALLENGE","sourceId":"2026-10-19|fis.caminar-10","amount":15}}
]'::jsonb) $$, 'subir registros propios');
select is((select count(*) from public.user_records)::int, 2, 'Ana ve sus registros');

-- Mismo lote otra vez: idempotente.
select public.push_records('[{"entity":"instance","id":"2026-10-19|fis.caminar-10","updatedAt":100,
  "data":{"challengeId":"fis.caminar-10","status":"COMPLETED","date":"2026-10-19"}}]'::jsonb);
select is((select count(*) from public.user_records)::int, 2, 'repetir un lote no duplica');

-- Una edición más vieja (otro teléfono sin conexión) no pisa la más nueva.
select public.push_records('[{"entity":"instance","id":"2026-10-19|fis.caminar-10","updatedAt":50,
  "data":{"challengeId":"fis.caminar-10","status":"SKIPPED","date":"2026-10-19"}}]'::jsonb);
select is((select data ->> 'status' from public.user_records where entity = 'instance'), 'COMPLETED', 'gana la edición más reciente');

select public.push_records('[{"entity":"instance","id":"2026-10-19|fis.caminar-10","updatedAt":200,
  "data":{"challengeId":"fis.caminar-10","status":"PENDING","date":"2026-10-19"}}]'::jsonb);
select is((select data ->> 'status' from public.user_records where entity = 'instance'), 'PENDING', 'una edición nueva sí cambia el registro');
select ok((select seq from public.user_records where entity = 'instance') > (select seq from public.user_records where entity = 'xp'),
  'el cambio avanza el cursor de descarga');

select throws_ok($$ select public.push_records('[{"entity":"xp","id":"otro-id","updatedAt":1,
  "data":{"sourceType":"CHALLENGE","sourceId":"x","amount":15}}]'::jsonb) $$, '23514', null, 'la XP exige id determinista');
select throws_ok($$ select public.push_records('[{"entity":"xp","id":"CHALLENGE:y","updatedAt":1,
  "data":{"sourceType":"CHALLENGE","sourceId":"y","amount":5000}}]'::jsonb) $$, '23514', null, 'la XP rechaza cantidades imposibles');
select throws_ok($$ select public.push_records('[{"entity":"secreto","id":"1","updatedAt":1,"data":{}}]'::jsonb) $$,
  '23514', null, 'solo entidades conocidas');
select throws_ok($$ insert into public.user_records (user_id, entity, id, data, client_updated_at)
  values ('00000000-0000-0000-0000-00000000000b', 'pause', 'p', '{}', 1) $$, '42501', null, 'no se escribe en nombre de otra persona');
select is((select count(*) from public.user_records where entity = 'pause')::int, 0, 'nada a nombre ajeno');

select pg_temp.as_user('00000000-0000-0000-0000-00000000000b');
select is((select count(*) from public.user_records)::int, 0, 'Beto no ve los registros de Ana');
select lives_ok($$ update public.user_records set deleted = true $$, 'actualizar sin permiso no falla');
reset role;
select is((select count(*) from public.user_records where deleted)::int, 0, '...pero no toca filas ajenas');

-- ── Perfiles ────────────────────────────────────────────────────────────────────────────────────

select pg_temp.as_user('00000000-0000-0000-0000-00000000000a');
select throws_ok($$ insert into public.profiles (user_id, username, display_name)
  values ('00000000-0000-0000-0000-00000000000a', 'admin_levelup', 'Ana') $$, '23514', null, 'nombres reservados no');
select throws_ok($$ insert into public.profiles (user_id, username, display_name)
  values ('00000000-0000-0000-0000-00000000000a', 'Ana Mayúsculas', 'Ana') $$, '23514', null, 'formato de usuario');
select lives_ok($$ insert into public.profiles (user_id, username, display_name, leaderboard_opt_in)
  values ('00000000-0000-0000-0000-00000000000a', 'ana', 'Ana', true) $$, 'Ana crea su perfil');
select pg_temp.as_user('00000000-0000-0000-0000-00000000000b');
insert into public.profiles (user_id, username, display_name, leaderboard_opt_in)
  values ('00000000-0000-0000-0000-00000000000b', 'beto', 'Beto', true);
select is((select count(*) from public.profiles)::int, 1, 'cada quien lee solo su perfil directamente');
select pg_temp.as_user('00000000-0000-0000-0000-00000000000c');
insert into public.profiles (user_id, username, display_name, discoverable)
  values ('00000000-0000-0000-0000-00000000000c', 'cris', 'Cris', false);

-- ── Amistades ───────────────────────────────────────────────────────────────────────────────────

select pg_temp.as_user('00000000-0000-0000-0000-00000000000b');
select is((select count(*) from public.search_profiles('ana'))::int, 1, 'buscar por usuario');
select is((select count(*) from public.search_profiles('cri'))::int, 0, 'quien no quiere aparecer no aparece');
select is((select count(*) from public.search_profiles('an'))::int, 0, 'búsquedas de menos de 3 letras no devuelven nada');
select is((select r ->> 'level' from public.search_profiles('ana') r), null, 'un desconocido no ve el nivel');
select throws_ok($$ insert into public.friendships values ('00000000-0000-0000-0000-00000000000a', '00000000-0000-0000-0000-00000000000b') $$,
  '42501', null, 'no se crean amistades a mano');

select lives_ok($$ select public.send_friend_request('ana') $$, 'Beto invita a Ana');
select throws_ok($$ select public.send_friend_request('ana') $$, 'P0001', 'already_requested', 'sin solicitudes duplicadas');
select pg_temp.as_user('00000000-0000-0000-0000-00000000000a');
select is((select count(*) from public.notifications where kind = 'friend_request')::int, 1, 'Ana recibe el aviso');
select public.respond_friend_request((select id from public.friend_requests where status = 'pending'), true);
select is((select count(*) from public.friends())::int, 1, 'ya son amigos');
select is((select (r ->> 'level')::int from public.friends() r), 1, 'entre amigos se ve el nivel');

select pg_temp.as_user('00000000-0000-0000-0000-00000000000b');
select is((select (r ->> 'weeklyXp')::int from public.friends() r), 15, 'y la XP de la semana');
select lives_ok($$ select public.send_reaction('00000000-0000-0000-0000-00000000000a', 'aplauso') $$, 'felicitar a un amigo');
select throws_ok($$ select public.send_reaction('00000000-0000-0000-0000-00000000000c', 'aplauso') $$, 'P0001', 'not_friends',
  'no se reacciona a desconocidos');
select is((select count(*) from public.friends_leaderboard())::int, 2, 'la clasificación muestra a quienes participan');

-- ── Retos en grupo ──────────────────────────────────────────────────────────────────────────────

select lives_ok($$ select public.create_group_challenge('fis.caminar-10', 'coop', 10, current_date, current_date + 6,
  array['00000000-0000-0000-0000-00000000000a']::uuid[]) $$, 'crear un reto en grupo con amigos');
select throws_ok($$ select public.create_group_challenge('fis.caminar-10', 'coop', 10, current_date, current_date + 6,
  array['00000000-0000-0000-0000-00000000000c']::uuid[]) $$, 'P0001', 'not_friends', 'solo se invita a amigos');

select pg_temp.as_user('00000000-0000-0000-0000-00000000000a');
select lives_ok($$ select public.respond_group_invite((select id from public.group_challenges), true) $$, 'Ana se une al grupo');
select public.push_records(format('[{"entity":"instance","id":"%s|fis.caminar-10","updatedAt":300,
  "data":{"challengeId":"fis.caminar-10","status":"COMPLETED","date":"%s"}}]', current_date, current_date)::jsonb);
select is((select (m ->> 'completed')::int from public.my_groups() g, jsonb_array_elements(g -> 'members') m
  where m ->> 'username' = 'ana'), 1, 'el avance del grupo lo cuenta el servidor con lo sincronizado');

-- ── Bloqueos ────────────────────────────────────────────────────────────────────────────────────

select pg_temp.as_user('00000000-0000-0000-0000-00000000000a');
select public.block_user('00000000-0000-0000-0000-00000000000b');
select is((select count(*) from public.friends())::int, 0, 'bloquear corta la amistad');
select pg_temp.as_user('00000000-0000-0000-0000-00000000000b');
select is((select count(*) from public.search_profiles('ana'))::int, 0, 'quien te bloqueó desaparece de la búsqueda');
select throws_ok($$ select public.send_friend_request('ana') $$, 'P0001', 'user_not_found', 'y no se le puede volver a invitar');

-- ── Borrar la cuenta ────────────────────────────────────────────────────────────────────────────

select pg_temp.as_user('00000000-0000-0000-0000-00000000000a');
select public.delete_account();
reset role;
select is((select count(*) from public.user_records where user_id = '00000000-0000-0000-0000-00000000000a')::int
  + (select count(*) from public.profiles where user_id = '00000000-0000-0000-0000-00000000000a')::int
  + (select count(*) from public.blocks where blocker = '00000000-0000-0000-0000-00000000000a')::int, 0,
  'borrar la cuenta borra progreso, perfil y bloqueos');

select * from finish();
rollback;
