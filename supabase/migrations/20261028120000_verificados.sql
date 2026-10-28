-- Retos físicos verificados con Health Connect: el cliente marca `verified` en la instancia al completarla;
-- aquí solo se cuentan para mostrarlos en grupos y clasificación. Nada cambia en cómo se suma la XP.

create or replace function public.weekly_verified(u uuid)
returns int language sql stable security definer set search_path = '' as $$
  select count(*)::int from public.user_records
  where user_id = u and entity = 'instance' and not deleted
    and data ->> 'status' = 'COMPLETED' and (data ->> 'verified')::boolean is true
    and (data ->> 'date')::date >= date_trunc('week', now())::date;
$$;

revoke all on function public.weekly_verified(uuid) from public, anon, authenticated;

create or replace function public.my_groups()
returns setof jsonb language sql stable security definer set search_path = '' as $$
  select jsonb_build_object(
    'id', g.id, 'challengeId', g.challenge_id, 'mode', g.mode, 'target', g.target,
    'startsOn', g.starts_on, 'endsOn', g.ends_on, 'ownerId', g.owner, 'myStatus', mine.status,
    'members', (
      select coalesce(jsonb_agg(jsonb_build_object(
        'userId', m.user_id, 'username', p.username, 'displayName', p.display_name, 'status', m.status,
        'completed', c.completed, 'verified', c.verified) order by p.display_name), '[]'::jsonb)
      from public.group_members m join public.profiles p on p.user_id = m.user_id
      cross join lateral (
        select count(*) as completed, count(*) filter (where (r.data ->> 'verified')::boolean is true) as verified
        from public.user_records r
        where r.user_id = m.user_id and r.entity = 'instance' and not r.deleted
          and r.data ->> 'challengeId' = g.challenge_id and r.data ->> 'status' = 'COMPLETED'
          and (r.data ->> 'date')::date between g.starts_on and g.ends_on
      ) c
      where m.group_id = g.id and m.status in ('joined', 'invited')
        and not public.is_blocked_between(public.me(), m.user_id)
    ))
  from public.group_challenges g
  join public.group_members mine on mine.group_id = g.id and mine.user_id = public.me()
  where mine.status in ('joined', 'invited') and g.ends_on >= current_date - 7
  order by g.ends_on;
$$;

create or replace function public.friends_leaderboard()
returns setof jsonb language plpgsql stable security definer set search_path = '' as $$
declare
  me uuid := public.me();
begin
  if not exists (select 1 from public.profiles where user_id = me and leaderboard_opt_in) then
    return;
  end if;
  return query
    select jsonb_build_object('userId', p.user_id, 'username', p.username, 'displayName', p.display_name,
                              'frame', p.frame, 'weeklyXp', public.weekly_xp(p.user_id), 'isMe', p.user_id = me,
                              'weeklyVerified', public.weekly_verified(p.user_id))
    from public.profiles p
    where p.leaderboard_opt_in and (p.user_id = me or public.are_friends(me, p.user_id))
    order by public.weekly_xp(p.user_id) desc, p.display_name
    limit 50;
end;
$$;
