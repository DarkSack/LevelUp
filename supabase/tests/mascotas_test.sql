-- Pruebas de la mascota en el perfil social: se guarda con touch_profile, la ven los demás y lo inválido se ignora.
begin;
create extension if not exists pgtap with schema extensions;
select plan(6);

insert into auth.users (id, email, aud, role) values
  ('00000000-0000-0000-0000-0000000000a1', 'ana@example.com', 'authenticated', 'authenticated'),
  ('00000000-0000-0000-0000-0000000000b1', 'beto@example.com', 'authenticated', 'authenticated');
insert into public.profiles (user_id, username, display_name) values
  ('00000000-0000-0000-0000-0000000000a1', 'ana', 'Ana'),
  ('00000000-0000-0000-0000-0000000000b1', 'beto', 'Beto');
insert into public.friendships (user_a, user_b) values ('00000000-0000-0000-0000-0000000000a1', '00000000-0000-0000-0000-0000000000b1');

create function pg_temp.as_user(u uuid) returns void language plpgsql as $$
begin
  reset role;
  perform set_config('request.jwt.claims', json_build_object('sub', u, 'role', 'authenticated')::text, true);
  set local role authenticated;
end;
$$;

select pg_temp.as_user('00000000-0000-0000-0000-0000000000a1');
select lives_ok($$ select public.touch_profile(5, 'frame.basic', 'zorro', 'tlacuache', 'corona,bufanda', 'Pepe') $$,
  'Ana guarda su mascota');
select lives_ok($$ select public.touch_profile(5, 'frame.basic', 'zorro') $$, 'la versión vieja sigue funcionando');

select pg_temp.as_user('00000000-0000-0000-0000-0000000000b1');
select is((select f ->> 'pet' from public.friends() f), 'tlacuache', 'Beto ve la mascota de Ana');
select is((select (f ->> 'petAccessories') || '|' || (f ->> 'petName') from public.friends() f), 'corona,bufanda|Pepe',
  'con sus accesorios y su nombre');

select pg_temp.as_user('00000000-0000-0000-0000-0000000000a1');
select lives_ok($$ select public.touch_profile(5, 'frame.basic', 'zorro', 'DROP TABLE', 'x;y', repeat('n', 40)) $$,
  'lo inválido no da error…');
reset role;
select is((select pet || '|' || pet_accessories || '|' || pet_name from public.profiles where username = 'ana'),
  'tlacuache|corona,bufanda|Pepe', '…y se ignora');

select * from finish();
rollback;
