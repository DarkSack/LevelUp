-- LevelUp: favoritos, metas, XP de regreso, mensajes de ánimo, perfil con avatar, título y trofeos,
-- e historial de retos en grupo.
--
-- Aplicar ANTES de publicar la versión de la app que sincroniza favoritos y metas: el servidor rechaza
-- entidades desconocidas y un lote con una sola fila rechazada no se guarda entero.

-- ════════════════════════════════════════════════════════════════════════════════════════════════
-- Sincronización
-- ════════════════════════════════════════════════════════════════════════════════════════════════

alter table public.user_records drop constraint user_records_entity_check;
alter table public.user_records add constraint user_records_entity_check check (entity in
  ('preferences', 'custom_category', 'custom_challenge', 'instance', 'xp', 'achievement', 'mission_run', 'pause',
   'favorite', 'goal'));

-- COMEBACK: el primer reto tras una semana o más sin actividad (id «COMEBACK:<fecha>», uno por día como mucho).
alter table public.user_records drop constraint xp_valid;
alter table public.user_records add constraint xp_valid check (entity <> 'xp' or (
  data ->> 'sourceType' in ('CHALLENGE', 'ACHIEVEMENT', 'MISSION', 'COMEBACK')
  and id = (data ->> 'sourceType') || ':' || (data ->> 'sourceId')
  and jsonb_typeof(data -> 'amount') = 'number'
  and (data ->> 'amount')::int between 1 and 500
));

-- ════════════════════════════════════════════════════════════════════════════════════════════════
-- Mensajes de ánimo: más reacciones predefinidas. Siguen sin texto libre, así no hay nada que moderar.
-- ════════════════════════════════════════════════════════════════════════════════════════════════

alter table public.reactions drop constraint reactions_kind_check;
alter table public.reactions add constraint reactions_kind_check check (kind in
  ('aplauso', 'fuerza', 'estrella', 'fuego', 'corazon',
   'animo_puedes', 'animo_sigue', 'animo_paso', 'animo_contigo', 'animo_descansa'));

-- ════════════════════════════════════════════════════════════════════════════════════════════════
-- Perfil: avatar (clave de un emoji predefinido), título elegido y hasta tres trofeos para mostrar
-- ════════════════════════════════════════════════════════════════════════════════════════════════

alter table public.profiles
  add column avatar text not null default '' check (avatar ~ '^[a-z_]{0,20}$'),
  -- Nivel del título (los de XpRules.TITLES). Solo se muestra si de verdad se alcanzó ese nivel.
  add column title_level int check (title_level in (1, 3, 5, 8, 12, 16, 20, 30)),
  -- Ids de logros. Solo se muestran los que están desbloqueados en los datos sincronizados.
  add column trophies text[] not null default '{}' check (
    cardinality(trophies) <= 3 and array_to_string(trophies, ',') ~ '^[a-z0-9_.,-]*$');

/** Lo que una persona deja ver de sí misma a quien la mira. Título y trofeos, como el nivel: solo amigos y si lo permite. */
create or replace function public.profile_card(viewer uuid, p public.profiles)
returns jsonb language sql stable security definer set search_path = '' as $$
  with visible as (select p.share_progress and public.are_friends(viewer, p.user_id) as progress)
  select jsonb_build_object(
    'userId', p.user_id,
    'username', p.username,
    'displayName', p.display_name,
    'frame', p.frame,
    'avatar', p.avatar,
    'isFriend', public.are_friends(viewer, p.user_id),
    'level', case when v.progress then public.level_for(public.total_xp(p.user_id)) end,
    'streak', case when v.progress then p.current_streak end,
    'weeklyXp', case when v.progress then public.weekly_xp(p.user_id) end,
    'title', case when v.progress and p.title_level <= public.level_for(public.total_xp(p.user_id)) then p.title_level end,
    'trophies', case when v.progress then (
      select coalesce(jsonb_agg(t order by t), '[]'::jsonb) from unnest(p.trophies) t
      where exists (select 1 from public.user_records r
                    where r.user_id = p.user_id and r.entity = 'achievement' and r.id = t and not r.deleted)
    ) else '[]'::jsonb end,
    'lastActiveAt', case when p.show_activity and public.are_friends(viewer, p.user_id) then p.last_active_at end
  )
  from visible v;
$$;

revoke all on function public.profile_card(uuid, public.profiles) from public, anon, authenticated;

/** Como touch_profile, y además el avatar elegido en la app (Perfil → Avatar y marco). */
create function public.touch_profile(streak int, avatar_frame text, avatar_key text)
returns void language sql security definer set search_path = '' as $$
  update public.profiles
  set current_streak = greatest(0, least(streak, 100000)),
      frame = case when avatar_frame ~ '^[a-z0-9_.-]{1,40}$' then avatar_frame else frame end,
      avatar = case when avatar_key ~ '^[a-z_]{0,20}$' then avatar_key else avatar end,
      last_active_at = now()
  where user_id = public.me();
$$;

-- ════════════════════════════════════════════════════════════════════════════════════════════════
-- Historial de participación en retos en grupo
-- ════════════════════════════════════════════════════════════════════════════════════════════════

/** Retos en grupo terminados (o que dejé), con lo que hice yo y lo que hizo el grupo. */
create function public.my_group_history()
returns setof jsonb language sql stable security definer set search_path = '' as $$
  select jsonb_build_object(
    'id', g.id, 'challengeId', g.challenge_id, 'mode', g.mode, 'target', g.target,
    'startsOn', g.starts_on, 'endsOn', g.ends_on, 'myStatus', mine.status,
    'members', (select count(*) from public.group_members m where m.group_id = g.id and m.status in ('joined', 'left')),
    'myCompleted', (
      select count(*) from public.user_records r
      where r.user_id = public.me() and r.entity = 'instance' and not r.deleted
        and r.data ->> 'challengeId' = g.challenge_id and r.data ->> 'status' = 'COMPLETED'
        and (r.data ->> 'date')::date between g.starts_on and g.ends_on),
    'groupCompleted', (
      select count(*) from public.group_members m
      join public.user_records r on r.user_id = m.user_id
      where m.group_id = g.id and m.status in ('joined', 'left') and r.entity = 'instance' and not r.deleted
        and r.data ->> 'challengeId' = g.challenge_id and r.data ->> 'status' = 'COMPLETED'
        and (r.data ->> 'date')::date between g.starts_on and g.ends_on))
  from public.group_challenges g
  join public.group_members mine on mine.group_id = g.id and mine.user_id = public.me()
  where mine.status = 'left' or (mine.status = 'joined' and g.ends_on < current_date)
  order by g.ends_on desc
  limit 50;
$$;

do $$
declare
  f text;
begin
  foreach f in array array['touch_profile(integer, text, text)', 'my_group_history()'] loop
    execute format('revoke all on function public.%s from public, anon', f);
    execute format('grant execute on function public.%s to authenticated', f);
  end loop;
end;
$$;
