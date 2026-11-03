-- Recuperar la contraseña sin correo: con la sesión iniciada se crea un código de recuperación (se muestra una
-- sola vez y aquí solo se guarda su hash). Con él, sin sesión, se pone una contraseña nueva. Sirve una vez,
-- cierra las sesiones abiertas y hay un máximo de 5 intentos por cuenta y hora.

create table public.recovery_codes (
  user_id uuid primary key references auth.users (id) on delete cascade,
  code_hash text not null,
  created_at timestamptz not null default now()
);

create table public.recovery_attempts (
  email text not null,
  at timestamptz not null default now()
);
create index recovery_attempts_email on public.recovery_attempts (email, at desc);

-- Nadie las lee desde fuera: solo las funciones de abajo.
alter table public.recovery_codes enable row level security;
alter table public.recovery_attempts enable row level security;
revoke all on table public.recovery_codes, public.recovery_attempts from anon, authenticated;

/** Código nuevo (el anterior deja de servir), en grupos de 4: «A1B2-C3D4-E5F6-0789». */
create function public.create_recovery_code()
returns text language plpgsql security definer set search_path = '' as $$
declare
  me uuid := public.me();
  raw text := upper(encode(extensions.gen_random_bytes(8), 'hex'));
begin
  if me is null then
    raise exception 'not_signed_in' using errcode = 'P0001';
  end if;
  insert into public.recovery_codes (user_id, code_hash)
  values (me, extensions.crypt(raw, extensions.gen_salt('bf', 10)))
  on conflict (user_id) do update set code_hash = excluded.code_hash, created_at = now();
  return substr(raw, 1, 4) || '-' || substr(raw, 5, 4) || '-' || substr(raw, 9, 4) || '-' || substr(raw, 13, 4);
end;
$$;

/** Cuándo se creó el código vigente, o null si no hay. */
create function public.my_recovery_code_date()
returns timestamptz language sql stable security definer set search_path = '' as $$
  select created_at from public.recovery_codes where user_id = public.me();
$$;

/**
 * Pone una contraseña nueva con el código. Devuelve 'ok', 'invalid' (cuenta o código que no cuadran: no se dice
 * cuál), 'weak_password' o 'rate_limited'. No lanza errores para que el intento quede anotado.
 */
create function public.recover_password(login_email text, code text, new_password text)
returns text language plpgsql security definer set search_path = '' as $$
declare
  e text := lower(trim(login_email));
  clean text := upper(regexp_replace(coalesce(code, ''), '[^0-9A-Fa-f]', '', 'g'));
  uid uuid;
  hash text;
begin
  if (select count(*) from public.recovery_attempts where email = e and at > now() - interval '1 hour') >= 5 then
    return 'rate_limited';
  end if;
  insert into public.recovery_attempts (email) values (e);
  delete from public.recovery_attempts where at < now() - interval '1 day';
  if char_length(coalesce(new_password, '')) not between 8 and 72 or new_password !~ '[A-Za-z]' or new_password !~ '[0-9]' then
    return 'weak_password';
  end if;
  select u.id, r.code_hash into uid, hash
  from auth.users u join public.recovery_codes r on r.user_id = u.id
  where lower(u.email) = e;
  if uid is null or char_length(clean) <> 16 or hash <> extensions.crypt(clean, hash) then
    return 'invalid';
  end if;
  update auth.users set encrypted_password = extensions.crypt(new_password, extensions.gen_salt('bf', 10)), updated_at = now()
  where id = uid;
  delete from public.recovery_codes where user_id = uid;
  delete from auth.sessions where user_id = uid;
  delete from auth.refresh_tokens where user_id = uid::text;
  delete from public.recovery_attempts where email = e;
  return 'ok';
end;
$$;

revoke all on function public.create_recovery_code(), public.my_recovery_code_date(),
  public.recover_password(text, text, text) from public, anon, authenticated;
grant execute on function public.create_recovery_code(), public.my_recovery_code_date() to authenticated;
grant execute on function public.recover_password(text, text, text) to anon, authenticated;
