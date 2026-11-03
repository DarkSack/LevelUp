-- Hacerle cariño a la mascota de un amigo: una reacción predefinida más ('mascota'), con los mismos límites
-- y el mismo aviso que las demás (send_reaction). Sin texto libre.

alter table public.reactions drop constraint reactions_kind_check;
alter table public.reactions add constraint reactions_kind_check check (kind in
  ('aplauso', 'fuerza', 'estrella', 'fuego', 'corazon',
   'animo_puedes', 'animo_sigue', 'animo_paso', 'animo_contigo', 'animo_descansa',
   'mascota'));
