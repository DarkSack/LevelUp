export default function Home() {
  return (
    <main style={{ fontFamily: 'system-ui', padding: 40, color: '#eaeef7', background: '#0B0F1A', minHeight: '100vh' }}>
      <h1 style={{ fontSize: 32, marginBottom: 8 }}>LevelUp Backend</h1>
      <p style={{ opacity: 0.75 }}>GraphQL endpoint: <code>/api/graphql</code></p>
    </main>
  );
}
