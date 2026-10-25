-- LevelUp: esquema del servidor.
--
-- El teléfono es la fuente inmediata de lectura y escritura (Room). El servidor guarda una copia por
-- usuario para recuperar el progreso y usar varios teléfonos, y aloja lo social. Toda tabla tiene RLS;
-- lo que toca a otra persona (amistades, reacciones, grupos) solo se escribe con funciones que validan.

-- ════════════════════════════════════════════════════════════════════════════════════════════════
-- Datos personales sincronizados
-- ════════════════════════════════════════════════════════════════════════════════════════════════

create sequence public.sync_seq;
grant usage on sequence public.sync_seq to authenticated;

-- Una fila por registro de la base local. shortcut: `data` es la fila de Room en JSON en vez de una tabla
-- por entidad; basta para copiar, fusionar y contar. Normalizar si el servidor necesita más consultas.
create table public.user_records (
  user_id uuid not null default auth.uid() references auth.users (id) on delete cascade,
  entity text not null check (entity in
    ('preferences', 'custom_category', 'custom_challenge', 'instance', 'xp', 'achievement', 'mission_run', 'pause')),
  id text not null check (char_length(id) between 1 and 120),
  data jsonb not null check (jsonb_typeof(data) = 'object' and pg_column_size(data) <= 16384),
  -- Reloj del teléfono: decide entre dos ediciones del mismo registro (gana la más reciente).
  client_updated_at bigint not null,
  deleted boolean not null default false,
  -- Reloj del servidor: orden de descarga y semana de la clasificación. No se fía del teléfono.
  seq bigint not null default nextval('public.sync_seq'),
  server_created_at timestamptz not null default now(),
  server_updated_at timestamptz not null default now(),
  primary key (user_id, entity, id),
  -- La XP lleva un id determinista (tipo:origen): el mismo reto completado en dos teléfonos es una sola fila.
  constraint xp_valid check (entity <> 'xp' or (
    data ->> 'sourceType' in ('CHALLENGE', 'ACHIEVEMENT', 'MISSION')
    and id = (data ->> 'sourceType') || ':' || (data ->> 'sourceId')
    and jsonb_typeof(data -> 'amount') = 'number'
    and (data ->> 'amount')::int between 1 and 500
  ))
);

create index user_records_seq on public.user_records (user_id, seq);
create index user_records_instances on public.user_records (user_id, (data ->> 'challengeId'))
  where entity = 'instance';
create index user_records_xp_week on public.user_records (user_id, server_created_at)
  where entity = 'xp' and not deleted;

alter table public.user_records enable row level security;

create policy "cada quien lee lo suyo" on public.user_records
  for select to authenticated using (user_id = (select auth.uid()));
create policy "cada quien escribe lo suyo" on public.user_records
  for insert to authenticated with check (user_id = (select auth.uid()));
create policy "cada quien cambia lo suyo" on public.user_records
  for update to authenticated using (user_id = (select auth.uid())) with check (user_id = (select auth.uid()));
-- Sin política de DELETE: borrar es marcar `deleted`, para que el resto de teléfonos se entere.

/**
 * Sube cambios. Idempotente: repetir el mismo lote no cambia nada. Cada registro se aplica solo si es
 * más reciente que lo guardado; en un empate se queda lo que ya estaba (así, al fusionar un teléfono
 * nuevo, los datos de la cuenta no se pisan con los de invitado). El bloqueo por usuario hace que `seq` avance en orden de commit,
 * así la descarga por cursor nunca se salta filas.
 */
create function public.push_records(records jsonb)
returns void
language plpgsql
security invoker
set search_path = ''
as $$
declare
  me uuid := auth.uid();
begin
  if me is null then
    raise exception 'not_authenticated' using errcode = '28000';
  end if;
  if jsonb_typeof(records) <> 'array' or jsonb_array_length(records) > 500 then
    raise exception 'invalid_batch' using errcode = '22023';
  end if;

  perform pg_advisory_xact_lock(hashtextextended(me::text, 0));

  insert into public.user_records as t (user_id, entity, id, data, client_updated_at, deleted)
  select me, r.entity, r.id, coalesce(r.data, '{}'::jsonb), r."updatedAt", coalesce(r.deleted, false)
  from jsonb_to_recordset(records) as r(entity text, id text, data jsonb, "updatedAt" bigint, deleted boolean)
  on conflict (user_id, entity, id) do update
    set data = excluded.data,
        client_updated_at = excluded.client_updated_at,
        deleted = excluded.deleted,
        seq = nextval('public.sync_seq'),
        server_updated_at = now()
    where excluded.client_updated_at > t.client_updated_at;
end;
$$;

revoke all on function public.push_records(jsonb) from public, anon;
grant execute on function public.push_records(jsonb) to authenticated;

-- ════════════════════════════════════════════════════════════════════════════════════════════════
-- Perfil social (opcional: sin perfil, LevelUp funciona igual)
-- ════════════════════════════════════════════════════════════════════════════════════════════════

create table public.profiles (
  user_id uuid primary key references auth.users (id) on delete cascade,
  username text not null check (username ~ '^[a-z0-9_]{3,20}$'),
  display_name text not null check (char_length(btrim(display_name)) between 1 and 30 and display_name !~ '[\x00-\x1f]'),
  frame text not null default 'frame.basic' check (frame ~ '^[a-z0-9_.-]{1,40}$'),
  discoverable boolean not null default true,     -- aparece al buscar por nombre de usuario
  share_progress boolean not null default true,   -- sus amigos ven nivel, racha y XP de la semana
  show_activity boolean not null default false,   -- sus amigos ven cuándo usó la app por última vez
  leaderboard_opt_in boolean not null default false,
  current_streak int not null default 0 check (current_streak between 0 and 100000),
  last_active_at timestamptz,
  created_at timestamptz not null default now()
);

create unique index profiles_username on public.profiles (username);

-- Nombres que se prestan a suplantar al equipo de la app.
alter table public.profiles add constraint username_not_reserved check (username !~
  '^(admin|administrador|levelup|soporte|support|moderador|moderator|staff|oficial|official|sistema|system)');

alter table public.profiles enable row level security;

-- El perfil propio se lee y edita directamente; los ajenos solo a través de las funciones de abajo,
-- que devuelven únicamente lo que su dueño permite.
create policy "perfil propio: leer" on public.profiles
  for select to authenticated using (user_id = (select auth.uid()));
create policy "perfil propio: crear" on public.profiles
  for insert to authenticated with check (user_id = (select auth.uid()));
create policy "perfil propio: cambiar" on public.profiles
  for update to authenticated using (user_id = (select auth.uid())) with check (user_id = (select auth.uid()));

create table public.blocks (
  blocker uuid not null references auth.users (id) on delete cascade,
  blocked uuid not null references auth.users (id) on delete cascade,
  created_at timestamptz not null default now(),
  primary key (blocker, blocked),
  check (blocker <> blocked)
);

alter table public.blocks enable row level security;
create policy "bloqueos propios: leer" on public.blocks
  for select to authenticated using (blocker = (select auth.uid()));
create policy "bloqueos propios: quitar" on public.blocks
  for delete to authenticated using (blocker = (select auth.uid()));
-- Bloquear va por block_user(), que además corta la amistad y las solicitudes.

create table public.friend_requests (
  id uuid primary key default gen_random_uuid(),
  sender uuid not null references auth.users (id) on delete cascade,
  receiver uuid not null references auth.users (id) on delete cascade,
  status text not null default 'pending' check (status in ('pending', 'accepted', 'declined', 'cancelled')),
  created_at timestamptz not null default now(),
  responded_at timestamptz,
  check (sender <> receiver)
);

create unique index friend_requests_one_pending on public.friend_requests
  (least(sender, receiver), greatest(sender, receiver)) where status = 'pending';
create index friend_requests_receiver on public.friend_requests (receiver, status);
create index friend_requests_sender on public.friend_requests (sender, created_at);

alter table public.friend_requests enable row level security;
create policy "solicitudes propias: leer" on public.friend_requests
  for select to authenticated using ((select auth.uid()) in (sender, receiver));

create table public.friendships (
  user_a uuid not null references auth.users (id) on delete cascade,
  user_b uuid not null references auth.users (id) on delete cascade,
  created_at timestamptz not null default now(),
  primary key (user_a, user_b),
  check (user_a < user_b)
);

create index friendships_b on public.friendships (user_b);

alter table public.friendships enable row level security;
create policy "amistades propias: leer" on public.friendships
  for select to authenticated using ((select auth.uid()) in (user_a, user_b));

create table public.reactions (
  id uuid primary key default gen_random_uuid(),
  sender uuid not null references auth.users (id) on delete cascade,
  receiver uuid not null references auth.users (id) on delete cascade,
  -- Sin texto libre: solo reacciones predefinidas, así no hace falta moderar mensajes.
  kind text not null check (kind in ('aplauso', 'fuerza', 'estrella', 'fuego', 'corazon')),
  created_at timestamptz not null default now(),
  check (sender <> receiver)
);

create index reactions_receiver on public.reactions (receiver, created_at desc);
create index reactions_sender on public.reactions (sender, created_at);

alter table public.reactions enable row level security;
create policy "reacciones propias: leer" on public.reactions
  for select to authenticated using ((select auth.uid()) in (sender, receiver));

create table public.group_challenges (
  id uuid primary key default gen_random_uuid(),
  owner uuid not null references auth.users (id) on delete cascade,
  -- Solo retos del catálogo (ids como "fis.caminar-10"): nada de títulos escritos por usuarios.
  challenge_id text not null check (challenge_id ~ '^[a-z]{2,4}\.[a-z0-9-]{1,40}$'),
  mode text not null check (mode in ('coop', 'versus')),
  target int not null check (target between 1 and 500),
  starts_on date not null,
  ends_on date not null,
  created_at timestamptz not null default now(),
  check (ends_on >= starts_on and ends_on - starts_on <= 60)
);

create table public.group_members (
  group_id uuid not null references public.group_challenges (id) on delete cascade,
  user_id uuid not null references auth.users (id) on delete cascade,
  status text not null check (status in ('invited', 'joined', 'declined', 'left')),
  invited_by uuid references auth.users (id) on delete set null,
  joined_at timestamptz,
  primary key (group_id, user_id)
);

create index group_members_user on public.group_members (user_id, status);

alter table public.group_challenges enable row level security;
alter table public.group_members enable row level security;

create function public.is_group_member(g uuid)
returns boolean language sql stable security definer set search_path = '' as $$
  select exists (select 1 from public.group_members m where m.group_id = g and m.user_id = auth.uid());
$$;

create policy "grupos donde estoy: leer" on public.group_challenges
  for select to authenticated using (public.is_group_member(id));
create policy "miembros de mis grupos: leer" on public.group_members
  for select to authenticated using (public.is_group_member(group_id));

create table public.notifications (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users (id) on delete cascade,
  kind text not null check (kind in ('friend_request', 'friend_accepted', 'reaction', 'group_invite', 'group_joined')),
  actor uuid references auth.users (id) on delete cascade,
  ref_id text,
  read boolean not null default false,
  created_at timestamptz not null default now()
);

create index notifications_user on public.notifications (user_id, created_at desc);

alter table public.notifications enable row level security;
create policy "avisos propios: leer" on public.notifications
  for select to authenticated using (user_id = (select auth.uid()));
create policy "avisos propios: marcar leídos" on public.notifications
  for update to authenticated using (user_id = (select auth.uid())) with check (user_id = (select auth.uid()));
create policy "avisos propios: borrar" on public.notifications
  for delete to authenticated using (user_id = (select auth.uid()));

create table public.reports (
  id uuid primary key default gen_random_uuid(),
  reporter uuid references auth.users (id) on delete set null,
  reported uuid not null references auth.users (id) on delete cascade,
  reason text not null check (reason in ('nombre_ofensivo', 'acoso', 'spam', 'suplantacion', 'otro')),
  status text not null default 'abierta' check (status in ('abierta', 'revisada', 'descartada')),
  created_at timestamptz not null default now()
);

-- Las denuncias solo las ve el propietario desde el panel de Supabase (sin políticas = nadie más).
alter table public.reports enable row level security;

-- Permisos explícitos (las políticas de arriba deciden qué filas). El rol anon no toca nada.
revoke all on table public.user_records, public.profiles, public.blocks, public.friend_requests, public.friendships,
  public.reactions, public.group_challenges, public.group_members, public.notifications, public.reports from anon, authenticated;
grant select, insert, update on table public.user_records, public.profiles to authenticated;
grant select, delete on table public.blocks to authenticated;
grant select on table public.friend_requests, public.friendships, public.reactions, public.group_challenges,
  public.group_members to authenticated;
grant select, update, delete on table public.notifications to authenticated;

-- ════════════════════════════════════════════════════════════════════════════════════════════════
-- Ayudas internas
-- ════════════════════════════════════════════════════════════════════════════════════════════════

create function public.me()
returns uuid language plpgsql stable set search_path = '' as $$
begin
  if auth.uid() is null then
    raise exception 'not_authenticated' using errcode = '28000';
  end if;
  return auth.uid();
end;
$$;

create function public.are_friends(a uuid, b uuid)
returns boolean language sql stable security definer set search_path = '' as $$
  select exists (select 1 from public.friendships where user_a = least(a, b) and user_b = greatest(a, b));
$$;

create function public.is_blocked_between(a uuid, b uuid)
returns boolean language sql stable security definer set search_path = '' as $$
  select exists (select 1 from public.blocks where (blocker = a and blocked = b) or (blocker = b and blocked = a));
$$;

-- Nivel a partir de la XP total, misma fórmula que XpRules (total del nivel L = 50·L·(L−1)).
create function public.level_for(xp bigint)
returns int language sql immutable set search_path = '' as $$
  select greatest(1, floor((1 + sqrt(1 + 0.08 * greatest(xp, 0))) / 2)::int);
$$;

create function public.total_xp(u uuid)
returns bigint language sql stable security definer set search_path = '' as $$
  select coalesce(sum((data ->> 'amount')::int), 0) from public.user_records
  where user_id = u and entity = 'xp' and not deleted;
$$;

-- XP de esta semana (lunes a domingo, hora del servidor). Como mucho 600 por día cuentan: evita que
-- subir de golpe XP inventada convierta la clasificación en otra cosa.
create function public.weekly_xp(u uuid)
returns bigint language sql stable security definer set search_path = '' as $$
  select coalesce(sum(least(day_xp, 600)), 0) from (
    select date_trunc('day', server_created_at) as day, sum((data ->> 'amount')::int) as day_xp
    from public.user_records
    where user_id = u and entity = 'xp' and not deleted and server_created_at >= date_trunc('week', now())
    group by 1
  ) d;
$$;

create function public.notify(target uuid, what text, who uuid, ref text)
returns void language sql security definer set search_path = '' as $$
  insert into public.notifications (user_id, kind, actor, ref_id) values (target, what, who, ref);
$$;

revoke all on function public.is_group_member(uuid), public.me(), public.are_friends(uuid, uuid),
  public.is_blocked_between(uuid, uuid), public.total_xp(uuid), public.weekly_xp(uuid),
  public.notify(uuid, text, uuid, text) from public, anon, authenticated;
grant execute on function public.is_group_member(uuid) to authenticated;

-- ════════════════════════════════════════════════════════════════════════════════════════════════
-- Funciones que usa la app (todas exigen sesión y validan del lado del servidor)
-- ════════════════════════════════════════════════════════════════════════════════════════════════

/** Lo que una persona deja ver de sí misma a quien la mira. */
create function public.profile_card(viewer uuid, p public.profiles)
returns jsonb language sql stable security definer set search_path = '' as $$
  select jsonb_build_object(
    'userId', p.user_id,
    'username', p.username,
    'displayName', p.display_name,
    'frame', p.frame,
    'isFriend', public.are_friends(viewer, p.user_id),
    'level', case when p.share_progress and public.are_friends(viewer, p.user_id) then public.level_for(public.total_xp(p.user_id)) end,
    'streak', case when p.share_progress and public.are_friends(viewer, p.user_id) then p.current_streak end,
    'weeklyXp', case when p.share_progress and public.are_friends(viewer, p.user_id) then public.weekly_xp(p.user_id) end,
    'lastActiveAt', case when p.show_activity and public.are_friends(viewer, p.user_id) then p.last_active_at end
  );
$$;
revoke all on function public.profile_card(uuid, public.profiles) from public, anon, authenticated;

create function public.search_profiles(query text)
returns setof jsonb language plpgsql stable security definer set search_path = '' as $$
declare
  me uuid := public.me();
  q text := lower(btrim(query));
begin
  if char_length(q) < 3 then
    return;
  end if;
  return query
    select public.profile_card(me, p) from public.profiles p
    where p.username like replace(replace(q, '%', ''), '_', '\_') || '%'
      and p.discoverable and p.user_id <> me
      and not public.is_blocked_between(me, p.user_id)
    order by p.username
    limit 20;
end;
$$;

create function public.send_friend_request(target_username text)
returns uuid language plpgsql security definer set search_path = '' as $$
declare
  me uuid := public.me();
  target uuid;
  new_id uuid;
begin
  if not exists (select 1 from public.profiles where user_id = me) then
    raise exception 'profile_required' using errcode = 'P0001';
  end if;
  select user_id into target from public.profiles where username = lower(btrim(target_username));
  if target is null or target = me or public.is_blocked_between(me, target) then
    -- Mismo error para "no existe" y "te bloqueó": no se revela quién bloquea a quién.
    raise exception 'user_not_found' using errcode = 'P0001';
  end if;
  if public.are_friends(me, target) then
    raise exception 'already_friends' using errcode = 'P0001';
  end if;
  if (select count(*) from public.friend_requests where sender = me and status = 'pending') >= 20
     or (select count(*) from public.friend_requests where sender = me and created_at > now() - interval '1 day') >= 30 then
    raise exception 'rate_limited' using errcode = 'P0001';
  end if;
  if exists (select 1 from public.friend_requests where sender = me and receiver = target
             and status = 'declined' and responded_at > now() - interval '30 days') then
    raise exception 'recently_declined' using errcode = 'P0001';
  end if;
  -- Si la otra persona ya me había invitado, aceptar en vez de duplicar.
  select id into new_id from public.friend_requests where sender = target and receiver = me and status = 'pending';
  if new_id is not null then
    perform public.respond_friend_request(new_id, true);
    return new_id;
  end if;
  insert into public.friend_requests (sender, receiver) values (me, target)
    on conflict do nothing returning id into new_id;
  if new_id is null then
    raise exception 'already_requested' using errcode = 'P0001';
  end if;
  perform public.notify(target, 'friend_request', me, new_id::text);
  return new_id;
end;
$$;

create function public.respond_friend_request(request_id uuid, accept boolean)
returns void language plpgsql security definer set search_path = '' as $$
declare
  me uuid := public.me();
  req public.friend_requests;
begin
  select * into req from public.friend_requests where id = request_id and receiver = me and status = 'pending' for update;
  if req.id is null then
    raise exception 'request_not_found' using errcode = 'P0001';
  end if;
  update public.friend_requests set status = case when accept then 'accepted' else 'declined' end, responded_at = now()
    where id = request_id;
  if accept then
    insert into public.friendships (user_a, user_b) values (least(me, req.sender), greatest(me, req.sender))
      on conflict do nothing;
    perform public.notify(req.sender, 'friend_accepted', me, request_id::text);
  end if;
end;
$$;

create function public.cancel_friend_request(request_id uuid)
returns void language plpgsql security definer set search_path = '' as $$
begin
  update public.friend_requests set status = 'cancelled', responded_at = now()
    where id = request_id and sender = public.me() and status = 'pending';
end;
$$;

create function public.remove_friend(friend uuid)
returns void language plpgsql security definer set search_path = '' as $$
declare
  me uuid := public.me();
begin
  delete from public.friendships where user_a = least(me, friend) and user_b = greatest(me, friend);
end;
$$;

create function public.block_user(target uuid)
returns void language plpgsql security definer set search_path = '' as $$
#variable_conflict use_variable
declare
  me uuid := public.me();
begin
  if target = me then
    raise exception 'invalid_target' using errcode = 'P0001';
  end if;
  insert into public.blocks (blocker, blocked) values (me, target) on conflict do nothing;
  delete from public.friendships where user_a = least(me, target) and user_b = greatest(me, target);
  update public.friend_requests set status = 'cancelled', responded_at = now()
    where status = 'pending' and ((sender = me and receiver = target) or (sender = target and receiver = me));
  delete from public.notifications where (user_id = me and actor = target) or (user_id = target and actor = me);
  -- Invitaciones pendientes entre los dos a retos en grupo: se retiran.
  delete from public.group_members m using public.group_challenges g
    where m.group_id = g.id and m.status = 'invited'
      and ((m.user_id = me and m.invited_by = target) or (m.user_id = target and m.invited_by = me));
end;
$$;

create function public.friends()
returns setof jsonb language sql stable security definer set search_path = '' as $$
  select public.profile_card(public.me(), p) from public.friendships f
  join public.profiles p on p.user_id = case when f.user_a = public.me() then f.user_b else f.user_a end
  where public.me() in (f.user_a, f.user_b)
  order by p.display_name;
$$;

create function public.pending_requests()
returns setof jsonb language sql stable security definer set search_path = '' as $$
  select public.profile_card(public.me(), p) || jsonb_build_object(
    'requestId', r.id, 'incoming', r.receiver = public.me(), 'createdAt', r.created_at)
  from public.friend_requests r
  join public.profiles p on p.user_id = case when r.sender = public.me() then r.receiver else r.sender end
  where r.status = 'pending' and public.me() in (r.sender, r.receiver)
  order by r.created_at desc;
$$;

create function public.send_reaction(target uuid, reaction text)
returns void language plpgsql security definer set search_path = '' as $$
declare
  me uuid := public.me();
  new_id uuid;
begin
  if not public.are_friends(me, target) or public.is_blocked_between(me, target) then
    raise exception 'not_friends' using errcode = 'P0001';
  end if;
  if (select count(*) from public.reactions where sender = me and created_at > now() - interval '1 day') >= 50
     or (select count(*) from public.reactions where sender = me and receiver = target and created_at > now() - interval '1 day') >= 5 then
    raise exception 'rate_limited' using errcode = 'P0001';
  end if;
  insert into public.reactions (sender, receiver, kind) values (me, target, reaction) returning id into new_id;
  perform public.notify(target, 'reaction', me, reaction);
end;
$$;

create function public.my_notifications()
returns setof jsonb language sql stable security definer set search_path = '' as $$
  select jsonb_build_object(
    'id', n.id, 'kind', n.kind, 'refId', n.ref_id, 'read', n.read, 'createdAt', n.created_at,
    'actor', case when p.user_id is not null then jsonb_build_object('userId', p.user_id, 'username', p.username, 'displayName', p.display_name) end)
  from public.notifications n
  left join public.profiles p on p.user_id = n.actor
  where n.user_id = public.me() and n.created_at > now() - interval '90 days'
  order by n.created_at desc
  limit 100;
$$;

create function public.create_group_challenge(
  challenge text, group_mode text, goal int, starts date, ends date, invitees uuid[])
returns uuid language plpgsql security definer set search_path = '' as $$
declare
  me uuid := public.me();
  new_id uuid;
  friend uuid;
begin
  if not exists (select 1 from public.profiles where user_id = me) then
    raise exception 'profile_required' using errcode = 'P0001';
  end if;
  if coalesce(array_length(invitees, 1), 0) not between 1 and 9 then
    raise exception 'invalid_members' using errcode = 'P0001';
  end if;
  if starts < current_date - 1 then
    raise exception 'invalid_dates' using errcode = 'P0001';
  end if;
  if (select count(*) from public.group_challenges where owner = me and created_at > now() - interval '1 day') >= 10 then
    raise exception 'rate_limited' using errcode = 'P0001';
  end if;
  insert into public.group_challenges (owner, challenge_id, mode, target, starts_on, ends_on)
    values (me, challenge, group_mode, goal, starts, ends) returning id into new_id;
  insert into public.group_members (group_id, user_id, status, joined_at) values (new_id, me, 'joined', now());
  foreach friend in array invitees loop
    if friend = me or not public.are_friends(me, friend) or public.is_blocked_between(me, friend) then
      raise exception 'not_friends' using errcode = 'P0001';
    end if;
    insert into public.group_members (group_id, user_id, status, invited_by) values (new_id, friend, 'invited', me)
      on conflict do nothing;
    perform public.notify(friend, 'group_invite', me, new_id::text);
  end loop;
  return new_id;
end;
$$;

create function public.respond_group_invite(group_id uuid, accept boolean)
returns void language plpgsql security definer set search_path = '' as $$
declare
  me uuid := public.me();
  inviter uuid;
begin
  update public.group_members m set status = case when accept then 'joined' else 'declined' end,
         joined_at = case when accept then now() end
    where m.group_id = respond_group_invite.group_id and m.user_id = me and m.status = 'invited'
    returning m.invited_by into inviter;
  if not found then
    raise exception 'invite_not_found' using errcode = 'P0001';
  end if;
  if accept and inviter is not null then
    perform public.notify(inviter, 'group_joined', me, group_id::text);
  end if;
end;
$$;

create function public.leave_group(group_id uuid)
returns void language plpgsql security definer set search_path = '' as $$
begin
  update public.group_members m set status = 'left'
    where m.group_id = leave_group.group_id and m.user_id = public.me() and m.status = 'joined';
end;
$$;

/**
 * Retos en grupo donde participo o me invitaron, con el avance de cada miembro. El avance se cuenta en el
 * servidor a partir de los retos completados que cada miembro ya sincronizó (no lo declara el cliente).
 */
create function public.my_groups()
returns setof jsonb language sql stable security definer set search_path = '' as $$
  select jsonb_build_object(
    'id', g.id, 'challengeId', g.challenge_id, 'mode', g.mode, 'target', g.target,
    'startsOn', g.starts_on, 'endsOn', g.ends_on, 'ownerId', g.owner, 'myStatus', mine.status,
    'members', (
      select coalesce(jsonb_agg(jsonb_build_object(
        'userId', m.user_id, 'username', p.username, 'displayName', p.display_name, 'status', m.status,
        'completed', (
          select count(*) from public.user_records r
          where r.user_id = m.user_id and r.entity = 'instance' and not r.deleted
            and r.data ->> 'challengeId' = g.challenge_id and r.data ->> 'status' = 'COMPLETED'
            and (r.data ->> 'date')::date between g.starts_on and g.ends_on
        )) order by p.display_name), '[]'::jsonb)
      from public.group_members m join public.profiles p on p.user_id = m.user_id
      where m.group_id = g.id and m.status in ('joined', 'invited')
        and not public.is_blocked_between(public.me(), m.user_id)
    ))
  from public.group_challenges g
  join public.group_members mine on mine.group_id = g.id and mine.user_id = public.me()
  where mine.status in ('joined', 'invited') and g.ends_on >= current_date - 7
  order by g.ends_on;
$$;

/** Clasificación semanal entre amigos que eligieron aparecer. Quien no participa no ve ni sale. */
create function public.friends_leaderboard()
returns setof jsonb language plpgsql stable security definer set search_path = '' as $$
declare
  me uuid := public.me();
begin
  if not exists (select 1 from public.profiles where user_id = me and leaderboard_opt_in) then
    return;
  end if;
  return query
    select jsonb_build_object('userId', p.user_id, 'username', p.username, 'displayName', p.display_name,
                              'frame', p.frame, 'weeklyXp', public.weekly_xp(p.user_id), 'isMe', p.user_id = me)
    from public.profiles p
    where p.leaderboard_opt_in and (p.user_id = me or public.are_friends(me, p.user_id))
    order by public.weekly_xp(p.user_id) desc, p.display_name
    limit 50;
end;
$$;

create function public.report_user(target uuid, why text)
returns void language plpgsql security definer set search_path = '' as $$
declare
  me uuid := public.me();
begin
  if target = me then
    raise exception 'invalid_target' using errcode = 'P0001';
  end if;
  if (select count(*) from public.reports where reporter = me and created_at > now() - interval '1 day') >= 10 then
    raise exception 'rate_limited' using errcode = 'P0001';
  end if;
  insert into public.reports (reporter, reported, reason) values (me, target, why);
end;
$$;

/** Racha, marco y última actividad que ven los amigos (si la persona lo permite). La app la llama al sincronizar. */
create function public.touch_profile(streak int, avatar_frame text)
returns void language sql security definer set search_path = '' as $$
  update public.profiles
  set current_streak = greatest(0, least(streak, 100000)),
      frame = case when avatar_frame ~ '^[a-z0-9_.-]{1,40}$' then avatar_frame else frame end,
      last_active_at = now()
  where user_id = public.me();
$$;

/** Borra la cuenta y, en cascada, todo lo guardado en el servidor: progreso, perfil, amistades, grupos y avisos. */
create function public.delete_account()
returns void language plpgsql security definer set search_path = '' as $$
begin
  delete from auth.users where id = public.me();
end;
$$;

do $$
declare
  f text;
begin
  foreach f in array array[
    'search_profiles(text)', 'send_friend_request(text)', 'respond_friend_request(uuid, boolean)',
    'cancel_friend_request(uuid)', 'remove_friend(uuid)', 'block_user(uuid)', 'friends()', 'pending_requests()',
    'send_reaction(uuid, text)', 'my_notifications()',
    'create_group_challenge(text, text, integer, date, date, uuid[])', 'respond_group_invite(uuid, boolean)',
    'leave_group(uuid)', 'my_groups()', 'friends_leaderboard()', 'report_user(uuid, text)', 'touch_profile(integer, text)',
    'delete_account()'
  ] loop
    execute format('revoke all on function public.%s from public, anon', f);
    execute format('grant execute on function public.%s to authenticated', f);
  end loop;
end;
$$;

-- Retención: los avisos sociales se borran a los 90 días y las solicitudes ya respondidas al año.
create extension if not exists pg_cron;
select cron.schedule('levelup-retencion', '17 4 * * *', $$
  delete from public.notifications where created_at < now() - interval '90 days';
  delete from public.friend_requests where status <> 'pending' and responded_at < now() - interval '365 days';
$$);
