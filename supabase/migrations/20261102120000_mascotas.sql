-- Mascotas de racha en el perfil social: la que lleva la persona, sus accesorios y el nombre que le puso.
-- Se ven donde ya se ve el avatar (búsqueda, amigos y solicitudes). Solo claves cortas y un nombre corto.

alter table public.profiles
  add column pet text not null default '' check (pet ~ '^[a-z_]{0,20}$'),
  add column pet_accessories text not null default '' check (char_length(pet_accessories) <= 300 and pet_accessories ~ '^[a-z_,]*$'),
  add column pet_name text not null default '' check (char_length(pet_name) <= 20 and pet_name !~ '[[:cntrl:]]');

/** Lo que una persona deja ver de sí misma a quien la mira. Igual que antes, más su mascota (como el avatar: a quien ve la tarjeta). */
create or replace function public.profile_card(viewer uuid, p public.profiles)
returns jsonb language sql stable security definer set search_path = '' as $$
  with visible as (select p.share_progress and public.are_friends(viewer, p.user_id) as progress)
  select jsonb_build_object(
    'userId', p.user_id,
    'username', p.username,
    'displayName', p.display_name,
    'frame', p.frame,
    'avatar', p.avatar,
    'pet', p.pet,
    'petAccessories', p.pet_accessories,
    'petName', p.pet_name,
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

/** Como touch_profile de 3 argumentos (que sigue para las versiones viejas), y además la mascota. Lo inválido se ignora. */
create function public.touch_profile(streak int, avatar_frame text, avatar_key text, pet_key text, pet_extras text, pet_label text)
returns void language sql security definer set search_path = '' as $$
  update public.profiles
  set current_streak = greatest(0, least(streak, 100000)),
      frame = case when avatar_frame ~ '^[a-z0-9_.-]{1,40}$' then avatar_frame else frame end,
      avatar = case when avatar_key ~ '^[a-z_]{0,20}$' then avatar_key else avatar end,
      pet = case when pet_key ~ '^[a-z_]{0,20}$' then pet_key else pet end,
      pet_accessories = case when char_length(pet_extras) <= 300 and pet_extras ~ '^[a-z_,]*$' then pet_extras else pet_accessories end,
      pet_name = case when char_length(pet_label) <= 20 and pet_label !~ '[[:cntrl:]]' then pet_label else pet_name end,
      last_active_at = now()
  where user_id = public.me();
$$;

revoke all on function public.touch_profile(integer, text, text, text, text, text) from public, anon;
grant execute on function public.touch_profile(integer, text, text, text, text, text) to authenticated;
