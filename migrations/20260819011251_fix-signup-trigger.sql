-- InsForge auth.users stores metadata in `profile` (jsonb) + `metadata` (jsonb)
-- rather than Supabase's `raw_user_meta_data`. Rewrite the trigger BODY only
-- (we don't own auth.users, but the function `handle_new_user()` lives in
-- public and is ours to redefine).

create or replace function handle_new_user()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
  insert into profiles (id, username, display_name)
  values (
    new.id,
    coalesce(
      nullif(new.profile->>'username', ''),
      nullif(new.metadata->>'username', ''),
      'player_' || substr(replace(new.id::text, '-', ''), 1, 8)
    ),
    coalesce(
      nullif(new.profile->>'name', ''),
      nullif(new.profile->>'display_name', ''),
      nullif(new.metadata->>'display_name', ''),
      split_part(new.email, '@', 1),
      'Player'
    )
  )
  on conflict (id) do nothing;

  insert into profile_stats (user_id) values (new.id)
  on conflict (user_id) do nothing;

  return new;
exception when others then
  return new;
end;
$$;
