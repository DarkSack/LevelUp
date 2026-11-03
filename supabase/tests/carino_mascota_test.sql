-- Pruebas del cariño a la mascota: entre amigos llega como aviso; a quien no es amigo, no.
begin;
create extension if not exists pgtap with schema extensions;
select plan(3);

insert into auth.users (id, email, aud, role) values
  ('00000000-0000-0000-0000-0000000000a2', 'ana2@example.com', 'authenticated', 'authenticated'),
  ('00000000-0000-0000-0000-0000000000b2', 'beto2@example.com', 'authenticated', 'authenticated'),
  ('00000000-0000-0000-0000-0000000000c2', 'caro2@example.com', 'authenticated', 'authenticated');
insert into public.profiles (user_id, username, display_name) values
  ('00000000-0000-0000-0000-0000000000a2', 'ana2', 'Ana'),
  ('00000000-0000-0000-0000-0000000000b2', 'beto2', 'Beto'),
  ('00000000-0000-0000-0000-0000000000c2', 'caro2', 'Caro');
insert into public.friendships (user_a, user_b) values ('00000000-0000-0000-0000-0000000000a2', '00000000-0000-0000-0000-0000000000b2');

create function pg_temp.as_user(u uuid) returns void language plpgsql as $$
begin
  reset role;
  perform set_config('request.jwt.claims', json_build_object('sub', u, 'role', 'authenticated')::text, true);
  set local role authenticated;
end;
$$;

select pg_temp.as_user('00000000-0000-0000-0000-0000000000a2');
select lives_ok($$ select public.send_reaction('00000000-0000-0000-0000-0000000000b2', 'mascota') $$,
  'Ana le hace cariño a la mascota de Beto');
select throws_ok($$ select public.send_reaction('00000000-0000-0000-0000-0000000000c2', 'mascota') $$,
  'P0001', 'not_friends', 'a quien no es amigo, no');

reset role;
select is((select ref_id from public.notifications where user_id = '00000000-0000-0000-0000-0000000000b2' and kind = 'reaction'),
  'mascota', 'Beto recibe el aviso');

select * from finish();
rollback;
