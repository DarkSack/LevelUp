-- Additional write policies so the client can update its own profile and
-- participate in the community proposals. XP/stats writes stay server-only
-- (edge functions use the admin key).

-- profiles: user can update their own row (already had select).
drop policy if exists "profiles update own" on profiles;
create policy "profiles update own"
  on profiles for update
  using (id = auth.uid())
  with check (id = auth.uid());

-- challenge_proposals: read visible (own + non-rejected), insert own,
-- update own while still pending.
drop policy if exists "proposals insert own" on challenge_proposals;
create policy "proposals insert own"
  on challenge_proposals for insert
  with check (author_id = auth.uid());

drop policy if exists "proposals update own pending" on challenge_proposals;
create policy "proposals update own pending"
  on challenge_proposals for update
  using (author_id = auth.uid() and status = 'pending')
  with check (author_id = auth.uid() and status = 'pending');

-- proposal_votes: user manages their own votes only.
drop policy if exists "votes insert own" on proposal_votes;
create policy "votes insert own"
  on proposal_votes for insert
  with check (user_id = auth.uid());

drop policy if exists "votes delete own" on proposal_votes;
create policy "votes delete own"
  on proposal_votes for delete
  using (user_id = auth.uid());
