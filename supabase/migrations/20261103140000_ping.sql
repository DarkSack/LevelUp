-- Un latido para que el proyecto gratis de Supabase no se pause tras 7 días sin uso (lo llama
-- .github/workflows/despierto.yml cada 3 días). Toca la base de datos y no devuelve nada de nadie.

create function public.ping()
returns int language sql stable set search_path = '' as $$
  select 1;
$$;

revoke all on function public.ping() from public;
grant execute on function public.ping() to anon, authenticated;
