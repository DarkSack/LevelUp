-- Dispositivos con la sesión iniciada: la lista de «Cuenta y sesiones» y cerrar la de uno concreto.

/** Mis sesiones abiertas (una por dispositivo), la más reciente primero; `current` marca la de quien pregunta. */
create function public.my_sessions()
returns setof jsonb language sql stable security definer set search_path = '' as $$
  select jsonb_build_object(
    'id', s.id, 'userAgent', coalesce(s.user_agent, ''), 'createdAt', s.created_at,
    'lastUsedAt', coalesce(s.refreshed_at at time zone 'utc', s.updated_at, s.created_at),
    'current', s.id::text = (auth.jwt() ->> 'session_id'))
  from auth.sessions s
  where s.user_id = public.me()
  order by coalesce(s.refreshed_at at time zone 'utc', s.updated_at, s.created_at) desc
  limit 20;
$$;

/** Cierra la sesión de otro de mis dispositivos: borra la sesión y, en cascada, sus tokens de refresco. */
create function public.end_session(session uuid)
returns void language plpgsql security definer set search_path = '' as $$
begin
  if session::text = (auth.jwt() ->> 'session_id') then
    raise exception 'current_session' using errcode = '22023';
  end if;
  delete from auth.sessions where id = session and user_id = public.me();
end;
$$;

do $$
declare
  f text;
begin
  foreach f in array array['my_sessions()', 'end_session(uuid)'] loop
    execute format('revoke all on function public.%s from public, anon', f);
    execute format('grant execute on function public.%s to authenticated', f);
  end loop;
end;
$$;
